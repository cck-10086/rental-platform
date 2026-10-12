# -*- coding: utf-8 -*-
"""
主链路 E2E 测试：登录 → 上传合同 → 开始审查 → 等待完成 → 报告断言 → 导出 PDF

前置条件：
1. 后端 8088 已启动（java -jar），数据库与 MinIO 就绪
2. 前端 4173 已启动（npm run build && npm run preview）
3. DEEPSEEK_API_KEY 可用（审查为真实 AI 调用）

运行方式（在 rental-platform-frontend 目录下）：
    # 凭据出于隐私不入仓库，通过环境变量传入（Windows PowerShell 示例）
    $env:E2E_USERNAME='admin'; $env:E2E_PASSWORD='<演示密码>'
    python e2e/main_flow.py
依赖：pip install playwright && python -m playwright install chromium
"""
import os
import re
import sys
import tempfile
import uuid
from pathlib import Path

from playwright.sync_api import expect, sync_playwright

BASE_URL = os.environ.get("E2E_BASE_URL", "http://localhost:4173")
USERNAME = os.environ.get("E2E_USERNAME", "")
PASSWORD = os.environ.get("E2E_PASSWORD", "")

# 每次运行生成唯一文本，确保不命中审查缓存，走完整审查链路
CONTRACT_TEXT = """房屋租赁合同
出租方（甲方）：张三
承租方（乙方）：李四

第一条 房屋基本情况：甲方将位于某市某区幸福路12号3单元402室的房屋出租给乙方使用，建筑面积60平方米。
第二条 租赁期限：自2026年11月1日起至2027年10月31日止，共计12个月。
第三条 租金及支付方式：月租金2000元，乙方应于每月5日前支付当月租金。
第四条 押金：乙方于签约时向甲方支付押金4000元。
第五条 房屋维修：房屋及附属设施的自然损耗由甲方负责维修，甲方应在接到通知后5日内完成维修。
第六条 违约责任：乙方逾期支付租金的，每逾期一日按月租金的2%向甲方支付违约金。
第七条 租金调整：租赁期内甲方有权根据市场情况随时调整租金，乙方应按调整后的标准支付。
第八条 争议解决：本合同履行过程中发生争议的，双方应友好协商解决。

（E2E 运行标识：{run_id}）
"""


def run() -> None:
    if not USERNAME or not PASSWORD:
        print("错误：请通过环境变量 E2E_USERNAME / E2E_PASSWORD 提供测试账号（出于隐私不入仓库）")
        sys.exit(1)
    run_id = uuid.uuid4().hex[:12]
    tmp_file = Path(tempfile.gettempdir()) / f"e2e-contract-{run_id}.txt"
    tmp_file.write_text(CONTRACT_TEXT.format(run_id=run_id), encoding="utf-8")

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={"width": 1440, "height": 900})

        try:
            # 1. 登录
            page.goto(f"{BASE_URL}/login")
            page.get_by_placeholder("用户名").fill(USERNAME)
            page.get_by_placeholder("密码").fill(PASSWORD)
            page.get_by_role("button", name="登录").click()
            expect(page).to_have_url(re.compile(r"dashboard"), timeout=15000)
            print("[1/5] 登录成功，已进入工作台")

            # 2. 上传合同（自动跳转审查报告页）
            page.goto(f"{BASE_URL}/contract/upload")
            page.locator('input[type="file"]').set_input_files(str(tmp_file))
            page.get_by_placeholder(re.compile("请输入合同标题")).fill(f"E2E 主链路测试合同 {run_id}")
            page.get_by_role("button", name="开始上传").click()
            expect(page).to_have_url(re.compile(r"/contract/review/\d+"), timeout=30000)
            print("[2/5] 合同上传成功，已跳转审查报告页")

            # 3. 发起审查并等待 AI 完成（真实调用，预留 150 秒）
            page.get_by_role("button", name=re.compile("开始审查")).click()
            first_clause = page.locator(".clause-item").first
            expect(first_clause).to_be_visible(timeout=150_000)
            print("[3/5] AI 审查完成，风险条款已生成")

            # 4. 报告关键状态断言：摘要统计 + 反馈按钮
            expect(page.locator(".summary-card")).to_be_visible()
            expect(page.locator(".clause-feedback__buttons").first).to_be_visible()
            print("[4/5] 报告页断言通过：摘要统计与反馈按钮均正常")

            # 5. 导出 PDF 报告
            with page.expect_download() as download_info:
                page.get_by_role("button", name="导出报告 PDF").click()
            download = download_info.value
            pdf_path = Path(tempfile.gettempdir()) / download.suggested_filename
            download.save_as(str(pdf_path))
            assert download.suggested_filename.endswith(".pdf"), "导出文件应为 PDF"
            assert pdf_path.stat().st_size > 500, "导出的 PDF 不应为空文件"
            print(f"[5/5] 报告 PDF 导出成功：{pdf_path.name}（{pdf_path.stat().st_size} 字节）")

            print("E2E 主链路测试全部通过 ✓")
        finally:
            browser.close()
            if tmp_file.exists():
                tmp_file.unlink()


if __name__ == "__main__":
    try:
        run()
    except Exception as exc:  # noqa: BLE001
        print(f"E2E 主链路测试失败：{exc}")
        sys.exit(1)
