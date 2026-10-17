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
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ProvideTextStyle
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private fun payTwo(n: Int): String = n.toString().padStart(2, '0')

private fun payDate(epochMillis: Long): String {
    val d = GMTDate(epochMillis)
    return "${payTwo(d.dayOfMonth)}.${payTwo(d.month.ordinal + 1)}.${d.year}"
}

/** «3 €» или «1,67 €»: цена за день считается из цены плана. */
private fun payPerDay(priceEur: Int, days: Int): String {
    if (days <= 0) return ""
    val cents = priceEur * 100 / days
    return if (cents % 100 == 0) "${cents / 100} €" else "${cents / 100},${payTwo(cents % 100)} €"
}

/**
 * Тарифы и оплата. Экран живёт в двух состояниях: выбор тарифа и заказ с реквизитами.
 * reason: DEMO_LIMIT, LICENSE_EXPIRED или MENU.
 */
@Composable
fun PaywallScreen(
    reason: String,
    onAccessGranted: () -> Unit,
    onSessionExpired: () -> Unit
) {
    val strings = LocalStrings.current
    val ps = paymentStrings(strings)
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var plans by remember { mutableStateOf<List<PlanItemDto>>(emptyList()) }
    var awayMode by remember { mutableStateOf(false) }
    var awayUntil by remember { mutableStateOf("") }
    var selectedDays by remember { mutableStateOf<Int?>(null) }
    var agreed by remember { mutableStateOf(false) }
    var isCreating by remember { mutableStateOf(false) }

    var order by remember { mutableStateOf<PaymentOrderDto?>(null) }
    var details by remember { mutableStateOf<PaymentDetailsDto?>(null) }
    var paidUntil by remember { mutableStateOf<String?>(null) }

    suspend fun loadPlans() {
        isLoading = true
        errorText = null
        try {
            val response = ApiClient.getPlans()
            plans = response.plans
            awayMode = response.awayMode
            awayUntil = response.awayUntil
            if (selectedDays == null) selectedDays = response.plans.maxByOrNull { it.days }?.days

            // если заказ уже создан раньше, продолжаем с него, а не создаём второй
            val active = runCatching { ApiClient.getActivePaymentOrder() }.getOrNull()
            if (active?.success == true && active.order != null) {
                order = active.order
                details = active.details
            }
        } catch (e: Exception) {
            if (e is SessionExpiredException) onSessionExpired() else errorText = friendlyServerErrorMessage(e, strings)
        }
        isLoading = false
    }

    LaunchedEffect(Unit) { loadPlans() }

    // опрос статуса заказа: доступ открывается сам, без кнопки «я оплатил»
    val currentOrder = order
    LaunchedEffect(currentOrder?.orderId) {
        val id = currentOrder?.orderId ?: return@LaunchedEffect
        while (paidUntil == null) {
            delay(5000)
            try {
                val response = ApiClient.getPaymentOrder(id)
                val fresh = response.order ?: continue
                order = fresh
                if (fresh.status == "PAID") {
                    val expires = response.license?.expiresAt
                    paidUntil = if (expires != null) payDate(expires) else ""
                    break
                }
                if (fresh.status == "CANCELLED" || fresh.status == "DECLINED") break
            } catch (e: Exception) {
                if (e is SessionExpiredException) {
                    onSessionExpired()
                    break
                }
                // сеть моргнула: просто ждём следующей попытки
            }
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
                val activeOrder = order
                PayBackPill(
                    text = if (activeOrder != null && paidUntil == null) ps.cancelOrder else strings.supportBack,
                    onClick = {
                        if (activeOrder != null && paidUntil == null) {
                            scope.launch {
                                runCatching { ApiClient.cancelPaymentOrder(activeOrder.orderId) }
                                order = null
                            }
                        } else {
                            onAccessGranted()
                        }
                    }
                )
                Spacer(Modifier.height(if (wide) 20.dp else 14.dp))

                Text(
                    text = if (activeOrder != null) ps.payTitle else ps.plansTitle,
                    fontFamily = interFontFamily(),
                    fontWeight = FontWeight.Bold,
                    fontSize = if (wide) 40.sp else 27.sp,
                    color = Color.White
                )
                if (activeOrder == null) {
                    Text(
                        text = ps.plansSubtitle,
                        fontFamily = interFontFamily(),
                        fontSize = if (wide) 15.sp else 13.sp,
                        color = Dash.Text2,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }
                Spacer(Modifier.height(if (wide) 20.dp else 14.dp))

                when {
                    isLoading -> Box(
                        modifier = Modifier.fillMaxWidth().padding(top = 60.dp),
                        contentAlignment = Alignment.Center
                    ) { CircularProgressIndicator(color = Brand.Yellow) }

                    errorText != null -> DashPanel {
                        Text(text = errorText.orEmpty(), fontFamily = interFontFamily(), fontSize = 14.sp, color = Dash.Red)
                        Spacer(Modifier.height(14.dp))
                        PayYellowButton(text = strings.testRetry, onClick = { scope.launch { loadPlans() } })
                    }

                    activeOrder != null -> OrderBlock(
                        order = activeOrder,
                        details = details,
                        paidUntil = paidUntil,
                        ps = ps,
                        wide = wide,
                        onGoToTests = onAccessGranted
                    )

                    awayMode -> AwayBlock(ps = ps, awayUntil = awayUntil, onGoToTests = onAccessGranted)

                    else -> PlansBlock(
                        plans = plans,
                        selectedDays = selectedDays,
                        agreed = agreed,
                        isCreating = isCreating,
                        ps = ps,
                        wide = wide,
                        onSelect = { selectedDays = it },
                        onToggleAgree = { agreed = !agreed },
                        onPay = {
                            val days = selectedDays ?: return@PlansBlock
                            isCreating = true
                            scope.launch {
                                try {
                                    val response = ApiClient.createPaymentOrder(PaymentClaimRequestDto(days))
                                    if (response.success && response.order != null) {
                                        order = response.order
                                        details = response.details
                                    } else if (response.code == "AWAY") {
                                        awayMode = true
                                        awayUntil = response.awayUntil
                                    } else {
                                        errorText = strings.errorGeneric
                                    }
                                } catch (e: Exception) {
                                    if (e is SessionExpiredException) onSessionExpired()
                                    else errorText = friendlyServerErrorMessage(e, strings)
                                }
                                isCreating = false
                            }
                        }
                    )
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ───────────────────────── выбор тарифа ─────────────────────────

@Composable
private fun PlansBlock(
    plans: List<PlanItemDto>,
    selectedDays: Int?,
    agreed: Boolean,
    isCreating: Boolean,
    ps: PaymentStrings,
    wide: Boolean,
    onSelect: (Int) -> Unit,
    onToggleAgree: () -> Unit,
    onPay: () -> Unit
) {
    val bestDays = plans.maxByOrNull { it.days }?.days

    if (wide) {
        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            plans.forEach { plan ->
                PlanCard(
                    plan = plan,
                    selected = plan.days == selectedDays,
                    best = plan.days == bestDays,
                    ps = ps,
                    wide = true,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onClick = { onSelect(plan.days) }
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            plans.forEach { plan ->
                PlanCard(
                    plan = plan,
                    selected = plan.days == selectedDays,
                    best = plan.days == bestDays,
                    ps = ps,
                    wide = false,
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onSelect(plan.days) }
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    DashPanel(padding = if (wide) 20.dp else 14.dp) {
        Text(
            text = ps.includesTitle,
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = if (wide) 18.sp else 16.sp,
            color = Color.White
        )
        Spacer(Modifier.height(10.dp))
        listOf(ps.includeTests, ps.includeExam, ps.includeStats).forEach { line ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                PayTickGlyph(color = Dash.Green, glyphSize = 18.dp)
                Text(
                    text = line,
                    fontFamily = interFontFamily(),
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
        }
    }

    Spacer(Modifier.height(16.dp))

    AgreeRow(agreed = agreed, ps = ps, onToggle = onToggleAgree)
    Spacer(Modifier.height(12.dp))
    PayYellowButton(
        text = ps.goToPay,
        enabled = agreed && selectedDays != null && !isCreating,
        loading = isCreating,
        onClick = onPay
    )
}

@Composable
private fun PlanCard(
    plan: PlanItemDto,
    selected: Boolean,
    best: Boolean,
    ps: PaymentStrings,
    wide: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val borderColor = when {
        selected -> Brand.Yellow
        best -> Brand.Yellow.copy(alpha = 0.5f)
        else -> Dash.PanelBorder
    }
    Column(
        modifier = modifier
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    listOf(Dash.PanelTop.copy(alpha = 0.92f), Dash.PanelBottom.copy(alpha = 0.95f))
                )
            )
            .border(if (selected) 2.dp else 1.dp, borderColor, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(if (wide) 18.dp else 14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = ps.days(plan.days),
                fontFamily = interFontFamily(),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color.White,
                modifier = Modifier.weight(1f)
            )
            if (best) {
                val chipShape = RoundedCornerShape(50)
                Text(
                    text = ps.best,
                    fontFamily = interFontFamily(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = Color(0xFF1A1440),
                    modifier = Modifier
                        .clip(chipShape)
                        .background(Brand.Yellow)
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                )
            }
            if (selected) {
                Spacer(Modifier.width(8.dp))
                Box(
                    modifier = Modifier.size(22.dp).clip(CircleShape).background(Brand.Yellow),
                    contentAlignment = Alignment.Center
                ) { PayTickGlyph(color = Color(0xFF1A1440), glyphSize = 14.dp) }
            }
        }
        Spacer(Modifier.height(if (wide) 14.dp else 8.dp))
        Text(
            text = "${plan.priceEur} €",
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = if (wide) 34.sp else 26.sp,
            color = Color.White
        )
        Text(
            text = ps.perDay(payPerDay(plan.priceEur, plan.days)),
            fontFamily = interFontFamily(),
            fontSize = 12.sp,
            color = Dash.Text2
        )
    }
}

@Composable
private fun AgreeRow(agreed: Boolean, ps: PaymentStrings, onToggle: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF0A0F45).copy(alpha = 0.55f))
            .border(1.dp, Dash.PanelBorder, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onToggle)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val boxShape = RoundedCornerShape(6.dp)
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(boxShape)
                .background(if (agreed) Brand.Yellow else Color.Transparent)
                .border(1.5.dp, if (agreed) Brand.Yellow else Dash.Ring, boxShape),
            contentAlignment = Alignment.Center
        ) {
            if (agreed) PayTickGlyph(color = Color(0xFF1A1440), glyphSize = 14.dp)
        }
        // страницы условий ещё не сделаны, поэтому пока это обычный текст, а не ссылки
        Text(
            text = "${ps.agree} ${ps.terms} · ${ps.privacy}",
            fontFamily = interFontFamily(),
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.9f),
            textDecoration = TextDecoration.None,
            modifier = Modifier.padding(start = 12.dp)
        )
    }
}

// ───────────────────────── заказ ─────────────────────────

@Composable
private fun OrderBlock(
    order: PaymentOrderDto,
    details: PaymentDetailsDto?,
    paidUntil: String?,
    ps: PaymentStrings,
    wide: Boolean,
    onGoToTests: () -> Unit
) {
    val left: @Composable ColumnScope.() -> Unit = {
        DashPanel(padding = if (wide) 20.dp else 14.dp) {
            Text(
                text = ps.orderNo(order.orderId),
                fontFamily = interFontFamily(),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = Color.White
            )
            Text(
                text = ps.accessFor(order.days),
                fontFamily = interFontFamily(),
                fontSize = 13.sp,
                color = Dash.Text2,
                modifier = Modifier.padding(top = 2.dp)
            )
            Text(
                text = "${order.amountEur} €",
                fontFamily = interFontFamily(),
                fontWeight = FontWeight.Bold,
                fontSize = if (wide) 36.sp else 28.sp,
                color = Color.White,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Spacer(Modifier.height(12.dp))
        RequisitesPanel(details = details, ps = ps, wide = wide)
    }

    val right: @Composable ColumnScope.() -> Unit = {
        CodePanel(code = order.code, ps = ps, wide = wide)
        Spacer(Modifier.height(12.dp))
        if (paidUntil == null) WaitingPanel(ps = ps) else PaidPanel(ps = ps, until = paidUntil, onGoToTests = onGoToTests)
    }

    if (wide) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(modifier = Modifier.weight(1f), content = left)
            Column(modifier = Modifier.weight(1f), content = right)
        }
    } else {
        Column(modifier = Modifier.fillMaxWidth()) {
            right()
            Spacer(Modifier.height(12.dp))
            left()
        }
    }
}

@Composable
private fun RequisitesPanel(details: PaymentDetailsDto?, ps: PaymentStrings, wide: Boolean) {
    DashPanel(padding = if (wide) 20.dp else 14.dp) {
        Text(
            text = ps.requisites,
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = if (wide) 18.sp else 16.sp,
            color = Color.White
        )
        Spacer(Modifier.height(12.dp))

        val phone = details?.bizumPhone.orEmpty()
        val card = details?.cardNumber.orEmpty()
        val holder = details?.holderName.orEmpty()

        if (phone.isNotBlank()) {
            RequisiteRow(label = ps.bizum, value = phone, ps = ps)
        }
        if (card.isNotBlank()) {
            if (phone.isNotBlank()) Spacer(Modifier.height(10.dp))
            RequisiteRow(label = ps.card, value = card, ps = ps)
        }
        if (holder.isNotBlank()) {
            Text(
                text = "${ps.holder}: $holder",
                fontFamily = interFontFamily(),
                fontSize = 12.sp,
                color = Dash.Text2,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
        Text(
            text = ps.howToPay,
            fontFamily = interFontFamily(),
            fontSize = 12.sp,
            color = Dash.Text2,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
private fun RequisiteRow(label: String, value: String, ps: PaymentStrings) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF0A0F45).copy(alpha = 0.65f))
            .border(1.dp, Dash.PanelBorder, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(text = label, fontFamily = interFontFamily(), fontSize = 11.sp, color = Dash.Text2)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
            Text(
                text = value,
                fontFamily = interFontFamily(),
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = Color.White,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            CopyButton(text = value, ps = ps)
        }
    }
}

@Composable
private fun CodePanel(code: String, ps: PaymentStrings, wide: Boolean) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF1A2480), Color(0xFF0C1456))))
            .border(2.dp, Brand.Yellow, shape)
            .padding(if (wide) 20.dp else 14.dp)
    ) {
        Text(
            text = ps.codeTitle,
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = if (wide) 18.sp else 16.sp,
            color = Color.White
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = code,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                fontSize = if (wide) 34.sp else 26.sp,
                color = Brand.Yellow,
                modifier = Modifier.weight(1f)
            )
            CopyButton(text = code, ps = ps)
        }
        Text(
            text = ps.codeHint,
            fontFamily = interFontFamily(),
            fontSize = 12.sp,
            color = Dash.Text2,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
private fun WaitingPanel(ps: PaymentStrings) {
    DashPanel(padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(color = Dash.Blue, strokeWidth = 3.dp, modifier = Modifier.size(26.dp))
            Text(
                text = ps.waitingTitle,
                fontFamily = interFontFamily(),
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = Color.White,
                modifier = Modifier.padding(start = 14.dp)
            )
        }
        Text(
            text = ps.waitingText,
            fontFamily = interFontFamily(),
            fontSize = 13.sp,
            color = Dash.Text2,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
}

@Composable
private fun PaidPanel(ps: PaymentStrings, until: String, onGoToTests: () -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Dash.Green.copy(alpha = 0.16f))
            .border(1.5.dp, Dash.Green, shape)
            .padding(18.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(Dash.Green),
                contentAlignment = Alignment.Center
            ) { PayTickGlyph(color = Color.White, glyphSize = 20.dp) }
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(
                    text = ps.paidTitle,
                    fontFamily = interFontFamily(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color.White
                )
                if (until.isNotBlank()) {
                    Text(
                        text = ps.accessUntil(until),
                        fontFamily = interFontFamily(),
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
    Spacer(Modifier.height(12.dp))
    PayYellowButton(text = ps.goToTests, onClick = onGoToTests)
}

@Composable
private fun AwayBlock(ps: PaymentStrings, awayUntil: String, onGoToTests: () -> Unit) {
    DashPanel(padding = 20.dp) {
        Text(
            text = ps.awayTitle,
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = 19.sp,
            color = Color.White
        )
        Text(
            text = if (awayUntil.isBlank()) ps.awayNoDate else ps.awayText(awayUntil),
            fontFamily = interFontFamily(),
            fontSize = 14.sp,
            color = Dash.Text2,
            modifier = Modifier.padding(top = 10.dp)
        )
    }
    Spacer(Modifier.height(14.dp))
    PayYellowButton(text = ps.goToTests, onClick = onGoToTests)
}

// ───────────────────────── мелкие части ─────────────────────────

@Composable
private fun CopyButton(text: String, ps: PaymentStrings) {
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(1600)
            copied = false
        }
    }
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(if (copied) Dash.Green.copy(alpha = 0.22f) else Color(0xFF1B2790).copy(alpha = 0.85f))
            .border(1.dp, if (copied) Dash.Green else Dash.Ring.copy(alpha = 0.7f), shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                clipboard.setText(AnnotatedString(text))
                copied = true
            }
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (copied) PayTickGlyph(color = Dash.Green, glyphSize = 14.dp) else CopyGlyph(color = Color.White, glyphSize = 14.dp)
        Text(
            text = if (copied) ps.copied else ps.copy,
            fontFamily = interFontFamily(),
            fontSize = 12.sp,
            color = Color.White,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun PayYellowButton(
    text: String,
    enabled: Boolean = true,
    loading: Boolean = false,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFFFFD23F), Color(0xFFFFB21E))))
            .clickable(enabled = enabled, interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color(0xFF1A1440), strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(
            text = text,
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color(0xFF1A1440),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun PayBackPill(text: String, onClick: () -> Unit) {
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
private fun PayTickGlyph(color: Color, glyphSize: Dp) {
    Canvas(modifier = Modifier.size(glyphSize)) {
        val w = this.size.width
        val h = this.size.height
        val tick = Path().apply {
            moveTo(w * 0.14f, h * 0.54f)
            lineTo(w * 0.40f, h * 0.80f)
            lineTo(w * 0.86f, h * 0.22f)
        }
        drawPath(
            path = tick,
            color = color,
            style = Stroke(width = w * 0.16f, cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}

@Composable
private fun CopyGlyph(color: Color, glyphSize: Dp) {
    Canvas(modifier = Modifier.size(glyphSize)) {
        val w = this.size.width
        val h = this.size.height
        val sw = w * 0.11f
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.08f, h * 0.08f),
            size = androidx.compose.ui.geometry.Size(w * 0.58f, h * 0.58f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f),
            style = Stroke(width = sw)
        )
        drawRoundRect(
            color = color,
            topLeft = Offset(w * 0.34f, h * 0.34f),
            size = androidx.compose.ui.geometry.Size(w * 0.58f, h * 0.58f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(w * 0.12f),
            style = Stroke(width = sw)
        )
    }
}
