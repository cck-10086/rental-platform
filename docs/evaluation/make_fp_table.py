# -*- coding: utf-8 -*-
"""FP 裁决表生成：读取基线评测报告中的假阳性（FP）记录，生成人工裁决表。

用法：
    python make_fp_table.py

输出：
    fp_adjudication.csv（列：contract_id, clause, risk_level, risk_type, 预分类, 人工裁决, 备注）
    并在终端打印三类预分类统计。

预分类规则：
    - 同一 contract_id 内 clause 文本完全相同 → 「重复报告」，组内仅首条标「重复报告-首条」
    - clause 含全角空格日期空位模式（如「年　月」「　日」）→ 「日期空位」
    - 其余 → 「待人工」
可重复执行（覆盖生成）；不修改 results 目录下任何文件。
"""

import csv
import re
from collections import Counter
from pathlib import Path

BASELINE_REPORT = Path(__file__).resolve().parent / "results" / "eval_report_20260917_182515.json"
OUTPUT_CSV = Path(__file__).resolve().parent / "fp_adjudication.csv"

# 全角空格（U+3000）日期空位：模板中「2026年9月　日」之类未填写的签署日期
DATE_BLANK_PATTERN = re.compile(r"年\u3000月|\u3000月|\u3000日")

# 人工裁决列的候选项（仅作为表头说明，不预填）
ADJUDICATION_OPTIONS = "合理发现 / 过度预警 / 重复报告"


def preclassify(rows):
    """对单份合同的 FP 列表做预分类，就地填充 row['预分类']。"""
    # 1) 完全相同的 clause 视为重复报告组：组内首条标「重复报告-首条」
    groups = {}
    for row in rows:
        groups.setdefault(row["clause"], []).append(row)
    for members in groups.values():
        if len(members) > 1:
            members[0]["预分类"] = "重复报告-首条"
            for member in members[1:]:
                member["预分类"] = "重复报告"

    # 2) 未落入重复组的条目：检测全角空格日期空位
    for row in rows:
        if row["预分类"]:
            continue
        if DATE_BLANK_PATTERN.search(row["clause"]):
            row["预分类"] = "日期空位"
        else:
            row["预分类"] = "待人工"


def main():
    import json

    if not BASELINE_REPORT.exists():
        raise SystemExit(f"基线报告不存在: {BASELINE_REPORT}")
    with open(BASELINE_REPORT, encoding="utf-8") as f:
        report = json.load(f)

    all_rows = []
    for contract in report.get("per_contract", []):
        rows = [{
            "contract_id": contract.get("contract_id", ""),
            "clause": (fp.get("clause") or "").strip(),
            "risk_level": (fp.get("risk_level") or "").strip(),
            "risk_type": (fp.get("risk_type") or "").strip(),
            "预分类": "",
            "人工裁决": "",
            "备注": "",
        } for fp in contract.get("false_positive", [])]
        preclassify(rows)
        all_rows.extend(rows)

    # 覆盖生成裁决表（utf-8-sig 便于 Excel 直接打开）
    with open(OUTPUT_CSV, "w", newline="", encoding="utf-8-sig") as f:
        writer = csv.DictWriter(f, fieldnames=[
            "contract_id", "clause", "risk_level", "risk_type",
            "预分类", "人工裁决", "备注"])
        writer.writeheader()
        writer.writerows(all_rows)

    # 终端打印预分类统计
    counter = Counter(row["预分类"] for row in all_rows)
    print(f"裁决表已生成: {OUTPUT_CSV}")
    print(f"总行数: {len(all_rows)}（预期 176）")
    print(f"人工裁决列候选项: {ADJUDICATION_OPTIONS}")
    print("预分类统计:")
    for label in ["重复报告-首条", "重复报告", "日期空位", "待人工"]:
        print(f"  {label}: {counter.get(label, 0)}")

    if len(all_rows) != 176:
        raise SystemExit(f"行数异常: {len(all_rows)} != 176，请检查基线报告")


if __name__ == "__main__":
    main()
