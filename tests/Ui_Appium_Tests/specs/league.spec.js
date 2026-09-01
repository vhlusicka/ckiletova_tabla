const assert = require('node:assert/strict');
const {
  PLAYERS_4,
  PLAYERS_6,
  currentLeagueSides,
  currentMatchPlayers,
  enterVisibleScores,
  expectText,
  expectTextContains,
  finishLeague,
  playNext,
  relaunchApp,
  resetApp,
  setupLeague,
  startTournament,
  tapElement,
  tapText,
  tapTextContains,
  uiText,
  visibleEditTexts
} = require('../helpers/app');

async function rowValues(contestant) {
  const name = await $(
    `android=new UiSelector().className("android.widget.TextView").textContains(${JSON.stringify(contestant)})`
  );
  await name.waitForDisplayed();
  const row = await name.$('..');
  const cells = await row.$$('android.widget.TextView');
  return Promise.all(cells.map((cell) => cell.getText()));
}

async function openNextLeagueGame() {
  await tapTextContains('PLAY GAME');
  await expectText('HOME');
  await expectText('VS');
  await expectText('AWAY');
}

describe('Feature 02 — League stage', () => {
  beforeEach(async () => {
    await setupLeague();
  });

  it('CT-02-001 — Record a league match result', async () => {
    await tapText('PLAY GAME 1  ›');
    await expectText('HOME');
    await expectText('VS');
    await expectText('AWAY');
    await enterVisibleScores(2, 1);
    await tapText('FINISH MATCH');
    await expectTextContains('2 – 1');
    await tapText('Confirm');
    await expectText('League table');
    await expectTextContains('1 league games played');
  });

  it('CT-02-002 — Calculate league standings correctly', async () => {
    await openNextLeagueGame();
    const first = await currentLeagueSides();
    await enterVisibleScores(2, 1);
    await tapText('FINISH MATCH');
    await tapText('Confirm');
    const homeRow = await rowValues(first.home);
    const awayRow = await rowValues(first.away);
    assert.deepEqual(homeRow.slice(1, 6), ['3', '1', '1', '0', '0']);
    assert.deepEqual(awayRow.slice(1, 6), ['0', '1', '0', '0', '1']);
    assert.equal(homeRow[6], '+1');
    assert.equal(awayRow[6], '-1');

    await openNextLeagueGame();
    const second = await currentLeagueSides();
    const beforeHome = await rowValues(second.home).catch(() => null);
    await enterVisibleScores(1, 1);
    await tapText('FINISH MATCH');
    await tapText('Confirm');
    const afterHome = await rowValues(second.home);
    const afterAway = await rowValues(second.away);
    assert.equal(Number(afterHome[2]), Number(beforeHome?.[2] || 0) + 1);
    assert.ok(Number(afterHome[4]) >= 1);
    assert.ok(Number(afterAway[4]) >= 1);
  });

  it('CT-02-003 — Schedule contestants without excessive waiting', async () => {
    await resetApp();
    await startTournament({ players: PLAYERS_6, type: 'league', matches: 2 });
    const scheduled = [];
    for (let game = 1; game <= 3; game += 1) {
      await tapText(`PLAY GAME ${game}  ›`);
      scheduled.push(...(await currentMatchPlayers(PLAYERS_6)));
      if (game < 3) {
        await enterVisibleScores(1, 0);
        await tapText('FINISH MATCH');
        await tapText('Confirm');
      }
    }
    assert.equal(new Set(scheduled).size, 6);
    assert.equal(scheduled.length, 6);
  });

  it('CT-02-004 — Maintain fair home and away scheduling', async () => {
    const homeCount = Object.fromEntries(PLAYERS_4.map((name) => [name, 0]));
    const awayCount = Object.fromEntries(PLAYERS_4.map((name) => [name, 0]));
    while (!(await $(uiText('Export results...'))).isExisting()) {
      await openNextLeagueGame();
      const sides = await currentLeagueSides();
      homeCount[sides.home] += 1;
      awayCount[sides.away] += 1;
      await enterVisibleScores(1, 0);
      await tapText('FINISH MATCH');
      await tapText('Confirm');
    }
    for (const player of PLAYERS_4) {
      const values = await rowValues(player);
      assert.equal(values[2], '2');
      assert.ok(Math.abs(homeCount[player] - awayCount[player]) <= 1);
    }
  });

  it('CT-02-005 — View contestant and complete match histories', async () => {
    await openNextLeagueGame();
    const sides = await currentLeagueSides();
    await enterVisibleScores(2, 1);
    await tapText('FINISH MATCH');
    await tapText('Confirm');
    await tapTextContains(sides.home);
    await expectTextContains('LEAGUE');
    await expectTextContains('HOME / AWAY');
    await expectTextContains('2 – 1');
    await tapText('‹  Back to table');
    await tapText('All matches');
    await expectTextContains('GAME 1');
    await expectTextContains('LEAGUE');
    await expectTextContains('2 – 1');
  });

  it('CT-02-006 — Correct the latest match result', async () => {
    await playNext({ first: 2, second: 1 });
    await playNext({ first: 1, second: 0 });
    await tapText('All matches');
    const editButtons = await $$(uiText('Edit latest result'));
    assert.equal(editButtons.length, 1);
    await tapElement(editButtons[0]);
    const fields = await visibleEditTexts();
    await fields[0].clearValue();
    await fields[0].setValue('3');
    await fields[1].clearValue();
    await fields[1].setValue('0');
    await driver.hideKeyboard().catch(() => {});
    await tapText('SAVE CORRECTION');
    await tapText('Confirm');
    await tapText('All matches');
    await expectTextContains('GAME 2');
    await expectTextContains('3 – 0');
  });

  it('CT-02-007 — Finish a league-only tournament', async () => {
    await finishLeague();
    const finished = await $(uiText('Export results...'));
    await finished.waitForDisplayed();
    assert.equal(await finished.isEnabled(), true);
    const firstDataRow = await $('//android.widget.LinearLayout/android.widget.TextView[contains(@text,"1  ")]');
    assert.equal(await firstDataRow.isDisplayed(), true);
  });

  it('CT-02-008 — Persist an active tournament after reopening the app', async () => {
    await playNext({ first: 2, second: 1 });
    await tapTextContains('PLAY GAME');
    const expectedFixture = await currentMatchPlayers(PLAYERS_4);
    await tapText('Cancel');
    await relaunchApp();
    await expectText('League table');
    await expectTextContains('1 league games played');
    await tapText('All matches');
    await expectTextContains('2 – 1');
    await tapText('‹  Back to table');
    await tapTextContains('PLAY GAME');
    assert.deepEqual((await currentMatchPlayers(PLAYERS_4)).sort(), expectedFixture.sort());
  });

  it('CT-02-009 — Reset the tournament with countdown protection', async () => {
    await tapText('Reset tournament');
    await expectText('Reset the whole tournament?');
    await expectTextContains('Please wait 5 seconds.');
    const initial = await $(uiText('YES (5)'));
    await initial.waitForDisplayed();
    assert.equal(await initial.isEnabled(), false);
    const reset = await $(uiText('YES, RESET'));
    await reset.waitForEnabled({ timeout: 8000 });
    await tapElement(reset);
    await expectText('Čkiletova tabla');
    await expectText('Select tournament format  (required)');
    assert.equal((await visibleEditTexts()).length, 2);
  });
});
