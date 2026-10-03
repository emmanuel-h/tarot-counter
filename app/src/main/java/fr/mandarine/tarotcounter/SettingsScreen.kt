package fr.mandarine.tarotcounter

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import fr.mandarine.tarotcounter.ui.theme.Dimens
import fr.mandarine.tarotcounter.ui.theme.TarotCounterTheme

// SettingsScreen (Salon restyle, issue #203): grouped cards of list rows, each with
// a leading icon.
//
//   ┌──────────────────────────────────┐
//   │ ←  Settings                      │
//   │ APPEARANCE                       │
//   │ ┌──────────────────────────────┐ │
//   │ │ [palette] Theme (Light|Dark) │ │
//   │ └──────────────────────────────┘ │
//   │ LANGUAGE                         │
//   │ ┌──────────────────────────────┐ │
//   │ │ [globe] (English | Français) │ │
//   │ └──────────────────────────────┘ │
//   │ HELP                             │
//   │ ┌──────────────────────────────┐ │
//   │ │ [book]  Rules              › │ │   → full-screen rules page
//   │ │ [mail]  Send Feedback      › │ │   → email client
//   │ └──────────────────────────────┘ │
//   │ ABOUT                            │
//   │ ┌──────────────────────────────┐ │
//   │ │ [info]  Version        2.2.0 │ │
//   │ └──────────────────────────────┘ │
//   └──────────────────────────────────┘
//
// onThemeChange:  persisted by the ViewModel when the user picks a theme.
// onLocaleChange: persisted by the ViewModel when the user picks a language.
// onBack:         back arrow → landing screen.
// versionName:    shown in About; defaults to the app's BuildConfig.VERSION_NAME.
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    onThemeChange: (AppTheme) -> Unit = {},
    onLocaleChange: (AppLocale) -> Unit = {},
    onBack: () -> Unit = {},
    versionName: String = BuildConfig.VERSION_NAME
) {
    // The current locale and theme come from CompositionLocals provided by MainActivity.
    val locale  = LocalAppLocale.current
    val theme   = LocalAppTheme.current
    val strings = appStrings(locale)
    val context = LocalContext.current

    // The rules page replaces this screen while it is open; back closes it.
    var showRules by remember { mutableStateOf(false) }
    if (showRules) {
        BackHandler { showRules = false }
        RulesScreen(onBack = { showRules = false }, modifier = modifier)
        return
    }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .widthIn(max = MAX_CONTENT_WIDTH)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.ScreenMargin)
                .padding(bottom = Dimens.SpaceL),
            verticalArrangement = Arrangement.spacedBy(Dimens.SpaceL)
        ) {
            SalonTopBar(title = strings.settingsTitle, onBack = onBack)

            // ── Appearance ────────────────────────────────────────────────────
            SettingsGroup(strings.appearanceLabel) {
                SettingsRow(icon = Icons.Default.Palette, label = strings.themeLabel) {
                    ChoiceToggle(
                        options  = listOf(AppTheme.LIGHT to strings.themeLight, AppTheme.DARK to strings.themeDark),
                        selected = theme,
                        onSelect = onThemeChange,
                        tag      = "theme",
                        modifier = Modifier.width(184.dp)
                    )
                }
            }

            // ── Language ──────────────────────────────────────────────────────
            // Language names are written in their own language (a French speaker
            // looks for "Français"), with text rather than flag emoji.
            SettingsGroup(strings.languageLabel) {
                SettingsRow(icon = Icons.Default.Language, label = null) {
                    ChoiceToggle(
                        options  = listOf(AppLocale.EN to "English", AppLocale.FR to "Français"),
                        selected = locale,
                        onSelect = onLocaleChange,
                        tag      = "locale",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // ── Help ──────────────────────────────────────────────────────────
            SettingsGroup(strings.helpLabel) {
                SettingsRow(
                    icon    = Icons.AutoMirrored.Filled.MenuBook,
                    label   = strings.rulesButton,
                    onClick = { showRules = true }
                ) { Chevron() }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                SettingsRow(
                    icon    = Icons.Default.Email,
                    label   = strings.feedbackButton,
                    onClick = {
                        // ACTION_SENDTO + "mailto:" opens only email clients.
                        val intent = Intent(Intent.ACTION_SENDTO, "mailto:mandarinetech.dev@gmail.com".toUri())
                        context.startActivity(Intent.createChooser(intent, null))
                    }
                ) { Chevron() }
            }

            // ── About ─────────────────────────────────────────────────────────
            SettingsGroup(strings.aboutLabel) {
                SettingsRow(icon = Icons.Default.Info, label = strings.versionLabel) {
                    Text(
                        text     = versionName,
                        style    = MaterialTheme.typography.bodyMedium,
                        color    = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("version_name")
                    )
                }
            }
        }
    }
}

// A titled group: small upper-case heading above a paper card holding its rows.
@Composable
private fun SettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.SpaceS)) {
        Text(
            text  = title.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        SalonCard(modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(0.dp)) {
            Column { content() }
        }
    }
}

// One settings row: leading icon, optional label, and a trailing slot (a toggle,
// a value, a chevron…). With onClick the whole row is a button.
@Composable
private fun SettingsRow(
    icon: ImageVector,
    label: String?,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = Dimens.SpaceM, vertical = Dimens.SpaceS),
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM)
    ) {
        Icon(
            imageVector        = icon,
            contentDescription = null, // the label names the row
            tint               = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (label != null) {
            Text(text = label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            trailing()
        } else {
            // No label: the trailing control takes the rest of the row.
            Box(modifier = Modifier.weight(1f)) { trailing() }
        }
    }
}

@Composable
private fun Chevron() {
    Icon(
        imageVector        = Icons.AutoMirrored.Filled.KeyboardArrowRight,
        contentDescription = null,
        tint               = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// A two-option segmented toggle, used for the theme and the language.
// `<T>` makes it generic: it works for AppTheme and AppLocale alike.
@Composable
private fun <T> ChoiceToggle(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    tag: String,
    modifier: Modifier = Modifier
) {
    val labelSize = rememberSharedAutoSizeState(options.map { it.second })
    SingleChoiceSegmentedButtonRow(modifier = modifier) {
        options.forEachIndexed { index, (value, label) ->
            SegmentedButton(
                shape    = SegmentedButtonDefaults.itemShape(index, options.size),
                selected = selected == value,
                // Re-selecting the current option is harmless: it is persisted again.
                onClick  = { onSelect(value) },
                icon     = {},
                colors   = salonSegmentedButtonColors(),
                modifier = Modifier.testTag("${tag}_$index")
            ) {
                AutoSizeText(
                    text            = label,
                    modifier        = Modifier.padding(horizontal = 1.dp),
                    sharedSizeState = labelSize
                )
            }
        }
    }
}

// ── Previews ──────────────────────────────────────────────────────────────────

@Preview(heightDp = 900)
@Composable
private fun SettingsScreenPreview(@PreviewParameter(ThemeModeProvider::class) dark: Boolean) {
    TarotCounterTheme(darkTheme = dark) {
        androidx.compose.material3.Surface(color = MaterialTheme.colorScheme.background) {
            SettingsScreen(versionName = "2.2.0")
        }
    }
}
