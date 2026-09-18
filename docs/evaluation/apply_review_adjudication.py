# -*- coding: utf-8 -*-
# 合入第二轮人工复核裁决：逐条写入CSV，与rescored报告交叉核对，计算双口径真实精确率
# 编码：H=高风险合理发现 M=中风险合理发现 L=低风险/过度预警
#       DF=重复报告-首条 D=重复报告 T=填写完整性提醒
import csv, json, collections

BASE = r'E:\study\毕业设计\docs\evaluation'
CSV_PATH = BASE + r'\fp_adjudication.csv'
RESCORED = BASE + r'\results\eval_report_rescored.json'

# 复核裁决（来自用户上传的逐条裁决文件，按合同+序号与CSV行一一对应）
REVIEW = {
    'C01': ['L','M','L','L','L','L','L','L','L','T','L'],
    'C02': ['L','L','L','L','L','L','L','L','L','T'],
    'C03': ['M','L','L','L','L','L','L','M','L','L','L'],
    'C04': ['H','L','L','L','L','L','L','L','L'],
    'C05': ['M','L','L','L','L','M','L','T'],
    'C06': ['H','L','DF','L','L','L','DF','L','L','L','L','D','D'],
    'C07': ['L','DF','D','L','L','H','M','L','L','L','T'],
    'C08': ['M','L','L','L','L','L','L','M'],
    'C09': ['L','L','L','L','L','L','L','L','L','L','L','T'],
    'C10': ['M','L','L','L','L','M','T','L'],
    'C11': ['M','L','L','L','L','L','L','L','T','L'],
    'C12': ['L','L','L','M','L','L','L','L','L','M','T'],
    'C13': ['M','L','L','L','L','L','L','L'],
    'C14': ['L','L','L','L','L','T'],
    'C15': ['M','L','L','L','L','M','L','L','L','T','L'],
    'C16': ['H','DF','D','L','L','L','L','L','D','L'],
    'C17': ['L','L','L','L','L','H','L','M','L','L','L','T','L'],
    'C18': ['M','L','L','L','L','L'],
}
LABEL = {'H':'高风险合理发现','M':'中风险合理发现','L':'低风险/过度预警',
         'DF':'重复报告-首条','D':'重复报告','T':'填写完整性提醒'}

# 高风险条目的关键词断言（防止行序错位）
H_CHECK = {('C04',0):'10%', ('C06',0):'随时进入', ('C07',5):'装修',
           ('C16',0):'三倍', ('C17',5):'粉刷'}

rows = list(csv.DictReader(open(CSV_PATH, encoding='utf-8-sig')))
assert len(rows) == 176, '行数异常: %d' % len(rows)

by_c = collections.OrderedDict()
for r in rows:
    by_c.setdefault(r['contract_id'], []).append(r)
for cid, codes in REVIEW.items():
    assert len(by_c[cid]) == len(codes), '%s 条数不符: CSV%d vs 裁决%d' % (cid, len(by_c[cid]), len(codes))
for (cid, idx), kw in H_CHECK.items():
    assert kw in by_c[cid][idx]['clause'], '%s 第%d行缺少关键词%s，行序可能错位' % (cid, idx+1, kw)

# 载入rescored报告，构建每合同的FP条款集合（规范化：去全部空白）
res = json.load(open(RESCORED, encoding='utf-8'))
def norm(s): return ''.join(s.split())
fp_exact, fp_fuzzy = {}, {}
for c in res['per_contract']:
    fp_exact[c['contract_id']] = {norm(fp['clause']) for fp in c.get('false_positive', [])}
    fp_fuzzy[c['contract_id']] = [norm(fp['clause']) for fp in c.get('false_positive', [])]

def survives(row):
    cid, n = row['contract_id'], norm(row['clause'])
    if n in fp_exact[cid]:
        return 'exact'
    for f in fp_fuzzy[cid]:
        if n[:12] in f or f[:12] in n:
            return 'fuzzy'
    return 'no'

# 填写复核裁决与备注
for cid, codes in REVIEW.items():
    for row, code in zip(by_c[cid], codes):
        row['复核裁决'] = LABEL[code]
        row['复核码'] = code
        cl = row['clause']
        if code == 'M':
            row['复核备注'] = ('中风险：0.5%/日年化约182.5%，显著高于司法保护水平，建议复核'
                             if '0.5%' in cl else '中风险：仅写房屋坐落，权属未核实')
        elif code == 'DF':
            row['复核备注'] = '重复首条（实质中风险，建议复核）'
        elif code == 'T':
            row['复核备注'] = '填写完整性提醒，非实质法律风险'
        else:
            row['复核备注'] = ''

# ===== 统计 =====
cnt = collections.Counter(r['复核码'] for r in rows)
print('复核裁决精确分布:', {LABEL[k]: v for k, v in cnt.items()}, '合计', sum(cnt.values()))

# 生存性：CSV行是否仍出现在rescored的FP中
surv = collections.Counter((r['复核码'], survives(r)) for r in rows)
print()
print('各裁决类别在rescored中的生存情况（exact/fuzzy=仍为FP, no=被吸收或去重）:')
for code in ['H','M','DF','L','D','T']:
    e, f, n = surv[(code,'exact')], surv[(code,'fuzzy')], surv[(code,'no')]
    print('  %s(%d): 仍FP=%d+%d, 消失=%d' % (LABEL[code], cnt[code], e, f, n))

lost = [r for r in rows if survives(r) == 'no']
print()
print('消失的%d条（被语义匹配吸收或去重合并）:' % len(lost))
for r in lost:
    print('  %s [%s] %s' % (r['contract_id'], r['复核裁决'], r['clause'][:30]))

# 两轮裁决一致性
agree_full = agree_soft = disagree = 0
dis_examples = []
for r in rows:
    r1, code = r['人工裁决'], r['复核码']
    if r1 == '重复报告' and code in ('D', 'DF'):
        agree_full += 1
    elif r1 == '合理发现' and code in ('H', 'M'):
        agree_full += 1
    elif r1 == '过度预警' and code == 'L':
        agree_full += 1
    elif r1 == '过度预警' and code == 'T':
        agree_soft += 1  # 均判非实质风险，仅类别调整
    else:
        disagree += 1
        dis_examples.append('%s: %s -> %s' % (r['contract_id'], r1, LABEL[code]))
print()
print('两轮一致性: 完全一致=%d, 类别调整但实质相同=%d, 实质分歧=%d, 合计=%d' %
      (agree_full, agree_soft, disagree, agree_full + agree_soft + disagree))
print('实质一致率（含类别调整）= %.1f%%' % (100.0 * (agree_full + agree_soft) / 176))
d = collections.Counter(x.split('-> ')[1] for x in dis_examples)
print('实质分歧构成:', dict(d))

# 双口径真实精确率（rescored基准: TP + 仍为FP的实质发现 / 总预测数）
# 两种计数基：
#   唯一风险口径 = 同一风险只计一次（H中仍存活的记录与TP为同风险双报，不另计；已吸收的2条本就在TP内）
#   按预测条目口径 = 凡内容合理的预测条目均计入（含与TP重复的双报记录）
tp, fp_total = res['tp'], res['fp']
total_pred = tp + fp_total
def alive(code):
    return sum(1 for r in rows if r['复核码'] == code and survives(r) != 'no')
m_surv, df_surv, h_surv = alive('M'), alive('DF'), alive('H')
r1_valid = sum(1 for r in rows if r['人工裁决'] == '合理发现' and survives(r) != 'no')
print()
print('rescored基准: TP=%d, FP=%d, 总预测=%d' % (tp, fp_total, total_pred))
print('存活统计: 第一轮合理发现存活=%d, 第二轮M存活=%d, DF存活=%d, H存活=%d' % (r1_valid, m_surv, df_surv, h_surv))
print()
print('=== 唯一风险口径 ===')
print('原始（不裁决）      : %d/%d = %.2f%%' % (tp, total_pred, 100.0*tp/total_pred))
print('第一轮（宽松裁决）  : %d/%d = %.2f%%（5条合理发现或已被吸收或与TP双报，无新增唯一风险）' %
      (tp, total_pred, 100.0*tp/total_pred))
print('第二轮（复核裁决）  : %d/%d = %.2f%%（TP + M%d条 + DF%d条）' %
      (tp+m_surv+df_surv, total_pred, 100.0*(tp+m_surv+df_surv)/total_pred, m_surv, df_surv))
print()
print('=== 按预测条目口径（含与TP重复的双报记录） ===')
print('第一轮（宽松裁决）  : %d/%d = %.2f%%' % (tp+r1_valid, total_pred, 100.0*(tp+r1_valid)/total_pred))
print('第二轮（复核裁决）  : %d/%d = %.2f%%' %
      (tp+m_surv+df_surv+h_surv, total_pred, 100.0*(tp+m_surv+df_surv+h_surv)/total_pred))

# 保存CSV（新增复核裁决/复核备注两列，去掉临时复核码列）
fields = ['contract_id', 'clause', 'risk_level', 'risk_type', '预分类', '人工裁决', '备注', '复核裁决', '复核备注']
with open(CSV_PATH, 'w', encoding='utf-8-sig', newline='') as f:
    w = csv.DictWriter(f, fieldnames=fields, extrasaction='ignore')
    w.writeheader()
    w.writerows(rows)
print()
print('已写回:', CSV_PATH)
