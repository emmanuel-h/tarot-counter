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

### Tablets

`store/screenshots/{tablet7,tablet10}/fr/light/` holds 3 screenshots per tablet size (`2_game`, `3_round_entry`, `4_game_over`), French and light theme only. They are 1080 × 1920 px like the phone set, but the frame's density is overridden so the screen lays out at the tablet's width: 600 dp for 7" and 800 dp for 10" (portrait). Content is capped at 600 dp, so the 10" shots show the centred column with felt margins, as on a real tablet. The sizes and screen list are set by `FormFactor` in `StoreScreenshots`.

### Generating

It is skipped unless it is asked for explicitly, so normal test runs are unaffected:

```bash
./gradlew connectedDebugAndroidTest \
    -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true \
    -Pandroid.testInstrumentationRunnerArguments.class=fr.mandarine.tarotcounter.StoreScreenshots \
    -Pandroid.testInstrumentationRunnerArguments.storeScreenshots=true
rm -rf store/screenshots
adb pull /sdcard/Android/data/fr.mandarine.tarotcounter/files/store store/screenshots
```

`storeScreenshots` chooses the sets: `phone`, `tablet`, or `true` for both. All of them run on the phone emulator.

`leaveApksInstalledAfterRun` matters: without it, Gradle uninstalls the app after the run, and the files go with it.

## Feature graphic

`store/feature_graphic_{en,fr}.png` (1024 × 500) is drawn by `tools/store/feature_graphic.py` using Pillow, the app's own fonts from `res/font`, and the icon SVG. It shows:

- felt green with a slight vignette,
- the icon in its brass ring,
- the Figtree Bold wordmark (the localized app name: "Tarot Counter" / "Tarot"),
- a brass double hairline with the four suits,
- a tagline.

The English version is also copied to `play_store_feature_graphic.png`.

```bash
python3 tools/icon/generate_icons.py      # refreshes tools/icon/tarot_icon.svg first
python3 tools/store/feature_graphic.py
```

## Icon

The Play Store icon `ic_launcher.png` (512 × 512) comes from `tools/icon/generate_icons.py`; see `docs/icon-design.md`.

## Publishing to Google Play

`tools/store/publish-play.py` talks to the Play Developer Publishing API: it uploads the bundle and its R8 mapping, releases it on a track with the notes in each language, and with `--screenshots` empties and refills the listing's screenshot slots. All of it goes into one *edit* (Play's transaction) committed once, and a failure deletes the edit, so the listing never shows half an update. `/release-store` runs it; `--dry-run` prints the calls without sending any, and `--validate-only` has Play check the edit and then throws it away.

```bash
python3 tools/store/publish-play.py --screenshots                # listing images alone
python3 tools/store/publish-play.py --bundle … --mapping … --track production \
    --release-name 3.1.0 --notes-dir notes/ [--screenshots]
```

`--notes-dir` holds one file per language, named by Play's code: `en-US.txt`, `fr-FR.txt` (500 characters at most each).

`--screenshots` uploads this mapping, set by `LISTINGS` at the top of the script:

| Play slot | en-US | fr-FR |
|---|---|---|
| Phone (max 8) | `en/light/1`–`5`, then `en/dark/2`–`4` | `fr/light/1`–`5`, then `fr/dark/2`–`4` |
| 7-inch | left untouched | `tablet7/fr/light/*` |
| 10-inch | left untouched | `tablet10/fr/light/*` |

It needs only `requests`, `PyJWT` and `cryptography`, which the system Python already has — no Google client library, no fastlane, no Ruby.

### One-time setup

1. In the [Google Cloud console](https://console.cloud.google.com/), pick or create a project and enable the **Google Play Android Developer API**.
2. Under *IAM & Admin → Service accounts*, create a service account (no project role needed), then *Keys → Add key → JSON*. Save the file as `~/.config/tarotcounter/play-service-account.json` and `chmod 600` it. Somewhere else works too if `PLAY_SERVICE_ACCOUNT` points at it. Never in the repository — `.gitignore` refuses `*service-account*.json` as a backstop. A service account already set up for another app of the same developer account can be reused: copy or symlink its key here.
3. In the [Play Console](https://play.google.com/console/), *Users and permissions*, invite the service account's e-mail address (or edit it if it is already there), and on TarotCounter give it *Release to production…*, *Release apps to testing tracks* and *Manage store presence*.
4. Check it: `python3 tools/store/publish-play.py --screenshots --validate-only` signs in, builds the edit, has Play validate it, and publishes nothing. A new permission can take a few minutes, occasionally longer, before the API accepts it.
