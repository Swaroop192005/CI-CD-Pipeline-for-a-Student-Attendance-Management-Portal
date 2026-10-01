'use strict';
// Minimal ANSI SGR -> HTML converter so that real tool output (Maven, Docker,
// Ansible, Git) keeps its colours when rendered into an evidence screenshot.

const FG = {
  30: '#484f58', 31: '#ff7b72', 32: '#3fb950', 33: '#d29922', 34: '#58a6ff',
  35: '#bc8cff', 36: '#39c5cf', 37: '#b1bac4',
  90: '#6e7681', 91: '#ffa198', 92: '#56d364', 93: '#e3b341', 94: '#79c0ff',
  95: '#d2a8ff', 96: '#56d4dd', 97: '#f0f6fc',
};
const BG = {
  40: '#484f58', 41: '#da3633', 42: '#238636', 43: '#9e6a03', 44: '#1f6feb',
  45: '#8957e5', 46: '#1b7c83', 47: '#b1bac4',
};

const esc = (s) => s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');

function ansiToHtml(text) {
  // Drop carriage-return progress redraws (Docker pull, Maven downloads) so the
  // rendered image shows the final state of each line, as a real terminal does.
  text = text.replace(/\r(?!\n)/g, '\n');
  // Apply backspace destructively, the way a real terminal does. A pty emits
  // these around line wraps, and rendering them literally leaves stray glyphs.
  if (text.includes('\b')) {
    const buf = [];
    for (const ch of text) {
      if (ch === '\b') { if (buf.length && buf[buf.length - 1] !== '\n') buf.pop(); }
      else buf.push(ch);
    }
    text = buf.join('');
  }
  // Strip OSC (window-title) sequences and every non-SGR CSI sequence (cursor
  // moves, erase-line). Only the SGR form (final byte 'm') is kept, because that
  // is what the colour parser below consumes.
  text = text.replace(/\x1b\][^\x07\x1b]*(?:\x07|\x1b\\)/g, '')
             .replace(/\x1b\[[0-9;?]*[@-~]/g, (seq) => (seq.endsWith('m') ? seq : ''))
             .replace(/\x1b[()][AB0]/g, '')
             .replace(/\x1b[=>78]/g, '');

  let out = '';
  let open = false;
  const state = { fg: null, bg: null, bold: false, dim: false, underline: false };

  const closeSpan = () => { if (open) { out += '</span>'; open = false; } };
  const openSpan = () => {
    const st = [];
    if (state.fg) st.push(`color:${state.fg}`);
    if (state.bg) st.push(`background:${state.bg}`);
    if (state.bold) st.push('font-weight:700');
    if (state.dim) st.push('opacity:.65');
    if (state.underline) st.push('text-decoration:underline');
    if (st.length) { out += `<span style="${st.join(';')}">`; open = true; }
  };

  const re = /\x1b\[([0-9;]*)m/g;
  let last = 0, m;
  while ((m = re.exec(text)) !== null) {
    out += esc(text.slice(last, m.index));
    last = re.lastIndex;
    closeSpan();
    const codes = (m[1] === '' ? '0' : m[1]).split(';').map(Number);
    for (const c of codes) {
      if (c === 0) { state.fg = state.bg = null; state.bold = state.dim = state.underline = false; }
      else if (c === 1) state.bold = true;
      else if (c === 2) state.dim = true;
      else if (c === 4) state.underline = true;
      else if (c === 22) { state.bold = false; state.dim = false; }
      else if (c === 24) state.underline = false;
      else if (c === 39) state.fg = null;
      else if (c === 49) state.bg = null;
      else if (FG[c]) state.fg = FG[c];
      else if (BG[c]) state.bg = BG[c];
    }
    openSpan();
  }
  out += esc(text.slice(last));
  closeSpan();
  return out;
}

module.exports = { ansiToHtml, esc };
