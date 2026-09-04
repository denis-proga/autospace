package com.autospace.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun MainMenuScreen(
    username: String,
    refreshKey: Int,
    onTestSelected: (TestInfo, TestMode) -> Unit,
    onOpenStats: () -> Unit
) {
    val colors = LocalAppColors.current
    val strings = LocalStrings.current
    val interFamily = interFontFamily()
    val spaceGroteskFamily = spaceGroteskFontFamily()

    var selectedMode by remember { mutableStateOf(TestMode.LEARNING) }
    val tests = remember { generateTestList() }
    val scope = rememberCoroutineScope()

    var results by remember { mutableStateOf<List<TestResultItemDto>>(emptyList()) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var isResetting by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var alreadyCompletedTestNumber by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(username, refreshTrigger, refreshKey) {
        try {
            val response = ApiClient.getResults(username)
            results = response.results
        } catch (e: Exception) {
        }
    }

    val latestByTestMode = remember(results) {
        results.groupBy { it.testNumber to it.mode }
            .mapValues { (_, list) -> list.maxByOrNull { it.completedAt } }
    }

    val windowSizeClass = LocalWindowSizeClass.current
    val contentModifier = if (windowSizeClass == WindowSizeClass.Compact) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.fillMaxWidth().widthIn(max = 900.dp)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = contentModifier.fillMaxHeight().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.mainMenuAppName,
                    fontFamily = spaceGroteskFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 30.sp,
                    color = colors.textPrimary
                )
                Text(
                    text = strings.mainMenuStats,
                    fontFamily = interFamily,
                    color = colors.textSecondary,
                    modifier = Modifier.clickable { onOpenStats() }
                )
            }

            PillToggle(
                options = listOf(TestMode.LEARNING, TestMode.EXAM),
                selected = selectedMode,
                labelFor = { if (it == TestMode.LEARNING) strings.commonLearning else strings.commonExam },
                onSelected = { selectedMode = it },
                modifier = Modifier.padding(top = 20.dp, bottom = 20.dp)
            )

            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 120.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(bottom = 16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(tests) { test ->
                    val latestResult = latestByTestMode[test.number to selectedMode.name]
                    TestCard(
                        testNumber = test.number,
                        result = latestResult,
                        selectedMode = selectedMode,
                        onClick = {
                            if (latestResult != null) {
                                alreadyCompletedTestNumber = test.number
                            } else {
                                onTestSelected(test, selectedMode)
                            }
                        }
                    )
                }
            }

            OutlinedButton(
                onClick = { showResetConfirmation = true },
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(strings.mainMenuResetAll, fontFamily = interFamily, color = colors.textSecondary)
            }
        }
    }

    if (showResetConfirmation) {
        AlertDialog(
            onDismissRequest = { if (!isResetting) showResetConfirmation = false },
            title = { Text(strings.mainMenuResetConfirmTitle) },
            text = { Text(strings.mainMenuResetConfirmText) },
            confirmButton = {
                Button(
                    onClick = {
                        isResetting = true
                        scope.launch {
                            try {
                                ApiClient.resetProgress(ResetProgressRequestDto(username = username))
                                refreshTrigger++
                            } catch (e: Exception) {
                            }
                            isResetting = false
                            showResetConfirmation = false
                        }
                    }
                ) {
                    Text(if (isResetting) strings.mainMenuResetting else strings.mainMenuResetConfirmButton)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showResetConfirmation = false },
                    enabled = !isResetting
                ) {
                    Text(strings.mainMenuCancel)
                }
            }
        )
    }

    if (alreadyCompletedTestNumber != null) {
        AlertDialog(
            onDismissRequest = { alreadyCompletedTestNumber = null },
            title = { Text(strings.mainMenuAlreadyCompletedTitle) },
            text = { Text(strings.mainMenuAlreadyCompletedText("${strings.commonTestWord} $alreadyCompletedTestNumber")) },
            confirmButton = {
                Button(onClick = { alreadyCompletedTestNumber = null }) {
                    Text(strings.mainMenuOk)
                }
            }
        )
    }
}

@Composable
private fun TestCard(
    testNumber: Int,
    result: TestResultItemDto?,
    selectedMode: TestMode,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val interFamily = interFontFamily()
    val strings = LocalStrings.current

    val statusColor = when {
        result == null -> null
        selectedMode == TestMode.EXAM -> colors.examBlue
        result.correctCount >= 27 -> colors.success
        else -> colors.error
    }
    val isPassedLearning = result != null && selectedMode == TestMode.LEARNING && result.correctCount >= 27
    val badgeSymbol = if (selectedMode == TestMode.EXAM || isPassedLearning) "✓" else "✕"

    Box(
        modifier = Modifier
            .height(80.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(statusColor?.copy(alpha = 0.12f) ?: colors.card)
            .border(1.dp, statusColor ?: colors.border, RoundedCornerShape(14.dp))
            .clickable { onClick() }
    ) {
        Text(
            text = "${strings.commonTestWord} $testNumber",
            fontFamily = interFamily,
            color = colors.textPrimary,
            modifier = Modifier.align(Alignment.Center)
        )

        if (statusColor != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(6.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(statusColor),
                contentAlignment = Alignment.Center
            ) {
                Text(text = badgeSymbol, color = Color.White, fontSize = 11.sp)
            }
        }
    }
}