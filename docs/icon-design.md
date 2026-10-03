# App Icon Design

## Concept

The launcher icon shows the three **bouts** (oudlers) of French Tarot fanned like a hand of cards on the felt of a card table: the three cards that are worth bonus points in every game.

The Salon refresh (issue #205) keeps that concept and moves it to the app's palette: ivory cards with brass borders on felt green, framed by a brass double hairline ring. The ring echoes the `SuitDivider` used across the app.

```
┌──────────────────────┐
│  ╭──── brass ────╮   │   felt green background (#1F4D3A)
│ ╱ ┌──┐┌────┐┌──┐ ╲  │   double hairline ring, r = 34 / 32
│ │ │1 ││♠ ♥ ││21│ │  │   three ivory cards, brass borders
│ ╲ └──┘│♦ ♣ │└──┘ ╱  │   fan scaled to 82 % inside the ring
│  ╰────└────┘────╯   │
└──────────────────────┘
```

## Cards

| Position | Card | Symbol |
|----------|------|--------|
| Left (−22°) | Le Petit — Trump I | Bold **"1"** in the top-left corner |
| Centre (0°) | L'Excuse | The four suits (♠ ♥ ♦ ♣) in a 2×2 grid with four brass corner stars |
| Right (+22°) | Le Monde — Trump XXI | Bold **"21"** in the top-right corner |

## Composition

All three cards share the same **36×50 dp** base shape and rotate around a common pivot at **(54, 89)** to form the fan. Drawing order is left, right, centre, so the centre card stays on top.

Since #205 the whole fan sits in a group scaled to **82 %** around the canvas centre. This keeps it inside the brass ring and away from the edges of round launcher masks. The ring (radius 34, plus an inner hairline at 32) stays inside the **72 dp adaptive-icon safe zone**, so every mask (circle, squircle, rounded square) shows it whole.

## Colour palette (Salon)

The colours match `ui/theme/Color.kt`.

| Colour | Hex | Usage |
|--------|-----|-------|
| Felt green | `#1F4D3A` | Background layer; numerals "1" and "21" |
| Brass | `#B08A3E` | Ring, card borders, inner frames, corner stars |
| Paper ivory | `#FFFDF8` | Card bodies |
| Salon red | `#9B2C2C` | Hearts (♥) and diamonds (♦) |
| Ink | `#1E2420` | Spades (♠) and clubs (♣) |

## Splash screen

On launch, the `core-splashscreen` library shows the icon on the page background while the app starts: ivory (`#F6F1E7`) in light mode, and the night felt (`#101A15`) when the device is in dark mode.

- The icon is `@drawable/ic_splash`, a `layer-list` of the two launcher layers.
- The theme is `Theme.TarotCounter.Starting` in `values/themes.xml` and `values-night/themes.xml`.
- `MainActivity` calls `installSplashScreen()` before `super.onCreate()`, then switches to `Theme.TarotCounter`.

That theme's window background uses the same colour, so there is no white flash between the splash and the first Compose frame.

## Files

| File | Role |
|------|------|
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | Adaptive icon foreground (vector): the fan |
| `app/src/main/res/drawable/ic_launcher_background.xml` | Adaptive icon background (vector): felt + brass ring |
| `app/src/main/res/mipmap-anydpi-v26/ic_launcher*.xml` | Adaptive icon descriptors (the monochrome layer reuses the foreground) |
| `app/src/main/res/mipmap-*/ic_launcher*.webp` | Legacy raster icons (API < 26), square and round |
| `app/src/main/res/drawable/ic_splash.xml` | Splash screen icon (both layers) |
| `ic_launcher.png` | 512×512 Play Store icon |
| `tools/icon/generate_icons.py` | Renders all raster icons from the vector drawables |
| `tools/icon/tarot_icon.svg` | SVG mirror of the drawables (generated) |

## Regenerating raster assets

After changing either vector drawable, run:

```bash
python3 tools/icon/generate_icons.py   # needs rsvg-convert (librsvg) and Pillow
```

The script converts the drawables to SVG, handling group scale, rotation and pivot, then renders them:

- the legacy `ic_launcher.webp` icons (the centre 84 × 84 dp of the canvas, full-bleed),
- the `ic_launcher_round.webp` icons (the same, with an anti-aliased circle mask),
- the 512 × 512 Play Store `ic_launcher.png`.
