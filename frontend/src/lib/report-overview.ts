import type { Candidate } from './extraction-client.ts';

// Reading issues are extraction concerns, not clinical diagnoses or range interpretations.
export function readingNeedsCheck(c: Candidate): boolean {
  return c.confidence === 'LOW' || c.numericValue == null || !c.originalUnit ||
    c.reasons.includes('AMBIGUOUS_NUMERIC_PUNCTUATION');
}
export function filterReadings(candidates: Candidate[], query: string, checksOnly: boolean): Candidate[] {
  const term = query.trim().toLocaleLowerCase();
  return candidates.filter(c => (!checksOnly || readingNeedsCheck(c)) &&
    (!term || [c.originalTestName, c.originalValue, c.originalUnit, c.reportDate]
      .some(value => value?.toLocaleLowerCase().includes(term))));
}
