#!/usr/bin/env bash
# ACKS Reader — 下载本地字体 (通过 Google Fonts CSS API 动态获取真实 URL)
# 可从任意位置执行，自动定位到正确的项目目录

set -e

# 脚本自身所在目录 → 上一级就是 android/ 项目根
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"
DIR="$PROJECT_DIR/app/src/main/assets/fonts"

mkdir -p "$DIR"
echo "📁 字体输出目录: $DIR"
echo ""

# Android Chrome UA — 让 Google Fonts 返回 WOFF2 格式
UA="Mozilla/5.0 (Linux; Android 10; Pixel 4) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

fetch_font() {
    local family="$1"
    local outfile="$2"

    echo "⬇  $family ..."
    local api_url="https://fonts.googleapis.com/css2?family=${family}&display=swap"
    local css
    css=$(curl -fsSL -A "$UA" "$api_url")

    # 优先取 /* latin */ 注释后的 WOFF2 URL；没有则取最后一个
    local woff2_url
    woff2_url=$(printf '%s\n' "$css" \
        | grep -A8 '/\* latin \*/' \
        | grep -o 'https://fonts\.gstatic\.com[^)]*\.woff2' \
        | head -1)

    if [ -z "$woff2_url" ]; then
        woff2_url=$(printf '%s\n' "$css" \
            | grep -o 'https://fonts\.gstatic\.com[^)]*\.woff2' \
            | tail -1)
    fi

    if [ -z "$woff2_url" ]; then
        echo "   ❌ 找不到 WOFF2 URL，跳过"
        return 1
    fi

    echo "   → $woff2_url"
    curl -fsSL "$woff2_url" -o "$DIR/$outfile"
    echo "   ✓ $(du -sh "$DIR/$outfile" | cut -f1)  →  $outfile"
}

fetch_font "Space+Grotesk:wght@300..700"       "SpaceGrotesk.woff2"
fetch_font "DM+Sans:opsz,wght@9..40,300..700"  "DMSans.woff2"
fetch_font "JetBrains+Mono:wght@400..700"      "JetBrainsMono.woff2"

echo ""
echo "✅ 完成"
ls -lh "$DIR/"*.woff2 2>/dev/null
