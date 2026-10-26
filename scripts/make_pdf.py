"""Usage: python scripts/make_pdf.py site site/poool-documentation.pdf

Print the MkDocs print page (all chapters, Mermaid rendered) to a PDF with headless Chromium."""
import functools, http.server, sys, threading
from playwright.sync_api import sync_playwright

site, out = sys.argv[1], sys.argv[2]
mermaid_local = sys.argv[3] if len(sys.argv) > 3 else None   # only for offline testing

handler = functools.partial(http.server.SimpleHTTPRequestHandler, directory=site)
server = http.server.ThreadingHTTPServer(("127.0.0.1", 8000), handler)
threading.Thread(target=server.serve_forever, daemon=True).start()

with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page()
    if mermaid_local:
        page.route("**/mermaid*.js", lambda r: r.fulfill(path=mermaid_local, content_type="application/javascript"))
    page.emulate_media(color_scheme="light")
    page.goto("http://127.0.0.1:8000/print_page/", wait_until="networkidle")
    page.wait_for_timeout(3000)            # let Mermaid finish drawing the diagrams
    page.pdf(path=out, format="A4", print_background=True,
             margin={"top": "15mm", "bottom": "15mm", "left": "12mm", "right": "12mm"})
    browser.close()
server.shutdown()
print("PDF written to", out)