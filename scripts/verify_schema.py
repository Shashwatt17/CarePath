"""Run rollback-only schema checks against migrated PostgreSQL. Requires psycopg 3."""
import os
from pathlib import Path
import psycopg
root = Path(__file__).resolve().parents[1]
config = {}
env_file = root / '.env'
if env_file.exists():
    for line in env_file.read_text().splitlines():
        if line.strip() and not line.lstrip().startswith('#') and '=' in line:
            key, value = line.split('=', 1)
            config[key.strip()] = value.strip()
config.update(os.environ)
uri = config.get('DATABASE_TEST_URL')
kwargs = {} if uri else dict(host='localhost', port=config.get('POSTGRES_PORT','5432'),
    dbname=config.get('POSTGRES_DB','carepath'), user=config.get('POSTGRES_USER','carepath'),
    password=config.get('POSTGRES_PASSWORD',''))
with psycopg.connect(uri or '', autocommit=True, **kwargs) as connection:
    with connection.cursor() as cursor:
        try:
            for name in ['schema-invariants.sql', 'auth-schema-invariants.sql', 'vault-schema-invariants.sql', 'extraction-schema-invariants.sql', 'review-schema-invariants.sql', 'assistant-schema-invariants.sql']:
                cursor.execute((root / 'scripts' / name).read_text())
                while cursor.nextset():
                    pass
            print('PASS: 40 rollback-only schema invariants')
        finally:
            connection.execute('ROLLBACK')
