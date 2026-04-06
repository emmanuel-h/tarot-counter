# App Icon Design

## Concept

The launcher icon shows the three **bouts** (oudlers) of French Tarot fanned like a hand of cards on a green baize background — the three cards that are worth bonus points in every game.

## Cards

| Position | Card | Symbol |
|----------|------|--------|
| Left (−22°) | Le Petit — Trump I | Bold **"1"** in top-left corner |
| Centre (0°) | L'Excuse | Four suit symbols (♠ ♥ ♦ ♣) in a 2×2 grid with four gold corner stars |
| Right (+22°) | Le Monde — Trump XXI | Bold **"21"** in top-right corner |

## Composition

All three cards share the same **36×50 dp** base shape and are **rotated around a common pivot at (54, 89)** — below the canvas centre — to create a natural hand-of-cards fan. Drawing order is left → right → centre, so the centre card is always on top.

The rotation angle of **±22°** was chosen to:
- Expose the numerals on the side cards so they are not hidden behind the centre card.
- Keep all important content within the **72×72 dp adaptive-icon safe zone** (18 dp inset from the 108×108 dp canvas).

The pivot was set at **y = 89** (raised from an earlier y = 95) so that the fan sits vertically centred within the icon frame, minimising the green border above and below the cards.

## Colour Palette

| Colour | Hex | Usage |
|--------|-----|-------|
| White | `#FFFFFF` | Card bodies |
| Antique gold | `#C4972A` | Card borders, inner frames, corner stars |
| Deep purple | `#2A0F5E` | Numerals ("1", "21") |
| Red | `#CC0000` | Hearts (♥) and diamonds (♦) |
| Black | `#000000` | Spades (♠) and clubs (♣) |
| Felt green | `#1E6B1E` | Background layer |

## L'Excuse Centre Card

The centre card displays the four French playing-card suits in a 2×2 grid:

```
♠  ♥
♦  ♣
```

Four **4-pointed gold stars** (outer radius 4 dp, inner radius 1.5 dp) are placed near the four corners of the inner frame as decorative markers identifying the card as the special Excuse.

## Files

| File | Role |
|------|------|
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | Adaptive icon foreground (vector) |
| `app/src/main/res/drawable/ic_launcher_background.xml` | Adaptive icon background (green felt) |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | Adaptive icon descriptor |
| `app/src/main/res/mipmap-*/ic_launcher*.webp` | Legacy raster icons (pre-API 26) |
| `ic_launcher.png` | 1024×1024 Play Store icon |

## Regenerating Raster Assets

The `.webp` files and the Play Store `ic_launcher.png` are rendered from `/tmp/tarot_icon.svg`
(an SVG mirror of the Android Vector Drawable) using:

```bash
# Play Store icon (1024×1024)
rsvg-convert -w 1024 -h 1024 tarot_icon.svg -o ic_launcher.png

# Legacy mipmap icons (Pillow converts PNG → webp)
python3 generate_icons.py
```

If you update `ic_launcher_foreground.xml`, keep the SVG in sync and re-run the above commands to update all raster assets.
