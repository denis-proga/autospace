package com.autospace.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke

@Composable
fun StatsScreen(
    isLoading: Boolean,
    results: List<TestResultItemDto>
) {
    val windowSizeClass = LocalWindowSizeClass.current
    val contentModifier = if (windowSizeClass == WindowSizeClass.Compact) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.fillMaxWidth().widthIn(max = 700.dp)
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(modifier = contentModifier.padding(16.dp)) {
            Text(
                text = "Статистика",
                style = MaterialTheme.typography.headlineSmall
            )

            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            if (results.isEmpty()) {
                Text(
                    text = "Вы пока не проходили тесты",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 16.dp)
                )
                return@Column
            }

            val totalTests = results.size
            val averagePercent = results.sumOf { it.correctCount * 100 / it.totalQuestions } / totalTests
            val bestResult = results.maxOf { it.correctCount }

            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard("Пройдено", "$totalTests", Modifier.weight(1f))
                StatCard("Успеваемость", "$averagePercent%", Modifier.weight(1f))
                StatCard("Лучший", "$bestResult", Modifier.weight(1f))
            }

            ProgressChart(
                results = results,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(results) { result ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Тест ${result.testNumber}",
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Text(
                                    text = if (result.mode == "EXAM") "Экзамен" else "Обучение",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }

                            val percent = result.correctCount * 100 / result.totalQuestions
                            Text(
                                text = "${result.correctCount} из ${result.totalQuestions} ($percent%)",
                                color = if (percent >= 90) Color(0xFF4CAF50) else Color(0xFFF44336),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.headlineSmall)
            Text(text = label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun ProgressChart(results: List<TestResultItemDto>, modifier: Modifier = Modifier) {
    // Старые слева, новые справа — список приходит от новых к старым
    val points = results.reversed().map { it.correctCount * 100f / it.totalQuestions }

    if (points.size < 2) return

    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Card(modifier = modifier) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Динамика",
                style = MaterialTheme.typography.titleSmall
            )

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .padding(top = 12.dp)
            ) {
                val stepX = size.width / (points.size - 1)

                // горизонтальные линии сетки: 0, 25, 50, 75, 100%
                for (i in 0..4) {
                    val y = size.height - size.height * (i / 4f)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                }

                val path = Path()
                points.forEachIndexed { index, percent ->
                    val x = stepX * index
                    val y = size.height - size.height * (percent / 100f)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }

                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                points.forEachIndexed { index, percent ->
                    val x = stepX * index
                    val y = size.height - size.height * (percent / 100f)
                    drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(x, y))
                }
            }
        }
    }
}