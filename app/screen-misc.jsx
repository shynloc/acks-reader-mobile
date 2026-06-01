// screen-misc.jsx — FirstRun (empty state), RecentScreen, SettingsScreen
(function () {
  const { useState } = React;
  const LOGO = window.__ACKS_LOGO || 'assets/acks-n.png';

  function Hero({ s, size = 64 }) {
    return (
      <div style={{ position: 'relative', width: size, height: size }}>
        <div style={{ position: 'absolute', inset: -10, background: `radial-gradient(circle, ${s.accentSoft} 0%, transparent 70%)`, filter: 'blur(4px)' }} />
        <img src={LOGO} alt="ACKS Reader" style={{ width: size, height: size, position: 'relative', filter: 'drop-shadow(0 6px 18px rgba(242,100,25,0.35))' }} />
      </div>
    );
  }

  function FirstRun({ ctx }) {
    const { s, r, platform, go } = ctx;
    const sources = ['微信', '飞书', 'Telegram', '文件管理', '邮件', '云盘'];
    const steps = [
      { n: '1', icon: 'inbox', t: '在任意 App 收到文件', d: '微信 / 飞书 / Telegram 里的 .md 或 .html' },
      { n: '2', icon: 'open', t: '点「打开方式」', d: '在分享菜单中选择 ACKS Reader' },
      { n: '3', icon: 'sparkle', t: '即刻精美预览', d: '切主题、换视口、导出所见即所得' },
    ];
    return (
      <div style={{ flex: 1, overflowY: 'auto', display: 'flex', flexDirection: 'column' }}>
        <div style={{ padding: '30px 24px 26px', textAlign: 'center', position: 'relative', overflow: 'hidden' }}>
          <img src={LOGO} alt="" style={{ position: 'absolute', right: -50, top: -30, width: 180, opacity: 0.05, pointerEvents: 'none' }} />
          <div style={{ display: 'flex', justifyContent: 'center', marginBottom: 18 }}><Hero s={s} size={76} /></div>
          <h1 style={{ fontSize: 27, fontWeight: 700, color: s.fg, fontFamily: "'Space Grotesk','Noto Sans SC',sans-serif", letterSpacing: '-0.02em', lineHeight: 1.15 }}>ACKS Reader</h1>
          <p style={{ fontSize: 14, color: s.fg2, marginTop: 8, lineHeight: 1.55, fontFamily: "'Noto Sans SC',sans-serif", maxWidth: 280, margin: '8px auto 0' }}>通用 Markdown / HTML 阅读器。<br/>从任意 App「打开方式」进入，精美预览并导出。</p>
        </div>

        <div style={{ padding: '0 20px', display: 'flex', flexWrap: 'wrap', gap: 7, justifyContent: 'center', marginBottom: 26 }}>
          {sources.map(x => (
            <span key={x} style={{ fontSize: 11.5, color: s.fg2, background: s.surface, border: `1px solid ${s.border}`, padding: '5px 12px', borderRadius: r.pill, fontFamily: "'Noto Sans SC',sans-serif", fontWeight: 500 }}>{x}</span>
          ))}
        </div>

        <div style={{ padding: '0 20px', display: 'flex', flexDirection: 'column', gap: 11, marginBottom: 22 }}>
          {steps.map(st => (
            <div key={st.n} style={{ display: 'flex', alignItems: 'center', gap: 14, padding: '15px 16px', background: s.surface, border: `1px solid ${s.border}`, borderRadius: r.md }}>
              <div style={{ flexShrink: 0, width: 42, height: 42, borderRadius: r.sm, background: s.accentSoft, color: s.accent, display: 'flex', alignItems: 'center', justifyContent: 'center', position: 'relative' }}>
                <Icon name={st.icon} size={22} />
                <span style={{ position: 'absolute', top: -6, left: -6, width: 19, height: 19, borderRadius: '50%', background: s.accent, color: '#fff', fontSize: 11, fontWeight: 700, display: 'flex', alignItems: 'center', justifyContent: 'center', fontFamily: "'Space Grotesk',sans-serif" }}>{st.n}</span>
              </div>
              <div style={{ flex: 1 }}>
                <div style={{ fontSize: 14.5, fontWeight: 600, color: s.fg, fontFamily: "'Noto Sans SC',sans-serif" }}>{st.t}</div>
                <div style={{ fontSize: 12, color: s.fg3, marginTop: 2, fontFamily: "'Noto Sans SC',sans-serif" }}>{st.d}</div>
              </div>
            </div>
          ))}
        </div>

        <div style={{ marginTop: 'auto', padding: '8px 20px 26px' }}>
          <button onClick={() => go('preview')} style={{ width: '100%', padding: '15px', border: 'none', background: s.accentGrad || s.accent, borderRadius: r.md, color: '#fff', fontSize: 15, fontWeight: 700, cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 8, fontFamily: "'Noto Sans SC',sans-serif", boxShadow: `0 10px 26px -10px ${s.accent}` }}>
            <Icon name="sparkle" size={18} color="#fff" /> 打开示例文档
          </button>
          <button onClick={() => go('recent')} style={{ width: '100%', marginTop: 10, padding: '13px', border: `1px solid ${s.border}`, background: 'transparent', borderRadius: r.md, color: s.fg, fontSize: 14, fontWeight: 600, cursor: 'pointer', fontFamily: "'Noto Sans SC',sans-serif" }}>查看最近文件</button>
        </div>
      </div>
    );
  }

  function RecentScreen({ ctx }) {
    const { s, r, platform, go, openDoc } = ctx;
    const big = platform === 'ios';
    const isAndroid = platform === 'android';
    const [scrollY, setScrollY] = useState(0);
    // iOS large-title collapse: 0 → expanded, 1 → collapsed
    const collapse = big ? Math.min(1, scrollY / 52) : 0;
    const RowWrap = ({ children, onClick, key2 }) => isAndroid
      ? <window.Ripple onClick={onClick} radius={r.md} color={s.accentSoft} style={{ marginBottom: 6 }}>{children}</window.Ripple>
      : <div onClick={onClick} style={{ marginBottom: 6, cursor: 'pointer', borderRadius: r.md }} onMouseEnter={e => e.currentTarget.style.background = s.surface2} onMouseLeave={e => e.currentTarget.style.background = 'transparent'}>{children}</div>;

    return (
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0, position: 'relative' }}>
        <div style={{ flexShrink: 0, padding: big ? '6px 20px 0' : '12px 20px 14px', borderBottom: `1px solid ${collapse > 0.6 || !big ? s.border : 'transparent'}`, background: big ? s.glass : s.bg, backdropFilter: big ? 'blur(18px)' : 'none', WebkitBackdropFilter: big ? 'blur(18px)' : 'none', transition: 'border-color 200ms', zIndex: 10 }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', height: big ? 36 : 'auto' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
              <img src={LOGO} alt="" style={{ width: 24, height: 24 }} />
              {/* iOS: inline compact title appears as large title collapses */}
              <span style={{ fontSize: 17, fontWeight: 700, color: s.fg, fontFamily: "'Space Grotesk','Noto Sans SC',sans-serif", letterSpacing: '-0.01em', opacity: big ? collapse : 1 }}>{big ? '最近' : <span style={{ fontSize: 19 }}>最近</span>}</span>
            </div>
            <div style={{ display: 'flex', gap: 4 }}>
              <IconButton icon="search" s={s} r={r} />
              <IconButton icon="settings" s={s} r={r} onClick={() => go('settings')} />
            </div>
          </div>
          {/* iOS large title — collapses on scroll */}
          {big && (
            <div style={{ height: (1 - collapse) * 46, overflow: 'hidden', transition: 'height 120ms linear' }}>
              <span style={{ display: 'block', fontSize: 32, fontWeight: 700, color: s.fg, fontFamily: "'Space Grotesk','Noto Sans SC',sans-serif", letterSpacing: '-0.03em', transform: `scale(${1 - collapse * 0.12})`, transformOrigin: 'left center', opacity: 1 - collapse, paddingTop: 2 }}>最近</span>
            </div>
          )}
          <div onClick={() => go('firstrun')} style={{ margin: big ? '6px 0 12px' : '12px 0 0', display: 'flex', alignItems: 'center', gap: 10, padding: '11px 13px', background: s.accentSoft, border: `1px dashed ${s.accentBorder}`, borderRadius: r.md, cursor: 'pointer', opacity: big ? (1 - collapse * 1.6) : 1, height: big ? (1 - Math.min(1, collapse * 1.6)) * 46 : 'auto', overflow: 'hidden', transition: 'opacity 120ms' }}>
            <Icon name="open" size={18} color={s.accent} />
            <span style={{ fontSize: 12.5, color: s.fg, fontFamily: "'Noto Sans SC',sans-serif", flex: 1, whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>从其它 App「打开方式」进入，文件会自动出现在这里</span>
            <Icon name="chevronRight" size={16} color={s.fg3} />
          </div>
        </div>
        <div onScroll={e => setScrollY(e.currentTarget.scrollTop)} style={{ flex: 1, overflowY: 'auto', padding: big ? '8px 14px 90px' : '10px 14px 24px' }}>
          {window.RECENT.map((f, i) => (
            <RowWrap key={i} onClick={() => openDoc(f.doc, f.theme)}>
              <div style={{ width: '100%', display: 'flex', alignItems: 'center', gap: 13, padding: '13px 12px', borderRadius: r.md, textAlign: 'left', pointerEvents: 'none' }}>
                <div style={{ flexShrink: 0, width: 46, height: 46, borderRadius: r.sm, background: f.c + '1f', border: `1px solid ${f.c}33`, display: 'flex', alignItems: 'center', justifyContent: 'center', position: 'relative' }}>
                  <Icon name={f.fmt === 'HTML' ? 'code' : f.fmt === 'BUNDLE' ? 'folder' : (f.fmt === 'BIN' ? 'file' : 'fileText')} size={22} color={f.c} />
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <div style={{ fontSize: 14, fontWeight: 600, color: s.fg, fontFamily: "'Noto Sans SC',sans-serif", whiteSpace: 'nowrap', overflow: 'hidden', textOverflow: 'ellipsis' }}>{f.name}</div>
                  <div style={{ display: 'flex', alignItems: 'center', gap: 7, marginTop: 3 }}>
                    <span style={{ fontSize: 9.5, fontWeight: 700, letterSpacing: '0.04em', color: f.c, background: f.c + '18', padding: '1px 6px', borderRadius: r.xs }}>{f.fmt}</span>
                    <span style={{ fontSize: 11, color: s.fg3, fontFamily: "'Noto Sans SC',sans-serif" }}>{f.source} · {f.when}</span>
                  </div>
                </div>
                <Icon name="chevronRight" size={18} color={s.fg3} />
              </div>
            </RowWrap>
          ))}
        </div>
        {/* Android M3 extended FAB */}
        {isAndroid && (
          <window.Ripple onClick={() => go('firstrun')} radius={r.lg} color="rgba(255,255,255,0.28)"
            style={{ position: 'absolute', right: 18, bottom: 22, width: 'max-content', background: s.accentGrad || s.accent, borderRadius: r.lg, boxShadow: `0 6px 18px -4px ${s.accent}, 0 2px 6px rgba(0,0,0,0.2)` }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: 9, padding: '15px 20px', pointerEvents: 'none' }}>
              <Icon name="plus" size={22} color="#fff" />
              <span style={{ fontSize: 14.5, fontWeight: 700, color: '#fff', fontFamily: "'Noto Sans SC',sans-serif" }}>打开文件</span>
            </div>
          </window.Ripple>
        )}
      </div>
    );
  }

  function SettingsScreen({ ctx }) {
    const { s, r, platform, go } = ctx;
    const big = platform === 'ios';
    const Group = ({ title, children }) => (
      <div style={{ marginBottom: 22 }}>
        <div style={{ fontSize: 11, fontWeight: 700, letterSpacing: '0.08em', textTransform: 'uppercase', color: s.fg3, margin: '0 4px 8px', fontFamily: "'Space Grotesk',sans-serif" }}>{title}</div>
        <div style={{ background: s.surface, border: `1px solid ${s.border}`, borderRadius: r.md, overflow: 'hidden' }}>{children}</div>
      </div>
    );
    const Row = ({ icon, label, sub, right, last, onClick }) => {
      const inner = (
        <div style={{ display: 'flex', alignItems: 'center', gap: 13, padding: '13px 14px', borderBottom: last ? 'none' : `1px solid ${s.border}`, cursor: onClick ? 'pointer' : 'default', pointerEvents: platform === 'android' && onClick ? 'none' : 'auto' }}>
          {icon && <div style={{ width: 30, height: 30, borderRadius: r.xs, background: s.accentSoft, color: s.accent, display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0 }}><Icon name={icon} size={17} /></div>}
          <div style={{ flex: 1 }}>
            <div style={{ fontSize: 14, color: s.fg, fontFamily: "'Noto Sans SC',sans-serif" }}>{label}</div>
            {sub && <div style={{ fontSize: 11, color: s.fg3, marginTop: 1 }}>{sub}</div>}
          </div>
          {right}
        </div>
      );
      if (platform === 'android' && onClick) return <window.Ripple onClick={onClick} color={s.accentSoft}>{inner}</window.Ripple>;
      return <div onClick={onClick}>{inner}</div>;
    };

    const [followSys, setFollowSys] = useState(true);
    const [defSafe, setDefSafe] = useState(true);
    const [mermaid, setMermaid] = useState(true);
    const [math, setMath] = useState(true);

    return (
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0 }}>
        <div style={{ flexShrink: 0, display: 'flex', alignItems: 'center', gap: 8, padding: '8px 12px', borderBottom: `1px solid ${s.border}`, background: s.glass, backdropFilter: 'blur(18px)', WebkitBackdropFilter: 'blur(18px)' }}>
          <button onClick={() => go('recent')} style={{ width: 40, height: 40, border: 'none', background: 'transparent', color: s.accent, display: 'flex', alignItems: 'center', cursor: 'pointer' }}><Icon name="back" size={24} /></button>
          <span style={{ fontSize: 17, fontWeight: 700, color: s.fg, fontFamily: "'Space Grotesk','Noto Sans SC',sans-serif" }}>设置</span>
        </div>
        <div style={{ flex: 1, overflowY: 'auto', padding: '18px 16px 28px' }}>
          <Group title="外观 Appearance">
            <Row icon="palette" label="默认主题" sub="新文档使用的渲染主题" right={<span style={{ fontSize: 13, color: s.fg3, display: 'flex', alignItems: 'center', gap: 4 }}>AI 报告 <Icon name="chevronRight" size={15} /></span>} onClick={() => go('@theme')} />
            <Row icon="sun" label="跟随系统明暗" sub="Follow system light / dark" right={<Toggle s={s} on={followSys} onChange={setFollowSys} />} last />
          </Group>
          <Group title="HTML 安全 Security">
            <Row icon="shield" label="未知 HTML 默认安全预览" sub="Safe Preview for unknown files" right={<Toggle s={s} on={defSafe} onChange={setDefSafe} />} last />
          </Group>
          <Group title="渲染扩展 Rendering">
            <Row icon="bars" label="Mermaid 图表" sub="按需加载，不增加启动负担" right={<Toggle s={s} on={mermaid} onChange={setMermaid} />} />
            <Row icon="code" label="数学公式 KaTeX" sub="Math rendering" right={<Toggle s={s} on={math} onChange={setMath} />} last />
          </Group>
          <Group title="存储 Storage">
            <Row icon="folder" label="沙盒占用" sub="已导入 5 个文件" right={<span style={{ fontSize: 13, color: s.fg3, fontFamily: "'Space Grotesk',sans-serif" }}>29.8 MB</span>} />
            <Row icon="trash" label="清理导入副本" sub="保留最近列表条目" right={<Icon name="chevronRight" size={15} color={s.fg3} />} last onClick={() => {}} />
          </Group>
          <Group title="关于 About">
            <Row icon="info" label="ACKS Reader" sub="版本 0.1 · 本地优先，默认不上传" right={<Icon name="chevronRight" size={15} color={s.fg3} />} last />
          </Group>
          <p style={{ textAlign: 'center', fontSize: 11, color: s.fg3, marginTop: 6, fontFamily: "'Space Grotesk',sans-serif" }}>ACKS Studio · 爱驰科驶</p>
        </div>
      </div>
    );
  }

  Object.assign(window, { FirstRun, RecentScreen, SettingsScreen });
})();
