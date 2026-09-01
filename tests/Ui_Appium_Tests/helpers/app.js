const assert = require('node:assert/strict');

const APP_ID = 'com.debelatabla.fifaleague';
const PLAYERS_4 = ['Ana', 'Bruno', 'Carla', 'David'];
const PLAYERS_6 = ['Ana', 'Bruno', 'Carla', 'David', 'Eva', 'Filip'];

function uiText(value) {
  return `android=new UiSelector().text(${JSON.stringify(value)})`;
}

function uiTextContains(value) {
  return `android=new UiSelector().textContains(${JSON.stringify(value)})`;
}

async function displayed(selector, timeout = 10000) {
  const element = await $(selector);
  await element.waitForDisplayed({ timeout });
  return element;
}

async function tapElement(element) {
  const [{ x, y }, { width, height }] = await Promise.all([
    element.getLocation(),
    element.getSize()
  ]);
  await driver.execute('mobile: shell', {
    command: 'input',
    args: [
      'tap',
      String(Math.round(x + width / 2)),
      String(Math.round(y + height / 2))
    ]
  });
}

async function tapText(value) {
  let element = await $(uiText(value));
  if (!(await element.isDisplayed().catch(() => false))) {
    element = await $(
      `android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().text(${JSON.stringify(value)}))`
    );
  }
  await element.waitForDisplayed({ timeout: 10000 });
  await tapElement(element);
  return element;
}

async function tapTextContains(value) {
  const element = await displayed(uiTextContains(value));
  await tapElement(element);
  return element;
}

async function expectText(value) {
  assert.equal(await (await displayed(uiText(value))).isDisplayed(), true);
}

async function expectTextContains(value) {
  assert.equal(await (await displayed(uiTextContains(value))).isDisplayed(), true);
}

async function expectTextAbsent(value) {
  const element = await $(uiText(value));
  assert.equal(await element.isExisting(), false, `Expected text to be absent: ${value}`);
}

async function visibleEditTexts() {
  const elements = await $$('android.widget.EditText');
  const visible = [];
  for (const element of elements) {
    if (await element.isDisplayed()) visible.push(element);
  }
  return visible;
}

async function replaceValue(element, value) {
  await tapElement(element);
  await element.clearValue();
  await element.setValue(String(value));
}

async function resetApp() {
  await driver.terminateApp(APP_ID).catch(() => {});
  await driver.execute('mobile: clearApp', { appId: APP_ID });
  await driver.activateApp(APP_ID);
  await expectText('Čkiletova tabla');
  await expectText('Select tournament format  (required)');
}

async function relaunchApp() {
  await driver.terminateApp(APP_ID);
  await driver.activateApp(APP_ID);
}

async function fillPlayers(names) {
  for (let index = 0; index < names.length; index += 1) {
    if (index >= 2) await tapText('＋  Add contestant');
    let field = await $(uiText(`Contestant ${index + 1}`));
    if (!(await field.isDisplayed().catch(() => false))) {
      field = await $(
        `android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().text(${JSON.stringify(`Contestant ${index + 1}`)}))`
      );
    }
    await replaceValue(field, names[index]);
    await driver.hideKeyboard().catch(() => {});
  }
}

async function configureTournament({ type, matches, qualifiers }) {
  await tapText('Select tournament format  (required)');
  await tapText(type === 'knockout' ? 'League + knockout' : 'League only');
  const fields = await visibleEditTexts();
  assert.ok(fields.length >= (type === 'knockout' ? 2 : 1));
  await replaceValue(fields[0], matches);
  if (type === 'knockout') await replaceValue(fields[1], qualifiers);
  await driver.hideKeyboard().catch(() => {});
  await tapText('SAVE');
}

async function startTournament({ players = PLAYERS_4, type = 'league', matches = 2, qualifiers = 4 } = {}) {
  await fillPlayers(players);
  await configureTournament({ type, matches, qualifiers });
  await tapText('CONFIRM CONTESTANTS');
  await expectText('League table');
}

async function setupLeague(options = {}) {
  await resetApp();
  await startTournament({ players: PLAYERS_4, type: 'league', matches: 2, ...options });
}

async function setupLeagueKnockout(options = {}) {
  await resetApp();
  await startTournament({
    players: PLAYERS_6,
    type: 'knockout',
    matches: 2,
    qualifiers: 4,
    ...options
  });
}

async function currentMatchPlayers(candidates) {
  const found = [];
  for (const name of candidates) {
    const element = await $(uiText(name));
    if ((await element.isExisting()) && (await element.isDisplayed())) found.push(name);
  }
  if (found.length === 2) return found;

  const source = await driver.getPageSource();
  const sourceMatches = candidates.filter((name) => source.includes(`text="${name}"`));
  assert.equal(sourceMatches.length, 2, `Expected two match contestants, found: ${sourceMatches}`);
  return sourceMatches;
}

async function currentLeagueSides() {
  const banner = await displayed(uiTextContains('HOME'));
  const lines = (await banner.getText())
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line && !['HOME', 'VS', 'AWAY'].includes(line));
  assert.equal(lines.length, 2, `Expected HOME and AWAY contestants, found: ${lines}`);
  return { home: lines[0], away: lines[1] };
}

async function currentKnockoutContestants() {
  const banner = await displayed(uiTextContains('VS'));
  const lines = (await banner.getText())
    .split('\n')
    .map((line) => line.trim())
    .filter((line) => line && line !== 'VS');
  assert.equal(lines.length, 2, `Expected two knockout contestants, found: ${lines}`);
  return lines;
}

async function enterVisibleScores(first, second) {
  const fields = await visibleEditTexts();
  assert.equal(fields.length, 2, 'Expected exactly two visible score fields');
  await replaceValue(fields[0], first);
  await replaceValue(fields[1], second);
  await driver.hideKeyboard().catch(() => {});
}

async function playNext({ first = 1, second = 0, players = PLAYERS_4, knockout = false } = {}) {
  if (knockout) await tapText('PLAY NEXT KNOCKOUT MATCH  ›');
  else await tapTextContains('PLAY GAME');
  const contestants = knockout
    ? await currentKnockoutContestants()
    : await currentMatchPlayers(players);
  await enterVisibleScores(first, second);
  await tapText('FINISH MATCH');
  await expectTextContains(`${first} – ${second}`);
  await tapText('Confirm');
  await expectText('League table');
  return contestants;
}

async function finishLeague({ players = PLAYERS_4, score = [1, 0] } = {}) {
  for (let guard = 0; guard < 100; guard += 1) {
    if (await (await $(uiText('Export results...'))).isExisting()) return;
    if (await (await $(uiText('GO TO KNOCKOUT  ›'))).isExisting()) return;
    await playNext({ first: score[0], second: score[1], players });
  }
  throw new Error('League did not finish within the safety limit');
}

async function reachKnockout() {
  await setupLeagueKnockout();
  await finishLeague({ players: PLAYERS_6 });
  await expectText('GO TO KNOCKOUT  ›');
  await tapText('GO TO KNOCKOUT  ›');
  await expectTextContains('Knockout stage');
}

async function toastText(value) {
  const toast = await $(`//android.widget.Toast[@text=${JSON.stringify(value)}]`);
  await toast.waitForExist({ timeout: 4000 });
  return toast;
}

async function allTextsInParentOf(textValue) {
  const textElement = await displayed(uiTextContains(textValue));
  const parent = await textElement.$('..');
  const children = await parent.$$('android.widget.TextView');
  return Promise.all(children.map((child) => child.getText()));
}

module.exports = {
  APP_ID,
  PLAYERS_4,
  PLAYERS_6,
  allTextsInParentOf,
  configureTournament,
  currentLeagueSides,
  currentKnockoutContestants,
  currentMatchPlayers,
  displayed,
  enterVisibleScores,
  expectText,
  expectTextAbsent,
  expectTextContains,
  fillPlayers,
  finishLeague,
  playNext,
  reachKnockout,
  relaunchApp,
  replaceValue,
  resetApp,
  setupLeague,
  setupLeagueKnockout,
  startTournament,
  tapElement,
  tapText,
  tapTextContains,
  toastText,
  uiText,
  uiTextContains,
  visibleEditTexts
};
