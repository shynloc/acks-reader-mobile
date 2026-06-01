// data.jsx — sample documents, viewport presets, recent files, font options.
(function () {
  const DOCS = {
    'report.md': {
      title: '2026 Q2 AI 业务周报',
      format: 'markdown', ext: 'md', size: '14.2 KB', source: '飞书 Feishu',
      md: `# 2026 Q2 AI 业务周报

> [!NOTE]
> 本周交付 2 个企业 AI 训练方案，新增询价 5 笔，整体转化率较上周 **+18%**。

智能体业务持续放量。我们在企业培训与汽车改装两条线同步推进，本报告汇总关键指标、进展与下周计划。

## 核心指标 Key Metrics

| 指标 Metric | 本周 | 上周 | 变化 |
| --- | ---: | ---: | :---: |
| 活跃项目 | 7 | 5 | +2 |
| AI 智能体 | 3 | 3 | — |
| 改装订单额 | ¥48,200 | ¥40,800 | +18% |
| 客户满意度 | 96% | 94% | +2pt |

## 本周进展 Highlights

- 完成 **HKS 涡轮套件** 选型与报价
- 企业 AI 培训方案交付，客户验收通过
- 智能客服 Agent 正式上线
- [x] 部署推理服务
- [x] 压测通过 (P99 < 120ms)
- [ ] 灰度放量至 100%

## 技术备注 Tech Note

部署使用流式响应以降低首字延迟：

\`\`\`python
async def stream_reply(prompt: str):
    async for token in model.generate(prompt):
        yield token  # 首字延迟 < 300ms
\`\`\`

> 推理成本下降 32%，得益于 KV-cache 复用与批量调度。

## 下周计划 Next

1. 完成 Brembo GT 套件交付
2. 新增 2 个智能体场景
3. 启动客户案例沉淀

延迟越低，体验越[丝滑](https://example.com)。我们将持续优化这条体验曲线。`
    },
    'readme.md': {
      title: 'ACKS Reader — README',
      format: 'markdown', ext: 'md', size: '8.6 KB', source: 'Telegram',
      md: `# ACKS Reader

A mobile universal **Markdown / HTML** previewer & export tool.

## Features

- Open from WeChat, Feishu, Telegram, file managers
- 13+ design-grade rendering themes
- WYSIWYG export to PDF / long image
- Safe & interactive HTML modes

## Install

\`\`\`bash
git clone https://github.com/shynloc/acksstudio
cd acksstudio && ./gradlew installDebug
\`\`\`

> [!WARNING]
> System WebView 90+ is required for Canvas / WebGL content.

## API

| Method | Returns | Note |
| --- | --- | --- |
| \`render(src)\` | \`html\` | semantic output |
| \`export(opts)\` | \`Blob\` | pdf / png |

See \`docs/\` for the full reference.`
    },
    'essay.md': {
      title: '兰亭集序 · 节选',
      format: 'markdown', ext: 'md', size: '3.1 KB', source: '微信 WeChat',
      md: `# 兰亭集序

> 永和九年，岁在癸丑，暮春之初，会于会稽山阴之兰亭，修禊事也。

群贤毕至，少长咸集。此地有崇山峻岭，茂林修竹；又有清流激湍，映带左右，引以为流觞曲水，列坐其次。

## 游目骋怀

虽无丝竹管弦之盛，一觞一咏，亦足以畅叙幽情。是日也，天朗气清，惠风和畅。

仰观宇宙之大，俯察品类之盛，所以游目骋怀，足以极视听之娱，信可乐也。

## 感怀

夫人之相与，俯仰一世。每览昔人兴感之由，若合一契，未尝不临文嗟悼，不能喻之于怀。`
    },
    'arch.md': {
      title: '推理服务架构 & 公式',
      format: 'markdown', ext: 'md', size: '6.8 KB', source: '飞书 Feishu',
      md: `# 推理服务架构 & 公式

> [!NOTE]
> 本文演示 ACKS Reader 的 **Mermaid 流程图** 与 **数学公式** 渲染。

## 请求流转 Pipeline

\`\`\`mermaid
graph LR
A[客户端] --> B{网关鉴权}
B -->|通过| C[调度器]
B -->|拒绝| E[返回 401]
C --> D[推理引擎]
D --> F[流式响应]
\`\`\`

调度器按批量与优先级分配算力，下游推理引擎复用 KV-cache。

## 决策流程 Decision

\`\`\`mermaid
graph TD
S[收到 Prompt] --> T{命中缓存?}
T -->|是| U[直接返回]
T -->|否| V[模型生成]
V --> W[写入缓存]
W --> U
\`\`\`

## 关键公式 Formulas

注意力权重定义为 $a_i = \\frac{e^{s_i}}{\\sum_{j=1}^{n} e^{s_j}}$，其中 $s_i$ 为打分。

总延迟满足 $T_{total} \\leq T_{queue} + T_{compute}$，吞吐量 $\\rho = \\frac{\\lambda}{\\mu} < 1$。

缩放点积注意力：

$$ Attention(Q,K,V) = softmax\\left(\\frac{Q K^{T}}{\\sqrt{d_k}}\\right) V $$

成本期望：

$$ E[C] = \\sum_{i=1}^{n} p_i \\cdot c_i + \\alpha \\times \\beta $$

> [!TIP]
> 公式与图表均随主题换色，导出时所见即所得。`
    },
    'huge.md': {
      title: '年度技术白皮书（大型）',
      format: 'markdown', ext: 'md', size: '4.2 MB', source: '云盘 Cloud',
      md: ['# 年度技术白皮书\n\n> [!NOTE]\n> 本文档较大，已分块渲染以保证流畅。']
        .concat(Array.from({ length: 9 }).map((_, i) => `\n## 第 ${i + 1} 章 · 章节标题\n\n这是一段较长的正文内容，用于演示大型文档的分块渲染与滚动性能。延迟越低，体验越丝滑。\n\n| 指标 | 值 | 变化 |\n| --- | ---: | :---: |\n| 吞吐 | ${1000 + i * 120} | +${i + 2}% |\n| 延迟 | ${120 - i * 4}ms | ↓ |\n\n- 要点一\n- 要点二\n- [x] 已完成项\n\n\`\`\`js\nconst chapter = ${i + 1};\n\`\`\``)).join('\n')
    },
    'broken.md': {
      title: '损坏文档示例 broken.md',
      format: 'markdown', ext: 'md', size: '0.4 KB', source: '下载 Downloads',
      md: '# 损坏文档示例\n\n<<<<<< 这是一段无法正确解析的内容 >>>>>>\n\n| 表格 | 缺少分隔\n| 一列\n\n```\n未闭合的代码块，编码可能损坏 \ufffd\ufffd\ufffd'
    }
  };

  // demo state hints — drive the prototype's loading / error / unsupported / large flows
  const DOC_STATE = {
    'broken.md': 'broken',
    'huge.md': 'large',
    'dashboard.html': 'missing',
    'data.bin': 'unsupported',
  };

  const VIEWPORTS = [
    { id: 'phone', name: '手机', en: 'Phone', w: 390, icon: 'phone' },
    { id: 'desktop', name: '桌面', en: 'Desktop', w: 1024, icon: 'monitor' },
    { id: 'a4', name: 'A4', en: 'A4 Print', w: 794, icon: 'a4' },
    { id: 'social', name: '长图', en: 'Social', w: 480, icon: 'longimg' },
    { id: 'custom', name: '自定义', en: 'Custom', w: 600, icon: 'custom' },
  ];

  const RECENT = [
    { name: '2026 Q2 AI 业务周报', doc: 'report.md', fmt: 'MD', source: '飞书', when: '刚刚', theme: 'aireport', c: '#7C5CFF' },
    { name: 'ACKS Reader — README', doc: 'readme.md', fmt: 'MD', source: 'Telegram', when: '2 小时前', theme: 'technical', c: '#0E9F6E' },
    { name: '推理服务架构 & 公式 arch.md', doc: 'arch.md', fmt: 'MD', source: '飞书', when: '1 小时前', theme: 'technical', c: '#0E9F6E' },
    { name: '产品落地页 index.html', doc: 'index.html', fmt: 'HTML', source: '文件管理器', when: '昨天', theme: null, c: '#F26419' },
    { name: '兰亭集序 · 节选', doc: 'essay.md', fmt: 'MD', source: '微信', when: '昨天', theme: 'cnvertical', c: '#9E2B25' },
    { name: '组件库演示 bundle.zip', doc: 'bundle.zip', fmt: 'BUNDLE', source: '邮件', when: '3 天前', theme: null, c: '#3B82F6' },
    { name: '演示 · 大型白皮书 huge.md', doc: 'huge.md', fmt: 'MD', source: '云盘', when: '演示态', theme: 'business', c: '#F59E0B', demo: true },
    { name: '演示 · 看板 dashboard.html', doc: 'dashboard.html', fmt: 'HTML', source: '邮件', when: '演示态', theme: null, c: '#7C5CFF', demo: true },
    { name: '演示 · 损坏文档 broken.md', doc: 'broken.md', fmt: 'MD', source: '下载', when: '演示态', theme: null, c: '#EF4444', demo: true },
    { name: '演示 · 未知格式 data.bin', doc: 'data.bin', fmt: 'BIN', source: '下载', when: '演示态', theme: null, c: '#65656C', demo: true },
  ];

  // Google fonts for typography panel — grouped, with css-ready family value
  const FONT_OPTIONS = {
    title: [
      { label: '系统默认', v: 'inherit' },
      { label: 'Space Grotesk', v: "'Space Grotesk',sans-serif" },
      { label: 'Playfair Display', v: "'Playfair Display',serif" },
      { label: 'Anton', v: "'Anton',sans-serif" },
      { label: 'Noto Serif SC', v: "'Noto Serif SC',serif" },
      { label: 'Ma Shan Zheng 毛笔', v: "'Ma Shan Zheng',cursive" },
      { label: 'Bricolage Grotesque', v: "'Bricolage Grotesque',sans-serif", gf: 'Bricolage+Grotesque:opsz,wght@12..96,400;12..96,700;12..96,800' },
      { label: 'Syne', v: "'Syne',sans-serif", gf: 'Syne:wght@600;700;800' },
      { label: 'Libre Baskerville', v: "'Libre Baskerville',serif", gf: 'Libre+Baskerville:ital,wght@0,400;0,700;1,400' },
      { label: 'LXGW WenKai 楷体', v: "'LXGW WenKai TC',cursive", gf: 'LXGW+WenKai+TC:wght@400;700' },
    ],
    body: [
      { label: '系统默认', v: 'inherit' },
      { label: 'Inter', v: "'Inter',sans-serif" },
      { label: 'DM Sans', v: "'DM Sans',sans-serif" },
      { label: 'Lora', v: "'Lora',serif" },
      { label: 'EB Garamond', v: "'EB Garamond',serif" },
      { label: 'Noto Sans SC', v: "'Noto Sans SC',sans-serif" },
      { label: 'Noto Serif SC', v: "'Noto Serif SC',serif" },
      { label: 'Manrope', v: "'Manrope',sans-serif", gf: 'Manrope:wght@400;500;600;700' },
      { label: 'IBM Plex Sans', v: "'IBM Plex Sans',sans-serif", gf: 'IBM+Plex+Sans:wght@400;500;600;700' },
      { label: 'Spectral', v: "'Spectral',serif", gf: 'Spectral:ital,wght@0,400;0,600;1,400' },
      { label: 'LXGW WenKai 楷体', v: "'LXGW WenKai TC',serif", gf: 'LXGW+WenKai+TC:wght@400;700' },
    ],
    quote: [
      { label: '随正文', v: 'inherit' },
      { label: 'Playfair Italic', v: "'Playfair Display',serif" },
      { label: 'Cormorant', v: "'Cormorant Garamond',serif" },
      { label: 'Noto Serif SC', v: "'Noto Serif SC',serif" },
      { label: 'Spectral Italic', v: "'Spectral',serif", gf: 'Spectral:ital,wght@1,400;1,500' },
    ],
    code: [
      { label: 'JetBrains Mono', v: "'JetBrains Mono',monospace" },
      { label: '系统等宽', v: 'ui-monospace,monospace' },
      { label: 'Fira Code', v: "'Fira Code',monospace", gf: 'Fira+Code:wght@400;500' },
      { label: 'IBM Plex Mono', v: "'IBM Plex Mono',monospace", gf: 'IBM+Plex+Mono:wght@400;500' },
    ],
    table: [
      { label: '随正文', v: 'inherit' },
      { label: 'Inter', v: "'Inter',sans-serif" },
      { label: 'JetBrains Mono', v: "'JetBrains Mono',monospace" },
      { label: 'IBM Plex Sans', v: "'IBM Plex Sans',sans-serif", gf: 'IBM+Plex+Sans:wght@400;500;600' },
    ],
  };

  // map a font-family css value -> its Google Fonts spec (for on-demand loading)
  const FONT_GF = {};
  Object.keys(FONT_OPTIONS).forEach(role => FONT_OPTIONS[role].forEach(o => { if (o.gf) FONT_GF[o.v] = o.gf; }));

  const ROLE_LABELS = [
    { id: 'title', name: '标题', en: 'Title' },
    { id: 'body', name: '正文', en: 'Body' },
    { id: 'quote', name: '引用', en: 'Quote' },
    { id: 'code', name: '代码', en: 'Code' },
    { id: 'table', name: '表格', en: 'Table' },
  ];

  const ACCENT_OPTIONS = ['#F26419', '#E31E24', '#7C5CFF', '#0E9F6E', '#1F5FA8', '#9E2B25'];

  window.DOCS = DOCS;
  window.DOC_STATE = DOC_STATE;
  window.VIEWPORTS = VIEWPORTS;
  window.RECENT = RECENT;
  window.FONT_OPTIONS = FONT_OPTIONS;
  window.FONT_GF = FONT_GF;
  window.ROLE_LABELS = ROLE_LABELS;
  window.ACCENT_OPTIONS = ACCENT_OPTIONS;
})();
