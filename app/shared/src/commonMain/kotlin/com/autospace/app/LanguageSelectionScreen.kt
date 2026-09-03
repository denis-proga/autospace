package com.autospace.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun LanguageSelectionScreen(onLanguageSelected: (Language) -> Unit) {
    val colors = LocalAppColors.current
    val interFamily = interFontFamily()
    val spaceGroteskFamily = spaceGroteskFontFamily()

    val windowSizeClass = LocalWindowSizeClass.current
    val contentModifier = if (windowSizeClass == WindowSizeClass.Compact) {
        Modifier.fillMaxWidth()
    } else {
        Modifier.fillMaxWidth().widthIn(max = 460.dp)
    }

    var selected by remember { mutableStateOf<Language?>(null) }
    val locked = selected != null

    LaunchedEffect(selected) {
        val chosen = selected
        if (chosen != null) {
            delay(300)
            onLanguageSelected(chosen)
        }
    }

    Box(modifier = Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Column(
                modifier = contentModifier,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Choose your language",
                    style = MaterialTheme.typography.headlineSmall,
                    fontFamily = spaceGroteskFamily,
                    fontWeight = FontWeight.Medium,
                    color = colors.textPrimary
                )

                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Language.entries.forEach { language ->
                        val isSelected = selected == language
                        val rowAlpha by animateFloatAsState(
                            targetValue = if (locked && !isSelected) 0.35f else 1f,
                            animationSpec = tween(250)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .alpha(rowAlpha)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) colors.accent.copy(alpha = 0.14f) else colors.card)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) colors.accent else colors.border,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable(enabled = !locked) { selected = language }
                                .padding(horizontal = 18.dp, vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = language.displayName,
                                fontFamily = interFamily,
                                color = colors.textPrimary
                            )

                            AnimatedVisibility(
                                visible = isSelected,
                                enter = scaleIn(animationSpec = tween(200)) + fadeIn(animationSpec = tween(200)),
                                exit = scaleOut(animationSpec = tween(150)) + fadeOut(animationSpec = tween(150))
                            ) {
                                Box(
                                    modifier = Modifier.size(22.dp).clip(CircleShape).background(colors.accent),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("✓", color = Color.White, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }