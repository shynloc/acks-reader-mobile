// themes.jsx — 13 real Markdown rendering themes (WYSIWYG, iframe-isolated).
// window.THEMES = [{id,name,en,use,tag,modes,defaultMode,swatch:{light,dark}}]
// window.buildThemeDoc(themeId, mdHtml, {mode, ov, fontSource}) -> full HTML doc string
// ov = { fonts:{title,body,quote,code,table}, baseSize, colors:{title,body,accent} }
// fontSource: "google" | "cn_mirror" | "local"  (default: "google")
(function () {
  // ---- Font source routing (Android-specific) --------------------------------
  // "google"     → fonts.googleapis.com  (overseas / when reachable)
  // "cn_mirror"  → fonts.loli.net        (mainland China mirror)
  // "local"      → @font-face pointing to file:///android_asset/fonts/
  const FONT_BASES = {
    google:    'https://fonts.googleapis.com/css2?',
    cn_mirror: 'https://fonts.loli.net/css2?',
  };
  // Core fonts bundled locally in assets/fonts/ (variable WOFF2)
  const LOCAL_FONT_FACES =
    "@font-face{font-family:'Space Grotesk';font-weight:300 700;font-display:swap;" +
      "src:url('file:///android_asset/fonts/SpaceGrotesk.woff2')format('woff2');}" +
    "@font-face{font-family:'DM Sans';font-weight:300 700;font-style:normal;font-display:swap;" +
      "src:url('file:///android_asset/fonts/DMSans.woff2')format('woff2');}" +
    "@font-face{font-family:'JetBrains Mono';font-weight:400 700;font-display:swap;" +
      "src:url('file:///android_asset/fonts/JetBrainsMono.woff2')format('woff2');";

  const FONT_PARAMS = [
    "family=Space+Grotesk:wght@400;500;600;700",
    "family=DM+Sans:opsz,wght@9..40,400;9..40,500;9..40,600;9..40,700",
    "family=Inter:wght@400;500;600;700",
    "family=Noto+Sans+SC:wght@400;500;700",
    "family=Noto+Serif+SC:wght@400;500;600;700;900",
    "family=JetBrains+Mono:wght@400;500;700",
    "family=Playfair+Display:ital,wght@0,400;0,600;0,700;0,900;1,400",
    "family=Cormorant+Garamond:ital,wght@0,400;0,500;0,600;1,400",
    "family=EB+Garamond:ital,wght@0,400;0,500;1,400",
    "family=Lora:ital,wght@0,400;0,500;0,600;1,400",
    "family=Source+Serif+4:opsz,wght@8..60,400;8..60,600",
    "family=Archivo:wght@500;700;900",
    "family=Anton",
    "family=Ma+Shan+Zheng",
  ].join("&") + "&display=swap";

  // Backward-compat alias used by THEME_FONTS_HREF / capture mode
  const FONTS = FONT_BASES.google + FONT_PARAMS;

  // Returns the full <link> or <style> tag to inject into the rendered document
  function getFontsHtml(source) {
    if (source === 'local') {
      return '<style>' + LOCAL_FONT_FACES + '</style>';
    }
    const base = FONT_BASES[source] || FONT_BASES.google;
    return '<link href="' + base + FONT_PARAMS + '" rel="stylesheet">';
  }

  // ---- On-demand extra fonts (typography override panel) --------------------
  // Tracks which base URL to use for the current render session
  var _gfBase = FONT_BASES.google;

  const injected = {};
  function ensureGoogleFont(spec) {
    if (!spec || injected[spec]) return;
    injected[spec] = true;
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = _gfBase + 'family=' + spec + '&display=swap';
    link.setAttribute('data-acks-gf', spec);
    document.head.appendChild(link);
  }
  function gfLink(specs) {
    if (!specs || !specs.length) return '';
    const uniq = Array.from(new Set(specs.filter(Boolean)));
    if (!uniq.length) return '';
    return '<link href="' + _gfBase + uniq.map(function(s){ return 'family=' + s; }).join('&') + '&display=swap" rel="stylesheet">';
  }

  // shared structural block: tables, code, callouts, hr, images, links, lists
  function struct(c) {
    return `
    .md-link{color:${c.accent};text-decoration:none;border-bottom:1px solid ${c.accent}55;}
    .md-link:hover{border-bottom-color:${c.accent};}
    .md-hr{border:none;height:1px;background:${c.rule};margin:2em 0;}
    .md-img{display:block;margin:1.4em 0;}
    .md-img-ph{display:flex;align-items:center;justify-content:center;height:150px;background:${c.codeBg};color:${c.muted};border:1px dashed ${c.border};border-radius:6px;font-size:.82em;letter-spacing:.04em;}
    .md-ul,.md-ol{padding-left:1.4em;margin:.9em 0;}
    .md-ul li,.md-ol li{margin:.38em 0;}
    .md-tasklist{list-style:none;padding-left:.2em;}
    .md-task{display:flex;align-items:flex-start;gap:.6em;}
    .md-checkbox{flex:0 0 1.05em;width:1.05em;height:1.05em;border:1.5px solid ${c.border};border-radius:4px;display:inline-flex;align-items:center;justify-content:center;font-size:.7em;color:#fff;margin-top:.15em;}
    .md-checkbox.md-checked{background:${c.accent};border-color:${c.accent};}
    .md-code{font-family:var(--f-code,'JetBrains Mono',monospace);font-size:.86em;background:${c.inlineBg};color:${c.inlineFg};padding:.12em .4em;border-radius:4px;border:1px solid ${c.border}66;}
    .md-pre{font-family:var(--f-code,'JetBrains Mono',monospace);background:${c.codeBg};color:${c.codeFg};border:1px solid ${c.codeBorder};border-radius:8px;margin:1.3em 0;overflow:hidden;}
    .md-pre-bar{display:flex;align-items:center;gap:6px;padding:9px 14px;border-bottom:1px solid ${c.codeBorder};background:${c.codeBar};}
    .md-dot{width:9px;height:9px;border-radius:50%;background:${c.muted}55;}
    .md-lang{margin-left:auto;font-size:11px;letter-spacing:.08em;text-transform:uppercase;color:${c.muted};font-family:var(--f-code,'JetBrains Mono',monospace);}
    .md-pre code{display:block;padding:16px;overflow-x:auto;font-size:.84em;line-height:1.65;white-space:pre;}
    .md-table-wrap{overflow-x:auto;margin:1.3em 0;border-radius:8px;border:1px solid ${c.border};}
    .md-table{border-collapse:collapse;width:100%;font-family:var(--f-table,inherit);font-size:.9em;}
    .md-table th{background:${c.headBg};color:${c.headFg};font-weight:600;text-align:left;padding:.7em 1em;border-bottom:1px solid ${c.border};white-space:nowrap;}
    .md-table td{padding:.62em 1em;border-bottom:1px solid ${c.rule};}
    .md-table tr:last-child td{border-bottom:none;}
    .md-table tbody tr:nth-child(even){background:${c.zebra};}
    .md-callout{display:flex;flex-direction:column;gap:.3em;margin:1.3em 0;padding:.9em 1.1em;border-radius:8px;background:${c.calBg};border:1px solid ${c.calBorder};border-left:3px solid ${c.accent};}
    .md-callout-label{font-size:.7em;font-weight:700;letter-spacing:.12em;color:${c.accent};}
    .md-callout-warning{border-left-color:#F59E0B;} .md-callout-warning .md-callout-label{color:#F59E0B;}
    .md-callout-tip{border-left-color:#22C55E;} .md-callout-tip .md-callout-label{color:#22C55E;}
    .md-mermaid{margin:1.4em 0;text-align:center;overflow-x:auto;padding:14px 6px;background:${c.codeBg};border:1px solid ${c.border};border-radius:10px;}
    .md-mermaid svg{margin:0 auto;}
    .mm-node{fill:${c.headBg};stroke:${c.accent};stroke-width:1.5;}
    .mm-sub{fill:${c.calBg};}
    .mm-edge{stroke:${c.muted};stroke-width:1.5;}
    .mm-arrowhead{fill:${c.muted};stroke:none;}
    .mm-label{fill:${c.headFg};font-family:var(--f-body,inherit);font-size:13px;font-weight:600;}
    .mm-elabel{fill:${c.muted};font-size:11px;font-weight:500;}
    .mm-elabel-bg{fill:${c.codeBg};stroke:${c.border};stroke-width:1;}
    .md-math{font-family:'Source Serif 4',Cambria,Georgia,serif;font-style:normal;white-space:nowrap;}
    .md-math i{font-style:italic;padding:0 .02em;}
    .md-math-block{display:block;text-align:center;font-size:1.22em;margin:1.3em 0;padding:.4em 0;overflow-x:auto;}
    .mjx-frac{display:inline-flex;flex-direction:column;vertical-align:middle;text-align:center;margin:0 .12em;}
    .mjx-num{border-bottom:1.2px solid currentColor;padding:0 .35em .1em;}
    .mjx-den{padding:.1em .35em 0;}
    .mjx-sqrt{display:inline-flex;align-items:flex-start;}
    .mjx-radic{font-size:1.1em;}
    .mjx-under{border-top:1.2px solid currentColor;padding:0 .15em;margin-top:.05em;}
    .mjx-bigop{font-size:1.6em;line-height:.7;vertical-align:middle;}
    .mjx-int{font-style:italic;}
    .mjx-sumwrap{display:inline-flex;flex-direction:column;align-items:center;vertical-align:middle;margin:0 .15em;}
    .mjx-sup-lim,.mjx-sub-lim{font-size:.58em;line-height:1;}
    .mjx-op{padding:0 .2em;}
    .mjx-mathbb,.mjx-mathbf{font-weight:700;} .mjx-text,.mjx-mathrm{font-style:normal;font-family:var(--f-body,inherit);}
    .md-math sup,.md-math sub{font-size:.7em;}
    `;
  }

  // each theme: meta + css(mode) returning the document <style> body
  const THEMES = [
    {
      id: 'clean', name: '清隽阅读', en: 'Clean Reading', use: '阅读', tag: 'Reading',
      modes: ['light', 'dark'], defaultMode: 'light',
      swatch: { light: ['#FBFAF8', '#23211d', '#C2613A'], dark: ['#16140f', '#e8e4db', '#E08A4E'] },
      css: (m) => {
        const d = m === 'dark';
        const bg = d ? '#16140f' : '#FBFAF8', fg = d ? '#d9d4c8' : '#2a2722', head = d ? '#f1ede3' : '#23211d';
        const acc = 'var(--c-accent,#C2613A)', mut = d ? '#8a8475' : '#9b9483';
        return `
        html{font-size:var(--s-base,18px);} body{background:${bg};color:var(--c-body,${fg});font-family:var(--f-body,'Lora',Georgia,serif);line-height:1.78;}
        .md-h{font-family:var(--f-title,'Source Serif 4','Lora',serif);color:var(--c-title,${head});line-height:1.25;letter-spacing:-.01em;}
        .md-h1{font-size:2.05em;font-weight:600;margin:0 0 .6em;} .md-h2{font-size:1.5em;font-weight:600;margin:1.7em 0 .5em;} .md-h3{font-size:1.2em;font-weight:600;margin:1.4em 0 .4em;}
        .md-p{margin:1.05em 0;} .md-quote{font-family:var(--f-quote,'Lora',serif);font-style:italic;color:${mut};border-left:2px solid ${acc};padding-left:1.1em;margin:1.4em 0;}
        ${struct({accent:'#C2613A',rule:d?'#2a2720':'#ece7dc',border:d?'#33302a':'#e0dacd',muted:mut,headBg:d?'#1d1a14':'#f3efe5',headFg:head,zebra:d?'#1a1812':'#f6f2ea',inlineBg:d?'#221f18':'#f0ebe0',inlineFg:d?'#e0b48a':'#9a4d27',codeBg:d?'#100f0b':'#f5f1e8',codeFg:d?'#cfc9bb':'#3a372f',codeBorder:d?'#2a2720':'#e6e0d3',codeBar:d?'#16140f':'#efe9dc',calBg:d?'#1b1812':'#f5f0e6',calBorder:d?'#2e2a22':'#e8e1d2'})}`;
      }
    },
    {
      id: 'business', name: '商务报告', en: 'Business Report', use: '商务', tag: 'Business',
      modes: ['light', 'dark'], defaultMode: 'light',
      swatch: { light: ['#FFFFFF', '#0f1c2e', '#1F5FA8'], dark: ['#0d1117', '#dbe3ee', '#4d8fd6'] },
      css: (m) => {
        const d = m === 'dark';
        const bg = d ? '#0d1117' : '#FFFFFF', fg = d ? '#c4cdd9' : '#27313f', head = d ? '#eef2f7' : '#0f1c2e';
        const acc = 'var(--c-accent,#1F5FA8)', mut = d ? '#7d8694' : '#6b7480';
        return `
        html{font-size:var(--s-base,16px);} body{background:${bg};color:var(--c-body,${fg});font-family:var(--f-body,'Inter','Noto Sans SC',sans-serif);line-height:1.62;}
        .md-h{font-family:var(--f-title,'Inter',sans-serif);color:var(--c-title,${head});letter-spacing:-.015em;}
        .md-h1{font-size:1.9em;font-weight:700;margin:0 0 .3em;padding-bottom:.4em;border-bottom:2px solid ${acc};}
        .md-h2{font-size:1.32em;font-weight:700;margin:1.7em 0 .5em;padding-left:.5em;border-left:4px solid ${acc};}
        .md-h3{font-size:1.08em;font-weight:600;margin:1.3em 0 .4em;color:${acc};}
        .md-p{margin:.85em 0;} .md-quote{background:${d?'#141c27':'#f4f7fb'};border-left:3px solid ${acc};padding:.8em 1.1em;margin:1.2em 0;color:${mut};font-size:.96em;}
        ${struct({accent:'#1F5FA8',rule:d?'#222c38':'#eef1f5',border:d?'#283442':'#dfe5ec',muted:mut,headBg:d?'#16202c':'#f1f5fa',headFg:head,zebra:d?'#11181f':'#f8fafc',inlineBg:d?'#1a2330':'#eef3f9',inlineFg:d?'#7fb0e0':'#1c4f87',codeBg:d?'#0a0e13':'#f6f8fa',codeFg:d?'#c4cdd9':'#2c3540',codeBorder:d?'#222c38':'#e4e9ef',codeBar:d?'#0d141c':'#eef2f6',calBg:d?'#121a24':'#f3f7fb',calBorder:d?'#243040':'#dde6f0'})}`;
      }
    },
    {
      id: 'technical', name: '技术文档', en: 'Technical Docs', use: '技术', tag: 'Technical',
      modes: ['light', 'dark'], defaultMode: 'light',
      swatch: { light: ['#FFFFFF', '#1b2733', '#0E9F6E'], dark: ['#0e1116', '#cdd6e0', '#2BD4A0'] },
      css: (m) => {
        const d = m === 'dark';
        const bg = d ? '#0e1116' : '#FFFFFF', fg = d ? '#bcc6d1' : '#2b3640', head = d ? '#eef3f8' : '#16202b';
        const acc = 'var(--c-accent,#0E9F6E)', mut = d ? '#79838f' : '#737d87';
        return `
        html{font-size:var(--s-base,15.5px);} body{background:${bg};color:var(--c-body,${fg});font-family:var(--f-body,'Inter','Noto Sans SC',sans-serif);line-height:1.65;}
        .md-h{font-family:var(--f-title,'Space Grotesk',sans-serif);color:var(--c-title,${head});letter-spacing:-.01em;}
        .md-h1{font-size:1.85em;font-weight:700;margin:0 0 .5em;}
        .md-h2{font-size:1.3em;font-weight:600;margin:1.8em 0 .5em;padding-bottom:.3em;border-bottom:1px solid ${d?'#222a33':'#e8ecf1'};}
        .md-h2::before{content:'#';color:${acc};margin-right:.4em;font-weight:500;} .md-h3{font-size:1.06em;font-weight:600;margin:1.3em 0 .4em;}
        .md-p{margin:.8em 0;} .md-quote{border-left:3px solid ${acc};padding:.2em 0 .2em 1em;margin:1.1em 0;color:${mut};}
        ${struct({accent:'#0E9F6E',rule:d?'#1d242c':'#eef1f4',border:d?'#262f38':'#e1e6eb',muted:mut,headBg:d?'#151a21':'#f4f6f8',headFg:head,zebra:d?'#11151a':'#f9fafb',inlineBg:d?'#16201c':'#eafaf3',inlineFg:d?'#3ddaa6':'#0a7a54',codeBg:d?'#0a0d12':'#1b2733',codeFg:d?'#cdd6e0':'#e6edf3',codeBorder:d?'#1f262e':'#1b2733',codeBar:d?'#0d1117':'#141d27',calBg:d?'#11171d':'#f3f7f9',calBorder:d?'#222c34':'#e0e7ec'})}`;
      }
    },
    {
      id: 'darkcode', name: '暗夜代码', en: 'Dark Code', use: '代码', tag: 'Dark',
      modes: ['dark'], defaultMode: 'dark',
      swatch: { dark: ['#0b0d12', '#c8d3e0', '#F97316'] },
      css: () => {
        const acc = 'var(--c-accent,#F97316)';
        return `
        html{font-size:var(--s-base,15px);} body{background:#0b0d12;color:var(--c-body,#c2cdda);font-family:var(--f-body,'Inter','Noto Sans SC',sans-serif);line-height:1.7;}
        .md-h{font-family:var(--f-title,'JetBrains Mono',monospace);color:var(--c-title,#eef2f7);letter-spacing:-.01em;}
        .md-h1{font-size:1.7em;font-weight:700;margin:0 0 .5em;} .md-h1::before{content:'> ';color:${acc};}
        .md-h2{font-size:1.25em;font-weight:600;margin:1.7em 0 .5em;color:#9bd4ff;} .md-h3{font-size:1.05em;font-weight:600;margin:1.3em 0 .4em;color:#c792ea;}
        .md-p{margin:.85em 0;} .md-quote{border-left:3px solid ${acc};background:#11141b;padding:.7em 1em;margin:1.2em 0;color:#9aa6b4;}
        ${struct({accent:'#F97316',rule:'#1a1f29',border:'#222936',muted:'#6b7686',headBg:'#13171f',headFg:'#dde6f0',zebra:'#0f131a',inlineBg:'#1a1410',inlineFg:'#ffb27a',codeBg:'#070910',codeFg:'#d6e1f0',codeBorder:'#1c2330',codeBar:'#0d1018',calBg:'#11151d',calBorder:'#212835'})}
        .md-pre code{color:#c8d3e0;}`;
      }
    },
    {
      id: 'social', name: '社交长图', en: 'Social Long Image', use: '长图', tag: 'Social',
      modes: ['light', 'dark'], defaultMode: 'light',
      swatch: { light: ['#FFF7F0', '#2a1a0f', '#F26419'], dark: ['#1a1208', '#f3e6d6', '#FF8A3D'] },
      css: (m) => {
        const d = m === 'dark';
        const bg = d ? '#1a1208' : '#FFF7F0', fg = d ? '#e7d9c8' : '#3a2c1f', head = d ? '#fff3e6' : '#2a1a0f';
        const acc = 'var(--c-accent,#F26419)', mut = d ? '#a8967f' : '#8a7560';
        return `
        html{font-size:var(--s-base,18px);} body{background:${bg};color:var(--c-body,${fg});font-family:var(--f-body,'Noto Sans SC','DM Sans',sans-serif);line-height:1.85;}
        .md-h{font-family:var(--f-title,'Noto Sans SC','Space Grotesk',sans-serif);color:var(--c-title,${head});}
        .md-h1{font-size:2.2em;font-weight:700;margin:0 0 .5em;line-height:1.2;background:linear-gradient(100deg,${acc},#FFB23D);-webkit-background-clip:text;background-clip:text;color:transparent;}
        .md-h2{font-size:1.45em;font-weight:700;margin:1.6em 0 .5em;display:inline-block;} .md-h2::after{content:'';display:block;width:2.2em;height:5px;border-radius:3px;background:${acc};margin-top:.3em;}
        .md-h3{font-size:1.15em;font-weight:600;margin:1.3em 0 .4em;color:${acc};}
        .md-p{margin:1em 0;} .md-quote{background:${d?'#221708':'#fff0e2'};border:none;border-radius:14px;padding:1.1em 1.3em;margin:1.4em 0;color:${head};font-weight:500;box-shadow:0 6px 20px ${d?'#0000':'rgba(242,100,25,.08)'};}
        ${struct({accent:'#F26419',rule:d?'#2c2010':'#f3e3d2',border:d?'#352713':'#f0ddc8',muted:mut,headBg:d?'#241809':'#ffeada',headFg:head,zebra:d?'#1e1509':'#fffaf4',inlineBg:d?'#2a1c0c':'#ffe6d2',inlineFg:d?'#ffac6e':'#c44e12',codeBg:d?'#120c05':'#fff3e9',codeFg:d?'#e7d9c8':'#4a3826',codeBorder:d?'#2c2010':'#f2e0cd',codeBar:d?'#1a1208':'#ffeee0',calBg:d?'#221708':'#fff0e2',calBorder:d?'#33260f':'#f3ddc6'})}`;
      }
    },
    {
      id: 'academic', name: '学术论文', en: 'Academic Paper', use: '学术', tag: 'Academic',
      modes: ['light'], defaultMode: 'light',
      swatch: { light: ['#FCFCFA', '#1a1a1a', '#7A1F2B'] },
      css: () => {
        const acc = 'var(--c-accent,#7A1F2B)';
        return `
        html{font-size:var(--s-base,17px);} body{background:#FCFCFA;color:var(--c-body,#222);font-family:var(--f-body,'EB Garamond',Georgia,serif);line-height:1.7;}
        .md-content{max-width:none;}
        .md-h{font-family:var(--f-title,'EB Garamond',serif);color:var(--c-title,#1a1a1a);}
        .md-h1{font-size:1.95em;font-weight:600;text-align:center;margin:0 0 .8em;line-height:1.3;}
        .md-h2{font-size:1.3em;font-weight:600;margin:1.7em 0 .4em;} .md-h3{font-size:1.1em;font-weight:600;font-style:italic;margin:1.3em 0 .3em;}
        .md-p{margin:.6em 0;text-align:justify;text-indent:1.6em;} .md-h1+.md-p,.md-h2+.md-p,.md-h3+.md-p{text-indent:0;}
        .md-quote{font-size:.95em;color:#555;margin:1.2em 2em;border-left:2px solid #ccc;padding-left:1em;font-style:italic;}
        ${struct({accent:'#7A1F2B',rule:'#ececec',border:'#dcdcd6',muted:'#777',headBg:'#f4f3ef',headFg:'#1a1a1a',zebra:'#f9f9f6',inlineBg:'#f2f0eb',inlineFg:'#6a1a24',codeBg:'#f7f6f2',codeFg:'#333',codeBorder:'#e6e4dd',codeBar:'#f0eee8',calBg:'#f6f4ef',calBorder:'#e4e1d8'})}`;
      }
    },
    {
      id: 'wechat', name: '公众号文章', en: 'WeChat Article', use: '推文', tag: 'Article',
      modes: ['light', 'dark'], defaultMode: 'light',
      swatch: { light: ['#FFFFFF', '#2c2c2c', '#07A35A'], dark: ['#1a1a1a', '#d4d4d4', '#2BCB7E'] },
      css: (m) => {
        const d = m === 'dark';
        const bg = d ? '#1a1a1a' : '#FFFFFF', fg = d ? '#c9c9c9' : '#3f3f3f', head = d ? '#f0f0f0' : '#2c2c2c';
        const acc = 'var(--c-accent,#07A35A)', mut = d ? '#888' : '#999';
        return `
        html{font-size:var(--s-base,16.5px);} body{background:${bg};color:var(--c-body,${fg});font-family:var(--f-body,'Noto Sans SC',sans-serif);line-height:1.9;letter-spacing:.01em;}
        .md-h{font-family:var(--f-title,'Noto Sans SC',sans-serif);color:var(--c-title,${head});text-align:center;}
        .md-h1{font-size:1.55em;font-weight:700;margin:0 0 1em;} .md-h2{font-size:1.22em;font-weight:600;margin:1.8em 0 .7em;}
        .md-h2 span,.md-h2{display:block;} .md-h2::before{content:'';display:inline-block;width:.5em;height:1em;background:${acc};border-radius:2px;vertical-align:-.12em;margin-right:.5em;}
        .md-h3{font-size:1.05em;font-weight:600;margin:1.3em 0 .4em;text-align:left;color:${acc};}
        .md-p{margin:1.1em 0;} .md-quote{background:${d?'#141414':'#f7f7f7'};border-left:3px solid ${acc};padding:.8em 1em;margin:1.3em 0;color:${mut};font-size:.94em;border-radius:0 6px 6px 0;}
        ${struct({accent:'#07A35A',rule:d?'#262626':'#f0f0f0',border:d?'#2e2e2e':'#e6e6e6',muted:mut,headBg:d?'#202020':'#f5f5f5',headFg:head,zebra:d?'#1d1d1d':'#fafafa',inlineBg:d?'#172017':'#eafaf1',inlineFg:d?'#4cd494':'#06864a',codeBg:d?'#121212':'#f8f8f8',codeFg:d?'#c9c9c9':'#3a3a3a',codeBorder:d?'#262626':'#ededed',codeBar:d?'#1a1a1a':'#f2f2f2',calBg:d?'#161616':'#f6f8f6',calBorder:d?'#2a2a2a':'#e6ece8'})}`;
      }
    },
    {
      id: 'magazine', name: '杂志随笔', en: 'Magazine Essay', use: '杂志', tag: 'Editorial',
      modes: ['light', 'dark'], defaultMode: 'light',
      swatch: { light: ['#F7F4EF', '#1c1a17', '#B33A2B'], dark: ['#17150f', '#e6e0d4', '#D9604E'] },
      css: (m) => {
        const d = m === 'dark';
        const bg = d ? '#17150f' : '#F7F4EF', fg = d ? '#d8d2c4' : '#2c2823', head = d ? '#f3eee2' : '#1c1a17';
        const acc = 'var(--c-accent,#B33A2B)', mut = d ? '#9a9384' : '#8a8475';
        return `
        html{font-size:var(--s-base,18px);} body{background:${bg};color:var(--c-body,${fg});font-family:var(--f-body,'Lora',Georgia,serif);line-height:1.72;}
        .md-h{font-family:var(--f-title,'Playfair Display',serif);color:var(--c-title,${head});}
        .md-h1{font-size:2.6em;font-weight:900;line-height:1.08;margin:0 0 .5em;letter-spacing:-.01em;}
        .md-h2{font-size:1.55em;font-weight:700;margin:1.7em 0 .4em;} .md-h3{font-size:1.18em;font-weight:600;font-style:italic;margin:1.3em 0 .3em;}
        .md-p{margin:1em 0;} .md-h1+.md-p::first-letter,.md-content>.md-p:first-of-type::first-letter{font-family:var(--f-title,'Playfair Display',serif);font-size:3.1em;font-weight:900;float:left;line-height:.82;margin:.05em .12em 0 0;color:${acc};}
        .md-quote{font-family:var(--f-quote,'Playfair Display',serif);font-style:italic;font-size:1.45em;line-height:1.4;color:${head};border:none;border-top:2px solid ${acc};border-bottom:2px solid ${acc};padding:.7em 0;margin:1.6em 0;text-align:center;}
        ${struct({accent:'#B33A2B',rule:d?'#2a2619':'#e9e3d6',border:d?'#322d1f':'#e2dccc',muted:mut,headBg:d?'#211d13':'#efe9da',headFg:head,zebra:d?'#1c1810':'#f3efe6',inlineBg:d?'#251f12':'#efe7d6',inlineFg:d?'#e0826f':'#9c3023',codeBg:d?'#100e09':'#f2eee4',codeFg:d?'#d8d2c4':'#3a352c',codeBorder:d?'#2a2619':'#e6e0d0',codeBar:d?'#17150f':'#ece6da',calBg:d?'#201c12':'#f1ece0',calBorder:d?'#2e2a1c':'#e6dfce'})}`;
      }
    },
    {
      id: 'aireport', name: 'AI 报告', en: 'AI Report', use: 'AI', tag: 'AI',
      modes: ['light', 'dark'], defaultMode: 'dark',
      swatch: { dark: ['#0c0e1a', '#cdd2e6', '#7C5CFF'], light: ['#FAFAFF', '#16182a', '#6D4DFF'] },
      css: (m) => {
        const d = m === 'dark';
        const bg = d ? '#0c0e1a' : '#FAFAFF', fg = d ? '#c2c8de' : '#2a2d42', head = d ? '#f0f1fa' : '#16182a';
        const acc = 'var(--c-accent,#7C5CFF)', acc2 = d ? '#4DD0E1' : '#2BA8C4', mut = d ? '#7a809a' : '#6e7390';
        return `
        html{font-size:var(--s-base,16px);} body{background:${bg};color:var(--c-body,${fg});font-family:var(--f-body,'Space Grotesk','Noto Sans SC',sans-serif);line-height:1.68;}
        .md-h{font-family:var(--f-title,'Space Grotesk',sans-serif);color:var(--c-title,${head});letter-spacing:-.02em;}
        .md-h1{font-size:2em;font-weight:700;margin:0 0 .5em;background:linear-gradient(100deg,${acc},${acc2});-webkit-background-clip:text;background-clip:text;color:transparent;}
        .md-h2{font-size:1.32em;font-weight:600;margin:1.7em 0 .5em;} .md-h2::before{content:'';display:inline-block;width:.65em;height:.65em;border-radius:3px;background:linear-gradient(135deg,${acc},${acc2});margin-right:.5em;vertical-align:.02em;}
        .md-h3{font-size:1.08em;font-weight:600;margin:1.3em 0 .4em;color:${acc};}
        .md-p{margin:.85em 0;} .md-quote{background:${d?'linear-gradient(135deg,#13152a,#10131f)':'#f1f0ff'};border:1px solid ${d?'#23264a':'#e2e0ff'};border-left:3px solid ${acc};padding:.8em 1.1em;margin:1.2em 0;border-radius:8px;color:${mut};}
        ${struct({accent:'#7C5CFF',rule:d?'#1c1f38':'#ecebf7',border:d?'#262a48':'#e0def2',muted:mut,headBg:d?'#14172c':'#f2f1fb',headFg:head,zebra:d?'#0f1124':'#f8f8fe',inlineBg:d?'#1a1c38':'#efedff',inlineFg:d?'#a896ff':'#5a3ee0',codeBg:d?'#080a14':'#f6f5fc',codeFg:d?'#c2c8de':'#2c2f44',codeBorder:d?'#1e2240':'#e6e4f4',codeBar:d?'#0d0f1d':'#efeefa',calBg:d?'#12142a':'#f1f0ff',calBorder:d?'#24284a':'#e0ddf5'})}`;
      }
    },
    {
      id: 'euro', name: '欧式古典', en: 'European Classical', use: '古典', tag: 'Classic',
      modes: ['light', 'dark'], defaultMode: 'light',
      swatch: { light: ['#F6F1E7', '#2b2417', '#9C7A3C'], dark: ['#171309', '#e6dcc6', '#C9A85E'] },
      css: (m) => {
        const d = m === 'dark';
        const bg = d ? '#171309' : '#F6F1E7', fg = d ? '#ddd3bd' : '#2f2a1d', head = d ? '#f2ead6' : '#2b2417';
        const acc = 'var(--c-accent,#9C7A3C)', mut = d ? '#9b9077' : '#867b60';
        return `
        html{font-size:var(--s-base,18px);} body{background:${bg};color:var(--c-body,${fg});font-family:var(--f-body,'Cormorant Garamond','EB Garamond',serif);line-height:1.74;font-size:1.06rem;}
        .md-h{font-family:var(--f-title,'Playfair Display',serif);color:var(--c-title,${head});text-align:center;}
        .md-h1{font-size:2.5em;font-weight:700;margin:.2em 0 .1em;letter-spacing:.01em;}
        .md-h1::after{content:'❧';display:block;color:${acc};font-size:.5em;margin:.3em 0 .6em;font-weight:400;}
        .md-h2{font-size:1.6em;font-weight:600;margin:1.6em 0 .4em;font-variant:small-caps;letter-spacing:.04em;} 
        .md-h3{font-size:1.25em;font-weight:600;font-style:italic;margin:1.3em 0 .3em;}
        .md-p{margin:.85em 0;} .md-p:first-of-type{text-align:justify;}
        .md-quote{font-family:var(--f-quote,'Cormorant Garamond',serif);font-style:italic;font-size:1.3em;text-align:center;color:${acc};border:none;margin:1.5em 2em;line-height:1.5;}
        .md-quote::before{content:'“';font-size:1.4em;vertical-align:-.3em;} .md-quote::after{content:'”';font-size:1.4em;vertical-align:-.5em;}
        ${struct({accent:'#9C7A3C',rule:d?'#2c2614':'#e6dcc4',border:d?'#352d18':'#ddd0b2',muted:mut,headBg:d?'#221c0e':'#ede3cc',headFg:head,zebra:d?'#1c1709':'#f1ebdc',inlineBg:d?'#26200f':'#ece1c8',inlineFg:d?'#d3b06a':'#876225',codeBg:d?'#100d06':'#f0ead9',codeFg:d?'#ddd3bd':'#3a342a',codeBorder:d?'#2c2614':'#e2d8c0',codeBar:d?'#171309':'#e9e1cf',calBg:d?'#201a0d':'#efe7d2',calBorder:d?'#2e2713':'#e2d6ba'})}`;
      }
    },
    {
      id: 'cnclassic', name: '中式古典', en: 'Chinese Classical', use: '国风', tag: 'Classic',
      modes: ['light', 'dark'], defaultMode: 'light',
      swatch: { light: ['#F3ECE0', '#2a211a', '#9E2B25'], dark: ['#16110d', '#e4d9c8', '#C8463F'] },
      css: (m) => {
        const d = m === 'dark';
        const bg = d ? '#16110d' : '#F3ECE0', fg = d ? '#d8cdb9' : '#3a3026', head = d ? '#efe4d2' : '#2a211a';
        const acc = 'var(--c-accent,#9E2B25)', mut = d ? '#988c78' : '#857a66';
        return `
        html{font-size:var(--s-base,18px);} body{background:${bg};color:var(--c-body,${fg});font-family:var(--f-body,'Noto Serif SC',serif);line-height:1.95;}
        .md-h{font-family:var(--f-title,'Ma Shan Zheng','Noto Serif SC',serif);color:var(--c-title,${head});}
        .md-h1{font-size:2.3em;font-weight:400;text-align:center;margin:.3em 0 .6em;letter-spacing:.08em;}
        .md-h1::after{content:'';display:block;width:3em;height:3px;background:${acc};margin:.4em auto 0;}
        .md-h2{font-size:1.5em;font-weight:600;font-family:var(--f-title,'Noto Serif SC',serif);margin:1.7em 0 .5em;padding-left:.6em;border-left:5px double ${acc};letter-spacing:.05em;}
        .md-h3{font-size:1.18em;font-weight:600;margin:1.3em 0 .4em;color:${acc};letter-spacing:.04em;}
        .md-p{margin:1em 0;text-indent:2em;letter-spacing:.03em;} .md-h1+.md-p,.md-h2+.md-p{text-indent:0;}
        .md-quote{font-family:var(--f-quote,'Noto Serif SC',serif);color:${acc};border:none;padding:.5em 1.5em;margin:1.4em 0;position:relative;font-size:1.05em;background:${d?'#1c1610':'#ece2d2'};border-radius:4px;}
        ${struct({accent:'#9E2B25',rule:d?'#28201a':'#e4d8c5',border:d?'#312820':'#dccfb9',muted:mut,headBg:d?'#1f1812':'#ebe0cd',headFg:head,zebra:d?'#1a140f':'#efe6d7',inlineBg:d?'#241a14':'#ecdfca',inlineFg:d?'#d96058':'#8a201b',codeBg:d?'#100c08':'#eee3d1',codeFg:d?'#d8cdb9':'#3a322a',codeBorder:d?'#28201a':'#e0d4bf',codeBar:d?'#16110d':'#e7dcc9',calBg:d?'#1e1711':'#ece1ce',calBorder:d?'#2c241c':'#e0d3bc'})}`;
      }
    },
    {
      id: 'cnvertical', name: '中式传统·竖排', en: 'Chinese Vertical', use: '竖排', tag: 'Vertical',
      modes: ['light', 'dark'], defaultMode: 'light',
      swatch: { light: ['#EFE7D6', '#241c12', '#8C2820'], dark: ['#15110b', '#e0d4be', '#C2453B'] },
      vertical: true,
      css: (m) => {
        const d = m === 'dark';
        const bg = d ? '#15110b' : '#EFE7D6', fg = d ? '#ddd1b9' : '#2c241a', head = d ? '#efe3cd' : '#241c12';
        const acc = 'var(--c-accent,#8C2820)', mut = d ? '#988b74' : '#7d7159';
        return `
        html{font-size:var(--s-base,19px);}
        body{background:${bg};color:var(--c-body,${fg});font-family:var(--f-body,'Noto Serif SC',serif);}
        .md-content{writing-mode:vertical-rl;-webkit-writing-mode:vertical-rl;text-orientation:upright;height:100%;line-height:2.2;letter-spacing:.12em;padding:8px 4px;}
        .md-content::-webkit-scrollbar{height:6px;}
        .md-h{font-family:var(--f-title,'Ma Shan Zheng','Noto Serif SC',serif);color:var(--c-title,${head});}
        .md-h1{font-size:2em;font-weight:400;margin:0 0 0 .4em;letter-spacing:.18em;border-right:3px solid ${acc};padding-right:.3em;}
        .md-h2{font-size:1.4em;font-weight:600;margin:0 0 0 .3em;color:${acc};letter-spacing:.1em;}
        .md-h3{font-size:1.15em;font-weight:600;margin:0 0 0 .2em;}
        .md-p{margin:0 0 0 .2em;text-orientation:upright;}
        .md-quote{border:none;border-right:3px solid ${acc};padding:0 .4em 0 0;margin:0 .3em 0 0;color:${mut};font-style:normal;}
        .md-table-wrap,.md-pre{writing-mode:horizontal-tb;text-orientation:mixed;}
        ${struct({accent:'#8C2820',rule:d?'#271f15':'#e0d3bc',border:d?'#302619':'#d6c8af',muted:mut,headBg:d?'#1e1710':'#e7dbc6',headFg:head,zebra:d?'#191309':'#ebe1cf',inlineBg:d?'#231910':'#e8dac3',inlineFg:d?'#d35a50':'#7d201a',codeBg:d?'#100b07':'#eadfca',codeFg:d?'#ddd1b9':'#352c22',codeBorder:d?'#271f15':'#dccfb6',codeBar:d?'#15110b':'#e3d8c2',calBg:d?'#1d160f':'#e8ddc8',calBorder:d?'#2b2317':'#dccdb3'})}`;
      }
    },
    {
      id: 'poster', name: '前卫海报', en: 'Avant-garde Poster', use: '海报', tag: 'Bold',
      modes: ['light', 'dark'], defaultMode: 'dark',
      swatch: { dark: ['#0a0a0a', '#fafafa', '#FF4D00'], light: ['#FAFA00', '#0a0a0a', '#FF2D00'] },
      css: (m) => {
        const d = m === 'dark';
        const bg = d ? '#0a0a0a' : '#F2F000', fg = d ? '#f0f0f0' : '#0a0a0a', head = d ? '#ffffff' : '#0a0a0a';
        const acc = 'var(--c-accent,' + (d ? '#FF4D00' : '#FF2D00') + ')', mut = d ? '#888' : '#444';
        return `
        html{font-size:var(--s-base,16px);} body{background:${bg};color:var(--c-body,${fg});font-family:var(--f-body,'Archivo','Noto Sans SC',sans-serif);line-height:1.55;}
        .md-h{font-family:var(--f-title,'Anton','Archivo',sans-serif);color:var(--c-title,${head});text-transform:uppercase;line-height:1.0;}
        .md-h1{font-size:3.4em;font-weight:400;margin:0 0 .5em;padding-bottom:.08em;letter-spacing:-.01em;}
        .md-h1::first-line{color:${acc};}
        .md-h2{font-size:1.9em;font-weight:400;margin:1.3em 0 .3em;background:${acc};color:${d?'#0a0a0a':'#F2F000'};display:inline-block;padding:.05em .3em;transform:rotate(-1deg);}
        .md-h3{font-size:1.25em;font-weight:900;font-family:var(--f-title,'Archivo',sans-serif);margin:1.2em 0 .3em;text-decoration:underline;text-decoration-color:${acc};text-decoration-thickness:3px;}
        .md-p{margin:.8em 0;font-weight:500;} .md-quote{font-family:var(--f-quote,'Anton',sans-serif);text-transform:uppercase;font-size:1.7em;line-height:1.05;border:none;border-top:4px solid ${acc};border-bottom:4px solid ${acc};padding:.4em 0;margin:1.4em 0;color:${acc};}
        ${struct({accent:acc.includes('var')? (d?'#FF4D00':'#FF2D00'):acc,rule:d?'#222':'#0a0a0a44',border:d?'#2a2a2a':'#0a0a0a',muted:mut,headBg:d?'#161616':'#0a0a0a',headFg:d?'#fff':'#F2F000',zebra:d?'#111':'#f2f00022',inlineBg:d?'#1a1a1a':'#0a0a0a',inlineFg:d?'#FF6A33':'#F2F000',codeBg:d?'#050505':'#0a0a0a',codeFg:d?'#f0f0f0':'#F2F000',codeBorder:d?'#2a2a2a':'#0a0a0a',codeBar:d?'#141414':'#0a0a0a',calBg:d?'#141414':'#0a0a0a18',calBorder:d?'#2a2a2a':'#0a0a0a'})}`;
      }
    },
  ];

  const THEME_MAP = {};
  THEMES.forEach(t => { THEME_MAP[t.id] = t; });

  function buildThemeDoc(themeId, mdHtml, opts) {
    opts = opts || {};
    const t = THEME_MAP[themeId] || THEMES[0];
    const mode = opts.mode && t.modes.includes(opts.mode) ? opts.mode : t.defaultMode;
    const ov = opts.ov || {};

    // Sync the on-demand font base for this render session
    const fontSrc = opts.fontSource || 'google';
    _gfBase = (fontSrc === 'local') ? FONT_BASES.google : (FONT_BASES[fontSrc] || FONT_BASES.google);

    let rootVars = '';
    if (ov.fonts) {
      const f = ov.fonts;
      if (f.title) rootVars += `--f-title:${f.title};`;
      if (f.body) rootVars += `--f-body:${f.body};`;
      if (f.quote) rootVars += `--f-quote:${f.quote};`;
      if (f.code) rootVars += `--f-code:${f.code};`;
      if (f.table) rootVars += `--f-table:${f.table};`;
    }
    if (ov.baseSize) rootVars += `--s-base:${ov.baseSize}px;`;
    if (ov.colors) {
      if (ov.colors.title) rootVars += `--c-title:${ov.colors.title};`;
      if (ov.colors.body) rootVars += `--c-body:${ov.colors.body};`;
      if (ov.colors.accent) rootVars += `--c-accent:${ov.colors.accent};`;
    }
    const pad = opts.pad != null ? opts.pad : 22;
    const gjs = opts.interactive ? `<script>${GESTURE_JS}<\/script>` : '';
    const extra = gfLink(opts.extraFonts);
    return `<!DOCTYPE html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
${getFontsHtml(fontSrc)}${extra}
<style>
*,*::before,*::after{box-sizing:border-box;margin:0;padding:0;}
html{-webkit-font-smoothing:antialiased;-moz-osx-font-smoothing:grayscale;${rootVars}}
body{padding:${pad}px;min-height:100%;}
.md-content{max-width:780px;margin:0 auto;}
img{max-width:100%;}
.acks-hl{background:rgba(242,100,25,.28);border-radius:3px;}
.acks-find{background:rgba(242,100,25,.32);border-radius:2px;}
.acks-find-active{background:#F26419;color:#fff;border-radius:2px;box-shadow:0 0 0 2px rgba(242,100,25,.4);}
${t.css(mode)}
</style></head>
<body><div class="md-content">${mdHtml}</div>${gjs}</body></html>`;
  }

  window.THEMES = THEMES;
  window.THEME_MAP = THEME_MAP;
  window.buildThemeDoc = buildThemeDoc;
  window.THEME_FONTS_HREF = FONTS;

  // ---- capture-mode helpers: render theme into LIGHT DOM (no iframe) so html-to-image can snapshot it ----
  function rootVarsOf(ov) {
    ov = ov || {}; let v = '';
    if (ov.fonts) { const f = ov.fonts; if (f.title) v += `--f-title:${f.title};`; if (f.body) v += `--f-body:${f.body};`; if (f.quote) v += `--f-quote:${f.quote};`; if (f.code) v += `--f-code:${f.code};`; if (f.table) v += `--f-table:${f.table};`; }
    if (ov.baseSize) v += `--s-base:${ov.baseSize}px;`;
    if (ov.colors) { if (ov.colors.title) v += `--c-title:${ov.colors.title};`; if (ov.colors.body) v += `--c-body:${ov.colors.body};`; if (ov.colors.accent) v += `--c-accent:${ov.colors.accent};`; }
    return v;
  }
  // scope a flat theme css block to a container selector; html/body -> container
  function scopeThemeCss(css, sel) {
    return css.replace(/([^{}]+)\{([^}]*)\}/g, (m, s, b) => {
      return s.split(',').map(x => {
        x = x.trim(); if (!x) return x;
        if (x === 'html' || x === 'body') return sel;
        return sel + ' ' + x;
      }).join(',') + '{' + b + '}';
    });
  }
  // returns { css, rootVars } for a scoped container; caller injects <style>css</style> + container with inline rootVars
  window.buildThemeScoped = function (themeId, opts) {
    opts = opts || {};
    const t = THEME_MAP[themeId] || THEMES[0];
    const mode = opts.mode && t.modes.includes(opts.mode) ? opts.mode : t.defaultMode;
    const sel = opts.sel || '.acks-cap';
    const base = `${sel}{-webkit-font-smoothing:antialiased;}
${sel} *,${sel} *::before,${sel} *::after{box-sizing:border-box;margin:0;padding:0;}
${sel} .md-content{max-width:780px;margin:0 auto;}
${sel} img{max-width:100%;}
${sel} .acks-find{background:rgba(242,100,25,.32);border-radius:2px;}
${sel} .acks-find-active{background:#F26419;color:#fff;border-radius:2px;}`;
    return { css: base + scopeThemeCss(t.css(mode), sel), rootVars: rootVarsOf(opts.ov), mode };
  };
  window.ensureGoogleFont = ensureGoogleFont;

  // injected into the rendered doc when opts.interactive — long-press select, pinch / dbl-tap zoom
  const GESTURE_JS = `
(function(){
  var doc=document, de=doc.documentElement;
  var base=parseFloat(getComputedStyle(de).fontSize)||16, zoom=1;
  function applyZoom(z){ zoom=Math.max(0.8,Math.min(2.4,z)); de.style.fontSize=(base*zoom)+'px'; try{parent.postMessage({__acks:'zoom',zoom:zoom},'*');}catch(e){} }
  function dist(t){ return Math.hypot(t[0].clientX-t[1].clientX, t[0].clientY-t[1].clientY); }
  var sd=0, sz=1;
  doc.addEventListener('touchstart',function(e){ if(e.touches.length===2){ sd=dist(e.touches); sz=zoom; clearLP(); } },{passive:true});
  doc.addEventListener('touchmove',function(e){ if(e.touches.length===2&&sd){ e.preventDefault(); applyZoom(sz*dist(e.touches)/sd); } },{passive:false});
  doc.addEventListener('touchend',function(e){ if(e.touches.length<2) sd=0; },{passive:true});
  doc.addEventListener('wheel',function(e){ if(e.ctrlKey){ e.preventDefault(); applyZoom(zoom-e.deltaY*0.004); } },{passive:false});
  var lastTap=0;
  doc.addEventListener('click',function(e){ var n=Date.now(); if(n-lastTap<320){ applyZoom(zoom>1.05?1:1.5); } lastTap=n; });
  var lp=null, dp=null;
  function clearHL(){ var o=doc.querySelectorAll('.acks-hl'); for(var i=0;i<o.length;i++){ var p=o[i].parentNode; p.replaceChild(doc.createTextNode(o[i].textContent),o[i]); if(p.normalize)p.normalize(); } }
  function clearLP(){ if(lp){ clearTimeout(lp); lp=null; } }
  function wordAt(x,y){
    var range=doc.caretRangeFromPoint?doc.caretRangeFromPoint(x,y):null;
    if(!range||range.startContainer.nodeType!==3) return null;
    var node=range.startContainer, text=node.textContent, i=range.startOffset;
    var isW=function(c){ return c&&/[^\\s.,;:!?，。；：！？、（）()]/.test(c); };
    var a=i,b=i; while(a>0&&isW(text[a-1]))a--; while(b<text.length&&isW(text[b]))b++;
    if(b<=a) return null; return {node:node,a:a,b:b,word:text.slice(a,b)};
  }
  function longPress(x,y){
    clearHL(); var w=wordAt(x,y); if(!w) return;
    try{
      var rng=doc.createRange(); rng.setStart(w.node,w.a); rng.setEnd(w.node,w.b);
      var sp=doc.createElement('span'); sp.className='acks-hl'; rng.surroundContents(sp);
      var rc=sp.getBoundingClientRect();
      try{ if(navigator.vibrate)navigator.vibrate(12); }catch(e){}
      parent.postMessage({__acks:'longpress',word:w.word,x:rc.left+rc.width/2,top:rc.top,bottom:rc.bottom},'*');
    }catch(err){}
  }
  doc.addEventListener('touchstart',function(e){ if(e.touches.length===1){ var t=e.touches[0]; dp={x:t.clientX,y:t.clientY}; lp=setTimeout(function(){ longPress(dp.x,dp.y); },470); } },{passive:true});
  doc.addEventListener('touchmove',function(e){ if(lp&&dp&&e.touches[0]){ var t=e.touches[0]; if(Math.abs(t.clientX-dp.x)>8||Math.abs(t.clientY-dp.y)>8) clearLP(); } },{passive:true});
  doc.addEventListener('touchend',clearLP,{passive:true});
  doc.addEventListener('mousedown',function(e){ dp={x:e.clientX,y:e.clientY}; lp=setTimeout(function(){ longPress(dp.x,dp.y); },470); });
  doc.addEventListener('mousemove',function(e){ if(lp&&dp&&(Math.abs(e.clientX-dp.x)>8||Math.abs(e.clientY-dp.y)>8)) clearLP(); });
  doc.addEventListener('mouseup',clearLP);
  window.addEventListener('message',function(e){
    var d=e.data; if(!d||!d.__acks) return;
    if(d.__acks==='clearhl') clearHL();
    if(d.__acks==='resetzoom') applyZoom(1);
    if(d.__acks==='search') doSearch(d.q);
    if(d.__acks==='findnav') findNav(d.dir);
    if(d.__acks==='clearfind') clearFind();
    if(d.__acks==='gotoHeading') gotoHeading(d.i);
    if(d.__acks==='trackHeadings') startTrack();
  });

  // ---- in-document search ----
  var marks=[], cur=-1;
  function clearFind(){
    for(var i=0;i<marks.length;i++){ var p=marks[i].parentNode; if(p){ p.replaceChild(doc.createTextNode(marks[i].textContent),marks[i]); p.normalize&&p.normalize(); } }
    marks=[]; cur=-1;
  }
  function doSearch(q){
    clearFind();
    if(!q||q.length<1){ parent.postMessage({__acks:'searchres',count:0,cur:0},'*'); return; }
    var ql=q.toLowerCase();
    var walker=doc.createTreeWalker(doc.querySelector('.md-content'),NodeFilter.SHOW_TEXT,{acceptNode:function(n){return n.nodeValue&&n.nodeValue.trim()&&n.parentNode&&!/(SCRIPT|STYLE)/.test(n.parentNode.tagName)?NodeFilter.FILTER_ACCEPT:NodeFilter.FILTER_REJECT;}});
    var nodes=[],n; while(n=walker.nextNode()) nodes.push(n);
    nodes.forEach(function(node){
      var t=node.nodeValue, tl=t.toLowerCase(), idx=0, hits=[];
      while((idx=tl.indexOf(ql,idx))!==-1){ hits.push(idx); idx+=ql.length; }
      if(!hits.length) return;
      var frag=doc.createDocumentFragment(), last=0;
      hits.forEach(function(h){
        if(h>last) frag.appendChild(doc.createTextNode(t.slice(last,h)));
        var m=doc.createElement('mark'); m.className='acks-find'; m.textContent=t.slice(h,h+ql.length); frag.appendChild(m); marks.push(m); last=h+ql.length;
      });
      if(last<t.length) frag.appendChild(doc.createTextNode(t.slice(last)));
      node.parentNode.replaceChild(frag,node);
    });
    if(marks.length){ cur=0; activateFind(); }
    parent.postMessage({__acks:'searchres',count:marks.length,cur:marks.length?1:0},'*');
  }
  function activateFind(){
    marks.forEach(function(m,i){ m.className=i===cur?'acks-find-active':'acks-find'; });
    if(marks[cur]) marks[cur].scrollIntoView({block:'center',behavior:'smooth'});
    parent.postMessage({__acks:'searchres',count:marks.length,cur:cur+1},'*');
  }
  function findNav(dir){ if(!marks.length) return; cur=(cur+dir+marks.length)%marks.length; activateFind(); }

  // ---- TOC: heading tracking + jump ----
  function headings(){ return [].slice.call(doc.querySelectorAll('.md-h1,.md-h2,.md-h3')); }
  function gotoHeading(i){ var hs=headings(); if(hs[i]) hs[i].scrollIntoView({block:'start',behavior:'smooth'}); }
  var tracking=false;
  function startTrack(){
    if(tracking) return; tracking=true;
    function report(){
      var hs=headings(), active=0, mid=de.scrollTop+60;
      for(var i=0;i<hs.length;i++){ if(hs[i].offsetTop<=mid) active=i; else break; }
      parent.postMessage({__acks:'activehead',i:active},'*');
    }
    (doc.scrollingElement?window:doc).addEventListener('scroll',report,{passive:true});
    report();
  }
})();
`;
})();
