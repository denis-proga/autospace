package com.autospace.app

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * segmentColors: цвет статуса каждого вопроса, null = не отвечен (серый).
 * Текущий сегмент выше остальных и с контуром. Клик по сегменту вызывает onSegmentClick.
 */
@Composable
fun SegmentedProgressBar(
    segmentColors: List<Color?>,
    currentIndex: Int,
    onSegmentClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current

    Row(
        modifier = modifier.height(28.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        segmentColors.forEachIndexed { index, statusColor ->
            val isCurrent = index == currentIndex
            val barHeight by animateDpAsState(
                targetValue = if (isCurrent) 12.dp else 4.dp,
                animationSpec = tween(180)
            )
            val interaction = remember { MutableInteractionSource() }

            // Внешний Box задаёт увеличенную зону нажатия (28dp по высоте)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(interactionSource = interaction, indication = null) {
                        onSegmentClick(index)
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(barHeight)
                        .clip(RoundedCornerShape(3.dp))
                        .background(statusColor ?: colors.border)
                        .then(
                            if (isCurrent) Modifier.border(1.5.dp, colors.textPrimary, RoundedCornerShape(3.dp))
                            else Modifier
                        )
                )
            }
        }
    }
}