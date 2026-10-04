"""Package reproducible source without credentials or build artifacts."""
import argparse
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
root = Path(__file__).resolve().parents[1]
parser = argparse.ArgumentParser()
parser.add_argument("--phase", default="phase13", choices=["phase1", "phase2", "phase3", "phase4", "phase5", "phase6", "phase7", "phase8", "phase9", "phase10", "phase11", "phase12", "phase13"])
args = parser.parse_args()
output = root.parent / f"carepath-{args.phase}.zip"
excluded = {".git", ".verification", "node_modules", ".next", "target", ".data", "__pycache__", ".idea", "coverage"}
with ZipFile(output, "w", ZIP_DEFLATED) as archive:
    for path in sorted(root.rglob("*")):
        if not path.is_file() or any(p in excluded for p in path.relative_to(root).parts):
            continue
        if path.name.startswith(".env") and path.name != ".env.example":
            continue
        if path.suffix in {".zip", ".tsbuildinfo", ".log", ".pem", ".key", ".p12", ".jks"}:
            continue
        archive.write(path, Path("carepath") / path.relative_to(root))
print(output)
