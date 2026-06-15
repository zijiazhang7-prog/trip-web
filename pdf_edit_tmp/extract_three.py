# -*- coding: utf-8 -*-
import glob
from pathlib import Path

import fitz

base = Path(r"c:\Users\Lenovo\Desktop\trip\中期")
pdf = next(base.glob("*.pdf"))
out = Path(__file__).resolve().parent
doc = fitz.open(str(pdf))
for idx, pno in enumerate([4, 5, 10]):
    page = doc.load_page(pno)
    for img in page.get_images(full=True):
        xref = img[0]
        bi = doc.extract_image(xref)
        fn = out / f"slide_{pno+1:02d}_orig.{bi['ext']}"
        fn.write_bytes(bi["image"])
        print(fn, bi["width"], bi["height"])
        break
doc.close()
