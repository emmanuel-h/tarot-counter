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

### Captions (issue #225)

People often see only the first two or three screenshots in search results, so the ones on Play carry a short caption saying what each screen does. `tools/store/caption_screenshots.py` (Pillow) reads the raw phone captures and writes framed versions to `store/captioned/{en,fr}/{light,dark}/`, with the same file names. The raw captures in `store/screenshots/` are not changed, so they can be regenerated and the captions redrawn at any time:

```bash
python3 tools/store/caption_screenshots.py
```

Each output is 1080 × 1920 (9:16), styled like the feature graphic:

```
┌──────────────────────────┐
│        felt green        │
│   Standings at a glance  │  Figtree Bold, ivory, wraps to 2 balanced lines
│  ═════  ♠ ♥ ♦ ♣  ═════   │  brass double hairline with the suits
│   ┌──────────────────┐   │
│   │                  │   │  the app screen, scaled to 821 × 1450,
│   │    app screen    │   │  rounded corners, brass frame, soft shadow
│   │                  │   │
│   └──────────────────┘   │
└──────────────────────────┘
```

| Screen | EN | FR |
|---|---|---|
| `1_home` | All your Tarot games | Toutes vos parties de tarot |
| `2_game` | Standings at a glance | Le classement en un coup d'œil |
| `3_round_entry` | Scores calculated for you | Le score calculé pour vous |
| `4_game_over` | Winner and score chart | Le vainqueur et l'évolution des scores |
| `5_history` | The full score sheet | Toute la feuille de marque |

The captions are set in `CAPTIONS` at the top of the script.

Which shots get captions:

- **Phone, light and dark**: captioned. The dark shots fill phone slots 6–8 on Play next to the light ones, so they get the same frame (on the same felt page) and the same caption as their light counterpart; leaving them bare would make the listing look unfinished.
- **Tablets**: left raw. They exist in French only, are shown in their own Play slots, and their wide layout already reads at a glance; the raw files are uploaded as they are.

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

## Listing text (issues #226, #227, #228)

The Play listing's title and descriptions are versioned under `store/listing/`, so a wording change is reviewed like code:

```
store/listing/
├── en-US/
│   ├── title.txt               ≤ 30 characters
│   ├── short_description.txt   ≤ 80 characters
│   └── full_description.txt    ≤ 4000 characters
└── fr-FR/
    └── (same three files)
```

Folder names are Play language codes. The trailing newline is dropped; everything else, blank lines included, is sent as is (plain text, no HTML). Limits are counted in Unicode characters, as Play counts them, so `é` or `–` count as one.

| | fr-FR | en-US |
|---|---|---|
| Title | « Compteur Tarot: Score & Points » (30) | "French Tarot Score Counter" (26) |
| Short description | « Compteur de points pour le tarot à 3, 4 ou 5 : contrats, poignées, chelem. » (74) | "Score keeper for French Tarot, 3–5 players: contracts, bonuses, history." (72) |

Writing guidelines:

- **First lines**: what the app does, for 3, 4 or 5 players, and **free, no ads, no account**.
- **Keywords in sentences**, never as a list: compteur, calcul des points, feuille de marque, règles FFT, prise / garde / garde sans / garde contre, bouts, petit au bout, poignée, chelem, appel au roi; French Tarot, score keeper, scoresheet, calculator. Keyword stuffing and other apps' names are against Play policy.
- **Claim only what the app does.** The app has no permission at all, so "no internet access, your games stay on your phone" is true; keep it true if a permission is ever added.

The French title follows the « Compteur Tarot – Score & Points » proposal of #228, but that is 31 characters, so the dash became a colon. The English title carries **French** because English "tarot" searches are full of fortune-telling apps.

The launcher label (`app_name`: "Tarot Counter" / « Tarot ») stays as it is: a short label fits under an icon, and the Play title only needs to rank. So the feature graphic wordmark, which uses `app_name`, needs no regeneration. See `docs/app-name.md`.

**"AI" badge in Play search.** It cannot be checked from the API. Our guess is that Play marks text it translated or summarised automatically (for example the English listing showing the French title before an en-US title existed), not text we wrote. Once both languages have their own title and descriptions, check the badge again in Play search.

Check the text without sending anything, then publish it:

```bash
python3 -m unittest discover tools/store                    # limits, files, dry-run calls
python3 tools/store/publish-play.py --listing --dry-run     # prints the PATCH calls and lengths
python3 tools/store/publish-play.py --listing --validate-only
python3 tools/store/publish-play.py --listing
```

`--listing` sends one `PATCH edits/{id}/listings/{language}` per folder, inside the same edit as `--bundle` and `--screenshots` when they are combined. PATCH changes only the three text fields; images stay untouched. Text over a limit, an empty file or a missing file stops the script before it signs in.

## Publishing to Google Play

`tools/store/publish-play.py` talks to the Play Developer Publishing API: it uploads the bundle and its R8 mapping, releases it on a track with the notes in each language, with `--screenshots` empties and refills the listing's screenshot slots, and with `--listing` sets the title and descriptions (see above). All of it goes into one *edit* (Play's transaction) committed once, and a failure deletes the edit, so the listing never shows half an update. `/release-store` runs it; `--dry-run` prints the calls without sending any, and `--validate-only` has Play check the edit and then throws it away.

```bash
python3 tools/store/publish-play.py --screenshots                # listing images alone
python3 tools/store/publish-play.py --listing                    # listing text alone
python3 tools/store/publish-play.py --bundle … --mapping … --track production \
    --release-name 3.1.0 --notes-dir notes/ [--screenshots]
```

`--notes-dir` holds one file per language, named by Play's code: `en-US.txt`, `fr-FR.txt` (500 characters at most each).

`--screenshots` uploads this mapping, set by `LISTINGS` at the top of the script:

| Play slot | en-US | fr-FR |
|---|---|---|
| Phone (max 8) | `captioned/en/light/1`–`5`, then `captioned/en/dark/2`–`4` | `captioned/fr/light/1`–`5`, then `captioned/fr/dark/2`–`4` |
| 7-inch | left untouched | `screenshots/tablet7/fr/light/*` |
| 10-inch | left untouched | `screenshots/tablet10/fr/light/*` |

Paths are relative to `store/`. Phone slots take the captioned shots, so run `caption_screenshots.py` after regenerating the raw captures; the script stops before touching Play if a file is missing.

It needs only `requests`, `PyJWT` and `cryptography`, which the system Python already has — no Google client library, no fastlane, no Ruby.

### One-time setup

1. In the [Google Cloud console](https://console.cloud.google.com/), pick or create a project and enable the **Google Play Android Developer API**.
2. Under *IAM & Admin → Service accounts*, create a service account (no project role needed), then *Keys → Add key → JSON*. Save the file as `~/.config/tarotcounter/play-service-account.json` and `chmod 600` it. Somewhere else works too if `PLAY_SERVICE_ACCOUNT` points at it. Never in the repository — `.gitignore` refuses `*service-account*.json` as a backstop. A service account already set up for another app of the same developer account can be reused: copy or symlink its key here.
3. In the [Play Console](https://play.google.com/console/), *Users and permissions*, invite the service account's e-mail address (or edit it if it is already there), and on TarotCounter give it *Release to production…*, *Release apps to testing tracks* and *Manage store presence*.
4. Check it: `python3 tools/store/publish-play.py --screenshots --validate-only` signs in, builds the edit, has Play validate it, and publishes nothing. A new permission can take a few minutes, occasionally longer, before the API accepts it.
