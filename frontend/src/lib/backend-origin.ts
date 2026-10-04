/** Server configuration only; errors intentionally omit the supplied value. */
export function backendOrigin(value: string): string {
 try {
  const url = new URL(value);
  if (!["http:", "https:"].includes(url.protocol) || url.username || url.password || url.search || url.hash || (url.pathname !== "/" && url.pathname !== "")) throw new Error();
  return url.origin;
 } catch { throw new Error("BACKEND_URL must be an HTTP(S) origin without credentials, path, query or fragment."); }
}
