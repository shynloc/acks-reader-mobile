// card-export.js — 图文卡片分页引擎（单位估算法，无 DOM layout 触发）
// window.buildCardPages(markdown, opts) → JSON string
(function () {
  var CARD_W = 540;
  var CARD_H = 720;
  var CARD_PAD = 28;

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
  function coverHtml(title, subtitle, themeId, fontSrc) {
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
      'html,body{width:' + CARD_W + 'px;height:' + CARD_H + 'px;overflow:hidden;' +
        'background:' + bg + ';font-family:\'DM Sans\',\'Noto Sans SC\',sans-serif;}' +
      'body{display:flex;flex-direction:column;justify-content:center;align-items:center;' +
        'padding:' + CARD_PAD + 'px;text-align:center;position:relative;}' +
      '.bar{width:48px;height:4px;background:' + acc + ';border-radius:2px;margin:0 auto 24px;}' +
      '.title{font-size:2em;font-weight:700;line-height:1.2;color:' + fg + ';' +
        'letter-spacing:-.02em;margin-bottom:14px;}' +
      '.sub{font-size:.95em;color:' + fg + '88;line-height:1.5;}' +
      '.deco{position:absolute;width:22px;height:22px;border-color:' + acc + ';border-style:solid;}' +
      '.tl{top:' + CARD_PAD + 'px;left:' + CARD_PAD + 'px;border-width:2.5px 0 0 2.5px;}' +
      '.br{bottom:' + CARD_PAD + 'px;right:' + CARD_PAD + 'px;border-width:0 2.5px 2.5px 0;}' +
      '.tag{position:absolute;bottom:' + CARD_PAD + 'px;left:50%;transform:translateX(-50%);' +
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
  function contentHtml(blocksHtml, index, total, themeId, fontSrc) {
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
      'html{font-size:20px;-webkit-font-smoothing:antialiased;--s-base:20px;}' +
      'body{padding:' + CARD_PAD + 'px;width:' + CARD_W + 'px;' +
        'min-height:' + CARD_H + 'px;overflow:hidden;}' +
      '.md-content{width:100%;max-width:100%;}img{max-width:100%;}' +
      themeCss +
      // 卡片密度微调：收紧上下边距
      '.md-h1{margin:.1em 0 .35em;}.md-h2{margin:.6em 0 .3em;}' +
      '.md-h3{margin:.5em 0 .25em;}.md-p{margin:.45em 0;}' +
      '.md-ul,.md-ol{margin:.4em 0;}.md-pre{margin:.6em 0;}' +
      '.md-table-wrap{margin:.6em 0;}.md-quote{margin:.5em 0;}' +
      // 页码指示器
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

})();
