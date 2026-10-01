#!/usr/bin/env node
'use strict';
/*
 * shot-term.js — render a REAL captured terminal transcript into a PNG for the
 * project evidence pack.
 *
 * The input is always a .log file produced by actually executing the command
 * (see run-and-shot.sh). This script only typesets that text; it never invents
 * output. The raw .log is committed next to every PNG so any screenshot can be
 * verified against its source.
 *
 * Usage:
 *   node shot-term.js --log <file> --out <png> [--title "cmd"] [--exit N]
 *                     [--cwd DIR] [--host NAME] [--cols N] [--max-lines N]
 */
const fs = require('fs');
const path = require('path');
const { chromium } = require(process.env.PW_MODULE || '/opt/node-tools/node_modules/playwright');
const { ansiToHtml, esc } = require('./ansi.js');

const argv = process.argv.slice(2);
const arg = (n, d) => { const i = argv.indexOf('--' + n); return i === -1 ? d : argv[i + 1]; };
const has = (n) => argv.includes('--' + n);

const logFile = arg('log');
const outFile = arg('out');
if (!logFile || !outFile) { console.error('usage: shot-term.js --log <file> --out <png> [--title T]'); process.exit(2); }

const title    = arg('title', path.basename(logFile, '.log'));
const host     = arg('host', 'samp-devops');
const cwd      = arg('cwd', '~/CI-CD-Pipeline-for-a-Student-Attendance-Management-Portal');
const cols     = parseInt(arg('cols', '110'), 10);
const maxLines = parseInt(arg('max-lines', '0'), 10);
const exitCode = arg('exit', null);
const stampArg = arg('stamp', null);

let raw = fs.readFileSync(logFile, 'utf8').replace(/\s+$/, '');
// run-and-shot.sh prefixes each .log with "$ <cmd>" and "# executed ..." for
// standalone readability. The image draws its own prompt line and footer, so
// drop that header here to avoid showing the command twice.
raw = raw.replace(/^\$ .*\n(?:# executed .*\n)?\n?/, '').replace(/^\n+/, '');
// The exit status is shown in the footer, so drop the trailing marker the
// wrapper appends to the .log for standalone readability.
raw = raw.replace(/\n*\[exit code: -?\d+\]\s*$/, '');
let trimmed = 0;
if (maxLines > 0) {
  const lines = raw.split('\n');
  if (lines.length > maxLines) {
    const head = Math.ceil(maxLines * 0.45), tail = maxLines - head;
    trimmed = lines.length - maxLines;
    raw = lines.slice(0, head).join('\n')
        + `\n\x1b[2m... [${trimmed} lines omitted - see ${path.basename(logFile)} for the full transcript] ...\x1b[0m\n`
        + lines.slice(-tail).join('\n');
  }
}

const body = ansiToHtml(raw);
const stamp = stampArg || (new Date().toISOString().replace('T', ' ').slice(0, 19) + ' UTC');
const statusTxt = exitCode === null ? '' : (exitCode === '0'
  ? `<span style="color:#3fb950">&#10003; exit 0</span>`
  : `<span style="color:#ff7b72">&#10007; exit ${esc(exitCode)}</span>`);

const html = `<!doctype html><html><head><meta charset="utf-8"><style>
  *{box-sizing:border-box;margin:0;padding:0}
  body{background:#010409;padding:22px;font-family:"DejaVu Sans Mono","Liberation Mono",monospace}
  .win{background:#0d1117;border:1px solid #30363d;border-radius:10px;overflow:hidden;
       box-shadow:0 14px 40px rgba(0,0,0,.6);width:${cols}ch;max-width:100%}
  .bar{display:flex;align-items:center;gap:8px;background:#161b22;border-bottom:1px solid #30363d;padding:9px 14px}
  .dot{width:11px;height:11px;border-radius:50%}
  .bar .t{flex:1;text-align:center;color:#8b949e;font-size:12.5px;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;padding:0 10px}
  pre{padding:14px 16px;color:#c9d1d9;font-size:13px;line-height:1.5;white-space:pre-wrap;word-break:break-word;tab-size:4}
  .prompt{color:#3fb950;font-weight:700}.path{color:#58a6ff;font-weight:700}
  .foot{display:flex;justify-content:space-between;gap:16px;background:#161b22;border-top:1px solid #30363d;
        padding:8px 14px;color:#6e7681;font-size:11.5px}
</style></head><body>
<div class="win">
  <div class="bar">
    <span class="dot" style="background:#ff5f57"></span>
    <span class="dot" style="background:#febc2e"></span>
    <span class="dot" style="background:#28c840"></span>
    <span class="t">${esc(title)}</span>
  </div>
  <pre><span class="prompt">${esc(host)}</span>:<span class="path">${esc(cwd)}</span>$ ${esc(title)}
${body}</pre>
  <div class="foot"><span>Student Attendance Management Portal &mdash; DevOps evidence</span><span>${statusTxt}${statusTxt ? ' &middot; ' : ''}executed ${esc(stamp)}</span></div>
</div></body></html>`;

(async () => {
  const browser = await chromium.launch({ args: ['--no-sandbox', '--disable-dev-shm-usage'] });
  const page = await browser.newPage({ deviceScaleFactor: 2 });
  await page.setContent(html, { waitUntil: 'load' });
  await page.locator('.win').screenshot({ path: outFile });
  await browser.close();
  const kb = (fs.statSync(outFile).size / 1024).toFixed(0);
  console.log(`[shot-term] ${outFile} (${kb} KB)${trimmed ? ` [${trimmed} lines elided in image; full log retained]` : ''}`);
})().catch(e => { console.error('[shot-term] FAILED:', e.message); process.exit(1); });
