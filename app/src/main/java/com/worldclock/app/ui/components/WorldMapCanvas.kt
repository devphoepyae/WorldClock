package com.worldclock.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.worldclock.app.R
import com.worldclock.app.domain.map.EquirectangularProjection
import com.worldclock.app.domain.map.SolarPosition
import com.worldclock.app.domain.map.SubsolarPoint
import com.worldclock.app.domain.map.WorldLandmasses
import com.worldclock.app.ui.theme.LocalWorldClockPalette
import com.worldclock.app.ui.theme.WorldClockColors
import com.worldclock.app.ui.theme.WorldClockPalette
import com.worldclock.app.ui.theme.WorldClockShapes
import com.worldclock.app.ui.theme.WorldClockTextStyles
import java.time.Instant

/** Degrees between grid lines. */
private const val GRID_STEP_DEGREES = 30.0

/** Samples used to trace the terminator; one every 2° of longitude. */
private const val TERMINATOR_STEPS = 180

/**
 * Test-only tags, kept beside the map so tests never have to match display
 * strings or screen positions.
 */
internal object WorldMapTestTags {
    fun marker(cityId: String): String = "mapMarker_$cityId"

    fun cityPill(cityId: String): String = "mapCityPill_$cityId"

    const val SELECTED_CITY_PANEL: String = "mapSelectedCityPanel"

    const val MAP_CANVAS: String = "mapCanvas"
}

/**
 * The schematic world map (spec section 4.4).
 *
 * Everything is drawn with Compose on device from bundled vectors: landmasses,
 * a latitude/longitude graticule, the live day/night terminator, the active
 * city's meridian, and a UTC offset ruler. There are no map tiles, no map
 * provider and no API key.
 *
 * City markers are real composables rather than canvas shapes so each one gets a
 * 44 dp touch target, a ripple, and a screen-reader label describing the city and
 * its current time.
 */
@Composable
fun WorldMapCanvas(
    cities: List<CityCardModel>,
    now: Instant,
    onCityClick: (CityCardModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    val subsolar = remember(now) { SolarPosition.subsolarPoint(now) }
    val textMeasurer = rememberTextMeasurer()
    val activeCity = cities.firstOrNull { it.isActive }
    val mapDescription = stringResource(R.string.a11y_world_map)
    val rulerDescription = stringResource(R.string.a11y_utc_ruler)

    // Draw code is not composable, so the palette is read once here and passed
    // down into each drawing step.
    val palette = LocalWorldClockPalette.current
    val rulerTextStyle = remember(palette) {
        WorldClockTextStyles.MapRulerLabel.copy(color = palette.mutedForeground)
    }

    BoxWithConstraints(
        modifier = modifier
            .clip(WorldClockShapes.medium)
            .background(WorldClockColors.Card)
            .border(1.dp, WorldClockColors.Border, WorldClockShapes.medium)
            .selectableGroup(),
    ) {        val width = maxWidth
        val height = maxHeight

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .semantics {
                    contentDescription = "$mapDescription. $rulerDescription."
                },
        ) {
            drawOcean(palette)
            drawGraticule(palette)
            drawLandmasses(palette)
            drawNight(subsolar, palette)
            drawActiveMeridian(activeCity, palette)
            drawUtcRuler(textMeasurer, rulerTextStyle, palette)
        }

        cities.forEach { city ->
            CityMapMarker(
                city = city,
                x = width * EquirectangularProjection.xFraction(city.longitude).toFloat(),
                y = height * EquirectangularProjection.yFraction(city.latitude).toFloat(),
                onClick = { onCityClick(city) },
            )
        }
    }
}
private fun DrawScope.drawOcean(palette: WorldClockPalette) {
    drawRect(color = palette.card)
}

private fun DrawScope.drawGraticule(palette: WorldClockPalette) {
    val color = palette.border
    var longitude = EquirectangularProjection.MIN_LONGITUDE
    while (longitude <= EquirectangularProjection.MAX_LONGITUDE) {
        val x = size.width * EquirectangularProjection.xFraction(longitude).toFloat()
        drawLine(color, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
        longitude += GRID_STEP_DEGREES
    }
    var latitude = -60.0
    while (latitude <= 60.0) {
        val y = size.height * EquirectangularProjection.yFraction(latitude).toFloat()
        drawLine(color, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        latitude += GRID_STEP_DEGREES
    }
}

private fun DrawScope.drawLandmasses(palette: WorldClockPalette) {
    val color = palette.mapLandmass
    WorldLandmasses.polygons.forEach { outline ->
        val path = Path()
        outline.forEachIndexed { index, point ->
            val x = size.width * EquirectangularProjection.xFraction(point.longitude).toFloat()
            val y = size.height * EquirectangularProjection.yFraction(point.latitude).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        drawPath(path, color = color)
    }
}

/**
 * Fills the half of the globe currently in darkness.
 *
 * The terminator is traced by solving the horizon equation for every longitude,
 * then the path is closed through whichever pole is in shadow.
 */
private fun DrawScope.drawNight(subsolar: SubsolarPoint, palette: WorldClockPalette) {
    val path = Path()
    for (step in 0..TERMINATOR_STEPS) {
        val longitude = EquirectangularProjection.MIN_LONGITUDE +
            (EquirectangularProjection.MAX_LONGITUDE - EquirectangularProjection.MIN_LONGITUDE) *
            step / TERMINATOR_STEPS
        val latitude = SolarPosition.terminatorLatitude(longitude, subsolar)
        val x = size.width * EquirectangularProjection.xFraction(longitude).toFloat()
        val y = size.height * EquirectangularProjection.yFraction(latitude).toFloat()
        if (step == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    if (subsolar.latitude >= 0.0) {
        // The Sun is north of the equator, so the southern pole is in shadow.
        path.lineTo(size.width, size.height)
        path.lineTo(0f, size.height)
    } else {
        path.lineTo(size.width, 0f)
        path.lineTo(0f, 0f)
    }
    path.close()
    drawPath(path, color = palette.mapNight)
}

private fun DrawScope.drawActiveMeridian(city: CityCardModel?, palette: WorldClockPalette) {
    if (city == null) return
    val x = size.width * EquirectangularProjection.xFraction(city.longitude).toFloat()
    drawLine(
        color = palette.mapAccent,
        start = Offset(x, 0f),
        end = Offset(x, size.height),
        strokeWidth = 1.5f,
    )
}

/**
 * UTC offset ruler along the bottom edge: a tick every hour of longitude, with
 * a compact label every two hours.
 */
private fun DrawScope.drawUtcRuler(
    textMeasurer: TextMeasurer,
    textStyle: TextStyle,
    palette: WorldClockPalette,
) {
    val labelTop = size.height - 19.dp.toPx()
    val tickBottom = labelTop - 2.dp.toPx()
    val tickTop = tickBottom - 5.dp.toPx()

    for (offset in -12..14) {
        val longitude = offset * 15.0
        val x = size.width * EquirectangularProjection.xFraction(longitude).toFloat()
        drawLine(palette.border, Offset(x, tickTop), Offset(x, tickBottom), strokeWidth = 1f)
        if (offset % 2 != 0) continue

        val label = when {
            offset > 0 -> "+$offset"
            offset < 0 -> offset.toString()
            else -> "0"
        }
        val layout = textMeasurer.measure(label, textStyle)
        val halfWidth = layout.size.width / 2f
        // Skip labels that would spill over the map edge and collide with the
        // border; the ticks still mark every hour.
        if (x - halfWidth < 2f || x + halfWidth > size.width - 2f) continue

        drawText(
            textLayoutResult = layout,
            topLeft = Offset(x - halfWidth, labelTop),
        )
    }
}

/**
 * One city marker: a small dot for saved cities and a red diamond for the active
 * one. The composable is 44 dp so it is comfortably tappable.
 */
@Composable
private fun CityMapMarker(
    city: CityCardModel,
    x: Dp,
    y: Dp,
    onClick: () -> Unit,
) {
    val tapSize = 44.dp
    val description = if (city.isActive) {
        stringResource(
            R.string.a11y_map_marker_selected,
            city.name,
            city.timeText,
            city.offsetLabel,
        )
    } else {
        stringResource(R.string.a11y_map_marker, city.name, city.timeText, city.offsetLabel)
    }

    Box(
        modifier = Modifier
            .offset(x = x - tapSize / 2, y = y - tapSize / 2)
            .size(tapSize)
            .testTag(WorldMapTestTags.marker(city.id))
            .clip(CircleShape)
            .selectable(
                selected = city.isActive,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        if (city.isActive) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .rotate(45f)
                    .clip(RoundedCornerShape(2.dp))
                    .background(WorldClockColors.MapAccent)
                    .border(1.5.dp, WorldClockColors.Card, RoundedCornerShape(2.dp)),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(WorldClockColors.Card)
                    .border(1.dp, WorldClockColors.Foreground, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(WorldClockColors.Foreground),
                )
            }
        }
    }
}
