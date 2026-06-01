// screen-panels.jsx — Theme picker, Viewport, Export, HTML safety, Typography, More menu
(function () {
  const { useState, useEffect, useRef } = React;

  const MINI_MD = `# 标题 Heading\n\n正文段落，演示字体与行距 typography.\n\n> 一段引用文字 quote\n\n- 列表项 list item\n\n\`\`\`js\nconst a = 1;\n\`\`\``;

  function ThemeThumb({ theme, mode, accent }) {
    const ref = useRef(null);
    const [scale, setScale] = useState(0.42);
    const LOGICAL = 360;
    useEffect(() => {
      const el = ref.current;
      if (!el) return;
      const ro = new ResizeObserver(() => setScale(el.clientWidth / LOGICAL));
      ro.observe(el); setScale(el.clientWidth / LOGICAL);
      return () => ro.disconnect();
    }, []);
    const m = theme.modes.includes(mode) ? mode : theme.defaultMode;
    const html = window.renderMarkdown(MINI_MD);
    // capture mode: light DOM so html-to-image can snapshot the thumbnail
    if (window.__ACKS_CAPTURE) {
      const sel = '.cap-' + theme.id + '-' + m;
      const built = window.buildThemeScoped(theme.id, { mode: m, sel });
      const bg = (theme.swatch[built.mode] && theme.swatch[built.mode][0]) || '#fff';
      const cscale = 0.42;
      return (
        <div ref={ref} style={{ width: '100%', height: 132, overflow: 'hidden', position: 'relative', pointerEvents: 'none', background: bg }}>
          <style>{built.css}</style>
          <div className={sel.slice(1)} style={{ width: LOGICAL, transform: `scale(${cscale})`, transformOrigin: 'top left', padding: 16, background: bg }}>
            <div className="md-content" dangerouslySetInnerHTML={{ __html: html }} />
          </div>
        </div>
      );
    }
    const doc = window.buildThemeDoc(theme.id, html, { mode: m, pad: 16 });
    return (
      <div ref={ref} style={{ width: '100%', height: 132, overflow: 'hidden', position: 'relative', pointerEvents: 'none' }}>
        <iframe srcDoc={doc} title={theme.id} scrolling="no" style={{
          width: LOGICAL, height: LOGICAL / scale, border: 'none',
          transform: `scale(${scale})`, transformOrigin: 'top left',
        }} />
      </div>
    );
  }

  function ThemePicker({ ctx }) {
    const { s, r, themeId, themeMode, setState } = ctx;
    const [previewMode, setPreviewMode] = useState(themeMode === 'dark' ? 'dark' : 'light');
    return (
      <div style={{ padding: '0 16px 8px' }}>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 12 }}>
          <span style={{ fontSize: 12, color: s.fg2 }}>13 套设计级排版主题 · 所见即所得</span>
          <Segmented s={s} r={r} size="sm" value={previewMode} onChange={setPreviewMode}
            options={[{ value: 'light', label: '浅', icon: 'sun' }, { value: 'dark', label: '深', icon: 'moon' }]} />
        </div>
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: 11 }}>
          {window.THEMES.map(t => {
            const active = t.id === themeId;
            return (
              <button key={t.id} onClick={() => { setState({ themeId: t.id, themeMode: t.modes.includes(previewMode) ? previewMode : t.defaultMode }); }}
                style={{
                  textAlign: 'left', padding: 0, cursor: 'pointer', overflow: 'hidden',
                  background: s.surface2, borderRadius: r.md,
                  border: active ? `2px solid ${s.accent}` : `1px solid ${s.border}`,
                  boxShadow: active ? `0 6px 20px -6px ${s.accent}55` : 'none',
                  transition: 'all 220ms cubic-bezier(0.16,1,0.3,1)', position: 'relative',
                  transform: active ? 'translateY(-2px)' : 'none',
                }}>
                <div style={{ borderBottom: `1px solid ${s.border}`, background: t.swatch[t.modes.includes(previewMode) ? previewMode : t.defaultMode] ? t.swatch[t.modes.includes(previewMode) ? previewMode : t.defaultMode][0] : '#fff' }}>
                  <ThemeThumb theme={t} mode={previewMode} accent={s.accent} />
                </div>
                {active && <div style={{ position: 'absolute', top: 8, right: 8, width: 22, height: 22, borderRadius: '50%', background: s.accent, color: '#fff', display: 'flex', alignItems: 'center', justifyContent: 'center', boxShadow: '0 2px 6px rgba(0,0,0,0.3)' }}><Icon name="check" size={14} /></div>}
                <div style={{ padding: '9px 11px 11px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                    <span style={{ fontSize: 13.5, fontWeight: 700, color: s.fg, fontFamily: "'Noto Sans SC',sans-serif" }}>{t.name}</span>
                    <span style={{ display: 'flex', gap: 3 }}>
                      {t.modes.map(md => <span key={md} title={md} style={{ width: 9, height: 9, borderRadius: '50%', border: `1px solid ${s.border2}`, background: md === 'dark' ? '#222' : '#fff' }} />)}
                    </span>
                  </div>
                  <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginTop: 3 }}>
                    <span style={{ fontSize: 10.5, color: s.fg3, fontFamily: "'Space Grotesk',sans-serif", letterSpacing: '0.01em' }}>{t.en}</span>
                    <span style={{ fontSize: 9.5, fontWeight: 700, letterSpacing: '0.04em', color: s.accent, background: s.accentSoft, padding: '1px 6px', borderRadius: r.xs }}>{t.use}</span>
                  </div>
                </div>
              </button>
            );
          })}
        </div>
        <button onClick={() => ctx.go('@typography')} style={{ width: '100%', marginTop: 14, padding: '13px', border: `1px solid ${s.border}`, background: s.surface2, borderRadius: r.md, color: s.fg, fontSize: 13, fontWeight: 600, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8, fontFamily: "'DM Sans','Noto Sans SC',sans-serif" }}>
          <Icon name="type" size={17} color={s.accent} /> 排版微调 · 字体 / 字号 / 颜色
        </button>
      </div>
    );
  }

  function ViewportPicker({ ctx }) {
    const { s, r, viewport, customWidth, setState } = ctx;
    const VPS = window.VIEWPORTS;
    return (
      <div style={{ padding: '0 16px 8px' }}>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5,1fr)', gap: 7, marginBottom: 16 }}>
          {VPS.map(v => {
            const active = v.id === viewport.id;
            return (
              <button key={v.id} onClick={() => setState({ viewportId: v.id })} style={{
                display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 6, padding: '13px 4px', cursor: 'pointer',
                background: active ? s.accentSoft : s.surface2, borderRadius: r.sm,
                border: active ? `1.5px solid ${s.accentBorder}` : `1px solid ${s.border}`,
                color: active ? s.accent : s.fg2, transition: 'all 200ms cubic-bezier(0.16,1,0.3,1)',
              }}>
                <Icon name={v.icon} size={22} />
                <span style={{ fontSize: 11, fontWeight: 600, fontFamily: "'Noto Sans SC',sans-serif", color: active ? s.fg : s.fg2 }}>{v.name}</span>
                <span style={{ fontSize: 9, color: s.fg3, fontFamily: "'Space Grotesk',sans-serif" }}>{v.id === 'custom' ? customWidth : v.w}</span>
              </button>
            );
          })}
        </div>
        {viewport.id === 'custom' && (
          <div style={{ background: s.surface2, border: `1px solid ${s.border}`, borderRadius: r.md, padding: '14px 16px', marginBottom: 14 }}>
            <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 10 }}>
              <span style={{ fontSize: 12, color: s.fg2 }}>自定义宽度 Custom width</span>
              <span style={{ fontSize: 13, fontWeight: 700, color: s.accent, fontFamily: "'Space Grotesk',sans-serif" }}>{customWidth}px</span>
            </div>
            <input type="range" min={320} max={1440} step={10} value={customWidth} onChange={e => setState({ customWidth: +e.target.value })}
              style={{ width: '100%', accentColor: s.accent }} />
          </div>
        )}
        <div style={{ display: 'flex', gap: 8, alignItems: 'flex-start', padding: '11px 13px', background: s.accentSoft, borderRadius: r.sm }}>
          <Icon name="info" size={16} color={s.accent} style={{ marginTop: 1 }} />
          <span style={{ fontSize: 11.5, color: s.fg2, lineHeight: 1.5 }}>切换视口会丝滑缩放预览内容。导出将使用当前视口宽度 —— 所见即所得。</span>
        </div>
      </div>
    );
  }

  function ExportSheet({ ctx }) {
    const { s, r } = ctx;
    const [type, setType] = useState('pdf');
    const [preset, setPreset] = useState('a4');
    const [bg, setBg] = useState(true);
    const [meta, setMeta] = useState(true);
    const [phase, setPhase] = useState('ready'); // ready | rendering | done
    const [prog, setProg] = useState(0);
    const tilesN = 24;

    const presetW = { a4: 794, phone: 390, desktop: 1024, social: 480, custom: ctx.customWidth || 600 };
    const doRealExport = () => {
      if (ctx.isHtml) { try { window.print(); } catch (e) {} return; }
      const w = presetW[preset] || 794;
      const html = window.renderMarkdown(ctx.doc.md || '');
      let docStr = window.buildThemeDoc(ctx.themeId, html, { mode: ctx.themeMode, ov: ctx.ov, pad: 36 });
      const head = meta ? `<div class="acks-dochead" style="font:600 12px 'Space Grotesk',sans-serif;color:#888;padding:0 0 14px;border-bottom:1px solid #ddd;margin-bottom:20px;">${ctx.doc.title} · ${new Date().toISOString().slice(0, 10)} · ACKS Reader</div>` : '';
      // pagination: 'pages' = discrete A4 pages with page-breaks; 'pdf' = A4 flowing; 'long' = single continuous page
      let sizeRule, marginRule, extra = '';
      if (type === 'pages') {
        sizeRule = 'A4'; marginRule = '14mm';
        // avoid splitting headings / tables / code / callouts across pages; H1/H2 start fresh after first
        extra = `.md-h1,.md-h2{break-before:auto;}.md-h2{break-before:page;}.md-content>.md-h2:first-of-type{break-before:auto;}
          h1,h2,h3{break-after:avoid;}.md-table-wrap,.md-pre,.md-callout,blockquote,.md-mermaid,.md-math-block,img,.md-img{break-inside:avoid;}
          .md-p{orphans:3;widows:3;}`;
      } else if (type === 'pdf') {
        sizeRule = preset === 'a4' ? 'A4' : (w + 'px auto'); marginRule = '14mm';
        extra = `h1,h2,h3{break-after:avoid;}.md-table-wrap,.md-pre,.md-callout,blockquote,.md-mermaid{break-inside:avoid;}`;
      } else { // long image — one continuous page
        sizeRule = w + 'px auto'; marginRule = '0';
      }
      const pageCss = `<style>@page{size:${sizeRule};margin:${marginRule};}` +
        (bg ? '' : 'body{background:#fff !important;}') +
        `body{width:${w}px;margin:0 auto;}@media print{html,body{width:auto;}${extra}}</style>`;
      docStr = docStr.replace('</head>', pageCss + '</head>').replace('<div class="md-content">', head + '<div class="md-content">');
      const win = window.open('', '_blank');
      if (!win) { alert('请允许弹出窗口以预览导出结果'); return; }
      win.document.open(); win.document.write(docStr); win.document.close();
      setTimeout(() => { try { win.focus(); win.print(); } catch (e) {} }, 600);
    };

    const presets = type === 'pdf'
      ? [{ v: 'a4', l: 'A4' }, { v: 'phone', l: '手机' }, { v: 'desktop', l: '桌面' }]
      : [{ v: 'phone', l: '手机宽' }, { v: 'social', l: '社交长图' }, { v: 'custom', l: '自定义' }];

    const run = () => {
      setPhase('rendering'); setProg(0);
      let p = 0;
      const iv = setInterval(() => {
        p += Math.random() * 9 + 4;
        if (p >= 100) { p = 100; clearInterval(iv); setProg(100); setTimeout(() => setPhase('done'), 350); }
        else setProg(p);
      }, 90);
    };

    const typeOpts = [
      { value: 'pdf', label: 'PDF', icon: 'a4' },
      { value: 'long', label: '长图', icon: 'longimg' },
      { value: 'pages', label: '分页图', icon: 'grid' },
    ];

    if (phase === 'rendering' || phase === 'done') {
      const litTiles = Math.round((prog / 100) * tilesN);
      return (
        <div style={{ padding: '4px 18px 14px' }}>
          <div style={{ textAlign: 'center', marginBottom: 8 }}>
            <span style={{ fontSize: 13, color: s.fg2, fontFamily: "'Noto Sans SC',sans-serif" }}>{phase === 'done' ? '导出完成' : '正在渲染'} · {type.toUpperCase()}</span>
          </div>
          <div style={{ margin: '0 auto 18px', width: 168, aspectRatio: '3/4', background: s.surface2, border: `1px solid ${s.border}`, borderRadius: r.md, padding: 12, position: 'relative', overflow: 'hidden' }}>
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4,1fr)', gridTemplateRows: `repeat(${tilesN / 4},1fr)`, gap: 4, height: '100%' }}>
              {Array.from({ length: tilesN }).map((_, i) => (
                <div key={i} style={{
                  borderRadius: 3, background: i < litTiles ? s.accent : s.border,
                  opacity: i < litTiles ? (0.55 + 0.45 * Math.min(1, (litTiles - i) / 3)) : 0.5,
                  transform: i < litTiles ? 'scale(1)' : 'scale(0.86)',
                  transition: 'all 200ms cubic-bezier(0.16,1,0.3,1)',
                }} />
              ))}
            </div>
            {phase === 'done' && <div style={{ position: 'absolute', inset: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', background: s.scrim }}>
              <div style={{ width: 52, height: 52, borderRadius: '50%', background: '#22C55E', display: 'flex', alignItems: 'center', justifyContent: 'center', boxShadow: '0 6px 20px rgba(34,197,94,0.4)' }}><Icon name="check" size={30} color="#fff" /></div>
            </div>}
          </div>
          <div style={{ height: 6, borderRadius: 3, background: s.border, overflow: 'hidden', marginBottom: 6 }}>
            <div style={{ height: '100%', borderRadius: 3, background: phase === 'done' ? '#22C55E' : s.accent, width: prog + '%', transition: 'width 160ms linear' }} />
          </div>
          <div style={{ textAlign: 'center', fontSize: 11, color: s.fg3, fontFamily: "'Space Grotesk',sans-serif", marginBottom: 16 }}>
            {phase === 'done' ? '所见即所得 · 当前主题与视口宽度' : `渲染分块 ${Math.round(prog / 100 * tilesN)}/${tilesN} · ${Math.round(prog)}%`}
          </div>
          {phase === 'done' && (
            <div style={{ display: 'flex', gap: 10 }}>
              <button onClick={() => setPhase('ready')} style={{ flex: 1, padding: '13px', border: `1px solid ${s.border}`, background: s.surface2, borderRadius: r.sm, color: s.fg, fontSize: 14, fontWeight: 600, cursor: 'pointer', fontFamily: "'Noto Sans SC',sans-serif" }}>重新设置</button>
              <button onClick={doRealExport} style={{ flex: 1.4, padding: '13px', border: 'none', background: s.accent, borderRadius: r.sm, color: '#fff', fontSize: 14, fontWeight: 700, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 7, fontFamily: "'Noto Sans SC',sans-serif" }}><Icon name="download" size={17} color="#fff" />预览 / 保存</button>
            </div>
          )}
        </div>
      );
    }

    return (
      <div style={{ padding: '0 16px 8px' }}>
        <Label s={s}>输出格式 Format</Label>
        <Segmented s={s} r={r} value={type} onChange={(v) => { setType(v); setPreset(v === 'pdf' ? 'a4' : 'phone'); }} options={typeOpts} />
        <Label s={s} top>预设 Preset</Label>
        <div style={{ display: 'flex', gap: 7 }}>
          {presets.map(p => (
            <button key={p.v} onClick={() => setPreset(p.v)} style={{ flex: 1, padding: '11px 4px', border: preset === p.v ? `1.5px solid ${s.accentBorder}` : `1px solid ${s.border}`, background: preset === p.v ? s.accentSoft : s.surface2, color: preset === p.v ? s.accent : s.fg2, borderRadius: r.sm, fontSize: 12.5, fontWeight: 600, cursor: 'pointer', fontFamily: "'Noto Sans SC',sans-serif" }}>{p.l}</button>
          ))}
        </div>
        <div style={{ marginTop: 16, background: s.surface2, border: `1px solid ${s.border}`, borderRadius: r.md, overflow: 'hidden' }}>
          <RowToggle s={s} label="包含背景色" sub="Background" on={bg} onChange={setBg} border />
          <RowToggle s={s} label="附文件名与日期" sub="Title & date" on={meta} onChange={setMeta} />
        </div>
        <button onClick={run} style={{ width: '100%', marginTop: 16, padding: '15px', border: 'none', background: s.accentGrad || s.accent, borderRadius: r.md, color: '#fff', fontSize: 15, fontWeight: 700, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8, fontFamily: "'Noto Sans SC',sans-serif", boxShadow: `0 8px 22px -8px ${s.accent}` }}>
          <Icon name="download" size={19} color="#fff" /> 开始导出 {type === 'pdf' ? 'PDF' : type === 'long' ? '长图' : '分页图'}
        </button>
      </div>
    );
  }

  function HtmlSafetySheet({ ctx }) {
    const { s, r, htmlMode, setState } = ctx;
    const interactive = htmlMode === 'interactive';
    return (
      <div style={{ padding: '0 18px 8px' }}>
        <div style={{ display: 'flex', flexDirection: 'column', gap: 10 }}>
          {[
            { id: 'safe', icon: 'shieldCheck', title: '安全预览', en: 'Safe Preview', desc: '暂停脚本与网络请求，仅渲染静态内容。适合未知来源文件。', risk: false },
            { id: 'interactive', icon: 'zap', title: '完整交互', en: 'Full Interactive', desc: '允许 JavaScript、动画、Canvas / SVG / WebGL 运行。仅对信任文件启用。', risk: true },
          ].map(o => {
            const active = (o.id === 'interactive') === interactive;
            return (
              <button key={o.id} onClick={() => setState({ htmlMode: o.id })} style={{
                textAlign: 'left', display: 'flex', gap: 13, padding: '15px 15px', cursor: 'pointer',
                background: active ? s.accentSoft : s.surface2, borderRadius: r.md,
                border: active ? `1.5px solid ${s.accentBorder}` : `1px solid ${s.border}`, transition: 'all 200ms',
              }}>
                <div style={{ flexShrink: 0, width: 40, height: 40, borderRadius: r.sm, background: active ? s.accent : s.surfaceHi, color: active ? '#fff' : s.fg2, display: 'flex', alignItems: 'center', justifyContent: 'center', border: active ? 'none' : `1px solid ${s.border}` }}>
                  <Icon name={o.icon} size={22} />
                </div>
                <div style={{ flex: 1 }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                    <span style={{ fontSize: 15, fontWeight: 700, color: s.fg, fontFamily: "'Noto Sans SC',sans-serif" }}>{o.title}</span>
                    <span style={{ fontSize: 10, color: s.fg3, fontFamily: "'Space Grotesk',sans-serif" }}>{o.en}</span>
                    {active && <Icon name="check" size={16} color={s.accent} style={{ marginLeft: 'auto' }} />}
                  </div>
                  <p style={{ fontSize: 12, color: s.fg2, lineHeight: 1.5, marginTop: 4 }}>{o.desc}</p>
                </div>
              </button>
            );
          })}
        </div>
        {interactive && (
          <label style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 12, padding: '12px 14px', background: s.surface2, border: `1px solid ${s.border}`, borderRadius: r.sm, cursor: 'pointer' }}>
            <input type="checkbox" style={{ accentColor: s.accent, width: 17, height: 17 }} />
            <span style={{ fontSize: 12.5, color: s.fg, fontFamily: "'Noto Sans SC',sans-serif" }}>始终信任此文件 <span style={{ color: s.fg3 }}>Always trust</span></span>
          </label>
        )}
        <div style={{ display: 'flex', gap: 8, alignItems: 'flex-start', marginTop: 12, padding: '11px 13px', background: s.dark ? 'rgba(245,158,11,0.1)' : 'rgba(245,158,11,0.08)', borderRadius: r.sm }}>
          <Icon name="shield" size={16} color="#F59E0B" style={{ marginTop: 1 }} />
          <span style={{ fontSize: 11.5, color: s.fg2, lineHeight: 1.5 }}>文件运行于沙盒内，无法访问其它本地文件或上传内容。外部链接将经确认后用系统浏览器打开。</span>
        </div>
      </div>
    );
  }

  function TypographyPanel({ ctx }) {
    const { s, r, ov, setState } = ctx;
    const [loading, setLoading] = useState({});
    const famName = (v) => { const m = v && v.match(/'([^']+)'/); return m ? m[1] : null; };
    // preload preview faces for the catalog so sample labels render in their own font
    useEffect(() => {
      Object.keys(window.FONT_OPTIONS).forEach(role => window.FONT_OPTIONS[role].forEach(o => { if (o.gf && window.ensureGoogleFont) window.ensureGoogleFont(o.gf); }));
    }, []);
    const set = (patch) => setState({ ov: { ...ov, ...patch } });
    const setFont = (role, v, gf) => {
      if (gf && window.ensureGoogleFont) {
        window.ensureGoogleFont(gf);
        const fam = famName(v);
        if (fam && document.fonts && document.fonts.load) {
          setLoading(l => ({ ...l, [role]: true }));
          Promise.race([document.fonts.load(`16px '${fam}'`), new Promise(res => setTimeout(res, 2500))])
            .then(() => setLoading(l => ({ ...l, [role]: false })));
        }
      }
      set({ fonts: { ...(ov.fonts || {}), [role]: v } });
    };
    const fonts = ov.fonts || {};
    return (
      <div style={{ padding: '0 16px 8px' }}>
        <p style={{ fontSize: 11.5, color: s.fg3, marginBottom: 14, lineHeight: 1.5 }}>这些是<b style={{ color: s.fg2 }}>预览设置</b>，按角色覆盖主题字体——支持 Google Fonts 按需加载。ACKS Reader 是阅读器，不修改原文。</p>
        {window.ROLE_LABELS.map(role => (
          <div key={role.id} style={{ marginBottom: 13 }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: 6 }}>
              <span style={{ fontSize: 12.5, fontWeight: 600, color: s.fg, fontFamily: "'Noto Sans SC',sans-serif" }}>{role.name} <span style={{ fontSize: 10, color: s.fg3, fontWeight: 400 }}>{role.en}</span></span>
              {loading[role.id] && <span style={{ display: 'flex', alignItems: 'center', gap: 5, fontSize: 10.5, color: s.accent, fontFamily: "'Space Grotesk',sans-serif" }}><span style={{ width: 10, height: 10, borderRadius: '50%', border: `1.5px solid ${s.accentBorder}`, borderTopColor: s.accent, animation: 'acksSpin 0.7s linear infinite', display: 'inline-block' }} />加载中</span>}
            </div>
            <div style={{ display: 'flex', gap: 6, overflowX: 'auto', paddingBottom: 4, WebkitOverflowScrolling: 'touch' }}>
              {window.FONT_OPTIONS[role.id].map(f => {
                const active = (fonts[role.id] || 'inherit') === f.v;
                return (
                  <button key={f.label} onClick={() => setFont(role.id, f.v, f.gf)} style={{
                    flexShrink: 0, padding: '8px 13px', borderRadius: r.sm, cursor: 'pointer', whiteSpace: 'nowrap',
                    border: active ? `1.5px solid ${s.accentBorder}` : `1px solid ${s.border}`,
                    background: active ? s.accentSoft : s.surface2, color: active ? s.accent : s.fg2,
                    fontSize: 13, fontFamily: f.v === 'inherit' ? "'Noto Sans SC',sans-serif" : f.v, fontWeight: active ? 600 : 500,
                    display: 'flex', alignItems: 'center', gap: 5,
                  }}>{f.gf && <span style={{ width: 5, height: 5, borderRadius: '50%', background: active ? s.accent : s.fg3 }} title="Google Font" />}{f.label}</button>
                );
              })}
            </div>
          </div>
        ))}
        <div style={{ marginTop: 4, marginBottom: 14, background: s.surface2, border: `1px solid ${s.border}`, borderRadius: r.md, padding: '14px 16px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 10 }}>
            <span style={{ fontSize: 12.5, fontWeight: 600, color: s.fg, fontFamily: "'Noto Sans SC',sans-serif" }}>正文字号 Base size</span>
            <span style={{ fontSize: 13, fontWeight: 700, color: s.accent, fontFamily: "'Space Grotesk',sans-serif" }}>{ov.baseSize || 16}px</span>
          </div>
          <input type="range" min={13} max={24} step={0.5} value={ov.baseSize || 16} onChange={e => set({ baseSize: +e.target.value })} style={{ width: '100%', accentColor: s.accent }} />
        </div>
        <Label s={s}>强调色 Accent</Label>
        <div style={{ display: 'flex', gap: 9 }}>
          {window.ACCENT_OPTIONS.map(c => {
            const active = (ov.colors && ov.colors.accent) === c;
            return <button key={c} onClick={() => set({ colors: { ...(ov.colors || {}), accent: c } })} style={{ width: 34, height: 34, borderRadius: '50%', background: c, cursor: 'pointer', border: active ? `3px solid ${s.fg}` : `2px solid ${s.surface}`, boxShadow: active ? `0 0 0 1.5px ${c}` : 'none', transition: 'all 160ms' }} />;
          })}
          <button onClick={() => set({ colors: {} })} style={{ width: 34, height: 34, borderRadius: '50%', background: s.surface2, border: `1px dashed ${s.border2}`, color: s.fg3, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center' }}><Icon name="refresh" size={15} /></button>
        </div>
        <button onClick={() => setState({ ov: {} })} style={{ width: '100%', marginTop: 16, padding: '12px', border: `1px solid ${s.border}`, background: 'transparent', borderRadius: r.sm, color: s.fg2, fontSize: 13, fontWeight: 600, cursor: 'pointer', fontFamily: "'Noto Sans SC',sans-serif" }}>恢复主题默认排版</button>
      </div>
    );
  }

  function MoreMenu({ ctx }) {
    const { s, r, go } = ctx;
    const items = [
      { icon: 'info', label: '文档信息', en: 'Document info', act: () => go('@docinfo') },
      { icon: 'type', label: '排版微调', en: 'Typography', act: () => go('@typography') },
      { icon: 'clock', label: '最近文件', en: 'Recent files', act: () => go('recent') },
      { icon: 'settings', label: '设置', en: 'Settings', act: () => go('settings') },
    ];
    return (
      <div style={{ padding: '0 14px 8px' }}>
        {items.map((it, i) => (
          <button key={it.label} onClick={it.act} style={{ width: '100%', display: 'flex', alignItems: 'center', gap: 13, padding: '14px 12px', border: 'none', borderBottom: i < items.length - 1 ? `1px solid ${s.border}` : 'none', background: 'transparent', cursor: 'pointer', textAlign: 'left' }}>
            <Icon name={it.icon} size={21} color={s.fg2} />
            <span style={{ fontSize: 15, color: s.fg, fontFamily: "'Noto Sans SC',sans-serif" }}>{it.label}</span>
            <span style={{ marginLeft: 'auto', fontSize: 11, color: s.fg3, fontFamily: "'Space Grotesk',sans-serif" }}>{it.en}</span>
          </button>
        ))}
      </div>
    );
  }

  function DocInfoSheet({ ctx }) {
    const { s, r, doc, isHtml } = ctx;
    const rows = [
      ['文件名', doc.title], ['格式', isHtml ? 'HTML' : 'Markdown'], ['来源 App', doc.source],
      ['大小', doc.size], ['导入时间', '2026-05-31 10:00'], ['校验', 'sha256:7f3a…e2'],
    ];
    return (
      <div style={{ padding: '0 18px 10px' }}>
        <div style={{ background: s.surface2, border: `1px solid ${s.border}`, borderRadius: r.md, overflow: 'hidden' }}>
          {rows.map(([k, v], i) => (
            <div key={k} style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', padding: '12px 15px', borderBottom: i < rows.length - 1 ? `1px solid ${s.border}` : 'none' }}>
              <span style={{ fontSize: 12.5, color: s.fg2, fontFamily: "'Noto Sans SC',sans-serif" }}>{k}</span>
              <span style={{ fontSize: 12.5, color: s.fg, fontWeight: 500, fontFamily: "'DM Sans','Noto Sans SC',sans-serif", maxWidth: 200, textAlign: 'right', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>{v}</span>
            </div>
          ))}
        </div>
        <div style={{ display: 'flex', gap: 8, alignItems: 'flex-start', marginTop: 12, padding: '11px 13px', background: s.accentSoft, borderRadius: r.sm }}>
          <Icon name="shield" size={15} color={s.accent} style={{ marginTop: 1 }} />
          <span style={{ fontSize: 11.5, color: s.fg2, lineHeight: 1.5 }}>文件已复制进沙盒，本地优先，默认不上传任何内容。</span>
        </div>
      </div>
    );
  }

  // small helpers
  function Label({ children, s, top }) {
    return <div style={{ fontSize: 11, fontWeight: 700, letterSpacing: '0.08em', textTransform: 'uppercase', color: s.fg3, margin: top ? '18px 0 9px' : '0 0 9px', fontFamily: "'Space Grotesk',sans-serif" }}>{children}</div>;
  }
  function RowToggle({ s, label, sub, on, onChange, border }) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '13px 15px', borderBottom: border ? `1px solid ${s.border}` : 'none' }}>
        <div><div style={{ fontSize: 13.5, color: s.fg, fontWeight: 500, fontFamily: "'Noto Sans SC',sans-serif" }}>{label}</div><div style={{ fontSize: 10.5, color: s.fg3 }}>{sub}</div></div>
        <Toggle s={s} on={on} onChange={onChange} />
      </div>
    );
  }

  Object.assign(window, { ThemePicker, ViewportPicker, ExportSheet, HtmlSafetySheet, TypographyPanel, MoreMenu, DocInfoSheet, PanelLabel: Label, PanelRowToggle: RowToggle });
})();
