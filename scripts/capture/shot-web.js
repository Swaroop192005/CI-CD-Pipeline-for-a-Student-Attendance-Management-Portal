#!/usr/bin/env node
'use strict';
/*
 * shot-web.js — screenshot a REAL page served by a running instance of the
 * application (or Jenkins) using headless Chromium, and frame it in browser
 * chrome showing the live URL and capture time.
 *
 * Nothing is mocked: the target URL must actually respond, or the script fails
 * with a non-zero exit code so a missing screenshot can never pass silently.
 *
 * Usage:
 *   node shot-web.js --url http://localhost:8080/login --out shot.png
 *        [--title "Login page"] [--steps steps.json] [--wait-for "css"]
 *        [--full-page] [--width 1366] [--height 820] [--no-frame]
 *
 * steps.json: [ {"fill": "#username", "value": "faculty1"},
 *               {"click": "button[type=submit]"},
 *               {"waitFor": "[data-testid=dashboard]"},
 *               {"waitForUrl": "**\/dashboard"} ]
 */
const fs = require('fs');
const path = require('path');
const { chromium } = require(process.env.PW_MODULE || '/opt/node-tools/node_modules/playwright');

const argv = process.argv.slice(2);
const arg = (n, d) => { const i = argv.indexOf('--' + n); return i === -1 ? d : argv[i + 1]; };
const has = (n) => argv.includes('--' + n);

const url = arg('url');
const out = arg('out');
if (!url || !out) { console.error('usage: shot-web.js --url <url> --out <png> [--title T]'); process.exit(2); }

const title    = arg('title', url);
const width    = parseInt(arg('width', '1366'), 10);
const height   = parseInt(arg('height', '820'), 10);
const waitFor  = arg('wait-for', null);
const stepsArg = arg('steps', null);
const timeout  = parseInt(arg('timeout', '30000'), 10);
// Sites with long-polling or websockets (GitHub, Jenkins) never reach
// 'networkidle'; --wait-until load/domcontentloaded handles those.
const waitUntil = arg('wait-until', 'networkidle');
// Hosts to abort rather than wait for. A blocked host whose connection is held
// open rather than refused will stall a page load indefinitely, so telemetry and
// other non-visual endpoints are cut off at the request layer.
const blockList = (arg('block', '') || '').split(',').map(x => x.trim()).filter(Boolean);
const esc = (s) => String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');

(async () => {
  fs.mkdirSync(path.dirname(out), { recursive: true });
  // --ignore-certificate-errors is required, not merely belt-and-braces: the
  // outbound proxy terminates TLS with its own CA, which Chromium does not trust.
  // Context-level ignoreHTTPSErrors alone does not cover the main-frame
  // navigation, which fails with ERR_CERT_AUTHORITY_INVALID.
  const browser = await chromium.launch({
    args: ['--no-sandbox', '--disable-dev-shm-usage', '--ignore-certificate-errors'] });
  const ctx = await browser.newContext({ viewport: { width, height }, deviceScaleFactor: 2, ignoreHTTPSErrors: true });
  const page = await ctx.newPage();

  if (blockList.length) {
    await page.route('**/*', (route) => {
      const url = route.request().url();
      if (blockList.some(h => url.includes(h))) return route.abort();
      return route.continue();
    });
  }

  const resp = await page.goto(url, { waitUntil, timeout });
  if (resp && resp.status() >= 400) throw new Error(`${url} returned HTTP ${resp.status()}`);

  if (stepsArg) {
    const steps = JSON.parse(fs.readFileSync(stepsArg, 'utf8'));
    for (const s of steps) {
      if (s.fill !== undefined)        await page.fill(s.fill, s.value, { timeout });
      else if (s.click !== undefined)  await page.click(s.click, { timeout });
      else if (s.select !== undefined) await page.selectOption(s.select, s.value, { timeout });
      else if (s.check !== undefined)  await page.check(s.check, { timeout });
      else if (s.press !== undefined)  await page.press(s.press, s.value || 'Enter', { timeout });
      else if (s.waitFor !== undefined)    await page.waitForSelector(s.waitFor, { timeout });
      else if (s.waitForUrl !== undefined) await page.waitForURL(s.waitForUrl, { timeout });
      else if (s.goto !== undefined)       await page.goto(s.goto, { waitUntil, timeout });
    }
  }
  if (waitFor) await page.waitForSelector(waitFor, { timeout });
  await page.waitForLoadState('networkidle', { timeout: 8000 }).catch(() => {});

  const finalUrl = page.url();
  const shotOpts = { fullPage: has('full-page') };

  if (has('no-frame')) {
    await page.screenshot({ path: out, ...shotOpts });
  } else {
    const inner = (await page.screenshot(shotOpts)).toString('base64');
    const stamp = new Date().toISOString().replace('T', ' ').slice(0, 19) + ' UTC';
    const frame = await ctx.newPage();
    await frame.setContent(`<!doctype html><meta charset="utf-8"><style>
      *{box-sizing:border-box;margin:0;padding:0}
      body{background:#010409;padding:22px;font-family:system-ui,-apple-system,"DejaVu Sans",sans-serif}
      .win{border:1px solid #30363d;border-radius:10px;overflow:hidden;box-shadow:0 14px 40px rgba(0,0,0,.6);width:max-content}
      .bar{display:flex;align-items:center;gap:8px;background:#161b22;border-bottom:1px solid #30363d;padding:9px 14px}
      .dot{width:11px;height:11px;border-radius:50%}
      .url{flex:1;background:#0d1117;border:1px solid #30363d;border-radius:6px;color:#8b949e;
           font-family:"DejaVu Sans Mono",monospace;font-size:12px;padding:5px 11px;margin-left:8px;
           white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
      img{display:block}
      .foot{display:flex;justify-content:space-between;gap:16px;background:#161b22;border-top:1px solid #30363d;
            padding:8px 14px;color:#6e7681;font-size:11.5px;font-family:"DejaVu Sans Mono",monospace}
    </style>
    <div class="win">
      <div class="bar">
        <span class="dot" style="background:#ff5f57"></span>
        <span class="dot" style="background:#febc2e"></span>
        <span class="dot" style="background:#28c840"></span>
        <span class="url">${finalUrl.startsWith("https:") ? "&#128274;" : "&#9432;"} ${esc(finalUrl)}</span>
      </div>
      <img src="data:image/png;base64,${inner}">
      <div class="foot"><span>${esc(title)}</span><span>Chromium headless &middot; captured ${stamp}</span></div>
    </div>`, { waitUntil: 'load' });
    await frame.locator('.win').screenshot({ path: out });
  }

  await browser.close();
  console.log(`[shot-web] ${out} <- ${finalUrl} (${(fs.statSync(out).size / 1024).toFixed(0)} KB)`);
})().catch(e => { console.error('[shot-web] FAILED:', e.message); process.exit(1); });
