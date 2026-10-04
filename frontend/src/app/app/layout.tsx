import { AuthProvider } from "@/components/auth/auth-provider";
import { ProtectedShell } from "@/components/auth/protected-shell";
export default function AppLayout({ children }: { children: React.ReactNode }) {
  return <AuthProvider><ProtectedShell>{children}</ProtectedShell></AuthProvider>;
}
