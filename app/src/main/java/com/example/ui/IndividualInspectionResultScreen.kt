package com.example.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.AiAnalysisRecord
import com.example.ui.theme.DotBlack
import com.example.ui.theme.DotCardMist
import com.example.ui.theme.DotFailRed
import com.example.ui.theme.DotHeroCharcoal
import com.example.ui.theme.DotOffWhite
import com.example.ui.theme.DotPassGreen
import com.example.ui.theme.DotSlate
import com.example.ui.theme.DotTagBorder
import com.example.ui.theme.DotViolet
import com.example.ui.theme.DotWhite
import com.example.util.PdfReportGenerator
import java.io.File

/**
 * Full-screen dialog displaying an individual inspection result screen with
 * a prominent 'Share' floating action button that generates and shares a quick PDF summary report.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IndividualInspectionResultDialog(
    record: AiAnalysisRecord,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val context = LocalContext.current
        val isAnomalous = record.apiResponse.contains("anomal", ignoreCase = true) ||
                record.apiResponse.contains("defect", ignoreCase = true) ||
                record.apiResponse.contains("scratch", ignoreCase = true)

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "검사 결과 상세 (Inspection Result)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = DotWhite,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                record.formattedDate,
                                style = MaterialTheme.typography.labelSmall,
                                color = DotSlate,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Close Detail",
                                tint = DotWhite,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = DotHeroCharcoal
                    )
                )
            },
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    onClick = {
                        PdfReportGenerator.generateAndShareReport(context, record)
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share PDF Summary Report",
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    text = {
                        Text(
                            "PDF 보고서 공유",
                            fontWeight = FontWeight.SemiBold
                        )
                    },
                    containerColor = DotViolet,
                    contentColor = DotWhite,
                    shape = RoundedCornerShape(22.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(DotOffWhite)
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // Status & Metadata Card - 42dot Flat Tint Standard (0px Radius, Card Mist #f6f6f9)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DotCardMist),
                    shape = RoundedCornerShape(0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f, fill = false)) {
                                Text(
                                    text = if (record.id > 0) "표본 보고서 #DSP-${record.id}" else "표본 보고서",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = DotBlack,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Drosophila melanogaster",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DotSlate,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = if (isAnomalous) DotFailRed else DotPassGreen,
                                shape = RoundedCornerShape(22.dp)
                            ) {
                                Text(
                                    text = if (isAnomalous) "DEFECT FLAGGED" else "INSPECTION PASS",
                                    color = DotWhite,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    softWrap = false,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "공유 버튼을 눌러 PDF 요약 보고서를 즉시 전송할 수 있습니다.",
                            style = MaterialTheme.typography.labelSmall,
                            color = DotViolet
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Specimen Image Preview
                if (record.imageUri.isNotBlank()) {
                    Text(
                        "표본 캡처 이미지 (Specimen Capture)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = DotBlack
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SpecimenImagePreview(uriString = record.imageUri)
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // AI Detailed Findings Card
                Text(
                    "신경학적 육안 결함 분석 내용 (AI Findings)",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = DotBlack
                )
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(0.dp),
                    colors = CardDefaults.cardColors(containerColor = DotCardMist),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = record.apiResponse,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DotBlack,
                            lineHeight = 22.sp,
                            letterSpacing = (-0.2).sp
                        )
                    }
                }

                // Generous bottom spacer so FAB doesn't block content
                Spacer(modifier = Modifier.height(96.dp))
            }
        }
    }
}

@Composable
private fun SpecimenImagePreview(uriString: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap = remember(uriString) {
        try {
            val options = BitmapFactory.Options().apply {
                inSampleSize = 1
            }
            if (uriString.startsWith("content://")) {
                val uri = Uri.parse(uriString)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream, null, options)
                }
            } else {
                val file = File(uriString)
                if (file.exists()) {
                    BitmapFactory.decodeFile(file.absolutePath, options)
                } else null
            }
        } catch (e: Exception) {
            null
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Drosophila Specimen Image",
            modifier = modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(0.dp)),
            contentScale = ContentScale.Crop
        )
    } else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(120.dp)
                .clip(RoundedCornerShape(0.dp))
                .background(DotCardMist),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "이미지 파일 연결 불가",
                style = MaterialTheme.typography.bodySmall,
                color = DotSlate
            )
        }
    }
}
