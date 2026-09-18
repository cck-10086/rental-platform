# -*- coding: utf-8 -*-
"""三轮评测对比报告生成。

对比对象：
    1. 基线   = eval_report_20260917_182515（旧提示词 + 旧匹配逻辑）
    2. 修正匹配 = eval_report_rescored（旧提示词 + 新匹配逻辑）
    3. 复测   = eval_report_20260918_133732（新提示词 + 新匹配逻辑）

对比项：precision / recall / F1 / level_accuracy / 平均耗时 /
       每合同风险条数分布（最小-最大-均值）/ 干净合同（C01、C09）风险条数 /
       重复报告条数（同合同内条款原文完全相同的冗余输出数）。

输出：results/comparison_report.json 与 .csv（新文件，不覆盖任何既有报告）。
"""

import csv
import json
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent
REPORTS_DIR = BASE_DIR / "results"

# 三轮评测（轮次名称 → 报告文件）
ROUNDS = [
    ("基线（旧提示词+旧匹配）", "eval_report_20260917_182515.json"),
    ("修正匹配（旧提示词+新匹配）", "eval_report_rescored.json"),
    ("复测（新提示词+新匹配）", "eval_report_20260918_133732.json"),
]

CLEAN_CONTRACTS = ("C01", "C09")  # 零埋入风险的干净合同


def load_report(filename):
    with open(REPORTS_DIR / filename, encoding="utf-8") as f:
        return json.load(f)


def duplicate_count(report):
    """重复报告条数：同合同内 false_positive 条款原文完全相同的冗余条数（组内条数-1 求和）。"""
    total = 0
    for contract in report.get("per_contract", []):
        counter = {}
        for fp in contract.get("false_positive", []):
            key = (fp.get("clause") or "").strip()
            counter[key] = counter.get(key, 0) + 1
        total += sum(n - 1 for n in counter.values() if n > 1)
    return total


def summarize(name, report):
    """提取单轮对比指标。"""
    predicted_list = [c["predicted"] for c in report.get("per_contract", [])]
    clean = {c["contract_id"]: c["predicted"]
             for c in report.get("per_contract", []) if c["contract_id"] in CLEAN_CONTRACTS}
    return {
        "轮次": name,
        "precision": report.get("precision"),
        "recall": report.get("recall"),
        "f1": report.get("f1"),
        "level_accuracy": report.get("level_accuracy"),
        "avg_elapsed_sec": report.get("avg_elapsed_sec"),
        "tp": report.get("tp"),
        "fp": report.get("fp"),
        "fn": report.get("fn"),
        "风险条数_最小": min(predicted_list) if predicted_list else 0,
        "风险条数_最大": max(predicted_list) if predicted_list else 0,
        "风险条数_均值": round(sum(predicted_list) / len(predicted_list), 2) if predicted_list else 0,
        "干净合同_C01_预测数": clean.get("C01"),
        "干净合同_C09_预测数": clean.get("C09"),
        "重复报告条数": duplicate_count(report),
    }


def main():
    summaries = [summarize(name, load_report(filename)) for name, filename in ROUNDS]

    json_path = REPORTS_DIR / "comparison_report.json"
    json_path.write_text(json.dumps(summaries, ensure_ascii=False, indent=2), encoding="utf-8")

    csv_path = REPORTS_DIR / "comparison_report.csv"
    fieldnames = list(summaries[0].keys())
    with open(csv_path, "w", newline="", encoding="utf-8-sig") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(summaries)

    print(f"对比报告已写入: {json_path}")
    print(f"               {csv_path}\n")
    for s in summaries:
        print(f"== {s['轮次']} ==")
        print(f"  precision={s['precision']:.2%}  recall={s['recall']:.2%}  "
              f"F1={s['f1']:.4f}  定级准确率={s['level_accuracy']:.2%}")
        print(f"  TP={s['tp']}  FP={s['fp']}  FN={s['fn']}  平均耗时={s['avg_elapsed_sec']}s")
        print(f"  每合同风险条数: 最小={s['风险条数_最小']} 最大={s['风险条数_最大']} "
              f"均值={s['风险条数_均值']}")
        print(f"  干净合同预测数: C01={s['干净合同_C01_预测数']} C09={s['干净合同_C09_预测数']}  "
              f"重复报告条数={s['重复报告条数']}\n")


if __name__ == "__main__":
    import json
    main()
