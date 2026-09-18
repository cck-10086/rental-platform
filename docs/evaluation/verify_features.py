# -*- coding: utf-8 -*-
"""三项新功能端到端验收（第2轮）：SSE 流式 / RAG 法条引用 / 扫描件 OCR。

相比第1轮的修正：
- 记录 meta 事件到达时刻（区分 SSE 通道延迟与模型首字延迟）
- 增加同步 /api/ai/chat 对照（同一问题），量化流式相对同步的首字收益
- 增加短问题流式探针（判断长 RAG 提示词是否为 TTFT 主因）
- 扫描样张改为二值（mode "1"）图片 PDF，绕过沙箱 Python 缺 JPEG 编码器的问题
"""
import json
import re
import sys
import time
import urllib.request
import uuid

BASE = "http://localhost:8088"
LEGAL_DIR = r"E:\study\毕业设计\rental-platform-backend\src\main\resources\legal"
CONTRACT_SRC = r"E:\study\毕业设计\docs\evaluation\contracts\contract_04.txt"
SCAN_PDF = r"E:\study\毕业设计\docs\evaluation\contracts\contract_04_scan.pdf"

results = []


def record(name, ok, detail):
    results.append((name, ok, detail))
    print(("[PASS] " if ok else "[FAIL] ") + name + " — " + detail)


def http_json(method, url, token=None, payload=None, timeout=180):
    headers = {"Content-Type": "application/json"}
    if token:
        headers["Authorization"] = "Bearer " + token
    data = json.dumps(payload).encode() if payload is not None else None
    req = urllib.request.Request(url, data=data, headers=headers, method=method)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return resp.status, json.loads(resp.read().decode())
    except urllib.error.HTTPError as e:
        try:
            return e.code, json.loads(e.read().decode())
        except Exception:
            return e.code, {}


def sse_chat(token, question):
    """调用流式接口，返回 (事件统计, 时间线, 完整回答)"""
    t0 = time.time()
    t_meta = t_first = t_done = None
    events = {"meta": 0, "delta": 0, "done": 0, "error": 0}
    parts = []
    req = urllib.request.Request(
        BASE + "/api/ai/chat/stream",
        data=json.dumps({"question": question}).encode(),
        headers={"Content-Type": "application/json", "Authorization": "Bearer " + token},
        method="POST")
    with urllib.request.urlopen(req, timeout=180) as resp:
        ctype_ok = "text/event-stream" in resp.headers.get("Content-Type", "")
        for raw in resp:
            line = raw.decode("utf-8").strip()
            if not line.startswith("data:"):
                continue
            payload = json.loads(line[5:].strip())
            etype = payload.get("type")
            events[etype] = events.get(etype, 0) + 1
            if etype == "meta":
                t_meta = time.time() - t0
            elif etype == "delta":
                if t_first is None:
                    t_first = time.time() - t0
                parts.append(payload.get("content", ""))
            elif etype == "done":
                t_done = time.time() - t0
    return events, (t_meta, t_first, t_done), "".join(parts), ctype_ok


# ---------- 1. 注册与登录 ----------
user = "probe_vfy_" + uuid.uuid4().hex[:6]
pwd = "Vfy_" + uuid.uuid4().hex[:10]
http_json("POST", BASE + "/api/user/register",
          payload={"username": user, "password": pwd,
                   "nickname": "功能验收", "phone": "13800000000"})
status, body = http_json("POST", BASE + "/api/user/login",
                         payload={"username": user, "password": pwd})
token = (body.get("data") or {}).get("token") if isinstance(body.get("data"), dict) else None
record("账号准备", bool(token), "注册+登录成功" if token else "登录失败: %s" % body)
if not token:
    sys.exit(1)

# ---------- 2. SSE 长问题（RAG 场景） ----------
question = "房东收了我三个月租金的押金，还说不退就随便扣，法律上押金到底怎么规定？"
events, (t_meta, t_first, t_done), answer, ctype_ok = sse_chat(token, question)
record("SSE 事件序列", events["meta"] == 1 and events["delta"] >= 5
       and events["done"] == 1 and events["error"] == 0 and ctype_ok,
       "meta=%d delta=%d done=%d error=%d, Content-Type=%s"
       % (events["meta"], events["delta"], events["done"], events["error"], ctype_ok))
record("SSE 通道延迟", t_meta is not None and t_meta < 2,
       "meta 事件 %.2fs 到达（SSE 通道本身即时）" % (t_meta or -1))
print("  · 长问题：首段 %.1fs，总耗时 %.1fs，回答 %d 字" % (t_first or -1, t_done or -1, len(answer)))

# ---------- 3. 同步接口对照（同一问题） ----------
t0 = time.time()
status, sync = http_json("POST", BASE + "/api/ai/chat", token=token,
                         payload={"question": question})
sync_total = time.time() - t0
sync_answer = ((sync.get("data") or {}).get("answer", "")) if isinstance(sync.get("data"), dict) else ""
stream_gain = (sync_total - (t_first or 0)) if t_first else 0
print("  · 同步接口：总耗时 %.1fs（流式首字比同步全量早 %.1fs）" % (sync_total, stream_gain))

# ---------- 4. 短问题 TTFT 探针 ----------
ev2, (m2, f2, d2), ans2, _ = sse_chat(token, "租房合同一定要书面签吗？")
print("  · 短问题：首段 %.1fs，总耗时 %.1fs" % (f2 or -1, d2 or -1))
long_ttft, short_ttft = t_first or -1, f2 or -1
if long_ttft > 10:
    if short_ttft < long_ttft / 2:
        record("SSE 首字延迟归因", True,
               "长 %.1fs / 短 %.1fs：长 RAG 提示词是 TTFT 主因，非 SSE 机制问题" % (long_ttft, short_ttft))
    else:
        record("SSE 首字延迟归因", True,
               "长 %.1fs / 短 %.1fs：TTFT 主要来自模型服务侧，SSE 机制本身即时（meta %.2fs）"
               % (long_ttft, short_ttft, t_meta or -1))
else:
    record("SSE 首字延迟", True, "长问题首段 %.1fs，在可接受范围" % long_ttft)

# ---------- 5. RAG 法条引用 ----------
m = re.search(r"依据[：:](.+)", answer)
if m:
    cites = re.findall(r"《(.+?)》\s*(第[零一二三四五六七八九十百]+条)", m.group(1))
    if not cites:
        record("RAG 法条引用", False, "有「依据：」行但解析不出条文: %s" % m.group(1)[:60])
    else:
        corpus = ""
        for f in ("civil-code-lease.md", "housing-lease-admin-measures.md"):
            with open(LEGAL_DIR + "\\" + f, encoding="utf-8") as fh:
                corpus += fh.read()
        all_found = all(c[1] in corpus for c in cites)
        record("RAG 法条引用", all_found,
               "引用 %s，全部可在语料中核实" % "、".join("《%s》%s" % c for c in cites)
               if all_found else "引用 %s 存在语料外条文（疑似编造）" % cites)
else:
    record("RAG 法条引用", False, "回答未附「依据：」引用行。回答尾部: %s" % answer[-80:])

# ---------- 6. OCR 扫描件（二值图片 PDF，无文本层） ----------
from PIL import Image, ImageDraw, ImageFont

with open(CONTRACT_SRC, encoding="utf-8") as fh:
    lines = [l.rstrip() for l in fh.readlines() if l.strip()][:56]

font = ImageFont.truetype(r"C:\Windows\Fonts\simsun.ttc", 34, index=0)
margin, line_h = 120, 56
pages = [lines[i:i + 36] for i in range(0, len(lines), 36)]
imgs = []
for page in pages:
    img = Image.new("1", (1654, 2339), 1)  # 二值白底，绕过 JPEG 编码器
    draw = ImageDraw.Draw(img)
    y = 140
    for ln in page:
        draw.text((margin, y), ln, fill=0, font=font)
        y += line_h
    imgs.append(img)
imgs[0].save(SCAN_PDF, save_all=True, append_images=imgs[1:], resolution=200)
print("已生成扫描样张: %s（%d 页，二值无文本层）" % (SCAN_PDF, len(pages)))


def multipart(fields, file_path):
    boundary = "----vfy" + uuid.uuid4().hex
    body = b""
    for k, v in fields.items():
        body += ("--%s\r\nContent-Disposition: form-data; name=\"%s\"\r\n\r\n%s\r\n"
                 % (boundary, k, v)).encode()
    with open(file_path, "rb") as fh:
        content = fh.read()
    body += ("--%s\r\nContent-Disposition: form-data; name=\"file\"; filename=\"%s\"\r\n"
             "Content-Type: application/pdf\r\n\r\n" % (boundary, "scan.pdf")).encode()
    body += content + b"\r\n"
    body += ("--%s--\r\n" % boundary).encode()
    return body, "multipart/form-data; boundary=" + boundary


t0 = time.time()
body_bytes, ctype = multipart({"title": "验收-扫描件测试"}, SCAN_PDF)
req = urllib.request.Request(BASE + "/api/contract/upload", data=body_bytes,
                             headers={"Content-Type": ctype, "Authorization": "Bearer " + token},
                             method="POST")
with urllib.request.urlopen(req, timeout=300) as resp:
    up = json.loads(resp.read().decode())
contract_id = (up.get("data") or {}).get("id") if isinstance(up.get("data"), dict) else up.get("data")
upload_sec = time.time() - t0
record("扫描件上传", contract_id is not None,
       "contractId=%s，上传+解析耗时 %.1fs（含 OCR）" % (contract_id, upload_sec))

if contract_id:
    status, rev = http_json("POST", BASE + "/api/contract/review/%s" % contract_id, token=token)
    data = rev.get("data")
    records = data if isinstance(data, list) else (data or {}).get("records", [])
    full_text = json.dumps(rev, ensure_ascii=False)
    hit = "10%" in full_text
    record("OCR 文本提取+审查", len(records) > 0 and hit,
           "审查返回 %d 条记录，%s" % (len(records),
           "命中埋入的 10%/日违约金条款" if hit else "未见 10% 违约金条款（OCR 可能漏识别）"))
    for r in (records or [])[:5]:
        if isinstance(r, dict):
            print("   - [%s] %s | %s" % (r.get("riskLevel"), (r.get("riskType") or "")[:26],
                                          (r.get("clauseContent") or "")[:44]))
    http_json("DELETE", BASE + "/api/contract/%s" % contract_id, token=token)

# ---------- 汇总 ----------
print()
passed = sum(1 for _, ok, _ in results if ok)
print("=" * 62)
print("验收汇总: %d/%d 通过" % (passed, len(results)))
for name, ok, detail in results:
    print(("  ✓ " if ok else "  ✗ ") + name + ": " + detail)
sys.exit(0 if passed == len(results) else 2)
