import { createHash } from 'node:crypto';
import { readFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import assert from 'node:assert/strict';

const root = resolve(import.meta.dirname, '..');
const expected = {
  'docs/assets/logo.svg': '5fa7ef804c18bc073222f8b2100fd4c04239f9fb2e41d5d1ecc00f056e1043d2',
  'docs/assets/favicon.svg': '5fa7ef804c18bc073222f8b2100fd4c04239f9fb2e41d5d1ecc00f056e1043d2',
  'docs/assets/social-preview.png': '2ca4b1125453378eba138ca04e8cb57c0840b4af5f5887cd7079d831cda77d90',
  'docs/assets/oss-brand-checksums.txt': '53a818e80aa1542c4dd3a89407f66e47b6345b0c797bdc535b6cff1ed7a6affc',
  '.github/assets/readme-header.png': '8a6a2a43c210644c02e36b9aab9cd7c4bccb6df0ee3273c0c961b291dbefe06b',
  '.github/assets/social-preview.png': '2ca4b1125453378eba138ca04e8cb57c0840b4af5f5887cd7079d831cda77d90',
};
const distinctProjects = {
  O09: '94697c47e87ac17d9209b890da0993b9d85c04ddb2d91b27afc7cfc31dc0380d',
  O10: '927af7009de24040ac94c0586f3d93dac30c28b08c1d29b2a83c1bf74f1c4248',
};

async function file(relativePath) {
  return readFile(resolve(root, relativePath));
}

for (const [relativePath, hash] of Object.entries(expected)) {
  const actual = createHash('sha256').update(await file(relativePath)).digest('hex');
  assert.equal(actual, hash, `${relativePath} must be the OSS brand v0.1.1 O08 asset`);
}

const logo = (await file('docs/assets/logo.svg')).toString();
assert.match(logo, /data-oss-project="O08"/, 'logo must identify O08');
for (const [project, hash] of Object.entries(distinctProjects)) {
  assert.notEqual(createHash('sha256').update(logo).digest('hex'), hash, `O08 must not use ${project} artwork`);
}

const mkdocs = (await file('mkdocs.yml')).toString();
assert.match(mkdocs, /custom_dir:\s*docs\/overrides/, 'MkDocs must use source overrides');
assert.match(mkdocs, /logo:\s*assets\/logo\.svg/, 'MkDocs must use the O08 logo');
assert.match(mkdocs, /favicon:\s*assets\/favicon\.svg/, 'MkDocs must use the O08 favicon');

for (const readme of ['README.md', 'README.ko.md']) {
  const content = (await file(readme)).toString();
  assert.match(content, /\.github\/assets\/readme-header\.png/, `${readme} must use the vendored README header`);
  assert.match(content, /https:\/\/devslab\.kr\/brand\/open-source\//, `${readme} must link to the DevsLab OSS guide`);
  assert.match(content, /^# easy-paging-spring-boot-starter/m, `${readme} must retain its project heading`);
}

for (const index of ['docs/index.md', 'docs/index.ko.md']) {
  const content = (await file(index)).toString();
  assert.match(content, /assets\/logo\.svg/, `${index} must render the O08 mark`);
  assert.match(content, /devslab\.kr\/brand\/open-source\//, `${index} must attribute DevsLab`);
}

const override = (await file('docs/overrides/main.html')).toString();
assert.match(override, /assets\/social-preview\.png/, 'social metadata must use the vendored O08 preview');
assert.match(override, /twitter:image:alt/, 'social metadata must include a Twitter image alternative');
assert.match(override, /oss-docs-atmosphere/, 'the docs shell must include the restrained atmosphere layer');

const css = (await file('docs/stylesheets/extra.css')).toString();
assert.match(css, /\.oss-docs-atmosphere/, 'the docs atmosphere must be styled');
assert.match(css, /\[data-md-color-scheme="slate"\][^{]*\.oss-docs-atmosphere[\s\S]*?0\.10/, 'dark atmosphere must remain capped at .10 opacity');
assert.match(css, /forced-colors: active/, 'the atmosphere must respect forced-colors mode');

const ci = (await file('.github/workflows/ci.yml')).toString();
const buildJob = ci.slice(ci.indexOf('  build:'), ci.indexOf('  dialect-compat:'));
const dialectJob = ci.slice(ci.indexOf('  dialect-compat:'));
assert.match(buildJob, /run:\s*node scripts\/check-brand-assets\.mjs/, 'the routine CI build job must verify brand assets');
assert.match(dialectJob, /run:\s*node scripts\/check-brand-assets\.mjs/, 'the routine CI dialect job must verify brand assets');

const release = (await file('.github/workflows/release.yml')).toString();
assert.match(release, /run:\s*node scripts\/check-brand-assets\.mjs/, 'the release path must verify brand assets before publishing');

console.log('O08 DevsLab OSS brand assets and source references are valid.');
