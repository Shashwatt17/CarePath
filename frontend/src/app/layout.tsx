import type { Metadata } from "next";
import "./globals.css";
export const metadata: Metadata = { title: "CarePath · Your health records", description: "Evidence-grounded longitudinal health record intelligence. Records, evidence and visit preparation." };
export default function RootLayout({ children }: Readonly<{ children: React.ReactNode }>) {
  return <html lang="en"><body><a className="skip-link" href="#main">Skip to content</a>{children}</body></html>;
}
