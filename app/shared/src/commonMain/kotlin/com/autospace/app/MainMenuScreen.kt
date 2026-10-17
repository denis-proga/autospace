package com.autospace.app

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import autospace.app.shared.generated.resources.Res
import autospace.app.shared.generated.resources.autospace_logo
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource

internal val LocalCorrectTotal = androidx.compose.runtime.compositionLocalOf { 0 }

/** Ширина, на которой страница выглядит так, как на картинке. Если окно уже, всё уменьшается пропорционально. */
private const val REFERENCE_WIDTH_DP = 340f

/**
 * Последний выбранный режим (Обучение или Экзамен). Живёт, пока приложение открыто, а не пока открыта главная
 * страница, поэтому после теста главная остаётся в том же режиме, пока человек сам его не сменит.
 */
internal object MenuModeMemory {
    var mode by mutableStateOf(TestMode.LEARNING)
}

/**
 * Главная страница. Для окон уже опорной ширины масштабируются сразу все размеры (и dp, и sp):
 * страница остаётся такой же, как на картинке, только пропорционально меньше, а не разваливается на блоки.
 */
@Composable
fun MainMenuScreen(
    username: String,
    refreshKey: Int,
    onTestSelected: (TestInfo, TestMode) -> Unit,
    onOpenStats: () -> Unit,
    onOpenPaywall: () -> Unit,
    language: Language,
    onLanguageChange: (Language) -> Unit,
    onLogout: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenProfile: () -> Unit
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val base = LocalDensity.current
        val scale = (maxWidth.value / REFERENCE_WIDTH_DP).coerceIn(0.8f, 1f)
        androidx.compose.runtime.CompositionLocalProvider(
            LocalDensity provides androidx.compose.ui.unit.Density(base.density * scale, base.fontScale)
        ) {
            // Material задаёт тексту межстрочный интервал 24sp и разрядку букв: из-за этого мелкие подписи
            // становились выше плиток и обрезались. На этой странице берём естественный интервал шрифта.
            androidx.compose.material3.ProvideTextStyle(
                androidx.compose.material3.LocalTextStyle.current.copy(
                    lineHeight = androidx.compose.ui.unit.TextUnit.Unspecified,
                    letterSpacing = androidx.compose.ui.unit.TextUnit.Unspecified
                )
            ) {
                MainMenuContent(
                    username, refreshKey, onTestSelected, onOpenStats, onOpenPaywall,
                    language, onLanguageChange, onLogout, onOpenSupport, onOpenProfile
                )
            }
        }
    }
}

@Composable
private fun MainMenuContent(
    username: String,
    refreshKey: Int,
    onTestSelected: (TestInfo, TestMode) -> Unit,
    onOpenStats: () -> Unit,
    onOpenPaywall: () -> Unit,
    language: Language,
    onLanguageChange: (Language) -> Unit,
    onLogout: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenProfile: () -> Unit
) {
    val strings = LocalStrings.current
    val interFamily = interFontFamily()

    // режим хранится вне экрана, поэтому возврат с теста не сбрасывает выбранный Экзамен
    val selectedMode = MenuModeMemory.mode
    val tests = remember { generateTestList() }
    val scope = rememberCoroutineScope()

    var results by remember { mutableStateOf<List<TestResultItemDto>>(emptyList()) }
    var showResetConfirmation by remember { mutableStateOf(false) }
    var isResetting by remember { mutableStateOf(false) }
    var refreshTrigger by remember { mutableStateOf(0) }
    var alreadyCompletedTestNumber by remember { mutableStateOf<Int?>(null) }
    var license by remember { mutableStateOf<LicenseInfoDto?>(null) }
    var toastVisible by remember { mutableStateOf(PreviewData.SHOW) }

    LaunchedEffect(username, refreshTrigger, refreshKey) {
        try {
            results = ApiClient.getResults(username).results
        } catch (e: Exception) {
        }
        try {
            license = ApiClient.getLicense()
        } catch (e: Exception) {
        }
    }

    val latestByTestMode = remember(results) {
        results.groupBy { it.testNumber to it.mode }
            .mapValues { (_, list) -> list.maxByOrNull { it.completedAt } }
    }
    // сколько всего правильных ответов (по последнему результату каждого теста и режима)
    val correctTotal = remember(latestByTestMode) { latestByTestMode.values.sumOf { it?.correctCount ?: 0 } }

    fun onTileClick(test: TestInfo) {
        val latest = latestByTestMode[test.number to selectedMode.name]
        val lic = license
        val demoExhausted = lic != null && lic.status == "DEMO" &&
                test.number !in lic.demoTests && lic.demoTests.size >= lic.demoLimit
        val expiredLocked = lic != null && lic.status == "EXPIRED" && test.number !in lic.demoTests
        when {
            latest != null -> alreadyCompletedTestNumber = test.number
            demoExhausted || expiredLocked -> onOpenPaywall()
            else -> onTestSelected(test, selectedMode)
        }
    }

    androidx.compose.runtime.CompositionLocalProvider(LocalCorrectTotal provides correctTotal) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val layout = when {
            maxWidth >= 1320.dp -> DashLayout.Wide      // боковое меню, центр и правая колонка
            maxWidth >= 1000.dp -> DashLayout.Large     // центр и правая колонка
            maxWidth >= 640.dp -> DashLayout.Medium     // планшет: одна колонка, карточки внизу в два ряда
            else -> DashLayout.Compact                  // телефон
        }

        DashboardBackground()

        if (layout == DashLayout.Compact) {
            PhoneDashboard(
                username = username,
                license = license,
                tests = tests,
                mode = selectedMode,
                onModeChange = { MenuModeMemory.mode = it },
                stateFor = { test -> tileStateFor(test.number, selectedMode, latestByTestMode[test.number to selectedMode.name], license) },
                onTileClick = { onTileClick(it) },
                onResetAll = { showResetConfirmation = true },
                language = language,
                onLanguageChange = onLanguageChange,
                onLogout = onLogout,
                onOpenSupport = onOpenSupport,
                onOpenStats = onOpenStats,
                onOpenPaywall = onOpenPaywall,
                toastVisible = toastVisible,
                onToastClose = { toastVisible = false }
            )
        } else {
        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                layout = layout,
                username = username,
                language = language,
                onLanguageChange = onLanguageChange,
                onLogout = onLogout,
                onOpenSupport = onOpenSupport,
                onOpenProfile = onOpenProfile,
                onOpenStats = onOpenStats
            )

            val centerContent: @Composable () -> Unit = {
                GreetingBlock(layout, username, license, correctTotal, onOpenPaywall)
                Spacer(Modifier.height(if (layout == DashLayout.Compact) 14.dp else 20.dp))
                TestsPanel(
                    layout = layout,
                    tests = tests,
                    mode = selectedMode,
                    onModeChange = { MenuModeMemory.mode = it },
                    stateFor = { test -> tileStateFor(test.number, selectedMode, latestByTestMode[test.number to selectedMode.name], license) },
                    onTileClick = { onTileClick(it) },
                    onResetAll = { showResetConfirmation = true }
                )
            }

            when (layout) {
                DashLayout.Wide -> Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    Sidebar()
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) { centerContent() }
                        Column(modifier = Modifier.width(372.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            ContinuePanel()
                            QuestionPanel()
                            RatingPanel()
                            MistakesPanel()
                        }
                    }
                }

                DashLayout.Large -> Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) { centerContent() }
                    Column(modifier = Modifier.width(340.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        ContinuePanel()
                        QuestionPanel()
                        RatingPanel()
                        MistakesPanel()
                    }
                }

                DashLayout.Medium -> Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 20.dp)
                ) {
                    centerContent()
                    if (PreviewData.SHOW) {
                        Spacer(Modifier.height(20.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                ContinuePanel()
                                QuestionPanel()
                            }
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                RatingPanel()
                                MistakesPanel()
                            }
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }

                DashLayout.Compact -> Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    centerContent()
                    if (PreviewData.SHOW) {
                        Spacer(Modifier.height(10.dp))
                        SideCards()
                        if (toastVisible) {
                            Spacer(Modifier.height(14.dp))
                            AchievementToast(onClose = { toastVisible = false })
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
        }

        if (layout != DashLayout.Compact && PreviewData.SHOW && toastVisible) {
            AchievementToast(
                onClose = { toastVisible = false },
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp).widthIn(max = 340.dp)
            )
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
                OutlinedButton(onClick = { showResetConfirmation = false }, enabled = !isResetting) {
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
                Button(onClick = { alreadyCompletedTestNumber = null }) { Text(strings.mainMenuOk) }
            }
        )
    }
}

private fun tileStateFor(
    testNumber: Int,
    mode: TestMode,
    result: TestResultItemDto?,
    license: LicenseInfoDto?
): TileState {
    if (result != null) {
        return when {
            mode == TestMode.EXAM -> TileState.ExamDone
            result.correctCount >= 27 -> TileState.Passed
            else -> TileState.Failed
        }
    }
    if (license != null && license.status != "ACTIVE" && license.status != "FRIEND") {
        val held = testNumber in license.demoTests
        val canClaim = license.status == "DEMO" && license.demoTests.size < license.demoLimit
        if (!held && !canClaim) return TileState.Locked
    }
    return TileState.NotStarted
}

// ------------------------------------------------------------------------------------------
// Верхняя панель, меню языка, боковое меню
// ------------------------------------------------------------------------------------------

@Composable
private fun TopBar(
    layout: DashLayout,
    username: String,
    language: Language,
    onLanguageChange: (Language) -> Unit,
    onLogout: () -> Unit,
    onOpenSupport: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenStats: () -> Unit
) {
    val strings = LocalStrings.current
    val singleRow = layout == DashLayout.Wide || layout == DashLayout.Large

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val showName = maxWidth >= 400.dp
        val narrow = maxWidth < 360.dp

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF0A1148).copy(alpha = 0.92f))
                .padding(horizontal = if (layout == DashLayout.Compact) 14.dp else 20.dp, vertical = 10.dp)
        ) {
            if (layout == DashLayout.Wide) {
                // слева «Выйти» над боковым меню и название ровно там, где начинается левый край панелей;
                // по центру «Поддержка, Статистика, Язык»; справа аккаунт
                Box(modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Row(modifier = Modifier.align(Alignment.CenterStart), verticalAlignment = Alignment.CenterVertically) {
                        // ширина боковой панели 196dp минус внешний отступ 20dp: название начнётся на 216dp
                        Box(modifier = Modifier.width(196.dp)) {
                            LogoutPill(strings.commonLogout, onLogout, modifier = Modifier.widthIn(max = 176.dp))
                        }
                        BrandMark(small = false)
                    }
                    Row(modifier = Modifier.align(Alignment.Center), verticalAlignment = Alignment.CenterVertically) {
                        HeaderLink(text = strings.dashSupport, onClick = onOpenSupport, icon = { HelpCircle() })
                        Spacer(Modifier.width(28.dp))
                        HeaderLink(text = strings.mainMenuStats, onClick = onOpenStats, icon = { BarsIcon(Dash.Blue) })
                        Spacer(Modifier.width(28.dp))
                        LanguageMenuChip(language, onLanguageChange)
                    }
                    Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                        UserChip(username, showName = true, onClick = onOpenProfile)
                    }
                }
            } else if (singleRow) {
                Row(modifier = Modifier.height(52.dp), verticalAlignment = Alignment.CenterVertically) {
                    LogoutPill(strings.commonLogout, onLogout)
                    Spacer(Modifier.width(24.dp))
                    BrandMark(small = false)
                    Spacer(Modifier.weight(1f))
                    HeaderLink(text = strings.dashSupport, onClick = onOpenSupport, icon = { HelpCircle() })
                    Spacer(Modifier.width(18.dp))
                    HeaderLink(text = strings.mainMenuStats, onClick = onOpenStats, icon = { BarsIcon(Dash.Blue) })
                    Spacer(Modifier.width(18.dp))
                    LanguageMenuChip(language, onLanguageChange)
                    Spacer(Modifier.width(18.dp))
                    UserChip(username, showName = true, onClick = onOpenProfile)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BrandMark(small = narrow)
                    Spacer(Modifier.weight(1f))
                    LanguageMenuChip(language, onLanguageChange)
                    Spacer(Modifier.width(10.dp))
                    UserChip(username, showName = showName, onClick = onOpenProfile)
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LogoutPill(strings.commonLogout, onLogout, modifier = Modifier.weight(1.5f, fill = false), tight = true)
                    Spacer(Modifier.width(8.dp))
                    HeaderLink(
                        text = strings.dashSupport,
                        onClick = onOpenSupport,
                        icon = { HelpCircle() },
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Spacer(Modifier.width(4.dp))
                    HeaderLink(
                        text = strings.mainMenuStats,
                        onClick = onOpenStats,
                        icon = { BarsIcon(Dash.Blue) },
                        modifier = Modifier.weight(1.1f, fill = false)
                    )
                }
            }
        }
    }
}

@Composable
private fun BrandMark(small: Boolean) {
    Wordmark(fontSize = if (small) 22.sp else 26.sp)
}

@Composable
private fun LogoutPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, tight: Boolean = false) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF1B2A9C).copy(alpha = 0.45f))
            .border(1.dp, Dash.Ring.copy(alpha = 0.6f), shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = if (tight) 14.dp else 18.dp, vertical = if (tight) 9.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LogoutIcon(Color.White, 20.dp)
        Text(
            text = text,
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.SemiBold,
            fontSize = if (tight) 14.sp else 15.sp,
            color = Color.White,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun HelpCircle() {
    Box(
        modifier = Modifier.size(22.dp).border(1.5.dp, Dash.Blue, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text("?", fontFamily = interFontFamily(), fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Dash.Blue)
    }
}

@Composable
private fun HeaderLink(text: String, onClick: () -> Unit, icon: @Composable () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon()
        Text(
            text = text,
            fontFamily = interFontFamily(),
            fontSize = 14.sp,
            color = Dash.Text2,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun UserChip(username: String, showName: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AvatarCircle(initial = username, size = 40.dp)
        if (showName) {
            Text(
                text = username.replaceFirstChar { it.uppercase() },
                fontFamily = interFontFamily(),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color.White,
                maxLines = 1,
                modifier = Modifier.padding(start = 10.dp)
            )
        }
    }
}

/** Чип языка со списком: язык можно сменить прямо на главной. */
@Composable
internal fun LanguageMenuChip(language: Language, onChange: (Language) -> Unit, dense: Boolean = false) {
    var open by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val shape = RoundedCornerShape(50)

    Box {
        Row(
            modifier = Modifier
                .clip(shape)
                .background(Color(0xFF1B2A9C).copy(alpha = 0.45f))
                .border(1.dp, Dash.Ring.copy(alpha = 0.6f), shape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { open = !open }
                .padding(horizontal = if (dense) 8.dp else 14.dp, vertical = if (dense) 4.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlagBadge(language = language, diameter = if (dense) 15.dp else 22.dp)
            Text(
                text = language.code.uppercase(),
                fontFamily = interFontFamily(),
                fontWeight = FontWeight.SemiBold,
                fontSize = if (dense) 11.sp else 14.sp,
                color = Color.White,
                modifier = Modifier.padding(horizontal = if (dense) 5.dp else 8.dp)
            )
            ChevronDownIcon(Color.White, if (dense) 10.dp else 14.dp)
        }

        if (open) {
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, with(density) { (if (dense) 30.dp else 46.dp).roundToPx() }),
                onDismissRequest = { open = false }
            ) {
                Column(
                    modifier = Modifier
                        .width(190.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF111A66))
                        .border(1.dp, Dash.PanelBorder, RoundedCornerShape(16.dp))
                        .padding(6.dp)
                ) {
                    Language.entries.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (item == language) Color(0xFF2A3FC7).copy(alpha = 0.45f) else Color.Transparent)
                                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                                    open = false
                                    if (item != language) onChange(item)
                                }
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FlagBadge(language = item, diameter = 24.dp)
                            Text(
                                text = item.displayName,
                                fontFamily = interFontFamily(),
                                fontSize = 15.sp,
                                color = Color.White,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Sidebar() {
    val strings = LocalStrings.current
    Column(
        modifier = Modifier
            .width(196.dp)
            .fillMaxHeight()
            .background(Color(0xFF080E40).copy(alpha = 0.85f))
            .padding(horizontal = 14.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        SidebarItem(strings.navHome, selected = true) { HomeIcon(Color.White) }
        SidebarItem(strings.navTests, selected = false) { DocIcon(Dash.Text2) }
        SidebarItem(strings.navMistakes, selected = false) { RefreshIcon(Dash.Text2) }
        SidebarItem(strings.navAchievements, selected = false) { StarIcon(Dash.Text2) }
    }
}

@Composable
private fun SidebarItem(text: String, selected: Boolean, icon: @Composable () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(shape)
            .background(if (selected) Color(0xFF2A3FC7).copy(alpha = 0.38f) else Color.Transparent)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .padding(start = 0.dp)
                .width(4.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(topEnd = 4.dp, bottomEnd = 4.dp))
                .background(if (selected) Brand.Yellow else Color.Transparent)
        )
        Box(modifier = Modifier.padding(start = 14.dp)) { icon() }
        Text(
            text = text,
            fontFamily = interFontFamily(),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 15.sp,
            color = if (selected) Color.White else Dash.Text2,
            modifier = Modifier.padding(start = 14.dp)
        )
    }
}

// ------------------------------------------------------------------------------------------
// Приветствие, прогресс и тесты
// ------------------------------------------------------------------------------------------

@Composable
private fun GreetingBlock(
    layout: DashLayout,
    username: String,
    license: LicenseInfoDto?,
    correctTotal: Int,
    onOpenPaywall: () -> Unit
) {
    val strings = LocalStrings.current
    val name = username.replaceFirstChar { it.uppercase() }

    if (layout == DashLayout.Compact) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val shape = RoundedCornerShape(22.dp)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .border(1.dp, Dash.PanelBorder, shape)
            ) {
                SceneStrip(modifier = Modifier.matchParentSize())
                Column(modifier = Modifier.padding(18.dp)) {
                    AccentTitle(text = strings.dashGreeting(name), fontSize = 26.sp)
                    AccessChip(license = license, onClick = onOpenPaywall, modifier = Modifier.padding(top = 12.dp))
                }
            }
            ProgressStrip(compact = true, correctTotal = correctTotal)
        }
    } else {
        DashPanel(padding = 22.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AccentTitle(text = strings.dashGreeting(name), fontSize = 32.sp)
                Spacer(Modifier.width(20.dp))
                AccessChip(license = license, onClick = onOpenPaywall)
            }
            Spacer(Modifier.height(18.dp))
            ProgressStrip(compact = false, correctTotal = correctTotal)
        }
    }
}

@Composable
private fun ProgressStrip(compact: Boolean, correctTotal: Int) {
    val shape = RoundedCornerShape(20.dp)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF0A1146).copy(alpha = 0.7f))
            .border(1.dp, Dash.PanelBorder, shape)
    ) {
        val dense = maxWidth < 520.dp   // компактные размеры, как на телефонной картинке
        val grid = maxWidth < 380.dp    // совсем узко: два ряда по два показателя

        when {
            !PreviewData.SHOW -> Box(modifier = Modifier.padding(16.dp)) {
                CorrectCell(Modifier, large = !dense, dense = dense)
            }

            grid -> Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RankCell(Modifier.weight(1f), dense = true)
                    LevelCell(Modifier.weight(1f).padding(start = 12.dp), dense = true)
                }
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Dash.PanelBorder))
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    StreakCell(Modifier.weight(1f), dense = true)
                    CorrectCell(Modifier.weight(1f).padding(start = 12.dp), large = false, dense = true)
                }
            }

            else -> Row(
                modifier = Modifier.padding(horizontal = if (dense) 12.dp else 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RankCell(Modifier.weight(1f), dense)
                StripDivider()
                LevelCell(Modifier.weight(if (dense) 1.5f else 1.4f).padding(horizontal = if (dense) 8.dp else 16.dp), dense)
                StripDivider()
                StreakCell(Modifier.weight(if (dense) 1.4f else 1.2f).padding(horizontal = if (dense) 8.dp else 14.dp), dense)
                StripDivider()
                CorrectCell(Modifier.weight(if (dense) 1.25f else 1.1f).padding(start = if (dense) 8.dp else 14.dp), large = !dense, dense = dense)
            }
        }
    }
}

@Composable
private fun RankCell(modifier: Modifier, dense: Boolean) {
    val strings = LocalStrings.current
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        RankBadgeIcon(size = if (dense) 38.dp else 48.dp)
        Text(
            text = strings.dashRank,
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.SemiBold,
            fontSize = if (dense) 13.sp else 16.sp,
            color = Color.White,
            maxLines = 1,
            modifier = Modifier.padding(start = if (dense) 6.dp else 10.dp)
        )
    }
}

@Composable
private fun LevelCell(modifier: Modifier, dense: Boolean) {
    val strings = LocalStrings.current
    val interFamily = interFontFamily()
    Column(modifier = modifier) {
        Text(
            text = strings.dashLevel(PreviewData.LEVEL),
            fontFamily = interFamily,
            fontWeight = FontWeight.Bold,
            fontSize = if (dense) 14.sp else 16.sp,
            color = Color.White,
            maxLines = 1
        )
        Box(
            modifier = Modifier
                .padding(top = 6.dp, bottom = 5.dp)
                .fillMaxWidth()
                .height(7.dp)
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
            fontSize = if (dense) 11.sp else 12.sp,
            color = Dash.Text2,
            maxLines = 1
        )
    }
}

@Composable
private fun StreakCell(modifier: Modifier, dense: Boolean) {
    val strings = LocalStrings.current
    val interFamily = interFontFamily()
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        FlameIcon(size = if (dense) 28.dp else 36.dp)
        Column(modifier = Modifier.padding(start = if (dense) 6.dp else 8.dp)) {
            Text(
                text = strings.dashStreak(PreviewData.STREAK_DAYS),
                fontFamily = interFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (dense) 13.sp else 14.sp,
                color = Color.White,
                maxLines = 2
            )
            Text(
                text = strings.dashStreakLabel,
                fontFamily = interFamily,
                fontSize = if (dense) 10.sp else 11.sp,
                color = Dash.Text2,
                maxLines = 1
            )
        }
    }
}

/** Правильные ответы: настоящие данные. */
@Composable
private fun CorrectCell(modifier: Modifier, large: Boolean, dense: Boolean) {
    val strings = LocalStrings.current
    val interFamily = interFontFamily()
    val total = LocalCorrectTotal.current
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        StatusDot(Dash.Green, check = true, size = if (large) 44.dp else if (dense) 30.dp else 38.dp)
        Column(modifier = Modifier.padding(start = if (dense) 6.dp else 10.dp)) {
            Text(
                text = total.toString(),
                fontFamily = interFamily,
                fontWeight = FontWeight.Bold,
                fontSize = if (dense) 16.sp else 18.sp,
                color = Color.White,
                maxLines = 1
            )
            Text(
                text = strings.dashCorrectAnswers,
                fontFamily = interFamily,
                fontSize = if (dense) 10.sp else 11.sp,
                color = Dash.Text2,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun StripDivider() {
    Box(
        modifier = Modifier
            .padding(horizontal = 2.dp)
            .width(1.dp)
            .height(44.dp)
            .background(Dash.PanelBorder)
    )
}

@Composable
private fun TestsPanel(
    layout: DashLayout,
    tests: List<TestInfo>,
    mode: TestMode,
    onModeChange: (TestMode) -> Unit,
    stateFor: (TestInfo) -> TileState,
    onTileClick: (TestInfo) -> Unit,
    onResetAll: () -> Unit
) {
    val strings = LocalStrings.current
    val compact = layout == DashLayout.Compact

    val content: @Composable () -> Unit = {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = strings.navTests,
                fontFamily = interFontFamily(),
                fontWeight = FontWeight.Bold,
                fontSize = if (compact) 24.sp else 28.sp,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            ModeToggle(
                options = listOf(TestMode.LEARNING, TestMode.EXAM),
                selected = mode,
                labelFor = { if (it == TestMode.LEARNING) strings.commonLearning else strings.commonExam },
                onSelected = onModeChange
            )
        }
        Spacer(Modifier.height(16.dp))
        AdaptiveGrid(
            items = tests,
            minTile = if (compact) 66.dp else 78.dp,
            maxColumns = 10,
            gap = if (compact) 8.dp else 10.dp,
            // на планшете и десктопе показываем все тесты; контейнер с прокруткой только у телефонной раскладки
            visibleRows = if (compact) 3 else Int.MAX_VALUE
        ) { test, modifier ->
            TestTile(
                number = test.number,
                label = "${strings.commonTestWord} ${test.number}",
                state = stateFor(test),
                onClick = { onTileClick(test) },
                modifier = modifier
            )
        }
        Spacer(Modifier.height(16.dp))
        ResetAllButton(text = strings.mainMenuResetAll, onClick = onResetAll)
    }

    if (compact) {
        Column(modifier = Modifier.fillMaxWidth()) { content() }
    } else {
        DashPanel(padding = 22.dp) { content() }
    }
}

@Composable
private fun ResetAllButton(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(shape)
            .background(Color(0xFF1B2A9C).copy(alpha = 0.35f))
            .border(1.dp, Dash.PanelBorder, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        RefreshIcon(Color.White, 20.dp)
        Text(
            text = text,
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = Color.White,
            modifier = Modifier.padding(start = 10.dp)
        )
    }
}

// ------------------------------------------------------------------------------------------
// Боковые карточки (пока с примерными данными) и всплывающее достижение
// ------------------------------------------------------------------------------------------

@Composable
private fun ContinuePanel() {
    if (!PreviewData.SHOW) return
    val strings = LocalStrings.current
    val interFamily = interFontFamily()
    DashPanel(padding = 0.dp) {
        Box(modifier = Modifier.fillMaxWidth().height(150.dp)) {
            SceneStrip(modifier = Modifier.fillMaxSize())
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(46.dp).clip(RoundedCornerShape(12.dp)).background(Dash.Blue.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) { DocIcon(Color(0xFF7FB0FF), 28.dp, filled = true) }
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Text(strings.dashContinue, fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                        Text(
                            strings.dashContinueSubtitle(PreviewData.CONTINUE_TEST, PreviewData.CONTINUE_QUESTION, 30),
                            fontFamily = interFamily, fontSize = 13.sp, color = Dash.Text2, maxLines = 2
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                ContinueButton(strings.dashContinue, onClick = { })
            }
        }
    }
}

@Composable
private fun QuestionPanel() {
    if (!PreviewData.SHOW) return
    val strings = LocalStrings.current
    val interFamily = interFontFamily()
    DashPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BulbIcon(40.dp)
            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                Text(strings.dashQuestionOfDay, fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                Text(strings.dashQuestionOfDaySub, fontFamily = interFamily, fontSize = 13.sp, color = Dash.Text2)
            }
            TargetIcon(54.dp, color = Dash.Ring.copy(alpha = 0.6f))
        }
        PanelButton(strings.dashAnswer, onClick = { }, modifier = Modifier.padding(top = 14.dp).fillMaxWidth(0.82f))
    }
}

@Composable
private fun RatingPanel() {
    if (!PreviewData.SHOW) return
    val strings = LocalStrings.current
    val interFamily = interFontFamily()
    DashPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TrophyIcon(34.dp)
            Text(strings.dashWeeklyRating, fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White, modifier = Modifier.padding(start = 14.dp))
        }
        Column(modifier = Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            PreviewData.TOP.forEach { (name, score, place) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PlaceDot(place)
                    Box(modifier = Modifier.padding(start = 10.dp)) { AvatarCircle(initial = name, size = 28.dp) }
                    Text(name, fontFamily = interFamily, fontSize = 14.sp, color = Color.White, maxLines = 1, modifier = Modifier.padding(start = 10.dp).weight(1f))
                    Text(score.toString(), fontFamily = interFamily, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color.White)
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(strings.dashYourPlace, fontFamily = interFamily, fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))
            Text(" ${PreviewData.MY_PLACE}", fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brand.Yellow)
            Spacer(Modifier.weight(1f))
            Text(strings.dashFullRating, fontFamily = interFamily, fontSize = 13.sp, color = Dash.Blue, maxLines = 1)
            ChevronIcon(color = Dash.Blue, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun MistakesPanel() {
    if (!PreviewData.SHOW) return
    val strings = LocalStrings.current
    val interFamily = interFontFamily()
    DashPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            RefreshIcon(Color(0xFFFFB92E), 36.dp)
            Column(modifier = Modifier.padding(start = 14.dp).weight(1f)) {
                Text(strings.dashMistakes, fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                Text(strings.dashMistakesCount(PreviewData.MISTAKES), fontFamily = interFamily, fontSize = 13.sp, color = Dash.Text2)
            }
            DocIcon(Dash.Blue.copy(alpha = 0.7f), 52.dp, filled = true)
        }
        PanelButton(strings.dashRepeat, onClick = { }, modifier = Modifier.padding(top = 14.dp).fillMaxWidth(0.6f))
    }
}

/** Телефон: те же блоки в виде компактных строк. */
@Composable
private fun SideCards() {
    if (!PreviewData.SHOW) return
    val strings = LocalStrings.current
    val interFamily = interFontFamily()

    DashPanel(padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(Dash.Blue.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) { DocIcon(Color(0xFF7FB0FF), 26.dp, filled = true) }
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(strings.dashContinue + ":", fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                Text(
                    strings.dashContinueSubtitle(PreviewData.CONTINUE_TEST, PreviewData.CONTINUE_QUESTION, 30),
                    fontFamily = interFamily, fontSize = 13.sp, color = Dash.Text2
                )
            }
            PanelButton(strings.dashContinue, onClick = { }, yellow = true)
        }
    }
    CompactRow(icon = { BulbIcon(30.dp) }, title = strings.dashQuestionOfDay, subtitle = strings.dashQuestionOfDaySub) {
        PanelButton(strings.dashAnswer, onClick = { }, modifier = Modifier.height(38.dp))
    }
    CompactRow(icon = { TrophyIcon(30.dp) }, title = strings.dashWeeklyRating, subtitle = null, extra = {
        // три участника делят ширину поровну, чтобы имена не рвались по буквам
        Row(modifier = Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            PreviewData.TOP.forEach { (name, score, place) ->
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                    PlaceDot(place, small = true)
                    Text(
                        "$name $score",
                        fontFamily = interFamily,
                        fontSize = 10.sp,
                        color = Color.White,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
        Row(modifier = Modifier.padding(top = 6.dp)) {
            Text(strings.dashYourPlace, fontFamily = interFamily, fontSize = 12.sp, color = Color.White.copy(alpha = 0.85f))
            Text(" ${PreviewData.MY_PLACE}", fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Brand.Yellow)
        }
    }) {
        ChevronIcon(color = Dash.Blue, modifier = Modifier.size(20.dp))
    }
    CompactRow(icon = { TargetIcon(30.dp) }, title = strings.dashMistakes, subtitle = strings.dashMistakesCount(PreviewData.MISTAKES)) {
        PanelButton(strings.dashRepeat, onClick = { }, modifier = Modifier.height(38.dp))
    }
    CompactRow(icon = { MedalIcon(30.dp) }, title = strings.dashAchievements, subtitle = null, extra = {
        Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BadgeCircle(Brand.Yellow) { StarIcon(Brand.Yellow, 18.dp, filled = true) }
            BadgeCircle(Dash.Blue) { TrophyIcon(18.dp) }
            BadgeCircle(Color(0xFFFF7A59)) { FlameIcon(18.dp) }
        }
    }) {
        Text(strings.dashAllAchievements, fontFamily = interFamily, fontSize = 12.sp, color = Dash.Blue, maxLines = 1)
    }
}

@Composable
private fun CompactRow(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String?,
    extra: (@Composable () -> Unit)? = null,
    trailing: @Composable () -> Unit
) {
    val interFamily = interFontFamily()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.width(44.dp), contentAlignment = Alignment.Center) { icon() }
        Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
            Text(title, fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
            if (subtitle != null) Text(subtitle, fontFamily = interFamily, fontSize = 13.sp, color = Dash.Text2, maxLines = 2)
            if (extra != null) extra()
        }
        trailing()
    }
    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Dash.PanelBorder.copy(alpha = 0.5f)))
}

@Composable
private fun PlaceDot(place: Int, small: Boolean = false) {
    val color = when (place) {
        1 -> Color(0xFFFFB92E)
        2 -> Color(0xFFB8C2E8)
        else -> Color(0xFFD98B52)
    }
    val d = if (small) 20.dp else 26.dp
    Box(
        modifier = Modifier.size(d).clip(CircleShape).background(Color(0xFF0A1146)).border(1.5.dp, color, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(place.toString(), fontFamily = interFontFamily(), fontWeight = FontWeight.Bold, fontSize = if (small) 11.sp else 13.sp, color = color)
    }
}

@Composable
private fun BadgeCircle(ring: Color, icon: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(38.dp).clip(CircleShape).background(ring.copy(alpha = 0.18f)).border(2.dp, ring, CircleShape),
        contentAlignment = Alignment.Center
    ) { icon() }
}

@Composable
private fun AchievementToast(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val strings = LocalStrings.current
    val interFamily = interFontFamily()
    val shape = RoundedCornerShape(18.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF111A66))
            .border(1.5.dp, Brand.Yellow.copy(alpha = 0.8f), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(40.dp).clip(CircleShape).background(Brand.Yellow.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) { StarIcon(Brand.Yellow, 24.dp, filled = true) }
        Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
            Text(strings.dashNewAchievement, fontFamily = interFamily, fontSize = 12.sp, color = Dash.Text2)
            Text(strings.dashAchievementName, fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(14.dp)) {
                val w = size.width
                drawLine(Color.White.copy(alpha = 0.8f), Offset(0f, 0f), Offset(w, w), 2.5f, StrokeCap.Round)
                drawLine(Color.White.copy(alpha = 0.8f), Offset(w, 0f), Offset(0f, w), 2.5f, StrokeCap.Round)
            }
        }
    }
}
