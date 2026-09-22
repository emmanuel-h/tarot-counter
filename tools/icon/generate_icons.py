#!/usr/bin/env python3
"""
Regenerates every raster app icon from the adaptive-icon vector drawables
(issue #205), so the launcher, legacy and Play Store icons always match.

    python3 tools/icon/generate_icons.py

Reads
    app/src/main/res/drawable/ic_launcher_background.xml
    app/src/main/res/drawable/ic_launcher_foreground.xml
Writes
    app/src/main/res/mipmap-*/ic_launcher.webp        square legacy icons (API < 26)
    app/src/main/res/mipmap-*/ic_launcher_round.webp  round legacy icons
    ic_launcher.png                                   512 x 512 Play Store icon
    tools/icon/tarot_icon.svg                         SVG mirror of the drawables

Needs `rsvg-convert` (librsvg) and Pillow.
"""
import io
import subprocess
import xml.etree.ElementTree as ET
from pathlib import Path

from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[2]
RES = ROOT / "app/src/main/res"
ANDROID = "{http://schemas.android.com/apk/res/android}"

# Legacy launcher sizes per density bucket (48 dp icon).
DENSITIES = {"mdpi": 48, "hdpi": 72, "xhdpi": 96, "xxhdpi": 144, "xxxhdpi": 192}
# Part of the 108 x 108 adaptive canvas shown by legacy / Play icons: the centre
# 84 x 84, so the brass ring (radius 34) shows whole with a felt margin.
CROP = (12, 12, 84, 84)  # x, y, width, height in dp


def colour(value):
    """Android #AARRGGBB / #RRGGBB → (svg colour, opacity)."""
    v = value.lstrip("#")
    if len(v) == 8:
        alpha, rgb = int(v[:2], 16) / 255, v[2:]
    else:
        alpha, rgb = 1.0, v
    return "#" + rgb, alpha


def convert(node):
    """One <vector> child (path or group) → SVG markup."""
    tag = node.tag
    if tag == "group":
        # Android applies scale then rotation around the pivot.
        rot = node.get(ANDROID + "rotation", "0")
        sx = node.get(ANDROID + "scaleX", "1")
        sy = node.get(ANDROID + "scaleY", "1")
        px = node.get(ANDROID + "pivotX", "0")
        py = node.get(ANDROID + "pivotY", "0")
        inner = "".join(convert(child) for child in node)
        transform = f"translate({px} {py}) rotate({rot}) scale({sx} {sy}) translate(-{px} -{py})"
        return f'<g transform="{transform}">{inner}</g>'
    if tag == "path":
        attrs = [f'd="{node.get(ANDROID + "pathData")}"']
        fill = node.get(ANDROID + "fillColor")
        if fill:
            c, a = colour(fill)
            attrs.append(f'fill="{c}" fill-opacity="{a}"')
        else:
            attrs.append('fill="none"')
        stroke = node.get(ANDROID + "strokeColor")
        if stroke:
            c, a = colour(stroke)
            width = node.get(ANDROID + "strokeWidth", "1")
            attrs.append(f'stroke="{c}" stroke-opacity="{a}" stroke-width="{width}"')
        return "<path " + " ".join(attrs) + "/>"
    return ""


def layer(name):
    tree = ET.parse(RES / "drawable" / f"{name}.xml")
    return "".join(convert(child) for child in tree.getroot())


def svg(view_box):
    x, y, w, h = view_box
    return (
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="{x} {y} {w} {h}">'
        f'{layer("ic_launcher_background")}{layer("ic_launcher_foreground")}</svg>'
    )


def render(svg_text, size):
    png = subprocess.run(
        ["rsvg-convert", "-w", str(size), "-h", str(size)],
        input=svg_text.encode(), capture_output=True, check=True,
    ).stdout
    return Image.open(io.BytesIO(png)).convert("RGBA")


def circle(image):
    """Round legacy icon: everything outside the inscribed circle is transparent.
    The mask is drawn 4x larger and scaled down, which anti-aliases its edge."""
    big = (image.size[0] * 4, image.size[1] * 4)
    mask = Image.new("L", big, 0)
    ImageDraw.Draw(mask).ellipse((0, 0, big[0] - 1, big[1] - 1), fill=255)
    mask = mask.resize(image.size, Image.LANCZOS)
    out = Image.new("RGBA", image.size, (0, 0, 0, 0))
    out.paste(image, mask=mask)
    return out


def main():
    cropped = svg(CROP)
    (ROOT / "tools/icon/tarot_icon.svg").write_text(svg((0, 0, 108, 108)))
    for density, size in DENSITIES.items():
        square = render(cropped, size)
        square.convert("RGB").save(RES / f"mipmap-{density}" / "ic_launcher.webp", quality=95)
        circle(square).save(RES / f"mipmap-{density}" / "ic_launcher_round.webp", quality=95)
    # Google Play wants a 512 x 512 full-bleed square; it applies its own mask.
    render(cropped, 512).convert("RGB").save(ROOT / "ic_launcher.png")
    print("icons regenerated")


if __name__ == "__main__":
    main()
