package com.autospace.app

import androidx.compose.ui.graphics.ImageBitmap

expect suspend fun decodeImageBitmap(bytes: ByteArray): ImageBitmap?