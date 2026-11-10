# Supabase database trust anchor

`supabase-root-2021.crt` is the public CA certificate linked from Supabase's Database Settings, downloaded on 2026-11-10:
https://supabase-downloads.s3-ap-southeast-1.amazonaws.com/prod/ssl/prod-ca-2021.crt

SHA-256 certificate fingerprint:
807025AD50D4ED219D2C9C7D299C004F824EB00CF7F65AFEF607D07B72E6CAFA

The runtime Docker image imports this CA into Java's trust store. Keep JDBC `sslmode=verify-full` with `org.postgresql.ssl.DefaultJavaSSLFactory`; both certificate-chain and hostname verification remain enabled. This file contains no private key. Replace it when Supabase rotates its root CA (current expiry: 2031-04-26).
