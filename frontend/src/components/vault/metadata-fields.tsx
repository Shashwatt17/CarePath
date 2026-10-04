"use client";
import { categories, label, type Metadata, type Category } from "@/lib/vault-client";
export const emptyMetadata: Metadata = { documentType: "OTHER", documentDate: null, providerName: null, tags: [] };
export function MetadataFields({ value, onChange, disabled = false }: { value: Metadata; onChange: (value: Metadata) => void; disabled?: boolean }) {
  return <fieldset disabled={disabled} className="grid gap-4 sm:grid-cols-2"><legend className="sr-only">Document metadata</legend>
    <label><span className="field-label">Category</span><select className="field-input" value={value.documentType} onChange={(e) => onChange({ ...value, documentType: e.target.value as Category })}>{categories.map((category) => <option key={category} value={category}>{label(category)}</option>)}</select></label>
    <label><span className="field-label">Document date <span className="font-normal">(optional)</span></span><input className="field-input" type="date" value={value.documentDate ?? ""} onChange={(e) => onChange({ ...value, documentDate: e.target.value || null })} /></label>
    <label><span className="field-label">Provider / lab <span className="font-normal">(optional)</span></span><input className="field-input" maxLength={255} value={value.providerName ?? ""} onChange={(e) => onChange({ ...value, providerName: e.target.value || null })} /></label>
    <label><span className="field-label">Tags <span className="font-normal">(comma separated)</span></span><input className="field-input" maxLength={491} value={value.tags.join(",")} onChange={(e) => onChange({ ...value, tags: e.target.value.split(",") })} /><span className="text-xs text-muted-foreground">Up to 12 tags, 40 characters each.</span></label>
  </fieldset>;
}
export function cleanMetadata(value: Metadata): Metadata { return { documentType: value.documentType, documentDate: value.documentDate, providerName: value.providerName?.trim() || null, tags: [...new Set(value.tags.map((t) => t.trim()).filter(Boolean))] }; }
