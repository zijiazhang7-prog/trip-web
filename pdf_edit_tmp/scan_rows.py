# -*- coding: utf-8 -*-
from pathlib import Path

from PIL import Image

here = Path(__file__).resolve().parent
for name in ["slide_05_orig.jpeg", "slide_06_orig.jpeg", "slide_11_orig.jpeg"]:
    im = Image.open(here / name).convert("RGB")
    w, h = im.size
    print(name, w, h)
    for y in range(180, min(900, h), 2):
        dark = 0
        for x in range(120, min(2000, w)):
            r, g, b = im.getpixel((x, y))
            if r + g + b < 380:
                dark += 1
        if dark > 80:
            print("  y", y, "dark", dark)
    print()
