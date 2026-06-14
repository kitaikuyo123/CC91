"""
将 docs/project-summary.md 转换为带样式的 docs/report.html，
再用 Chrome headless 生成 docs/report.pdf。

依赖：pip install markdown
"""
import subprocess
import sys
from pathlib import Path

import markdown

ROOT = Path(__file__).parent
SRC_MD = ROOT / "project-summary.md"
OUT_HTML = ROOT / "report.html"
OUT_PDF = ROOT / "report.pdf"

CSS = """
@page { size: A4; margin: 18mm 16mm; }
* { box-sizing: border-box; }
body {
  font-family: "Microsoft YaHei", "PingFang SC", "Source Han Sans CN",
               "Noto Sans CJK SC", "SimSun", sans-serif;
  font-size: 11pt;
  line-height: 1.65;
  color: #1f2937;
  max-width: 100%;
  margin: 0 auto;
  padding: 0;
}
h1 { font-size: 22pt; border-bottom: 3px solid #2563eb; padding-bottom: 8px;
     margin-top: 0; color: #1e3a8a; }
h2 { font-size: 16pt; border-left: 5px solid #2563eb; padding-left: 10px;
     margin-top: 28px; color: #1e40af; page-break-after: avoid; }
h3 { font-size: 13pt; color: #1d4ed8; margin-top: 22px; page-break-after: avoid; }
h4 { font-size: 11.5pt; color: #374151; margin-top: 18px; page-break-after: avoid; }
p { margin: 8px 0; }
blockquote {
  border-left: 4px solid #f59e0b;
  background: #fffbeb;
  padding: 8px 14px;
  margin: 8px 0;
  color: #92400e;
  font-style: italic;
}
code {
  background: #f3f4f6;
  padding: 1px 5px;
  border-radius: 3px;
  font-family: "Consolas", "Source Code Pro", monospace;
  font-size: 10pt;
  color: #be185d;
}
pre {
  background: #1e293b;
  color: #e2e8f0;
  padding: 12px 16px;
  border-radius: 6px;
  overflow-x: auto;
  font-size: 9.5pt;
  line-height: 1.5;
}
pre code { background: transparent; color: inherit; padding: 0; }
table {
  border-collapse: collapse;
  width: 100%;
  margin: 12px 0;
  font-size: 10pt;
  page-break-inside: avoid;
}
th {
  background: #2563eb;
  color: white;
  padding: 8px 10px;
  text-align: left;
  border: 1px solid #1e40af;
  font-weight: 600;
}
td {
  padding: 6px 10px;
  border: 1px solid #d1d5db;
  vertical-align: top;
}
tr:nth-child(even) td { background: #f9fafb; }
ul, ol { padding-left: 24px; margin: 8px 0; }
li { margin: 3px 0; }
hr { border: none; border-top: 1px solid #d1d5db; margin: 20px 0; }
strong { color: #111827; }
a { color: #2563eb; text-decoration: none; }
"""


def build_html():
    md_text = SRC_MD.read_text(encoding="utf-8")
    html_body = markdown.markdown(
        md_text,
        extensions=["tables", "fenced_code", "toc", "sane_lists"],
    )
    html_doc = f"""<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<title>CC91 项目总结报告</title>
<style>
{CSS}
</style>
</head>
<body>
{html_body}
</body>
</html>
"""
    OUT_HTML.write_text(html_doc, encoding="utf-8")
    print(f"[OK] HTML written: {OUT_HTML} ({len(html_doc)} bytes)")


def build_pdf():
    chrome_candidates = [
        r"C:\Program Files\Google\Chrome\Application\chrome.exe",
        r"C:\Program Files (x86)\Google\Chrome\Application\chrome.exe",
    ]
    chrome = next((p for p in chrome_candidates if Path(p).exists()), None)
    if not chrome:
        print("[ERROR] Chrome not found. Please install or adjust path.")
        sys.exit(1)

    cmd = [
        chrome,
        "--headless=new",
        "--disable-gpu",
        "--no-sandbox",
        "--no-pdf-header-footer",
        f"--print-to-pdf={OUT_PDF}",
        OUT_HTML.as_uri(),
    ]
    print(f"[RUN] {' '.join(cmd)}")
    result = subprocess.run(cmd, capture_output=True, text=True, timeout=120)
    if result.returncode != 0:
        print(f"[WARN] returncode={result.returncode}")
        print("stderr:", result.stderr[:500])
    if OUT_PDF.exists():
        size_kb = OUT_PDF.stat().st_size // 1024
        print(f"[OK] PDF written: {OUT_PDF} ({size_kb} KB)")
    else:
        print(f"[ERROR] PDF not generated at {OUT_PDF}")
        sys.exit(1)


if __name__ == "__main__":
    build_html()
    build_pdf()
