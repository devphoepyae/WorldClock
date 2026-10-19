package com.worldclock.app.ui.screens

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.worldclock.app.R
import com.worldclock.app.data.prefs.AppTheme
import com.worldclock.app.ui.components.MinTouchTarget
import com.worldclock.app.ui.theme.WorldClockColors
import com.worldclock.app.ui.theme.WorldClockShapes
import com.worldclock.app.ui.theme.WorldClockTextStyles
import com.worldclock.app.ui.theme.WorldClockTheme
import com.worldclock.app.ui.theme.paletteFor

/**
 * The name of each theme, as shown in the picker.
 *
 * A resource rather than a property of the enum, so the names stay
 * translatable. Exhaustive, so adding a theme fails to compile until it is
 * named.
 */
@StringRes
internal fun themeNameRes(theme: AppTheme): Int = when (theme) {
    AppTheme.LIGHT_MONOCHROME -> R.string.theme_light_monochrome
    AppTheme.TRUE_DARK -> R.string.theme_true_dark
    AppTheme.WARM_SEPIA -> R.string.theme_warm_sepia
    AppTheme.COOL_SLATE -> R.string.theme_cool_slate
    AppTheme.HIGH_CONTRAST -> R.string.theme_high_contrast
}

/**
 * App settings presented as a bottom sheet.
 *
 * Opens from the gear action on the Clocks screen. The bottom bar keeps the
 * three destinations from the reference design; settings live behind an action
 * instead of a fourth tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    theme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = WorldClockColors.Background,
        contentColor = WorldClockColors.Foreground,
        scrimColor = Color(0x66000000),
        modifier = modifier,
    ) {
        SettingsContent(
            theme = theme,
            onThemeSelected = onThemeSelected,
        )
    }
}

/**
 * The sheet body. Kept separate from the sheet host so it can be rendered
 * directly in previews and screenshot tests.
 */
@Composable
internal fun SettingsContent(
    theme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
    ) {
        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.settings_eyebrow),
            style = WorldClockTextStyles.Eyebrow,
            color = WorldClockColors.MutedForeground,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.settings_title),
            style = WorldClockTextStyles.ScreenTitle,
            color = WorldClockColors.Foreground,
            modifier = Modifier.semantics { heading() },
        )

        Spacer(Modifier.height(20.dp))
        HorizontalDivider(color = WorldClockColors.Border)
        Spacer(Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.settings_appearance_label),
            style = WorldClockTextStyles.SectionLabel,
            color = WorldClockColors.MutedForeground,
            modifier = Modifier.semantics { heading() },
        )
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.settings_theme_group_label),
            style = WorldClockTextStyles.CityName,
            color = WorldClockColors.Foreground,
        )
        Spacer(Modifier.height(10.dp))

        // Presented like the app's other choice controls — a bordered container
        // holding one inverted pill per option — so the theme picker reads as
        // the same kind of control as the 12h/24h switch, only taller because a
        // theme needs a name and a preview rather than two characters.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(WorldClockShapes.medium)
                .background(WorldClockColors.Accent)
                .border(1.dp, WorldClockColors.Border, WorldClockShapes.medium)
                .padding(4.dp)
                // One radio group, so assistive technology announces the five
                // options as a set and says which one is chosen.
                .selectableGroup(),
        ) {
            AppTheme.entries.forEach { option ->
                ThemeOptionRow(
                    theme = option,
                    isSelected = option == theme,
                    onSelect = { onThemeSelected(option) },
                )
            }
        }

        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.settings_appearance_hint),
            style = WorldClockTextStyles.CityRegion,
            color = WorldClockColors.MutedForeground,
        )

        Spacer(Modifier.height(24.dp))
    }
}

/**
 * One theme in the picker: a preview swatch, the name, and the whole row as the
 * target.
 *
 * The row carries radio-button semantics with its own selected state, so a
 * screen reader announces the theme it would apply and whether it is the one in
 * force. The preview swatch is decorative and deliberately silent — its colour
 * is the name's illustration, not a second label.
 */
@Composable
private fun ThemeOptionRow(
    theme: AppTheme,
    isSelected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MinTouchTarget)
            .clip(WorldClockShapes.small)
            .background(if (isSelected) WorldClockColors.Primary else Color.Transparent)
            .selectable(
                selected = isSelected,
                role = Role.RadioButton,
                onClick = onSelect,
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ThemeSwatch(theme)
        Spacer(Modifier.width(12.dp))
        Text(
            text = stringResource(themeNameRes(theme)),
            style = WorldClockTextStyles.CityName,
            color = if (isSelected) {
                WorldClockColors.PrimaryForeground
            } else {
                WorldClockColors.Foreground
            },
        )
    }
}

/**
 * A thumbnail of a theme: its surface, its outline and its text colour.
 *
 * The colour values are read straight from the palette the theme resolves to,
 * so the preview cannot drift from what choosing the theme actually paints.
 */
@Composable
private fun ThemeSwatch(theme: AppTheme, modifier: Modifier = Modifier) {
    val palette = paletteFor(theme)
    Box(
        modifier = modifier
            .size(22.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(palette.background)
            .border(1.dp, palette.border, RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(palette.foreground),
        )
    }
}

@Preview(name = "Settings sheet 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun SettingsContentPreview() {
    WorldClockTheme {
        Column(verticalArrangement = Arrangement.Center) {
            SettingsContent(theme = AppTheme.LIGHT_MONOCHROME, onThemeSelected = {})
        }
    }
}

@Preview(name = "Settings sheet dark 360dp", widthDp = 360, heightDp = 800, showBackground = true)
@Composable
private fun SettingsContentDarkPreview() {
    WorldClockTheme(theme = AppTheme.TRUE_DARK) {
        Column(verticalArrangement = Arrangement.Center) {
            SettingsContent(theme = AppTheme.TRUE_DARK, onThemeSelected = {})
        }
    }
}
