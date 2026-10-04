"use client";
import { Button } from "@/components/ui/button";
export default function ErrorPage({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return <main id="main" className="mx-auto max-w-xl px-6 py-24"><h1 className="text-3xl font-semibold">CarePath couldn’t load this page.</h1><p className="my-6 text-muted-foreground">Please try again. No medical information is shown in this error.</p><Button onClick={reset}>Try again</Button></main>;
}
