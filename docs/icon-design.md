# App Icon Design

## Concept

The launcher icon shows the three **bouts** (oudlers) of French Tarot fanned like a hand of cards on a green baize background — the three cards that are worth bonus points in every game.

## Cards

| Position | Card | Symbol |
|----------|------|--------|
| Left (−22°) | Le Petit — Trump I | Bold **"1"** in top-left corner |
| Centre (0°) | L'Excuse | **Jester head** with pointed hat and four gold corner stars |
| Right (+22°) | Le Monde — Trump XXI | Bold **"21"** in top-right corner |

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

## Jester Head (L'Excuse)

The jester head is drawn in layers (bottom to top):
1. **Parchment face circle** (centre 54, 60, radius 11 dp) with a deep-purple stroke outline so it reads against the parchment card background.
2. **Deep-purple pointed hat** — an upward triangle sitting on top of the face (base at y=49, tip at y=33).
3. **Gold hat band** — a 4 dp bar at the hat/face boundary for a crisp visual separation.
4. **Gold bell diamond** — centred at the hat tip, matching the antique-gold colour of the card borders.
5. **Eyes and smile** — two small deep-purple squares for eyes and a quadratic-Bézier smile stroke.

Four **4-pointed gold stars** (outer radius 4 dp, inner radius 1.5 dp) are placed near the four corners of the inner frame as decorative markers identifying the card as the special Excuse.

## Files

| File | Role |
|------|------|
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | Adaptive icon foreground (vector) |
| `app/src/main/res/drawable/ic_launcher_background.xml` | Adaptive icon background (green felt) |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` | Adaptive icon descriptor |
| `app/src/main/res/mipmap-*/ic_launcher*.webp` | Legacy raster icons (pre-API 26) |
