# App Icon Design

## Concept

The launcher icon shows the three **bouts** (oudlers) of French Tarot fanned like a hand of cards on a green baize background — the three cards that are worth bonus points in every game.

## Cards

| Position | Card | Symbol |
|----------|------|--------|
| Left (−22°) | Le Petit — Trump I | Bold **"1"** numeral |
| Centre (0°) | L'Excuse | **Jester hat** with three gold bells |
| Right (+22°) | Le Monde — Trump XXI | Bold **"21"** numeral |

## Composition

All three cards share the same 32×50 dp base shape and are **rotated around a common pivot at (54, 95)** — below the canvas — to create a natural hand-of-cards fan. Drawing order is left → right → centre, so the centre card is always on top.

The rotation angle of **±22°** was chosen to:
- Expose the numerals on the side cards so they are not hidden behind the centre card.
- Keep all important content within the **72×72 dp adaptive-icon safe zone** (18 dp inset from the 108×108 dp canvas).

## Colour Palette

| Colour | Hex | Usage |
|--------|-----|-------|
| Warm parchment | `#F2E0A0` | Card bodies |
| Antique gold | `#C4972A` | Card borders, inner frames, jester bells |
| Deep purple | `#2A0F5E` | Numerals, jester hat body |
| Felt green | `#1E6B1E` | Background layer |

## Jester Hat (L'Excuse)

The jester hat is drawn as:
- A **W-shaped polygon** forming three upward prongs (the classic fool's cap silhouette).
- A **horizontal brim bar** anchoring the hat to the lower half of the card.
- Three **gold diamond shapes** at each prong tip — representing the jingling bells that identify a jester's cap.

The gold bells use `#C4972A` (the same antique gold as the card frames) to contrast against the deep-purple hat body, making them immediately recognisable at all icon sizes.

## Files

| File | Role |
|------|------|
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | Adaptive icon foreground (vector) |
| `app/src/main/res/drawable/ic_launcher_background.xml` | Adaptive icon background (green felt) |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | Adaptive icon descriptor |
| `app/src/main/res/mipmap-*/ic_launcher*.webp` | Legacy raster icons (pre-API 26) |
