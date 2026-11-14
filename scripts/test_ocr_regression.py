import copy
import unittest
from ocr_regression import regressions


class RegressionTest(unittest.TestCase):
    def setUp(self):
        self.baseline = {'corpusVersion': 'v1', 'cases': [{'id': 'one', 'metrics': {
            'expected': 2, 'matchedRows': 2, 'exactValues': 2, 'exactUnits': 2,
            'exactDates': 2, 'missingRows': 0, 'extraRows': 0},
            'dateExact': True, 'providerExact': True, 'error': None, 'retries': []}]}

    def test_unchanged(self):
        self.assertEqual([], regressions(self.baseline, copy.deepcopy(self.baseline)))

    def test_case_regression_and_unsafe_retry(self):
        current = copy.deepcopy(self.baseline)
        current['cases'][0]['metrics']['exactValues'] = 1
        current['cases'][0]['retries'] = [{'canAutoReplace': True, 'requiresReview': False}]
        self.assertEqual(2, len(regressions(self.baseline, current)))

    def test_missing_case_and_denominator_change(self):
        current = copy.deepcopy(self.baseline)
        current['cases'] = []
        self.assertTrue(regressions(self.baseline, current))
        current = copy.deepcopy(self.baseline)
        current['cases'][0]['metrics']['expected'] = 1
        self.assertIn('one: denominator changed', regressions(self.baseline, current))


if __name__ == '__main__':
    unittest.main()
