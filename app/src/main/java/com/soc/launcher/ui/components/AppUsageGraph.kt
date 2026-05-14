package com.soc.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size as ComposeSize
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.foundation.layout.Arrangement
import java.util.*
import com.soc.launcher.ui.theme.*

@Composable
fun AppUsageGraph(weeklyUsage: List<Long>) {
    // Increased to 12-hour scale
    val maxUsageMillis = 12L * 60 * 60 * 1000f
    val days = listOf("SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT")

    // Reduced height from 120.dp to 100.dp
    Box(modifier = Modifier
        .fillMaxWidth()
        .height(100.dp)
    ) {
        val hourLabels = listOf(3, 6, 9, 12)
        val todayIndex = remember {
            val cal = Calendar.getInstance()
            (cal.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY + 7) % 7
        }

        // Grid Lines, Hours, and Bars drawn on Canvas for perfect alignment
        Canvas(modifier = Modifier.fillMaxSize()) {
            val graphBottom = size.height - 30.dp.toPx()
            val graphTop = 10.dp.toPx()
            val graphHeight = graphBottom - graphTop

            // Draw Grid Lines
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.WHITE
                alpha = (255 * 0.3f).toInt()
                textSize = 8.sp.toPx()
            }

            hourLabels.forEach { hour ->
                val hMillis = hour.toLong() * 60 * 60 * 1000f
                val y = graphBottom - (hMillis / maxUsageMillis * graphHeight)

                drawLine(
                    color = FoltrainWhite.copy(alpha = 0.1f),
                    start = Offset(0f, y),
                    end = Offset(size.width, y),
                    strokeWidth = 0.5.dp.toPx()
                )

                drawContext.canvas.nativeCanvas.drawText("${hour}h", 0f, y - 4.dp.toPx(), textPaint)
            }

            // Draw Bars
            val barWidth = 16.dp.toPx()
            val spacing = size.width / days.size

            days.forEachIndexed { index, _ ->
                val usage = weeklyUsage.getOrNull(index) ?: 0L
                if (usage > 0) {
                    val barHeight = (usage.toFloat() / maxUsageMillis * graphHeight).coerceAtMost(graphHeight).coerceAtLeast(1.dp.toPx())
                    val isToday = index == todayIndex

                    val x = index * spacing + (spacing / 2) - (barWidth / 2)

                    // Main Bar
                    drawRoundRect(
                        color = if (isToday) FoltrainMain.copy(alpha = 0.4f) else FoltrainWhite.copy(alpha = 0.3f),
                        topLeft = Offset(x, graphBottom - barHeight),
                        size = ComposeSize(barWidth, barHeight),
                        cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                    )

                    // Solid bottom for the current day, or consistent baseline for others
                    val squareHeight = 2.dp.toPx().coerceAtMost(barHeight)
                    drawRect(
                        color = if (isToday) FoltrainMain else FoltrainWhite.copy(alpha = 0.3f),
                        topLeft = Offset(x, graphBottom - squareHeight),
                        size = ComposeSize(barWidth, squareHeight)
                    )
                }
            }
        }

        // X-axis labels and baseline
        Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.2f))
            )
            Row(
                modifier = Modifier.fillMaxWidth().height(29.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                days.forEachIndexed { index, day ->
                    val isToday = index == todayIndex
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(4.dp)
                                .background(if (isToday) FoltrainMain else Color.White.copy(alpha = 0.2f))
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = day,
                            fontFamily = Raleway,
                            color = if (isToday) FoltrainMain else Color.White.copy(alpha = 0.4f),
                            fontSize = 9.sp,
                            fontWeight = if (isToday) FontWeight.Medium else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}