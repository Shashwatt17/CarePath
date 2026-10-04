"""Generate local-only RSA signing keys using OpenSSL. Refuses to overwrite existing keys."""
from pathlib import Path
import os
import subprocess
root = Path(__file__).resolve().parents[1]
directory = root / '.data' / 'keys'
directory.mkdir(parents=True, exist_ok=True)
private, public = directory / 'private.pem', directory / 'public.pem'
if private.exists() or public.exists():
    raise SystemExit('Key files already exist. Refusing to overwrite them.')
old_mask = os.umask(0o077)
try:
    subprocess.run(['openssl', 'genpkey', '-algorithm', 'RSA', '-pkeyopt', 'rsa_keygen_bits:3072', '-out', str(private)], check=True)
    subprocess.run(['openssl', 'pkey', '-in', str(private), '-pubout', '-out', str(public)], check=True)
finally:
    os.umask(old_mask)
print('Created signing keys under .data/keys. Do not commit or share the private key.')
