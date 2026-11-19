package com.devqube.drinktracker.ui.components

import androidx.annotation.DrawableRes
import com.devqube.drinktracker.R
import com.devqube.drinktracker.data.Tool

/** Grafika narzędzia Grażyny – wspólna dla widgetu i karty w aplikacji. */
@DrawableRes
fun toolDrawable(tool: Tool): Int = when (tool) {
    Tool.KAPEC -> R.drawable.ic_tool_kapec
    Tool.PATELNIA -> R.drawable.ic_tool_patelnia
    Tool.WALEK -> R.drawable.ic_tool_walek
}

fun toolName(tool: Tool): String = when (tool) {
    Tool.KAPEC -> "Kapeć"
    Tool.PATELNIA -> "Patelnia"
    Tool.WALEK -> "Wałek"
}
