const assert = require('node:assert/strict');
const {
  expectText,
  expectTextAbsent,
  finishLeague,
  setupLeague,
  tapText,
  uiText
} = require('../helpers/app');

async function completeLeague() {
  await setupLeague();
  await finishLeague();
  const exportButton = await $(uiText('Export results...'));
  await exportButton.waitForDisplayed();
  return exportButton;
}

describe('Feature 04 — Results export', () => {
  it('CT-04-001 — Offer export only after tournament completion', async () => {
    await setupLeague();
    await expectTextAbsent('Export results...');
    await finishLeague();
    const exportButton = await $(uiText('Export results...'));
    await exportButton.waitForDisplayed();
    assert.equal(await exportButton.isEnabled(), true);
  });

  it('CT-04-002 — Share the completed tournament workbook', async () => {
    await completeLeague();
    await tapText('Export results...');
    await browser.waitUntil(
      async () => {
        const source = await driver.getPageSource();
        return source.includes('Share tournament results') && source.includes('Ckiletova-tabla-results.xlsx');
      },
      { timeout: 10000, timeoutMsg: 'Expected Android chooser with the Excel attachment' }
    );
    await driver.back();
    await expectText('League table');
  });
});
