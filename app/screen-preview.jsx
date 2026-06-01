// screen-preview.jsx — DocPreview (WebView + gestures), TopBar, capsule toolbar,
// pull-down ThemeRail, long-press menu, zoom badge, lifecycle state machine
(function () {
  const { useState, useEffect, useRef, useCallback } = React;

  function DocPreview({ docHtml, themeId, mode, vw, ov, area, isHtml, htmlMode, onScrollDir, onLongPress, onZoom, clearTick, zoomResetTick, searchQuery, findNavSig, gotoHeadingSig, onSearchRes, onActiveHead, extraFonts }) {
    const iframeRef = useRef(null);
    const [h, setH] = useState(1200);
    const [fade, setFade] = useState(false);
    const lastScroll = useRef(0);
    const scaleRef = useRef(1);

    const srcdoc = isHtml
      ? buildHtmlDoc(htmlMode)
      : window.buildThemeDoc(themeId, docHtml, { mode, ov, interactive: true, extraFonts });

    const measure = useCallback(() => {
      const f = iframeRef.current; if (!f) return;
      try { const doc = f.contentDocument; if (doc && doc.body) setH(Math.max(doc.body.scrollHeight, doc.documentElement.scrollHeight)); } catch (e) {}
    }, []);

    useEffect(() => { setFade(true); const t = setTimeout(() => setFade(false), 60); return () => clearTimeout(t); }, [themeId, mode, JSON.stringify(ov), htmlMode]);

    // parent-side message bridge for gestures
    useEffect(() => {
      const onMsg = (e) => {
        const d = e.data; if (!d || !d.__acks) return;
        const f = iframeRef.current; if (!f || e.source !== f.contentWindow) return;
        const rect = f.getBoundingClientRect(); const sc = scaleRef.current;
        if (d.__acks === 'longpress' && onLongPress) {
          onLongPress({ word: d.word, vx: rect.left + d.x * sc, top: rect.top + d.top * sc, bottom: rect.top + d.bottom * sc });
        } else if (d.__acks === 'zoom' && onZoom) { onZoom(d.zoom); }
        else if (d.__acks === 'searchres' && onSearchRes) { onSearchRes({ count: d.count, cur: d.cur }); }
        else if (d.__acks === 'activehead' && onActiveHead) { onActiveHead(d.i); }
      };
      window.addEventListener('message', onMsg);
      return () => window.removeEventListener('message', onMsg);
    }, [onLongPress, onZoom, onSearchRes, onActiveHead]);

    const post = (msg) => { const f = iframeRef.current; if (f && f.contentWindow) try { f.contentWindow.postMessage(msg, '*'); } catch (e) {} };
    useEffect(() => { post({ __acks: 'search', q: searchQuery || '' }); }, [searchQuery]);
    useEffect(() => { if (findNavSig && findNavSig.n) post({ __acks: 'findnav', dir: findNavSig.dir }); }, [findNavSig && findNavSig.n]);
    useEffect(() => { if (gotoHeadingSig && gotoHeadingSig.n) post({ __acks: 'gotoHeading', i: gotoHeadingSig.i }); }, [gotoHeadingSig && gotoHeadingSig.n]);

    useEffect(() => { const f = iframeRef.current; if (f && f.contentWindow && clearTick) try { f.contentWindow.postMessage({ __acks: 'clearhl' }, '*'); } catch (e) {} }, [clearTick]);
    useEffect(() => { const f = iframeRef.current; if (f && f.contentWindow && zoomResetTick) try { f.contentWindow.postMessage({ __acks: 'resetzoom' }, '*'); } catch (e) {} }, [zoomResetTick]);

    const onLoad = () => {
      measure(); setTimeout(measure, 350);
      const f = iframeRef.current;
      try {
        const win = f.contentWindow;
        win.postMessage({ __acks: 'trackHeadings' }, '*');
        win.addEventListener('scroll', () => {
          const y = win.scrollY || win.document.documentElement.scrollTop || 0;
          const dir = y > lastScroll.current + 4 ? 'down' : (y < lastScroll.current - 4 ? 'up' : null);
          if (dir && onScrollDir) onScrollDir(dir, y);
          lastScroll.current = y;
        }, { passive: true });
      } catch (e) {}
    };

    const scale = Math.min(1, area.w / vw); scaleRef.current = scale;
    const wrapW = vw * scale;

    // capture mode: render markdown as LIGHT DOM (scoped CSS) so html-to-image can snapshot it
    if (window.__ACKS_CAPTURE && !isHtml) {
      const built = window.buildThemeScoped(themeId, { mode, ov, sel: '.acks-cap' });
      const t = window.THEME_MAP[themeId];
      const bg = (t.swatch[built.mode] && t.swatch[built.mode][0]) || '#fff';
      return (
        <div style={{ width: area.w, height: area.h, overflow: 'hidden', display: 'flex', justifyContent: 'center', background: bg }}>
          <div style={{ width: wrapW, height: area.h, overflow: 'hidden', position: 'relative' }}>
            <div style={{ width: vw, transform: `scale(${scale})`, transformOrigin: 'top left', background: bg, minHeight: area.h / scale }}>
              <style>{built.css}</style>
              <div className="acks-cap" style={{ padding: 22, ...(built.rootVars ? cssVarStyle(built.rootVars) : {}) }}>
                <div className="md-content" dangerouslySetInnerHTML={{ __html: docHtml }} />
              </div>
            </div>
          </div>
        </div>
      );
    }

    return (
      <div style={{ width: area.w, height: area.h, overflow: 'auto', WebkitOverflowScrolling: 'touch', display: 'flex', justifyContent: 'center', background: 'transparent' }}>
        <div style={{ width: wrapW, minHeight: '100%', position: 'relative', transition: 'width 460ms cubic-bezier(0.16,1,0.3,1)' }}>
          <iframe ref={iframeRef} srcDoc={srcdoc} onLoad={onLoad} title="preview" scrolling="no"
            style={{ width: vw, height: Math.max(h, area.h / scale), border: 'none', display: 'block', transform: `scale(${scale})`, transformOrigin: 'top left', transition: 'transform 460ms cubic-bezier(0.16,1,0.3,1), opacity 200ms ease', opacity: fade ? 0.35 : 1, background: 'transparent' }} />
        </div>
      </div>
    );
  }

  // turn "--a:1;--b:2;" into a style object for React
  function cssVarStyle(str) {
    const o = {};
    str.split(';').forEach(d => { const i = d.indexOf(':'); if (i > 0) o[d.slice(0, i).trim()] = d.slice(i + 1).trim(); });
    return o;
  }

  function buildHtmlDoc(htmlMode) {
    const interactive = htmlMode === 'interactive';
    return `<!DOCTYPE html><html><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">
<style>
*{box-sizing:border-box;margin:0;padding:0;}
body{font-family:'Space Grotesk','Inter',sans-serif;background:#0c0e1a;color:#e8ecf5;min-height:100%;overflow:hidden;}
.wrap{padding:30px 22px;}
.tag{font-size:11px;letter-spacing:.16em;text-transform:uppercase;color:#7C5CFF;font-weight:700;}
h1{font-size:30px;font-weight:700;margin:10px 0 6px;background:linear-gradient(100deg,#7C5CFF,#4DD0E1);-webkit-background-clip:text;background-clip:text;color:transparent;}
p{color:#aab0c8;line-height:1.6;font-size:14px;margin-bottom:20px;}
.orb{width:140px;height:140px;border-radius:50%;margin:24px auto;background:conic-gradient(from 0deg,#7C5CFF,#4DD0E1,#F26419,#7C5CFF);filter:blur(2px);${interactive ? 'animation:spin 4s linear infinite;' : ''}}
@keyframes spin{to{transform:rotate(360deg);}}
.bar{height:10px;border-radius:6px;background:#1a1d33;overflow:hidden;margin:10px 0;}
.bar>i{display:block;height:100%;border-radius:6px;background:linear-gradient(90deg,#7C5CFF,#4DD0E1);width:30%;${interactive ? 'animation:load 2.4s ease-in-out infinite;' : 'width:62%;'}}
@keyframes load{0%{width:8%}50%{width:88%}100%{width:8%}}
.grid{display:grid;grid-template-columns:1fr 1fr;gap:10px;margin-top:18px;}
.card{background:#13152a;border:1px solid #23264a;border-radius:12px;padding:14px;}
.card b{font-size:22px;color:#fff;}.card span{font-size:11px;color:#7a809a;}
.miss{margin-top:18px;padding:12px;border:1px dashed #3a2f1a;border-radius:10px;color:#caa34a;font-size:12px;background:#161208;}
.note{margin-top:16px;font-size:12px;color:#5a607a;}
</style></head><body><div class="wrap">
<div class="tag">Interactive Demo · ${interactive ? 'LIVE' : 'STATIC'}</div>
<h1>AI Dashboard</h1>
<p>An AI-generated single-file page with CSS animation, Canvas &amp; live charts.</p>
<div class="orb"></div>
<div class="bar"><i></i></div>
<div class="grid"><div class="card"><b>${interactive ? '↑ 18%' : '— —'}</b><span>Conversion</span></div><div class="card"><b>120ms</b><span>P99 latency</span></div></div>
<div class="miss">▦ 缺失资源占位 · assets/hero.png, styles/theme.css</div>
<div class="note">${interactive ? '✓ Scripts running — animations live.' : '○ Safe Preview — scripts paused. Enable interactive mode to run.'}</div>
</div></body></html>`;
  }

  function TopBar({ ctx, doc, isHtml, onSearch }) {
    const { s, r, platform, go } = ctx;
    const big = platform === 'ios';
    return (
      <div style={{ flexShrink: 0, background: s.glass, backdropFilter: 'blur(18px)', WebkitBackdropFilter: 'blur(18px)', borderBottom: `1px solid ${s.border}`, zIndex: 20 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8, padding: '6px 10px 6px 6px', height: 50 }}>
          <button onClick={() => go('recent')} aria-label="back" style={{ width: 40, height: 40, border: 'none', background: 'transparent', color: s.accent, display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}><Icon name="back" size={24} /></button>
          <div style={{ flex: 1, minWidth: 0, textAlign: big ? 'center' : 'left' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 7, justifyContent: big ? 'center' : 'flex-start' }}>
              <span style={{ fontSize: 15, fontWeight: 600, color: s.fg, fontFamily: "'DM Sans','Noto Sans SC',sans-serif", whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis', maxWidth: big ? 200 : 230 }}>{doc.title}</span>
            </div>
            <div style={{ display: 'flex', alignItems: 'center', gap: 6, justifyContent: big ? 'center' : 'flex-start', marginTop: 1 }}>
              <span style={{ fontSize: 10, fontWeight: 700, letterSpacing: '0.06em', color: s.accent, background: s.accentSoft, padding: '1px 6px', borderRadius: r.xs }}>{isHtml ? 'HTML' : 'MD'}</span>
              {isHtml ? (
                <span onClick={() => go('@htmlsafety')} style={{ display: 'flex', alignItems: 'center', gap: 3, fontSize: 10, color: ctx.htmlMode === 'interactive' ? '#22C55E' : s.fg2, cursor: 'pointer' }}>
                  <Icon name={ctx.htmlMode === 'interactive' ? 'zap' : 'shield'} size={12} />{ctx.htmlMode === 'interactive' ? '完整交互' : '安全预览'}
                </span>
              ) : (<span style={{ fontSize: 10, color: s.fg3 }}>{doc.source} · {doc.size}</span>)}
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
            {!isHtml && <button onClick={onSearch} aria-label="search" style={{ width: 40, height: 40, border: 'none', background: 'transparent', color: s.fg2, display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}><Icon name="search" size={20} /></button>}
            <button onClick={() => go('@more')} aria-label="more" style={{ width: 40, height: 40, border: 'none', background: 'transparent', color: s.fg2, display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}><Icon name={platform === 'android' ? 'more' : 'moreH'} size={22} /></button>
          </div>
        </div>
      </div>
    );
  }

  // pull-down / tap theme quick-switch rail
  function ThemeRail({ ctx }) {
    const { s, r, themeId, themeMode, setState } = ctx;
    const [open, setOpen] = useState(false);
    const [drag, setDrag] = useState(0);
    const start = useRef(null);
    const onDown = (e) => { start.current = (e.touches ? e.touches[0].clientY : e.clientY); };
    const onMove = (e) => { if (start.current == null) return; const y = (e.touches ? e.touches[0].clientY : e.clientY); setDrag(Math.max(0, Math.min(96, y - start.current))); };
    const onUp = () => { if (start.current == null) return; setOpen(drag > 46 ? true : (drag < -10 ? false : open)); setDrag(0); start.current = null; };
    const railH = open ? 100 : drag;
    return (
      <div style={{ position: 'absolute', top: 0, left: 0, right: 0, zIndex: 18, pointerEvents: 'none' }}>
        <div style={{ height: railH, overflow: 'hidden', background: s.glass, backdropFilter: 'blur(20px)', WebkitBackdropFilter: 'blur(20px)', borderBottom: railH > 4 ? `1px solid ${s.border}` : 'none', transition: start.current == null ? 'height 320ms cubic-bezier(0.16,1,0.3,1)' : 'none', pointerEvents: 'auto' }}>
          <div style={{ padding: '12px 12px 10px', opacity: railH > 40 ? 1 : 0, transition: 'opacity 180ms' }}>
            <div style={{ fontSize: 10, fontWeight: 700, letterSpacing: '0.08em', color: s.fg3, marginBottom: 8, fontFamily: "'Space Grotesk',sans-serif", paddingLeft: 4 }}>下拉换主题 · QUICK THEME</div>
            <div style={{ display: 'flex', gap: 8, overflowX: 'auto', paddingBottom: 2 }}>
              {window.THEMES.map(t => {
                const active = t.id === themeId; const sw = t.swatch[t.modes.includes(themeMode) ? themeMode : t.defaultMode];
                return (
                  <button key={t.id} onClick={() => { setState({ themeId: t.id, themeMode: t.modes.includes(themeMode) ? themeMode : t.defaultMode }); setOpen(false); }} style={{ flexShrink: 0, display: 'flex', alignItems: 'center', gap: 7, padding: '7px 11px 7px 8px', borderRadius: r.pill, cursor: 'pointer', border: active ? `1.5px solid ${s.accent}` : `1px solid ${s.border}`, background: active ? s.accentSoft : s.surface2 }}>
                    <span style={{ display: 'flex', width: 16, height: 16, borderRadius: 5, overflow: 'hidden', border: `1px solid ${s.border2}` }}>
                      <i style={{ width: '50%', background: sw[0] }} /><i style={{ width: '50%', background: sw[2] }} />
                    </span>
                    <span style={{ fontSize: 12, fontWeight: active ? 700 : 500, color: active ? s.accent : s.fg, fontFamily: "'Noto Sans SC',sans-serif", whiteSpace: 'nowrap' }}>{t.name}</span>
                  </button>
                );
              })}
            </div>
          </div>
        </div>
        <div onClick={() => setOpen(o => !o)} onMouseDown={onDown} onMouseMove={onMove} onMouseUp={onUp} onMouseLeave={onUp}
          onTouchStart={onDown} onTouchMove={onMove} onTouchEnd={onUp}
          style={{ position: 'absolute', top: railH, left: '50%', transform: 'translateX(-50%)', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 3, padding: '5px 22px 9px', cursor: 'grab', pointerEvents: 'auto', userSelect: 'none', transition: start.current == null ? 'top 320ms cubic-bezier(0.16,1,0.3,1)' : 'none' }}>
          <div style={{ background: s.glass, backdropFilter: 'blur(12px)', WebkitBackdropFilter: 'blur(12px)', borderRadius: r.pill, border: `1px solid ${s.border}`, padding: '4px 12px', display: 'flex', alignItems: 'center', gap: 6, boxShadow: '0 4px 12px rgba(0,0,0,0.18)' }}>
            <span style={{ width: 26, height: 4, borderRadius: 3, background: s.border2 }} />
            <Icon name={open ? 'chevronUp' : 'palette'} size={13} color={s.accent} />
          </div>
        </div>
      </div>
    );
  }

  function LongPressMenu({ ctx, sel, area, onClose }) {
    const { s, r } = ctx;
    if (!sel) return null;
    const acts = [{ icon: 'file', label: '复制' }, { icon: 'sparkle', label: '高亮' }, { icon: 'bookOpen', label: '翻译' }, { icon: 'search', label: '搜索' }];
    const above = sel.top > 92;
    const left = Math.max(96, Math.min(area.w - 96, sel.left));
    const top = above ? sel.top - 8 : sel.bottom + 8;
    return (
      <div style={{ position: 'absolute', left, top, transform: `translate(-50%, ${above ? '-100%' : '0'})`, zIndex: 30 }}>
        <div style={{ background: s.dark ? '#000' : '#1c1c1f', borderRadius: r.sm, boxShadow: '0 8px 28px rgba(0,0,0,0.45)', display: 'flex', overflow: 'hidden', border: `1px solid ${s.border2}` }}>
          {acts.map((a, i) => (
            <button key={a.label} onClick={onClose} style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 3, padding: '9px 14px', border: 'none', borderRight: i < acts.length - 1 ? '1px solid rgba(255,255,255,0.12)' : 'none', background: 'transparent', color: '#fff', cursor: 'pointer', fontFamily: "'Noto Sans SC',sans-serif" }}>
              <Icon name={a.icon} size={17} color="#fff" /><span style={{ fontSize: 11, fontWeight: 500 }}>{a.label}</span>
            </button>
          ))}
        </div>
        <div style={{ width: 9, height: 9, background: s.dark ? '#000' : '#1c1c1f', position: 'absolute', left: '50%', [above ? 'bottom' : 'top']: -4, transform: 'translateX(-50%) rotate(45deg)', borderRight: above ? `1px solid ${s.border2}` : 'none', borderBottom: above ? `1px solid ${s.border2}` : 'none' }} />
      </div>
    );
  }

  function BottomToolbar({ ctx, isHtml, hidden }) {
    const { s, r, platform, go, themeId, viewport } = ctx;
    const theme = window.THEME_MAP[themeId];
    const items = [
      { id: '@theme', icon: 'palette', label: '主题', disabled: isHtml },
      { id: '@viewport', icon: 'monitor', label: '视口' },
      { id: '@export', icon: 'download', label: '导出' },
      { id: '@toc', icon: 'list', label: '目录' },
    ];
    const variant = ctx.toolbarVariant || 'capsule';
    const floating = variant !== 'bar';
    const inner = (
      <div style={{ display: 'flex', alignItems: 'stretch', gap: variant === 'segment' ? 0 : 4, padding: variant === 'segment' ? 4 : 6 }}>
        {items.map((it, idx) => {
          const dim = it.disabled;
          return (
            <button key={it.id} disabled={dim} onClick={() => go(it.id)} style={{ flex: 1, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 3, padding: variant === 'segment' ? '9px 4px' : '7px 6px', border: 'none', cursor: dim ? 'default' : 'pointer', background: 'transparent', color: dim ? s.fg3 : s.fg, borderRadius: r.sm, borderRight: variant === 'segment' && idx < items.length - 1 ? `1px solid ${s.border}` : 'none', opacity: dim ? 0.4 : 1, WebkitTapHighlightColor: 'transparent', transition: 'all 160ms' }}>
              <Icon name={it.icon} size={21} color={s.fg} /><span style={{ fontSize: 10, fontWeight: 600, fontFamily: "'DM Sans','Noto Sans SC',sans-serif" }}>{it.label}</span>
            </button>
          );
        })}
        <button onClick={() => go('@export')} aria-label="primary" style={{ flex: variant === 'segment' ? 1 : '0 0 auto', minWidth: 46, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', gap: 3, padding: variant === 'segment' ? '9px 4px' : '7px 14px', margin: variant === 'segment' ? 0 : '0 0 0 2px', border: 'none', cursor: 'pointer', background: s.accentGrad || s.accent, color: '#fff', borderRadius: variant === 'segment' ? 0 : r.sm }}>
          <Icon name="share" size={21} color="#fff" /><span style={{ fontSize: 10, fontWeight: 700, fontFamily: "'DM Sans','Noto Sans SC',sans-serif" }}>分享</span>
        </button>
      </div>
    );
    if (!floating) {
      return <div style={{ flexShrink: 0, background: s.glass, backdropFilter: 'blur(20px)', WebkitBackdropFilter: 'blur(20px)', borderTop: `1px solid ${s.border}`, paddingBottom: platform === 'android' ? 6 : 14, transform: hidden ? 'translateY(110%)' : 'translateY(0)', transition: 'transform 380ms cubic-bezier(0.16,1,0.3,1)' }}>{inner}</div>;
    }
    return (
      <div style={{ position: 'absolute', left: 12, right: 12, bottom: platform === 'android' ? 20 : 28, zIndex: 25, transform: hidden ? 'translateY(160%)' : 'translateY(0)', opacity: hidden ? 0 : 1, transition: 'transform 420ms cubic-bezier(0.16,1,0.3,1), opacity 300ms ease' }}>
        <div style={{ background: s.glass, backdropFilter: 'blur(24px) saturate(1.4)', WebkitBackdropFilter: 'blur(24px) saturate(1.4)', border: `1px solid ${s.border}`, borderRadius: variant === 'segment' ? r.md : r.pill, boxShadow: '0 8px 30px -6px rgba(0,0,0,0.35), inset 0 1px 0 rgba(255,255,255,0.06)', overflow: 'hidden' }}>{inner}</div>
      </div>
    );
  }

  function SearchBar({ ctx, query, setQuery, res, onNav, onClose }) {
    const { s, r, platform } = ctx;
    const inputRef = useRef(null);
    useEffect(() => { const t = setTimeout(() => inputRef.current && inputRef.current.focus(), 80); return () => clearTimeout(t); }, []);
    return (
      <div style={{ position: 'absolute', top: 0, left: 0, right: 0, zIndex: 28, background: s.glass, backdropFilter: 'blur(20px)', WebkitBackdropFilter: 'blur(20px)', borderBottom: `1px solid ${s.border}`, padding: '8px 10px', display: 'flex', alignItems: 'center', gap: 8, animation: 'acksSlideDown 260ms cubic-bezier(0.16,1,0.3,1)' }}>
        <div style={{ flex: 1, display: 'flex', alignItems: 'center', gap: 8, background: s.surface2, border: `1px solid ${s.border}`, borderRadius: r.sm, padding: '0 10px', height: 38 }}>
          <Icon name="search" size={17} color={s.fg3} />
          <input ref={inputRef} value={query} onChange={e => setQuery(e.target.value)} placeholder="在文档中搜索…" style={{ flex: 1, border: 'none', outline: 'none', background: 'transparent', color: s.fg, fontSize: 14, fontFamily: "'Noto Sans SC',sans-serif" }} />
          {query && (
            <span style={{ fontSize: 12, color: res.count ? s.fg2 : s.fg3, fontFamily: "'Space Grotesk',sans-serif", whiteSpace: 'nowrap' }}>{res.count ? `${res.cur}/${res.count}` : '无结果'}</span>
          )}
        </div>
        <button onClick={() => onNav(-1)} disabled={!res.count} aria-label="prev" style={{ width: 34, height: 34, borderRadius: r.xs, border: `1px solid ${s.border}`, background: s.surface2, color: res.count ? s.fg : s.fg3, display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: res.count ? 'pointer' : 'default' }}><Icon name="chevronUp" size={18} /></button>
        <button onClick={() => onNav(1)} disabled={!res.count} aria-label="next" style={{ width: 34, height: 34, borderRadius: r.xs, border: `1px solid ${s.border}`, background: s.surface2, color: res.count ? s.fg : s.fg3, display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: res.count ? 'pointer' : 'default' }}><Icon name="chevronDown" size={18} /></button>
        <button onClick={onClose} style={{ padding: '0 10px', height: 34, border: 'none', background: 'transparent', color: s.accent, fontSize: 14, fontWeight: 600, cursor: 'pointer', fontFamily: "'Noto Sans SC',sans-serif" }}>取消</button>
      </div>
    );
  }

  function PreviewScreen({ ctx }) {
    const { s, r, doc, isHtml, docId, docState } = ctx;
    const [toolbarHidden, setToolbarHidden] = useState(false);
    const [area, setArea] = useState({ w: 360, h: 600 });
    const [phase, setPhase] = useState('rendered');
    const [largeProg, setLargeProg] = useState(0);
    const [sel, setSel] = useState(null);
    const [clearTick, setClearTick] = useState(0);
    const [zoom, setZoom] = useState(1);
    const [zoomResetTick, setZoomResetTick] = useState(0);
    const [missDismiss, setMissDismiss] = useState(false);
    const [searchOpen, setSearchOpen] = useState(false);
    const [searchQuery, setSearchQuery] = useState('');
    const [searchRes, setSearchRes] = useState({ count: 0, cur: 0 });
    const [findNavSig, setFindNavSig] = useState({ dir: 1, n: 0 });
    const [activeHead, setActiveHead] = useState(0);
    const [gotoHeadingSig, setGotoHeadingSig] = useState({ i: 0, n: 0 });
    const areaRef = useRef(null);

    useEffect(() => {
      const el = areaRef.current; if (!el) return;
      const ro = new ResizeObserver(() => setArea({ w: el.clientWidth, h: el.clientHeight }));
      ro.observe(el); setArea({ w: el.clientWidth, h: el.clientHeight });
      return () => ro.disconnect();
    }, []);

    // lifecycle state machine per document
    useEffect(() => {
      setSel(null); setZoom(1); setMissDismiss(false); setSearchOpen(false); setSearchQuery(''); setSearchRes({ count: 0, cur: 0 }); setActiveHead(0);
      // capture mode: jump straight to the relevant terminal state (no timers)
      if (window.__ACKS_CAPTURE) {
        if (docState === 'broken') setPhase('error');
        else if (docState === 'large') { setPhase('rendered'); }
        else setPhase('rendered');
        return;
      }
      const ts = [];
      setPhase('importing');
      ts.push(setTimeout(() => setPhase('rendering'), 420));
      if (docState === 'broken') {
        ts.push(setTimeout(() => setPhase('error'), 1150));
      } else if (docState === 'large') {
        ts.push(setTimeout(() => {
          setPhase('large'); setLargeProg(0);
          let p = 0; const iv = setInterval(() => { p += Math.random() * 11 + 5; if (p >= 100) { p = 100; clearInterval(iv); setLargeProg(100); setTimeout(() => setPhase('rendered'), 300); } else setLargeProg(p); }, 110);
          ts.push(() => clearInterval(iv));
        }, 850));
      } else {
        ts.push(setTimeout(() => setPhase('rendered'), 1000));
      }
      return () => ts.forEach(t => (typeof t === 'function' ? t() : clearTimeout(t)));
    }, [docId, docState]);

    const onScrollDir = useCallback((dir, y) => { if (y < 40) { setToolbarHidden(false); return; } setToolbarHidden(dir === 'down'); setSel(null); }, []);    const onLongPress = useCallback((info) => {
      const ar = areaRef.current; if (!ar) return; const rc = ar.getBoundingClientRect();
      setSel({ word: info.word, left: info.vx - rc.left, top: info.top - rc.top, bottom: info.bottom - rc.top });
    }, []);
    const closeSel = () => { setSel(null); setClearTick(t => t + 1); };
    const onZoom = useCallback((z) => { setZoom(z); }, []);
    const onSearchRes = useCallback((rs) => { setSearchRes(rs); }, []);
    const onActiveHead = useCallback((i) => { setActiveHead(i); }, []);
    const gotoHeading = useCallback((i) => { setGotoHeadingSig(p => ({ i, n: p.n + 1 })); }, []);
    ctx._gotoHeading = gotoHeading; ctx._activeHead = activeHead;

    const docHtml = window.renderMarkdown(doc.md || '');
    const vw = ctx.viewport.id === 'custom' ? ctx.customWidth : ctx.viewport.w;
    const rendered = phase === 'rendered';

    return (
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0, position: 'relative', background: gridBg(s) }}>
        <TopBar ctx={ctx} doc={doc} isHtml={isHtml} onSearch={() => setSearchOpen(true)} />
        {rendered && searchOpen && (
          <SearchBar ctx={ctx} query={searchQuery} setQuery={setSearchQuery} res={searchRes} onNav={(d) => setFindNavSig(p => ({ dir: d, n: p.n + 1 }))} onClose={() => { setSearchOpen(false); setSearchQuery(''); const f = areaRef.current; }} />
        )}
        <div ref={areaRef} style={{ flex: 1, minHeight: 0, position: 'relative' }} onClick={() => { if (sel) closeSel(); }}>
          {rendered && area.w > 10 && (
            <DocPreview docHtml={docHtml} themeId={ctx.themeId} mode={ctx.themeMode} vw={vw} ov={ctx.ov} area={area} isHtml={isHtml} htmlMode={ctx.htmlMode} onScrollDir={onScrollDir} onLongPress={isHtml ? null : onLongPress} onZoom={onZoom} clearTick={clearTick} zoomResetTick={zoomResetTick} searchQuery={searchOpen ? searchQuery : ''} findNavSig={findNavSig} gotoHeadingSig={gotoHeadingSig} onSearchRes={onSearchRes} onActiveHead={onActiveHead} extraFonts={ctx.extraFonts} />
          )}
          {(phase === 'importing' || phase === 'rendering') && <window.DocSkeleton s={s} phase={phase} />}
          {phase === 'large' && <window.LargeProgress s={s} r={r} prog={largeProg} />}
          {phase === 'error' && <window.RenderError s={s} r={r} doc={doc} />}
          {rendered && isHtml && docState === 'missing' && !missDismiss && <window.MissingAssetsBanner s={s} r={r} count={2} onDismiss={() => setMissDismiss(true)} />}
          {rendered && !isHtml && !searchOpen && <ThemeRail ctx={ctx} />}
          {rendered && <LongPressMenu ctx={ctx} sel={sel} area={area} onClose={closeSel} />}
          {rendered && ctx.viewport.id !== 'phone' && (
            <div style={{ position: 'absolute', top: 10, left: '50%', transform: 'translateX(-50%)', background: s.glass, backdropFilter: 'blur(12px)', WebkitBackdropFilter: 'blur(12px)', border: `1px solid ${s.border}`, borderRadius: 999, padding: '4px 12px', fontSize: 11, fontWeight: 600, color: s.fg2, fontFamily: "'DM Sans',sans-serif", display: 'flex', alignItems: 'center', gap: 6, pointerEvents: 'none', zIndex: 16 }}>
              <Icon name={ctx.viewport.icon} size={13} color={s.accent} />{vw}px · {ctx.viewport.name}
            </div>
          )}
          {rendered && !isHtml && Math.abs(zoom - 1) > 0.02 && (
            <button onClick={() => { setZoom(1); setZoomResetTick(t => t + 1); }} style={{ position: 'absolute', right: 12, top: 12, zIndex: 17, display: 'flex', alignItems: 'center', gap: 6, background: s.glass, backdropFilter: 'blur(12px)', WebkitBackdropFilter: 'blur(12px)', border: `1px solid ${s.accentBorder}`, borderRadius: 999, padding: '5px 11px', cursor: 'pointer', boxShadow: '0 4px 12px rgba(0,0,0,0.2)' }}>
              <Icon name="aa" size={15} color={s.accent} /><span style={{ fontSize: 12, fontWeight: 700, color: s.accent, fontFamily: "'Space Grotesk',sans-serif" }}>{zoom.toFixed(1)}×</span>
              <Icon name="x" size={13} color={s.fg3} />
            </button>
          )}
        </div>
        <BottomToolbar ctx={ctx} isHtml={isHtml} hidden={toolbarHidden} />
      </div>
    );
  }

  function gridBg(s) {
    const line = s.dark ? 'rgba(255,255,255,0.025)' : 'rgba(0,0,0,0.03)';
    return `${s.surface2} radial-gradient(${line} 1px, transparent 1px) 0 0/18px 18px`;
  }

  Object.assign(window, { PreviewScreen });
})();
