# -*- coding: utf-8 -*-
from pathlib import Path

import fitz

mid = Path(r"c:\Users\Lenovo\Desktop\trip\中期")
rev = mid / "Zhiyouxing-Midterm-report-REVISED.pdf"
doc = fitz.open(str(rev))
for i in [4, 5, 10]:
    pix = doc.load_page(i).get_pixmap(matrix=fitz.Matrix(0.5, 0.5))
    out = Path(__file__).resolve().parent / f"verify_page_{i+1}.png"
    pix.save(str(out))
    print(out)
doc.close()
