"""ADDITIVE-ONLY merge of hand-written curation batches into curation.json.
- Adds node keys that don't exist yet.
- For a key that exists, adds only fields that are missing/empty.
- NEVER overwrites a field that already holds content; reports conflicts instead.
- Stamps each newly added node with the file sha256 it was curated against.
Usage: merge_curation.py <curation.json> <src_root> <batch.json>..."""
import json, sys, hashlib
from pathlib import Path

cur_path = Path(sys.argv[1]); src_root = Path(sys.argv[2])
cur = json.loads(cur_path.read_text()) if cur_path.exists() else {}
added = kept = conflicts = 0
for b in sys.argv[3:]:
    for key, fields in json.loads(Path(b).read_text()).items():
        f = src_root / key.split('#')[0]
        sha = hashlib.sha256(f.read_bytes()).hexdigest()[:12] if f.exists() else None
        node = cur.setdefault(key, {})
        for k, v in fields.items():
            if node.get(k) in (None, '', []):
                node[k] = v; added += 1
            elif node[k] != v:
                conflicts += 1
                print(f'CONFLICT (kept existing): {key} :: {k}')
            else:
                kept += 1
        node.setdefault('curated_sha', sha)
tmp = cur_path.with_suffix('.tmp')
tmp.write_text(json.dumps(cur, ensure_ascii=False, indent=1))
tmp.replace(cur_path)
print(f'fields added={added} unchanged={kept} conflicts={conflicts} nodes={len(cur)}')
