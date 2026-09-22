const fs = require('node:fs');
const os = require('node:os');
const path = require('node:path');

const suiteRoot = path.resolve(__dirname, '..');
const projectRoot = path.resolve(suiteRoot, '../..');
const defaultMarkdownReport = path.join(suiteRoot, 'test-results.md');
const casePattern = /^(CT-\d{2}-\d{3})\s+[—–-]\s+(.+)$/;
let screenshotSequence = 0;

function startRun() {
  const startedAt = new Date();
  const reportDirectory = fs.mkdtempSync(path.join(os.tmpdir(), 'debela-tabla-tests-'));
  process.env.DEBELA_TEST_REPORT_FILE = path.join(reportDirectory, 'results.ndjson');
  process.env.DEBELA_TEST_STARTED_AT = startedAt.toISOString();
  process.env.DEBELA_TEST_RUN_ID = `${startedAt.toISOString().replace(/[:.]/g, '-')}-${path.basename(reportDirectory)}`;
  process.env.DEBELA_TEST_RESULTS_FILE ||= defaultMarkdownReport;
}

function formatDuration(milliseconds) {
  const totalSeconds = Math.max(0, Math.round(milliseconds / 1000));
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  return [hours, minutes, seconds].map((value) => String(value).padStart(2, '0')).join(':');
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

function errorMessage(error) {
  if (!error) return '';
  return String(error.message || error).replace(/\x1b\[[0-9;]*m/g, '').replace(/\s+/g, ' ').trim();
}

async function recordTest(testTitle, passed, browser, error) {
  const reportFile = process.env.DEBELA_TEST_REPORT_FILE;
  if (!reportFile) throw new Error('Test report was not initialized');

  const entry = {
    ...caseDetails(testTitle),
    status: passed ? 'PASS' : 'FAIL',
    error: passed ? '' : errorMessage(error)
  };
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
          screenshot: entry.screenshot || previous?.screenshot,
          error: entry.error || previous?.error || ''
        });
      }
    }

    const rows = [...cases.values()];
    const markdownReport = path.resolve(process.env.DEBELA_TEST_RESULTS_FILE || defaultMarkdownReport);
    const endedAt = new Date();
    const startedAt = new Date(process.env.DEBELA_TEST_STARTED_AT || endedAt.toISOString());
    const duration = formatDuration(endedAt.getTime() - startedAt.getTime());
    const passed = rows.filter((row) => row.status === 'PASS').length;
    const failed = rows.filter((row) => row.status === 'FAIL').length;
    const escapeCell = (value) => String(value).replace(/\|/g, '\\|').replace(/\r?\n/g, ' ');
    const markdownRows = rows.map((row) => {
      let screenshot = '';
      if (row.screenshot) {
        const absoluteScreenshot = path.resolve(projectRoot, row.screenshot);
        const relativeScreenshot = path.relative(path.dirname(markdownReport), absoluteScreenshot).split(path.sep).join('/');
        screenshot = `[Open screenshot](${encodeURI(relativeScreenshot)})`;
      }
      return `| ${escapeCell(row.id)} | ${escapeCell(row.title)} | ${row.status} | ${escapeCell(row.error)} | ${screenshot} |`;
    });
    const markdown = [
      '# Appium test results',
      '',
      `- Run: ${process.env.DEBELA_TEST_RUN_ID || 'unknown'}`,
      `- Started: ${startedAt.toISOString()}`,
      `- Ended: ${endedAt.toISOString()}`,
      `- Total running time: ${duration}`,
      `- Executed: ${rows.length}`,
      `- Passed: ${passed}`,
      `- Failed: ${failed}`,
      '',
      '| ID | Title | Result | Error | Failure screenshot |',
      '|---|---|---|---|---|',
      ...markdownRows,
      ''
    ].join('\n');
    fs.mkdirSync(path.dirname(markdownReport), { recursive: true });
    fs.writeFileSync(markdownReport, markdown);

    const terminalError = (row) => row.error.length > 80 ? `${row.error.slice(0, 77)}...` : row.error;
    const widths = [
      Math.max(2, ...rows.map((row) => row.id.length)),
      Math.max(5, ...rows.map((row) => row.title.length)),
      6,
      Math.max(5, ...rows.map((row) => terminalError(row).length))
    ];
    const cells = (values) => `| ${values.map((value, index) => value.padEnd(widths[index])).join(' | ')} |`;
    const separator = `+-${widths.map((width) => '-'.repeat(width)).join('-+-')}-+`;

    const output = ['', `Markdown report: ${path.relative(projectRoot, markdownReport)}`, ''];
    for (const row of rows.filter((item) => item.screenshot)) {
      output.push(`${row.id} screenshot: ${row.screenshot}`);
    }
    if (output.length > 1) output.push('');
    output.push(`Test case results (${rows.length} executed)`);
    output.push(separator);
    output.push(cells(['ID', 'Title', 'Result', 'Error']));
    output.push(separator);
    for (const row of rows) output.push(cells([row.id, row.title, row.status, terminalError(row)]));
    output.push(separator);
    fs.writeSync(1, `${output.join('\n')}\n`);
  } finally {
    fs.rmSync(path.dirname(reportFile), { recursive: true, force: true });
    delete process.env.DEBELA_TEST_REPORT_FILE;
    delete process.env.DEBELA_TEST_RUN_ID;
    delete process.env.DEBELA_TEST_STARTED_AT;
  }
}

module.exports = { startRun, recordTest, printSummary };
