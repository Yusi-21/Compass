package com.mirea.compass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mirea.compass.ui.theme.CompassTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CompassTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CompassScreen()
                }
            }
        }
    }
}

@Composable
fun CompassScreen(viewModel: CompassViewModel =  viewModel()) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.initialize(context)
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> viewModel.startListening()
                Lifecycle.Event.ON_PAUSE -> viewModel.stopListening()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text(
            text = "🧭 Compass",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 32.dp)
        )

        Spacer(modifier = Modifier.weight(1f))

        if (uiState.hasSensor) {
            CompassView(azimuth = uiState.azimuth)

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Azimuth: ${uiState.azimuth.toInt()}°",
                fontSize = 28.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                DirectionText("N", uiState.azimuth, 0f)
                DirectionText("E", uiState.azimuth, 90f)
                DirectionText("S", uiState.azimuth, 180f)
                DirectionText("W", uiState.azimuth, 270f)
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = uiState.errorMessage.ifEmpty { "The device does not support orientation sensor" },
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun CompassView(azimuth: Float) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing)
        )
    )

    Box(
        modifier = Modifier
            .size(300.dp)
            .aspectRatio(1f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(-azimuth) // for north (N)
        ) {
            for (i in 0 until 360 step 30) {
                val angle = Math.toRadians(i.toDouble())
                val startX = size.width / 2 + (size.width / 2.5f) * kotlin.math.cos(angle).toFloat()
                val startY = size.height / 2 + (size.height / 2.5f) * kotlin.math.sin(angle).toFloat()
                val endX = size.width / 2 + (size.width / 2.2f) * kotlin.math.cos(angle).toFloat()
                val endY = size.height / 2 + (size.height / 2.2f) * kotlin.math.sin(angle).toFloat()

                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (i % 90 == 0) 3f else 1f
                )
            }
        }

        Canvas(
            modifier = Modifier
                .fillMaxSize(0.8f)
                .rotate(-azimuth)
        ) {
            drawLine(
                color = Color.Red,
                start = Offset(size.width / 2, size.height / 2),
                end = Offset(size.width / 2, 0f),
                strokeWidth = 8f
            )

            drawLine(
                color = Color.Gray,
                start = Offset(size.width / 2, size.height / 2),
                end = Offset(size.width / 2, size.height),
                strokeWidth = 8f
            )

            drawCircle(
                color = Color.Red,
                center = Offset(size.width / 2, 0f),
                radius = 12f
            )

            drawCircle(
                color = Color.White,
                center = Offset(size.width / 2, size.height / 2),
                radius = 20f
            )

            drawCircle(
                color = Color.Black,
                center = Offset(size.width / 2, size.height / 2),
                radius = 16f,
                style = Stroke(width = 2f)
            )
        }

        Text(
            text = "N",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Red,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (20).dp)
        )
    }
}

@Composable
fun DirectionText(direction: String, azimuth: Float, targetAngle: Float) {
    val diff = ((targetAngle - azimuth + 540) % 360 - 180)
    val isActive = kotlin.math.abs(diff) < 30

    Text(
        text = direction,
        fontSize = if (isActive) 20.sp else 16.sp,
        fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
        color = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray
    )
}