# -*- coding: utf-8 -*-
"""
合同审查功能量化评测脚本（零第三方依赖，仅用 Python 标准库）。

功能：
  1. 登录平台获取 JWT；
  2. 清理评测账号名下历史评测合同（title 以 "eval_" 前缀命名的旧数据）；
  3. 逐份上传 contracts/ 目录下的样本合同，调用审查接口；
  4. 将 AI 输出的风险条款与 annotations.csv 人工标注（ground truth）对比；
  5. 输出条款识别准确率/召回率/F1、风险定级准确率、平均耗时等指标，
     完整报告写入 results/ 目录（JSON + CSV），支持重复执行。

用法：
  python run_eval.py --validate            # 只校验标注表质量（不调后端）
  python run_eval.py                       # 完整评测（需后端已启动）
  python run_eval.py --limit 5             # 只评测前 5 份合同
  python run_eval.py --threshold 0.55      # 条款匹配相似度阈值（默认 0.55）

环境变量：
  EVAL_BASE_URL   后端地址，默认 http://localhost:8088
  EVAL_USERNAME   评测账号用户名，默认 probe_235205
  EVAL_PASSWORD   评测账号密码（必填，出于隐私不入仓库）
"""

import argparse
import csv
import difflib
import json
import os
import re
import sys
import time
import unicodedata
import urllib.error
import urllib.parse
import urllib.request
import uuid
from datetime import datetime
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
CONTRACTS_DIR = BASE_DIR / "contracts"
ANNOTATIONS_CSV = BASE_DIR / "annotations.csv"
REPORTS_DIR = BASE_DIR / "results"

UPLOAD_TITLE_PREFIX = "eval_"  # 评测上传的合同标题前缀，用于重复执行前的自动清理


# ---------------------------------------------------------------- HTTP 工具

def build_multipart(fields, files):
    """构造 multipart/form-data 请求体。files: {name: (filename, bytes, ctype)}"""
    boundary = uuid.uuid4().hex
    parts = []
    for name, value in fields.items():
        parts.append(
            f"--{boundary}\r\nContent-Disposition: form-data; name=\"{name}\"\r\n\r\n{value}\r\n".encode("utf-8"))
    for name, (filename, content, ctype) in files.items():
        parts.append(
            f"--{boundary}\r\nContent-Disposition: form-data; name=\"{name}\"; "
            f"filename=\"{filename}\"\r\nContent-Type: {ctype}\r\n\r\n".encode("utf-8"))
        parts.append(content)
        parts.append(b"\r\n")
    parts.append(f"--{boundary}--\r\n".encode("utf-8"))
    return b"".join(parts), f"multipart/form-data; boundary={boundary}"


def http_json(method, url, token=None, payload=None, data=None, content_type=None, timeout=180):
    """发送 HTTP 请求，返回 (status, body_dict)。非 2xx 也返回 body 便于诊断。"""
    headers = {}
    if token:
        headers["Authorization"] = f"Bearer {token}"
    body = None
    if payload is not None:
        body = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        headers["Content-Type"] = "application/json; charset=utf-8"
    elif data is not None:
        body = data
        headers["Content-Type"] = content_type
    req = urllib.request.Request(url, data=body, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, json.loads(resp.read().decode("utf-8"))
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode("utf-8"))
        except Exception:
            return e.code, {}


def check_result(action, status, body):
    """校验平台统一响应 Result{code,message,data}，失败则抛异常终止。"""
    if status != 200 or body.get("code") != 200:
        raise RuntimeError(f"{action} 失败: http={status}, code={body.get('code')}, "
                           f"message={body.get('message')}")


# ---------------------------------------------------------------- 业务流程

def login(base_url, username, password):
    status, body = http_json("POST", base_url + "/api/user/login",
                             payload={"username": username, "password": password})
    check_result("登录", status, body)
    token = (body.get("data") or {}).get("token")
    if not token:
        raise RuntimeError("登录成功但响应中未找到 token 字段")
    return token


def cleanup_previous_runs(base_url, token):
    """删除评测账号名下历史评测合同（标题以 eval_ 开头），保证脚本可重复执行。"""
    url = (base_url + "/api/contract/list?keyword=" + urllib.parse.quote(UPLOAD_TITLE_PREFIX)
           + "&current=1&size=100")
    status, body = http_json("GET", url, token=token)
    check_result("查询历史评测合同", status, body)
    records = ((body.get("data") or {}).get("records")) or []
    removed = 0
    for record in records:
        title = str(record.get("title") or "")
        if not title.startswith(UPLOAD_TITLE_PREFIX):
            continue
        del_status, del_body = http_json("DELETE", f"{base_url}/api/contract/{record['id']}", token=token)
        if del_status == 200 and del_body.get("code") == 200:
            removed += 1
    return removed


def run_evaluation(base_url, token, contracts, limit, offset=0, details_path=None):
    """逐份上传→审查，返回评测明细列表；每完成一份即追加写入明细文件，支持分批与断点合并。"""
    details = []
    for index, contract_path in enumerate(contracts):
        if limit and index >= limit:
            break
        if index < offset:
            continue
        contract_id = f"C{contract_path.stem.split('_')[-1]}"
        content = contract_path.read_bytes()
        title = UPLOAD_TITLE_PREFIX + contract_id

        body, content_type = build_multipart(
            {"title": title}, {"file": (contract_path.name, content, "text/plain")})
        up_status, up_body = http_json(
            "POST", base_url + "/api/contract/upload", token=token,
            data=body, content_type=content_type)
        check_result(f"上传 {contract_path.name}", up_status, up_body)
        contract_pk = (up_body.get("data") or {}).get("id")
        if not contract_pk:
            raise RuntimeError(f"上传 {contract_path.name} 成功但响应中未找到合同 id")

        start = time.perf_counter()
        rev_status, rev_body = http_json("POST", f"{base_url}/api/contract/review/{contract_pk}",
                                         token=token)
        elapsed = time.perf_counter() - start
        check_result(f"审查 {contract_path.name}", rev_status, rev_body)

        data = rev_body.get("data") or {}
        records = data.get("records") or []
        predictions = [{
            "clause": str(r.get("clauseContent") or ""),
            "risk_level": str(r.get("riskLevel") or "").strip().lower(),
            "risk_type": str(r.get("riskType") or ""),
        } for r in records]
        details.append({
            "contract_id": contract_id,
            "contract_file": contract_path.name,
            "elapsed_sec": round(elapsed, 2),
            "predictions": predictions,
        })
        print(f"  [{contract_id}] {contract_path.name} 上传+审查完成，"
              f"识别 {len(predictions)} 条，耗时 {elapsed:.1f}s")
        if details_path:
            with open(details_path, "a", encoding="utf-8") as f:
                f.write(json.dumps(details[-1], ensure_ascii=False) + "\n")
    return details


# ---------------------------------------------------------------- 标注与匹配

def load_annotations():
    """读取人工标注表。返回 {contract_id: [ {clause_type, true_risk_level, clause_text} ]}"""
    if not ANNOTATIONS_CSV.exists():
        raise FileNotFoundError(f"标注表不存在: {ANNOTATIONS_CSV}")
    annotations = {}
    with open(ANNOTATIONS_CSV, newline="", encoding="utf-8-sig") as f:
        for row in csv.DictReader(f):
            cid = (row.get("contract_id") or "").strip().upper()
            if not cid:
                continue
            annotations.setdefault(cid, []).append({
                "clause_type": (row.get("clause_type") or "").strip(),
                "true_risk_level": (row.get("true_risk_level") or "").strip().lower(),
                "clause_text": (row.get("clause_text") or "").strip(),
            })
    return annotations


def normalize(text):
    """文本规范化：NFKC 归一、去全部空白、转小写，用于相似度计算。"""
    text = unicodedata.normalize("NFKC", text or "")
    return re.sub(r"\s+", "", text).lower()


def similarity(a_norm, b_norm):
    """相似度：字符级 SequenceMatcher 与双向包含取最大值。"""
    if not a_norm or not b_norm:
        return 0.0
    if a_norm in b_norm or b_norm in a_norm:
        return 1.0
    return difflib.SequenceMatcher(None, a_norm, b_norm).ratio()


# 风险类型语义匹配关键词对照表：类别 → (标注侧 clause_type 关键词, 预测侧 risk_type 关键词)
# 双方命中同一类别即认为语义匹配；后续迭代可按需扩充类别与关键词。
RISK_TYPE_MATCH_TABLE = {
    "押金":   (["押金"], ["押金退还", "押金返还", "押金扣除", "押金"]),
    "违约金": (["违约金", "违约"], ["违约金过高", "逾期违约金", "违约金"]),
    "维修":   (["维修"], ["维修责任", "维修救济", "维修义务", "维修"]),
    "权属":   (["权属", "产权", "出租权"], ["权属", "出租权限", "权利瑕疵", "产权"]),
    "退租":   (["提前退租", "提前解除", "提前解约", "退租"], ["提前退租", "提前解除", "提前解约", "退租"]),
    "装修":   (["装修"], ["装修"]),
    "转租":   (["转租"], ["转租"]),
    "租金":   (["租金"], ["租金"]),
    # 「其他」类标注（如随时进入、强制搬离等居住权益条款）按预测侧关键词语义匹配
    "其他":   (["其他"], ["进入", "安宁", "隐私", "搬离", "买卖不破租赁", "出售房屋", "强制搬离", "居住权益"]),
}


def risk_type_match(clause_type, risk_type, clause_text=""):
    """风险类型语义匹配：标注 clause_type 与预测 risk_type 命中同一类别关键词即匹配。

    标注类型为「其他」或空时无类型信息可依，回退用标注条款原文判断预测侧关键词是否出现。
    """
    ct = normalize(clause_type)
    rt = normalize(risk_type)
    gt_text = normalize(clause_text)
    fallback_type = not ct or ct in normalize("其他")
    for gt_keys, pred_keys in RISK_TYPE_MATCH_TABLE.values():
        if not any(k in rt for k in pred_keys):
            continue
        if any(k in ct for k in gt_keys):
            return True
        if fallback_type and gt_text and any(k in gt_text for k in pred_keys):
            return True
    return False


def dedupe_predictions(predictions, sim_threshold=0.9):
    """预测去重：同合同内 clause 文本相同或相似度超过阈值的记录合并为一条。

    合并规则：clause 与 risk_level 保留首条，risk_type 用顿号连接保留全部。
    """
    merged = []
    for pred in predictions:
        norm = normalize(pred["clause"])
        target = None
        for m in merged:
            if similarity(norm, m["_norm"]) > sim_threshold:
                target = m
                break
        if target is not None:
            rt = pred.get("risk_type") or ""
            if rt and rt not in target["risk_type"]:
                target["risk_type"] = f"{target['risk_type']}、{rt}" if target["risk_type"] else rt
        else:
            merged.append({**pred, "_norm": norm})
    for m in merged:
        m.pop("_norm", None)
    return merged


def match_pairs(ground_truth, predictions, threshold):
    """贪心一对一匹配：文本相似度达阈值，或风险类型语义匹配（关键词对照表）。
    返回 (tp_pairs, fp_predictions, fn_annotations)。"""
    candidates = []
    for gi, gt in enumerate(ground_truth):
        gt_norm = normalize(gt["clause_text"])
        for pi, pred in enumerate(predictions):
            score = similarity(gt_norm, normalize(pred["clause"]))
            if score >= threshold:
                candidates.append((score, gi, pi))
            elif risk_type_match(gt.get("clause_type", ""), pred.get("risk_type", ""),
                                 gt.get("clause_text", "")):
                # 语义匹配作为补充候选：固定分数排在纯文本匹配之后参与贪心配对
                candidates.append((0.5, gi, pi))
    candidates.sort(reverse=True)

    used_g, used_p = set(), set()
    pairs = []
    for score, gi, pi in candidates:
        if gi in used_g or pi in used_p:
            continue
        used_g.add(gi)
        used_p.add(pi)
        pairs.append((gi, pi, score))
    fp = [p for i, p in enumerate(predictions) if i not in used_p]
    fn = [g for i, g in enumerate(ground_truth) if i not in used_g]
    return pairs, fp, fn


def validate_annotations(threshold):
    """校验标注表质量：每条 clause_text 应能在对应合同文件中找到高相似度原文。"""
    annotations = load_annotations()
    ok = bad = 0
    for cid in sorted(annotations):
        contract_path = CONTRACTS_DIR / f"contract_{cid[1:]}.txt"
        if not contract_path.exists():
            print(f"[缺失] {cid}: 合同文件不存在 {contract_path.name}")
            bad += len(annotations[cid])
            continue
        contract_norm = normalize(contract_path.read_text(encoding="utf-8-sig"))
        for item in annotations[cid]:
            score = similarity(normalize(item["clause_text"]), contract_norm)
            if score >= max(threshold, 0.8):
                ok += 1
            else:
                bad += 1
                print(f"[不匹配] {cid} ({item['clause_type']}/{item['true_risk_level']}) "
                      f"相似度={score:.2f}: {item['clause_text'][:40]}...")
    print(f"标注校验完成：{ok} 条通过，{bad} 条异常，共 {ok + bad} 条")
    return bad == 0


# ---------------------------------------------------------------- 指标与报告

def compute_metrics(details, annotations, threshold):
    """计算整体与每份合同的评测指标。"""
    tp = fp = fn = 0
    level_hit = 0
    per_contract = []
    for item in details:
        cid = item["contract_id"]
        ground_truth = annotations.get(cid, [])
        # 匹配前先对预测去重，避免同条款重复输出被重复计 FP
        deduped = dedupe_predictions(item["predictions"])
        pairs, fp_list, fn_list = match_pairs(ground_truth, deduped, threshold)
        c_tp, c_fp, c_fn = len(pairs), len(fp_list), len(fn_list)
        tp += c_tp
        fp += c_fp
        fn += c_fn
        c_hit = sum(1 for gi, pi, _ in pairs
                    if ground_truth[gi]["true_risk_level"] == deduped[pi]["risk_level"])
        level_hit += c_hit
        per_contract.append({
            **{k: item[k] for k in ("contract_id", "contract_file", "elapsed_sec")},
            "annotated": len(ground_truth),
            "predicted": len(deduped),
            "tp": c_tp, "fp": c_fp, "fn": c_fn,
            "level_hit": c_hit,
            "missed": [{"clause_type": g["clause_type"], "true_risk_level": g["true_risk_level"],
                        "clause_text": g["clause_text"]} for g in fn_list],
            "false_positive": [{"clause": p["clause"], "risk_level": p["risk_level"],
                                "risk_type": p["risk_type"]} for p in fp_list],
        })

    precision = tp / (tp + fp) if (tp + fp) else 0.0
    recall = tp / (tp + fn) if (tp + fn) else 0.0
    f1 = 2 * precision * recall / (precision + recall) if (precision + recall) else 0.0
    level_acc = level_hit / tp if tp else 0.0
    avg_elapsed = sum(d["elapsed_sec"] for d in details) / len(details) if details else 0.0
    return {
        "tp": tp, "fp": fp, "fn": fn,
        "precision": round(precision, 4),
        "recall": round(recall, 4),
        "f1": round(f1, 4),
        "level_hit": level_hit,
        "level_accuracy": round(level_acc, 4),
        "avg_elapsed_sec": round(avg_elapsed, 2),
        "per_contract": per_contract,
    }


def print_report(metrics):
    """控制台输出指标表格。"""
    print("\n================ 每份合同明细 ================")
    print(f"{'合同':<6}{'标注':>4}{'预测':>4}{'TP':>4}{'FP':>4}{'FN':>4}"
          f"{'定级正确':>6}{'耗时(s)':>9}")
    for c in metrics["per_contract"]:
        print(f"{c['contract_id']:<7}{c['annotated']:>5}{c['predicted']:>5}{c['tp']:>4}"
              f"{c['fp']:>4}{c['fn']:>4}{c['level_hit']:>7}{c['elapsed_sec']:>10}")
    print("\n================ 整体指标 ================")
    print(f"条款识别  TP={metrics['tp']}  FP={metrics['fp']}  FN={metrics['fn']}")
    print(f"条款识别准确率(Precision): {metrics['precision']:.2%}")
    print(f"条款识别召回率(Recall):    {metrics['recall']:.2%}")
    print(f"条款识别 F1:               {metrics['f1']:.4f}")
    print(f"风险定级准确率:            {metrics['level_accuracy']:.2%}"
          f"  （匹配对中 riskLevel 与标注一致 {metrics['level_hit']} 条）")
    print(f"平均审查耗时:              {metrics['avg_elapsed_sec']:.1f}s / 份")


def save_reports(metrics, threshold, base_url, out_stem=None):
    """完整报告落盘：JSON（含明细）+ CSV（每份合同汇总）。out_stem 指定固定文件名前缀。"""
    REPORTS_DIR.mkdir(exist_ok=True)
    ts = datetime.now().strftime("%Y%m%d_%H%M%S")
    stem = out_stem or f"eval_report_{ts}"
    json_path = REPORTS_DIR / f"{stem}.json"
    csv_path = REPORTS_DIR / f"{stem}.csv"

    report = {"evaluated_at": ts, "base_url": base_url, "match_threshold": threshold,
              **metrics}
    json_path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding="utf-8")

    with open(csv_path, "w", newline="", encoding="utf-8-sig") as f:
        writer = csv.writer(f)
        writer.writerow(["contract_id", "contract_file", "标注条款数", "预测条款数",
                         "TP", "FP", "FN", "定级正确", "耗时秒"])
        for c in metrics["per_contract"]:
            writer.writerow([c["contract_id"], c["contract_file"], c["annotated"],
                             c["predicted"], c["tp"], c["fp"], c["fn"],
                             c["level_hit"], c["elapsed_sec"]])
    print(f"\n报告已写入: {json_path}")
    print(f"           {csv_path}")


# ---------------------------------------------------------------- 入口

def load_details(path):
    """读取逐份追加的明细 JSONL，同一合同重复出现时保留最后一次，按合同 ID 排序。"""
    p = Path(path)
    if not p.exists():
        return []
    by_id = {}
    with open(p, encoding="utf-8") as f:
        for line in f:
            line = line.strip()
            if not line:
                continue
            item = json.loads(line)
            by_id[item.get("contract_id")] = item
    return sorted(by_id.values(), key=lambda d: d.get("contract_id") or "")


def main():
    parser = argparse.ArgumentParser(description="合同审查功能量化评测")
    parser.add_argument("--validate", action="store_true", help="仅校验标注表与合同原文一致性")
    parser.add_argument("--limit", type=int, default=0, help="只评测前 N 份合同（0=全部）")
    parser.add_argument("--offset", type=int, default=0, help="跳过前 N 份合同（配合 --details 分批执行）")
    parser.add_argument("--details", default=None, help="明细 JSONL 路径：逐份追加，跨批合并后统一汇总")
    parser.add_argument("--collect", action="store_true", help="不发起请求，仅从明细文件汇总指标")
    parser.add_argument("--rescore", action="store_true",
                        help="不调后端，读明细文件用新匹配逻辑重算指标，输出 eval_report_rescored")
    parser.add_argument("--threshold", type=float, default=0.55, help="条款匹配相似度阈值")
    parser.add_argument("--base-url", default=None, help="后端地址，默认读环境变量 EVAL_BASE_URL")
    parser.add_argument("--username", default=None, help="评测账号，默认读环境变量 EVAL_USERNAME")
    parser.add_argument("--password", default=None, help="评测密码，默认读环境变量 EVAL_PASSWORD")
    args = parser.parse_args()

    if args.validate:
        sys.exit(0 if validate_annotations(args.threshold) else 1)

    base_url = (args.base_url or os_env("EVAL_BASE_URL", "http://localhost:8088")).rstrip("/")
    details_path = args.details or str(REPORTS_DIR / "eval_details.jsonl")

    if args.rescore:
        details = load_details(details_path)
        if not details:
            print(f"错误：明细文件不存在或为空: {details_path}")
            sys.exit(1)
        annotations = load_annotations()
        metrics = compute_metrics(details, annotations, args.threshold)
        print_report(metrics)
        save_reports(metrics, args.threshold, base_url, out_stem="eval_report_rescored")
        return

    if args.collect:
        details = load_details(details_path)
        if not details:
            print(f"错误：明细文件不存在或为空: {details_path}")
            sys.exit(1)
        annotations = load_annotations()
        metrics = compute_metrics(details, annotations, args.threshold)
        print_report(metrics)
        save_reports(metrics, args.threshold, base_url)
        return

    username = args.username or os_env("EVAL_USERNAME", "probe_235205")
    password = args.password or os_env("EVAL_PASSWORD", "")
    if not password:
        print("错误：未提供评测账号密码，请设置环境变量 EVAL_PASSWORD 或使用 --password 参数")
        sys.exit(1)

    contracts = sorted(CONTRACTS_DIR.glob("contract_*.txt"))
    if not contracts:
        print(f"错误：样本合同目录为空: {CONTRACTS_DIR}")
        sys.exit(1)
    print(f"评测配置: base_url={base_url}, 用户={username}, 合同数={len(contracts)}, "
          f"匹配阈值={args.threshold}")

    token = login(base_url, username, password)
    print("登录成功，已获取 JWT")

    removed = cleanup_previous_runs(base_url, token)
    if removed:
        print(f"已清理历史评测合同 {removed} 份")

    print("开始逐份上传并审查...")
    details = run_evaluation(base_url, token, contracts, args.limit,
                             offset=args.offset, details_path=details_path)

    annotations = load_annotations()
    metrics = compute_metrics(details, annotations, args.threshold)
    print_report(metrics)
    save_reports(metrics, args.threshold, base_url)


def os_env(key, default):
    return os.environ.get(key, default)


if __name__ == "__main__":
    main()
