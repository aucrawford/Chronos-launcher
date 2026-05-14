package com.soc.launcher.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.soc.launcher.R

val Raleway = FontFamily(
    Font(R.font.raleway_regular, FontWeight.Normal),
    Font(R.font.raleway_extrabold, FontWeight.Bold),
    Font(R.font.raleway_extralight, FontWeight.Light),
    Font(R.font.raleway_semibold, FontWeight.Medium)
)

val FoltrainTypography = Typography(
    // Large titles (Clock, Screen Headers)
    displayLarge = TextStyle(
        fontFamily = Raleway,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp
    ),
    // Bold accents (VPN: ACTIVE, TEMP: 40C)
    labelLarge = TextStyle(
        fontFamily = Raleway,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp
    ),
    // Standard Labels (Stats, App Names)
    bodyMedium = TextStyle(
        fontFamily = Raleway,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp
    ),
    // Bold accents
    labelMedium = TextStyle(
        fontFamily = Raleway,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    ),
    displaySmall = TextStyle(
        fontFamily = Raleway,
        fontWeight = FontWeight.Light,
        fontSize = 15.sp
    )
)