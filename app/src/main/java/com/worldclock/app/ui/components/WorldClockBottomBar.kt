package com.worldclock.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.worldclock.app.R

/** The two navigable destinations in the bottom bar. */
enum class WorldClockTab {
    Clocks,
    Map,
}

/**
 * Bottom navigation: Search, Clocks and Map (spec section 3).
 *
 * Search is an action that opens the city search sheet; Clocks and Map are the
 * destinations and expose selected-state semantics.
 */
@Composable
fun WorldClockBottomBar(
    selectedTab: WorldClockTab,
    onSelectTab: (WorldClockTab) -> Unit,
    onSearchClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircleIconButton(
            iconRes = R.drawable.ic_search,
            contentDescription = stringResource(R.string.nav_search),
            onClick = onSearchClick,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        CircleIconButton(
            iconRes = R.drawable.ic_clock,
            contentDescription = stringResource(R.string.nav_clocks),
            onClick = { onSelectTab(WorldClockTab.Clocks) },
            selected = selectedTab == WorldClockTab.Clocks,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
        CircleIconButton(
            iconRes = R.drawable.ic_globe,
            contentDescription = stringResource(R.string.nav_map),
            onClick = { onSelectTab(WorldClockTab.Map) },
            selected = selectedTab == WorldClockTab.Map,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
}
