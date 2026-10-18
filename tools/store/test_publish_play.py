"""Tests for publish-play.py's listing text (--listing), run with no network:

    python3 -m unittest discover tools/store
"""
import contextlib, importlib.util, io, os, sys, tempfile, unittest
from unittest import mock

# The script's file name has a hyphen, so it cannot be imported with a plain
# `import`; load it from its path instead.
HERE = os.path.dirname(os.path.abspath(__file__))
spec = importlib.util.spec_from_file_location("publish_play", os.path.join(HERE, "publish-play.py"))
pp = importlib.util.module_from_spec(spec)
spec.loader.exec_module(pp)


def write_listing(root, language, title="Titre", short="Court", full="Long"):
    """Create root/<language>/ with the three listing files."""
    folder = os.path.join(root, language)
    os.makedirs(folder)
    for name, text in [("title.txt", title), ("short_description.txt", short),
                       ("full_description.txt", full)]:
        with open(os.path.join(folder, name), "w", encoding="utf-8") as f:
            f.write(text + "\n")


def run_main(*argv):
    """Run main() with these arguments; return (stdout, PlayError message or None)."""
    out = io.StringIO()
    with mock.patch.object(sys, "argv", ["publish-play.py", *argv]), contextlib.redirect_stdout(out):
        try:
            pp.main()
        except pp.PlayError as e:
            return out.getvalue(), str(e)
    return out.getvalue(), None


class ListingTextTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        self.root = self.tmp.name

    def tearDown(self):
        self.tmp.cleanup()

    def test_repository_listing_is_within_play_limits(self):
        listings = pp.listing_text(pp.LISTING)
        self.assertEqual(sorted(listings), ["en-US", "fr-FR"])
        for body in listings.values():
            self.assertLessEqual(len(body["title"]), 30)
            self.assertLessEqual(len(body["shortDescription"]), 80)
            self.assertLessEqual(len(body["fullDescription"]), 4000)

    def test_reads_fields_and_drops_trailing_newline(self):
        write_listing(self.root, "fr-FR", title="Compteur", short="Court", full="Ligne 1\n\nLigne 2")
        self.assertEqual(pp.listing_text(self.root)["fr-FR"], {
            "language": "fr-FR", "title": "Compteur", "shortDescription": "Court",
            "fullDescription": "Ligne 1\n\nLigne 2"})

    def test_limit_counts_characters_not_bytes(self):
        # 30 accented letters are 60 bytes in UTF-8 but 30 characters: allowed.
        write_listing(self.root, "fr-FR", title="é" * 30)
        self.assertEqual(len(pp.listing_text(self.root)["fr-FR"]["title"]), 30)

    def test_each_limit_is_enforced(self):
        for kwargs, field in [({"title": "x" * 31}, "title"),
                              ({"short": "x" * 81}, "shortDescription"),
                              ({"full": "x" * 4001}, "fullDescription")]:
            with self.subTest(field=field), tempfile.TemporaryDirectory() as root:
                write_listing(root, "en-US", **kwargs)
                with self.assertRaisesRegex(pp.PlayError, f"en-US {field} is"):
                    pp.listing_text(root)

    def test_missing_file_is_refused(self):
        write_listing(self.root, "en-US")
        os.remove(os.path.join(self.root, "en-US", "title.txt"))
        with self.assertRaisesRegex(pp.PlayError, "missing"):
            pp.listing_text(self.root)

    def test_dry_run_patches_each_language_inside_one_edit(self):
        out, error = run_main("--listing", "--dry-run")
        self.assertIsNone(error)
        calls = [line.strip() for line in out.splitlines() if line.startswith("  ") and not line.startswith("    ")]
        self.assertEqual(calls, ["POST /edits",
                                 "PATCH /edits/dry-run/listings/en-US",
                                 "PATCH /edits/dry-run/listings/fr-FR",
                                 "POST /edits/dry-run:commit"])

    def test_text_over_a_limit_fails_before_any_call(self):
        write_listing(self.root, "en-US", title="x" * 31)
        with mock.patch.object(pp, "LISTING", self.root):
            out, error = run_main("--listing", "--dry-run")
        self.assertIn("31 characters; Play takes 30", error)
        self.assertEqual(out, "")  # not even the edit was created


if __name__ == "__main__":
    unittest.main()
