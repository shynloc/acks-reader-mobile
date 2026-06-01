// states.jsx — loading / error / unsupported / large-doc / missing-assets states
(function () {
  const { useState, useEffect } = React;

  // shimmer skeleton lines mimicking a rendering document
  function Skeleton({ s, phase }) {
    const bar = (w, h = 14, mt = 12) => (
      <div style={{ width: w, height: h, borderRadius: 5, marginTop: mt, background: `linear-gradient(90deg, ${s.surface2} 25%, ${s.surfaceHi} 37%, ${s.surface2} 63%)`, backgroundSize: '400% 100%', animation: 'acksShimmer 1.3s ease-in-out infinite' }} />
    );
    return (
      <div style={{ position: 'absolute', inset: 0, background: s.surface, display: 'flex', flexDirection: 'column', zIndex: 6 }}>
        <div style={{ flex: 1, padding: '26px 24px', overflow: 'hidden' }}>
          {bar('62%', 28, 0)}
          {bar('100%')}{bar('94%')}{bar('98%')}{bar('40%')}
          <div style={{ marginTop: 22 }}>{bar('48%', 18, 0)}</div>
          {bar('100%')}{bar('90%')}
          <div style={{ marginTop: 20, height: 88, borderRadius: 8, background: s.surface2, border: `1px solid ${s.border}` }} />
          {bar('96%', 14, 20)}{bar('88%')}
        </div>
        <div style={{ flexShrink: 0, display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 9, padding: '14px', borderTop: `1px solid ${s.border}` }}>
          <span style={{ width: 15, height: 15, borderRadius: '50%', border: `2px solid ${s.border2}`, borderTopColor: s.accent, animation: 'acksSpin 0.7s linear infinite', display: 'inline-block' }} />
          <span style={{ fontSize: 12, color: s.fg2, fontFamily: "'Noto Sans SC',sans-serif" }}>{phase === 'importing' ? '正在导入文件…' : '正在渲染预览…'}</span>
        </div>
      </div>
    );
  }

  function RenderError({ s, r, doc }) {
    const [showSrc, setShowSrc] = useState(false);
    if (showSrc) {
      return (
        <div style={{ position: 'absolute', inset: 0, background: s.bg, display: 'flex', flexDirection: 'column', zIndex: 6 }}>
          <div style={{ flexShrink: 0, display: 'flex', alignItems: 'center', gap: 8, padding: '10px 14px', borderBottom: `1px solid ${s.border}`, background: s.surface }}>
            <Icon name="code" size={16} color={s.fg2} />
            <span style={{ fontSize: 12.5, color: s.fg, fontWeight: 600, fontFamily: "'Noto Sans SC',sans-serif" }}>源码回退视图</span>
            <button onClick={() => setShowSrc(false)} style={{ marginLeft: 'auto', border: 'none', background: 'transparent', color: s.accent, fontSize: 12.5, cursor: 'pointer', fontFamily: "'Noto Sans SC',sans-serif" }}>返回</button>
          </div>
          <pre style={{ flex: 1, overflow: 'auto', margin: 0, padding: 16, fontFamily: "'JetBrains Mono',monospace", fontSize: 12, lineHeight: 1.7, color: s.fg2, whiteSpace: 'pre-wrap', wordBreak: 'break-all' }}>{doc.md || '（无可显示的源内容）'}</pre>
        </div>
      );
    }
    return (
      <StateScaffold s={s} icon="alert" tint="#EF4444"
        title="渲染失败" sub="Markdown 解析中断 / WebView 返回空白页"
        body="文档中存在无法解析的结构（未闭合代码块、损坏表格或编码问题）。你可以查看原始源码，或重新导入。">
        <div style={{ display: 'flex', gap: 10, width: '100%' }}>
          <Btn s={s} r={r} onClick={() => setShowSrc(true)} primary>查看源码</Btn>
          <Btn s={s} r={r} onClick={() => {}}>重新导入</Btn>
        </div>
      </StateScaffold>
    );
  }

  function UnsupportedScreen({ ctx }) {
    const { s, r, go } = ctx;
    const fmts = ['.md', '.markdown', '.html', '.htm', '.txt', '.zip'];
    return (
      <div style={{ flex: 1, display: 'flex', flexDirection: 'column', minHeight: 0 }}>
        <div style={{ flexShrink: 0, display: 'flex', alignItems: 'center', gap: 8, padding: '6px 10px', height: 50, borderBottom: `1px solid ${s.border}`, background: s.glass, backdropFilter: 'blur(18px)', WebkitBackdropFilter: 'blur(18px)' }}>
          <button onClick={() => go('recent')} style={{ width: 40, height: 40, border: 'none', background: 'transparent', color: s.accent, display: 'flex', alignItems: 'center', cursor: 'pointer' }}><Icon name="back" size={24} /></button>
          <span style={{ fontSize: 15, fontWeight: 600, color: s.fg, fontFamily: "'Noto Sans SC',sans-serif" }}>无法打开</span>
        </div>
        <div style={{ flex: 1, minHeight: 0 }}>
          <StateScaffold s={s} icon="file" tint={s.fg3}
            title="暂不支持此文件" sub="data.bin · 未知格式"
            body="ACKS Reader 专注于文档预览，目前支持以下格式：">
            <div style={{ display: 'flex', flexWrap: 'wrap', gap: 7, justifyContent: 'center', marginBottom: 18 }}>
              {fmts.map(f => <span key={f} style={{ fontSize: 12, fontWeight: 600, color: s.accent, background: s.accentSoft, border: `1px solid ${s.accentBorder}`, padding: '4px 11px', borderRadius: r.pill, fontFamily: "'JetBrains Mono',monospace" }}>{f}</span>)}
            </div>
            <Btn s={s} r={r} onClick={() => go('recent')} primary>选择其它文件</Btn>
          </StateScaffold>
        </div>
      </div>
    );
  }

  function LargeProgress({ s, r, prog }) {
    return (
      <StateScaffold s={s} icon="layers" tint="#F59E0B"
        title="正在处理大型文档" sub="4.2 MB · 分块渲染中">
        <div style={{ width: '100%', maxWidth: 240 }}>
          <div style={{ height: 7, borderRadius: 4, background: s.border, overflow: 'hidden' }}>
            <div style={{ height: '100%', borderRadius: 4, background: '#F59E0B', width: prog + '%', transition: 'width 140ms linear' }} />
          </div>
          <div style={{ textAlign: 'center', marginTop: 9, fontSize: 11.5, color: s.fg3, fontFamily: "'Space Grotesk',sans-serif" }}>{Math.round(prog)}% · 已渲染 {Math.round(prog / 100 * 9)}/9 章</div>
        </div>
      </StateScaffold>
    );
  }

  function MissingAssetsBanner({ s, r, count, onDismiss }) {
    return (
      <div style={{ position: 'absolute', left: 10, right: 10, top: 10, zIndex: 15, display: 'flex', alignItems: 'center', gap: 10, padding: '10px 13px', borderRadius: r.md, background: s.dark ? 'rgba(245,158,11,0.14)' : 'rgba(245,158,11,0.12)', border: `1px solid rgba(245,158,11,0.4)`, backdropFilter: 'blur(12px)', WebkitBackdropFilter: 'blur(12px)' }}>
        <Icon name="alert" size={17} color="#F59E0B" />
        <div style={{ flex: 1 }}>
          <div style={{ fontSize: 12.5, fontWeight: 600, color: s.fg, fontFamily: "'Noto Sans SC',sans-serif" }}>缺失 {count} 个本地资源</div>
          <div style={{ fontSize: 10.5, color: s.fg2 }}>图片 / 样式未随文件导入，已用占位显示</div>
        </div>
        <button onClick={onDismiss} style={{ border: 'none', background: 'transparent', color: s.fg3, cursor: 'pointer', display: 'flex' }}><Icon name="x" size={16} /></button>
      </div>
    );
  }

  // shared scaffold for centered state screens
  function StateScaffold({ s, icon, tint, title, sub, body, children }) {
    return (
      <div style={{ position: 'absolute', inset: 0, display: 'flex', flexDirection: 'column', alignItems: 'center', justifyContent: 'center', textAlign: 'center', padding: '24px 30px', background: s.surface, zIndex: 6 }}>
        <div style={{ width: 64, height: 64, borderRadius: 18, background: tint + '1f', border: `1px solid ${tint}44`, color: tint, display: 'flex', alignItems: 'center', justifyContent: 'center', marginBottom: 18 }}>
          <Icon name={icon} size={30} />
        </div>
        <div style={{ fontSize: 18, fontWeight: 700, color: s.fg, fontFamily: "'Space Grotesk','Noto Sans SC',sans-serif" }}>{title}</div>
        {sub && <div style={{ fontSize: 11.5, color: s.fg3, marginTop: 4, fontFamily: "'Space Grotesk','Noto Sans SC',sans-serif" }}>{sub}</div>}
        {body && <p style={{ fontSize: 13, color: s.fg2, lineHeight: 1.6, marginTop: 12, maxWidth: 300, fontFamily: "'Noto Sans SC',sans-serif" }}>{body}</p>}
        <div style={{ marginTop: 20, width: '100%', maxWidth: 300, display: 'flex', flexDirection: 'column', alignItems: 'center' }}>{children}</div>
      </div>
    );
  }
  function Btn({ s, r, onClick, primary, children }) {
    return (
      <button onClick={onClick} style={{ flex: 1, padding: '12px 18px', borderRadius: r.sm, cursor: 'pointer', fontSize: 13.5, fontWeight: 600, fontFamily: "'Noto Sans SC',sans-serif", border: primary ? 'none' : `1px solid ${s.border}`, background: primary ? s.accent : 'transparent', color: primary ? '#fff' : s.fg, width: '100%' }}>{children}</button>
    );
  }

  Object.assign(window, { DocSkeleton: Skeleton, RenderError, UnsupportedScreen, LargeProgress, MissingAssetsBanner });
})();
