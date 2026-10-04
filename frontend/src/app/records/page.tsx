import { RecordsVault } from "@/components/vault/records-vault";
export default async function RecordsPage({ searchParams }: { searchParams: Promise<{ deleted?: string }> }) {
  const { deleted } = await searchParams;
  return <>{deleted && <p role="status" className="mb-6 rounded-lg bg-muted p-4 text-sm">{deleted === "complete" ? "Document and stored original deleted." : "Document removed from your account. Stored-file deletion is queued for retry."}</p>}<RecordsVault /></>;
}
