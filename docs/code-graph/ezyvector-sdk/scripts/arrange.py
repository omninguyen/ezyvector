"""STRUCTURE ONLY. Reads ast-raw.json, writes data.json (nodes+edges+degrees).
Never reads or writes curation.json. Layer comes from the lane rule the human
confirmed in Step 1 (role lanes 0-6), applied mechanically by package."""
import json, sys, collections
from pathlib import Path
raw = json.load(open(sys.argv[1])); out = Path(sys.argv[2])
PREFIX = 'org/youngmonkeys/ezyvector/'
CORE = {'hnsw/HnswIndex.java', 'storage/EzyVectorFileStorage.java', 'service/EzyVectorService.java'}
LANE = {'constant': 0, 'service': 1, 'converter': 2, 'repo': 3, 'pagination': 4,
        'entity': 5, 'model': 5, 'result': 5, 'hnsw': 6, 'storage': 6}
N = {n['id']: n for n in raw['nodes']}
rel = lambda sf: sf[len(PREFIX):] if sf.startswith(PREFIX) else sf
files = {n['id']: n for n in raw['nodes'] if n['label'].endswith('.java')}
file_by_src = {n['source_file']: nid for nid, n in files.items()}
kind, parent = {}, {}
for e in raw['edges']:
    if e['relation'] in ('contains', 'method', 'case_of'):
        kind[e['target']] = e['relation']; parent[e['target']] = e['source']

def owner_name(nid):
    """Dotted class path of a class node inside its file (Node, HnswIndex...)."""
    return N[nid]['label']

def rep(nid):
    n = N.get(nid)
    if not n or not n.get('source_file'):
        return None                       # external lib / annotation: noise
    sf = n['source_file']; f = rel(sf); fid = file_by_src[sf]
    if nid == fid: return fid
    k = kind.get(nid)
    if k == 'contains':
        return fid if n['label'] + '.java' == f.split('/')[-1] else nid
    if k == 'method':
        return nid if f in CORE else rep(parent[nid])
    if k == 'case_of':
        return rep(parent[nid])
    return fid

def make_node(rid):
    n = N[rid]; f = rel(n['source_file']); cls = f.split('/')[-1][:-5]
    if rid in files:
        return dict(id=rid, key=f, label=cls, kind='class', file=f, line=1)
    k = kind[rid]
    if k == 'contains':   # inner class
        return dict(id=rid, key=f + '#' + n['label'], label=cls + '.' + n['label'],
                    kind='inner', file=f, line=int(n['source_location'][1:]))
    p = parent[rid]; pl = N[p]['label']
    own = cls if pl == cls else pl          # method of top class or of inner class
    m = n['label'].lstrip('.')
    lbl = (cls + '.' + m) if own == cls else (cls + '.' + own + '.' + m)
    return dict(id=rid, key=f + '#' + (m if own == cls else own + '.' + m), label=lbl,
                kind='method', file=f, line=int(n['source_location'][1:]),
                owner=file_by_src[n['source_file']] if own == cls else p)

nodes = {}
for n in raw['nodes']:
    r = rep(n['id'])
    if r and r not in nodes: nodes[r] = make_node(r)
for nd in nodes.values():
    nd['layer'] = LANE[nd['file'].split('/')[0]]

E = {}; dropped = collections.Counter()
for e in raw['edges']:
    s, t = rep(e['source']), rep(e['target'])
    if not s or not t: dropped['external_endpoint:' + e['relation']] += 1; continue
    if s == t: dropped['self_after_collapse:' + e['relation']] += 1; continue
    key = (s, t, e['relation'])
    cur = E.setdefault(key, dict(s=s, t=t, r=e['relation'], n=0, inferred=False))
    cur['n'] += 1
    if e.get('confidence') == 'INFERRED': cur['inferred'] = True
edges = list(E.values())
deg_in, deg_out = collections.Counter(), collections.Counter()
for e in edges: deg_out[e['s']] += 1; deg_in[e['t']] += 1
for nid, nd in nodes.items(): nd['in'] = deg_in[nid]; nd['out'] = deg_out[nid]
# Violation rule (human-confirmed): lane number may only depend on a HIGHER lane number
# (drawn below it). Lane 0 is cross-cutting and exempt.
viol = []
for e in edges:
    a, b = nodes[e['s']]['layer'], nodes[e['t']]['layer']
    if a and b and a > b: e['viol'] = True; viol.append(e)
labels = collections.Counter(nd['label'] for nd in nodes.values())
json.dump(dict(nodes=sorted(nodes.values(), key=lambda x: (x['layer'], x['file'], x['line'])), edges=edges),
          open(out, 'w'), ensure_ascii=False, indent=1)
print('nodes', len(nodes), collections.Counter(nd['kind'] for nd in nodes.values()))
print('edges', len(edges), collections.Counter(e['r'] for e in edges))
print('per lane', sorted(collections.Counter(nd['layer'] for nd in nodes.values()).items()))
print('dropped', dict(dropped))
print('violations', len(viol), [(nodes[e['s']]['label'], nodes[e['t']]['label'], e['r']) for e in viol][:10])
print('label collisions', {l: c for l, c in labels.items() if c > 1})
print('zero in-degree (non-method)', [nd['label'] for nd in nodes.values() if nd['in'] == 0])
