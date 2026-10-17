package com.autospace.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.coroutines.launch
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ktor.util.date.GMTDate

private fun profTwo(n: Int): String = n.toString().padStart(2, '0')

private fun profDate(epochMillis: Long): String {
    val d = GMTDate(epochMillis)
    return "${profTwo(d.dayOfMonth)}.${profTwo(d.month.ordinal + 1)}.${d.year}"
}

/**
 * Профиль: аккаунт, доступ, язык и короткая сводка прогресса.
 * Данные уже есть на клиенте, поэтому экран ничего не грузит сам.
 */
@Composable
fun ProfileScreen(
    username: String,
    results: List<TestResultItemDto>,
    onProfileUpdated: (String) -> Unit,
    language: Language,
    onLanguageChange: (Language) -> Unit,
    onOpenPaywall: () -> Unit,
    onLogout: () -> Unit,
    onBack: () -> Unit
) {
    val strings = LocalStrings.current
    val ps = profileStrings(strings)

    // лицензию и почту берём сами: в App их нет, а главная страница грузит их для себя
    var license by remember { mutableStateOf<LicenseInfoDto?>(null) }
    LaunchedEffect(Unit) {
        try {
            license = ApiClient.getLicense()
            MenuAvatar.id = license?.avatar.orEmpty()
        } catch (e: Exception) {
            if (e is SessionExpiredException) onLogout()
        }
    }
    val email = license?.email.orEmpty()

    val scope = rememberCoroutineScope()
    var name by remember(username) { mutableStateOf(username) }
    var editingName by remember { mutableStateOf(false) }
    var nameDraft by remember { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val avatarId = license?.avatar.orEmpty()

    fun saveProfile(newName: String?, newAvatar: String?) {
        saving = true
        nameError = null
        scope.launch {
            try {
                val response = ApiClient.updateProfile(UpdateProfileRequestDto(username = newName, avatar = newAvatar))
                if (response.success) {
                    if (newName != null) {
                        name = response.username
                        onProfileUpdated(response.username)
                        editingName = false
                    }
                    license = license?.copy(avatar = response.avatar)
                    MenuAvatar.id = response.avatar
                } else {
                    nameError = when (response.code) {
                        "USERNAME_TAKEN" -> ps.nameTaken
                        "BAD_USERNAME" -> ps.nameBad
                        else -> strings.errorGeneric
                    }
                }
            } catch (e: Exception) {
                if (e is SessionExpiredException) onLogout() else nameError = friendlyServerErrorMessage(e, strings)
            }
            saving = false
        }
    }

    ProvideTextStyle(
        LocalTextStyle.current.copy(lineHeight = TextUnit.Unspecified, letterSpacing = TextUnit.Unspecified)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val wide = maxWidth >= 900.dp
            val pad = if (wide) 28.dp else 14.dp

            DashboardBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = pad, vertical = if (wide) 20.dp else 12.dp)
            ) {
                ProfBackPill(text = strings.supportBack, onClick = onBack)
                Spacer(Modifier.height(if (wide) 20.dp else 14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    UserAvatar(username = name, avatarId = avatarId, size = if (wide) 64.dp else 52.dp)
                    Column(modifier = Modifier.padding(start = 14.dp)) {
                        Text(
                            text = name.replaceFirstChar { it.uppercase() },
                            fontFamily = interFontFamily(),
                            fontWeight = FontWeight.Bold,
                            fontSize = if (wide) 34.sp else 24.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = ps.title,
                            fontFamily = interFontFamily(),
                            fontSize = 13.sp,
                            color = Dash.Text2
                        )
                    }
                }
                Spacer(Modifier.height(if (wide) 20.dp else 14.dp))

                val accountCard: @Composable () -> Unit = {
                    DashPanel(padding = if (wide) 20.dp else 14.dp) {
                        SectionTitle(ps.accountSection, wide)
                        Spacer(Modifier.height(10.dp))
                        if (editingName) {
                            NameEditor(
                                value = nameDraft,
                                label = ps.usernameLabel,
                                saveText = ps.save,
                                cancelText = ps.cancel,
                                error = nameError,
                                saving = saving,
                                onChange = { nameDraft = it },
                                onCancel = {
                                    editingName = false
                                    nameError = null
                                },
                                onSave = { saveProfile(nameDraft, null) }
                            )
                        } else {
                            InfoRow(
                                label = ps.usernameLabel,
                                value = name,
                                actionText = ps.edit,
                                onAction = {
                                    nameDraft = name
                                    nameError = null
                                    editingName = true
                                }
                            )
                        }
                        if (email.isNotBlank()) {
                            Spacer(Modifier.height(8.dp))
                            InfoRow(label = ps.emailLabel, value = email)
                        }
                    }
                }

                val licenseCard: @Composable () -> Unit = {
                    LicenseCard(license = license, ps = ps, wide = wide, onOpenPaywall = onOpenPaywall)
                }

                if (wide) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) { accountCard() }
                        Box(modifier = Modifier.weight(1f).fillMaxHeight()) { licenseCard() }
                    }
                } else {
                    accountCard()
                    Spacer(Modifier.height(12.dp))
                    licenseCard()
                }

                Spacer(Modifier.height(if (wide) 16.dp else 12.dp))

                DashPanel(padding = if (wide) 20.dp else 14.dp) {
                    SectionTitle(ps.statsSection, wide)
                    Spacer(Modifier.height(10.dp))
                    if (results.isEmpty()) {
                        Text(text = ps.statsNone, fontFamily = interFontFamily(), fontSize = 14.sp, color = Dash.Text2)
                    } else {
                        val passed = results.filter { it.totalQuestions > 0 && it.correctCount * 100 / it.totalQuestions >= 90 }
                            .map { it.testNumber }
                            .distinct()
                            .size
                        val average = results.sumOf {
                            if (it.totalQuestions <= 0) 0 else it.correctCount * 100 / it.totalQuestions
                        } / results.size
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            MiniStat(value = "$passed", label = ps.statsTests, color = Dash.Green, modifier = Modifier.weight(1f))
                            MiniStat(value = "$average%", label = ps.statsAverage, color = Brand.Yellow, modifier = Modifier.weight(1f))
                        }
                    }
                }

                Spacer(Modifier.height(if (wide) 16.dp else 12.dp))

                DashPanel(padding = if (wide) 20.dp else 14.dp) {
                    SectionTitle(ps.avatarSection, wide)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(if (wide) 14.dp else 8.dp)
                    ) {
                        Avatars.ids.forEach { id ->
                            AvatarOption(
                                username = name,
                                id = id,
                                selected = id == avatarId,
                                size = if (wide) 72.dp else 46.dp,
                                onClick = { saveProfile(null, if (id == avatarId) "" else id) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(if (wide) 16.dp else 12.dp))

                DashPanel(padding = if (wide) 20.dp else 14.dp) {
                    SectionTitle(ps.languageSection, wide)
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Language.entries.forEach { lang ->
                            LanguageRow(
                                language = lang,
                                selected = lang == language,
                                onClick = { onLanguageChange(lang) }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(if (wide) 16.dp else 12.dp))

                LogoutButton(text = ps.logout, onClick = onLogout)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun LicenseCard(license: LicenseInfoDto?, ps: ProfileStrings, wide: Boolean, onOpenPaywall: () -> Unit) {
    val status = license?.status.orEmpty()
    val gift = license?.giftAccess == true
    val accent = when {
        gift -> Dash.Green
        status == "ACTIVE" || status == "FRIEND" -> Dash.Green
        status == "BLOCKED" -> Dash.Red
        status == "EXPIRED" -> Dash.Red
        else -> Brand.Yellow
    }
    val label = when (status) {
        "ACTIVE" -> ps.statusActive
        "FRIEND" -> ps.statusFriend
        "EXPIRED" -> ps.statusExpired
        "BLOCKED" -> ps.statusBlocked
        else -> ps.statusDemo
    }

    DashPanel(padding = if (wide) 20.dp else 14.dp) {
        SectionTitle(ps.licenseSection, wide)
        Spacer(Modifier.height(12.dp))

        val chipShape = RoundedCornerShape(50)
        Row(
            modifier = Modifier
                .clip(chipShape)
                .background(accent.copy(alpha = 0.16f))
                .border(1.dp, accent.copy(alpha = 0.8f), chipShape)
                .padding(start = 10.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(accent))
            Text(
                text = label,
                fontFamily = interFontFamily(),
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = Color.White,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        val expires = license?.expiresAt
        val daysLeft = license?.daysLeft
        if (expires != null) {
            Text(
                text = ps.until(profDate(expires)),
                fontFamily = interFontFamily(),
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.9f),
                modifier = Modifier.padding(top = 10.dp)
            )
        }
        if (daysLeft != null && daysLeft > 0) {
            Text(
                text = ps.daysLeft(daysLeft),
                fontFamily = interFontFamily(),
                fontSize = 13.sp,
                color = Dash.Text2,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (license != null && status != "ACTIVE" && status != "FRIEND") {
            Text(
                text = ps.demoLeft(license.demoTests.size, license.demoLimit),
                fontFamily = interFontFamily(),
                fontSize = 13.sp,
                color = Dash.Text2,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        if (gift) {
            Text(
                text = ps.giftNote,
                fontFamily = interFontFamily(),
                fontSize = 13.sp,
                color = Dash.Green,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        if (status != "BLOCKED") {
            Spacer(Modifier.height(14.dp))
            ProfYellowButton(
                text = if (status == "ACTIVE" || status == "FRIEND") ps.extend else ps.buy,
                onClick = onOpenPaywall
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String, wide: Boolean) {
    Text(
        text = text,
        fontFamily = interFontFamily(),
        fontWeight = FontWeight.Bold,
        fontSize = if (wide) 18.sp else 16.sp,
        color = Color.White
    )
}

@Composable
private fun InfoRow(label: String, value: String, actionText: String? = null, onAction: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF0A0F45).copy(alpha = 0.6f))
            .border(1.dp, Dash.PanelBorder, shape)
            .padding(horizontal = 12.dp, vertical = 9.dp)
    ) {
        Text(text = label, fontFamily = interFontFamily(), fontSize = 11.sp, color = Dash.Text2)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 2.dp)) {
            Text(
                text = value,
                fontFamily = interFontFamily(),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (actionText != null && onAction != null) {
                Text(
                    text = actionText,
                    fontFamily = interFontFamily(),
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = Brand.Yellow,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onAction)
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun MiniStat(value: String, label: String, color: Color, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(color.copy(alpha = 0.12f))
            .border(1.dp, color.copy(alpha = 0.5f), shape)
            .padding(vertical = 12.dp, horizontal = 10.dp)
    ) {
        Text(
            text = value,
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = 24.sp,
            color = color
        )
        Text(text = label, fontFamily = interFontFamily(), fontSize = 12.sp, color = Dash.Text2)
    }
}

@Composable
private fun LanguageRow(language: Language, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) Brand.Yellow.copy(alpha = 0.12f) else Color(0xFF0A0F45).copy(alpha = 0.6f))
            .border(1.dp, if (selected) Brand.Yellow else Dash.PanelBorder, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = language.displayName,
            fontFamily = interFontFamily(),
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            fontSize = 14.sp,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = language.code.uppercase(),
            fontFamily = interFontFamily(),
            fontSize = 12.sp,
            color = Dash.Text2
        )
        if (selected) {
            Spacer(Modifier.width(10.dp))
            Box(
                modifier = Modifier.size(20.dp).clip(CircleShape).background(Brand.Yellow),
                contentAlignment = Alignment.Center
            ) { ProfTick(color = Color(0xFF1A1440), glyphSize = 13.dp) }
        }
    }
}

@Composable
private fun ProfYellowButton(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFFFFD23F), Color(0xFFFFB21E))))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color(0xFF1A1440)
        )
    }
}

@Composable
private fun LogoutButton(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .border(1.dp, Dash.Red.copy(alpha = 0.7f), shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = Dash.Red
        )
    }
}

@Composable
private fun ProfBackPill(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(Color(0xFF14104A).copy(alpha = 0.78f))
            .border(1.dp, Dash.Ring.copy(alpha = 0.7f), shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(start = 10.dp, end = 18.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ChevronIcon(color = Brand.Yellow, left = true, modifier = Modifier.size(18.dp))
        Text(
            text = text,
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.Medium,
            fontSize = 14.sp,
            color = Color.White,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun ProfTick(color: Color, glyphSize: Dp) {
    Canvas(modifier = Modifier.size(glyphSize)) {
        val w = this.size.width
        val h = this.size.height
        val tick = Path().apply {
            moveTo(w * 0.14f, h * 0.54f)
            lineTo(w * 0.40f, h * 0.80f)
            lineTo(w * 0.86f, h * 0.22f)
        }
        drawPath(path = tick, color = color, style = Stroke(width = w * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
private fun NameEditor(
    value: String,
    label: String,
    saveText: String,
    cancelText: String,
    error: String?,
    saving: Boolean,
    onChange: (String) -> Unit,
    onCancel: () -> Unit,
    onSave: () -> Unit
) {
    val shape = RoundedCornerShape(14.dp)
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, fontFamily = interFontFamily(), fontSize = 11.sp, color = Dash.Text2)
        BasicTextField(
            value = value,
            onValueChange = onChange,
            singleLine = true,
            textStyle = TextStyle(
                color = Color.White,
                fontSize = 15.sp,
                fontFamily = interFontFamily(),
                fontWeight = FontWeight.SemiBold
            ),
            cursorBrush = SolidColor(Brand.Yellow),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .clip(shape)
                .background(Color(0xFF0A0F45).copy(alpha = 0.6f))
                .border(1.dp, Brand.Yellow.copy(alpha = 0.8f), shape)
                .padding(horizontal = 12.dp, vertical = 11.dp)
        )
        if (error != null) {
            Text(
                text = error,
                fontFamily = interFontFamily(),
                fontSize = 12.sp,
                color = Dash.Red,
                modifier = Modifier.padding(top = 6.dp)
            )
        }
        Row(modifier = Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Brand.Yellow)
                    .clickable(enabled = !saving, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onSave)
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(
                    text = saveText,
                    fontFamily = interFontFamily(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color(0xFF1A1440)
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .border(1.dp, Dash.Ring, RoundedCornerShape(50))
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onCancel)
                    .padding(horizontal = 18.dp, vertical = 8.dp)
            ) {
                Text(text = cancelText, fontFamily = interFontFamily(), fontSize = 13.sp, color = Color.White)
            }
        }
    }
}

/** Один аватар в сетке выбора: повторное нажатие по выбранному снимает картинку. */
@Composable
private fun AvatarOption(username: String, id: String, selected: Boolean, size: Dp, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
    ) {
        UserAvatar(
            username = username,
            avatarId = id,
            size = size,
            ring = if (selected) Brand.Yellow else Color.White.copy(alpha = 0.25f)
        )
    }
}
