package com.autospace.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import autospace.app.shared.generated.resources.Res
import autospace.app.shared.generated.resources.autospace_logo
import org.jetbrains.compose.resources.painterResource

/** Светло-синий цвет подзаголовков на телефонной картинке. */
private val PhoneSub = Color(0xFF7FB0FF)

/**
 * Главная страница для телефона. Размеры сняты с телефонной картинки-референса:
 * мелкая типографика, плотные отступы, пять плиток в ряд, блоки идут компактными строками.
 */
@Composable
internal fun PhoneDashboard(
    username: String,
    license: LicenseInfoDto?,
    tests: List<TestInfo>,
    mode: TestMode,
    onModeChange: (TestMode) -> Unit,
    stateFor: (TestInfo) -> TileState,
    onTileClick: (TestInfo) -> Unit,
    onResetAll: () -> Unit,
    language: Language,
    onLanguageChange: (Language) -> Unit,
    onLogout: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenProfile: () -> Unit,
    continueProgress: LatestProgressDto?,
    onContinue: (LatestProgressDto) -> Unit,
    onDailyQuestion: () -> Unit,
    board: LeaderboardDto?,
    onOpenFullRating: () -> Unit,
    mistakesCount: Int,
    onMistakes: () -> Unit,
    onOpenStats: () -> Unit,
    onOpenPaywall: () -> Unit,
    toastVisible: Boolean,
    onToastClose: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        PhoneTopBar(username, language, onLanguageChange, onLogout, onOpenSupport, onOpenProfile, onOpenStats)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PhoneGreeting(username, license, onOpenPaywall)
            PhoneStrip()
            if (continueProgress != null) PhoneContinue(continueProgress, onContinue)
            PhoneTests(tests, mode, onModeChange, stateFor, onTileClick, onResetAll)
            if (PreviewData.SHOW) PhoneRows(toastVisible, onToastClose, onDailyQuestion, board, onOpenFullRating, mistakesCount, onMistakes)
            Spacer(Modifier.height(12.dp))
        }
    }
}

// ------------------------------------------------------------------------------------------
// Верх: логотип, язык, пользователь; ниже «Выйти», «Поддержка», «Статистика»
// ------------------------------------------------------------------------------------------

@Composable
private fun PhoneTopBar(
    username: String,
    language: Language,
    onLanguageChange: (Language) -> Unit,
    onLogout: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenStats: () -> Unit
) {
    val strings = LocalStrings.current
    val interFamily = interFontFamily()

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val showName = maxWidth >= 330.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A1148).copy(alpha = 0.92f))
                .padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(Res.drawable.autospace_logo),
                    contentDescription = "AutoSpace",
                    modifier = Modifier.size(28.dp)
                )
                Wordmark(fontSize = 20.sp, modifier = Modifier.padding(start = 6.dp))
                Spacer(Modifier.weight(1f))
                LanguageMenuChip(language, onLanguageChange, dense = true)
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onOpenProfile)
                ) {
                    UserAvatar(username = username, avatarId = MenuAvatar.id, size = 26.dp)
                }
                if (showName) {
                    Text(
                        text = username.replaceFirstChar { it.uppercase() },
                        fontFamily = interFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val pill = RoundedCornerShape(50)
                Row(
                    modifier = Modifier
                        .clip(pill)
                        .background(Color(0xFF1B2A9C).copy(alpha = 0.45f))
                        .border(1.dp, Dash.Ring.copy(alpha = 0.6f), pill)
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onLogout)
                        .padding(horizontal = 11.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LogoutIcon(Color.White, 14.dp)
                    Text(
                        text = strings.commonLogout,
                        fontFamily = interFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.5.sp,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                PhoneLink(strings.dashSupport, onOpenSupport) {
                    Box(
                        modifier = Modifier.size(16.dp).border(1.5.dp, Dash.Blue, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("?", fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 10.sp, lineHeight = 10.sp, color = Dash.Blue)
                    }
                }
                Box(
                    modifier = Modifier.padding(horizontal = 6.dp).width(1.dp).height(14.dp).background(Dash.PanelBorder)
                )
                PhoneLink(strings.mainMenuStats, onOpenStats) { BarsIcon(Dash.Blue, 14.dp) }
            }
        }
    }
}

@Composable
private fun PhoneLink(text: String, onClick: () -> Unit, icon: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Text(
            text = text,
            fontFamily = interFontFamily(),
            fontSize = 11.sp,
            color = Dash.Text2,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(start = 5.dp)
        )
    }
}

// ------------------------------------------------------------------------------------------
// Приветствие, показатели, «Продолжить»
// ------------------------------------------------------------------------------------------

@Composable
private fun PhoneGreeting(username: String, license: LicenseInfoDto?, onOpenPaywall: () -> Unit) {
    val strings = LocalStrings.current
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, Dash.PanelBorder, shape)
    ) {
        SceneStrip(modifier = Modifier.matchParentSize())
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
            AccentTitle(text = strings.dashGreeting(username.replaceFirstChar { it.uppercase() }), fontSize = 20.sp)
            AccessChip(license = license, onClick = onOpenPaywall, dense = true, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

@Composable
private fun PhoneStrip() {
    val strings = LocalStrings.current
    val interFamily = interFontFamily()
    val shape = RoundedCornerShape(16.dp)
    val total = LocalCorrectTotal.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF0A1146).copy(alpha = 0.7f))
            .border(1.dp, Dash.PanelBorder, shape)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        if (PreviewData.SHOW) {
            // верх: звание и уровень с полоской опыта
            Row(verticalAlignment = Alignment.CenterVertically) {
                RankBadgeIcon(size = 40.dp)
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = strings.dashRank,
                            fontFamily = interFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = strings.dashLevel(PreviewData.LEVEL),
                            fontFamily = interFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Brand.Yellow,
                            maxLines = 1
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(top = 6.dp, bottom = 4.dp)
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.12f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(PreviewData.XP.toFloat() / PreviewData.XP_NEXT)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(50))
                                .background(Brush.horizontalGradient(listOf(Color(0xFFFFD84D), Color(0xFFFFA928))))
                        )
                    }
                    Text(
                        text = "${PreviewData.XP} / ${PreviewData.XP_NEXT} XP",
                        fontFamily = interFamily,
                        fontSize = 10.sp,
                        color = Dash.Text2,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
            Box(modifier = Modifier.padding(vertical = 10.dp).fillMaxWidth().height(1.dp).background(Dash.PanelBorder))
        }

        // низ: серия дней и правильные ответы, каждому своя половина
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (PreviewData.SHOW) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    FlameIcon(size = 30.dp)
                    Column(modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = strings.dashStreak(PreviewData.STREAK_DAYS),
                            fontFamily = interFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = Color.White,
                            maxLines = 2
                        )
                        Text(
                            text = strings.dashStreakLabel,
                            fontFamily = interFamily,
                            fontSize = 10.sp,
                            color = Dash.Text2,
                            maxLines = 1
                        )
                    }
                }
                PhoneStripDivider()
            }
            Row(
                modifier = Modifier.weight(1f).padding(start = if (PreviewData.SHOW) 12.dp else 0.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                StatusDot(Dash.Green, check = true, size = 28.dp)
                Column(modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        text = total.toString(),
                        fontFamily = interFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White,
                        maxLines = 1
                    )
                    Text(
                        text = strings.dashCorrectAnswers,
                        fontFamily = interFamily,
                        fontSize = 10.sp,
                        color = Dash.Text2,
                        maxLines = 2
                    )
                }
            }
        }
    }
}

@Composable
private fun PhoneStripDivider() {
    Box(modifier = Modifier.width(1.dp).height(34.dp).background(Dash.PanelBorder))
}

@Composable
private fun PhoneContinue(progress: LatestProgressDto, onContinue: (LatestProgressDto) -> Unit) {
    val strings = LocalStrings.current
    val interFamily = interFontFamily()
    val shape = RoundedCornerShape(16.dp)
    Box(modifier = Modifier.fillMaxWidth().clip(shape).border(1.dp, Dash.PanelBorder, shape)) {
        SceneStrip(modifier = Modifier.matchParentSize())
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(32.dp).clip(RoundedCornerShape(9.dp)).background(Dash.Blue.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) { DocIcon(Color(0xFF7FB0FF), 18.dp, filled = true) }
            Column(modifier = Modifier.padding(start = 9.dp).weight(1f)) {
                Text(strings.dashContinue + ":", fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White)
                Text(
                    strings.dashContinueSubtitle(progress.testNumber, progress.currentIndex + 1, 30),
                    fontFamily = interFamily, fontSize = 10.sp, color = PhoneSub, maxLines = 2
                )
            }
            PanelButton(strings.dashContinue, onClick = { onContinue(progress) }, yellow = true, dense = true)
        }
    }
}

// ------------------------------------------------------------------------------------------
// Тесты
// ------------------------------------------------------------------------------------------

@Composable
private fun PhoneTests(
    tests: List<TestInfo>,
    mode: TestMode,
    onModeChange: (TestMode) -> Unit,
    stateFor: (TestInfo) -> TileState,
    onTileClick: (TestInfo) -> Unit,
    onResetAll: () -> Unit
) {
    val strings = LocalStrings.current
    val interFamily = interFontFamily()

    Column(modifier = Modifier.fillMaxWidth().padding(top = 2.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = strings.navTests,
                fontFamily = interFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            ModeToggle(
                options = listOf(TestMode.LEARNING, TestMode.EXAM),
                selected = mode,
                labelFor = { if (it == TestMode.LEARNING) strings.commonLearning else strings.commonExam },
                onSelected = onModeChange,
                dense = true
            )
        }
        Spacer(Modifier.height(8.dp))
        AdaptiveGrid(
            items = tests,
            minTile = 60.dp,
            maxColumns = 5,
            gap = 5.dp,
            visibleRows = 4,
            rowHeight = 48.dp
        ) { test, modifier ->
            TestTile(
                number = test.number,
                label = "${strings.commonTestWord} ${test.number}",
                state = stateFor(test),
                onClick = { onTileClick(test) },
                modifier = modifier,
                dense = true
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onResetAll)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            RefreshIcon(Color.White.copy(alpha = 0.85f), 13.dp)
            Text(
                text = strings.mainMenuResetAll,
                fontFamily = interFamily,
                fontSize = 10.5.sp,
                color = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

// ------------------------------------------------------------------------------------------
// Строки: вопрос дня, рейтинг, ошибки, достижения, новое достижение
// ------------------------------------------------------------------------------------------

@Composable
private fun PhoneRows(toastVisible: Boolean, onToastClose: () -> Unit, onDailyQuestion: () -> Unit, board: LeaderboardDto?, onOpenFullRating: () -> Unit, mistakesCount: Int, onMistakes: () -> Unit) {
    val strings = LocalStrings.current
    val interFamily = interFontFamily()

    Column(modifier = Modifier.fillMaxWidth()) {
        PhoneRow(icon = { BulbIcon(22.dp) }, title = strings.dashQuestionOfDay, subtitle = strings.dashQuestionOfDaySub) {
            PanelButton(strings.dashAnswer, onClick = onDailyQuestion, dense = true)
        }

        if (board != null && board.entries.isNotEmpty()) PhoneRow(icon = { TrophyIcon(22.dp) }, title = strings.dashWeeklyRating, subtitle = null, onClick = onOpenFullRating, extra = {
            Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                (board?.entries ?: emptyList()).forEach { entry ->
                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        PhonePlace(entry.rank)
                        Text(
                            "${entry.username} ${entry.points}",
                            fontFamily = interFamily,
                            fontSize = 9.5.sp,
                            color = Color.White,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(start = 3.dp)
                        )
                    }
                }
            }
            Row(modifier = Modifier.padding(top = 3.dp)) {
                Text(strings.dashYourPlace, fontFamily = interFamily, fontSize = 10.sp, color = Color.White.copy(alpha = 0.85f))
                Text(
                    if ((board?.myRank ?: 0) > 0) " ${board?.myRank}" else " —",
                    fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Brand.Yellow
                )
            }
        }) {
            ChevronIcon(color = Dash.Blue, modifier = Modifier.size(14.dp))
        }

        if (mistakesCount > 0) PhoneRow(icon = { TargetIcon(22.dp) }, title = strings.dashMistakes, subtitle = strings.dashMistakesCount(mistakesCount)) {
            PanelButton(strings.dashRepeat, onClick = onMistakes, dense = true)
        }

        PhoneRow(icon = { MedalIcon(22.dp) }, title = strings.dashAchievements, subtitle = null, extra = {
            Row(modifier = Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                PhoneBadge(Brand.Yellow) { StarIcon(Brand.Yellow, 12.dp, filled = true) }
                PhoneBadge(Dash.Blue) { TrophyIcon(12.dp) }
                PhoneBadge(Color(0xFFFF7A59)) { FlameIcon(12.dp) }
            }
        }) {
            Text(strings.dashAllAchievements, fontFamily = interFamily, fontSize = 10.sp, color = Dash.Blue, maxLines = 1)
        }

        if (toastVisible) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onToastClose)
                    .padding(vertical = 10.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(26.dp).clip(CircleShape).background(Brand.Yellow.copy(alpha = 0.16f))
                        .border(1.dp, Brand.Yellow.copy(alpha = 0.5f), CircleShape),
                    contentAlignment = Alignment.Center
                ) { StarIcon(Brand.Yellow, 14.dp, filled = true) }
                Column(modifier = Modifier.padding(start = 9.dp).weight(1f)) {
                    Text(strings.dashNewAchievement, fontFamily = interFamily, fontSize = 9.5.sp, color = Dash.Text2)
                    Text(strings.dashAchievementName, fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color.White)
                }
                ChevronIcon(color = Dash.Text2, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
private fun PhoneRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String?,
    extra: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    val interFamily = interFontFamily()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
                } else Modifier
            )
            .padding(vertical = 8.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(28.dp), contentAlignment = Alignment.Center) { icon() }
        Column(modifier = Modifier.padding(start = 8.dp).weight(1f)) {
            Text(title, fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = Color.White, maxLines = 1)
            if (subtitle != null) {
                Text(subtitle, fontFamily = interFamily, fontSize = 10.sp, color = PhoneSub, maxLines = 2)
            }
            if (extra != null) extra()
        }
        Box(modifier = Modifier.padding(start = 8.dp)) { trailing() }
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Dash.PanelBorder.copy(alpha = 0.5f)))
}

@Composable
private fun PhonePlace(place: Int) {
    val color = when (place) {
        1 -> Color(0xFFFFB92E)
        2 -> Color(0xFFB8C2E8)
        else -> Color(0xFFD98B52)
    }
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.16f))
            .border(1.5.dp, color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = place.toString(),
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            lineHeight = 10.sp,
            color = color
        )
    }
}

@Composable
private fun PhoneBadge(ring: Color, icon: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(22.dp).clip(CircleShape).background(ring.copy(alpha = 0.18f)).border(1.5.dp, ring, CircleShape),
        contentAlignment = Alignment.Center
    ) { icon() }
}
