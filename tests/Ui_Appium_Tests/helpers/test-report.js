const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const suiteRoot = path.resolve(__dirname, '..');
const projectRoot = path.resolve(suiteRoot, '../..');
const casePattern = /^(CT-\d{2}-\d{3})\s+[—–-]\s+(.+)$/;
let screenshotSequence = 0;

function startRun() {
  const reportDirectory = fs.mkdtempSync(path.join(os.tmpdir(), 'debela-tabla-tests-'));
  process.env.DEBELA_TEST_REPORT_FILE = path.join(reportDirectory, 'results.ndjson');
  process.env.DEBELA_TEST_RUN_ID = `${new Date().toISOString().replace(/[:.]/g, '-')}-${path.basename(reportDirectory)}`;
}

function caseDetails(testTitle) {
  const match = casePattern.exec(testTitle);
  return match
    ? { id: match[1], title: match[2] }
    : { id: 'UNKNOWN', title: testTitle };
}

function screenshotName(testTitle) {
  const { id, title } = caseDetails(testTitle);
  const safeTitle = title.normalize('NFKD').replace(/[^a-zA-Z0-9]+/g, '-').replace(/^-|-$/g, '').slice(0, 100);
  return `${id}-${safeTitle || 'test'}-${Date.now()}-${process.pid}-${++screenshotSequence}.png`;
}

async function recordTest(testTitle, passed, browser) {
  const reportFile = process.env.DEBELA_TEST_REPORT_FILE;
  if (!reportFile) throw new Error('Test report was not initialized');

  const entry = { ...caseDetails(testTitle), status: passed ? 'PASS' : 'FAIL' };
  if (!passed) {
    const screenshotDirectory = path.join(suiteRoot, 'screenshots', process.env.DEBELA_TEST_RUN_ID);
    const screenshotFile = path.join(screenshotDirectory, screenshotName(testTitle));
    try {
      fs.mkdirSync(screenshotDirectory, { recursive: true });
      await browser.saveScreenshot(screenshotFile);
      entry.screenshot = path.relative(projectRoot, screenshotFile);
      console.log(`Failure screenshot: ${entry.screenshot}`);
    } catch (error) {
      console.warn(`Could not capture a screenshot for ${entry.id}: ${error.message}`);
    }
  }

  fs.appendFileSync(reportFile, `${JSON.stringify(entry)}\n`);
}

function printSummary() {
  const reportFile = process.env.DEBELA_TEST_REPORT_FILE;
  if (!reportFile) return;

  try {
    const cases = new Map();
    if (fs.existsSync(reportFile)) {
      for (const line of fs.readFileSync(reportFile, 'utf8').split('\n').filter(Boolean)) {
        const entry = JSON.parse(line);
        const key = entry.id === 'UNKNOWN' ? entry.title : entry.id;
        const previous = cases.get(key);
        cases.set(key, {
          ...entry,
          status: previous?.status === 'FAIL' ? 'FAIL' : entry.status,
          screenshot: entry.screenshot || previous?.screenshot
        });
      }
    }

    const rows = [...cases.values()];
    const widths = [
      Math.max(2, ...rows.map((row) => row.id.length)),
      Math.max(5, ...rows.map((row) => row.title.length)),
      6
    ];
    const cells = (values) => `| ${values.map((value, index) => value.padEnd(widths[index])).join(' | ')} |`;
    const separator = `+-${widths.map((width) => '-'.repeat(width)).join('-+-')}-+`;

    const output = [''];
    for (const row of rows.filter((item) => item.screenshot)) {
      output.push(`${row.id} screenshot: ${row.screenshot}`);
    }
    if (output.length > 1) output.push('');
    output.push(`Test case results (${rows.length} executed)`);
    output.push(separator);
    output.push(cells(['ID', 'Title', 'Result']));
    output.push(separator);
    for (const row of rows) output.push(cells([row.id, row.title, row.status]));
    output.push(separator);
    fs.writeSync(1, `${output.join('\n')}\n`);
  } finally {
    fs.rmSync(path.dirname(reportFile), { recursive: true, force: true });
    delete process.env.DEBELA_TEST_REPORT_FILE;
    delete process.env.DEBELA_TEST_RUN_ID;
  }
}

module.exports = { startRun, recordTest, printSummary };
