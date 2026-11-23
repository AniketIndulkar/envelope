"""Renders app/src/main/assets/samples/*.txt to letter-like PNGs with headless Chrome (for OCR demos)."""
import html, pathlib, subprocess, tempfile
root = pathlib.Path(__file__).resolve().parent.parent
samples = root / "app/src/main/assets/samples"
chrome = "/Applications/Google Chrome.app/Contents/MacOS/Google Chrome"
css = """body{margin:0;background:#fff}
.page{box-sizing:border-box;width:900px;min-height:1270px;padding:70px 80px;font:19px/1.5 Arial,Helvetica,sans-serif;color:#1a1a1a}
.stamp{font:bold 13px Arial,sans-serif;color:#b3261e;letter-spacing:.08em;background:#fdecea;display:inline-block;padding:4px 8px;margin-bottom:28px}
pre{white-space:pre-wrap;font:inherit;margin:0}"""
for txt in sorted(samples.glob("*.txt")):
    lines = txt.read_text().splitlines()
    stamp, body = lines[0], "\n".join(lines[2:])
    page = f"<html><head><style>{css}</style></head><body><div class='page'><div class='stamp'>{html.escape(stamp)}</div><pre>{html.escape(body)}</pre></div></body></html>"
    with tempfile.NamedTemporaryFile("w", suffix=".html", delete=False) as f:
        f.write(page)
    out = txt.with_suffix(".png")
    subprocess.run([chrome, "--headless=new", "--disable-gpu", "--hide-scrollbars", f"--screenshot={out}",
                    "--window-size=900,1270", f"file://{f.name}"], check=True, capture_output=True)
    print("rendered", out.name)
