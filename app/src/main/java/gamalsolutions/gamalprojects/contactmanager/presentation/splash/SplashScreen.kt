package gamalsolutions.gamalprojects.contactmanager.presentation.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import gamalsolutions.gamalprojects.R
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * شاشة البداية (Splash Screen) بتدرج زمردي هادئ وحركة دخول سلسة للشعار والعنوان.
 */
@Composable
fun SplashScreen(
    onTimeout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val logoScale = remember { Animatable(0.72f) }
    val contentAlpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch {
            logoScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
        launch {
            contentAlpha.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 550, easing = FastOutSlowInEasing)
            )
        }
        delay(1450L)
        onTimeout()
    }

    val topGlow = MaterialTheme.colorScheme.primary
    val deepBackdrop = MaterialTheme.colorScheme.onPrimaryContainer

    Box(
        modifier = modifier
            .fillMaxSize()
            .testTag("splash_screen")
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF096C69),
                        Color(0xFF064E4B),
                        Color(0xFF032D2B)
                    )
                )
            )
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            topGlow.copy(alpha = 0.28f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.5f, size.height * 0.36f),
                        radius = size.minDimension * 0.65f
                    ),
                    center = Offset(size.width * 0.5f, size.height * 0.36f),
                    radius = size.minDimension * 0.65f
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            deepBackdrop.copy(alpha = 0.22f),
                            Color.Transparent
                        ),
                        center = Offset(size.width * 0.85f, size.height * 0.85f),
                        radius = size.minDimension * 0.45f
                    ),
                    center = Offset(size.width * 0.85f, size.height * 0.85f),
                    radius = size.minDimension * 0.45f
                )
            }
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.alpha(contentAlpha.value)
        ) {
            Surface(
                shape = RoundedCornerShape(30.dp),
                shadowElevation = 14.dp,
                color = Color(0xFF096C69),
                modifier = Modifier
                    .size(116.dp)
                    .scale(logoScale.value)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_launcher_art),
                    contentDescription = stringResource(R.string.app_name),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(30.dp))
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = stringResource(R.string.app_name),
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.app_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFFCCFBF1),
                textAlign = TextAlign.Center
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 28.dp)
                .alpha(contentAlpha.value)
        ) {
            LinearProgressIndicator(
                color = Color(0xFF5EEAD4),
                trackColor = Color.White.copy(alpha = 0.2f),
                modifier = Modifier
                    .width(140.dp)
                    .height(4.dp)
                    .clip(CircleShape)
            )
        }
    }
}
