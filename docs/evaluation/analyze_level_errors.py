# -*- coding: utf-8 -*-
# 定级错误归因：复用 run_eval 的匹配逻辑重建 TP 对，列出真值vs预测等级分布
import sys, json, csv, collections

sys.path.insert(0, r'E:\study\毕业设计\docs\evaluation')
import run_eval as R

BASE = r'E:\study\毕业设计\docs\evaluation'
details = [json.loads(l) for l in open(BASE + r'\results\eval_details_v2.jsonl', encoding='utf-8')]
annotations = R.load_annotations()

pairs = []  # (合同, 条款类型, 真值等级, 预测等级)
for d in details:
    cid = d['contract_id']
    gt = annotations.get(cid, [])
    preds = R.dedupe_predictions(d['predictions'])
    matched_idx = set()
    for a in gt:
        best, best_j = None, -1
        for j, p in enumerate(preds):
            if j in matched_idx:
                continue
            sim = R.similarity(R.normalize(a['clause_text']), R.normalize(p['clause']))
            hit = sim >= 0.55 or R.risk_type_match(a['clause_type'], p['risk_type'], p['clause'])
            if hit and (best is None or sim > best):
                best, best_j = sim, j
        if best_j >= 0:
            matched_idx.add(best_j)
            pairs.append((cid, a['clause_type'], a['true_risk_level'], preds[best_j]['risk_level']))

print('TP匹配对: %d（报告口径29）' % len(pairs))
LEVELS = ['high', 'medium', 'low']
wrong = [(c, t, tr, pr) for c, t, tr, pr in pairs if tr != pr]
print('定级错误: %d 条' % len(wrong))
print()
print('%-5s %-14s %-8s %-8s %s' % ('合同', '条款类型', '真值', '预测', '偏差方向'))
for c, t, tr, pr in wrong:
    d = '偏高' if LEVELS.index(pr) < LEVELS.index(tr) else '偏低'
    print('%-6s %-16s %-9s %-9s %s' % (c, t, tr, pr, d))
print()
# 混淆矩阵
print('混淆矩阵（行=真值，列=预测）:')
print('%-8s %-6s %-8s %-6s %-5s' % ('', 'high', 'medium', 'low', '合计'))
for tr in LEVELS:
    row = [sum(1 for _, _, t2, p in pairs if t2 == tr and p == pl) for pl in LEVELS]
    print('%-8s %-6d %-8d %-6d %d' % (tr, row[0], row[1], row[2], sum(row)))
print()
# 按条款类型的定级正确率
by_type = collections.defaultdict(lambda: [0, 0])
for c, t, tr, pr in pairs:
    by_type[t][1] += 1
    if tr == pr:
        by_type[t][0] += 1
print('按条款类型:')
for t, (ok, n) in sorted(by_type.items(), key=lambda x: -x[1][1]):
    print('  %-14s %d/%d' % (t, ok, n))
