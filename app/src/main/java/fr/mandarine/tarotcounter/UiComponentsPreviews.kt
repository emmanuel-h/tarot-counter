package fr.mandarine.tarotcounter

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import fr.mandarine.tarotcounter.ui.theme.Dimens
import fr.mandarine.tarotcounter.ui.theme.TarotCounterTheme

// ─────────────────────────────────────────────────────────────────────────────
// Android Studio previews of the Salon components (issue #196).
//
// Each preview takes a `dark: Boolean` from [ThemeModeProvider], so Android
// Studio renders every component twice: once in the light "Salon" theme and
// once in the dark "Salon at night" theme. Previews are not shipped behaviour,
// so this file is excluded from mutation testing.
// ─────────────────────────────────────────────────────────────────────────────

/** Feeds `false` (light) then `true` (dark) to each preview. */
class ThemeModeProvider : PreviewParameterProvider<Boolean> {
    override val values = sequenceOf(false, true)
}

private val previewPlayers = listOf("Alice", "Bruno", "Chloé", "David", "Émile")

// Wraps a preview in the theme, the EN locale and the page background.
@Composable
private fun SalonPreview(dark: Boolean, content: @Composable () -> Unit) {
    TarotCounterTheme(darkTheme = dark) {
        CompositionLocalProvider(LocalAppLocale provides AppLocale.EN) {
            Column(
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.background)
                    .padding(Dimens.ScreenMargin),
                verticalArrangement = Arrangement.spacedBy(Dimens.SpaceM)
            ) { content() }
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun SalonCardPreview(@PreviewParameter(ThemeModeProvider::class) dark: Boolean) {
    SalonPreview(dark) {
        SalonCard(modifier = Modifier.fillMaxWidth(), title = "New game") {
            AppButton(text = "Start game", onClick = {}, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun FeltCardPreview(@PreviewParameter(ThemeModeProvider::class) dark: Boolean) {
    SalonPreview(dark) {
        FeltCard(modifier = Modifier.fillMaxWidth()) {
            androidx.compose.material3.Text(
                text  = "Round 5",
                style = MaterialTheme.typography.headlineLarge
            )
            AvatarStack(
                names     = previewPlayers.take(4),
                size      = AvatarSize.M,
                ringColor = fr.mandarine.tarotcounter.ui.theme.LightTarotColors.felt
            )
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun PlayerAvatarPreview(@PreviewParameter(ThemeModeProvider::class) dark: Boolean) {
    SalonPreview(dark) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceS),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            PlayerAvatar(name = "Alice", seatIndex = 0, size = AvatarSize.S)
            PlayerAvatar(name = "Bruno", seatIndex = 1, size = AvatarSize.M)
            PlayerAvatar(name = "Chloé", seatIndex = 2, size = AvatarSize.L)
        }
        AvatarStack(names = previewPlayers)
        AvatarStack(names = previewPlayers.take(4), size = AvatarSize.M)
    }
}

@Preview(widthDp = 360)
@Composable
private fun SuitDividerPreview(@PreviewParameter(ThemeModeProvider::class) dark: Boolean) {
    SalonPreview(dark) { SuitDivider() }
}

@Preview(widthDp = 360)
@Composable
private fun SectionHeaderPreview(@PreviewParameter(ThemeModeProvider::class) dark: Boolean) {
    SalonPreview(dark) {
        SectionHeader(title = "Who took?")
        SectionHeader(title = "Last rounds") {
            AppTextButton(text = "See all", onClick = {})
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun ScoreTextPreview(@PreviewParameter(ThemeModeProvider::class) dark: Boolean) {
    SalonPreview(dark) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.SpaceM),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            ScoreText(score = 96, size = ScoreSize.S)
            ScoreText(score = -48, size = ScoreSize.M)
            ScoreText(score = 312, size = ScoreSize.XL)
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun SalonTopBarPreview(@PreviewParameter(ThemeModeProvider::class) dark: Boolean) {
    SalonPreview(dark) {
        SalonTopBar(
            title   = "Tarot Counter",
            actions = listOf(TopBarAction(Icons.Default.Settings, "Settings") {})
        )
        SalonTopBar(
            title   = "Round 5",
            onBack  = {},
            actions = listOf(
                TopBarAction(Icons.AutoMirrored.Filled.ShowChart, "Score history") {},
                TopBarAction(Icons.Default.Settings, "Settings") {}
            )
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun ButtonsPreview(@PreviewParameter(ThemeModeProvider::class) dark: Boolean) {
    SalonPreview(dark) {
        AppButton(text = "Confirm round", onClick = {}, modifier = Modifier.fillMaxWidth())
        AppOutlinedButton(text = "Skip round", onClick = {}, modifier = Modifier.fillMaxWidth())
        AppButton(text = "Disabled", onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth())
        AppTextButton(text = "Cancel", onClick = {})
    }
}

@Preview(widthDp = 360)
@Composable
private fun SegmentedPreview(@PreviewParameter(ThemeModeProvider::class) dark: Boolean) {
    SalonPreview(dark) {
        val options   = listOf("3", "4", "5")
        val labelSize = rememberSharedAutoSizeState(AppLocale.EN)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    shape    = SegmentedButtonDefaults.itemShape(index, options.size),
                    selected = option == "4",
                    onClick  = {},
                    icon     = {},
                    colors   = salonSegmentedButtonColors()
                ) {
                    AutoSizeText(
                        text            = option,
                        modifier        = Modifier.padding(horizontal = 1.dp),
                        sharedSizeState = labelSize
                    )
                }
            }
        }
    }
}

@Preview(widthDp = 360)
@Composable
private fun TextFieldsPreview(@PreviewParameter(ThemeModeProvider::class) dark: Boolean) {
    SalonPreview(dark) {
        PlayerNameField(
            value = "Alice", onValueChange = {}, seatIndex = 0,
            placeholder = "Player 1", modifier = Modifier.fillMaxWidth()
        )
        PlayerNameField(
            value = "", onValueChange = {}, seatIndex = 1,
            placeholder = "Player 2", modifier = Modifier.fillMaxWidth()
        )
        PlayerNameField(
            value = "Alice", onValueChange = {}, seatIndex = 2, placeholder = "Player 3",
            isError = true, supportingText = "Name already used",
            modifier = Modifier.fillMaxWidth()
        )
        SalonTextField(value = "", onValueChange = {}, placeholder = "Points", modifier = Modifier.fillMaxWidth())
    }
}
