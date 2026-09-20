const { spawn } = require('node:child_process');
const path = require('node:path');
const { startRun, printSummary } = require('../helpers/test-report');

const suiteRoot = path.resolve(__dirname, '..');
const wdio = path.join(suiteRoot, 'node_modules/@wdio/cli/bin/wdio.js');

startRun();
const runner = spawn(
  process.execPath,
  [wdio, 'run', path.join(suiteRoot, 'wdio.conf.js'), '--autoCompileOpts.autoCompile=false', ...process.argv.slice(2)],
  {
    cwd: suiteRoot,
    stdio: 'inherit',
    env: { ...process.env, DEBELA_TEST_WRAPPED: '1' }
  }
);

let finished = false;
function finish(exitCode, error) {
  if (finished) return;
  finished = true;
  if (error) console.error(error);
  try {
    printSummary();
  } catch (summaryError) {
    console.error(summaryError);
    exitCode = 1;
  }
  process.exitCode = exitCode;
}

runner.on('error', (error) => finish(1, error));
runner.on('close', (code) => finish(code ?? 1));
