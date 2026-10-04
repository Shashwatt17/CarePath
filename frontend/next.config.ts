import type { NextConfig } from "next";
import { config } from "dotenv";
import path from "node:path";
import { backendOrigin } from "./src/lib/backend-origin.ts";

config({ path: path.resolve(process.cwd(), "../.env"), quiet: true });
const nextConfig: NextConfig = {
  poweredByHeader: false,
  // Match the backend multipart envelope; Next defaults to a smaller 10 MiB proxy buffer.
  experimental: { proxyClientMaxBodySize: "21mb", proxyTimeout: 120000 },
  reactStrictMode: true,
  async rewrites() {
    const base = backendOrigin(process.env.BACKEND_URL ?? "http://localhost:8080");
    return [{ source: "/api/v1/:path*", destination: `${base}/api/v1/:path*` }];
  },
  async headers() {
    return [{ source: "/share", headers: [{key:"Cache-Control",value:"no-store"},{key:"X-Robots-Tag",value:"noindex, nofollow, noarchive"},{key:"Content-Security-Policy",value:"default-src 'self'; script-src 'self' 'unsafe-inline'; style-src 'self' 'unsafe-inline'; img-src 'self' data:; connect-src 'self'; object-src 'none'; base-uri 'none'; form-action 'none'; frame-ancestors 'none'"}] }, { source: "/:path*", headers: [
      { key: "X-Content-Type-Options", value: "nosniff" },
      { key: "X-Frame-Options", value: "DENY" },
      { key: "Referrer-Policy", value: "no-referrer" },
      { key: "Permissions-Policy", value: "camera=(), microphone=(), geolocation=(self)" },
    ] }];
  },
};
export default nextConfig;
