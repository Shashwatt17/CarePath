"use client";
import { createContext, useContext, useEffect, useState, useCallback } from "react";
import { authClient, type User } from "@/lib/auth-client";

type AuthState = { user: User | null; state: "loading" | "ready" | "error"; error: string; retry: () => void; notify: (type: "signed-in" | "signed-out") => void };
const Context = createContext<AuthState | null>(null);
export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [state, setState] = useState<AuthState["state"]>("loading");
  const [error, setError] = useState("");
  const [attempt, setAttempt] = useState(0);
  const retry = useCallback(() => { setState("loading"); setAttempt((n) => n + 1); }, []);
  const notify = useCallback((type: "signed-in" | "signed-out") => {
    if (typeof BroadcastChannel !== "undefined") { const c = new BroadcastChannel("carepath-session"); c.postMessage(type); c.close(); }
  }, []);
  useEffect(() => {
    let active = true;
    const unsubscribe = authClient.subscribe((value) => { if (active) { setUser(value); setState("ready"); } });
    authClient.restore().then(() => { if (active) { setUser(authClient.currentUser()); setState("ready"); } })
      .catch(() => { if (active) { setError("CarePath couldn’t check your session. Please retry."); setState("error"); } });
    const channel = typeof BroadcastChannel !== "undefined" ? new BroadcastChannel("carepath-session") : null;
    if (channel) channel.onmessage = (event: MessageEvent<unknown>) => {
      if (event.data === "signed-out") authClient.clear();
      if (event.data === "signed-in") { authClient.clear(); retry(); }
    };
    return () => { active = false; unsubscribe(); channel?.close(); };
  }, [attempt, retry]);
  return <Context.Provider value={{ user, state, error, retry, notify }}>{children}</Context.Provider>;
}
export function useAuth() { const context = useContext(Context); if (!context) throw new Error("AuthProvider is required"); return context; }
