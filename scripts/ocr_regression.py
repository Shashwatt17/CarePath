"""Compare synthetic OCR runs without hiding regressions in aggregate totals."""
import json
import sys


def regressions(baseline, current):
    errors = []
    if baseline['corpusVersion'] != current['corpusVersion']:
        return ['Corpus version differs; explicit baseline review required']
    if baseline.get('environment', {}).get('manifestSha256') != current.get('environment', {}).get('manifestSha256'):
        return ['Manifest content differs; explicit baseline review required']
    old = {c['id']: c for c in baseline['cases']}
    new = {c['id']: c for c in current['cases']}
    if len(old) != len(baseline['cases']) or len(new) != len(current['cases']):
        return ['Duplicate case IDs']
    if old.keys() != new.keys():
        return ['Case set differs; explicit baseline review required']
    for key, before in old.items():
        after = new[key]
        if before['metrics']['expected'] != after['metrics']['expected']:
            errors.append(f'{key}: denominator changed')
        for field in ('matchedRows', 'exactValues', 'exactUnits', 'exactDates'):
            if after['metrics'][field] < before['metrics'][field]:
                errors.append(f'{key}: {field} decreased')
        for field in ('missingRows', 'extraRows'):
            if after['metrics'][field] > before['metrics'][field]:
                errors.append(f'{key}: {field} increased')
        for field in ('dateExact', 'providerExact'):
            if before[field] and not after[field]:
                errors.append(f'{key}: {field} regressed')
        if after.get('error') and after.get('error') != before.get('error'):
            errors.append(f'{key}: new processing error')
        if any(r.get('canAutoReplace') or not r.get('requiresReview') for r in after.get('retries', [])):
            errors.append(f'{key}: unsafe retry policy')
    return errors


if __name__ == '__main__':
    if len(sys.argv) != 3:
        sys.exit('Usage: python scripts/ocr_regression.py baseline.json current.json')
    with open(sys.argv[1]) as f:
        baseline = json.load(f)
    with open(sys.argv[2]) as f:
        current = json.load(f)
    problems = regressions(baseline, current)
    print('\n'.join(problems) if problems else 'No measured regression against baseline (not a clinical accuracy guarantee).')
    sys.exit(bool(problems))
