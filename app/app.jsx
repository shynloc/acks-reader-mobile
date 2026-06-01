// app.jsx — ReaderApp orchestrator + control deck + Tweaks + mount
(function () {
  const { useState, useEffect, useRef } = React;

  const HTML_DOCS = {
    'index.html': { title: '产品落地页 index.html', source: '文件管理器', size: '22.4 KB', ext: 'html' },
    'bundle.zip': { title: '组件库演示 bundle', source: '邮件', size: '1.2 MB', ext: 'html' },
  };

  // deep-link boot: #screen=preview&doc=report.md&theme=cnvertical&mode=light&sheet=theme&platform=ios&dark=0&chrome=0
  function parseBoot() {
    if (window.__ACKS_BOOT_OVERRIDE) return window.__ACKS_BOOT_OVERRIDE;
    const h = (location.hash || '').replace(/^#/, '');
    if (!h) return null;
    const q = {};
    h.split('&').forEach(kv => { const i = kv.indexOf('='); if (i > 0) q[decodeURIComponent(kv.slice(0, i))] = decodeURIComponent(kv.slice(i + 1)); });
    return Object.keys(q).length ? q : null;
  }

  function HomeIndicator({ platform, s }) {
    if (platform === 'android') {
      return (
        <div style={{ flexShrink: 0, height: 22, display: 'flex', alignItems: 'center', justifyContent: 'center', background: 'transparent', position: 'relative', zIndex: 30 }}>
          <div style={{ width: 108, height: 4, borderRadius: 3, background: s.fg, opacity: 0.5 }} />
        </div>
      );
    }
    return (
      <div style={{ flexShrink: 0, height: 26, display: 'flex', alignItems: 'flex-end', justifyContent: 'center', paddingBottom: 8, background: 'transparent', position: 'relative', zIndex: 30 }}>
        <div style={{ width: 138, height: 5, borderRadius: 3, background: s.fg, opacity: 0.85 }} />
      </div>
    );
  }

  function TocSheet({ ctx }) {
    const { s, r, doc, go } = ctx;
    const md = doc.md || '';
    const heads = md.split('\n').filter(l => /^#{1,3}\s/.test(l)).map(l => {
      const m = l.match(/^(#{1,3})\s+(.*)/); return { lv: m[1].length, t: m[2] };
    });
    const active = ctx._activeHead || 0;
    if (!heads.length) return <div style={{ padding: 24, color: s.fg3, fontSize: 13, textAlign: 'center' }}>此文档暂无目录</div>;
    return (
      <div style={{ padding: '0 14px 8px' }}>
        {heads.map((h, i) => {
          const isActive = i === active;
          return (
            <button key={i} onClick={() => { ctx._gotoHeading && ctx._gotoHeading(i); ctx.closeSheet(); }} style={{ width: '100%', display: 'flex', alignItems: 'center', gap: 10, padding: '11px 12px', paddingLeft: 12 + (h.lv - 1) * 18, border: 'none', borderBottom: i < heads.length - 1 ? `1px solid ${s.border}` : 'none', background: isActive ? s.accentSoft : 'transparent', borderRadius: r.xs, cursor: 'pointer', textAlign: 'left' }}>
              <span style={{ width: 5, height: 5, borderRadius: '50%', background: isActive ? s.accent : (h.lv === 1 ? s.fg2 : s.fg3), flexShrink: 0, boxShadow: isActive ? `0 0 0 3px ${s.accentSoft}` : 'none' }} />
              <span style={{ fontSize: h.lv === 1 ? 14.5 : 13, fontWeight: isActive ? 700 : (h.lv === 1 ? 600 : 500), color: isActive ? s.accent : (h.lv === 1 ? s.fg : s.fg2), fontFamily: "'Noto Sans SC',sans-serif" }}>{h.t}</span>
              {isActive && <span style={{ marginLeft: 'auto', fontSize: 9.5, fontWeight: 700, letterSpacing: '0.06em', color: s.accent, fontFamily: "'Space Grotesk',sans-serif" }}>当前</span>}
            </button>
          );
        })}
      </div>
    );
  }

  function ReaderApp({ platform, shellDark, tweaks, boot }) {
    const b = boot || {};
    const [screen, setScreen] = useState(b.screen || 'recent');
    const [sheet, setSheet] = useState(b.sheet || null);
    const [docId, setDocId] = useState(b.doc || 'report.md');
    const [themeId, setThemeId] = useState(b.theme || 'aireport');
    const [themeMode, setThemeMode] = useState(b.mode || (b.theme && window.THEME_MAP[b.theme] ? window.THEME_MAP[b.theme].defaultMode : 'dark'));
    const [viewportId, setViewportId] = useState(b.viewport || 'phone');
    const [customWidth, setCustomWidth] = useState(600);
    const [htmlMode, setHtmlMode] = useState('safe');
    const [ov, setOv] = useState({});

    const s = window.SHELL(shellDark, tweaks.accentMode);
    const r = window.radii(platform, tweaks.radiusStyle);
    const isHtml = !window.DOCS[docId];
    const doc = isHtml ? (HTML_DOCS[docId] || { title: docId, source: '文件', size: '—', ext: 'html', md: '' }) : window.DOCS[docId];
    const docState = (window.DOC_STATE || {})[docId] || 'ok';
    const viewport = window.VIEWPORTS.find(v => v.id === viewportId) || window.VIEWPORTS[0];
    const extraFonts = (function () {
      const out = []; const f = ov.fonts || {}; const map = window.FONT_GF || {};
      Object.keys(f).forEach(role => { if (map[f[role]]) out.push(map[f[role]]); });
      return out;
    })();

    const go = (route) => {
      if (route && route[0] === '@') { setSheet(route.slice(1)); return; }
      setSheet(null); setScreen(route);
    };
    const closeSheet = () => setSheet(null);
    const openDoc = (id, theme) => {
      setDocId(id);
      const html = !window.DOCS[id];
      if (!html && theme && window.THEME_MAP[theme]) { setThemeId(theme); setThemeMode(window.THEME_MAP[theme].defaultMode); }
      if (html) setHtmlMode('safe');
      setSheet(null);
      setScreen((window.DOC_STATE || {})[id] === 'unsupported' ? 'unsupported' : 'preview');
    };
    const setState = (patch) => {
      if ('themeId' in patch) setThemeId(patch.themeId);
      if ('themeMode' in patch) setThemeMode(patch.themeMode);
      if ('viewportId' in patch) setViewportId(patch.viewportId);
      if ('customWidth' in patch) setCustomWidth(patch.customWidth);
      if ('htmlMode' in patch) setHtmlMode(patch.htmlMode);
      if ('ov' in patch) setOv(patch.ov);
    };

    const ctx = {
      s, r, platform, go, closeSheet, openDoc, setState,
      doc, docId, docState, isHtml, themeId, themeMode, viewportId, viewport, customWidth, htmlMode, ov, extraFonts,
      toolbarVariant: tweaks.toolbarVariant, gridBg: true,
    };

    const SHEETS = {
      theme: { title: '渲染主题 Themes', full: true, comp: window.ThemePicker },
      viewport: { title: '预览视口 Viewport', comp: window.ViewportPicker },
      export: { title: '导出 Export', comp: window.ExportSheet },
      htmlsafety: { title: 'HTML 安全模式', comp: window.HtmlSafetySheet },
      typography: { title: '排版微调 Typography', full: true, comp: window.TypographyPanel },
      more: { title: '更多 More', comp: window.MoreMenu },
      docinfo: { title: '文档信息 Info', comp: window.DocInfoSheet },
      toc: { title: '目录 Contents', comp: TocSheet },
    };
    const activeSheet = sheet ? SHEETS[sheet] : null;

    let body;
    if (screen === 'firstrun') body = <window.FirstRun ctx={ctx} />;
    else if (screen === 'recent') body = <window.RecentScreen ctx={ctx} />;
    else if (screen === 'settings') body = <window.SettingsScreen ctx={ctx} />;
    else if (screen === 'unsupported') body = <window.UnsupportedScreen ctx={ctx} />;
    else body = <window.PreviewScreen ctx={ctx} />;

    // status bar text color: derive from screen bg (preview uses surface2)
    return (
      <window.DeviceFrame platform={platform} dark={shellDark} label={platform === 'ios' ? 'iOS 26 · iPhone' : 'Android 16 · Pixel'}>
        <window.StatusBar platform={platform} s={s} />
        <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0, position: 'relative' }}>
          {body}
        </div>
        <HomeIndicator platform={platform} s={s} />
        {activeSheet && (
          <window.Sheet open={!!sheet} onClose={closeSheet} s={s} r={r} platform={platform} title={activeSheet.title} full={activeSheet.full}>
            <activeSheet.comp ctx={ctx} />
          </window.Sheet>
        )}
      </window.DeviceFrame>
    );
  }

  // ---------- Top-level App with control deck + tweaks ----------
  const TWEAK_DEFAULTS = /*EDITMODE-BEGIN*/{
    "shellDark": true,
    "platform": "both",
    "radiusStyle": "platform",
    "uiFont": "grotesk",
    "toolbarVariant": "capsule",
    "accentMode": "orange"
  }/*EDITMODE-END*/;

  function FitStage({ children, deps }) {
    const wrapRef = useRef(null);
    const innerRef = useRef(null);
    const [scale, setScale] = useState(1);
    const [dim, setDim] = useState({ w: 0, h: 0 });
    const fit = () => {
      const wrap = wrapRef.current, inner = innerRef.current;
      if (!wrap || !inner) return;
      const cw = inner.scrollWidth, ch = inner.scrollHeight;
      const aw = wrap.clientWidth - 24, ah = wrap.clientHeight - 24;
      // guard: skip until layout has settled to real positive sizes
      if (aw <= 0 || ah <= 0 || cw <= 0 || ch <= 0) return;
      const sc = Math.max(0.05, Math.min(1, aw / cw, ah / ch));
      setScale(sc); setDim({ w: cw, h: ch });
    };
    useEffect(() => {
      fit();
      const raf1 = requestAnimationFrame(fit);
      const raf2 = requestAnimationFrame(() => requestAnimationFrame(fit));
      const ts = [setTimeout(fit, 80), setTimeout(fit, 300), setTimeout(fit, 800)];
      const ro = new ResizeObserver(fit);
      if (wrapRef.current) ro.observe(wrapRef.current);
      if (innerRef.current) ro.observe(innerRef.current);
      if (document.fonts && document.fonts.ready) document.fonts.ready.then(fit).catch(() => {});
      window.addEventListener('resize', fit);
      return () => { cancelAnimationFrame(raf1); cancelAnimationFrame(raf2); ts.forEach(clearTimeout); ro.disconnect(); window.removeEventListener('resize', fit); };
    }, deps);
    return (
      <div ref={wrapRef} style={{ flex: 1, minHeight: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', overflow: 'auto' }}>
        <div style={{ width: dim.w * scale, height: dim.h * scale, flexShrink: 0 }}>
          <div ref={innerRef} style={{ transform: `scale(${scale})`, transformOrigin: 'top left', width: dim.w || 'auto', display: 'inline-block' }}>
            {children}
          </div>
        </div>
      </div>
    );
  }

  function BothFrames({ dark, tweaks }) {
    const [showSecond, setShowSecond] = useState(false);
    useEffect(() => { const t = setTimeout(() => setShowSecond(true), 450); return () => clearTimeout(t); }, []);
    return (
      <div style={{ display: 'flex', gap: 40, alignItems: 'flex-start' }}>
        <ReaderApp platform="ios" shellDark={dark} tweaks={tweaks} />
        {showSecond
          ? <ReaderApp platform="android" shellDark={dark} tweaks={tweaks} />
          : <div style={{ width: 438, height: 910, borderRadius: 57, background: dark ? '#1a1a1d' : '#e4e4e8', display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}>
              <span style={{ width: 22, height: 22, borderRadius: '50%', border: '2px solid #8884', borderTopColor: '#F26419', animation: 'acksSpin 0.7s linear infinite', display: 'inline-block' }} />
            </div>}
      </div>
    );
  }

  function App() {
    const boot = parseBoot();
    const chrome = !boot || boot.chrome !== '0';
    if (boot && boot.capture === '1') {
      window.__ACKS_CAPTURE = true;
      // capture mode renders theme content in light DOM — ensure all theme fonts are present in this doc
      if (!document.getElementById('acks-theme-fonts') && window.THEME_FONTS_HREF) {
        const l = document.createElement('link'); l.id = 'acks-theme-fonts'; l.rel = 'stylesheet'; l.href = window.THEME_FONTS_HREF; document.head.appendChild(l);
      }
    }
    const [t, setTweak] = window.useTweaks(TWEAK_DEFAULTS);
    const [platform, setPlatform] = useState((boot && boot.platform) || t.platform);
    const [dark, setDark] = useState(boot && boot.dark != null ? boot.dark !== '0' : t.shellDark);
    useEffect(() => { if (chrome) setPlatform(t.platform); }, [t.platform]);
    useEffect(() => { if (chrome) setDark(t.shellDark); }, [t.shellDark]);

    const accentMode = (boot && boot.accent) || t.accentMode;
    const tweaks = { radiusStyle: (boot && boot.radius) || t.radiusStyle, uiFont: t.uiFont, toolbarVariant: (boot && boot.toolbar) || t.toolbarVariant, accentMode };

    // boot mode: single chrome-less frame, fills viewport (for embedding in spec doc)
    if (!chrome) {
      const p = platform === 'both' ? 'ios' : platform;
      // capture mode: render frame at natural size, top-left, NO transform (so html-to-image can snapshot)
      if (window.__ACKS_CAPTURE) {
        return (
          <div style={{ display: 'inline-block', background: dark ? '#0A0A0B' : '#ECECEE', padding: 16 }}>
            <ReaderApp platform={p} shellDark={dark} tweaks={tweaks} boot={boot} />
          </div>
        );
      }
      return (
        <div style={{ height: '100vh', display: 'flex', alignItems: 'center', justifyContent: 'center', background: dark ? '#0A0A0B' : '#ECECEE', padding: 8 }}>
          <FitStage deps={[p, dark, tweaks.radiusStyle, tweaks.toolbarVariant, tweaks.accentMode]}>
            <ReaderApp platform={p} shellDark={dark} tweaks={tweaks} boot={boot} />
          </FitStage>
        </div>
      );
    }

    const deckBg = dark ? '#161618' : '#FFFFFF';
    const deckBorder = dark ? '#2A2A2E' : '#E6E6EA';
    const deckFg = dark ? '#F5F5F6' : '#18181B';
    const deckFg2 = dark ? '#9A9AA0' : '#65656C';
    const accent = '#F26419';

    const frames = platform === 'both'
      ? <BothFrames dark={dark} tweaks={tweaks} />
      : <ReaderApp platform={platform} shellDark={dark} tweaks={tweaks} />;

    const segBtn = (val, label) => {
      const active = platform === val;
      return (
        <button key={val} onClick={() => { setPlatform(val); setTweak('platform', val); }} style={{
          padding: '7px 15px', border: 'none', borderRadius: 8, cursor: 'pointer',
          background: active ? accent : 'transparent', color: active ? '#fff' : deckFg2,
          fontSize: 13, fontWeight: active ? 700 : 600, fontFamily: "'Space Grotesk','Noto Sans SC',sans-serif",
          transition: 'all 200ms cubic-bezier(0.16,1,0.3,1)',
        }}>{label}</button>
      );
    };

    return (
      <div className={'acks-ui' + (t.uiFont === 'system' ? ' acks-sysfont' : '')} style={{ height: '100vh', display: 'flex', flexDirection: 'column', background: dark ? '#0A0A0B' : '#ECECEE' }}>
        <div style={{ flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 16, padding: '12px 20px', background: deckBg, borderBottom: `1px solid ${deckBorder}`, flexWrap: 'wrap' }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: 11 }}>
            <img src={window.__ACKS_LOGO || "assets/acks-n.png"} alt="" style={{ width: 26, height: 26 }} />
            <div>
              <div style={{ fontSize: 15, fontWeight: 700, color: deckFg, fontFamily: "'Space Grotesk',sans-serif", letterSpacing: '-0.01em', lineHeight: 1.1 }}>ACKS Reader</div>
              <div style={{ fontSize: 10.5, color: deckFg2, fontFamily: "'Noto Sans SC',sans-serif" }}>UI / UX 高保真原型 · 可点击</div>
            </div>
          </div>
          <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
            <div style={{ display: 'flex', gap: 2, background: dark ? '#0E0E10' : '#F1F1F3', border: `1px solid ${deckBorder}`, borderRadius: 10, padding: 3 }}>
              {segBtn('ios', 'iOS')}{segBtn('android', 'Android')}{segBtn('both', '并排')}
            </div>
            <button onClick={() => { const nd = !dark; setDark(nd); setTweak('shellDark', nd); }} style={{
              display: 'flex', alignItems: 'center', gap: 7, padding: '8px 14px', borderRadius: 10, cursor: 'pointer',
              border: `1px solid ${deckBorder}`, background: dark ? '#0E0E10' : '#F1F1F3', color: deckFg,
              fontSize: 13, fontWeight: 600, fontFamily: "'Space Grotesk','Noto Sans SC',sans-serif",
            }}>
              <Icon name={dark ? 'moon' : 'sun'} size={17} color={accent} />{dark ? '深色' : '浅色'}
            </button>
          </div>
        </div>
        <FitStage deps={[platform, dark, t.radiusStyle, t.toolbarVariant, t.uiFont]}>{frames}</FitStage>

        <window.TweaksPanel>
          <window.TweakSection label="平台 Platform" />
          <window.TweakRadio label="设备框架" value={t.platform} options={[{ value: 'ios', label: 'iOS' }, { value: 'android', label: 'Android' }, { value: 'both', label: '并排' }]} onChange={(v) => { setTweak('platform', v); }} />
          <window.TweakToggle label="深色外壳 Dark shell" value={t.shellDark} onChange={(v) => { setTweak('shellDark', v); }} />
          <window.TweakSection label="外观 Style" />
          <window.TweakRadio label="强调色" value={t.accentMode} options={[{ value: 'orange', label: '橙' }, { value: 'red', label: '龙红' }, { value: 'gradient', label: '能量渐变' }]} onChange={(v) => setTweak('accentMode', v)} />
          <window.TweakRadio label="圆角风格" value={t.radiusStyle} options={[{ value: 'platform', label: '随平台' }, { value: 'sharp', label: '锐利' }, { value: 'round', label: '圆润' }]} onChange={(v) => setTweak('radiusStyle', v)} />
          <window.TweakRadio label="界面字体" value={t.uiFont} options={[{ value: 'grotesk', label: 'Grotesk' }, { value: 'system', label: '系统字' }]} onChange={(v) => setTweak('uiFont', v)} />
          <window.TweakSection label="底部工具栏 Toolbar" />
          <window.TweakRadio label="样式变体" value={t.toolbarVariant} options={[{ value: 'capsule', label: '胶囊' }, { value: 'segment', label: '分段' }, { value: 'bar', label: '贴底' }]} onChange={(v) => setTweak('toolbarVariant', v)} />
        </window.TweaksPanel>
      </div>
    );
  }

  const _rootEl = document.getElementById('root');
  const _acksRoot = _rootEl ? ReactDOM.createRoot(_rootEl) : null;
  function _acksMount() {
    if (!_acksRoot) return;
    if (window.__ACKS_BOOT_OVERRIDE && window.__ACKS_BOOT_OVERRIDE.capture === '1') window.__ACKS_CAPTURE = true;
    _acksRoot.render(<App />);
  }
  window.__acksRemount = _acksMount;
  window.ACKSReaderApp = ReaderApp;   // exposed for the spec collection's inline harness
  if (_rootEl && !window.__ACKS_NO_AUTOMOUNT) _acksMount();
})();
