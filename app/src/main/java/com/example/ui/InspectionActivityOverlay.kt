package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Air
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Grain
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.ActivityStatus
import com.example.domain.ActivityType
import com.example.domain.InspectionActivityItem

enum class ActivityFilter {
    ALL,
    FEATURES,
    ANOMALIES
}

/**
 * High-tech glassmorphic overlay for displaying real-time drosophila features
 * and anomalies as they are processed by the Gemini API.
 */
@Composable
fun InspectionActivityOverlay(
    activities: List<InspectionActivityItem>,
    isAnalyzing: Boolean,
    onSharePdfReport: () -> Unit,
    onViewDetailReport: () -> Unit,
    onClearActivities: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(true) }
    var activeFilter by remember { mutableStateOf(ActivityFilter.ALL) }
    val listState = rememberLazyListState()

    // Auto-scroll to top as new real-time items are processed
    LaunchedEffect(activities.size) {
        if (activities.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    // Auto-expand overlay when Gemini analysis starts
    LaunchedEffect(isAnalyzing) {
        if (isAnalyzing) {
            isExpanded = true
        }
    }

    val anomalyCount = activities.count { it.type == ActivityType.ANOMALY || it.status == ActivityStatus.DEFECT }
    val featureCount = activities.count { it.type == ActivityType.FEATURE && it.status == ActivityStatus.PASS }

    val filteredActivities = remember(activities, activeFilter) {
        when (activeFilter) {
            ActivityFilter.ALL -> activities
            ActivityFilter.FEATURES -> activities.filter { it.type == ActivityType.FEATURE }
            ActivityFilter.ANOMALIES -> activities.filter { it.type == ActivityType.ANOMALY || it.status == ActivityStatus.DEFECT }
        }
    }

    Box(modifier = modifier) {
        if (!isExpanded) {
            // Minimized Floating Pill HUD
            Surface(
                onClick = { isExpanded = true },
                shape = RoundedCornerShape(24.dp),
                color = Color(0xDD0F172A),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                shadowElevation = 6.dp,
                modifier = Modifier.padding(8.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    PulsingLiveIndicator(isAnalyzing = isAnalyzing, hasAnomalies = anomalyCount > 0)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Inspection Activity",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    if (anomalyCount > 0) {
                        Surface(
                            color = Color(0xFFEF4444),
                            shape = CircleShape,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$anomalyCount",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    } else if (featureCount > 0) {
                        Surface(
                            color = Color(0xFF10B981),
                            shape = CircleShape,
                            modifier = Modifier.size(20.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "$featureCount",
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Outlined.ExpandLess,
                        contentDescription = "Expand Inspection Activity",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        } else {
            // Expanded HUD Panel
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xEA0F172A) // 92% Slate 900
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp)
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier.weight(1f, fill = false),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PulsingLiveIndicator(isAnalyzing = isAnalyzing, hasAnomalies = anomalyCount > 0)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = "Inspection Activity",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = if (isAnalyzing) "Gemini API real-time feature streaming..." else "Live Drosophila Morphology Feed",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isAnalyzing) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Badge Counters
                            if (featureCount > 0) {
                                Surface(
                                    color = Color(0xFF065F46).copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
                                ) {
                                    Text(
                                        text = "$featureCount Pass",
                                        color = Color(0xFF34D399),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        softWrap = false,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            if (anomalyCount > 0) {
                                Surface(
                                    color = Color(0xFF7F1D1D).copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
                                ) {
                                    Text(
                                        text = "$anomalyCount Anomaly",
                                        color = Color(0xFFFCA5A5),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        softWrap = false,
                                        maxLines = 1,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(4.dp))
                            }

                            // Minimize Button
                            IconButton(
                                onClick = { isExpanded = false },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ExpandMore,
                                    contentDescription = "Collapse Activity Panel",
                                    tint = Color(0xFFCBD5E1),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    // Real-time Loading Indicator
                    if (isAnalyzing) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = Color(0xFF38BDF8),
                            trackColor = Color(0xFF1E293B)
                        )
                    }

                    // Filter Chips Row
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ActivityFilterChip(
                            label = "All (${activities.size})",
                            selected = activeFilter == ActivityFilter.ALL,
                            onClick = { activeFilter = ActivityFilter.ALL }
                        )
                        ActivityFilterChip(
                            label = "Features ($featureCount)",
                            selected = activeFilter == ActivityFilter.FEATURES,
                            onClick = { activeFilter = ActivityFilter.FEATURES }
                        )
                        ActivityFilterChip(
                            label = "Anomalies ($anomalyCount)",
                            selected = activeFilter == ActivityFilter.ANOMALIES,
                            onClick = { activeFilter = ActivityFilter.ANOMALIES },
                            highlightColor = if (anomalyCount > 0) Color(0xFFEF4444) else null
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Scrollable Activity List
                    if (filteredActivities.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp)
                                .background(Color(0xFF1E293B).copy(alpha = 0.5f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isAnalyzing) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF38BDF8),
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Scanning drosophila features with Gemini...",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color(0xFFE2E8F0)
                                    )
                                }
                            } else {
                                Text(
                                    text = if (activities.isEmpty()) "검사 시작 또는 촬영 시 실시간 감지 목록이 표시됩니다." else "선택한 필터에 해당하는 항목이 없습니다.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 80.dp, max = 220.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            items(
                                items = filteredActivities,
                                key = { it.id }
                            ) { item ->
                                ActivityItemRow(item = item)
                            }
                        }
                    }

                    // Action Footer Row
                    if (activities.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = onClearActivities,
                                modifier = Modifier.height(36.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = Color(0xFF94A3B8)
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF334155))
                            ) {
                                Text("목록 지우기", fontSize = 11.sp)
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                OutlinedButton(
                                    onClick = onViewDetailReport,
                                    modifier = Modifier.height(36.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = Color(0xFFE2E8F0)
                                    ),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF475569))
                                ) {
                                    Text("상세 분석", fontSize = 11.sp)
                                }

                                Button(
                                    onClick = onSharePdfReport,
                                    modifier = Modifier.height(36.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF10B981),
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Share,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("PDF 공유", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    highlightColor: Color? = null
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = if (selected) {
            highlightColor?.copy(alpha = 0.25f) ?: Color(0xFF38BDF8).copy(alpha = 0.2f)
        } else {
            Color(0xFF1E293B)
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) highlightColor ?: Color(0xFF38BDF8) else Color(0xFF334155)
        )
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) highlightColor ?: Color(0xFF38BDF8) else Color(0xFF94A3B8),
            softWrap = false,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ActivityItemRow(item: InspectionActivityItem) {
    val isAnomaly = item.type == ActivityType.ANOMALY || item.status == ActivityStatus.DEFECT
    val isProcessing = item.status == ActivityStatus.PROCESSING

    val cardBg = when {
        isProcessing -> Color(0xFF0F2236)
        isAnomaly -> Color(0xFF2D1217)
        else -> Color(0xFF13281E)
    }

    val borderColor = when {
        isProcessing -> Color(0xFF0284C7).copy(alpha = 0.6f)
        isAnomaly -> Color(0xFFEF4444).copy(alpha = 0.6f)
        else -> Color(0xFF10B981).copy(alpha = 0.5f)
    }

    val icon: ImageVector = when {
        isAnomaly -> Icons.Outlined.Warning
        item.anatomicalRegion.contains("eye", ignoreCase = true) -> Icons.Outlined.Visibility
        item.anatomicalRegion.contains("wing", ignoreCase = true) -> Icons.Outlined.Air
        item.anatomicalRegion.contains("bristle", ignoreCase = true) || item.anatomicalRegion.contains("chaetae", ignoreCase = true) -> Icons.Outlined.Grain
        item.anatomicalRegion.contains("cuticle", ignoreCase = true) || item.anatomicalRegion.contains("abdomen", ignoreCase = true) -> Icons.Outlined.Layers
        else -> Icons.Outlined.BugReport
    }

    val iconTint = when {
        isProcessing -> Color(0xFF38BDF8)
        isAnomaly -> Color(0xFFEF4444)
        else -> Color(0xFF10B981)
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Left Status Icon
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(iconTint.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = iconTint,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Body
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.anatomicalRegion,
                        modifier = Modifier.weight(1f, fill = false),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Confidence Pill
                        if (!isProcessing && item.confidence > 0) {
                            Text(
                                text = "${item.confidence}%",
                                fontSize = 10.sp,
                                color = Color(0xFF94A3B8),
                                fontWeight = FontWeight.SemiBold,
                                softWrap = false,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }

                        // Status Badge
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = when {
                                isProcessing -> Color(0xFF0284C7)
                                isAnomaly -> Color(0xFFEF4444)
                                else -> Color(0xFF10B981)
                            }
                        ) {
                            Text(
                                text = when {
                                    isProcessing -> "ANALYZING"
                                    isAnomaly -> "DEFECT"
                                    else -> "PASS"
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White,
                                softWrap = false,
                                maxLines = 1,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.details,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 11.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 14.sp
                )

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = item.specimenSource,
                        fontSize = 9.sp,
                        color = Color(0xFF64748B)
                    )
                    Text(
                        text = item.formattedTime,
                        fontSize = 9.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        }
    }
}

@Composable
private fun PulsingLiveIndicator(isAnalyzing: Boolean, hasAnomalies: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isAnalyzing) 500 else 1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val color = when {
        isAnalyzing -> Color(0xFF38BDF8) // Cyan pulsing during active Gemini call
        hasAnomalies -> Color(0xFFEF4444) // Red pulsing if anomaly
        else -> Color(0xFF10B981) // Green idle
    }

    Box(
        modifier = Modifier
            .size(10.dp)
            .alpha(if (isAnalyzing || hasAnomalies) alpha else 1f)
            .background(color, CircleShape)
            .border(1.5.dp, color.copy(alpha = 0.5f), CircleShape)
    )
}
