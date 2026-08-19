package com.salvia.aiz

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import kotlinx.coroutines.delay

// Color Palette
val SkyBlue = Color(0xFF4A90E2)
val LightPurple = Color(0xFFB39DDB)
val LightPurpleContainer = Color(0xFFF3E8FF)
val PureWhite = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF1A1A2E)
val TextSecondary = Color(0xFF4A4A4A)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SalviaApp()
        }
    }
}

@Composable
fun SalviaApp() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "welcome") {
        composable("welcome") {
            WelcomeScreen(onNavigateToMain = { nav.navigate("chat") { popUpTo("welcome") { inclusive = true } } })
        }
        composable("chat") {
            ChatScreen()
        }
    }
}

@Composable
fun WelcomeScreen(onNavigateToMain: () -> Unit) {
    val transition = rememberInfiniteTransition()
    val logoAlpha by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = EaseInOutCubic), RepeatMode.Reverse)
    )
    var buttonVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(2500); buttonVisible = true }

    Box(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(PureWhite, LightPurpleContainer))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier.size(220.dp).clip(RoundedCornerShape(32.dp))
                    .background(Brush.linearGradient(listOf(SkyBlue, LightPurple)))
                    .padding(4.dp).clip(RoundedCornerShape(28.dp)).background(PureWhite).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = "https://z-cdn-media.chatglm.cn/files/d1a66a52-4e68-4e6a-9cb7-c8a5534f4137.png?auth_key=1887157008-575486a271fd4627a2bb6b091cada77d-0-3a890561d90c33c6af7d3afcdb7040ff",
                    contentDescription = "Logo",
                    modifier = Modifier.fillMaxSize().alpha(logoAlpha)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("SalviaAIZ", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.welcome_subtitle), fontSize = 16.sp, color = TextSecondary)
            Spacer(modifier = Modifier.height(48.dp))
            AnimatedVisibility(visible = buttonVisible, enter = fadeIn(tween(500)) + slideInVertically { it / 2 }) {
                Button(
                    onClick = onNavigateToMain, shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SkyBlue, contentColor = PureWhite),
                    modifier = Modifier.padding(horizontal = 48.dp).height(52.dp)
                ) { Text(stringResource(R.string.get_started), fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen() {
    var text by remember { mutableStateOf("") }
    Scaffold(
        containerColor = PureWhite,
        topBar = { TopAppBar(title = { Text("SalviaAIZ", color = SkyBlue, fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = PureWhite)) },
        bottomBar = {
            Surface(color = PureWhite, shadowElevation = 4.dp) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = text, onValueChange = { text = it },
                        placeholder = { Text(stringResource(R.string.chat_input_hint)) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = PureWhite, focusedBorderColor = SkyBlue, unfocusedBorderColor = Color.LightGray)
                    )
                    Spacer(Modifier.width(8.dp))
                    FloatingActionButton(onClick = {}, containerColor = SkyBlue, contentColor = PureWhite, shape = CircleShape) {
                        Text("↑", fontSize = 20.sp)
                    }
                }
            }
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(60.dp))
            Text(stringResource(R.string.chat_welcome), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}
