# Store assets (issue #205)

Every Google Play asset is generated from the app itself, so it can be refreshed whenever the UI changes.

## Screenshots

`store/screenshots/{en,fr}/{light,dark}/` holds 5 screenshots per language and theme, each 1080 × 1920 px (9:16, within Play's 2:1 limit):

| File | Screen |
|---|---|
| `1_home.png` | Home: game in progress, new game card |
| `2_game.png` | Game: standings with a leader, "Who took?" tiles |
| `3_round_entry.png` | Round entry: contract, bouts, points and the live result |
| `4_game_over.png` | Game over: winner card, ranking, score chart |
| `5_history.png` | Score history table |

They come from `StoreScreenshots` (`src/androidTest`), a parameterized instrumented class. It renders each screen with the same sample game (Alice, Bruno, Chloé, David, five rounds) in EN/FR × light/dark, then saves a PNG on the device. Animations are off (`LocalReducedMotion`) so no capture lands mid-count.

It is skipped unless it is asked for explicitly, so normal test runs are unaffected:

```bash
./gradlew connectedDebugAndroidTest \
    -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true \
    -Pandroid.testInstrumentationRunnerArguments.class=fr.mandarine.tarotcounter.StoreScreenshots \
    -Pandroid.testInstrumentationRunnerArguments.storeScreenshots=true
rm -rf store/screenshots
adb pull /sdcard/Android/data/fr.mandarine.tarotcounter/files/store store/screenshots
```

`leaveApksInstalledAfterRun` matters: without it, Gradle uninstalls the app after the run, and the files go with it.

## Feature graphic

`store/feature_graphic_{en,fr}.png` (1024 × 500) is drawn by `tools/store/feature_graphic.py` using Pillow, the app's own fonts from `res/font`, and the icon SVG. It shows:

- felt green with a slight vignette,
- the icon in its brass ring,
- the Cormorant wordmark (the localized app name: "Tarot Counter" / "Tarot"),
- a brass double hairline with the four suits,
- a tagline.

The English version is also copied to `play_store_feature_graphic.png`.

```bash
python3 tools/icon/generate_icons.py      # refreshes tools/icon/tarot_icon.svg first
python3 tools/store/feature_graphic.py
```

## Icon

The Play Store icon `ic_launcher.png` (512 × 512) comes from `tools/icon/generate_icons.py`; see `docs/icon-design.md`.
