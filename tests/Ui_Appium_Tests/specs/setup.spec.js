const assert = require('node:assert/strict');
const {
  configureTournament,
  expectText,
  expectTextAbsent,
  expectTextContains,
  fillPlayers,
  resetApp,
  startTournament,
  tapText,
  toastText,
  uiText,
  visibleEditTexts
} = require('../helpers/app');

describe('Feature 01 — Tournament setup screen', () => {
  beforeEach(async () => {
    await resetApp();
  });

  it('CT-01-001 — Display a fresh tournament setup screen', async () => {
    await expectText('Čkiletova tabla');
    await expectText('Create a tournament. Add 2–8 contestants to begin.');
    const fields = await visibleEditTexts();
    assert.equal(fields.length, 2);
    assert.equal(await fields[0].getText(), 'Contestant 1');
    assert.equal(await fields[1].getText(), 'Contestant 2');
    await expectText('Select tournament format  (required)');
    await expectTextAbsent('League only');
    await expectTextAbsent('League + knockout');
  });

  it('CT-01-002 — Add contestants and start a league tournament', async () => {
    await startTournament({ players: ['Ana', 'Bruno'], type: 'league', matches: 2 });
    await expectText('League table');
    await expectTextContains('Ana');
    await expectTextContains('Bruno');
  });

  it('CT-01-003 — Add contestant fields up to the supported maximum', async () => {
    for (let index = 3; index <= 8; index += 1) await tapText('＋  Add contestant');
    for (let index = 1; index <= 8; index += 1) {
      const source = await driver.getPageSource();
      assert.ok(source.includes(`Contestant ${index}`));
    }
    await tapText('＋  Add contestant');
    await toastText('Maximum 8 contestants');
    assert.equal((await driver.getPageSource()).includes('Contestant 9'), false);
  });

  it('CT-01-004 — Assign an optional football team', async () => {
    await fillPlayers(['Ana', 'Bruno']);
    const assignButtons = await $$(uiText('＋ Assign team'));
    assert.ok(assignButtons.length >= 2);
    await assignButtons[0].click();
    const teamField = (await visibleEditTexts())[0];
    await teamField.setValue('Dinamo Zagreb');
    await driver.hideKeyboard().catch(() => {});
    await tapText('Save');
    await expectText('Team: Dinamo Zagreb');
    await configureTournament({ type: 'league', matches: 2 });
    await tapText('CONFIRM CONTESTANTS');
    await expectTextContains('Ana');
    await expectTextContains('Dinamo Zagreb');
  });

  it('CT-01-005 — Prevent starting without mandatory tournament settings', async () => {
    await fillPlayers(['Ana', 'Bruno']);
    await tapText('CONFIRM CONTESTANTS');
    await toastText('Select the required tournament format and number of matches');
    await expectTextAbsent('League table');
    await tapText('Select tournament format  (required)');
    await tapText('League only');
    await tapText('Save');
    await browser.waitUntil(
      async () => (await driver.getPageSource()).includes('Enter a number from 1 to 100'),
      { timeout: 5000, timeoutMsg: 'Expected required match-count validation' }
    );
    await expectText('Tournament format');
  });

  it('CT-01-006 — Configure a league and knockout tournament', async () => {
    await fillPlayers(['Ana', 'Bruno', 'Carla', 'David', 'Eva', 'Filip']);
    await configureTournament({ type: 'knockout', matches: 2, qualifiers: 4 });
    await expectTextContains('Tournament: League + knockout');
    await expectTextContains('2 league matches/player');
    await expectTextContains('Top 4');
    await tapText('CONFIRM CONTESTANTS');
    await expectText('League table');
    await expectTextContains('League stage');
  });

  it('CT-01-007 — Display application information', async () => {
    const info = await $('~Information');
    await info.waitForDisplayed();
    await info.click();
    await expectText('Čkiletova tabla');
    await expectTextContains('Author: Vilim Hlusicka (vilim.hlusicka@gmail.com)');
    await expectTextContains('Version: 1.1.5');
    await expectTextContains('Changes in 1.1.5:');
    await expectTextContains('Added a version changelog.');
    await tapText('OK');
    await expectTextAbsent('Changes in 1.1.5:');
  });
});
