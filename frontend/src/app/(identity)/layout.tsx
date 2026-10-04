import Link from "next/link";
import { AuthProvider } from "@/components/auth/auth-provider";
export default function IdentityLayout({ children }: { children: React.ReactNode }) {
  return <AuthProvider><header className="mx-auto max-w-6xl px-6 py-7"><Link className="text-xl font-semibold" href="/">CarePath<span className="text-primary"> +</span></Link></header><main id="main" className="mx-auto max-w-md px-6 pb-16 pt-10">{children}</main></AuthProvider>;
}
