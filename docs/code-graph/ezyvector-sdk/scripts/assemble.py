"""Plain marker replace: template + data.json + curation.json + meta.json -> viewer.html.
Also runs the 7a data checks (orphans, uncurated, phantom files)."""
import json, sys, html
from pathlib import Path
tpl, data, cur, meta, src, out = sys.argv[1:7]
D = json.load(open(data)); C = json.load(open(cur)) if Path(cur).exists() else {}; M = json.load(open(meta))
keys = {n['key'] for n in D['nodes']}
orph = sorted(set(C) - keys)
unc = [n['key'] for n in D['nodes'] if not C.get(n['key'], {}).get('what')]
phantom = [n['key'] for n in D['nodes'] if not (Path(src) / n['file']).exists()]
missing_fields = [k for k in keys & set(C) for f in ('character','what','why') if not C[k].get(f)]
j = lambda o: json.dumps(o, ensure_ascii=False).replace('</', '<\\/')
meta_pub = {k: M[k] for k in ('intro', 'rule', 'curated_against') if k in M}
s = Path(tpl).read_text()
for mk, val in [('__TITLE__', html.escape(M['titulo'])), ('__SUBTITLE__', html.escape(M['contexto'])),
                ('__LAYERS__', j(M['layers'])), ('__DATA__', j(D)), ('__CURATION__', j(C)), ('__META__', j(meta_pub))]:
    assert mk in s, mk
    s = s.replace(mk, val)
Path(out).write_text(s)
print(f'viewer -> {out} ({len(s)//1024} KB)')
print(f'nodes={len(D["nodes"])} edges={len(D["edges"])} curated={len(keys)-len(unc)} uncurated={len(unc)} orphans={len(orph)} phantom={len(phantom)} missing_core_fields={len(missing_fields)}')
for o in orph: print('  ORPHAN', o)
for p in phantom: print('  PHANTOM', p)
for m in missing_fields[:10]: print('  MISSING FIELD', m)
issues = {}
for k, v in C.items():
    if v.get('issue'): issues.setdefault(v['issue'], []).append(k)
print('issues', {k: len(v) for k, v in issues.items()})
