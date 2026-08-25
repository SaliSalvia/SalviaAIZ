@file:OptIn(ExperimentalMaterial3Api::class)

package com.salvia.aiz

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
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
import java.util.UUID

// Colors
val SkyBlue = Color(0xFF4A90E2)
val LightPurple = Color(0xFFB39DDB)
val LightPurpleContainer = Color(0xFFF3E8FF)
val PureWhite = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF1A1A2E)
val TextSecondary = Color(0xFF4A4A4A)

// Constants
const val CHAT_MODEL = "glm-4.6"
const val IMAGE_MODEL = "cogview-3"
const val PREFS_NAME = "SalviaAIZPrefs"
const val KEY_API = "api_key"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SalviaApp() }
    }
}

data class ChatMessage(val id: String = UUID.randomUUID().toString(), val text: String, val isUser: Boolean, val imageUri: String? = null)

@Composable
fun SalviaApp() {
    val nav = rememberNavController()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var apiKey by remember { mutableStateOf(prefs.getString(KEY_API, "") ?: "") }

    Scaffold(
        containerColor = PureWhite,
        bottomBar = {
            NavigationBar(containerColor = PureWhite, tonalElevation = 0.dp) {
                val items = listOf("Chat" to Icons.Outlined.Chat, "Image" to Icons.Outlined.Image, "Settings" to Icons.Outlined.Settings)
                val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route
                items.forEach { (label, icon) ->
                    NavigationBarItem(
                        selected = currentRoute == label.lowercase(),
                        onClick = { nav.navigate(label.lowercase()) { popUpTo(nav.graph.startDestinationId) { saveState = true } launchSingleTop = true } },
                        icon = { Icon(icon, label, modifier = Modifier.size(24.dp)) },
                        colors = NavigationBarItemDefaults.colors(selectedIconColor = SkyBlue, unselectedIconColor = LightPurple)
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = if (apiKey.isBlank()) "welcome" else "chat", modifier = Modifier.padding(padding)) {
            composable("welcome") {
                WelcomeScreen(
                    apiKey = apiKey,
                    onApiKeyChange = { newKey -> apiKey = newKey; prefs.edit().putString(KEY_API, newKey).apply() },
                    onNavigateToMain = { nav.navigate("chat") { popUpTo("welcome") { inclusive = true } } }
                )
            }
            composable("chat") { ChatScreen(apiKey) }
            composable("image") { ImageGenScreen(apiKey) }
            composable("settings") { PlaceholderScreen("Settings") }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(title, color = TextSecondary, fontWeight = FontWeight.Bold)
    }
}

// ==================== صفحه خوش آمدگویی ====================
@Composable
fun WelcomeScreen(apiKey: String, onApiKeyChange: (String) -> Unit, onNavigateToMain: () -> Unit) {
    var buttonVisible by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) { delay(1000); buttonVisible = true }

    Box(
        modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(PureWhite, LightPurpleContainer))),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 24.dp)) {
            Box(
                modifier = Modifier.size(140.dp).clip(RoundedCornerShape(32.dp)).background(Brush.linearGradient(listOf(SkyBlue, LightPurple))).padding(4.dp).clip(RoundedCornerShape(28.dp)).background(PureWhite).padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(model = "https://z-cdn-media.chatglm.cn/files/d1a66a52-4e68-4e6a-9cb7-c8a5534f4137.png?auth_key=1887157008-575486a271fd4627a2bb6b091cada77d-0-3a890561d90c33c6af7d3afcdb7040ff", contentDescription = "Logo", modifier = Modifier.fillMaxSize())
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text("SalviaAIZ", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedTextField(
                value = apiKey, onValueChange = onApiKeyChange,
                label = { Text("Z.ai API Key") }, modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp), singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = TextFieldDefaults.outlinedTextFieldColors(focusedBorderColor = SkyBlue, unfocusedBorderColor = LightPurple)
            )
            Spacer(modifier = Modifier.height(24.dp))
            AnimatedVisibility(visible = buttonVisible, enter = fadeIn(tween(500))) {
                Button(onClick = { keyboard?.hide(); onNavigateToMain() }, enabled = apiKey.isNotBlank(), shape = RoundedCornerShape(24.dp), colors = ButtonDefaults.buttonColors(containerColor = SkyBlue), modifier = Modifier.fillMaxWidth().height(52.dp)) {
                    Text("Start", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ==================== صفحه چت ====================
@Composable
fun ChatScreen(apiKey: String) {
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val messages = remember { mutableStateListOf<ChatMessage>() }
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var attachedImageUri by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        attachedImageUri = uri?.toString()
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex)
    }

    Scaffold(
        containerColor = PureWhite,
        topBar = { TopAppBar(title = { Text("SalviaAIZ Chat", color = SkyBlue, fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = PureWhite)) },
        bottomBar = {
            Surface(color = PureWhite, shadowElevation = 4.dp) {
                Column {
                    attachedImageUri?.let {
                        Box(Modifier.padding(8.dp)) {
                            AsyncImage(model = it, contentDescription = "Attachment", modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)))
                            IconButton(onClick = { attachedImageUri = null }, modifier = Modifier.size(20.dp).align(Alignment.TopEnd).background(Color.Black.copy(alpha=0.5f), CircleShape)) {
                                Icon(Icons.Filled.Close, "Remove", tint = Color.White, modifier = Modifier.size(12.dp))
                            }
                        }
                    }
                    Row(Modifier.padding(8.dp).fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                        IconButton(onClick = { imagePicker.launch("image/*") }) {
                            Icon(Icons.Filled.AddCircle, "Attach", tint = SkyBlue, modifier = Modifier.size(28.dp))
                        }
                        OutlinedTextField(
                            value = inputText, onValueChange = { inputText = it },
                            placeholder = { Text("Message SalviaAIZ...") },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(24.dp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = PureWhite, focusedBorderColor = SkyBlue, unfocusedBorderColor = Color.LightGray)
                        )
                        Spacer(Modifier.width(8.dp))
                        FloatingActionButton(
                            onClick = {
                                if ((inputText.isNotBlank() || attachedImageUri != null) && !isLoading) {
                                    val userMsg = inputText
                                    val img = attachedImageUri
                                    messages.add(ChatMessage(text = userMsg, isUser = true, imageUri = img))
                                    inputText = ""
                                    attachedImageUri = null
                                    isLoading = true
                                    scope.launch {
                                        val botMsgId = UUID.randomUUID().toString()
                                        messages.add(ChatMessage(id = botMsgId, text = "", isUser = false))
                                        streamZaiChatResponse(userMsg, apiKey) { chunk ->
                                            val idx = messages.indexOfFirst { it.id == botMsgId }
                                            if (idx != -1) {
                                                val updated = messages[idx].copy(text = messages[idx].text + chunk)
                                                messages[idx] = updated
                                            }
                                        }
                                        isLoading = false
                                    }
                                }
                            },
                            containerColor = SkyBlue, contentColor = PureWhite, shape = CircleShape
                        ) {
                            if (isLoading) CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PureWhite, strokeWidth = 2.dp)
                            else Icon(Icons.Filled.Send, "Send", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    ) { padding ->
        if (messages.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("How can I help you today?", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = TextPrimary) }
        } else {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(messages) { ChatBubble(it) }
            }
        }
    }
}

@Composable
fun ChatBubble(message: ChatMessage) {
    val alignment = if (message.isUser) Alignment.End else Alignment.Start
    val bgColor = if (message.isUser) SkyBlue else LightPurple
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = alignment) {
        message.imageUri?.let {
            AsyncImage(model = it, contentDescription = null, modifier = Modifier.size(200.dp).clip(RoundedCornerShape(12.dp)).padding(bottom = 4.dp))
        }
        if (message.text.isNotBlank()) {
            Surface(color = bgColor, shape = RoundedCornerShape(16.dp), modifier = Modifier.widthIn(max = 280.dp)) {
                Text(text = message.text, color = PureWhite, modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), fontSize = 15.sp)
            }
        }
    }
}

// ==================== صفحه تولید عکس ====================
@Composable
fun ImageGenScreen(apiKey: String) {
    var prompt by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var selectedSize by remember { mutableStateOf("1024x1024") }
    val scope = rememberCoroutineScope()

    val sizes = listOf("1024x1024" to "Square", "768x1344" to "Tall", "1344x768" to "Wide")

    Scaffold(
        containerColor = PureWhite,
        topBar = { TopAppBar(title = { Text("AI Image Studio", color = SkyBlue, fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = PureWhite)) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            OutlinedTextField(
                value = prompt, onValueChange = { prompt = it },
                placeholder = { Text("Describe the image you want to create...") },
                modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp, max = 120.dp),
                shape = RoundedCornerShape(16.dp),
                colors = TextFieldDefaults.outlinedTextFieldColors(containerColor = PureWhite, focusedBorderColor = SkyBlue, unfocusedBorderColor = Color.LightGray)
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                sizes.forEach { (size, label) ->
                    val isSelected = selectedSize == size
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isSelected) LightPurple else PureWhite,
                        modifier = Modifier.clickable { selectedSize = size }
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            color = if (isSelected) PureWhite else TextSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    if (prompt.isNotBlank() && !isLoading) {
                        isLoading = true
                        imageUrl = null
                        scope.launch {
                            imageUrl = generateZaiImage(prompt, selectedSize, apiKey)
                            isLoading = false
                        }
                    }
                },
                enabled = prompt.isNotBlank() && !isLoading,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SkyBlue, disabledContainerColor = Color.Gray)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PureWhite, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Generating Image...")
                } else {
                    Icon(Icons.Filled.AutoAwesome, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Generate", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(Modifier.height(24.dp))

            Box(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        LinearProgressIndicator(color = SkyBlue, trackColor = LightPurpleContainer, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(4.dp)))
                        Spacer(Modifier.height(8.dp))
                        Text("Creating your masterpiece...", color = TextSecondary, fontSize = 14.sp)
                    }
                } else if (imageUrl != null) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Generated Image",
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(24.dp))
                    )
                } else {
                    Icon(Icons.Outlined.Image, null, modifier = Modifier.size(80.dp), tint = LightPurple.copy(alpha = 0.5f))
                }
            }
        }
    }
}

// ==================== توابع ارتباط با سرور ====================
suspend fun streamZaiChatResponse(userText: String, apiKey: String, onToken: (String) -> Unit) {
    withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val jsonBody = JSONObject().apply {
                put("model", CHAT_MODEL)
                put("stream", true)
                val messagesArray = JSONArray()
                messagesArray.put(JSONObject().put("role", "user").put("content", userText))
                put("messages", messagesArray)
            }.toString()

            val body = jsonBody.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://api.z.ai/api/paas/v4/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Accept", "text/event-stream")
                .post(body).build()

            val response = client.newCall(request).execute()
            val source = response.body?.source() ?: return@withContext

            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: continue
                if (line.startsWith("data:")) {
                    val data = line.removePrefix("data:").trim()
                    if (data == "[DONE]") break
                    try {
                        val json = JSONObject(data)
                        val content = json.getJSONArray("choices").getJSONObject(0).getJSONObject("delta").optString("content")
                        if (content.isNotEmpty()) onToken(content)
                    } catch (e: Exception) { }
                }
            }
        } catch (e: Exception) {
            onToken("\n[Error: ${e.message}]")
        }
    }
}

suspend fun generateZaiImage(prompt: String, size: String, apiKey: String): String? {
    return withContext(Dispatchers.IO) {
        try {
            val client = OkHttpClient()
            val jsonBody = JSONObject().apply {
                put("model", IMAGE_MODEL)
                put("prompt", prompt)
                put("size", size)
            }.toString()

            val body = jsonBody.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://api.z.ai/api/paas/v4/images/generations")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(body).build()

            val response = client.newCall(request).execute()
            val resStr = response.body?.string() ?: ""
            val jsonRes = JSONObject(resStr)

            if (jsonRes.has("data")) {
                jsonRes.getJSONArray("data").getJSONObject(0).getString("url")
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
