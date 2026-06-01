// extensions.jsx — lightweight Mermaid flowchart + math (TeX subset) renderers.
// window.renderMermaid(src) -> svg html string ; window.renderMath(tex, display) -> html
(function () {
  function esc(s) { return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;'); }

  // ---------------- Mermaid (graph/flowchart TD|LR) ----------------
  function renderMermaid(src) {
    try {
      const lines = src.split('\n').map(l => l.trim()).filter(Boolean);
      let dir = 'TD';
      const head = lines[0] && lines[0].match(/^(?:graph|flowchart)\s+(TD|TB|LR|RL|BT)/i);
      if (head) { dir = head[1].toUpperCase(); lines.shift(); }
      const horizontal = dir === 'LR' || dir === 'RL';

      const nodes = {}; const edges = [];
      const order = [];
      function ensure(id, raw) {
        if (!nodes[id]) { nodes[id] = { id, label: id, shape: 'rect' }; order.push(id); }
        if (raw != null) {
          let shape = 'rect', label = raw;
          const m = raw.match(/^\(\((.*)\)\)$/) || raw.match(/^\((.*)\)$/) ? null : null;
          if (/^\[\[.*\]\]$/.test(raw)) { shape = 'subroutine'; label = raw.slice(2, -2); }
          else if (/^\[.*\]$/.test(raw)) { shape = 'rect'; label = raw.slice(1, -1); }
          else if (/^\(\(.*\)\)$/.test(raw)) { shape = 'circle'; label = raw.slice(2, -2); }
          else if (/^\(.*\)$/.test(raw)) { shape = 'round'; label = raw.slice(1, -1); }
          else if (/^\{.*\}$/.test(raw)) { shape = 'diamond'; label = raw.slice(1, -1); }
          else if (/^>.*\]$/.test(raw)) { shape = 'rect'; label = raw.slice(1, -1); }
          nodes[id].label = label.replace(/^["']|["']$/g, ''); nodes[id].shape = shape;
        }
      }
      const tok = '([A-Za-z0-9_]+)(\\[\\[.*?\\]\\]|\\[.*?\\]|\\(\\(.*?\\)\\)|\\(.*?\\)|\\{.*?\\}|>.*?\\])?';
      const edgeRe = new RegExp('^' + tok + '\\s*(-->|---|-\\.->|==>)\\s*(?:\\|(.*?)\\|\\s*)?' + tok + '$');
      lines.forEach(line => {
        const m = line.match(edgeRe);
        if (m) {
          ensure(m[1], m[2]); ensure(m[5], m[6]);
          edges.push({ from: m[1], to: m[5], label: m[4] || '', style: m[3] });
        } else {
          const nm = line.match(new RegExp('^' + tok + '$'));
          if (nm && nm[2]) ensure(nm[1], nm[2]);
        }
      });
      const ids = order;
      if (!ids.length) throw new Error('empty');

      // layering: longest path from roots
      const indeg = {}; ids.forEach(i => indeg[i] = 0);
      edges.forEach(e => { indeg[e.to] = (indeg[e.to] || 0) + 1; });
      const level = {}; ids.forEach(i => level[i] = 0);
      // iterate to propagate longest path
      for (let k = 0; k < ids.length; k++) {
        let changed = false;
        edges.forEach(e => { if (level[e.to] < level[e.from] + 1) { level[e.to] = level[e.from] + 1; changed = true; } });
        if (!changed) break;
      }
      const levels = {};
      ids.forEach(i => { (levels[level[i]] = levels[level[i]] || []).push(i); });
      const maxLevel = Math.max(...ids.map(i => level[i]));

      // sizing
      const measure = (lab, shape) => {
        const w = Math.max(54, lab.length * 8.4 + (shape === 'diamond' ? 40 : 28));
        return { w, h: shape === 'diamond' ? 52 : (shape === 'circle' ? 56 : 40) };
      };
      ids.forEach(i => { const d = measure(nodes[i].label, nodes[i].shape); nodes[i].w = d.w; nodes[i].h = d.h; });

      const GAP_LV = 64, GAP_N = 26, PAD = 14;
      const pos = {};
      // position per level
      const levelMain = [], levelCross = [];
      let cursorMain = PAD;
      for (let lv = 0; lv <= maxLevel; lv++) {
        const arr = levels[lv] || [];
        const mainSize = horizontal ? Math.max(...arr.map(i => nodes[i].w), 0) : Math.max(...arr.map(i => nodes[i].h), 0);
        let cross = PAD;
        arr.forEach(i => {
          if (horizontal) { pos[i] = { x: cursorMain, y: cross }; cross += nodes[i].h + GAP_N; }
          else { pos[i] = { x: cross, y: cursorMain }; cross += nodes[i].w + GAP_N; }
        });
        cursorMain += mainSize + GAP_LV;
      }
      // canvas extent
      let W = 0, H = 0;
      ids.forEach(i => { W = Math.max(W, pos[i].x + nodes[i].w + PAD); H = Math.max(H, pos[i].y + nodes[i].h + PAD); });
      // center each level along cross axis
      const crossMax = horizontal ? H : W;
      for (let lv = 0; lv <= maxLevel; lv++) {
        const arr = levels[lv] || []; if (!arr.length) continue;
        const last = arr[arr.length - 1];
        const used = horizontal ? (pos[last].y + nodes[last].h) : (pos[last].x + nodes[last].w);
        const off = (crossMax - used - PAD) / 2;
        arr.forEach(i => { if (horizontal) pos[i].y += off; else pos[i].x += off; });
      }

      // build svg
      const cx = i => pos[i].x + nodes[i].w / 2, cy = i => pos[i].y + nodes[i].h / 2;
      let edgeSvg = '';
      edges.forEach(e => {
        const x1 = cx(e.from), y1 = cy(e.from), x2 = cx(e.to), y2 = cy(e.to);
        // anchor on box borders
        let sx = x1, sy = y1, ex = x2, ey = y2;
        if (horizontal) { sx = pos[e.from].x + nodes[e.from].w; ex = pos[e.to].x; }
        else { sy = pos[e.from].y + nodes[e.from].h; ey = pos[e.to].y; }
        const mx = (sx + ex) / 2, my = (sy + ey) / 2;
        const d = horizontal ? `M${sx},${sy} C${mx},${sy} ${mx},${ey} ${ex},${ey}` : `M${sx},${sy} C${sx},${my} ${ex},${my} ${ex},${ey}`;
        const dash = e.style === '-.->' ? ' stroke-dasharray="5 4"' : '';
        edgeSvg += `<path d="${d}" class="mm-edge" fill="none"${dash} marker-end="url(#mm-arrow)"/>`;
        if (e.label) edgeSvg += `<rect x="${mx - e.label.length * 3.6 - 4}" y="${my - 9}" width="${e.label.length * 7.2 + 8}" height="18" rx="4" class="mm-elabel-bg"/><text x="${mx}" y="${my + 4}" class="mm-elabel" text-anchor="middle">${esc(e.label)}</text>`;
      });
      let nodeSvg = '';
      ids.forEach(i => {
        const n = nodes[i], x = pos[i].x, y = pos[i].y;
        let shapeEl;
        if (n.shape === 'diamond') {
          shapeEl = `<polygon points="${x + n.w / 2},${y} ${x + n.w},${y + n.h / 2} ${x + n.w / 2},${y + n.h} ${x},${y + n.h / 2}" class="mm-node mm-diamond"/>`;
        } else if (n.shape === 'circle') {
          shapeEl = `<ellipse cx="${x + n.w / 2}" cy="${y + n.h / 2}" rx="${n.w / 2}" ry="${n.h / 2}" class="mm-node"/>`;
        } else {
          const rx = n.shape === 'round' ? 18 : 7;
          shapeEl = `<rect x="${x}" y="${y}" width="${n.w}" height="${n.h}" rx="${rx}" class="mm-node${n.shape === 'subroutine' ? ' mm-sub' : ''}"/>`;
          if (n.shape === 'subroutine') shapeEl += `<line x1="${x + 5}" y1="${y}" x2="${x + 5}" y2="${y + n.h}" class="mm-edge"/><line x1="${x + n.w - 5}" y1="${y}" x2="${x + n.w - 5}" y2="${y + n.h}" class="mm-edge"/>`;
        }
        nodeSvg += shapeEl + `<text x="${x + n.w / 2}" y="${y + n.h / 2 + 4.5}" class="mm-label" text-anchor="middle">${esc(n.label)}</text>`;
      });
      return `<div class="md-mermaid"><svg viewBox="0 0 ${Math.ceil(W)} ${Math.ceil(H)}" width="${Math.ceil(W)}" style="max-width:100%;height:auto;" xmlns="http://www.w3.org/2000/svg">
<defs><marker id="mm-arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z" class="mm-arrowhead"/></marker></defs>
${edgeSvg}${nodeSvg}</svg></div>`;
    } catch (e) {
      return `<pre class="md-pre"><div class="md-pre-bar"><span class="md-lang">mermaid</span></div><code>${esc(src)}</code></pre>`;
    }
  }

  // ---------------- Math (TeX subset) ----------------
  const GREEK = { alpha: 'α', beta: 'β', gamma: 'γ', delta: 'δ', epsilon: 'ε', zeta: 'ζ', eta: 'η', theta: 'θ', iota: 'ι', kappa: 'κ', lambda: 'λ', mu: 'μ', nu: 'ν', xi: 'ξ', pi: 'π', rho: 'ρ', sigma: 'σ', tau: 'τ', phi: 'φ', chi: 'χ', psi: 'ψ', omega: 'ω', Gamma: 'Γ', Delta: 'Δ', Theta: 'Θ', Lambda: 'Λ', Xi: 'Ξ', Pi: 'Π', Sigma: 'Σ', Phi: 'Φ', Psi: 'Ψ', Omega: 'Ω' };
  const OPS = { times: '×', cdot: '·', div: '÷', pm: '±', mp: '∓', leq: '≤', le: '≤', geq: '≥', ge: '≥', neq: '≠', ne: '≠', approx: '≈', equiv: '≡', infty: '∞', partial: '∂', nabla: '∇', forall: '∀', exists: '∃', in: '∈', notin: '∉', subset: '⊂', supset: '⊃', cup: '∪', cap: '∩', rightarrow: '→', to: '→', leftarrow: '←', Rightarrow: '⇒', Leftarrow: '⇐', leftrightarrow: '↔', langle: '⟨', rangle: '⟩', cdots: '⋯', ldots: '…', prime: '′', star: '⋆', circ: '∘', bullet: '•', oplus: '⊕', otimes: '⊗', propto: '∝', sim: '∼', deg: '°' };

  function takeGroup(s, i) { // s[i]==='{'
    let depth = 0, j = i;
    for (; j < s.length; j++) { if (s[j] === '{') depth++; else if (s[j] === '}') { depth--; if (depth === 0) break; } }
    return { body: s.slice(i + 1, j), next: j + 1 };
  }
  function takeAtom(s, i) {
    if (s[i] === '{') return takeGroup(s, i);
    if (s[i] === '\\') { let m = s.slice(i).match(/^\\[A-Za-z]+/); if (m) return { body: s.slice(i, i + m[0].length), next: i + m[0].length }; return { body: s.slice(i, i + 2), next: i + 2 }; }
    return { body: s[i], next: i + 1 };
  }

  function math2html(tex) {
    let out = ''; let i = 0;
    while (i < tex.length) {
      const c = tex[i];
      if (c === '\\') {
        const m = tex.slice(i).match(/^\\([A-Za-z]+)/);
        if (m) {
          const cmd = m[1];
          if (cmd === 'frac' || cmd === 'dfrac' || cmd === 'tfrac') {
            let j = i + 1 + cmd.length; const a = takeAtom(tex, j); const b = takeAtom(tex, a.next);
            out += `<span class="mjx-frac"><span class="mjx-num">${math2html(a.body)}</span><span class="mjx-den">${math2html(b.body)}</span></span>`;
            i = b.next; continue;
          }
          if (cmd === 'sqrt') {
            let j = i + 1 + cmd.length; const a = takeAtom(tex, j);
            out += `<span class="mjx-sqrt"><span class="mjx-radic">√</span><span class="mjx-under">${math2html(a.body)}</span></span>`;
            i = a.next; continue;
          }
          if (cmd === 'sum' || cmd === 'prod' || cmd === 'int' || cmd === 'lim') {
            const sym = cmd === 'sum' ? '∑' : cmd === 'prod' ? '∏' : cmd === 'int' ? '∫' : 'lim';
            i += 1 + cmd.length;
            let sub = '', sup = '';
            while (tex[i] === '_' || tex[i] === '^') { const k = tex[i]; const a = takeAtom(tex, i + 1); if (k === '_') sub = a.body; else sup = a.body; i = a.next; }
            if (cmd === 'int') { out += `<span class="mjx-bigop mjx-int">${sym}</span>`; if (sub) out += `<sub>${math2html(sub)}</sub>`; if (sup) out += `<sup>${math2html(sup)}</sup>`; }
            else { out += `<span class="mjx-sumwrap"><span class="mjx-sup-lim">${math2html(sup)}</span><span class="mjx-bigop">${sym}</span><span class="mjx-sub-lim">${math2html(sub)}</span></span>`; }
            continue;
          }
          if (cmd === 'left' || cmd === 'right') { i += 1 + cmd.length; const d = tex[i] || ''; out += esc(d === '.' ? '' : d); i++; continue; }
          if (cmd === 'mathbb' || cmd === 'mathbf' || cmd === 'mathrm' || cmd === 'text' || cmd === 'mathcal') { let j = i + 1 + cmd.length; const a = takeAtom(tex, j); out += `<span class="mjx-${cmd}">${math2html(a.body)}</span>`; i = a.next; continue; }
          if (GREEK[cmd]) { out += GREEK[cmd]; i += 1 + cmd.length; continue; }
          if (OPS[cmd]) { out += `<span class="mjx-op">${OPS[cmd]}</span>`; i += 1 + cmd.length; continue; }
          // unknown command — drop backslash, keep name
          out += esc(cmd); i += 1 + cmd.length; continue;
        } else { i++; continue; }
      }
      if (c === '^' || c === '_') { const a = takeAtom(tex, i + 1); out += c === '^' ? `<sup>${math2html(a.body)}</sup>` : `<sub>${math2html(a.body)}</sub>`; i = a.next; continue; }
      if (c === '{' || c === '}') { i++; continue; }
      if (c === '*') { out += '<span class="mjx-op">×</span>'; i++; continue; }
      if (c === ' ') { out += ' '; i++; continue; }
      if (/[A-Za-z]/.test(c)) { out += `<i>${c}</i>`; i++; continue; }
      out += esc(c); i++;
    }
    return out;
  }

  function renderMath(tex, display) {
    try { return `<span class="md-math${display ? ' md-math-block' : ''}">${math2html(tex.trim())}</span>`; }
    catch (e) { return `<code class="md-code">${esc(tex)}</code>`; }
  }

  window.renderMermaid = renderMermaid;
  window.renderMath = renderMath;
})();
