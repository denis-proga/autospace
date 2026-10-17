package com.autospace.app

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import autospace.app.shared.generated.resources.Res
import autospace.app.shared.generated.resources.avatar_1
import autospace.app.shared.generated.resources.avatar_2
import autospace.app.shared.generated.resources.avatar_3
import autospace.app.shared.generated.resources.avatar_4
import autospace.app.shared.generated.resources.avatar_5
import autospace.app.shared.generated.resources.avatar_6
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * Аватары-роботы. Выбор хранится строкой («avatar_3») рядом с именем пользователя,
 * пустая строка — кружок с первой буквой имени, как было раньше.
 */
internal object Avatars {
    val ids: List<String> = listOf("avatar_1", "avatar_2", "avatar_3", "avatar_4", "avatar_5", "avatar_6")

    fun resourceFor(id: String): DrawableResource? = when (id) {
        "avatar_1" -> Res.drawable.avatar_1
        "avatar_2" -> Res.drawable.avatar_2
        "avatar_3" -> Res.drawable.avatar_3
        "avatar_4" -> Res.drawable.avatar_4
        "avatar_5" -> Res.drawable.avatar_5
        "avatar_6" -> Res.drawable.avatar_6
        else -> null
    }

    /** Общий тёмный фон под всеми аватарами: картинки прозрачные. */
    fun backdropFor(id: String): Color = Color(0xFF1B2790)
}

/**
 * Кружок аватара: картинка, если она выбрана, иначе первая буква имени.
 * Заменяет прежний AvatarCircle там, где нужен выбранный аватар.
 */
@Composable
internal fun UserAvatar(
    username: String,
    avatarId: String,
    size: Dp = 40.dp,
    ring: Color = Color.White.copy(alpha = 0.35f)
) {
    val resource = Avatars.resourceFor(avatarId)
    if (resource == null) {
        AvatarCircle(initial = username, size = size, ring = ring)
        return
    }
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Avatars.backdropFor(avatarId))
            .border(1.dp, ring, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(resource),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    }
}
