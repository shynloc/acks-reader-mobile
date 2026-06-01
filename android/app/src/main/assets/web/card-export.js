// card-export.js — smart Markdown→card pagination for 图文卡片 export
// Exposes: window.buildCardPages(markdown, opts) → JSON string
//          window.buildCardHtml(page, opts) → complete HTML string

(function () {
  var CARD_W = 540;   // CSS px width of card viewport
  var CARD_H = 720;   // CSS px height of card (3:4 ratio)
  var CARD_PAD = 28;  // padding inside card

  var FILL = { loose: 0.68, balanced: 0.80, dense: 0.91 };

  // ── Measurement CSS: must match card rendering fonts/sizes ─────────────────
  // We inject this temporarily during block height measurement
  function getMeasureCss() {
    var ff = "font-family:'DM Sans','Noto Sans SC',sans-serif;";
    return [
      '.card-measure{width:' + (CARD_W - CARD_PAD * 2) + 'px;font-size:20px;line-height:1.65;' + ff + '}',
      '.card-measure .md-h1{font-size:1.7em;font-weight:700;margin:0 0 .5em;line-height:1.2;}',
      '.card-measure .md-h2{font-size:1.25em;font-weight:600;margin:.8em 0 .4em;}',
      '.card-measure .md-h3{font-size:1.05em;font-weight:600;margin:.6em 0 .3em;}',
      '.card-measure .md-p{margin:.6em 0;}',
      '.card-measure .md-ul,.card-measure .md-ol{padding-left:1.2em;margin:.5em 0;}',
      '.card-measure .md-ul li,.card-measure .md-ol li{margin:.25em 0;}',
      '.card-measure .md-pre{margin:.8em 0;padding:12px 14px;border-radius:8px;font-size:.78em;line-height:1.55;}',
      '.card-measure .md-table{font-size:.85em;}',
      '.card-measure .md-table th,.card-measure .md-table td{padding:.45em .7em;}',
      '.card-measure .md-quote{padding:.5em .8em;margin:.7em 0;}',
      '.card-measure .md-mermaid{padding:10px 6px;margin:.8em 0;}',
    ].join('');
  }

  // ── Paginate content into card-sized chunks ────────────────────────────────
  function paginate(mdHtml, fillFactor) {
    var availH = (CARD_H - CARD_PAD * 2) * fillFactor;

    // Inject temporary measurement styles
    var styleEl = document.createElement('style');
    styleEl.id = '__acks_card_measure_style';
    styleEl.textContent = getMeasureCss();
    document.head.appendChild(styleEl);

    // Create hidden measurement container
    var div = document.createElement('div');
    div.style.cssText = 'position:absolute;top:-9999px;left:0;width:' + CARD_W + 'px;visibility:hidden;pointer-events:none;';
    div.innerHTML = '<div class="card-measure">' + mdHtml + '</div>';
    document.body.appendChild(div);

    var content = div.querySelector('.card-measure');
    var blocks = Array.from(content.children);

    var pages = [], cur = [], curH = 0, GAP = 14;

    // Smart grouping rules:
    // - Keep heading with its following paragraph (heading orphan prevention)
    // - Don't split list items mid-list if possible
    function isHeading(el) {
      return /^md-h[123]$/.test(el.className.split(' ')[0]);
    }

    for (var i = 0; i < blocks.length; i++) {
      var block = blocks[i];
      var blockH = block.getBoundingClientRect().height + GAP;

      // Heading: try to keep with next sibling
      if (isHeading(block) && i + 1 < blocks.length) {
        var nextH = blocks[i + 1].getBoundingClientRect().height + GAP;
        var combinedH = blockH + nextH;
        // If heading + next block fits, force them together on next page
        if (combinedH <= availH && curH + combinedH > availH && cur.length > 0) {
          pages.push(cur); cur = []; curH = 0;
        }
      }

      // Block alone exceeds a full page — put it solo
      if (blockH > availH && cur.length === 0) {
        pages.push([block.outerHTML]);
        continue;
      }
      // Would overflow — start new page
      if (cur.length > 0 && curH + blockH > availH) {
        pages.push(cur); cur = []; curH = 0;
      }
      cur.push(block.outerHTML);
      curH += blockH;
    }
    if (cur.length > 0) pages.push(cur);

    // Cleanup
    document.body.removeChild(div);
    document.head.removeChild(styleEl);

    return pages;
  }

  // ── Build cover card HTML ─────────────────────────────────────────────────
  function coverHtml(title, subtitle, themeId, fontSrc) {
    var t = window.THEME_MAP && window.THEME_MAP[themeId];
    var mode = t ? t.defaultMode : 'light';
    var swatch = t ? (mode === 'dark' ? t.swatch.dark : t.swatch.light) : ['#0D0F1A', '#E8E5FF', '#9B7EE8'];
    var bg = swatch[0] || '#0D0F1A';
    var titleColor = swatch[1] || '#FFFFFF';
    var accent = swatch[2] || '#F26419';
    var fontsHtml = window.getFontsHtml ? window.getFontsHtml(fontSrc || 'local') : '';

    return '<!DOCTYPE html><html><head><meta charset="utf-8">' +
      '<meta name="viewport" content="width=' + CARD_W + ',initial-scale=1">' +
      fontsHtml +
      '<style>' +
      '*{box-sizing:border-box;margin:0;padding:0;}' +
      'html,body{width:' + CARD_W + 'px;height:' + CARD_H + 'px;overflow:hidden;}' +
      'body{background:' + bg + ';display:flex;flex-direction:column;justify-content:center;align-items:center;padding:' + CARD_PAD + 'px;text-align:center;font-family:"DM Sans","Noto Sans SC",sans-serif;}' +
      '.cover-accent{width:48px;height:4px;background:' + accent + ';border-radius:2px;margin:0 auto 28px;}' +
      '.cover-title{font-size:2.1em;font-weight:700;line-height:1.2;color:' + titleColor + ';letter-spacing:-.02em;margin-bottom:16px;}' +
      '.cover-sub{font-size:1em;color:' + titleColor + '88;line-height:1.5;margin-top:8px;}' +
      '.cover-tag{position:absolute;bottom:' + CARD_PAD + 'px;right:' + CARD_PAD + 'px;font-size:11px;color:' + titleColor + '44;letter-spacing:.06em;}' +
      '.cover-deco{position:absolute;top:' + CARD_PAD + 'px;left:' + CARD_PAD + 'px;width:24px;height:24px;border-top:2.5px solid ' + accent + ';border-left:2.5px solid ' + accent + ';}' +
      '.cover-deco2{position:absolute;bottom:' + CARD_PAD + 'px;left:' + CARD_PAD + 'px;width:24px;height:24px;border-bottom:2.5px solid ' + accent + ';border-left:2.5px solid ' + accent + ';}' +
      '</style></head>' +
      '<body>' +
      '<div class="cover-deco"></div>' +
      '<div class="cover-deco2"></div>' +
      '<div class="cover-accent"></div>' +
      '<div class="cover-title">' + escHtml(title) + '</div>' +
      (subtitle ? '<div class="cover-sub">' + escHtml(subtitle) + '</div>' : '') +
      '<div class="cover-tag">ACKS Reader</div>' +
      '</body></html>';
  }

  function escHtml(s) {
    return String(s).replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
  }

  // ── Build content card HTML ───────────────────────────────────────────────
  function contentHtml(blocksHtml, index, total, themeId, fontSrc) {
    var t = window.THEME_MAP && window.THEME_MAP[themeId];
    if (!t) t = window.THEMES && window.THEMES[0];
    if (!t) return '<html><body>' + blocksHtml + '</body></html>';

    var mode = t.defaultMode;
    var fontsHtml = window.getFontsHtml ? window.getFontsHtml(fontSrc || 'local') : '';
    var themeCss = t.css(mode);
    var swatch = mode === 'dark' ? t.swatch.dark : t.swatch.light;
    var accent = swatch ? swatch[2] : '#F26419';

    return '<!DOCTYPE html><html><head><meta charset="utf-8">' +
      '<meta name="viewport" content="width=' + CARD_W + ',initial-scale=1">' +
      fontsHtml +
      '<style>' +
      '*,*::before,*::after{box-sizing:border-box;margin:0;padding:0;}' +
      'html{font-size:20px;-webkit-font-smoothing:antialiased;--s-base:20px;}' +
      'body{padding:' + CARD_PAD + 'px;width:' + CARD_W + 'px;min-height:' + CARD_H + 'px;overflow:hidden;}' +
      '.md-content{width:100%;max-width:100%;}' +
      'img{max-width:100%;}' +
      themeCss +
      // Card-specific overrides: tighter margins for card density
      '.md-h1{margin-bottom:.4em;}.md-h2{margin-top:.8em;margin-bottom:.35em;}.md-h3{margin-top:.6em;margin-bottom:.25em;}' +
      '.md-p{margin:.55em 0;}.md-ul,.md-ol{margin:.45em 0;}.md-pre{margin:.7em 0;}' +
      // Page indicator
      '.card-page-num{position:fixed;bottom:12px;right:16px;font-size:10px;opacity:.45;font-family:"DM Sans",sans-serif;letter-spacing:.04em;}' +
      '</style></head>' +
      '<body><div class="md-content">' + blocksHtml + '</div>' +
      '<div class="card-page-num">' + index + ' / ' + total + '</div>' +
      '</body></html>';
  }

  // ── Main API ──────────────────────────────────────────────────────────────
  window.buildCardPages = function (markdown, opts) {
    opts = opts || {};
    var density = opts.density || 'balanced';
    var fillFactor = FILL[density] || 0.80;
    var themeId = opts.themeId || 'aireport';
    var fontSrc = opts.fontSource || 'local';
    var withCover = opts.withCover !== false;

    // Render markdown to HTML
    var mdHtml = window.renderMarkdown ? window.renderMarkdown(markdown) : markdown;

    // Paginate
    var pages = paginate(mdHtml, fillFactor);
    var contentCount = pages.length;
    var totalCards = contentCount + (withCover ? 1 : 0);

    var result = [];

    if (withCover) {
      var titleMatch = markdown.match(/^#\s+(.+)$/m);
      var subMatch = markdown.match(/^##\s+(.+)$/m);
      var title = titleMatch ? titleMatch[1].replace(/[*_`[\]]/g, '') : 'ACKS Reader';
      var subtitle = subMatch ? subMatch[1].replace(/[*_`[\]]/g, '') : '';
      result.push({
        isCover: true,
        index: 1,
        total: totalCards,
        html: coverHtml(title, subtitle, themeId, fontSrc)
      });
    }

    pages.forEach(function (blocks, i) {
      var cardIndex = (withCover ? 2 : 1) + i;
      result.push({
        isCover: false,
        index: cardIndex,
        total: totalCards,
        html: contentHtml(blocks.join(''), cardIndex, totalCards, themeId, fontSrc)
      });
    });

    return JSON.stringify({ pages: result, count: result.length });
  };

})();
