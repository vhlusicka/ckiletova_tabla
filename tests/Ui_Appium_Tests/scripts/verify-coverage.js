const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');

const suiteRoot = path.resolve(__dirname, '..');
const specification = fs.readFileSync(
  path.resolve(suiteRoot, '../CT Test Specification.md'),
  'utf8'
);

const documentedIds = [...specification.matchAll(/^\| (CT-\d{2}-\d{3}) /gm)].map(
  (match) => match[1]
);
const implementedIds = fs
  .readdirSync(path.join(suiteRoot, 'specs'))
  .filter((name) => name.endsWith('.spec.js'))
  .flatMap((name) => {
    const source = fs.readFileSync(path.join(suiteRoot, 'specs', name), 'utf8');
    return [...source.matchAll(/\bit\('(CT-\d{2}-\d{3}) /g)].map((match) => match[1]);
  });

assert.equal(new Set(documentedIds).size, documentedIds.length, 'Duplicate test IDs in specification');
assert.equal(new Set(implementedIds).size, implementedIds.length, 'Duplicate test IDs in specs');
assert.deepEqual(
  [...implementedIds].sort(),
  [...documentedIds].sort(),
  'Implemented Appium IDs must match the test specification exactly'
);

console.log(`Verified ${implementedIds.length} Appium tests against the test specification.`);
