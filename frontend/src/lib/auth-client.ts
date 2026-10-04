export type User = { id: string; email: string; displayName: string };
export type Access = { accessToken: string; expiresIn: number; user: User };
export class ApiError extends Error {
  status: number;
  code: string;
  constructor(status: number, code: string, message: string) {
    super(message); this.status = status; this.code = code;
  }
}
function isUser(value: unknown): value is User {
  if (typeof value !== "object" || value === null) return false;
  const u = value as Partial<User>;
  return typeof u.id === "string" && typeof u.email === "string" && typeof u.displayName === "string";
}
function userResponse(value: unknown): User {
  if (!isUser(value)) throw new ApiError(502, "INVALID_RESPONSE", "CarePath returned an invalid account response.");
  return value;
}
function isAccess(value: unknown): value is Access {
  if (typeof value !== "object" || value === null) return false;
  const a = value as Partial<Access>;
  return typeof a.accessToken === "string" && a.accessToken.length > 0 && typeof a.expiresIn === "number"
    && a.expiresIn > 0 && !!a.user && typeof a.user.id === "string" && typeof a.user.email === "string" && typeof a.user.displayName === "string";
}
export function createAuthClient(transport: typeof fetch = (...args) => fetch(...args)) {
  let access: Access | null = null;
  let expiresAt = 0;
  let refreshing: Promise<Access | null> | null = null;
  let queue: Promise<unknown> = Promise.resolve();
  const listeners = new Set<(user: User | null) => void>();
  function set(value: Access | null) {
    access = value; expiresAt = value ? Date.now() + value.expiresIn * 1000 : 0;
    listeners.forEach((listener) => listener(value?.user ?? null));
  }
  // Per-tab serialization plus Web Locks across tabs when supported. No tokens in storage/channels.
  function locked<T>(operation: () => Promise<T>): Promise<T> {
    const run = async (): Promise<T> => {
      if (typeof navigator !== "undefined" && navigator.locks) return await navigator.locks.request("carepath-auth", operation);
      return await operation();
    };
    const result = queue.then(run, run); queue = result.catch(() => undefined); return result;
  }
  async function request(path: string, init: RequestInit = {}): Promise<unknown> {
    let response: Response;
    try {
      response = await transport(`/api/v1/auth/${path}`, { ...init, credentials: "same-origin", cache: "no-store", signal: AbortSignal.timeout(12000),
        headers: { "Content-Type": "application/json", "X-CarePath-Client": "web", ...init.headers } });
    } catch { throw new ApiError(0, "NETWORK_ERROR", "CarePath couldn’t connect. Please try again."); }
    if (!response.ok) {
      let code = "REQUEST_FAILED";
      try { const body: unknown = await response.json(); if (body && typeof body === "object" && "code" in body && typeof body.code === "string") code = body.code; } catch { /* No raw proxy/server errors shown. */ }
      const messages: Record<string, string> = {
        INVALID_CREDENTIALS: "Email or password is incorrect.",
        REGISTRATION_UNAVAILABLE: "Registration could not be completed. Try signing in or use different details.",
        VALIDATION_FAILED: "Check your details. Passwords must be 12–72 characters and no more than 72 UTF-8 bytes.",
        SESSION_INVALID: "Your session ended. Please sign in again.",
        RATE_LIMITED: "Too many attempts. Please wait before trying again.",
        ORIGIN_REJECTED: "CarePath could not verify this request. Check the configured site address.",
      };
      throw new ApiError(response.status, code, messages[code] ?? "CarePath could not complete the request. Please try again.");
    }
    return response.status === 204 || response.status === 201 ? null : response.json();
  }
  async function refresh(force = false): Promise<Access | null> {
    if (refreshing) return refreshing;
    refreshing = locked(async () => {
      if (!force && access && expiresAt > Date.now() + 15000) return access;
      try {
        const result = await request("refresh", { method: "POST" });
        if (!isAccess(result)) throw new ApiError(502, "INVALID_RESPONSE", "CarePath returned an invalid session response.");
        set(result); return result;
      } catch (error) {
        if (error instanceof ApiError && error.status === 401) { set(null); return null; }
        throw error;
      }
    }).finally(() => { refreshing = null; });
    return refreshing;
  }
  return {
    async api(path: string, init: RequestInit = {}): Promise<Response> {
      if (!/^\/api\/v1\/(?:activity|search|nearby-care|care|visit-packs|shares|documents|history|assistant|review\/candidates|observations|terminology\/concepts)(?:[/?]|$)/.test(path) || path.includes("..")) throw new Error("Invalid API path");
      let token = await refresh();
      if (!token) throw new ApiError(401, "SESSION_INVALID", "Please sign in again.");
      const send = () => transport(path, { ...init, credentials: "same-origin", cache: "no-store", signal: init.signal ?? AbortSignal.timeout(30000), headers: { ...init.headers, Authorization: `Bearer ${token!.accessToken}` } });
      let response = await send();
      if (response.status === 401) {
        token = await refresh(true);
        if (!token) throw new ApiError(401, "SESSION_INVALID", "Please sign in again.");
        response = await send();
      }
      return response;
    },
    async upload(body: FormData, progress: (value: number) => void): Promise<Response> {
      const token = await refresh();
      if (!token) throw new ApiError(401, "SESSION_INVALID", "Please sign in again.");
      return new Promise((resolve, reject) => {
        const xhr = new XMLHttpRequest(); xhr.open("POST", "/api/v1/documents"); xhr.timeout = 120000;
        xhr.setRequestHeader("Authorization", `Bearer ${token.accessToken}`);
        xhr.upload.onprogress = (e) => { if (e.lengthComputable) progress(Math.round(e.loaded / e.total * 100)); };
        xhr.onload = () => resolve(new Response(xhr.responseText, { status: xhr.status, headers: { "Content-Type": "application/json" } }));
        xhr.onerror = xhr.ontimeout = () => reject(new ApiError(0, "NETWORK_ERROR", "Upload could not be confirmed. Refresh the records list before retrying."));
        xhr.send(body);
      });
    },
    subscribe(listener: (user: User | null) => void) { listeners.add(listener); return () => { listeners.delete(listener); }; },
    currentUser: () => access?.user ?? null,
    clear: () => set(null),
    restore: () => refresh(),
    async register(email: string, password: string, displayName: string) {
      await locked(() => request("register", { method: "POST", body: JSON.stringify({ email, password, displayName }) }));
    },
    async login(email: string, password: string) {
      return locked(async () => {
        const result = await request("login", { method: "POST", body: JSON.stringify({ email, password }) });
        if (!isAccess(result)) throw new ApiError(502, "INVALID_RESPONSE", "CarePath returned an invalid session response.");
        set(result); return result.user;
      });
    },
    async logout() {
      // Preserve state on outage: do not pretend the server session was revoked.
      await locked(async () => { await request("logout", { method: "POST" }); set(null); });
    },
    async me(): Promise<User> {
      let token = await refresh();
      if (!token) throw new ApiError(401, "SESSION_INVALID", "Please sign in again.");
      try { return userResponse(await request("me", { headers: { Authorization: `Bearer ${token.accessToken}` } })); }
      catch (error) {
        if (!(error instanceof ApiError) || error.status !== 401) throw error;
        token = await refresh(true);
        if (!token) throw new ApiError(401, "SESSION_INVALID", "Please sign in again.");
        return userResponse(await request("me", { headers: { Authorization: `Bearer ${token.accessToken}` } }));
      }
    },
  };
}
export const authClient = createAuthClient();
