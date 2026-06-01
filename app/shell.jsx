// shell.jsx — shell tokens, adaptive device frames (iOS/Android), UI primitives.
// Exports to window: SHELL, radii, StatusBar, DeviceFrame, Sheet, Segmented, IconButton, Toggle, useAnimatedMount
(function () {
  const { useState, useEffect, useRef } = React;

  function SHELL(dark, accentMode) {
    accentMode = accentMode || 'orange';
    const A = accentMode === 'red'
      ? { accent: '#E31E24', soft: 'rgba(227,30,36,0.13)', border: 'rgba(227,30,36,0.34)', grad: null }
      : accentMode === 'gradient'
      ? { accent: '#F26419', soft: 'rgba(242,100,25,0.13)', border: 'rgba(242,100,25,0.34)', grad: 'linear-gradient(100deg,#F26419,#E31E24)' }
      : { accent: '#F26419', soft: null, border: null, grad: null };
    return dark ? {
      dark: true,
      bg: '#0B0B0C', surface: '#151517', surface2: '#1C1C1F', surfaceHi: '#252529',
      border: '#28282C', border2: '#34343A',
      fg: '#F5F5F6', fg2: '#9A9AA0', fg3: '#5E5E64',
      accent: A.accent, accentText: '#fff', accentSoft: A.soft || 'rgba(242,100,25,0.14)', accentBorder: A.border || 'rgba(242,100,25,0.34)', accentGrad: A.grad,
      scrim: 'rgba(0,0,0,0.55)', glass: 'rgba(20,20,22,0.72)',
    } : {
      dark: false,
      bg: '#F1F1F3', surface: '#FFFFFF', surface2: '#F7F7F9', surfaceHi: '#FFFFFF',
      border: '#E6E6EA', border2: '#D7D7DC',
      fg: '#18181B', fg2: '#65656C', fg3: '#A1A1AA',
      accent: A.accent, accentText: '#fff', accentSoft: A.soft || 'rgba(242,100,25,0.10)', accentBorder: A.border || 'rgba(242,100,25,0.28)', accentGrad: A.grad,
      scrim: 'rgba(20,20,25,0.32)', glass: 'rgba(255,255,255,0.74)',
    };
  }

  // radius scale from platform + tweak ('platform'|'sharp'|'round')
  function radii(platform, style) {
    let base = platform === 'android'
      ? { xs: 8, sm: 12, md: 18, lg: 26, xl: 30, pill: 999 }
      : { xs: 6, sm: 9, md: 14, lg: 20, xl: 26, pill: 999 };
    if (style === 'sharp') base = { xs: 2, sm: 4, md: 6, lg: 8, xl: 10, pill: 6 };
    if (style === 'round') base = { xs: 12, sm: 18, md: 26, lg: 34, xl: 40, pill: 999 };
    return base;
  }

  function useAnimatedMount(open, dur = 320) {
    const [render, setRender] = useState(open);
    const [shown, setShown] = useState(false);
    useEffect(() => {
      let t;
      if (open) { setRender(true); t = setTimeout(() => setShown(true), 20); }
      else { setShown(false); t = setTimeout(() => setRender(false), dur); }
      return () => clearTimeout(t);
    }, [open]);
    return [render, shown];
  }

  function StatusBar({ platform, s, light }) {
    const fg = light != null ? (light ? '#fff' : '#000') : s.fg;
    const time = '9:41';
    if (platform === 'android') {
      return (
        <div style={{ height: 30, display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '0 18px 0 20px', position: 'relative', zIndex: 5, flexShrink: 0 }}>
          <span style={{ fontSize: 13, fontWeight: 600, color: fg, fontFamily: "'DM Sans',sans-serif" }}>{time}</span>
          <div style={{ position: 'absolute', left: '50%', top: 9, transform: 'translateX(-50%)', width: 9, height: 9, borderRadius: '50%', background: '#0a0a0a', border: `1.5px solid ${s.surface}` }} />
          <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: fg }}>
            <svg width="16" height="11" viewBox="0 0 16 11" fill="none"><path d="M8 1.5C5.5 1.5 3.3 2.5 1.7 4.1M8 5C6.6 5 5.3 5.6 4.3 6.5M8 8.5l1.6-1.7C8.7 6 7.3 6 6.4 6.8z" stroke={fg} strokeWidth="1.2" strokeLinecap="round"/></svg>
            <svg width="17" height="11" viewBox="0 0 17 11" fill="none"><path d="M1 10V4.5M5 10V2.8M9 10V1.2M13 10V0" stroke={fg} strokeWidth="1.6" strokeLinecap="round"/></svg>
            <div style={{ display: 'flex', alignItems: 'center', gap: 2 }}>
              <div style={{ width: 22, height: 11, borderRadius: 3, border: `1.4px solid ${fg}`, padding: 1.5, opacity: 0.95 }}>
                <div style={{ width: '72%', height: '100%', background: fg, borderRadius: 1 }} />
              </div>
            </div>
          </div>
        </div>
      );
    }
    // iOS — dynamic island
    return (
      <div style={{ height: 44, display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', padding: '0 26px 7px', position: 'relative', zIndex: 5, flexShrink: 0 }}>
        <span style={{ fontSize: 15, fontWeight: 600, color: fg, fontFamily: "'DM Sans',sans-serif", letterSpacing: '0.01em', minWidth: 54 }}>{time}</span>
        <div style={{ position: 'absolute', left: '50%', top: 9, transform: 'translateX(-50%)', width: 92, height: 28, borderRadius: 16, background: '#000' }} />
        <div style={{ display: 'flex', alignItems: 'center', gap: 6, color: fg }}>
          <svg width="18" height="11" viewBox="0 0 18 11" fill="none"><rect x="0" y="7" width="3" height="4" rx="1" fill={fg}/><rect x="5" y="5" width="3" height="6" rx="1" fill={fg}/><rect x="10" y="2.5" width="3" height="8.5" rx="1" fill={fg}/><rect x="15" y="0" width="3" height="11" rx="1" fill={fg} opacity="0.4"/></svg>
          <svg width="16" height="11" viewBox="0 0 16 11" fill="none"><path d="M8 2C5.4 2 3 3 1.2 4.8l1.4 1.4C4 4.8 5.9 4 8 4s4 .8 5.4 2.2l1.4-1.4C13 3 10.6 2 8 2zm0 4c-1.2 0-2.3.5-3.1 1.3L8 10.5l3.1-3.2C10.3 6.5 9.2 6 8 6z" fill={fg}/></svg>
          <div style={{ display: 'flex', alignItems: 'center', gap: 3 }}>
            <div style={{ width: 24, height: 12, borderRadius: 3.5, border: `1.4px solid ${fg}`, opacity: 0.5, padding: 1.6 }}>
              <div style={{ width: '78%', height: '100%', background: fg, borderRadius: 1.5 }} />
            </div>
          </div>
        </div>
      </div>
    );
  }

  function DeviceFrame({ platform, dark, children, label }) {
    const s = SHELL(dark);
    const isAndroid = platform === 'android';
    const W = isAndroid ? 412 : 390;
    const H = isAndroid ? 884 : 844;
    const bezel = isAndroid ? 13 : 15;
    const screenRadius = isAndroid ? 44 : 52;
    const bezelRadius = screenRadius + bezel;
    return (
      <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 14 }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
          <span style={{ fontSize: 11, fontWeight: 700, letterSpacing: '0.14em', textTransform: 'uppercase', color: '#6b6b72', fontFamily: "'Space Grotesk',sans-serif" }}>{label}</span>
        </div>
        <div style={{
          width: W + bezel * 2, height: H + bezel * 2, borderRadius: bezelRadius, padding: bezel,
          background: isAndroid ? 'linear-gradient(150deg,#3a3a40,#1a1a1d 60%)' : 'linear-gradient(155deg,#46464c,#212126 55%,#36363b)',
          boxShadow: '0 2px 4px rgba(0,0,0,0.4), 0 30px 70px -20px rgba(0,0,0,0.55), inset 0 0 0 1.5px rgba(255,255,255,0.06)',
          position: 'relative', flexShrink: 0,
        }}>
          {/* side buttons */}
          {!isAndroid && <>
            <div style={{ position: 'absolute', left: -2.5, top: 130, width: 3, height: 30, borderRadius: 2, background: '#2a2a2e' }} />
            <div style={{ position: 'absolute', left: -2.5, top: 178, width: 3, height: 54, borderRadius: 2, background: '#2a2a2e' }} />
            <div style={{ position: 'absolute', left: -2.5, top: 244, width: 3, height: 54, borderRadius: 2, background: '#2a2a2e' }} />
            <div style={{ position: 'absolute', right: -2.5, top: 200, width: 3, height: 70, borderRadius: 2, background: '#2a2a2e' }} />
          </>}
          {isAndroid && <>
            <div style={{ position: 'absolute', right: -2.5, top: 170, width: 3, height: 40, borderRadius: 2, background: '#2a2a2e' }} />
            <div style={{ position: 'absolute', right: -2.5, top: 220, width: 3, height: 64, borderRadius: 2, background: '#2a2a2e' }} />
          </>}
          <div style={{
            width: W, height: H, borderRadius: screenRadius, overflow: 'hidden', background: s.bg,
            display: 'flex', flexDirection: 'column', position: 'relative',
          }}>
            {children}
          </div>
        </div>
      </div>
    );
  }

  // bottom sheet with detents (iOS half/full), drag-to-snap, drag-to-dismiss
  function Sheet({ open, onClose, s, r, children, title, full, platform, maxHeight }) {
    const [render, shown] = useAnimatedMount(open, 360);
    const isIOS = platform === 'ios';
    const detents = isIOS ? (full ? [0.92] : [0.58, 0.92]) : [full ? 0.92 : 0.8];
    const [di, setDi] = useState(0);
    const [drag, setDrag] = useState(0);
    const startY = useRef(null);
    const frameH = platform === 'android' ? 884 : 844;
    useEffect(() => { if (open) { setDi(0); setDrag(0); } }, [open]);
    if (!render) return null;
    const sheetR = r.lg;
    const heightPct = detents[di] * 100;

    const onDown = (e) => { startY.current = (e.touches ? e.touches[0].clientY : e.clientY); };
    const onMove = (e) => { if (startY.current == null) return; const y = (e.touches ? e.touches[0].clientY : e.clientY); setDrag(y - startY.current); };
    const onUp = () => {
      if (startY.current == null) return;
      const d = drag; startY.current = null; setDrag(0);
      const threshold = frameH * 0.12;
      if (d > threshold) {
        if (di > 0) setDi(di - 1); else onClose();
      } else if (d < -threshold && di < detents.length - 1) {
        setDi(di + 1);
      }
    };

    return (
      <div style={{ position: 'absolute', inset: 0, zIndex: 40, display: 'flex', flexDirection: 'column', justifyContent: 'flex-end' }}>
        <div onClick={onClose} style={{ position: 'absolute', inset: 0, background: s.scrim, backdropFilter: 'blur(2px)', WebkitBackdropFilter: 'blur(2px)', opacity: shown ? 1 : 0, transition: 'opacity 360ms cubic-bezier(0.16,1,0.3,1)' }} />
        <div style={{
          position: 'relative', background: s.surface, borderTopLeftRadius: sheetR, borderTopRightRadius: sheetR,
          borderTop: `1px solid ${s.border}`, boxShadow: '0 -10px 40px rgba(0,0,0,0.25)',
          height: heightPct + '%', display: 'flex', flexDirection: 'column',
          transform: shown ? `translateY(${Math.max(0, drag)}px)` : 'translateY(110%)',
          transition: startY.current == null ? 'transform 400ms cubic-bezier(0.16,1,0.3,1), height 360ms cubic-bezier(0.16,1,0.3,1)' : 'none',
          paddingBottom: platform === 'android' ? 24 : 30,
        }}>
          <div onMouseDown={onDown} onMouseMove={onMove} onMouseUp={onUp} onMouseLeave={onUp}
            onTouchStart={onDown} onTouchMove={onMove} onTouchEnd={onUp}
            style={{ cursor: 'grab', flexShrink: 0, touchAction: 'none', userSelect: 'none' }}>
            <div style={{ display: 'flex', justifyContent: 'center', paddingTop: 10, paddingBottom: 2 }}>
              <div style={{ width: 38, height: 5, borderRadius: 3, background: s.border2 }} />
            </div>
            {title && (
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', padding: '8px 20px 12px' }}>
                <span style={{ fontSize: 17, fontWeight: 700, color: s.fg, fontFamily: "'Space Grotesk','Noto Sans SC',sans-serif", letterSpacing: '-0.01em' }}>{title}</span>
                <button onClick={onClose} aria-label="close" style={{ width: 30, height: 30, borderRadius: r.pill, border: 'none', background: s.surface2, color: s.fg2, display: 'flex', alignItems: 'center', justifyContent: 'center', cursor: 'pointer' }}><Icon name="x" size={17} /></button>
              </div>
            )}
          </div>
          <div style={{ overflowY: 'auto', flex: 1, WebkitOverflowScrolling: 'touch' }}>{children}</div>
        </div>
      </div>
    );
  }

  // Material ripple — wrap any tappable surface (Android). Returns ripple layer + handler.
  function Ripple({ color, children, style, onClick, disabled, radius }) {
    const [ripples, setRipples] = useState([]);
    const ref = useRef(null);
    const spawn = (e) => {
      const el = ref.current; if (!el) return;
      const rect = el.getBoundingClientRect();
      const x = (e.touches ? e.touches[0].clientX : e.clientX) - rect.left;
      const y = (e.touches ? e.touches[0].clientY : e.clientY) - rect.top;
      const size = Math.max(rect.width, rect.height) * 2;
      const id = Date.now() + Math.random();
      setRipples(rs => [...rs, { id, x, y, size }]);
      setTimeout(() => setRipples(rs => rs.filter(r => r.id !== id)), 620);
    };
    return (
      <div ref={ref} onMouseDown={disabled ? null : spawn} onClick={disabled ? null : onClick}
        style={{ position: 'relative', overflow: 'hidden', borderRadius: radius || 0, ...style }}>
        {children}
        {ripples.map(rp => (
          <span key={rp.id} style={{ position: 'absolute', left: rp.x, top: rp.y, width: rp.size, height: rp.size, marginLeft: -rp.size / 2, marginTop: -rp.size / 2, borderRadius: '50%', background: color || 'rgba(242,100,25,0.22)', pointerEvents: 'none', animation: 'acksRipple 600ms cubic-bezier(0.16,1,0.3,1)', zIndex: 0 }} />
        ))}
      </div>
    );
  }

  function Segmented({ options, value, onChange, s, r, accent, size = 'md' }) {
    const pad = size === 'sm' ? '6px 0' : '8px 0';
    const fs = size === 'sm' ? 12 : 13;
    return (
      <div style={{ display: 'flex', background: s.surface2, border: `1px solid ${s.border}`, borderRadius: r.sm, padding: 3, gap: 2, position: 'relative' }}>
        {options.map(o => {
          const active = o.value === value;
          return (
            <button key={o.value} onClick={() => onChange(o.value)} style={{
              flex: 1, padding: pad, border: 'none', cursor: 'pointer', borderRadius: r.xs,
              background: active ? (accent ? s.accent : s.surfaceHi) : 'transparent',
              color: active ? (accent ? s.accentText : s.fg) : s.fg2,
              fontSize: fs, fontWeight: active ? 600 : 500, fontFamily: "'DM Sans','Noto Sans SC',sans-serif",
              boxShadow: active && !accent ? '0 1px 3px rgba(0,0,0,0.12)' : 'none',
              transition: 'all 240ms cubic-bezier(0.16,1,0.3,1)', display: 'flex', alignItems: 'center', justifyContent: 'center', gap: 5, whiteSpace: 'nowrap',
            }}>{o.icon && <Icon name={o.icon} size={15} />}{o.label}</button>
          );
        })}
      </div>
    );
  }

  function IconButton({ icon, onClick, s, r, size = 38, active, label }) {
    return (
      <button onClick={onClick} aria-label={label} style={{
        width: size, height: size, borderRadius: r.sm, cursor: 'pointer',
        background: active ? s.accentSoft : 'transparent',
        border: active ? `1px solid ${s.accentBorder}` : `1px solid transparent`,
        color: active ? s.accent : s.fg2, display: 'flex', alignItems: 'center', justifyContent: 'center',
        transition: 'all 180ms cubic-bezier(0.16,1,0.3,1)', WebkitTapHighlightColor: 'transparent',
      }}><Icon name={icon} size={size > 40 ? 22 : 20} /></button>
    );
  }

  function Toggle({ on, onChange, s, accent }) {
    const col = accent || s.accent;
    return (
      <button onClick={() => onChange(!on)} style={{
        width: 46, height: 28, borderRadius: 999, border: 'none', cursor: 'pointer', padding: 2,
        background: on ? col : s.border2, transition: 'background 240ms cubic-bezier(0.16,1,0.3,1)', flexShrink: 0,
        display: 'flex', alignItems: 'center',
      }}>
        <div style={{
          width: 24, height: 24, borderRadius: '50%', background: '#fff',
          transform: on ? 'translateX(18px)' : 'translateX(0)',
          transition: 'transform 280ms cubic-bezier(0.34,1.4,0.5,1)', boxShadow: '0 1px 3px rgba(0,0,0,0.3)',
        }} />
      </button>
    );
  }

  Object.assign(window, { SHELL, radii, StatusBar, DeviceFrame, Sheet, Ripple, Segmented, IconButton, Toggle, useAnimatedMount });
})();
