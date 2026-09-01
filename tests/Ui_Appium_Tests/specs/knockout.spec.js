const assert = require('node:assert/strict');
const {
  PLAYERS_6,
  currentKnockoutContestants,
  currentMatchPlayers,
  enterVisibleScores,
  expectText,
  expectTextAbsent,
  expectTextContains,
  finishLeague,
  playNext,
  reachKnockout,
  relaunchApp,
  setupLeagueKnockout,
  tapText,
  toastText,
  uiText,
  uiTextContains
} = require('../helpers/app');

async function playKnockout(first, second) {
  return playNext({ first, second, players: PLAYERS_6, knockout: true });
}

describe('Feature 03 — Knockout stage', () => {
  it('CT-03-001 — Complete the league and review knockout qualification', async () => {
    await setupLeagueKnockout();
    await finishLeague({ players: PLAYERS_6, score: [2, 0] });
    await expectTextContains('League stage complete');
    await expectText('GO TO KNOCKOUT  ›');
    await expectTextContains('5  ');
    await expectTextContains('6  ');
  });

  it('CT-03-002 — Open the knockout bracket', async () => {
    await reachKnockout();
    await expectTextContains('Knockout stage');
    await expectTextAbsent('#  PLAYER');
    await expectText('SEMIFINALS');
    const source = await driver.getPageSource();
    const qualifierCount = PLAYERS_6.filter((name) => source.includes(`text="${name}"`)).length;
    assert.equal(qualifierCount, 4);
    const horizontalScroll = await $('android.widget.HorizontalScrollView');
    assert.equal(await horizontalScroll.isDisplayed(), true);
  });

  it('CT-03-003 — Record and display a knockout result', async () => {
    await reachKnockout();
    await tapText('PLAY NEXT KNOCKOUT MATCH  ›');
    const contestants = await currentKnockoutContestants();
    await expectText('VS');
    await expectTextAbsent('HOME');
    await expectTextAbsent('AWAY');
    await enterVisibleScores(3, 1);
    await tapText('FINISH MATCH');
    await tapText('Confirm');
    await expectTextContains(`${contestants[0]}`);
    await expectTextContains('3');
    await expectTextContains('1');
  });

  it('CT-03-004 — Prevent a drawn knockout result', async () => {
    await reachKnockout();
    await tapText('PLAY NEXT KNOCKOUT MATCH  ›');
    await enterVisibleScores(2, 2);
    await tapText('FINISH MATCH');
    await toastText('A knockout match cannot end in a draw');
    await expectText('FINISH MATCH');
    await tapText('Cancel');
    assert.equal(await (await $(uiTextContains('    2'))).isExisting(), false);
  });

  it('CT-03-005 — Advance winners and populate the next knockout round', async () => {
    await reachKnockout();
    const firstWinners = [];
    firstWinners.push((await playKnockout(3, 1))[0]);
    firstWinners.push((await playKnockout(2, 0))[0]);
    await expectText('SEMIFINALS');
    await expectText('FINAL');
    for (const winner of firstWinners) await expectTextContains(winner);
  });

  it('CT-03-006 — Finish the knockout tournament', async () => {
    await reachKnockout();
    await playKnockout(3, 1);
    await playKnockout(2, 0);
    await expectText('FINAL');
    await playKnockout(1, 0);
    const finished = await $(uiText('Export results...'));
    await finished.waitForDisplayed();
    assert.equal(await finished.isEnabled(), true);
    await expectText('FINAL');
  });

  it('CT-03-007 — Display knockout matches in match histories', async () => {
    await reachKnockout();
    await playKnockout(3, 1);
    await tapText('All matches');
    await expectTextContains('KNOCKOUT');
    await expectTextContains('3 – 1');
    assert.equal((await driver.getPageSource()).includes('HOME / AWAY'), true);
    const knockoutHeading = await $(uiTextContains('KNOCKOUT'));
    const card = await knockoutHeading.$('..');
    const cardText = (await Promise.all((await card.$$('android.widget.TextView')).map((el) => el.getText()))).join(' ');
    assert.equal(cardText.includes('HOME'), false);
    assert.equal(cardText.includes('AWAY'), false);
  });

  it('CT-03-008 — Persist the knockout bracket after reopening the app', async () => {
    await reachKnockout();
    await playKnockout(3, 1);
    await tapText('PLAY NEXT KNOCKOUT MATCH  ›');
    const expectedFixture = await currentMatchPlayers(PLAYERS_6);
    await tapText('Cancel');
    await relaunchApp();
    await expectTextContains('Knockout stage');
    await expectTextContains('3');
    await expectTextContains('1');
    await tapText('PLAY NEXT KNOCKOUT MATCH  ›');
    assert.deepEqual((await currentMatchPlayers(PLAYERS_6)).sort(), expectedFixture.sort());
  });
});
