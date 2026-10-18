#!/usr/bin/env python3
"""
Frames the phone store screenshots with a localized caption band (issue #225).

    python3 tools/store/caption_screenshots.py

Reads the raw captures in store/screenshots/{en,fr}/{light,dark}/ (written by the
StoreScreenshots instrumented class) and writes the captioned versions to
store/captioned/{en,fr}/{light,dark}/. The raw captures are left untouched, so
they can be regenerated at any time and this script run again.

Each output is 1080 x 1920 (9:16, Play's minimum for phone shots): a felt-green
page in the Salon style of the feature graphic, the caption in Figtree Bold, a
brass double hairline with the four suits, and the app screen scaled down below
it with rounded corners and a brass frame. Tablet shots are not captioned.
Needs Pillow.
"""
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parents[2]
FONTS = ROOT / "app/src/main/res/font"
RAW = ROOT / "store/screenshots"
OUT = ROOT / "store/captioned"
W, H = 1080, 1920

# Salon palette (app/src/main/java/.../ui/theme/Color.kt), as in feature_graphic.py.
FELT = (31, 77, 58)              # SalonFelt #1F4D3A
FELT_DARK = (22, 56, 42)         # vignette edge
IVORY = (246, 241, 231)          # SalonIvory #F6F1E7
BRASS = (176, 138, 62)           # SalonBrass #B08A3E
SUIT_FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf"

# One caption per screen, per language (the table in issue #225).
CAPTIONS = {
    "1_home": {"en": "All your Tarot games", "fr": "Toutes vos parties de tarot"},
    "2_game": {"en": "Standings at a glance", "fr": "Le classement en un coup d'œil"},
    "3_round_entry": {"en": "Scores calculated for you", "fr": "Le score calculé pour vous"},
    "4_game_over": {"en": "Winner and score chart", "fr": "Le vainqueur et l'évolution des scores"},
    "5_history": {"en": "The full score sheet", "fr": "Toute la feuille de marque"},
}

# Layout. The band holds the caption (one or two lines) and the hairline;
# the screen fills the rest, keeping its own 9:16 ratio.
BAND = 400                        # height of the caption band
SCREEN_H = H - BAND - 70          # leaves a felt margin under the screen
SCREEN_W = round(SCREEN_H * 9 / 16)
RADIUS = 44                       # rounded corners of the screen
FRAME = 6                         # brass frame width around the screen
CAPTION_SIZE = 84
MAX_TEXT_W = W - 2 * 90


def background():
    """Felt page with a soft vertical vignette (darker towards the bottom).
    The dark-theme shots get the same felt page: it is the brand colour, and the
    dark app screen stands out well against it."""
    img = Image.new("RGB", (W, H), FELT)
    draw = ImageDraw.Draw(img)
    for y in range(H):
        t = y / H
        draw.line([(0, y), (W, y)], fill=tuple(int(FELT[i] * (1 - t) + FELT_DARK[i] * t) for i in range(3)))
    return img


def wrap(draw, text, font):
    """Split the caption into lines that fit MAX_TEXT_W, balancing two lines."""
    if draw.textlength(text, font=font) <= MAX_TEXT_W:
        return [text]
    words = text.split()
    # Pick the break that makes the longer of the two lines as short as possible.
    best = min(range(1, len(words)), key=lambda i: max(
        draw.textlength(" ".join(words[:i]), font=font),
        draw.textlength(" ".join(words[i:]), font=font)))
    return [" ".join(words[:best]), " ".join(words[best:])]


def caption_band(img, text):
    draw = ImageDraw.Draw(img)
    font = ImageFont.truetype(str(FONTS / "figtree_bold.ttf"), CAPTION_SIZE)
    lines = wrap(draw, text, font)
    # Shrink if a line is still too wide (a single very long word).
    while max(draw.textlength(l, font=font) for l in lines) > MAX_TEXT_W:
        font = ImageFont.truetype(str(FONTS / "figtree_bold.ttf"), font.size - 4)
    line_h = int(font.size * 1.18)
    text_h = line_h * len(lines)
    ornament_gap, ornament_h = 44, 30
    y = (BAND - text_h - ornament_gap - ornament_h) // 2 + 10
    for line in lines:
        draw.text((W / 2, y), line, font=font, fill=IVORY, anchor="ma")
        y += line_h

    # Brass double hairline with the four suits, centred under the caption.
    brass = BRASS
    y += ornament_gap - 10
    suits = ImageFont.truetype(SUIT_FONT, 28)
    suit_text = "♠  ♥  ♦  ♣"
    sw = draw.textlength(suit_text, font=suits)
    line_w, gap = 150, 24
    left = W / 2 - sw / 2 - gap - line_w
    right = W / 2 + sw / 2 + gap
    for dy in (0, 6):
        draw.line([(left, y + dy), (left + line_w, y + dy)], fill=brass, width=2)
        draw.line([(right, y + dy), (right + line_w, y + dy)], fill=brass, width=2)
    draw.text((W / 2, y + 3), suit_text, font=suits, fill=brass, anchor="mm")


def rounded_mask(size, radius):
    """Anti-aliased rounded-rectangle mask (drawn 4x larger, then scaled down)."""
    w, h = size
    big = Image.new("L", (w * 4, h * 4), 0)
    ImageDraw.Draw(big).rounded_rectangle((0, 0, w * 4 - 1, h * 4 - 1), radius * 4, fill=255)
    return big.resize(size, Image.LANCZOS)


def framed(raw_path, text):
    img = background()
    caption_band(img, text)

    screen = Image.open(raw_path).convert("RGB").resize((SCREEN_W, SCREEN_H), Image.LANCZOS)
    x = (W - SCREEN_W) // 2
    y = BAND
    brass = BRASS

    # Soft drop shadow under the screen.
    shadow = Image.new("L", (W, H), 0)
    ImageDraw.Draw(shadow).rounded_rectangle(
        (x - FRAME, y - FRAME + 14, x + SCREEN_W + FRAME, y + SCREEN_H + FRAME + 14),
        RADIUS + FRAME, fill=110)
    shadow = shadow.filter(ImageFilter.GaussianBlur(18))
    img.paste((0, 0, 0), mask=shadow)

    # Brass frame, then the screen itself with rounded corners on top.
    fw, fh = SCREEN_W + 2 * FRAME, SCREEN_H + 2 * FRAME
    img.paste(brass, (x - FRAME, y - FRAME, x - FRAME + fw, y - FRAME + fh),
              mask=rounded_mask((fw, fh), RADIUS + FRAME))
    img.paste(screen, (x, y), mask=rounded_mask((SCREEN_W, SCREEN_H), RADIUS))
    return img


def main():
    count = 0
    for lang in ("en", "fr"):
        for theme in ("light", "dark"):
            out_dir = OUT / lang / theme
            out_dir.mkdir(parents=True, exist_ok=True)
            for screen, texts in CAPTIONS.items():
                raw = RAW / lang / theme / f"{screen}.png"
                framed(raw, texts[lang]).save(out_dir / f"{screen}.png", optimize=True)
                count += 1
    print(f"{count} captioned screenshots written to {OUT.relative_to(ROOT)}/")


if __name__ == "__main__":
    main()
