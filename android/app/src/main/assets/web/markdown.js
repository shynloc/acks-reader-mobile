// markdown.jsx — lightweight Markdown -> semantic HTML renderer
// Exported to window.renderMarkdown(src) -> html string with stable class names.
(function () {
  function esc(s) {
    return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
  }
  // inline: code, math, bold, italic, strikethrough, links, images (code & math protected from later passes)
  function inline(s) {
    const stash = [];
    const keep = (html) => { stash.push(html); return '\u0000' + (stash.length - 1) + '\u0000'; };
    s = s.replace(/`([^`]+)`/g, (m, c) => keep(`<code class="md-code">${esc(c)}</code>`));
    s = s.replace(/\$([^$\n]+)\$/g, (m, c) => keep(window.renderMath ? window.renderMath(c, false) : esc(m)));
    let out = esc(s);
    out = out.replace(/!\[([^\]]*)\]\(([^)]+)\)/g, (m, alt) => `<span class="md-img" role="img" aria-label="${alt}"><span class="md-img-ph">▦ ${alt || 'image'}</span></span>`);
    out = out.replace(/\[([^\]]+)\]\(([^)]+)\)/g, (m, t, href) => `<a class="md-link" href="${href}">${t}</a>`);
    out = out.replace(/\*\*([^*]+)\*\*/g, '<strong>$1</strong>');
    out = out.replace(/(^|[^*])\*([^*]+)\*/g, '$1<em>$2</em>');
    out = out.replace(/~~([^~]+)~~/g, '<del>$1</del>');
    out = out.replace(/\u0000(\d+)\u0000/g, (m, i) => stash[+i]);
    return out;
  }

  function renderMarkdown(src) {
    const lines = src.replace(/\r\n/g, '\n').split('\n');
    let html = '';
    let i = 0;
    while (i < lines.length) {
      let line = lines[i];

      // fenced code
      const fence = line.match(/^```(\w*)/);
      if (fence) {
        const lang = fence[1] || '';
        let code = [];
        i++;
        while (i < lines.length && !/^```/.test(lines[i])) { code.push(lines[i]); i++; }
        i++; // closing fence
        if (lang === 'mermaid' && window.renderMermaid) {
          html += window.renderMermaid(code.join('\n'));
          continue;
        }
        html += `<pre class="md-pre"${lang ? ` data-lang="${lang}"` : ''}><div class="md-pre-bar"><span class="md-dot"></span><span class="md-dot"></span><span class="md-dot"></span><span class="md-lang">${lang || 'code'}</span></div><code>${esc(code.join('\n'))}</code></pre>`;
        continue;
      }

      // block math $$ ... $$
      if (/^\$\$/.test(line)) {
        let buf = []; let first = line.replace(/^\$\$/, '');
        if (/\$\$\s*$/.test(first) && first.replace(/\$\$\s*$/, '').trim()) {
          buf.push(first.replace(/\$\$\s*$/, '')); i++;
        } else {
          if (first.trim()) buf.push(first);
          i++;
          while (i < lines.length && !/\$\$/.test(lines[i])) { buf.push(lines[i]); i++; }
          if (i < lines.length) { const tail = lines[i].replace(/\$\$.*$/, ''); if (tail.trim()) buf.push(tail); i++; }
        }
        html += window.renderMath ? window.renderMath(buf.join(' '), true) : `<pre class="md-pre"><code>${esc(buf.join('\n'))}</code></pre>`;
        continue;
      }

      // headings
      const h = line.match(/^(#{1,6})\s+(.*)$/);
      if (h) {
        const lv = h[1].length;
        html += `<h${lv} class="md-h md-h${lv}">${inline(h[2])}</h${lv}>`;
        i++; continue;
      }

      // horizontal rule
      if (/^(\*\s*){3,}$|^(-\s*){3,}$|^(_\s*){3,}$/.test(line.trim())) {
        html += '<hr class="md-hr"/>'; i++; continue;
      }

      // blockquote (supports callout marker > [!NOTE])
      if (/^>\s?/.test(line)) {
        let buf = [];
        let kind = '';
        while (i < lines.length && /^>\s?/.test(lines[i])) {
          let t = lines[i].replace(/^>\s?/, '');
          const cal = t.match(/^\[!(\w+)\]\s*(.*)$/);
          if (cal) { kind = cal[1].toLowerCase(); t = cal[2]; }
          buf.push(t);
          i++;
        }
        if (kind) {
          html += `<div class="md-callout md-callout-${kind}"><div class="md-callout-label">${kind.toUpperCase()}</div><div class="md-callout-body">${inline(buf.join(' '))}</div></div>`;
        } else {
          html += `<blockquote class="md-quote">${inline(buf.join(' '))}</blockquote>`;
        }
        continue;
      }

      // table
      if (/\|/.test(line) && i + 1 < lines.length && /^\s*\|?[\s:-]+\|[\s:|-]*$/.test(lines[i + 1])) {
        const parseRow = (r) => r.replace(/^\s*\|/, '').replace(/\|\s*$/, '').split('|').map(c => c.trim());
        const head = parseRow(line);
        const aligns = parseRow(lines[i + 1]).map(c => {
          const l = c.startsWith(':'), r = c.endsWith(':');
          return l && r ? 'center' : r ? 'right' : l ? 'left' : '';
        });
        i += 2;
        let body = [];
        while (i < lines.length && /\|/.test(lines[i]) && lines[i].trim() !== '') { body.push(parseRow(lines[i])); i++; }
        let t = '<div class="md-table-wrap"><table class="md-table"><thead><tr>';
        head.forEach((c, ci) => { t += `<th${aligns[ci] ? ` style="text-align:${aligns[ci]}"` : ''}>${inline(c)}</th>`; });
        t += '</tr></thead><tbody>';
        body.forEach(row => {
          t += '<tr>';
          row.forEach((c, ci) => { t += `<td${aligns[ci] ? ` style="text-align:${aligns[ci]}"` : ''}>${inline(c)}</td>`; });
          t += '</tr>';
        });
        t += '</tbody></table></div>';
        html += t;
        continue;
      }

      // task list / unordered list
      if (/^\s*[-*+]\s+/.test(line)) {
        let items = [];
        while (i < lines.length && /^\s*[-*+]\s+/.test(lines[i])) {
          let t = lines[i].replace(/^\s*[-*+]\s+/, '');
          const task = t.match(/^\[([ xX])\]\s+(.*)$/);
          if (task) {
            const done = task[1].toLowerCase() === 'x';
            items.push(`<li class="md-task"><span class="md-checkbox${done ? ' md-checked' : ''}">${done ? '✓' : ''}</span><span>${inline(task[2])}</span></li>`);
          } else {
            items.push(`<li>${inline(t)}</li>`);
          }
          i++;
        }
        const isTask = items.some(x => x.includes('md-task'));
        html += `<ul class="md-ul${isTask ? ' md-tasklist' : ''}">${items.join('')}</ul>`;
        continue;
      }

      // ordered list
      if (/^\s*\d+\.\s+/.test(line)) {
        let items = [];
        while (i < lines.length && /^\s*\d+\.\s+/.test(lines[i])) {
          items.push(`<li>${inline(lines[i].replace(/^\s*\d+\.\s+/, ''))}</li>`);
          i++;
        }
        html += `<ol class="md-ol">${items.join('')}</ol>`;
        continue;
      }

      // blank line
      if (line.trim() === '') { i++; continue; }

      // paragraph (gather until blank / block start)
      let para = [line];
      i++;
      while (i < lines.length && lines[i].trim() !== '' &&
             !/^(#{1,6}\s|>|```|\s*[-*+]\s|\s*\d+\.\s)/.test(lines[i]) &&
             !(/\|/.test(lines[i]) && i + 1 < lines.length && /^\s*\|?[\s:-]+\|/.test(lines[i + 1]))) {
        para.push(lines[i]); i++;
      }
      html += `<p class="md-p">${inline(para.join(' '))}</p>`;
    }
    return html;
  }

  window.renderMarkdown = renderMarkdown;
})();
