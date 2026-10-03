#!/usr/bin/env python3
"""Publish to Google Play through the Play Developer Publishing API.

    python3 tools/store/publish-play.py [--bundle AAB --mapping TXT --track TRACK
                                         --release-name NAME --notes-dir DIR]
                                        [--screenshots]
                                        [--validate-only] [--changes-not-sent-for-review]
                                        [--dry-run]

Everything goes into one edit, and the edit is committed once at the end, so the
listing never shows half an update. With --bundle, the bundle and its R8 mapping
are uploaded and released on TRACK with the notes in DIR/<language>.txt (file
names are Play language codes: en-US.txt, fr-FR.txt). With --screenshots, the
screenshot slots listed in LISTINGS are emptied and refilled from store/screenshots/.

The service account key is read from $PLAY_SERVICE_ACCOUNT, or
~/.config/tarotcounter/play-service-account.json — see docs/store-assets.md.
"""
import argparse, glob, json, os, sys, time

# PyJWT signs the service-account assertion; requests does the HTTP calls.
# Both ship with the system Python, so no Google client library is needed.
import jwt, requests

PKG = "fr.mandarine.tarotcounter"
SCOPE = "https://www.googleapis.com/auth/androidpublisher"
API = f"https://androidpublisher.googleapis.com/androidpublisher/v3/applications/{PKG}"
UPLOAD = f"https://androidpublisher.googleapis.com/upload/androidpublisher/v3/applications/{PKG}"
REPO = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
SCREENSHOTS = os.path.join(REPO, "store", "screenshots")
DEFAULT_KEY = os.path.expanduser("~/.config/tarotcounter/play-service-account.json")

# Which files fill which Play slot, per listing language. Paths are relative to
# store/screenshots/ and uploaded in the order given. Play shows phone shots in
# this order: every screen in light, then the three most striking in dark (Play
# takes at most 8). Tablet shots exist in French only; a slot that is not listed
# here is left as it is on Play rather than emptied.
PHONE = ["light/1_home", "light/2_game", "light/3_round_entry", "light/4_game_over",
         "light/5_history", "dark/2_game", "dark/3_round_entry", "dark/4_game_over"]
LISTINGS = {
    "en-US": {"phoneScreenshots": [f"en/{s}.png" for s in PHONE]},
    "fr-FR": {"phoneScreenshots": [f"fr/{s}.png" for s in PHONE],
              "sevenInchScreenshots": sorted(glob.glob("tablet7/fr/light/*.png", root_dir=SCREENSHOTS)),
              "tenInchScreenshots": sorted(glob.glob("tablet10/fr/light/*.png", root_dir=SCREENSHOTS))},
}


class PlayError(Exception):
    pass


def access_token(key_path):
    """Trade the service-account key for a one-hour OAuth access token."""
    with open(key_path) as f:
        key = json.load(f)
    now = int(time.time())
    assertion = jwt.encode(
        {"iss": key["client_email"], "scope": SCOPE, "aud": key["token_uri"],
         "iat": now, "exp": now + 3600},
        key["private_key"], algorithm="RS256")
    r = requests.post(key["token_uri"], timeout=60, data={
        "grant_type": "urn:ietf:params:oauth:grant-type:jwt-bearer",
        "assertion": assertion})
    if not r.ok:
        raise PlayError(f"sign-in refused: {r.status_code} {r.text}")
    return r.json()["access_token"]


class Play:
    def __init__(self, token, dry_run):
        self.dry_run = dry_run
        self.session = requests.Session()
        self.session.headers["Authorization"] = f"Bearer {token}"

    def call(self, method, url, **kw):
        print(f"  {method} {url.split(PKG, 1)[1] or '/'}")
        if self.dry_run:
            return {"id": "dry-run", "versionCode": 0}
        r = self.session.request(method, url, timeout=600, **kw)
        if not r.ok:
            raise PlayError(f"{method} {url}: {r.status_code} {r.text}")
        return r.json() if r.content else {}

    def upload(self, url, path, content_type):
        print(f"    ← {os.path.relpath(path)}")
        if self.dry_run:
            return self.call("POST", url)
        with open(path, "rb") as f:
            return self.call("POST", url, params={"uploadType": "media"}, data=f,
                             headers={"Content-Type": content_type})


def release_notes(notes_dir):
    """Read DIR/<language>.txt; Play rejects notes over 500 characters per language."""
    notes = []
    for path in sorted(glob.glob(f"{notes_dir}/*.txt")):
        text = open(path, encoding="utf-8").read().strip()
        language = os.path.basename(path)[:-len(".txt")]
        if len(text) > 500:
            raise PlayError(f"{language} release notes are {len(text)} characters; Play takes 500")
        notes.append({"language": language, "text": text})
    if not notes:
        raise PlayError(f"no <language>.txt release notes in {notes_dir}")
    return notes


def publish_bundle(play, edit, args):
    notes = release_notes(args.notes_dir)
    bundle = play.upload(f"{UPLOAD}/edits/{edit}/bundles", args.bundle, "application/octet-stream")
    code = bundle["versionCode"]
    # The mapping lets Play Console deobfuscate this version's crash stack traces.
    play.upload(f"{UPLOAD}/edits/{edit}/apks/{code}/deobfuscationFiles/proguard",
                args.mapping, "application/octet-stream")
    play.call("PUT", f"{API}/edits/{edit}/tracks/{args.track}", json={
        "track": args.track,
        "releases": [{"name": args.release_name, "versionCodes": [str(code)],
                      "status": "completed", "releaseNotes": notes}]})


def publish_screenshots(play, edit):
    # Check every slot before touching any, so a missing file fails fast.
    for language, slots in LISTINGS.items():
        for slot, shots in slots.items():
            if not 2 <= len(shots) <= 8:
                raise PlayError(f"{language} {slot}: {len(shots)} screenshots; Play takes 2 to 8")
            for shot in shots:
                if not os.path.isfile(os.path.join(SCREENSHOTS, shot)):
                    raise PlayError(f"{language} {slot}: missing store/screenshots/{shot}")
    for language, slots in LISTINGS.items():
        for slot, shots in slots.items():
            play.call("DELETE", f"{API}/edits/{edit}/listings/{language}/{slot}")
            for shot in shots:
                play.upload(f"{UPLOAD}/edits/{edit}/listings/{language}/{slot}",
                            os.path.join(SCREENSHOTS, shot), "image/png")


def main():
    p = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    p.add_argument("--bundle")
    p.add_argument("--mapping")
    p.add_argument("--track", choices=["internal", "alpha", "beta", "production"])
    p.add_argument("--release-name")
    p.add_argument("--notes-dir")
    p.add_argument("--screenshots", action="store_true")
    p.add_argument("--validate-only", action="store_true")
    p.add_argument("--changes-not-sent-for-review", action="store_true")
    p.add_argument("--dry-run", action="store_true")
    args = p.parse_args()

    release = [args.bundle, args.mapping, args.track, args.release_name, args.notes_dir]
    if any(release) and not all(release):
        p.error("--bundle needs --mapping, --track, --release-name and --notes-dir")
    if not args.bundle and not args.screenshots:
        p.error("nothing to publish: pass --bundle, --screenshots, or both")

    key = os.environ.get("PLAY_SERVICE_ACCOUNT", DEFAULT_KEY)
    play = Play("dry-run" if args.dry_run else access_token(key), args.dry_run)
    # An "edit" is Play's transaction: nothing is visible until it is committed.
    edit = play.call("POST", f"{API}/edits")["id"]
    try:
        if args.bundle:
            publish_bundle(play, edit, args)
        if args.screenshots:
            publish_screenshots(play, edit)
        if args.validate_only:
            play.call("POST", f"{API}/edits/{edit}:validate")
            play.call("DELETE", f"{API}/edits/{edit}")
            print("valid; nothing was published")
            return
        params = {"changesNotSentForReview": "true"} if args.changes_not_sent_for_review else {}
        play.call("POST", f"{API}/edits/{edit}:commit", params=params)
    except Exception:
        # Throw the edit away so the listing never shows half an update.
        if not args.dry_run:
            play.session.delete(f"{API}/edits/{edit}", timeout=60)
        raise
    print("dry run; nothing was sent" if args.dry_run else "published")


if __name__ == "__main__":
    try:
        main()
    except PlayError as e:
        sys.exit(f"publish-play: {e}")
