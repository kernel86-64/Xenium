package com.kernel64.xenium.ui.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ExpressiveItemPosition {
    TOP,
    MIDDLE,
    BOTTOM,
    SINGLE
}

fun getItemShape(
    position: ExpressiveItemPosition,
    cornerLarge: Dp = 24.dp,
    cornerSmall: Dp = 6.dp
): Shape {
    return when (position) {
        ExpressiveItemPosition.TOP -> RoundedCornerShape(
            topStart = cornerLarge,
            topEnd = cornerLarge,
            bottomStart = cornerSmall,
            bottomEnd = cornerSmall
        )
        ExpressiveItemPosition.MIDDLE -> RoundedCornerShape(cornerSmall)
        ExpressiveItemPosition.BOTTOM -> RoundedCornerShape(
            topStart = cornerSmall,
            topEnd = cornerSmall,
            bottomStart = cornerLarge,
            bottomEnd = cornerLarge
        )
        ExpressiveItemPosition.SINGLE -> RoundedCornerShape(cornerLarge)
    }
}

fun <T> List<T>.getPosition(index: Int): ExpressiveItemPosition {
    if (size <= 1) return ExpressiveItemPosition.SINGLE
    return when (index) {
        0 -> ExpressiveItemPosition.TOP
        size - 1 -> ExpressiveItemPosition.BOTTOM
        else -> ExpressiveItemPosition.MIDDLE
    }
}
