package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.AiAnalysisRecord
import com.example.data.MeasurementRecord
import com.example.ui.theme.DotBlack
import com.example.ui.theme.DotCardMist
import com.example.ui.theme.DotFailRed
import com.example.ui.theme.DotOffWhite
import com.example.ui.theme.DotPassGreen
import com.example.ui.theme.DotSlate
import com.example.ui.theme.DotTagBorder
import com.example.ui.theme.DotViolet
import com.example.ui.theme.DotWhite
import com.example.util.PdfReportGenerator
import com.example.util.rememberDebouncedClick

/**
 * SpcStatisticsScreen: Statistical Process Control (SPC) & Measurement History
 * Displays Cpk process capability index, Gaussian distribution stats, and AI historical records.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpcStatisticsScreen(viewModel: MainViewModel, modifier: Modifier = Modifier) {
    val spcResult by viewModel.spcResult.collectAsState()
    val history by viewModel.measurementHistory.collectAsState()
    val aiHistory by viewModel.filteredAiHistory.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val dateFilter by viewModel.dateFilter.collectAsState()
    val selectedAiRecords by viewModel.selectedAiRecords.collectAsState()
    val selectedDetail by viewModel.selectedInspectionDetail.collectAsState()
    val context = LocalContext.current

    // Display individual inspection result screen when selected
    if (selectedDetail != null) {
        IndividualInspectionResultDialog(
            record = selectedDetail!!,
            onDismiss = { viewModel.clearInspectionDetail() }
        )
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let { viewModel.exportSelectedToCsv(context, it) }
    }

    val debouncedExportCsv = rememberDebouncedClick {
        exportLauncher.launch("ai_inspection_history_${System.currentTimeMillis()}.csv")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DotOffWhite)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = DotCardMist),
            shape = RoundedCornerShape(0.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "SPC 공정 능력 통계",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = DotBlack,
                        modifier = Modifier.weight(1f, fill = false),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    val cpkBadge = when {
                        spcResult.cpk >= 1.33f -> "6-Sigma 우수" to DotPassGreen
                        spcResult.cpk >= 1.00f -> "공정 보통" to DotViolet
                        spcResult.sampleCount > 0 -> "개선 필요" to DotFailRed
                        else -> "데이터 대기" to DotSlate
                    }
                    Surface(
                        color = cpkBadge.second.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(22.dp),
                        border = BorderStroke(1.dp, cpkBadge.second)
                    ) {
                        Text(
                            text = cpkBadge.first,
                            color = cpkBadge.second,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatItem("공정능력 (Cpk)", String.format("%.2f", spcResult.cpk), modifier = Modifier.weight(1f))
                    StatItem("평균 단차 (μ)", "${String.format("%.3f", spcResult.mean)}mm", modifier = Modifier.weight(1f))
                    StatItem("표준편차 (σ)", "${String.format("%.3f", spcResult.stdDev)}mm", modifier = Modifier.weight(1f))
                    StatItem("검사 표본수", "${spcResult.sampleCount}건", modifier = Modifier.weight(1f))
                }
            }
        }

        Text(
            "최근 검사 이력",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = DotBlack,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentPadding = PaddingValues(16.dp)
        ) {
            items(history) { record ->
                HistoryItem(record)
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "AI 분석 이력",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = DotBlack,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("검색어 입력 (예: scratch, anomaly)", color = DotSlate) },
                    leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search", tint = DotSlate, modifier = Modifier.size(18.dp)) },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                                Icon(Icons.Outlined.Close, contentDescription = "Clear Search", tint = DotSlate, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(0.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = DotViolet,
                        unfocusedBorderColor = DotTagBorder.copy(alpha = 0.4f),
                        focusedContainerColor = DotWhite,
                        unfocusedContainerColor = DotWhite
                    )
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = dateFilter == DateFilter.ALL,
                        onClick = { viewModel.updateDateFilter(DateFilter.ALL) },
                        label = { Text("전체") },
                        shape = RoundedCornerShape(22.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DotViolet,
                            selectedLabelColor = DotWhite,
                            containerColor = DotCardMist,
                            labelColor = DotBlack
                        ),
                        border = BorderStroke(1.dp, if (dateFilter == DateFilter.ALL) DotViolet else DotTagBorder.copy(alpha = 0.35f))
                    )
                    FilterChip(
                        selected = dateFilter == DateFilter.TODAY,
                        onClick = { viewModel.updateDateFilter(DateFilter.TODAY) },
                        label = { Text("오늘") },
                        shape = RoundedCornerShape(22.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DotViolet,
                            selectedLabelColor = DotWhite,
                            containerColor = DotCardMist,
                            labelColor = DotBlack
                        ),
                        border = BorderStroke(1.dp, if (dateFilter == DateFilter.TODAY) DotViolet else DotTagBorder.copy(alpha = 0.35f))
                    )
                    FilterChip(
                        selected = dateFilter == DateFilter.LAST_7_DAYS,
                        onClick = { viewModel.updateDateFilter(DateFilter.LAST_7_DAYS) },
                        label = { Text("최근 7일") },
                        shape = RoundedCornerShape(22.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = DotViolet,
                            selectedLabelColor = DotWhite,
                            containerColor = DotCardMist,
                            labelColor = DotBlack
                        ),
                        border = BorderStroke(1.dp, if (dateFilter == DateFilter.LAST_7_DAYS) DotViolet else DotTagBorder.copy(alpha = 0.35f))
                    )
                }

                if (selectedAiRecords.isNotEmpty()) {
                    Button(
                        onClick = debouncedExportCsv,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        shape = RoundedCornerShape(0.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DotViolet, contentColor = DotWhite)
                    ) {
                        Text("선택 항목 CSV 내보내기 (${selectedAiRecords.size}건)", fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            if (aiHistory.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp, horizontal = 4.dp),
                        shape = RoundedCornerShape(0.dp),
                        color = DotCardMist
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = if (searchQuery.isBlank()) "📭" else "🔍",
                                fontSize = 36.sp
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (searchQuery.isBlank()) "저장된 AI 분석 이력이 없습니다" else "일치하는 검색 결과가 없습니다",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = DotBlack
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (searchQuery.isBlank()) "검사 화면에서 '일괄 분석'을 실행하거나 단일 검사를 진행해보세요."
                                else "'$searchQuery'와 일치하는 검사 결과가 없습니다. 다른 키워드로 검색해보세요.",
                                style = MaterialTheme.typography.bodySmall,
                                color = DotSlate,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(aiHistory) { aiRecord ->
                    AiHistoryItem(
                        record = aiRecord,
                        selected = selectedAiRecords.contains(aiRecord.id),
                        onSelect = { viewModel.toggleSelection(aiRecord.id) },
                        onOpenDetail = { viewModel.selectInspectionDetail(aiRecord) }
                    )
                }
            }
        }
    }
}

/**
 * AiHistoryItem: Inspection card item displaying timestamp, summary snippet, checkbox, and PDF share button.
 */
@Composable
fun AiHistoryItem(
    record: AiAnalysisRecord,
    selected: Boolean,
    onSelect: (Boolean) -> Unit,
    onOpenDetail: () -> Unit
) {
    val context = LocalContext.current
    val debouncedShareReport = rememberDebouncedClick {
        PdfReportGenerator.generateAndShareReport(context, record)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onOpenDetail() },
        shape = RoundedCornerShape(0.dp),
        colors = CardDefaults.cardColors(containerColor = DotCardMist)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = selected,
                onCheckedChange = onSelect,
                colors = CheckboxDefaults.colors(
                    checkedColor = DotViolet,
                    checkmarkColor = DotWhite,
                    uncheckedColor = DotTagBorder
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = record.formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = DotSlate,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = record.apiResponse,
                    style = MaterialTheme.typography.bodySmall,
                    color = DotBlack,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = debouncedShareReport
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = "Share PDF Report",
                    tint = DotViolet,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/**
 * StatItem: Compact statistical metric representation (e.g. Cpk, mean, stdDev).
 */
@Composable
fun StatItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.5.sp, letterSpacing = (-0.3).sp),
            color = DotSlate,
            maxLines = 1,
            softWrap = false
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.5.sp),
            color = DotBlack,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * HistoryItem: Single flush measurement record row with PASS/FAIL indicator.
 */
@Composable
fun HistoryItem(record: MeasurementRecord) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        shape = RoundedCornerShape(0.dp),
        color = DotWhite,
        border = BorderStroke(1.dp, DotCardMist)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f, fill = false)) {
                Text(
                    text = record.formattedDate,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = DotSlate,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "단차: ${String.format("%+.3f", record.flushHeight)}mm",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = DotBlack,
                    maxLines = 1,
                    softWrap = false
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                color = if (record.isPass) DotPassGreen else DotFailRed,
                shape = RoundedCornerShape(22.dp)
            ) {
                Text(
                    text = if (record.isPass) "PASS" else "FAIL",
                    color = DotWhite,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
        }
    }
}
