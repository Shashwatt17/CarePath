"""Additional Phase 5 demo, without changing the Phase 4 corpus. Requires reportlab."""
from pathlib import Path
from reportlab.pdfgen import canvas
from reportlab.lib.pagesizes import letter
root = Path(__file__).resolve().parents[1]
output = root / 'sample-data/phase5/normalization-lab.pdf'
output.parent.mkdir(parents=True, exist_ok=True)
c = canvas.Canvas(str(output), pagesize=letter, invariant=1)
c.setTitle('SYNTHETIC DEMO DATA - NOT A REAL PATIENT')
lines = ['SYNTHETIC DEMO DATA - NOT A REAL PATIENT', 'LAB REPORT',
         'Laboratory: CarePath Synthetic Laboratory', 'Report date: 2026-09-15', '',
         'Test | Result | Unit | Reference | Flag',
         'Hb | 104 | g/L | 120 - 160 | LOW',
         'Glucose | 90 | mg/dL | 70 - 100 |',
         'Sodium | 140 | mg/dL | 135 - 145 |', '',
         'This fixture deliberately includes an unsupported unit.',
         'The numbers and ranges are synthetic test data only.']
c.setFont('Courier', 10)
for index, line in enumerate(lines): c.drawString(36, 748 - index * 27, line)
c.setFont('Helvetica', 9); c.drawString(36, 34, 'Synthetic fixture only | Page 1')
c.save()
print(output)
