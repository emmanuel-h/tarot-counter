#!/usr/bin/env python3
"""
Draws the Google Play feature graphic (1024 x 500) in the Salon style (issue #205):
felt green, the app icon in its brass ring, the Cormorant wordmark, a brass
double hairline with the four suits, and a one-line tagline.

    python3 tools/store/feature_graphic.py

Writes store/feature_graphic_en.png and store/feature_graphic_fr.png, and copies
the English one to play_store_feature_graphic.png (the file the release docs use).
Needs Pillow, rsvg-convert, and tools/icon/tarot_icon.svg (tools/icon/generate_icons.py).
"""
import io
import shutil
import subprocess
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[2]
FONTS = ROOT / "app/src/main/res/font"
W, H = 1024, 500

FELT = (31, 77, 58)          # #1F4D3A
FELT_DARK = (22, 56, 42)
IVORY = (246, 241, 231)      # #F6F1E7
BRASS = (176, 138, 62)       # #B08A3E
BRASS_ON_FELT = (226, 201, 142)  # #E2C98E

TAGLINES = {
    "en": "The score keeper for French Tarot",
    "fr": "Le compteur de points du Tarot",
}
# The launcher name is localized (res/values*/strings.xml: app_name).
TITLES = {"en": "Tarot Counter", "fr": "Tarot"}
SUIT_FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"


def icon(size):
    """The launcher icon (felt, brass ring, fan of bouts) rendered in a circle."""
    svg = (ROOT / "tools/icon/tarot_icon.svg").read_text()
    # Show the whole ring with a small margin, like the round launcher icon.
    svg = svg.replace('viewBox="0 0 108 108"', 'viewBox="14 14 80 80"')
    png = subprocess.run(["rsvg-convert", "-w", str(size), "-h", str(size)],
                         input=svg.encode(), capture_output=True, check=True).stdout
    img = Image.open(io.BytesIO(png)).convert("RGBA")
    big = Image.new("L", (size * 4, size * 4), 0)
    ImageDraw.Draw(big).ellipse((0, 0, size * 4 - 1, size * 4 - 1), fill=255)
    out = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    out.paste(img, mask=big.resize((size, size), Image.LANCZOS))
    return out


def graphic(lang):
    img = Image.new("RGB", (W, H), FELT)
    draw = ImageDraw.Draw(img)
    # Soft vignette: darker felt towards the right edge.
    for x in range(W):
        t = x / W
        c = tuple(int(FELT[i] * (1 - 0.35 * t) + FELT_DARK[i] * 0.35 * t) for i in range(3))
        draw.line([(x, 0), (x, H)], fill=c)

    # Icon on the left, in a soft shadow.
    ic = icon(300)
    shadow = Image.new("RGBA", (320, 320), (0, 0, 0, 0))
    ImageDraw.Draw(shadow).ellipse((10, 16, 310, 316), fill=(0, 0, 0, 70))
    img.paste(shadow, (62, 92), shadow)
    img.paste(ic, (72, 100), ic)

    # Wordmark, then brass hairlines with suits, then tagline.
    x0 = 430
    title = ImageFont.truetype(str(FONTS / "cormorant_garamond_bold.ttf"), 92)
    draw.text((x0, 118), TITLES[lang], font=title, fill=IVORY)

    y = 262
    suits = ImageFont.truetype(SUIT_FONT, 26)
    suit_text = "♠  ♥  ♦  ♣"
    sw = draw.textlength(suit_text, font=suits)
    line_w = 110
    for dy in (0, 5):
        draw.line([(x0, y + dy), (x0 + line_w, y + dy)], fill=BRASS, width=1)
        draw.line([(x0 + line_w + sw + 40, y + dy), (x0 + 2 * line_w + sw + 40, y + dy)], fill=BRASS, width=1)
    draw.text((x0 + line_w + 20, y - 14), suit_text, font=suits, fill=BRASS)

    tag = ImageFont.truetype(str(FONTS / "figtree_medium.ttf"), 30)
    draw.text((x0, 310), TAGLINES[lang], font=tag, fill=BRASS_ON_FELT)
    return img


def main():
    out = ROOT / "store"
    out.mkdir(exist_ok=True)
    for lang in TAGLINES:
        graphic(lang).save(out / f"feature_graphic_{lang}.png")
    shutil.copy(out / "feature_graphic_en.png", ROOT / "play_store_feature_graphic.png")
    print("feature graphics written")


if __name__ == "__main__":
    main()
