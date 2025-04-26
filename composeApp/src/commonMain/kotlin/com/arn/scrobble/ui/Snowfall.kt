package com.arn.scrobble.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.dp
import kotlin.random.Random

data class Snowflake(
    var x: Float,
    var y: Float,
    var radius: Float,
    var speed: Float,
)

//@Composable
//fun SnowfallEffect(
//    modifier: Modifier = Modifier,
//) {
//    val snowflakes = remember { List(100) { generateRandomSnowflake() } }
//    val infiniteTransition = rememberInfiniteTransition(label = "")
//
//    val offsetY by infiniteTransition.animateFloat(
//        initialValue = 0f,
//        targetValue = 1000f,
//        animationSpec = infiniteRepeatable(
//            animation = tween(durationMillis = 5000, easing = LinearEasing),
//            repeatMode = RepeatMode.Restart
//        ), label = ""
//    )
//
//    Canvas(modifier = modifier) {
//        snowflakes.forEach { snowflake ->
//            drawSnowflake(snowflake, offsetY % size.height)
//        }
//    }
//}

fun generateRandomSnowflake(): Snowflake {
    return Snowflake(
        x = Random.nextFloat(),
        y = Random.nextFloat() * 1000f,
        radius = Random.nextFloat() * 5.dp.value + 2.dp.value, // Snowflake size
        speed = Random.nextFloat() * 10.dp.value  // Falling speed
    )
}

fun DrawScope.drawSnowflake(snowflake: Snowflake, offsetY: Float, width: Int, height: Int) {
    val newY = (snowflake.y + offsetY * snowflake.speed) % height
    drawCircle(
        Color.White,
        alpha = 0.4f + 0.3f * (newY / height),
        radius = snowflake.radius,
        center = Offset(snowflake.x * width, newY)
    )
}