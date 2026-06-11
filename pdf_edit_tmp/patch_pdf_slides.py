# -*- coding: utf-8 -*-
"""
Patch PDF pages 5, 6, 11 (1-based): Vue -> React stack; redo progress bars.
Outputs new PDF next to source.
"""
from __future__ import annotations

import glob
import shutil
from pathlib import Path

import fitz
from PIL import Image, ImageDraw, ImageFont

HERE = Path(__file__).resolve().parent
MID = Path(r"c:\Users\Lenovo\Desktop\trip\中期")
ORIGINAL_NAME = "智游行 AI旅游推荐系统中期汇报.pdf"
PDF_SRC = MID / ORIGINAL_NAME
if not PDF_SRC.exists():
    cands = [
        p
        for p in MID.glob("*.pdf")
        if "已修订" not in p.name and "REVISED" not in p.name.upper()
    ]
    if not cands:
        raise FileNotFoundError("未找到原始 PDF")
    PDF_SRC = cands[0]
OUT_PDF = MID / "智游行 AI旅游推荐系统中期汇报-已修订.pdf"
OUT_PDF_ASCII = MID / "Zhiyouxing-Midterm-report-REVISED.pdf"

# --- colors (match slide style) ---
NAVY = (18, 42, 74)
NAVY_FILL = (28, 55, 95)
TRACK = (236, 239, 243)
AI_GREY = (160, 178, 198)
WHITE = (255, 255, 255)
ORANGE_ARROW = (230, 126, 60)


def load_font(size: int) -> ImageFont.FreeTypeFont:
    for fp in (
        r"C:\Windows\Fonts\msyhbd.ttc",
        r"C:\Windows\Fonts\msyh.ttc",
        r"C:\Windows\Fonts\simhei.ttf",
    ):
        p = Path(fp)
        if p.exists():
            return ImageFont.truetype(str(p), size=size)
    return ImageFont.load_default()


def patch_slide5(im: Image.Image) -> Image.Image:
    im = im.convert("RGB")
    draw = ImageDraw.Draw(im)
    W, H = im.size
    # 覆盖「前端」整条 bullet（含箭头右侧文字区）
    x0, y0, x1, y1 = 72, 452, 1320, 538
    draw.rounded_rectangle((x0, y0, x1, y1), radius=8, fill=WHITE, outline=None)
    # 重画橙色小三角/箭头简化为小竖条左侧留白与原图协调 — 仅文字
    font = load_font(26)
    font_sm = load_font(22)
    lines = [
        "前端：React 19 + Vite + TypeScript + Tailwind CSS 4，",
        "注重动态交互与动效体验。",
    ]
    tx, ty = 188, 462
    for i, ln in enumerate(lines):
        f = font if i == 0 else font_sm
        draw.text((tx, ty + i * 34), ln, fill=NAVY, font=f)
    # 左侧小箭头装饰（简化为实心三角）
    cx, cy = 130, 488
    draw.polygon([(cx, cy - 10), (cx + 22, cy), (cx, cy + 10)], fill=ORANGE_ARROW)
    return im


def patch_slide6(im: Image.Image) -> Image.Image:
    im = im.convert("RGB")
    draw = ImageDraw.Draw(im)
    # 第一张卡片区域（估算与 2560 四栏布局对齐）
    x0, y0, x1, y1 = 48, 218, 618, 798
    draw.rounded_rectangle((x0, y0, x1, y1), radius=28, fill=WHITE, outline=(220, 228, 235), width=2)
    # 顶部色条（与原卡片风格接近）
    draw.rounded_rectangle((x0 + 2, y0 + 2, x1 - 2, y0 + 8), radius=6, fill=(230, 240, 235))
    title_f = load_font(34)
    sub_f = load_font(22)
    body_f = load_font(20)
    cx = (x0 + x1) // 2
    draw.text((cx - 118, y0 + 110), "React 19", fill=NAVY, font=title_f)
    draw.text((cx - 168, y0 + 168), "Vite · TypeScript", fill=NAVY, font=sub_f)
    draw.text((cx - 158, y0 + 204), "Tailwind CSS 4", fill=NAVY, font=sub_f)
    draw.text((cx - 140, y0 + 280), "组件化现代前端栈", fill=(90, 110, 125), font=body_f)
    draw.text((cx - 125, y0 + 318), "动效与响应式布局", fill=(90, 110, 125), font=body_f)
    return im


def draw_progress_bar(
    draw: ImageDraw.ImageDraw,
    y: int,
    label: str,
    pct: int,
    font_label: ImageFont.ImageFont,
    font_pct: ImageFont.ImageFont,
    x_label: int = 96,
    x0: int = 420,
    x1: int = 2380,
    h: int = 42,
    ai_style: bool = False,
) -> None:
    fill_col = AI_GREY if ai_style else NAVY_FILL
    draw.rounded_rectangle((x0, y, x1, y + h), radius=h // 2, fill=TRACK, outline=None)
    w = x1 - x0
    fw = int(w * (pct / 100.0))
    if fw < h:
        fw = h if pct > 0 else 0
    if fw > 0:
        draw.rounded_rectangle((x0, y, x0 + fw, y + h), radius=h // 2, fill=fill_col, outline=None)
    draw.text((x_label, y + 6), label, fill=NAVY, font=font_label)
    pct_text = f"{pct}%"
    tw, th = draw.textbbox((0, 0), pct_text, font=font_pct)[2:]
    px = x0 + fw - tw - 18 if fw > tw + 40 else x0 + fw + 12
    py = y + (h - th) // 2 - 2
    draw.text((px, py), pct_text, fill=WHITE if (fw > tw + 40 and not ai_style) else NAVY, font=font_pct)


def patch_slide11(im: Image.Image) -> Image.Image:
    im = im.convert("RGB")
    draw = ImageDraw.Draw(im)
    # 整块清除标题区，避免与原「~60%」标题叠字
    draw.rectangle((64, 150, 2488, 248), fill=WHITE)
    font_title = load_font(38)
    draw.text((96, 178), "当前开发进展汇总（模块完成度）", fill=NAVY, font=font_title)
    # 覆盖原进度条区域
    draw.rounded_rectangle((60, 500, 2500, 1120), radius=12, fill=WHITE)
    fl = load_font(26)
    fp = load_font(22)
    rows = [
        ("Auth / 用户系统", 85, False),
        ("旅游推荐 / 搜索逻辑", 93, False),
        ("美食推荐", 91, False),
        ("日记 / 图文发布", 82, False),
        ("Route / 路线规划", 60, False),
        ("AI 创新增强", 18, True),
    ]
    y0 = 540
    gap = 86
    for i, (lab, pct, ai) in enumerate(rows):
        draw_progress_bar(draw, y0 + i * gap, lab, pct, fl, fp, ai_style=ai)
    font_note = load_font(20)
    note = "说明：旅游推荐、美食推荐前端与联调完成度较高；日记模块可用；路线规划与其他增强项略少，后续迭代补齐。"
    draw.text((96, 1060), note, fill=(95, 115, 130), font=font_note)
    return im


def main() -> None:
    p5 = Image.open(HERE / "slide_05_orig.jpeg")
    p6 = Image.open(HERE / "slide_06_orig.jpeg")
    p11 = Image.open(HERE / "slide_11_orig.jpeg")
    p5e = patch_slide5(p5)
    p6e = patch_slide6(p6)
    p11e = patch_slide11(p11)
    tmp5 = HERE / "_patched_05.jpg"
    tmp6 = HERE / "_patched_06.jpg"
    tmp11 = HERE / "_patched_11.jpg"
    p5e.save(tmp5, quality=95)
    p6e.save(tmp6, quality=95)
    p11e.save(tmp11, quality=95)

    doc = fitz.open(str(PDF_SRC))
    for idx, path in ((4, tmp5), (5, tmp6), (10, tmp11)):
        page = doc.load_page(idx)
        page.insert_image(page.rect, filename=str(path), keep_proportion=False, overlay=True)
    doc.save(str(OUT_PDF))
    doc.close()
    shutil.copy2(OUT_PDF, OUT_PDF_ASCII)
    print("Wrote", OUT_PDF)
    print("Wrote", OUT_PDF_ASCII)


if __name__ == "__main__":
    main()
