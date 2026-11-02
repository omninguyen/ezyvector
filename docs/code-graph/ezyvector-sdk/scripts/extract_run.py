import sys, json
from pathlib import Path
from graphify.extract import extract, collect_files
root = Path(sys.argv[1]).resolve()
cache = Path(sys.argv[2]).resolve()
out = Path(sys.argv[3])
files = collect_files(root, root=root)
allj = sorted(root.rglob('*.java'))
print("collected", len(files), "of", len(allj))
missing = sorted(set(allj) - set(Path(f).resolve() for f in files))
for m in missing: print("  NOT COLLECTED:", m.relative_to(root))
res = extract(allj, cache, root=root, parallel=False)
json.dump(res, open(out, 'w'), indent=1, default=str)
print({k: (len(v) if hasattr(v,'__len__') else v) for k,v in res.items()})
