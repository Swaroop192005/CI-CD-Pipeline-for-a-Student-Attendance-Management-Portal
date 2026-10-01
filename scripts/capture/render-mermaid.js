#!/usr/bin/env node
'use strict';
/*
 * render-mermaid.js — render a committed .mmd diagram source to PNG (and SVG)
 * so the diagram is viewable as an image in the evidence pack, while the .mmd
 * stays the single source of truth (GitHub renders it natively too).
 *
 * Mermaid is NOT vendored into this repository (the bundle is ~3.5 MB). Install
 * it once before rendering diagrams:
 *
 *     npm install --prefix scripts/capture
 *
 * or point MERMAID_JS at an existing mermaid.min.js.
 *
 * Usage:
 *   node render-mermaid.js --in docs/diagrams/x.mmd --out docs/evidence/task-02/x.png
 *        [--title "Caption"] [--svg docs/diagrams/x.svg] [--theme default] [--width 1600]
 */
const fs = require('fs');
const path = require('path');
const { chromium } = require(process.env.PW_MODULE || '/opt/node-tools/node_modules/playwright');

const argv = process.argv.slice(2);
const arg = (n, d) => { const i = argv.indexOf('--' + n); return i === -1 ? d : argv[i + 1]; };

const inFile = arg('in'), outFile = arg('out');
if (!inFile || !outFile) { console.error('usage: render-mermaid.js --in <mmd> --out <png> [--svg <svg>]'); process.exit(2); }
const svgOut = arg('svg', null);
const title  = arg('title', path.basename(inFile, '.mmd'));
const theme  = arg('theme', 'base');
const width  = parseInt(arg('width', '1700'), 10);
const scale  = parseFloat(arg('scale', '1'));

const candidates = [
  process.env.MERMAID_JS,
  path.join(__dirname, 'node_modules/mermaid/dist/mermaid.min.js'),
  path.join(__dirname, '../../node_modules/mermaid/dist/mermaid.min.js'),
].filter(Boolean);
const mermaidJs = candidates.find(p => { try { return fs.statSync(p).isFile(); } catch { return false; } });
if (!mermaidJs) {
  console.error('[render-mermaid] mermaid.min.js not found. Run:\n    npm install --prefix scripts/capture\n' +
                'or set MERMAID_JS=/path/to/mermaid.min.js\nLooked in:\n  ' + candidates.join('\n  '));
  process.exit(3);
}

const src = fs.readFileSync(inFile, 'utf8');
const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

(async () => {
  fs.mkdirSync(path.dirname(outFile), { recursive: true });
  const browser = await chromium.launch({ args: ['--no-sandbox', '--disable-dev-shm-usage'] });
  const page = await browser.newPage({ viewport: { width, height: 1000 }, deviceScaleFactor: 2 });

  const errors = [];
  page.on('pageerror', e => errors.push(e.message));
  page.on('console', m => { if (m.type() === 'error') errors.push(m.text()); });

  await page.setContent(`<!doctype html><html><head><meta charset="utf-8"><style>
    *{box-sizing:border-box;margin:0;padding:0}
    body{background:#ffffff;padding:30px;font-family:system-ui,-apple-system,"DejaVu Sans",sans-serif}
    #wrap{display:inline-block;background:#fff;border:1px solid #d0d7de;border-radius:10px;padding:26px 30px}
    h1{font-size:17px;color:#1f2328;margin-bottom:4px;font-weight:650}
    .sub{font-size:11.5px;color:#656d76;margin-bottom:20px;font-family:"DejaVu Sans Mono",monospace}
    #d svg{max-width:none!important;height:auto}
  </style></head><body>
  <div id="wrap"><h1>${esc(title)}</h1>
  <div class="sub">Student Attendance Management Portal &middot; rendered from ${esc(path.basename(inFile))}</div>
  <div id="d" class="mermaid">${esc(src)}</div></div>
  </body></html>`, { waitUntil: 'load' });

  await page.evaluate((sc) => { window.__scale = sc; }, scale);
  await page.addScriptTag({ path: mermaidJs });
  const ok = await page.evaluate(async (t) => {
    try {
      // eslint-disable-next-line no-undef
      mermaid.initialize({ startOnLoad: false, theme: t, securityLevel: 'loose',
        flowchart: { curve: 'basis', htmlLabels: true, nodeSpacing: 34, rankSpacing: 46 },
        themeVariables: { fontFamily: 'system-ui, -apple-system, "DejaVu Sans", sans-serif', fontSize: '19px' } });
      // eslint-disable-next-line no-undef
      await mermaid.run({ nodes: [document.getElementById('d')] });
      const svg = document.querySelector('#d svg');
      if (!svg) return 'no svg produced';
      // Mermaid renders with width="100%", which makes the diagram collapse to
      // the width of the surrounding block (here, the caption). Pin the SVG to
      // its own viewBox so glyphs render at the font size we asked for.
      const vb = (svg.getAttribute('viewBox') || '').split(/[ ,]+/).map(Number);
      if (vb.length === 4 && vb[2] > 0) {
        svg.style.width = (vb[2] * window.__scale) + 'px';
        svg.style.height = (vb[3] * window.__scale) + 'px';
        svg.style.maxWidth = 'none';
        svg.removeAttribute('width'); svg.removeAttribute('height');
      }
      return true;
    } catch (e) { return 'ERR: ' + e.message; }
  }, theme);

  if (ok !== true) {
    console.error('[render-mermaid] diagram did not render:', ok, errors.length ? '\n  ' + errors.join('\n  ') : '');
    await browser.close();
    process.exit(1);
  }

  if (svgOut) {
    fs.mkdirSync(path.dirname(svgOut), { recursive: true });
    fs.writeFileSync(svgOut, await page.evaluate(() => document.querySelector('#d svg').outerHTML));
  }
  await page.locator('#wrap').screenshot({ path: outFile });
  await browser.close();
  console.log(`[render-mermaid] ${outFile} (${(fs.statSync(outFile).size / 1024).toFixed(0)} KB)` + (svgOut ? ` + ${svgOut}` : ''));
})().catch(e => { console.error('[render-mermaid] FAILED:', e.message); process.exit(1); });
