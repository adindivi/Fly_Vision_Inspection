package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.util.AppSettings
import com.example.util.rememberDebouncedClick

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ApiSettingsDialog(
    viewModel: MainViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val currentCustomKey by viewModel.customApiKey.collectAsState()
    val currentModel by viewModel.selectedModel.collectAsState()
    val isTesting by viewModel.isTestingApiKey.collectAsState()
    val testResult by viewModel.apiTestResult.collectAsState()

    var apiKeyText by remember { mutableStateOf(currentCustomKey) }
    var selectedModelText by remember { mutableStateOf(currentModel) }
    var isKeyVisible by remember { mutableStateOf(false) }

    val effectiveKey = if (apiKeyText.isNotBlank()) apiKeyText else viewModel.appSettings.getEffectiveApiKey()
    val isUsingCustom = apiKeyText.isNotBlank()
    val isKeyConfigured = effectiveKey.isNotBlank() && effectiveKey != "MY_GEMINI_API_KEY"

    val debouncedSave = rememberDebouncedClick {
        viewModel.updateApiSettings(apiKeyText.trim(), selectedModelText.trim())
        Toast.makeText(context, "API 설정이 안전하게 저장되었습니다.", Toast.LENGTH_SHORT).show()
        onDismiss()
    }

    val debouncedTest = rememberDebouncedClick {
        viewModel.testApiConnection(apiKeyText.trim(), selectedModelText.trim())
    }

    val debouncedReset = rememberDebouncedClick {
        viewModel.clearCustomApiKey()
        apiKeyText = ""
        selectedModelText = AppSettings.DEFAULT_MODEL
        viewModel.clearApiTestResult()
        Toast.makeText(context, "기본 빌드 키 설정으로 초기화되었습니다.", Toast.LENGTH_SHORT).show()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp)
            ) {
                // Header
                Text(
                    text = "Gemini AI API & 모델 설정",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Current Key Status Card (Rule 2 Anti-Squishing)
                Surface(
                    color = if (isKeyConfigured) Color(0xFF064E3B) else Color(0xFF7F1D1D),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isKeyConfigured) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                            contentDescription = null,
                            tint = if (isKeyConfigured) Color(0xFF34D399) else Color(0xFFF87171),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f, fill = false)) {
                            Text(
                                text = if (!isKeyConfigured) "API 키 미설정"
                                else if (isUsingCustom) "앱 직접 입력 키 적용 중"
                                else ".env 빌드 기본 키 적용 중",
                                color = Color.White,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                softWrap = false
                            )
                            if (isKeyConfigured) {
                                val masked = effectiveKey.take(6) + "••••••••" + effectiveKey.takeLast(4)
                                Text(
                                    text = masked,
                                    color = Color(0xFFE2E8F0),
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // API Key Input
                Text(
                    text = "Gemini API 키 직접 입력",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = apiKeyText,
                    onValueChange = { apiKeyText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("AIzaSy...") },
                    leadingIcon = {
                        Icon(Icons.Outlined.Key, contentDescription = null, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { isKeyVisible = !isKeyVisible }) {
                                Icon(
                                    imageVector = if (isKeyVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = "Toggle Visibility",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            IconButton(onClick = {
                                clipboardManager.getText()?.text?.let { text ->
                                    apiKeyText = text.trim()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentPaste,
                                    contentDescription = "Paste from Clipboard",
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    },
                    visualTransformation = if (isKeyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Model Selection
                Text(
                    text = "검사 AI 모델 선택",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    AppSettings.AVAILABLE_MODELS.forEach { modelName ->
                        FilterChip(
                            selected = selectedModelText == modelName,
                            onClick = { selectedModelText = modelName },
                            label = { Text(modelName, maxLines = 1, softWrap = false) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = selectedModelText,
                    onValueChange = { selectedModelText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("사용할 모델명") },
                    singleLine = true
                )

                // Test Connection Result
                if (testResult != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = testResult ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons (Rule 5: Explicit action labels)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = debouncedTest,
                        modifier = Modifier.weight(1f).height(48.dp),
                        enabled = !isTesting
                    ) {
                        if (isTesting) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text("연결 테스트")
                        }
                    }

                    Button(
                        onClick = debouncedSave,
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("설정 저장")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = debouncedReset) {
                        Text("기본값으로 초기화", color = MaterialTheme.colorScheme.error)
                    }

                    TextButton(onClick = onDismiss) {
                        Text("닫기")
                    }
                }
            }
        }
    }
}
