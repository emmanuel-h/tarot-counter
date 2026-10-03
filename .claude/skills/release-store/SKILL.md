---
name: release-store
description: Automate the full TarotCounter release workflow — bump semver, build signed AAB, create GitHub release, upload artifact. Use when the user wants to publish a new version to the Play Store.
argument-hint: "[major|minor|hotfix]  (default: minor)"
allowed-tools: Bash Read Edit Glob Grep Write
---

Automate the full TarotCounter release workflow.

## Input

`$ARGUMENTS` contains the release type: `major`, `minor`, or `hotfix`.
Default to `minor` if the argument is absent or unrecognised.

---

## Step 1 — Parse the release type

```
RELEASE_TYPE="${ARGUMENTS:-minor}"
if [[ "$RELEASE_TYPE" != "major" && "$RELEASE_TYPE" != "minor" && "$RELEASE_TYPE" != "hotfix" ]]; then
  RELEASE_TYPE="minor"
fi
echo "Release type: $RELEASE_TYPE"
```

---

## Step 2 — Read the current version from `app/build.gradle.kts`

Use `grep` with a Perl-compatible regex to extract the two version fields:

```bash
# Integer build number — incremented on every release.
CURRENT_CODE=$(grep -oP 'versionCode\s*=\s*\K[0-9]+' app/build.gradle.kts)

# Human-readable semver string, e.g. "1.2" or "1.2.3".
CURRENT_NAME=$(grep -oP 'versionName\s*=\s*"\K[^"]+' app/build.gradle.kts)

echo "Current: versionCode=$CURRENT_CODE  versionName=$CURRENT_NAME"
```

---

## Step 3 — Calculate the next version

Split `CURRENT_NAME` on `.` into MAJOR, MINOR, and PATCH components.
PATCH defaults to 0 if the current name has only two parts.

```bash
IFS='.' read -r VER_MAJOR VER_MINOR VER_PATCH <<< "$CURRENT_NAME"
VER_PATCH="${VER_PATCH:-0}"

case "$RELEASE_TYPE" in
  major)
    # X.0.0  →  (X+1).0.0
    VER_MAJOR=$((VER_MAJOR + 1))
    VER_MINOR=0
    VER_PATCH=0
    ;;
  minor)
    # X.Y    →  X.(Y+1)
    VER_MINOR=$((VER_MINOR + 1))
    VER_PATCH=0
    ;;
  hotfix)
    # X.Y.Z  →  X.Y.(Z+1)  [Z starts at 1 if the current name has no patch]
    VER_PATCH=$((VER_PATCH + 1))
    ;;
esac

NEW_CODE=$((CURRENT_CODE + 1))

# Always use full semver (X.Y.Z) for all release types.
NEW_NAME="${VER_MAJOR}.${VER_MINOR}.${VER_PATCH}"

echo "Next:    versionCode=$NEW_CODE  versionName=$NEW_NAME"
```

---

## Step 4 — Confirm with the user

Show the user the planned version bump before touching any file:

```
Current version : $CURRENT_NAME  (code $CURRENT_CODE)
Next version    : $NEW_NAME      (code $NEW_CODE)
Release type    : $RELEASE_TYPE
```

Ask: **"Proceed with this version bump and release? (yes/no)"**

If the user says no, stop and let them specify the correct release type.

---

## Step 5 — Patch `app/build.gradle.kts`

Use `sed` in-place to replace both version fields:

```bash
sed -i "s/versionCode\s*=\s*${CURRENT_CODE}/versionCode = ${NEW_CODE}/" app/build.gradle.kts
sed -i "s/versionName\s*=\s*\"${CURRENT_NAME}\"/versionName = \"${NEW_NAME}\"/" app/build.gradle.kts
```

Verify the replacement:

```bash
grep -E 'versionCode|versionName' app/build.gradle.kts
```

---

## Step 6 — Build the signed App Bundle

```bash
./gradlew bundleRelease
```

The output artifact will be at:
```
app/build/outputs/bundle/release/app-release.aab
```

If the build fails, show the error output to the user and stop.

---

## Step 7 — Commit and push the version bump

Stage only the build file (no secrets, no `.aab` binary), then push immediately:

```bash
git add app/build.gradle.kts
git commit -m "chore: bump version to ${NEW_NAME} (code ${NEW_CODE})"
git push
```

---

## Step 8 — Create the GitHub release and upload the artifact

```bash
TAG="v${NEW_NAME}"

# Create an annotated release on GitHub.
# --generate-notes asks GitHub to auto-generate a changelog from merged PRs/commits.
gh release create "$TAG" \
  --title "$TAG" \
  --generate-notes

# Attach the signed App Bundle to the release.
gh release upload "$TAG" \
  "app/build/outputs/bundle/release/app-release.aab" \
  --clobber

# Attach the R8 mapping file to the release.
# This file maps obfuscated class/method names back to their original names and is
# required to deobfuscate crash stack traces for this specific release version.
# Play Console also uses it automatically when you upload the AAB.
# Without archiving it here, the file would be overwritten on the next build.
gh release upload "$TAG" \
  "app/build/outputs/mapping/release/mapping.txt" \
  --clobber
```

---

## Step 9 — Generate Play Store release notes and copy to clipboard

Get all commits since the last release tag (feat/fix only, excluding chores/refactors/docs/tests):

```bash
PREV_TAG=$(gh release list --limit 2 --json tagName --jq '.[1].tagName')
git log "${PREV_TAG}..HEAD" --oneline --no-merges \
  | grep -E '^[a-f0-9]+ (feat|fix)' \
  | sed 's/^[a-f0-9]* //' \
  | sed 's/^(feat|fix)(\([^)]*\))?!?:\s*//'
```

**Rewrite those raw commit subjects into user-facing release notes.** Do NOT copy commit messages verbatim — they contain technical jargon (issue numbers, scope tags, internal names) that means nothing to end users.

Rules for writing the bullets:
- Describe **what the user can now do or what changed from their perspective** — never mention branch names, issue numbers, PR numbers, commit hashes, or code/class names.
- Use **plain language** (no conventional-commit prefixes like `feat:` or `fix:`).
- Use **imperative style** ("Add …", "Fix …", "Improve …").
- Merge commits that describe the same end-user change into a single bullet.
- Omit any change that has zero visible impact for end users (e.g. internal refactors, CI changes, test improvements, dependency upgrades, build system changes).
- If **all** changes in the release are purely technical (nothing is visible to the user), do not list bullets. Instead use a single generic line:
  - French: `- Améliorations internes et corrections mineures.`
  - English: `- Internal improvements and minor fixes.`

Write the notes:
- In **French** (`<fr-FR>` tags) — bullet points, user-facing language
- In **English** (`<en-US>` tags) — same bullets translated

### Hard length limit — 500 characters per language

Google Play rejects release notes longer than **500 characters per language** (the text
between the tags, newlines and `- ` prefixes included). French usually runs ~20 % longer
than English, so **write the French first** and size everything against it.

- **Aim for ≤ 450 characters** per language to leave a safety margin.
- Keep bullets short (one line, ~60 characters max) and keep at most ~8 bullets.
- For a large release, list only the most visible changes and merge related ones
  (e.g. "score history and game-over screen redesigned") — do not try to be exhaustive.
- Never truncate mid-sentence to fit; rewrite instead.

Format:
```
<fr-FR>
- …
</fr-FR>

<en-US>
- …
</en-US>
```

Put each language in its own variable, then **run the length check before copying anything**.
The check counts Unicode characters (not bytes — `é`, `«`, `…` count as one) and fails
if either language exceeds 500:

```bash
FR="- …
- …"

EN="- …
- …"

# Count Unicode code points with Python so accented characters are measured the way Play does.
check_len() {
  python3 -c 'import sys; print(len(sys.argv[1]))' "$1"
}
FR_LEN=$(check_len "$FR")
EN_LEN=$(check_len "$EN")
echo "fr-FR: ${FR_LEN}/500   en-US: ${EN_LEN}/500"

if (( FR_LEN > 500 || EN_LEN > 500 )); then
  echo "ERROR: release notes exceed the 500-character Play Store limit — shorten and re-run." >&2
  exit 1
fi
(( FR_LEN > 450 || EN_LEN > 450 )) && echo "WARNING: above the 450-character target; consider trimming."
```

**If the check fails, rewrite the bullets and run it again. Do not copy to the clipboard
or show the notes as final until both languages pass.**

Once both pass, copy the full block to the clipboard:

```bash
NOTES="<fr-FR>
${FR}
</fr-FR>

<en-US>
${EN}
</en-US>"

WAYLAND_DISPLAY=wayland-0 wl-copy "$NOTES"
```

Display the notes to the user together with both character counts (e.g. `fr-FR: 443/500`)
and confirm they are in the clipboard.

---

## Step 10 — Display the artifact download URL

```bash
gh release view "$TAG" --json assets \
  --jq '.assets[] | select(.name | endswith(".aab")) | .url'
```

> Note: `gh` exposes the public download link as `url` (e.g.
> `https://github.com/…/releases/download/v1.2.3/app-release.aab`). The older
> `browserDownloadUrl` field no longer exists and returns an empty string.

Display the URL clearly to the user.

Also remind the user:
- Upload the `.aab` to **Google Play Console → Production (or Internal testing) → Create new release**.
- The same signing keystore must be used for every future release.

---

## Summary output

End with a short summary block:

```
✓ Version bumped  : $CURRENT_NAME (code $CURRENT_CODE) → $NEW_NAME (code $NEW_CODE)
✓ AAB built       : app/build/outputs/bundle/release/app-release.aab
✓ GitHub release  : https://github.com/emmanuel-h/tarot-counter/releases/tag/$TAG
✓ Download URL    : <url from step 10>
✓ Release notes   : copied to clipboard
```
