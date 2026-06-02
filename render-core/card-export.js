// card-export.js — 图文卡片分页引擎（单位估算法，无 DOM layout 触发）
// window.buildCardPages(markdown, opts) → JSON string
(function () {
  var CARD_W = 540;
  var CARD_H = 720;
  var CARD_PAD = 28;
  var BOTTOM_BASE = 60;   // 底部固定基础边距，防止内容贴底，不受 padPx 影响

  // 每页容量（内容单位数）按密度
  var PAGE_CAP = { loose: 16, balanced: 22, dense: 30 };

  // ── 内容单位估算（不触发 layout）────────────────────────────────────────────
  // 原则：一行正文约 1 单位；标题、代码块、图表按视觉高度估算
  function estimateUnits(el) {
    var cls = (el.className || '');
    var text = el.textContent || '';
    var chars = text.length;
    var lines = Math.max(1, text.split('\n').length);

    if (cls.indexOf('md-h1') >= 0) return 5;
    if (cls.indexOf('md-h2') >= 0) return 3;
    if (cls.indexOf('md-h3') >= 0) return 2;
    if (cls.indexOf('md-mermaid') >= 0) return 8;
    if (cls.indexOf('md-math-block') >= 0) return 3;
    if (cls.indexOf('md-pre') >= 0) return Math.max(3, Math.min(Math.ceil(lines * 0.65), 14));
    if (cls.indexOf('md-table') >= 0) {
      // count rows from inner tr elements
      var rows = el.querySelectorAll ? el.querySelectorAll('tr').length : lines;
      return Math.max(3, Math.min(Math.ceil(rows * 1.4), 18));
    }
    if (cls.indexOf('md-quote') >= 0) return Math.max(2, Math.ceil(chars / 100));
    if (cls.indexOf('md-ul') >= 0 || cls.indexOf('md-ol') >= 0 || cls.indexOf('md-tasklist') >= 0) {
      var items = el.querySelectorAll ? el.querySelectorAll('li').length : 2;
      return Math.max(2, Math.ceil(items * 1.1 + chars / 200));
    }
    if (cls.indexOf('md-img') >= 0) return 6;
    if (cls.indexOf('md-hr') >= 0) return 1;
    // 普通段落：按字符数估算行数
    return Math.max(1, Math.ceil(chars / 110));
  }

  // ── 分页核心（纯 innerHTML 解析，不测量 layout）──────────────────────────────
  function paginate(mdHtml, cap) {
    // 解析 HTML 块（只解析 innerHTML，不插入 DOM，不触发 layout）
    var scratch = document.createElement('div');
    scratch.innerHTML = mdHtml;
    var blocks = Array.from(scratch.children);

    var pages = [], cur = [], curUnits = 0;

    for (var i = 0; i < blocks.length; i++) {
      var block = blocks[i];
      var units = estimateUnits(block);
      var isHeading = /md-h[123]/.test(block.className || '');

      // 防止标题孤悬：标题 + 下一块加起来放得下，但当前页放不下时，强制翻页
      if (isHeading && i + 1 < blocks.length) {
        var nextUnits = estimateUnits(blocks[i + 1]);
        if (cur.length > 0 && curUnits + units + nextUnits > cap && units + nextUnits <= cap) {
          pages.push(cur); cur = []; curUnits = 0;
        }
      }

      // 单块超出整页容量 → 单独一页
      if (units > cap && cur.length === 0) {
        pages.push([block.outerHTML]);
        continue;
      }
      // 加入后会溢出 → 翻页
      if (cur.length > 0 && curUnits + units > cap) {
        pages.push(cur); cur = []; curUnits = 0;
      }
      cur.push(block.outerHTML);
      curUnits += units;
    }
    if (cur.length > 0) pages.push(cur);
    return pages;
  }

  // ── 封面卡 HTML ──────────────────────────────────────────────────────────────
  function coverHtml(title, subtitle, themeId, fontSrc, padPx) {
    padPx = padPx || CARD_PAD;
    var t = window.THEME_MAP && window.THEME_MAP[themeId];
    var mode = t ? t.defaultMode : 'dark';
    var sw = t ? (mode === 'dark' ? t.swatch.dark : t.swatch.light) : null;
    var bg  = sw ? sw[0] : '#0D0F1A';
    var fg  = sw ? sw[1] : '#FFFFFF';
    var acc = sw ? sw[2] : '#F26419';
    var fonts = window.getFontsHtml ? window.getFontsHtml(fontSrc || 'local') : '';

    return '<!DOCTYPE html><html><head><meta charset="utf-8">' +
      '<meta name="viewport" content="width=' + CARD_W + ',initial-scale=1">' +
      fonts +
      '<style>' +
      '*{box-sizing:border-box;margin:0;padding:0;}' +
      'html,body{width:' + CARD_W + 'px;max-width:' + CARD_W + 'px;height:' + CARD_H + 'px;' +
        'overflow:hidden;word-break:break-word;overflow-wrap:break-word;' +
        'background:' + bg + ';font-family:\'DM Sans\',\'Noto Sans SC\',sans-serif;}' +
      'body{display:flex;flex-direction:column;justify-content:center;align-items:center;' +
        'padding:' + padPx + 'px;text-align:center;position:relative;}' +
      '.bar{width:48px;height:4px;background:' + acc + ';border-radius:2px;margin:0 auto 24px;}' +
      '.title{font-size:2em;font-weight:700;line-height:1.2;color:' + fg + ';' +
        'letter-spacing:-.02em;margin-bottom:14px;}' +
      '.sub{font-size:.95em;color:' + fg + '88;line-height:1.5;}' +
      '.deco{position:absolute;width:22px;height:22px;border-color:' + acc + ';border-style:solid;}' +
      '.tl{top:' + padPx + 'px;left:' + padPx + 'px;border-width:2.5px 0 0 2.5px;}' +
      '.br{bottom:' + padPx + 'px;right:' + padPx + 'px;border-width:0 2.5px 2.5px 0;}' +
      '.tag{position:absolute;bottom:' + padPx + 'px;left:50%;transform:translateX(-50%);' +
        'font-size:10px;color:' + fg + '40;letter-spacing:.08em;}' +
      '</style></head><body>' +
      '<div class="deco tl"></div>' +
      '<div class="deco br"></div>' +
      '<div class="bar"></div>' +
      '<div class="title">' + esc(title) + '</div>' +
      (subtitle ? '<div class="sub">' + esc(subtitle) + '</div>' : '') +
      '<div class="tag">ACKS READER</div>' +
      '</body></html>';
  }

  // ── 内容卡 HTML ──────────────────────────────────────────────────────────────
  function contentHtml(blocksHtml, index, total, themeId, fontSrc, fontSizePx, padPx) {
    fontSizePx = fontSizePx || 18;
    padPx = padPx || CARD_PAD;
    var t = window.THEME_MAP && window.THEME_MAP[themeId];
    if (!t) t = window.THEMES && window.THEMES[0];
    if (!t) return '<html><body>' + blocksHtml + '</body></html>';

    var mode = t.defaultMode;
    var fonts = window.getFontsHtml ? window.getFontsHtml(fontSrc || 'local') : '';
    var themeCss = t.css(mode);

    return '<!DOCTYPE html><html><head><meta charset="utf-8">' +
      '<meta name="viewport" content="width=' + CARD_W + ',initial-scale=1">' +
      fonts +
      '<style>' +
      '*,*::before,*::after{box-sizing:border-box;margin:0;padding:0;}' +
      'html{font-size:' + fontSizePx + 'px;-webkit-font-smoothing:antialiased;--s-base:' + fontSizePx + 'px;}' +
      'body{padding:' + padPx + 'px ' + padPx + 'px ' + BOTTOM_BASE + 'px ' + padPx + 'px;' +
        'width:' + CARD_W + 'px;max-width:' + CARD_W + 'px;' +
        'min-height:' + CARD_H + 'px;' +
        'overflow:hidden;}' +
      '.md-content{width:100%;max-width:100%;}' +
      'img{max-width:100%;height:auto;}' +
      themeCss +
      // ── 卡片覆盖层：钉死字号上限，强制所有内容换行不溢出 ──────────────────
      // 1vw = 5.4px（viewport=540px）
      '.md-h{word-break:break-word!important;overflow-wrap:break-word!important;' +
        'white-space:normal!important;max-width:100%!important;}' +
      // h1 最大 6vw≈32px，防止巨型标题撑爆卡片
      '.md-h1{font-size:min(1.85em,6vw)!important;line-height:1.25!important;' +
        'margin:.1em 0 .4em!important;word-break:break-word!important;' +
        'white-space:normal!important;letter-spacing:-.01em!important;}' +
      // h2 强制 block（poster 主题是 inline-block 会无限延伸）
      '.md-h2{font-size:min(1.4em,5vw)!important;display:block!important;' +
        'margin:.6em 0 .3em!important;word-break:break-word!important;' +
        'white-space:normal!important;width:auto!important;max-width:100%!important;}' +
      '.md-h3{font-size:min(1.1em,4.2vw)!important;margin:.5em 0 .25em!important;' +
        'word-break:break-word!important;}' +
      // 正文 & 引用
      '.md-p{margin:.45em 0!important;word-break:break-word!important;' +
        'overflow-wrap:break-word!important;}' +
      '.md-quote{font-size:min(1em,4vw)!important;white-space:normal!important;' +
        'word-break:break-word!important;margin:.55em 0!important;}' +
      // 列表
      '.md-ul,.md-ol{margin:.4em 0!important;}' +
      '.md-ul li,.md-ol li{word-break:break-word!important;overflow-wrap:break-word!important;}' +
      // 代码块：强制换行，缩小字号
      '.md-pre{margin:.6em 0!important;overflow:hidden!important;}' +
      '.md-pre code{white-space:pre-wrap!important;word-break:break-all!important;' +
        'font-size:.76em!important;}' +
      '.md-code{word-break:break-all!important;}' +
      // 表格：固定布局 + 缩小字号，防止列溢出
      '.md-table-wrap{overflow:hidden!important;margin:.55em 0!important;}' +
      '.md-table{font-size:.72em!important;table-layout:fixed!important;' +
        'width:100%!important;word-break:break-word!important;}' +
      '.md-table th,.md-table td{word-break:break-word!important;' +
        'overflow-wrap:break-word!important;overflow:hidden!important;}' +
      // 页码
      '.pn{position:fixed;bottom:10px;right:14px;font-size:10px;' +
        'opacity:.38;font-family:\'DM Sans\',sans-serif;letter-spacing:.04em;}' +
      '</style></head>' +
      '<body><div class="md-content">' + blocksHtml + '</div>' +
      '<div class="pn">' + index + ' / ' + total + '</div>' +
      '</body></html>';
  }

  function esc(s) {
    return String(s)
      .replace(/&/g,'&amp;').replace(/</g,'&lt;')
      .replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  }

  // ── 逐页渲染模式 API（v2）────────────────────────────────────────────────────

  // 1. 提取块列表（在 host WebView 里调用）→ JSON [{idx, html}, ...]
  //    data-bidx 属性打在块元素本身上，保留 CSS margin collapsing
  window.extractCardBlocks = function(markdown, opts) {
    var mdHtml = window.renderMarkdown ? window.renderMarkdown(markdown) : markdown;
    var scratch = document.createElement('div');
    scratch.innerHTML = mdHtml;
    var result = [];
    for (var i = 0; i < scratch.children.length; i++) {
      var el = scratch.children[i];
      el.setAttribute('data-bidx', String(i));
      result.push({ idx: i, html: el.outerHTML });
    }
    return JSON.stringify(result);
  };

  // 2. 构建测量文档（在 host WebView 里调用）→ HTML 字符串（无高度限制）
  window.buildMeasurementHtml = function(blocksJsonStr, opts) {
    opts = opts || {};
    var themeId   = opts.themeId   || 'aireport';
    var fontSrc   = opts.fontSource || 'local';
    var fontSizePx = opts.fontSizePx || 18;
    var padPx     = opts.padPx     || CARD_PAD;
    var t = (window.THEME_MAP && window.THEME_MAP[themeId]) || (window.THEMES && window.THEMES[0]);
    var mode = t ? t.defaultMode : 'dark';
    var themeCss = (t && t.css) ? t.css(mode) : '';
    var fonts = window.getFontsHtml ? window.getFontsHtml(fontSrc) : '';
    var blocks;
    try { blocks = JSON.parse(blocksJsonStr); } catch(e) { blocks = []; }
    var blocksHtml = blocks.map(function(b) { return b.html; }).join('');

    return '<!DOCTYPE html><html><head><meta charset="utf-8">' +
      '<meta name="viewport" content="width=' + CARD_W + ',initial-scale=1">' +
      fonts +
      '<style>' +
      '*,*::before,*::after{box-sizing:border-box;margin:0;padding:0;}' +
      'html{font-size:' + fontSizePx + 'px;-webkit-font-smoothing:antialiased;}' +
      'body{padding:' + padPx + 'px ' + padPx + 'px ' + BOTTOM_BASE + 'px ' + padPx + 'px;width:' + CARD_W + 'px;max-width:' + CARD_W + 'px;}' +
      'img{max-width:100%;height:auto;}' +
      '.md-content{width:100%;max-width:100%;}' +
      themeCss +
      '.md-h{word-break:break-word!important;overflow-wrap:break-word!important;white-space:normal!important;max-width:100%!important;}' +
      '.md-h1{font-size:min(1.85em,6vw)!important;line-height:1.25!important;margin:.1em 0 .4em!important;word-break:break-word!important;white-space:normal!important;letter-spacing:-.01em!important;}' +
      '.md-h2{font-size:min(1.4em,5vw)!important;display:block!important;margin:.6em 0 .3em!important;word-break:break-word!important;white-space:normal!important;width:auto!important;max-width:100%!important;}' +
      '.md-h3{font-size:min(1.1em,4.2vw)!important;margin:.5em 0 .25em!important;word-break:break-word!important;}' +
      '.md-p{margin:.45em 0!important;word-break:break-word!important;overflow-wrap:break-word!important;}' +
      '.md-quote{font-size:min(1em,4vw)!important;white-space:normal!important;word-break:break-word!important;margin:.55em 0!important;}' +
      '.md-ul,.md-ol{margin:.4em 0!important;}' +
      '.md-ul li,.md-ol li{word-break:break-word!important;overflow-wrap:break-word!important;}' +
      '.md-pre{margin:.6em 0!important;overflow:hidden!important;}' +
      '.md-pre code{white-space:pre-wrap!important;word-break:break-all!important;font-size:.76em!important;}' +
      '.md-table-wrap{overflow:hidden!important;margin:.55em 0!important;}' +
      '.md-table{font-size:.72em!important;table-layout:fixed!important;width:100%!important;word-break:break-word!important;}' +
      '.md-table th,.md-table td{word-break:break-word!important;overflow-wrap:break-word!important;overflow:hidden!important;}' +
      '</style></head>' +
      '<body><div class="md-content">' + blocksHtml + '</div></body></html>';
  };

  // 3. 查询块实际位置（在测量 WebView 里调用，返回 CSS px）
  window.getMeasuredPositions = function() {
    var blocks = Array.from(document.querySelectorAll('[data-bidx]'));
    var firstTop = blocks.length > 0
      ? Math.round(blocks[0].getBoundingClientRect().top + window.scrollY)
      : CARD_PAD;
    return JSON.stringify({
      firstTop: firstTop,
      positions: blocks.map(function(b) {
        var r = b.getBoundingClientRect();
        return {
          top:    Math.round(r.top    + window.scrollY),
          bottom: Math.round(r.bottom + window.scrollY)
        };
      })
    });
  };

  // 4. 封面卡独立 HTML（在 host WebView 里调用）
  window.buildCoverCardHtml = function(markdown, opts) {
    opts = opts || {};
    var titleM = markdown.match(/^#\s+(.+)$/m);
    var subM   = markdown.match(/^##\s+(.+)$/m);
    var title  = titleM ? titleM[1].replace(/[*_`[\]]/g, '') : 'ACKS Reader';
    var sub    = subM   ? subM[1].replace(/[*_`[\]]/g, '')   : '';
    return coverHtml(title, sub, opts.themeId || 'aireport', opts.fontSource || 'local',
      opts.padPx || CARD_PAD);
  };

  // 5. 单页内容卡独立 HTML（在 host WebView 里调用）
  window.buildContentCardHtml = function(blocksJsonStr, pageIdx, totalPages, opts) {
    opts = opts || {};
    var blocks;
    try { blocks = JSON.parse(blocksJsonStr); } catch(e) { blocks = []; }
    var html = blocks.map(function(b) { return b.html; }).join('');
    return contentHtml(html, pageIdx, totalPages, opts.themeId || 'aireport',
      opts.fontSource || 'local', opts.fontSizePx || 18, opts.padPx || CARD_PAD);
  };

  // ── 主入口（两步 API，避免大 JSON 回传）────────────────────────────────────────
  // Step 1: buildCardPages → 返回卡片数量（整数字符串），同时把 HTML 存入全局缓存
  // Step 2: getCardPageHtml(i) → 返回第 i 张卡片的完整 HTML 字符串

  window.buildCardPages = function (markdown, opts) {
    opts = opts || {};
    var density   = opts.density || 'balanced';
    var cap       = PAGE_CAP[density] || 22;
    var themeId   = opts.themeId || 'aireport';
    var fontSrc   = opts.fontSource || 'local';
    var withCover = opts.withCover !== false;

    var mdHtml = window.renderMarkdown ? window.renderMarkdown(markdown) : markdown;
    var pages  = paginate(mdHtml, cap);
    var total  = pages.length + (withCover ? 1 : 0);
    var result = [];

    if (withCover) {
      var titleM = markdown.match(/^#\s+(.+)$/m);
      var subM   = markdown.match(/^##\s+(.+)$/m);
      var title  = titleM ? titleM[1].replace(/[*_`[\]]/g,'') : 'ACKS Reader';
      var sub    = subM   ? subM[1].replace(/[*_`[\]]/g,'')   : '';
      result.push(coverHtml(title, sub, themeId, fontSrc));
    }

    pages.forEach(function (blocks, i) {
      var idx = (withCover ? 2 : 1) + i;
      result.push(contentHtml(blocks.join(''), idx, total, themeId, fontSrc));
    });

    // 缓存到全局，供 getCardPageHtml 逐张拉取
    window.__acksCardCache = result;
    return String(result.length);   // 只返回数量，避免回传大 JSON
  };

  window.getCardPageHtml = function (index) {
    var cache = window.__acksCardCache;
    if (!cache || index < 0 || index >= cache.length) return '';
    return cache[index];
  };

  // ── 长图切片模式：单张连续 HTML（Kotlin 截图后按卡片高度切片）──────────────────
  // 封面卡直接嵌入 HTML 顶部（精确 CARD_H px 高），内容区跟随其后自然流动。
  // Kotlin 侧按 CARD_CSS_H=720px 切片即可，无需任何 JS 分页逻辑。
  window.buildLongDocForSlice = function (markdown, opts) {
    opts = opts || {};
    var themeId   = opts.themeId   || 'aireport';
    var fontSrc   = opts.fontSource || 'local';
    var withCover = opts.withCover  !== false;

    var mdHtml = window.renderMarkdown ? window.renderMarkdown(markdown) : markdown;
    var t = window.THEME_MAP && window.THEME_MAP[themeId];
    if (!t && window.THEMES && window.THEMES.length) t = window.THEMES[0];

    var mode = t ? t.defaultMode : 'dark';
    var sw   = t && t.swatch ? (mode === 'dark' ? t.swatch.dark : t.swatch.light) : null;
    if (!sw && t && t.swatch) sw = t.swatch.dark || t.swatch.light;
    var bg  = sw ? sw[0] : '#0D0F1A';
    var fg  = sw ? sw[1] : '#FFFFFF';
    var acc = sw ? sw[2] : '#F26419';

    var fonts    = window.getFontsHtml ? window.getFontsHtml(fontSrc) : '';
    var themeCss = t && t.css ? t.css(mode) : '';

    var coverBlock = '';
    if (withCover) {
      var titleM = markdown.match(/^#\s+(.+)$/m);
      var subM   = markdown.match(/^##\s+(.+)$/m);
      var title  = titleM ? titleM[1].replace(/[*_`[\]]/g, '') : 'ACKS Reader';
      var sub    = subM   ? subM[1].replace(/[*_`[\]]/g, '')   : '';
      coverBlock =
        '<div class="acks-cover">' +
          '<div class="ac-deco ac-tl"></div>' +
          '<div class="ac-deco ac-br"></div>' +
          '<div class="ac-bar"></div>' +
          '<div class="ac-title">' + esc(title) + '</div>' +
          (sub ? '<div class="ac-sub">' + esc(sub) + '</div>' : '') +
          '<div class="ac-tag">ACKS READER</div>' +
        '</div>';
    }

    return '<!DOCTYPE html><html><head>' +
      '<meta charset="utf-8">' +
      '<meta name="viewport" content="width=' + CARD_W + ',initial-scale=1">' +
      fonts +
      '<style>' +
      '*,*::before,*::after{box-sizing:border-box;margin:0;padding:0;}' +
      'html{font-size:18px;-webkit-font-smoothing:antialiased;--s-base:18px;}' +
      'body{width:' + CARD_W + 'px;max-width:' + CARD_W + 'px;background:' + bg + ';}' +
      'img{max-width:100%;height:auto;}' +
      '.acks-cover{height:' + CARD_H + 'px;width:100%;position:relative;' +
        'display:flex;flex-direction:column;justify-content:center;align-items:center;' +
        'text-align:center;padding:' + CARD_PAD + 'px;overflow:hidden;}' +
      '.ac-deco{position:absolute;width:22px;height:22px;border-color:' + acc + ';border-style:solid;}' +
      '.ac-tl{top:' + CARD_PAD + 'px;left:' + CARD_PAD + 'px;border-width:2.5px 0 0 2.5px;}' +
      '.ac-br{bottom:' + CARD_PAD + 'px;right:' + CARD_PAD + 'px;border-width:0 2.5px 2.5px 0;}' +
      '.ac-bar{width:48px;height:4px;background:' + acc + ';border-radius:2px;margin:0 auto 24px;}' +
      '.ac-title{font-size:2em;font-weight:700;line-height:1.2;color:' + fg + ';' +
        'letter-spacing:-.02em;margin-bottom:14px;word-break:break-word;}' +
      '.ac-sub{font-size:.95em;color:' + fg + '88;line-height:1.5;word-break:break-word;}' +
      '.ac-tag{position:absolute;bottom:' + CARD_PAD + 'px;left:50%;transform:translateX(-50%);' +
        'font-size:10px;color:' + fg + '40;letter-spacing:.08em;}' +
      '.acks-content{padding:' + CARD_PAD + 'px;}.md-content{width:100%;max-width:100%;}' +
      themeCss +
      '.md-h{word-break:break-word!important;overflow-wrap:break-word!important;white-space:normal!important;max-width:100%!important;}' +
      '.md-h1{font-size:min(1.85em,6vw)!important;line-height:1.25!important;margin:.1em 0 .4em!important;word-break:break-word!important;white-space:normal!important;letter-spacing:-.01em!important;}' +
      '.md-h2{font-size:min(1.4em,5vw)!important;display:block!important;margin:.6em 0 .3em!important;word-break:break-word!important;white-space:normal!important;width:auto!important;max-width:100%!important;}' +
      '.md-h3{font-size:min(1.1em,4.2vw)!important;margin:.5em 0 .25em!important;word-break:break-word!important;}' +
      '.md-p{margin:.45em 0!important;word-break:break-word!important;overflow-wrap:break-word!important;}' +
      '.md-quote{font-size:min(1em,4vw)!important;white-space:normal!important;word-break:break-word!important;margin:.55em 0!important;}' +
      '.md-ul,.md-ol{margin:.4em 0!important;}' +
      '.md-ul li,.md-ol li{word-break:break-word!important;overflow-wrap:break-word!important;}' +
      '.md-pre{margin:.6em 0!important;overflow:hidden!important;}' +
      '.md-pre code{white-space:pre-wrap!important;word-break:break-all!important;font-size:.76em!important;}' +
      '.md-code{word-break:break-all!important;}' +
      '.md-table-wrap{overflow:hidden!important;margin:.55em 0!important;}' +
      '.md-table{font-size:.72em!important;table-layout:fixed!important;width:100%!important;word-break:break-word!important;}' +
      '.md-table th,.md-table td{word-break:break-word!important;overflow-wrap:break-word!important;overflow:hidden!important;}' +
      '</style></head>' +
      '<body>' + coverBlock +
      '<div class="acks-content"><div class="md-content">' + mdHtml + '</div></div>' +
      '</body></html>';
  };

  // 返回主题色 JSON（供 Kotlin 末页填充 bg 和页码绘制 fg）
  window.getThemeSwatchColors = function (themeId) {
    var t = window.THEME_MAP && window.THEME_MAP[themeId];
    if (!t && window.THEMES && window.THEMES.length) t = window.THEMES[0];
    var mode = t ? (t.defaultMode || 'dark') : 'dark';
    var sw = t && t.swatch ? (mode === 'dark' ? t.swatch.dark : t.swatch.light) : null;
    if (!sw && t && t.swatch) sw = t.swatch.dark || t.swatch.light;
    return JSON.stringify({
      bg:  sw ? sw[0] : '#0D0F1A',
      fg:  sw ? sw[1] : '#FFFFFF',
      acc: sw ? sw[2] : '#F26419'
    });
  };

})();
