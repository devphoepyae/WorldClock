package com.worldclock.app.ui.navigation

/** Navigation routes for the app shell. */
object Routes {

    const val CLOCKS = "clocks"
    const val MAP = "map"
    const val PLANNER = "planner"

    const val CITY_ID_ARG = "cityId"
    const val CITY_DETAIL = "city/{$CITY_ID_ARG}"

    fun cityDetail(cityId: String): String = "city/$cityId"
}
