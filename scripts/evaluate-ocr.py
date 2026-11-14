"""Run synthetic OCR evaluation locally; no app credentials, uploads or external OCR APIs."""
from pathlib import Path
import argparse, hashlib, json, os, platform, subprocess

root = Path(__file__).resolve().parents[1]
p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--java', default=str(Path(os.environ['JAVA_HOME'])/'bin/java') if os.environ.get('JAVA_HOME') else 'java')
p.add_argument('--maven', default='mvn')
p.add_argument('--classpath-file', type=Path, help='Reuse an existing test dependency classpath to avoid resolution')
p.add_argument('--compiled', action='store_true', help='Use already compiled test classes')
p.add_argument('--output', type=Path, default=root/'backend/target/ocr-evaluation')
p.add_argument('--tesseract', default='tesseract')
a = p.parse_args()
if not a.compiled:
    subprocess.run([a.maven, '-f', str(root/'backend/pom.xml'), '-DskipTests', 'test-compile'], check=True)
classpath_file = a.classpath_file
if classpath_file is None:
    classpath_file = root/'backend/target/ocr-evaluation-classpath.txt'
    subprocess.run([a.maven, '-f', str(root/'backend/pom.xml'), 'dependency:build-classpath', '-DincludeScope=test', '-Dmdep.outputFile='+str(classpath_file)], check=True)
classpath = os.pathsep.join([str(root/'backend/target/test-classes'), str(root/'backend/target/classes'), classpath_file.read_text().strip()])
subprocess.run([a.java, '-Xmx128m', '-XX:+UseSerialGC', '-Djava.awt.headless=true', '-cp', classpath,
               'com.carepath.intelligence.OcrEvaluation', str(root/'sample-data/ocr-evaluation/manifest.json'),
               str(a.output.resolve()), a.tesseract], check=True, timeout=600)
report_path = a.output.resolve()/'report.json'
report = json.loads(report_path.read_text())
report['environment'] = {
    'os': platform.system(), 'architecture': platform.machine(),
    'tesseract': subprocess.run([a.tesseract, '--version'], capture_output=True, text=True, check=True, timeout=10).stdout.splitlines()[0],
    'renderFont': 'Java logical SansSerif plain; host font substitution can change OCR output',
    'manifestSha256': hashlib.sha256((root/'sample-data/ocr-evaluation/manifest.json').read_bytes()).hexdigest(),
}
report_path.write_text(json.dumps(report, indent=2)+'\n')
print('Saved evaluation:', report_path)
