import test from 'node:test';
import assert from 'node:assert/strict';
import { filterReadings, readingNeedsCheck } from './report-overview.ts';
import type { Candidate } from './extraction-client.ts';
const candidate = (patch: Partial<Candidate> = {}): Candidate => ({ id: 'one', originalTestName: 'Glucose', originalValue: '97.6', originalUnit: 'mg/dL', numericValue: 97.6, referenceLower: 70, referenceUpper: 140, referenceText: '70-140', abnormalFlag: null, reportDate: '2026-07-18', providerName: 'Example Lab', confidence: 'HIGH', reasons: [], source: { page: 1, start: 0, end: 1, text: 'x', method: 'PDFBOX_TEXT', box: null }, ...patch });
test('search preserves same-name historical readings and original punctuation', () => {
  const rows = [candidate(), candidate({ id: 'two', reportDate: '2025-09-12', originalValue: '2:22', numericValue: null })];
  assert.equal(filterReadings(rows, 'glucose', false).length, 2);
  assert.equal(filterReadings(rows, '2025', false)[0].originalValue, '2:22');
});
test('quality filter does not interpret source abnormality as extraction failure', () => {
  assert.equal(readingNeedsCheck(candidate({ abnormalFlag: 'HIGH' })), false);
  assert.equal(readingNeedsCheck(candidate({ originalUnit: null })), true);
  assert.equal(readingNeedsCheck(candidate({ reasons: ['AMBIGUOUS_NUMERIC_PUNCTUATION'] })), true);
  assert.equal(filterReadings([candidate(), candidate({ id: 'two', confidence: 'LOW' })], '', true).length, 1);
});
