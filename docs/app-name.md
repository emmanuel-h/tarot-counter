# App Name & Branding

## System App Name

The name shown by the Android system (launcher icon, recent-apps screen, Settings → Apps) is
defined in the XML string resource `app_name`, referenced by `android:label` in
`AndroidManifest.xml`.

| Device language | Displayed name | Resource file |
|---|---|---|
| English (default) | Tarot Counter | `res/values/strings.xml` |
| French | Tarot | `res/values-fr/strings.xml` |

The French variant "Tarot" matches the in-app French title (`FrStrings.appTitle` in `AppLocale.kt`).

## In-App Title

The title shown inside the app (on the landing screen) comes from the `AppStrings.appTitle` field,
resolved at runtime via the `CompositionLocal`-based i18n system in `AppLocale.kt`:

| Locale | Title |
|---|---|
| `AppLocale.EN` | "Tarot Counter" |
| `AppLocale.FR` | "Tarot" |

The user can switch language at any time using the 🇬🇧 / 🇫🇷 flag chips on the landing screen.
The choice is persisted via DataStore and restored on next launch. If no preference is saved,
the app falls back to the device's system language.

## How the Two Systems Work Together

- The **system app name** (from `strings.xml`) is picked by Android based on the device language
  at install time (or when the device language changes). It cannot be changed by the app at runtime.
- The **in-app title** (from `AppLocale.kt`) follows the user's explicit in-app language choice,
  which may differ from the device language.

Both systems use the same naming convention so they stay consistent when the device language and
in-app language match.
