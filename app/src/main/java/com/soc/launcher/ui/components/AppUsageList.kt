package com.soc.launcher.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import com.soc.launcher.data.model.*
import androidx.compose.foundation.layout.Arrangement
import com.soc.launcher.ui.theme.*

@Composable
fun AppUsageList(usage: AppUsageInfo) {
    val context = LocalContext.current
    val hours = usage.totalTimeInForeground / (1000 * 60 * 60)
    val minutes = (usage.totalTimeInForeground / (1000 * 60)) % 60
    val timeStr = when {
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }

    val icon = remember(usage.packageName) {
        try {
            context.packageManager.getApplicationIcon(usage.packageName).toBitmap().asImageBitmap()
        } catch (e: Exception) {
            null
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = context.packageManager.getLaunchIntentForPackage(usage.packageName)
                if (intent != null) {
                    context.startActivity(intent)
                }
            }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Image(
                    bitmap = icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.width(12.dp))
            }
            Text(
                text = usage.name,
                fontFamily = Raleway,
                color = FoltrainWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = timeStr,
            fontFamily = Raleway,
            color = FoltrainWhite.copy(alpha = 0.6f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Light
        )
    }
}