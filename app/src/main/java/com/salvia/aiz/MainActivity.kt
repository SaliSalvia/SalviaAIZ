@file:OptIn(ExperimentalMaterial3Api::class)

package com.salvia.aiz

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.*
import coil.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

// Color Palette
val SkyBlue = Color(0xFF4A90E2)
val LightPurple = Color(0xFFB39DDB)
val LightPurpleContainer = Color(0xFFF3E8FF)
val PureWhite = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF1A1A2E)
val TextSecondary = Color(0xFF4A4A4A)

const val ZAI_MODEL = "glm-4.6"
const val PREFS_NAME = "SalviaAIZPrefs"
const val KEY_API = "api_key"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SalviaApp()
        }
    }
}

data class ChatMessage(val text: String, val isUser: Boolean)

@Composable
fun SalviaApp() {
    val nav = rememberNavController()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    
    var apiKey by remember { mutableStateOf(prefs.getString(KEY_API, "") ?: "") }

    NavHost(navController = nav, startDestination = "welcome") {
        composable("welcome") {
            WelcomeScreen(
                apiKey = apiKey,
                onApiKeyChange = { newKey ->
                    apiKey = newKey
                    prefs.edit().putString(KEY_API, newKey).apply()
                },
                onNavigateToMain = {
                    if (apiKey.isNotBlank()) {
                        nav.navigate("chat") { popUpTo("welcome") { inclusive = true } }
                    }
                }
            )
        }
        composable("chat") {
            ChatScreen(apiKey = apiKey)
        }
    }
}

@Composable
fun WelcomeScreen(apiKey: String, onApiKeyChange: (String) -> Unit, onNavigateToMain: () -> Unit) {
    val transition = rememberInfiniteTransition()
    val logoAlpha by transition.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500, easing = EaseInOutCubic), RepeatMode.Reverse)
    )
    var buttonVisible by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { delay(1500); buttonVisible = true }

    Box(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(PureWhite, LightPurpleContainer))),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 24.dp)
        ) {
            Box(
                modifier = Modifier.size(160.dp).clip(RoundedCornerShape(32.dp))
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
            Text("SalviaAIZ", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(R.string.welcome_subtitle), fontSize = 14.sp, color = TextSecondary)
            
            AnimatedVisibility(visible = buttonVisible, enter = fadeIn(tween(500)) + slideInVertically { it / 2 }) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = onApiKeyChange,
                        label = { Text("Z.ai API Key", color = TextSecondary) },
                        placeholder = { Text("کلید خود را وارد کنید", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = SkyBlue,
                            unfocusedBorderColor = LightPurple,
                            cursorColor = SkyBlue
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Button(
                        onClick = {
                            keyboard?.hide()
                            onNavigateToMain()
                        },
                        enabled = apiKey.isNotBlank(),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SkyBlue,
                            contentColor = PureWhite,
                            disabledContainerColor = Color.Gray,
                            disabledContentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth().height(52.dp)
                    ) {
                        Text(stringResource(R.string.get_started), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
fun ChatScreen(apiKey: String) {
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val scope = rememberCoroutineScope()

    Scaffold(
        containerColor = PureWhite,
        topBar = { 
            TopAppBar(
                title = { Text("SalviaAIZ", color = SkyBlue, fontWeight = FontWeight.Bold) }, 
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PureWhite) 
            ) 
        },
        bottomBar = {
            Surface(color = PureWhite, shadowElevation = 4.dp) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = inputText, onValueChange = { inputText = it },
                        placeholder = { Text(stringResource(R.string.chat_input_hint)) },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = PureWhite, focusedBorderColor = SkyBlue, unfocusedBorderColor = Color.LightGray)
                    )
                    Spacer(Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = {
                            if (inputText.isNotBlank() && !isLoading) {
                                val userMsg = inputText
                                messages.add(ChatMessage(userMsg, true))
                                inputText = ""
                                isLoading = true
                                
                                scope.launch {
                                    val botReply = sendMessageToZai(userMsg, apiKey)
                                    messages.add(ChatMessage(botReply, false))
                                    isLoading = false
                                }
                            }
                        },
                        containerColor = SkyBlue, contentColor = PureWhite, shape = CircleShape
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PureWhite, strokeWidth = 2.dp)
                        } else {
                            Text("↑", fontSize = 20.sp)
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (messages.isEmpty()) {
            Column(Modifier.padding(padding).fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(stringResource(R.string.chat_welcome), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(messages) { msg ->
                    ChatBubble(msg)
                }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bgColor = if (message.isUser) SkyBlue else LightPurple
    
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        Surface(
            color = bgColor,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Text(
                text = message.text,
                color = PureWhite,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                fontSize = 15.sp
            )
        }
    }
}

suspend fun sendMessageToZai(userText: String, apiKey: String): String {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val jsonBody = JSONObject().apply {
                put("model", ZAI_MODEL)
                val messagesArray = JSONArray()
                messagesArray.put(JSONObject().put("role", "user").put("content", userText))
                put("messages", messagesArray)
            }.toString()

            val body = jsonBody.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://api.z.ai/api/paas/v4/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val resStr = response.body?.string() ?: ""
            
            val jsonRes = JSONObject(resStr)
            if (jsonRes.has("error")) {
                "خطای سرور: کلید API اشتباه است یا اعتبار ندارد."
            } else {
                jsonRes.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content")
            }
        } catch (e: Exception) {
            "خطا در ارتباط با سرور: لطفا اینترنت خود را بررسی کنید."
        }
    }
}
