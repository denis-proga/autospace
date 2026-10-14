package com.autospace.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

/**
 * Тарифы и ручная оплата.
 * reason: DEMO_LIMIT (кончились бесплатные тесты), LICENSE_EXPIRED (кончился доступ) или MENU (открыто из меню).
 * Реквизиты и цены приходят с сервера (GET /plans), их можно менять без обновления приложения.
 */
@Composable
fun PaywallScreen(
    reason: String,
    onAccessGranted: () -> Unit,
    onSessionExpired: () -> Unit
) {
    val colors = LocalAppColors.current
    val strings = LocalStrings.current
    val interFamily = interFontFamily()
    val spaceGroteskFamily = spaceGroteskFontFamily()
    val scope = rememberCoroutineScope()

    val windowSizeClass = LocalWindowSizeClass.current
    val contentModifier = if (windowSizeClass == WindowSizeClass.Compact) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.fillMaxWidth().widthIn(max = 480.dp)
    }

    var plans by remember { mutableStateOf<PlansResponseDto?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var reloadTrigger by remember { mutableStateOf(0) }
    var selectedDays by remember { mutableStateOf<Int?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var isChecking by remember { mutableStateOf(false) }
    var infoMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(reloadTrigger) {
        loadFailed = false
        try {
            plans = ApiClient.getPlans()
        } catch (e: SessionExpiredException) {
            onSessionExpired()
        } catch (e: Exception) {
            loadFailed = true
        }
    }

    val title = when (reason) {
        "LICENSE_EXPIRED" -> strings.paywallTitleExpired
        "DEMO_LIMIT" -> strings.paywallTitleDemo
        else -> strings.paywallTitleMenu
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = contentModifier
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontFamily = spaceGroteskFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 26.sp,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = strings.paywallDescription,
                fontFamily = interFamily,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )

            val loadedPlans = plans
            when {
                loadedPlans != null -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        loadedPlans.plans.forEach { plan ->
                            PlanRow(
                                label = strings.paywallDaysLabel(plan.days),
                                price = "${plan.priceEur} €",
                                selected = selectedDays == plan.days,
                                onClick = { selectedDays = plan.days }
                            )
                        }
                    }

                    Text(
                        text = strings.paywallHowToPay,
                        fontFamily = interFamily,
                        fontWeight = FontWeight.Medium,
                        color = colors.textPrimary,
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                    )
                    Text(
                        text = loadedPlans.paymentInstructions.ifBlank { strings.paywallNoInstructions },
                        fontFamily = interFamily,
                        color = colors.textSecondary,
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    )

                    LoadingButton(
                        text = strings.paywallIPaid,
                        isLoading = isSending,
                        enabled = selectedDays != null,
                        modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                        onClick = {
                            val days = selectedDays
                            if (days != null) {
                                scope.launch {
                                    isSending = true
                                    infoMessage = null
                                    try {
                                        val response = ApiClient.sendPaymentClaim(PaymentClaimRequestDto(days))
                                        infoMessage = when {
                                            response.success -> strings.paywallClaimSent
                                            response.message == "CLAIM_PENDING" -> strings.paywallClaimPending
                                            else -> strings.paywallClaimFailed
                                        }
                                    } catch (e: SessionExpiredException) {
                                        onSessionExpired()
                                    } catch (e: Exception) {
                                        infoMessage = friendlyServerErrorMessage(e, strings)
                                    }
                                    isSending = false
                                }
                            }
                        }
                    )

                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                isChecking = true
                                infoMessage = null
                                try {
                                    val license = ApiClient.getLicense()
                                    if (license.status == "ACTIVE" || license.status == "FRIEND") {
                                        onAccessGranted()
                                    } else {
                                        infoMessage = strings.paywallNotYet
                                    }
                                } catch (e: SessionExpiredException) {
                                    onSessionExpired()
                                } catch (e: Exception) {
                                    infoMessage = friendlyServerErrorMessage(e, strings)
                                }
                                isChecking = false
                            }
                        },
                        enabled = !isChecking,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
                    ) {
                        if (isChecking) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Text(strings.paywallCheckAccess, fontFamily = interFamily, color = colors.textPrimary)
                        }
                    }
                }

                loadFailed -> {
                    Text(
                        text = strings.paywallLoadFailed,
                        fontFamily = interFamily,
                        color = colors.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(top = 24.dp)
                    )
                    OutlinedButton(
                        onClick = { reloadTrigger++ },
                        modifier = Modifier.padding(top = 12.dp)
                    ) {
                        Text(strings.testRetry)
                    }
                }

                else -> {
                    CircularProgressIndicator(modifier = Modifier.padding(top = 32.dp))
                }
            }

            val message = infoMessage
            if (message != null) {
                Text(
                    text = message,
                    fontFamily = interFamily,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun PlanRow(
    label: String,
    price: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalAppColors.current
    val interFamily = interFontFamily()
    val shape = RoundedCornerShape(14.dp)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (selected) colors.accent.copy(alpha = 0.14f) else colors.card)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) colors.accent else colors.border,
                shape = shape
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontFamily = interFamily, fontWeight = FontWeight.Medium, color = colors.textPrimary)
        Text(text = price, fontFamily = interFamily, fontWeight = FontWeight.Medium, color = colors.textPrimary)
    }
}
