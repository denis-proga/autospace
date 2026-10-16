package com.autospace.app

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.ktor.util.date.GMTDate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Центр поддержки. Сначала отвечает помощник (темы-кнопки и свободный вопрос), и только если ответ
 * не помог, пользователь пишет разработчику через форму. Обращения хранятся на сервере,
 * ответ админ даёт из Telegram командой /reply.
 */
@Composable
fun SupportCenterScreen(
    language: String,
    onBack: () -> Unit,
    onOpenPaywall: () -> Unit,
    onSessionExpired: () -> Unit
) {
    val strings = LocalStrings.current
    val scope = rememberCoroutineScope()

    var tickets by remember { mutableStateOf<List<TicketDto>>(emptyList()) }
    var remaining by remember { mutableStateOf(3) }
    var ticketsLoaded by remember { mutableStateOf(false) }
    var ticketsFailed by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableStateOf(0) }
    var serverOk by remember { mutableStateOf<Boolean?>(null) }
    var license by remember { mutableStateOf<LicenseInfoDto?>(null) }

    // помощник
    var chat by remember { mutableStateOf(listOf(ChatLine(fromBot = true, text = strings.botGreeting))) }
    var choices by remember { mutableStateOf<List<BotChoice>>(emptyList()) }
    var typing by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf("") }
    var lastTopic by remember { mutableStateOf<BotTopic?>(null) }
    var lastQuestion by remember { mutableStateOf("") }

    // форма обращения к разработчику
    var formOpen by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf("OTHER") }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var subjectError by remember { mutableStateOf(false) }
    var messageError by remember { mutableStateOf(false) }
    var formError by remember { mutableStateOf<String?>(null) }
    var submitting by remember { mutableStateOf(false) }
    var sentId by remember { mutableStateOf<Int?>(null) }
    var selected by remember { mutableStateOf<TicketDto?>(null) }

    fun topicChoices(): List<BotChoice> =
        listOf(BotTopic.NO_CODE, BotTopic.PASSWORD, BotTopic.ACCESS, BotTopic.PAYMENT, BotTopic.PROGRESS, BotTopic.LANGUAGE, BotTopic.QUESTION_ERROR)
            .map { BotChoice(topicLabel(it, strings), BotAction.Topic(it)) }

    LaunchedEffect(Unit) {
        choices = topicChoices()
        serverOk = ApiClient.isServerReachable()
    }
    LaunchedEffect(Unit) {
        try {
            license = ApiClient.getLicense()
        } catch (e: Exception) {
        }
    }
    LaunchedEffect(reloadKey) {
        try {
            val response = ApiClient.getTickets()
            tickets = response.tickets
            remaining = response.remaining
            ticketsFailed = false
        } catch (e: SessionExpiredException) {
            onSessionExpired()
        } catch (e: Exception) {
            ticketsFailed = true
        }
        ticketsLoaded = true
    }
    LaunchedEffect(sentId) {
        if (sentId != null) {
            delay(4000)
            sentId = null
        }
    }

    fun botSays(text: String, next: List<BotChoice>) {
        scope.launch {
            typing = true
            delay(450)
            chat = chat + ChatLine(fromBot = true, text = text)
            choices = next
            typing = false
        }
    }

    fun openForm() {
        category = ticketCategoryFor(lastTopic)
        subject = lastTopic?.let { topicLabel(it, strings) } ?: ""
        message = lastQuestion
        subjectError = false
        messageError = false
        formError = null
        formOpen = true
    }

    fun answerTopic(topic: BotTopic) {
        lastTopic = topic
        val answer = botAnswer(topic, strings, license)
        val next = mutableListOf<BotChoice>()
        if (topic == BotTopic.QUESTION_ERROR) {
            next += BotChoice(strings.botAskDeveloper, BotAction.Escalate, highlight = true)
            next += BotChoice(strings.botOtherTopic, BotAction.Topics)
        } else {
            next += BotChoice(strings.botHelped, BotAction.Helped)
            next += BotChoice(strings.botNotHelped, BotAction.NotHelped)
            if (answer.offerTariffs) next += BotChoice(strings.botOpenTariffs, BotAction.Tariffs, highlight = true)
        }
        botSays(answer.text, next)
    }

    fun handle(action: BotAction, userLabel: String) {
        if (typing) return
        chat = chat + ChatLine(fromBot = false, text = userLabel)
        choices = emptyList()
        when (action) {
            is BotAction.Topic -> answerTopic(action.topic)
            BotAction.Helped -> botSays(strings.botReplyHelped, topicChoices())
            BotAction.NotHelped -> botSays(
                strings.botReplyNotHelped,
                listOf(
                    BotChoice(strings.botAskDeveloper, BotAction.Escalate, highlight = true),
                    BotChoice(strings.botOtherTopic, BotAction.Topics)
                )
            )
            BotAction.Escalate -> openForm()
            BotAction.Tariffs -> {
                choices = topicChoices()
                onOpenPaywall()
            }
            BotAction.Topics -> botSays(strings.botPickTopic, topicChoices())
        }
    }

    fun sendFreeText() {
        val text = input.trim()
        if (text.isEmpty() || typing) return
        input = ""
        lastQuestion = text
        chat = chat + ChatLine(fromBot = false, text = text)
        choices = emptyList()
        val topic = matchTopic(text)
        if (topic != null) {
            answerTopic(topic)
        } else {
            lastTopic = null
            botSays(
                strings.botNoMatch,
                listOf(BotChoice(strings.botAskDeveloper, BotAction.Escalate, highlight = true)) + topicChoices()
            )
        }
    }

    fun submit() {
        if (submitting) return
        subjectError = subject.trim().length < 3
        messageError = message.trim().length < 20
        formError = null
        if (subjectError || messageError) return

        // к сообщению добавляем контекст для разработчика: тему помощника и состояние доступа
        val context = "\n\n— ${lastTopic?.name ?: "-"} / ${license?.status ?: "?"}"
        val body = message.trim().take(1000 - context.length) + context

        scope.launch {
            submitting = true
            try {
                val response = ApiClient.createTicket(
                    TicketCreateRequestDto(
                        category = category,
                        subject = subject.trim(),
                        message = body,
                        contactEmail = null,
                        language = language
                    )
                )
                if (response.success) {
                    sentId = response.ticketId
                    subject = ""
                    message = ""
                    remaining = response.remaining
                    formOpen = false
                    reloadKey++
                    chat = chat + ChatLine(fromBot = true, text = strings.botTicketSent(response.ticketId))
                    choices = topicChoices()
                } else {
                    formError = when (response.code) {
                        "TOO_MANY_TICKETS" -> strings.supportTooMany
                        "TOO_MANY_REQUESTS" -> strings.forgotTooManyRequests
                        else -> strings.errorGeneric
                    }
                }
            } catch (e: SessionExpiredException) {
                onSessionExpired()
            } catch (e: Exception) {
                formError = friendlyServerErrorMessage(e, strings)
            }
            submitting = false
        }
    }

    ProvideTextStyle(
        LocalTextStyle.current.copy(lineHeight = TextUnit.Unspecified, letterSpacing = TextUnit.Unspecified)
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val wide = maxWidth >= 900.dp
            val pad = if (wide) 28.dp else 14.dp

            SceneBackground(dim = 0f)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.00f to Color(0x330D0B45),
                                0.30f to Color(0xB30D0B45),
                                0.55f to Color(0xE60D0B45),
                                1.00f to Color(0xF20D0B45)
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = pad, vertical = if (wide) 20.dp else 12.dp)
            ) {
                BackPill(text = strings.supportBack, onClick = onBack)
                Spacer(Modifier.height(if (wide) 22.dp else 14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = strings.supportTitle,
                        style = TextStyle(
                            brush = Brush.horizontalGradient(
                                0.0f to Color.White,
                                0.55f to Color.White,
                                0.75f to Brand.Yellow,
                                1.0f to Brand.Yellow
                            )
                        ),
                        fontFamily = interFontFamily(),
                        fontWeight = FontWeight.Bold,
                        fontSize = if (wide) 46.sp else 30.sp
                    )
                    Spacer(Modifier.width(16.dp))
                    ServerStatusChip(serverOk)
                }
                Text(
                    text = strings.supportSubtitle,
                    fontFamily = interFontFamily(),
                    fontSize = if (wide) 15.sp else 13.sp,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(top = 6.dp, bottom = if (wide) 22.dp else 14.dp)
                )

                val mainCard: @Composable () -> Unit = {
                    if (formOpen) {
                        SupportPanel {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { formOpen = false }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ChevronIcon(color = Brand.Yellow, left = true, modifier = Modifier.size(16.dp))
                                Text(strings.botBackToAssistant, fontFamily = interFontFamily(), fontSize = 13.sp, color = Brand.Yellow, modifier = Modifier.padding(start = 4.dp))
                            }
                            Spacer(Modifier.height(10.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                HeadsetIcon(30.dp)
                                Column(modifier = Modifier.padding(start = 12.dp)) {
                                    Text(strings.botFormTitle, fontFamily = interFontFamily(), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.White)
                                    Text(strings.botFormSubtitle, fontFamily = interFontFamily(), fontSize = 13.sp, color = Dash.Text2)
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            val categories = listOf("PAYMENT", "NO_CODE", "QUESTION_ERROR", "TECH", "OTHER")
                            val categoryLabels = listOf(
                                strings.supportCatPayment, strings.supportCatNoCode, strings.supportCatQuestion,
                                strings.supportCatTech, strings.supportCatOther
                            )
                            ChipFlow(
                                labels = categoryLabels,
                                isSelected = { categories[it] == category },
                                onClick = { category = categories[it] }
                            )
                            Spacer(Modifier.height(16.dp))
                            SupportField(
                                label = strings.supportSubjectLabel,
                                value = subject,
                                onChange = { subject = it.take(150); subjectError = false },
                                placeholder = strings.supportSubjectPlaceholder,
                                isError = subjectError
                            )
                            if (subjectError) GentleError(strings.supportSubjectTooShort, Modifier.padding(top = 6.dp))
                            Spacer(Modifier.height(14.dp))
                            SupportField(
                                label = strings.supportMessageLabel,
                                value = message,
                                onChange = { message = it; messageError = false },
                                placeholder = strings.supportMessagePlaceholder,
                                multiline = true,
                                counterMax = 900,
                                isError = messageError
                            )
                            if (messageError) GentleError(strings.supportMessageTooShort, Modifier.padding(top = 6.dp))
                            Text(
                                text = strings.supportRemaining(remaining),
                                fontFamily = interFontFamily(),
                                fontSize = 12.sp,
                                color = Dash.Text2,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 12.dp)
                            )
                            if (formError != null) GentleError(formError ?: "", Modifier.padding(bottom = 10.dp))
                            PrimaryButton(
                                text = strings.supportSend,
                                onClick = { submit() },
                                enabled = !submitting,
                                isLoading = submitting
                            )
                        }
                    } else {
                        SupportPanel {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                HeadsetIcon(30.dp)
                                Column(modifier = Modifier.padding(start = 12.dp)) {
                                    Text(strings.supportBotTitle, fontFamily = interFontFamily(), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = Color.White)
                                    Text(strings.supportBotSubtitle, fontFamily = interFontFamily(), fontSize = 13.sp, color = Dash.Text2)
                                }
                            }
                            Spacer(Modifier.height(14.dp))

                            val threadScroll = rememberScrollState()
                            LaunchedEffect(chat.size, typing) { threadScroll.animateScrollTo(threadScroll.maxValue) }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 160.dp, max = if (wide) 400.dp else 320.dp)
                                    .verticalScroll(threadScroll),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                chat.forEach { line -> ChatBubble(line) }
                                if (typing) ChatBubble(ChatLine(fromBot = true, text = "…"))
                            }

                            if (choices.isNotEmpty()) {
                                Spacer(Modifier.height(12.dp))
                                ChipFlow(
                                    labels = choices.map { it.label },
                                    isSelected = { choices[it].highlight },
                                    onClick = { index -> handle(choices[index].action, choices[index].label) }
                                )
                            }
                            Spacer(Modifier.height(14.dp))
                            ChatInput(
                                value = input,
                                onChange = { input = it.take(300) },
                                placeholder = strings.botInputPlaceholder,
                                onSend = { sendFreeText() }
                            )
                        }
                    }
                }

                val ticketsCard: @Composable () -> Unit = {
                    SupportPanel {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DocIcon(Brand.Yellow, 26.dp)
                            Text(strings.supportMyTickets, fontFamily = interFontFamily(), fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White, modifier = Modifier.padding(start = 12.dp))
                        }
                        Spacer(Modifier.height(14.dp))
                        when {
                            !ticketsLoaded -> Text("…", fontFamily = interFontFamily(), color = Dash.Text2, fontSize = 14.sp)
                            ticketsFailed -> Text(strings.supportLoadFailed, fontFamily = interFontFamily(), color = Dash.Text2, fontSize = 13.sp)
                            tickets.isEmpty() -> Column(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                DocIcon(Dash.Ring, 34.dp)
                                Text(strings.supportNoTickets, fontFamily = interFontFamily(), fontSize = 14.sp, color = Dash.Text2, modifier = Modifier.padding(top = 8.dp))
                            }
                            else -> Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                tickets.forEach { ticket -> TicketRow(ticket) { selected = ticket } }
                            }
                        }
                    }
                }

                if (wide) {
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        Column(modifier = Modifier.weight(1.45f)) { mainCard() }
                        Column(modifier = Modifier.weight(1f)) { ticketsCard() }
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        mainCard()
                        ticketsCard()
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            val sent = sentId
            if (sent != null) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 18.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color(0xFF0E3B33))
                        .border(1.dp, Dash.Green, RoundedCornerShape(50))
                        .padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusDot(Dash.Green, check = true, size = 20.dp)
                    Text(
                        text = strings.supportSentToast(sent),
                        fontFamily = interFontFamily(),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = Color.White,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            }
        }

        val open = selected
        if (open != null) {
            TicketDialog(ticket = open, onClose = { selected = null })
        }
    }
}

// ------------------------------------------------------------------------------------------
// Детали страницы
// ------------------------------------------------------------------------------------------

@Composable
private fun SupportPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xE61A2480), Color(0xE60C1456))))
            .border(1.dp, Dash.PanelBorder, shape)
            .padding(20.dp),
        content = content
    )
}

@Composable
private fun BackPill(text: String, onClick: () -> Unit) {
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
        Text(text, fontFamily = interFontFamily(), fontWeight = FontWeight.Medium, fontSize = 14.sp, color = Color.White, modifier = Modifier.padding(start = 6.dp))
    }
}

/** Значок состояния связи: реальная проверка сервера, а не заглушка. */
@Composable
private fun ServerStatusChip(serverOk: Boolean?) {
    val strings = LocalStrings.current
    val color = when (serverOk) {
        true -> Dash.Green
        false -> Dash.Red
        null -> Brand.Yellow
    }
    val text = when (serverOk) {
        true -> strings.supportStatusOk
        false -> strings.supportStatusDown
        null -> strings.supportStatusChecking
    }
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .clip(shape)
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.8f), shape)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(text, fontFamily = interFontFamily(), fontSize = 12.sp, color = Color.White, maxLines = 1, softWrap = false, modifier = Modifier.padding(start = 8.dp))
    }
}

/** Ряды кнопок-«таблеток»: число в ряду зависит от ширины. */
@Composable
private fun ChipFlow(labels: List<String>, isSelected: (Int) -> Boolean, onClick: (Int) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val perRow = when {
            maxWidth >= 640.dp -> 5
            maxWidth >= 380.dp -> 3
            else -> 2
        }
        val indexed = labels.mapIndexed { index, label -> index to label }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            indexed.chunked(perRow).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    row.forEach { (index, label) -> Chip(label, isSelected(index)) { onClick(index) } }
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(
                if (selected) Brush.verticalGradient(listOf(Color(0xFFFFD84D), Color(0xFFFFB627)))
                else Brush.verticalGradient(listOf(Color(0xFF2A2590).copy(alpha = 0.55f), Color(0xFF2A2590).copy(alpha = 0.55f)))
            )
            .border(1.dp, if (selected) Color.Transparent else Color.White.copy(alpha = 0.28f), shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontFamily = interFontFamily(),
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = if (selected) Color(0xFF1B1540) else Color.White,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun ChatBubble(line: ChatLine) {
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (line.fromBot) Arrangement.Start else Arrangement.End,
        verticalAlignment = Alignment.Top
    ) {
        if (line.fromBot) {
            Box(
                modifier = Modifier.padding(end = 8.dp).size(28.dp).clip(CircleShape).background(Brand.Yellow.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) { HeadsetIcon(16.dp) }
        }
        Box(
            modifier = Modifier
                .widthIn(max = 480.dp)
                .clip(shape)
                .background(
                    if (line.fromBot) Brush.verticalGradient(listOf(Color(0xFF1B2A9C).copy(alpha = 0.55f), Color(0xFF1B2A9C).copy(alpha = 0.55f)))
                    else Brush.verticalGradient(listOf(Color(0xFF4A5CF0), Color(0xFF3446D8)))
                )
                .border(1.dp, if (line.fromBot) Dash.PanelBorder else Color.Transparent, shape)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(line.text, fontFamily = interFontFamily(), fontSize = 14.sp, color = Color.White)
        }
    }
}

@Composable
private fun ChatInput(value: String, onChange: (String) -> Unit, placeholder: String, onSend: () -> Unit) {
    val interFamily = interFontFamily()
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(50)
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(46.dp)
                .clip(shape)
                .background(Color(0xFF2A2590).copy(alpha = if (focused) 0.65f else 0.5f))
                .border(if (focused) 1.5.dp else 1.dp, if (focused) Brand.Yellow else Color.White.copy(alpha = 0.28f), shape)
                .padding(horizontal = 18.dp)
        ) {
            if (value.isEmpty()) {
                Text(placeholder, fontFamily = interFamily, fontSize = 14.sp, color = Color.White.copy(alpha = 0.55f), modifier = Modifier.align(Alignment.CenterStart))
            }
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontFamily = interFamily, fontSize = 14.sp),
                cursorBrush = SolidColor(Brand.Yellow),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onSend() }),
                interactionSource = interaction,
                modifier = Modifier.fillMaxWidth().align(Alignment.CenterStart)
            )
        }
        Spacer(Modifier.width(10.dp))
        Box(
            modifier = Modifier
                .size(46.dp)
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(Color(0xFFFFD84D), Color(0xFFFFB627))))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onSend),
            contentAlignment = Alignment.Center
        ) {
            ChevronIcon(color = Color(0xFF1B1540), modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun SupportField(
    label: String,
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    multiline: Boolean = false,
    counterMax: Int? = null,
    isError: Boolean = false
) {
    val interFamily = interFontFamily()
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val shape = RoundedCornerShape(14.dp)
    val borderColor = when {
        isError -> Brand.Error
        focused -> Brand.Yellow
        else -> Color.White.copy(alpha = 0.28f)
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, fontFamily = interFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (multiline) Modifier.heightIn(min = 112.dp) else Modifier.height(46.dp))
                .clip(shape)
                .background(Color(0xFF2A2590).copy(alpha = if (focused) 0.65f else 0.5f))
                .border(if (focused || isError) 1.5.dp else 1.dp, borderColor, shape)
                .padding(horizontal = 14.dp, vertical = if (multiline) 12.dp else 0.dp)
        ) {
            val fieldAlign = if (multiline) Alignment.TopStart else Alignment.CenterStart
            if (value.isEmpty()) {
                Text(placeholder, fontFamily = interFamily, fontSize = 14.sp, color = Color.White.copy(alpha = 0.55f), modifier = Modifier.align(fieldAlign))
            }
            BasicTextField(
                value = value,
                onValueChange = { new -> onChange(if (counterMax != null) new.take(counterMax) else new) },
                singleLine = !multiline,
                textStyle = TextStyle(color = Color.White, fontFamily = interFamily, fontSize = 14.sp),
                cursorBrush = SolidColor(Brand.Yellow),
                interactionSource = interaction,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(fieldAlign)
                    .padding(bottom = if (counterMax != null) 16.dp else 0.dp)
            )
            if (counterMax != null) {
                Text(
                    text = "${value.length} / $counterMax",
                    fontFamily = interFamily,
                    fontSize = 11.sp,
                    color = Dash.Text2,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
    }
}

@Composable
private fun TicketStatusChip(status: String) {
    val strings = LocalStrings.current
    val (text, bg, fg) = when (status) {
        "OPEN" -> Triple(strings.supportStatusOpen, Color(0xFF1E6BFF), Color.White)
        "IN_PROGRESS" -> Triple(strings.supportStatusWork, Color(0xFFFFC83D), Color(0xFF1B1540))
        "ANSWERED" -> Triple(strings.supportStatusAnswered, Color(0xFF2BD18A), Color(0xFF0B2A22))
        else -> Triple(strings.supportStatusClosed, Color(0xFF5B6590), Color.White)
    }
    Box(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontFamily = interFontFamily(), fontWeight = FontWeight.Bold, fontSize = 11.sp, color = fg, maxLines = 1, softWrap = false)
    }
}

@Composable
private fun TicketRow(ticket: TicketDto, onClick: () -> Unit) {
    val interFamily = interFontFamily()
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Color(0xFF0A1146).copy(alpha = 0.55f))
            .border(1.dp, Dash.PanelBorder, shape)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("#${ticket.id}", fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
        Column(modifier = Modifier.padding(horizontal = 12.dp).weight(1f)) {
            Text(ticket.subject, fontFamily = interFamily, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(formatTicketDate(ticket.createdAt), fontFamily = interFamily, fontSize = 11.sp, color = Dash.Text2)
        }
        TicketStatusChip(ticket.status)
        ChevronIcon(color = Color.White.copy(alpha = 0.8f), modifier = Modifier.padding(start = 8.dp).size(16.dp))
    }
}

@Composable
private fun TicketDialog(ticket: TicketDto, onClose: () -> Unit) {
    val strings = LocalStrings.current
    val interFamily = interFontFamily()
    val shape = RoundedCornerShape(22.dp)

    Dialog(onDismissRequest = onClose) {
        Column(
            modifier = Modifier
                .widthIn(max = 460.dp)
                .clip(shape)
                .background(Color(0xFF111A66))
                .border(1.dp, Dash.PanelBorder, shape)
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("#${ticket.id}", fontFamily = interFamily, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color.White)
                Spacer(Modifier.weight(1f))
                TicketStatusChip(ticket.status)
            }
            Text(ticket.subject, fontFamily = interFamily, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = Color.White, modifier = Modifier.padding(top = 8.dp))
            Text(formatTicketDate(ticket.createdAt), fontFamily = interFamily, fontSize = 12.sp, color = Dash.Text2, modifier = Modifier.padding(top = 2.dp, bottom = 14.dp))

            Text(strings.supportYourMessage, fontFamily = interFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Dash.Text2)
            Text(ticket.message, fontFamily = interFamily, fontSize = 14.sp, color = Color.White, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))

            Text(strings.supportReplyTitle, fontFamily = interFamily, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Brand.Yellow)
            Text(
                text = ticket.lastReply ?: strings.supportNoReplyYet,
                fontFamily = interFamily,
                fontSize = 14.sp,
                color = if (ticket.lastReply != null) Color.White else Dash.Text2,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )
            SecondaryButton(text = strings.supportClose, onClick = onClose, withChevron = false)
        }
    }
}

private fun formatTicketDate(epochMillis: Long): String {
    val d = GMTDate(epochMillis)
    fun two(n: Int) = n.toString().padStart(2, '0')
    return "${two(d.dayOfMonth)}.${two(d.month.ordinal + 1)}.${d.year} ${two(d.hours)}:${two(d.minutes)}"
}

@Composable
private fun HeadsetIcon(size: Dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val h = this.size.height
        val c = Brand.Yellow
        val sw = w * 0.085f
        drawArc(c, 180f, 180f, false, Offset(w * 0.16f, h * 0.10f), Size(w * 0.68f, h * 0.7f), style = Stroke(sw, cap = StrokeCap.Round))
        drawRoundRect(c, Offset(w * 0.08f, h * 0.44f), Size(w * 0.18f, h * 0.32f), CornerRadius(w * 0.07f))
        drawRoundRect(c, Offset(w * 0.74f, h * 0.44f), Size(w * 0.18f, h * 0.32f), CornerRadius(w * 0.07f))
        val mic = Path().apply {
            moveTo(w * 0.84f, h * 0.76f)
            lineTo(w * 0.78f, h * 0.90f)
            lineTo(w * 0.52f, h * 0.90f)
        }
        drawPath(mic, color = c, style = Stroke(sw, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
