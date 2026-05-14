package com.soc.launcher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MissedCalls(
    title: String,
    items: List<String>,
    onItemClick: ((Int) -> Unit)? = null,
    onPermissionClick: (() -> Unit)? = null,
    onClearClick: (() -> Unit)? = null,
    itemIcons: List<String?>? = null,
    itemSpamFlags: List<Boolean>? = null,
    maxItems: Int = Int.MAX_VALUE
) {
    val displayItems = items.take(maxItems)

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title.uppercase(),
                color = Color(0xFF4A90E2),
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            if (onClearClick != null && items.isNotEmpty() && items.first() != "No recent missed calls" && items.first() != "No recent messages") {
                Text(
                    "CLEAR",
                    color = Color(0xFF4A90E2).copy(alpha = 0.8f),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onClearClick() }
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (onPermissionClick != null) {
            Button(
                onClick = onPermissionClick,
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.1f)),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Grant Permission", color = Color.White, fontSize = 12.sp)
            }
        } else {
            displayItems.forEachIndexed { index, item ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = onItemClick != null) { onItemClick?.invoke(index) }
                        .padding(vertical = 8.dp)
                ) {
                    if (itemSpamFlags != null && index < itemSpamFlags.size && itemSpamFlags[index]) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Spam",
                            tint = Color.Red.copy(alpha = 0.6f),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                    } else if (itemIcons != null && index < itemIcons.size) {
                        val iconUri = itemIcons[index]
                        if (iconUri != null) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(Color.White.copy(alpha = 0.05f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.firstOrNull()?.toString()?.uppercase() ?: "?",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (item != "No recent missed calls" && item != "No recent messages") {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color.White.copy(alpha = 0.03f), RoundedCornerShape(16.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = item.firstOrNull()?.toString()?.uppercase() ?: "?",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        if (iconUri != null || (item != "No recent missed calls" && item != "No recent messages")) {
                            Spacer(Modifier.width(16.dp))
                        }
                    }
                    Text(
                        text = item,
                        color = if (itemSpamFlags != null && index < itemSpamFlags.size && itemSpamFlags[index]) Color.White.copy(alpha = 0.6f) else Color.White,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}