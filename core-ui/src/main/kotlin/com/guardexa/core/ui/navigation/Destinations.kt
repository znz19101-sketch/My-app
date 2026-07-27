package com.guardexa.core.ui.navigation

sealed class Destination(val route:String){
    data object Splash:Destination("splash")
    data object Dashboard:Destination("dashboard")
    data object Settings:Destination("settings")
}
