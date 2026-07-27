package com.guardexa.core.ui.components
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable

@Composable
fun GuardexaCard(content:@Composable ()->Unit){
    Card{ content() }
}
