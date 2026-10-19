package com.worldclock.app.ui.widget

import androidx.compose.ui.unit.dp
import com.worldclock.app.ui.mapping.WIDGET_MAX_ROWS

/**
 * The widget's geometry, in one place, because two callers need the same
 * numbers: the drawing, and [widgetRowBudget], which works out how many cities
 * fit in the space the launcher has given the card.
 *
 * The font sizes are here for the same reason. A row's height is derived from
 * the size of the text inside it, so the budget would drift away from the
 * drawing if the two ever stated those numbers separately.
 */
internal object WidgetGeometry {

    /** The card's corner radius, matching the app's own cards. */
    val CornerRadius = 16.dp

    val HorizontalPadding = 14.dp
    val VerticalPadding = 12.dp

    /** The header: a title line, then a hairline with a gap either side. */
    const val HeaderSizeSp = 11
    val GapAboveRule = 8.dp
    val RuleHeight = 1.dp
    val GapBelowRule = 7.dp

    /** One city row: the tallest child is the time, then the gap below it. */
    const val TimeSizeSp = 15
    const val NameSizeSp = 13
    val RowSpacing = 6.dp

    /** The `+N more` line, drawn only when cities had to be counted. */
    const val CountSizeSp = 11

    /**
     * A line of text's height as a multiple of its font size.
     *
     * Roboto's own metrics are about 1.17 em and a `TextView` adds font padding
     * on top, so 1.3 is the honest round-up: it errs towards a *smaller* budget,
     * which costs a little unused space at the bottom of the card and never a
     * row cut off by the launcher.
     */
    const val LineHeightFactor = 1.3f
}

/**
 * How many city rows fit in a card [availableHeightDp] tall.
 *
 * The widget declares a 4×2 cell footprint, but a cell is not a fixed number of
 * dp: the launcher decides, and it is usually generous. So the budget is taken
 * from the size the launcher reports for *this* widget instead of from a
 * constant, which also means a widget the user has resized taller shows more
 * cities and `+N more` always counts the truth (spec section 5.11).
 *
 * [savedCityCount] is how many cities the app holds. It matters because the
 * `+N more` line is part of the budget: when it will be drawn, one city has to
 * give up its row to it. Over-counting is safe — an id the directory no longer
 * knows reserves a row that is then simply left empty — so the caller can pass
 * the saved list as it stands.
 *
 * A launcher that reports nothing ([availableHeightDp] of zero or less) falls
 * back to [WIDGET_MAX_ROWS], the same answer as before this existed.
 */
internal fun widgetRowBudget(
    availableHeightDp: Int,
    savedCityCount: Int,
    fontScale: Float = 1f,
): Int {
    if (availableHeightDp <= 0) return WIDGET_MAX_ROWS

    val rows = rowsThatFit(availableHeightDp, fontScale, withCountLine = false)

    return if (savedCityCount <= rows) {
        rows
    } else {
        rowsThatFit(availableHeightDp, fontScale, withCountLine = true)
    }
}

/** The chrome every card carries, whether or not it draws a count line. */
private fun rowsThatFit(heightDp: Int, fontScale: Float, withCountLine: Boolean): Int {
    val chrome = WidgetGeometry.VerticalPadding.value * 2 +
        lineHeightDp(WidgetGeometry.HeaderSizeSp, fontScale) +
        (WidgetGeometry.GapAboveRule + WidgetGeometry.RuleHeight + WidgetGeometry.GapBelowRule).value +
        (if (withCountLine) lineHeightDp(WidgetGeometry.CountSizeSp, fontScale) else 0f)

    val rowPitch = lineHeightDp(WidgetGeometry.TimeSizeSp, fontScale) +
        WidgetGeometry.RowSpacing.value

    // The last row does not need the gap that follows it.
    val rows = ((heightDp - chrome + WidgetGeometry.RowSpacing.value) / rowPitch).toInt()

    // At least one city, so a cramped card still says something; at most the
    // design's own ceiling, so a stretched one stays a glance and not a list.
    return rows.coerceIn(1, WIDGET_MAX_ROWS)
}

/** A line of text in the widget, in dp, for the density and font size in use. */
private fun lineHeightDp(sizeSp: Int, fontScale: Float): Float =
    sizeSp * WidgetGeometry.LineHeightFactor * fontScale
