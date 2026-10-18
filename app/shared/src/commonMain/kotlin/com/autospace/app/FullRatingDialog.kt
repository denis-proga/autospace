package com.autospace.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Полный рейтинг: вкладки «Неделя» и «Всё время», до 50 участников. */
@Composable
internal fun FullRatingDialog(onDismiss: () -> Unit) {
    val strings = LocalStrings.current
    val rs = ratingStrings(strings)

    var scope by remember { mutableStateOf("week") }
    var board by remember { mutableStateOf<LeaderboardDto?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(scope) {
        board = null
        error = null
        try {
            board = ApiClient.getLeaderboard(scope, 50)
        } catch (e: Exception) {
            error = friendlyServerErrorMessage(e, strings)
        }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        val shape = RoundedCornerShape(24.dp)
        Box(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .heightIn(max = 640.dp)
                    .clip(shape)
                    .background(Brush.verticalGradient(listOf(Color(0xFF1A2480), Color(0xFF0C1456))))
                    .border(1.dp, Dash.PanelBorder, shape)
                    .padding(20.dp)
            ) {
                Text(
                    text = strings.dashWeeklyRating,
                    fontFamily = interFontFamily(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.White
                )
                Text(
                    text = rs.pointsHint,
                    fontFamily = interFontFamily(),
                    fontSize = 12.sp,
                    color = Dash.Text2,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScopeTab(text = rs.week, selected = scope == "week") { scope = "week" }
                    ScopeTab(text = rs.allTime, selected = scope == "all") { scope = "all" }
                }
                Spacer(Modifier.height(14.dp))

                val current = board
                when {
                    error != null -> Text(error.orEmpty(), fontFamily = interFontFamily(), fontSize = 14.sp, color = Dash.Red)

                    current == null -> Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 30.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = Brand.Yellow) }

                    current.entries.isEmpty() -> Text(
                        rs.empty,
                        fontFamily = interFontFamily(),
                        fontSize = 14.sp,
                        color = Dash.Text2
                    )

                    else -> Column(
                        modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        current.entries.forEach { entry ->
                            RatingRow(entry)
                        }
                        if (current.myRank > current.entries.size) {
                            Text(
                                text = "${strings.dashYourPlace} ${current.myRank} · ${current.myPoints}",
                                fontFamily = interFontFamily(),
                                fontSize = 13.sp,
                                color = Brand.Yellow,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Brand.Yellow)
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
                            .padding(horizontal = 24.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = strings.testClose,
                            fontFamily = interFontFamily(),
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = Color(0xFF1A1440)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ScopeTab(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(if (selected) Brand.Yellow else Color(0xFF1B2790).copy(alpha = 0.7f))
            .border(1.dp, if (selected) Brand.Yellow else Dash.Ring.copy(alpha = 0.6f), shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            fontFamily = interFontFamily(),
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp,
            color = if (selected) Color(0xFF1A1440) else Color.White
        )
    }
}

@Composable
private fun RatingRow(entry: LeaderboardEntryDto) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                if (entry.isMe) Brand.Yellow.copy(alpha = 0.12f) else Color(0xFF0A0F45).copy(alpha = 0.6f)
            )
            .border(1.dp, if (entry.isMe) Brand.Yellow.copy(alpha = 0.8f) else Dash.PanelBorder, shape)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${entry.rank}",
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = Dash.Text2,
            modifier = Modifier.size(width = 28.dp, height = 18.dp)
        )
        UserAvatar(username = entry.username, avatarId = entry.avatar, size = 30.dp)
        Text(
            text = entry.username,
            fontFamily = interFontFamily(),
            fontWeight = if (entry.isMe) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp,
            color = if (entry.isMe) Brand.Yellow else Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 10.dp)
        )
        Text(
            text = entry.points.toString(),
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = Color.White
        )
    }
}

/** Подписи окна рейтинга. Отдельный класс: в AppStrings лимит полей исчерпан. */
internal class RatingStrings(val week: String, val allTime: String, val pointsHint: String, val empty: String)

private val RatingRu = RatingStrings("Неделя", "Всё время", "Очки — это верные ответы в тестах", "Пока никто не проходил тесты")
private val RatingEn = RatingStrings("This week", "All time", "Points are correct answers in tests", "Nobody has taken a test yet")
private val RatingEs = RatingStrings("Semana", "Histórico", "Los puntos son respuestas correctas en los tests", "Todavía nadie ha hecho ningún test")
private val RatingDe = RatingStrings("Woche", "Gesamt", "Punkte sind richtige Antworten in den Tests", "Noch hat niemand einen Test gemacht")
private val RatingUk = RatingStrings("Тиждень", "Увесь час", "Очки — це правильні відповіді в тестах", "Поки ніхто не проходив тести")

internal fun ratingStrings(strings: AppStrings): RatingStrings = when (strings.botGreeting) {
    EnglishStrings.botGreeting -> RatingEn
    SpanishStrings.botGreeting -> RatingEs
    GermanStrings.botGreeting -> RatingDe
    UkrainianStrings.botGreeting -> RatingUk
    else -> RatingRu
}
