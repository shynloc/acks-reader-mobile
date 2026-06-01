// icons.jsx — Lucide-style line icons (1.5px stroke, 24x24 grid)
// Exported to window.Icon
(function () {
  const P = { fill: 'none', stroke: 'currentColor', strokeWidth: 1.6, strokeLinecap: 'round', strokeLinejoin: 'round' };

  const PATHS = {
    back:        <path d="M15 19l-7-7 7-7" {...P} />,
    chevronDown: <path d="M6 9l6 6 6-6" {...P} />,
    chevronUp:   <path d="M6 15l6-6 6 6" {...P} />,
    chevronRight:<path d="M9 6l6 6-6 6" {...P} />,
    chevronLeft: <path d="M15 6l-6 6 6 6" {...P} />,
    more:        <g {...P}><circle cx="12" cy="5" r="1.4" fill="currentColor" stroke="none"/><circle cx="12" cy="12" r="1.4" fill="currentColor" stroke="none"/><circle cx="12" cy="19" r="1.4" fill="currentColor" stroke="none"/></g>,
    moreH:       <g {...P}><circle cx="5" cy="12" r="1.4" fill="currentColor" stroke="none"/><circle cx="12" cy="12" r="1.4" fill="currentColor" stroke="none"/><circle cx="19" cy="12" r="1.4" fill="currentColor" stroke="none"/></g>,
    palette:     <g {...P}><circle cx="13.5" cy="6.5" r="1.2" fill="currentColor" stroke="none"/><circle cx="17.5" cy="10.5" r="1.2" fill="currentColor" stroke="none"/><circle cx="8.5" cy="7.5" r="1.2" fill="currentColor" stroke="none"/><circle cx="6.5" cy="12.5" r="1.2" fill="currentColor" stroke="none"/><path d="M12 2C6.5 2 2 6 2 11c0 4 3 7 7 7 1.5 0 2-1 2-2 0-.6-.3-1-.7-1.4-.4-.4-.6-.8-.6-1.3 0-1 .8-1.8 1.8-1.8H14c3.3 0 6-2.7 6-6 0-3.6-3.6-5.5-8-5.5z"/></g>,
    monitor:     <g {...P}><rect x="3" y="4" width="18" height="13" rx="2"/><path d="M8 21h8M12 17v4"/></g>,
    download:    <g {...P}><path d="M12 3v12M7 11l5 5 5-5M4 21h16"/></g>,
    share:       <g {...P}><path d="M4 12v7a1 1 0 001 1h14a1 1 0 001-1v-7M16 6l-4-4-4 4M12 2v13"/></g>,
    search:      <g {...P}><circle cx="11" cy="11" r="7"/><path d="M21 21l-4-4"/></g>,
    list:        <g {...P}><path d="M8 6h13M8 12h13M8 18h13M3.5 6h.01M3.5 12h.01M3.5 18h.01"/></g>,
    settings:    <g {...P}><circle cx="12" cy="12" r="3"/><path d="M12 2v2.5M12 19.5V22M4.9 4.9l1.8 1.8M17.3 17.3l1.8 1.8M2 12h2.5M19.5 12H22M4.9 19.1l1.8-1.8M17.3 6.7l1.8-1.8"/></g>,
    file:        <g {...P}><path d="M14 3H7a2 2 0 00-2 2v14a2 2 0 002 2h10a2 2 0 002-2V8z"/><path d="M14 3v5h5"/></g>,
    fileText:    <g {...P}><path d="M14 3H7a2 2 0 00-2 2v14a2 2 0 002 2h10a2 2 0 002-2V8z"/><path d="M14 3v5h5M9 13h6M9 17h4"/></g>,
    code:        <g {...P}><path d="M8 7l-5 5 5 5M16 7l5 5-5 5"/></g>,
    shield:      <g {...P}><path d="M12 2l8 3v6c0 5-3.5 8.5-8 11-4.5-2.5-8-6-8-11V5z"/></g>,
    shieldCheck: <g {...P}><path d="M12 2l8 3v6c0 5-3.5 8.5-8 11-4.5-2.5-8-6-8-11V5z"/><path d="M9 12l2 2 4-4"/></g>,
    zap:         <g {...P}><path d="M13 2L4 14h7l-1 8 9-12h-7z"/></g>,
    phone:       <g {...P}><rect x="6" y="2" width="12" height="20" rx="2.5"/><path d="M11 18h2"/></g>,
    a4:          <g {...P}><rect x="5" y="3" width="14" height="18" rx="1.5"/><path d="M9 8h6M9 12h6M9 16h3"/></g>,
    longimg:     <g {...P}><rect x="7" y="2" width="10" height="20" rx="1.5"/><path d="M7 9h10M7 15h10"/></g>,
    custom:      <g {...P}><path d="M4 12h16M4 12l3-3M4 12l3 3M20 12l-3-3M20 12l-3 3"/></g>,
    plus:        <path d="M12 5v14M5 12h14" {...P} />,
    check:       <path d="M5 12l5 5 9-11" {...P} />,
    x:           <path d="M6 6l12 12M18 6L6 18" {...P} />,
    sun:         <g {...P}><circle cx="12" cy="12" r="4"/><path d="M12 2v2M12 20v2M4 12H2M22 12h-2M5 5l1.5 1.5M17.5 17.5L19 19M19 5l-1.5 1.5M6.5 17.5L5 19"/></g>,
    moon:        <path d="M20 14.5A8 8 0 119.5 4a6.5 6.5 0 0010.5 10.5z" {...P} />,
    type:        <g {...P}><path d="M4 6V4h16v2M9 20h6M12 4v16"/></g>,
    image:       <g {...P}><rect x="3" y="4" width="18" height="16" rx="2"/><circle cx="8.5" cy="9.5" r="1.5"/><path d="M21 16l-5-5L5 20"/></g>,
    grid:        <g {...P}><rect x="3" y="3" width="7" height="7" rx="1.5"/><rect x="14" y="3" width="7" height="7" rx="1.5"/><rect x="3" y="14" width="7" height="7" rx="1.5"/><rect x="14" y="14" width="7" height="7" rx="1.5"/></g>,
    clock:       <g {...P}><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></g>,
    trash:       <g {...P}><path d="M4 7h16M9 7V5a1 1 0 011-1h4a1 1 0 011 1v2M6 7l1 13a1 1 0 001 1h8a1 1 0 001-1l1-13"/></g>,
    open:        <g {...P}><path d="M14 3h7v7M21 3l-9 9M19 14v5a2 2 0 01-2 2H5a2 2 0 01-2-2V7a2 2 0 012-2h5"/></g>,
    inbox:       <g {...P}><path d="M3 12h5l2 3h4l2-3h5M3 12l3-8h12l3 8v6a2 2 0 01-2 2H5a2 2 0 01-2-2z"/></g>,
    sparkle:     <g {...P}><path d="M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z"/></g>,
    layers:      <g {...P}><path d="M12 3l9 5-9 5-9-5z"/><path d="M3 13l9 5 9-5M3 8.5"/></g>,
    bookOpen:    <g {...P}><path d="M12 6c-1.5-1.2-3.5-2-6-2H3v14h3c2.5 0 4.5.8 6 2M12 6c1.5-1.2 3.5-2 6-2h3v14h-3c-2.5 0-4.5.8-6 2M12 6v14"/></g>,
    alert:       <g {...P}><path d="M12 3l9 16H3z"/><path d="M12 10v4M12 17h.01"/></g>,
    info:        <g {...P}><circle cx="12" cy="12" r="9"/><path d="M12 11v5M12 8h.01"/></g>,
    eye:         <g {...P}><path d="M2 12s3.5-7 10-7 10 7 10 7-3.5 7-10 7-10-7-10-7z"/><circle cx="12" cy="12" r="3"/></g>,
    bars:        <g {...P}><path d="M4 18V10M9 18V4M14 18v-6M19 18V7"/></g>,
    swap:        <g {...P}><path d="M7 4v13M7 17l-3-3M7 17l3-3M17 20V7M17 7l-3 3M17 7l3 3"/></g>,
    folder:      <g {...P}><path d="M3 7a2 2 0 012-2h4l2 2h8a2 2 0 012 2v8a2 2 0 01-2 2H5a2 2 0 01-2-2z"/></g>,
    drag:        <g {...P}><circle cx="9" cy="6" r="1.3" fill="currentColor" stroke="none"/><circle cx="15" cy="6" r="1.3" fill="currentColor" stroke="none"/><circle cx="9" cy="12" r="1.3" fill="currentColor" stroke="none"/><circle cx="15" cy="12" r="1.3" fill="currentColor" stroke="none"/><circle cx="9" cy="18" r="1.3" fill="currentColor" stroke="none"/><circle cx="15" cy="18" r="1.3" fill="currentColor" stroke="none"/></g>,
    aa:          <g {...P}><path d="M3 17l3.5-9 3.5 9M4.2 14h4.6M14 11.5c.8-1.3 4-1.6 4 1v4.5M18 13c-3.5 0-4.5 1-4.5 2.2 0 1 .8 1.8 2 1.8 1.5 0 2.5-1 2.5-2.5"/></g>,
    refresh:     <g {...P}><path d="M21 12a9 9 0 11-2.6-6.4M21 4v4h-4"/></g>,
    qr:          <g {...P}><rect x="3" y="3" width="7" height="7" rx="1"/><rect x="14" y="3" width="7" height="7" rx="1"/><rect x="3" y="14" width="7" height="7" rx="1"/><path d="M14 14h3v3M20 14v.01M14 20v.01M20 17v4"/></g>,
  };

  function Icon({ name, size = 22, color, style, strokeWidth }) {
    const inner = PATHS[name] || PATHS.file;
    return (
      <svg width={size} height={size} viewBox="0 0 24 24" style={{ display: 'block', color, flexShrink: 0, ...style }}
           {...(strokeWidth ? { strokeWidth } : {})}>
        {inner}
      </svg>
    );
  }
  window.Icon = Icon;
})();
