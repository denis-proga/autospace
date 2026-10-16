package com.autospace.app

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

/**
 * Вход и регистрация на одной стеклянной карточке по центру экрана.
 * language и onChangeLanguage: значок текущего языка в углу возвращает к выбору языка.
 */
@Composable
fun AuthScreen(
    errorMessage: String?,
    isLoading: Boolean,
    onModeChanged: () -> Unit,
    onLogin: (username: String, password: String) -> Unit,
    onRegister: (User) -> Unit,
    onForgotPassword: () -> Unit,
    language: Language,
    onChangeLanguage: () -> Unit
) {
    val strings = LocalStrings.current
    val interFamily = interFontFamily()
    val focus = LocalFocusManager.current

    var isLoginMode by remember { mutableStateOf(true) }
    var showWakingHint by remember { mutableStateOf(false) }

    LaunchedEffect(isLoading) {
        if (isLoading) {
            delay(5000)
            showWakingHint = true
        } else {
            showWakingHint = false
        }
    }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var agreed by remember { mutableStateOf(false) }

    val canSubmit = if (isLoginMode) {
        username.isNotBlank() && password.isNotBlank()
    } else {
        firstName.isNotBlank() && lastName.isNotBlank() && email.isNotBlank() &&
                username.isNotBlank() && password.isNotBlank() && agreed
    }

    fun submit() {
        if (!canSubmit || isLoading) return
        if (isLoginMode) {
            onLogin(username.trim(), password)
        } else {
            onRegister(
                User(
                    firstName = firstName.trim(),
                    lastName = lastName.trim(),
                    email = email.trim(),
                    username = username.trim(),
                    password = password
                )
            )
        }
    }

    fun switchMode() {
        isLoginMode = !isLoginMode
        onModeChanged()
    }

    FormScaffold(
        topEnd = {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF14104A).copy(alpha = 0.78f))
                    .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(50))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onChangeLanguage() }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlagBadge(language = language, diameter = 24.dp)
                Text(
                    text = language.code.uppercase(),
                    fontFamily = interFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = Color.White,
                    modifier = Modifier.padding(start = 8.dp, end = 4.dp)
                )
            }
        }
    ) {
        GlassCard(modifier = Modifier.widthIn(max = 440.dp).fillMaxWidth()) {
            AnimatedContent(
                targetState = isLoginMode,
                transitionSpec = { fadeIn(tween(260)) togetherWith fadeOut(tween(120)) },
                label = "authMode"
            ) { loginMode ->
                Column {
                    AccentTitle(text = if (loginMode) strings.authWelcomeTitle else strings.authCreateAccountTitle)
                    if (loginMode) {
                        Text(
                            text = strings.authWelcomeSubtitle,
                            fontFamily = interFamily,
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = Color.White.copy(alpha = 0.78f),
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    Column(
                        modifier = Modifier.padding(top = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (!loginMode) {
                            GlassTextField(
                                value = firstName,
                                onValueChange = { firstName = it },
                                placeholder = strings.authFirstName,
                                leading = { UserIcon() },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) })
                            )
                            GlassTextField(
                                value = lastName,
                                onValueChange = { lastName = it },
                                placeholder = strings.authLastName,
                                leading = { UserIcon() },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) })
                            )
                            GlassTextField(
                                value = email,
                                onValueChange = { email = it },
                                placeholder = strings.authEmail,
                                leading = { MailIcon() },
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) })
                            )
                        }

                        GlassTextField(
                            value = username,
                            onValueChange = { username = it },
                            placeholder = if (loginMode) strings.forgotIdentifierLabel else strings.authUsername,
                            leading = { UserIcon() },
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) })
                        )
                        GlassTextField(
                            value = password,
                            onValueChange = { password = it },
                            placeholder = strings.authPassword,
                            leading = { LockIcon() },
                            trailing = {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { isPasswordVisible = !isPasswordVisible },
                                    contentAlignment = Alignment.Center
                                ) {
                                    EyeIcon(open = isPasswordVisible)
                                }
                            },
                            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { submit() })
                        )

                        if (!loginMode) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                GlassCheckbox(checked = agreed, onCheckedChange = { agreed = it })
                                Text(
                                    text = buildAnnotatedString {
                                        append(strings.authAgreeStart)
                                        withStyle(linkStyle()) { append(strings.authTerms) }
                                        append(strings.authAgreeAnd)
                                        withStyle(linkStyle()) { append(strings.authPrivacy) }
                                    },
                                    fontFamily = interFamily,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.padding(start = 12.dp)
                                )
                            }
                        }

                        if (errorMessage != null) {
                            GentleError(message = errorMessage)
                        }

                        PrimaryButton(
                            text = if (loginMode) strings.authLoginButton else strings.authCreateAccount,
                            onClick = { submit() },
                            enabled = canSubmit,
                            isLoading = isLoading,
                            modifier = Modifier.padding(top = 6.dp)
                        )

                        if (loginMode) {
                            LinkText(
                                text = strings.authForgotPassword,
                                onClick = onForgotPassword,
                                enabled = !isLoading,
                                color = Color(0xFFC9C5FF),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.22f)))
                            Text(
                                text = strings.authOr,
                                fontFamily = interFamily,
                                fontSize = 14.sp,
                                color = Color.White.copy(alpha = 0.7f),
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                            Box(Modifier.weight(1f).height(1.dp).background(Color.White.copy(alpha = 0.22f)))
                        }

                        SecondaryButton(
                            text = if (loginMode) strings.authCreateAccount else strings.authBackToLogin,
                            onClick = { switchMode() },
                            enabled = !isLoading,
                            withChevron = loginMode,
                            leadingChevron = !loginMode
                        )
                    }
                }
            }
        }

        if (showWakingHint) {
            Text(
                text = strings.commonWakingHint,
                fontFamily = interFamily,
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.85f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 18.dp)
            )
        }
    }
}
