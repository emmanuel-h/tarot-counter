---
name: release-store
description: Automate the full TarotCounter release workflow — bump semver, optionally regenerate the store screenshots, build the signed AAB, create the GitHub release, and publish the bundle, release notes and screenshots to Google Play. Use when the user wants to publish a new version to the Play Store.
argument-hint: "[major|minor|hotfix]  (default: minor)"
allowed-tools: Bash Read Edit Glob Grep Write AskUserQuestion
---

Automate the full TarotCounter release workflow, from version bump to Google Play.

## Input

`$ARGUMENTS` contains the release type: `major`, `minor`, or `hotfix`.
Default to `minor` if the argument is absent or unrecognised.

---

## Step 1 — Parse the release type

```bash
RELEASE_TYPE="${ARGUMENTS:-minor}"
if [[ "$RELEASE_TYPE" != "major" && "$RELEASE_TYPE" != "minor" && "$RELEASE_TYPE" != "hotfix" ]]; then
  RELEASE_TYPE="minor"
fi
echo "Release type: $RELEASE_TYPE"
```

---

## Step 2 — Read the current version from `app/build.gradle.kts`

```bash
CURRENT_CODE=$(grep -oP 'versionCode\s*=\s*\K[0-9]+' app/build.gradle.kts)
CURRENT_NAME=$(grep -oP 'versionName\s*=\s*"\K[^"]+' app/build.gradle.kts)
echo "Current: versionCode=$CURRENT_CODE  versionName=$CURRENT_NAME"
```

---

## Step 3 — Calculate the next version

```bash
IFS='.' read -r VER_MAJOR VER_MINOR VER_PATCH <<< "$CURRENT_NAME"
VER_PATCH="${VER_PATCH:-0}"

case "$RELEASE_TYPE" in
  major)  VER_MAJOR=$((VER_MAJOR + 1)); VER_MINOR=0; VER_PATCH=0 ;;
  minor)  VER_MINOR=$((VER_MINOR + 1)); VER_PATCH=0 ;;
  hotfix) VER_PATCH=$((VER_PATCH + 1)) ;;
esac

NEW_CODE=$((CURRENT_CODE + 1))
NEW_NAME="${VER_MAJOR}.${VER_MINOR}.${VER_PATCH}"
echo "Next:    versionCode=$NEW_CODE  versionName=$NEW_NAME"
```

---

## Step 4 — Preflight: the Play service account key

```bash
PLAY_KEY="${PLAY_SERVICE_ACCOUNT:-$HOME/.config/tarotcounter/play-service-account.json}"
test -f "$PLAY_KEY" && echo "Play key: $PLAY_KEY" || echo "MISSING Play key: $PLAY_KEY"
```

If the key is missing, **stop before changing anything**. Tell the user the one-time
setup in `docs/store-assets.md` ("Publishing to Google Play") has not been done, and
point them at it. Never look for the key anywhere else, and never print its contents.

---

## Step 5 — Ask the user three questions

Ask all three in **one** `AskUserQuestion` call. Every option carries a `preview` that
shows what that choice produces (the user's global instructions require it on every
option).

1. **Version** — "Proceed with $CURRENT_NAME (code $CURRENT_CODE) → $NEW_NAME (code
   $NEW_CODE)?" Options: the computed bump (Recommended), the two other release types
   with their resulting numbers, and "Don't release". Preview: the `build.gradle.kts`
   diff and the tag.
2. **Track** — "Which Play track does $NEW_NAME go to?" Options: `production`
   (Recommended — this is what the store users get, after Google's review) and
   `internal` (internal testers only; promote it in the console later). Preview: who
   receives the build.
3. **Screenshots** — "Update the store screenshots?" Options: "Yes — regenerate and
   upload" and "No — keep the current listing images". Preview: for yes, the 26 files
   under `store/screenshots/` that get regenerated (`{en,fr}/{light,dark}/` × 5,
   `{tablet7,tablet10}/fr/light/` × 3) and the Play slots they replace — phone in
   en-US and fr-FR (8 each: light 1–5 + dark 2–4), 7-inch and 10-inch in fr-FR only;
   for no, "listing images unchanged".

If the user picks "Don't release", stop. If they pick a different release type,
recompute Step 3 with it. Keep the answers as `TRACK` and `SCREENSHOTS` (`yes`/`no`).

---

## Step 6 — Patch `app/build.gradle.kts`

```bash
sed -i "s/versionCode\s*=\s*${CURRENT_CODE}/versionCode = ${NEW_CODE}/" app/build.gradle.kts
sed -i "s/versionName\s*=\s*\"${CURRENT_NAME}\"/versionName = \"${NEW_NAME}\"/" app/build.gradle.kts
grep -E 'versionCode|versionName' app/build.gradle.kts
```

---

## Step 7 — Regenerate the screenshots (only if the user said yes)

Skip this whole step if the user chose not to update the screenshots.

**7a. An emulator.** Use a running one if `adb devices` lists an `emulator-*`;
otherwise boot the phone AVD. All sets, tablets included, are shot on the phone
emulator — `StoreScreenshots` overrides the density to get the tablet widths:

```bash
export ANDROID_HOME=~/Android/Sdk
ADB=$ANDROID_HOME/platform-tools/adb
DEV=$($ADB devices | awk '/^emulator-/{print $1; exit}')
if [ -z "$DEV" ]; then
  $ANDROID_HOME/emulator/emulator -avd Medium_Phone_API_36.1 -no-snapshot-save -no-boot-anim >/dev/null 2>&1 &
  $ADB wait-for-device
  until [ "$($ADB shell getprop sys.boot_completed | tr -d '\r')" = "1" ]; do sleep 2; done
  DEV=$($ADB devices | awk '/^emulator-/{print $1; exit}')
fi
echo "Device: $DEV"
```

(Start the emulator with `run_in_background` rather than `&` if the shell will not
leave it running.)

**7b. Run `StoreScreenshots` and pull the result** (see `docs/store-assets.md`).
`leaveApksInstalledAfterRun` keeps the app — and so the PNGs — on the device:

```bash
ANDROID_SERIAL=$DEV ./gradlew connectedDebugAndroidTest \
    -Pandroid.injected.androidTest.leaveApksInstalledAfterRun=true \
    -Pandroid.testInstrumentationRunnerArguments.class=fr.mandarine.tarotcounter.StoreScreenshots \
    -Pandroid.testInstrumentationRunnerArguments.storeScreenshots=true
rm -rf store/screenshots
$ADB -s "$DEV" pull /sdcard/Android/data/fr.mandarine.tarotcounter/files/store store/screenshots
find store/screenshots -name '*.png' | wc -l     # expect 26
```

If the run fails, read the test report under
`app/build/reports/androidTests/connected/`, fix the cause, and run 7b again. If the
emulator died mid-run (`adb devices` is empty), restart it as in 7a.

**7c. Look at every screenshot before going further.** The test cannot tell a good
capture from a bad one. Read all of them (or a contact sheet per set) and check each
for:
- the intended screen, fully laid out — no half-finished animation, no empty state
  where the sample game should be
- no crash dialog, no system dialog, no notification glyph in the status bar
- the right language and theme: English under `en/`, French under `fr/` and the tablet
  sets; dark only under `dark/`
- on the tablet sets, the centred column with felt margins at 10"

If any shot fails, fix the cause and run 7b again. Never publish a set you have not seen.

**7d. Commit the screenshots:**

```bash
git add store/screenshots
git commit -m "chore(store): regenerate screenshots for ${NEW_NAME}"
```

---

## Step 8 — Build the signed App Bundle

```bash
./gradlew bundleRelease
```

Outputs: `app/build/outputs/bundle/release/app-release.aab` and
`app/build/outputs/mapping/release/mapping.txt`. If the build fails, show the error and
stop.

---

## Step 9 — Commit and push the version bump

```bash
git add app/build.gradle.kts
git commit -m "chore: bump version to ${NEW_NAME} (code ${NEW_CODE})"
git push
```

---

## Step 10 — Create the GitHub release

The GitHub release is the archive of every bundle and mapping file shipped. The
mapping is what deobfuscates crash stack traces for this exact version.

```bash
TAG="v${NEW_NAME}"
gh release create "$TAG" --title "$TAG" --generate-notes
gh release upload "$TAG" app/build/outputs/bundle/release/app-release.aab --clobber
gh release upload "$TAG" app/build/outputs/mapping/release/mapping.txt --clobber
```

---

## Step 11 — Write the Play release notes

List what changed since the previous release. This repository uses conventional
commits; keep the `feat`/`fix` ones (any scope, e.g. `feat(205):`):

```bash
PREV_TAG=$(gh release list --limit 2 --json tagName --jq '.[1].tagName')
git log "${PREV_TAG}..HEAD" --oneline --no-merges | grep -E '^[a-f0-9]+ (feat|fix)'
```

**Rewrite them into user-facing release notes.** Do NOT copy commit messages verbatim.

Rules:
- Describe **what the user can now do or what changed from their perspective** — never
  mention issue numbers, PR numbers, commit hashes, or code/class names.
- Use **plain language**, **imperative style** ("Add …", "Fix …", "Improve …").
- Merge commits that describe the same end-user change into a single bullet.
- Omit changes with zero visible impact (refactors, CI, tests, build, store assets).
- If all changes are purely technical, use a single generic line instead:
  - French: `- Améliorations internes et corrections mineures.`
  - English: `- Internal improvements and minor fixes.`

### The 500-character limit

**Play rejects any language whose notes exceed 500 characters.** The limit is per
language. French runs 15–25% longer than the same English, so **write French first**
and let its length decide how much detail every bullet carries. Aim for **≤ 460
characters** per language — roughly 8 one-line bullets.

When over budget, cut in this order:
1. **Qualifiers before bullets** — the change survives; the elaboration goes.
2. **Bullets describing an absence** — a removed control is nothing to go looking for.
3. **Whole bullets**, least visible first. Never comma-splice two unrelated changes.

### Write and measure

The file names are the Play language codes — `publish-play.py` reads them as such.

```bash
NOTES="<your scratchpad directory>/notes"
mkdir -p "$NOTES"
cat > "$NOTES/fr-FR.txt" <<'EOF'
- …
EOF
cat > "$NOTES/en-US.txt" <<'EOF'
- …
EOF
wc -m "$NOTES/fr-FR.txt" "$NOTES/en-US.txt"
```

If either exceeds 500, trim by the order above and measure again. (`publish-play.py`
refuses over-long notes too, but catch it here.)

---

## Step 12 — Publish to Google Play

One edit, one commit: the bundle, its mapping, the release on the chosen track with both
languages' notes, and — only if the user said yes in Step 5 — the screenshots.

```bash
python3 tools/store/publish-play.py \
  --bundle app/build/outputs/bundle/release/app-release.aab \
  --mapping app/build/outputs/mapping/release/mapping.txt \
  --track "$TRACK" \
  --release-name "$NEW_NAME" \
  --notes-dir "$NOTES" \
  $([ "$SCREENSHOTS" = yes ] && echo --screenshots)
```

On failure the script deletes the edit, so nothing half-done reaches the listing. Read
the error it prints:
- **"Changes cannot be sent for review automatically"** — the app is set to need a
  manual send. Re-run with `--changes-not-sent-for-review`, then tell the user to press
  *Send for review* under *Publishing overview* in the Play Console.
- **"version code … already been used"** — this version code is already on Play. Do not
  bump again on your own; report it to the user.
- **401 / 403 on sign-in or on the first call** — the service account is missing its
  Play Console permissions on TarotCounter; point the user at the setup in
  `docs/store-assets.md`.

Any other failure: show the error and stop. Never retry blindly; the GitHub release is
already made and the version is already pushed, so a re-run of this step alone is all
that is needed once the cause is fixed.

---

## Summary output

```
✓ Version bumped  : $CURRENT_NAME (code $CURRENT_CODE) → $NEW_NAME (code $NEW_CODE)
✓ Screenshots     : regenerated and uploaded (26 files) | unchanged
✓ AAB built       : app/build/outputs/bundle/release/app-release.aab
✓ GitHub release  : https://github.com/emmanuel-h/tarot-counter/releases/tag/$TAG
✓ Google Play     : $NEW_NAME on the $TRACK track, sent for review
✓ Release notes   : fr-FR <n> chars, en-US <n> chars
```

Then show both release-note blocks, and say what was cut to fit if anything was.
Google still reviews every release and listing change; it goes live when review passes.
