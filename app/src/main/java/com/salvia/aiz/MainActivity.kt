@file:OptIn(ExperimentalMaterial3Api::class)

package com.salvia.aiz

import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.OpenableColumns
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

// --- سیستم رنگی نئونی / شیشه‌ای (Obsidian, Violet, Electric Cyan, Pure White) ---
val BgDark = Color(0xFF0A0914)
val BgCardDark = Color(0xFF131224)
val NeonViolet = Color(0xFF8B5CF6)
val DeepViolet = Color(0xFF6D28D9)
val ElectricCyan = Color(0xFF06B6D4)
val CyberMint = Color(0xFF10B981)
val PureWhite = Color(0xFFF8FAFC)
val TextSecondary = Color(0xFF94A3B8)
val GlassSurface = Color(0x18FFFFFF)
val GlassBorder = Color(0x2EFFFFFF)

const val CHAT_MODEL = "glm-4.6"
const val IMAGE_MODEL = "cogview-3"
const val PREFS_NAME = "SalviaAIZPrefs"
const val KEY_API = "api_key"

// کلاینت پرسرعت OkHttp با Connection Pooling و تایم‌اوت مناسب هوش مصنوعی
val globalOkHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
}

// اکستنشن استایل شیشه‌ای و لبه‌های یخی
fun Modifier.glassmorphic(
    cornerRadius: Dp = 24.dp,
    bg: Color = GlassSurface,
    border: Color = GlassBorder
) = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(bg)
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(listOf(border, Color(0x05FFFFFF))),
        shape = RoundedCornerShape(cornerRadius)
    )

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = NeonViolet,
                    secondary = ElectricCyan,
                    background = BgDark,
                    surface = BgCardDark
                )
            ) {
                SalviaApp()
            }
        }
    }
}

// مدل‌های داده‌ای پیشرفته
data class FileAttachment(
    val uri: String,
    val name: String,
    val mimeType: String,
    val sizeString: String,
    val isImage: Boolean
)

data class GeneratedArtifact(
    val title: String,
    val content: String,
    val extension: String,
    val type: String
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val attachment: FileAttachment? = null,
    val artifacts: List<GeneratedArtifact> = emptyList()
)

@Composable
fun SalviaApp() {
    val nav = rememberNavController()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var apiKey by remember { mutableStateOf(prefs.getString(KEY_API, "") ?: "") }

    Scaffold(
        containerColor = BgDark,
        bottomBar = {
            val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route
            if (currentRoute != "welcome") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .glassmorphic(cornerRadius = 32.dp, bg = Color(0x30131224), border = Color(0x408B5CF6))
                    ) {
                        val items = listOf(
                            Triple("Chat", Icons.Outlined.Chat, Icons.Filled.Chat),
                            Triple("Studio", Icons.Outlined.AutoAwesome, Icons.Filled.AutoAwesome),
                            Triple("Settings", Icons.Outlined.Settings, Icons.Filled.Settings)
                        )
                        items.forEach { (label, unselectedIcon, selectedIcon) ->
                            val route = label.lowercase()
                            val isSelected = currentRoute == route
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    nav.navigate(route) {
                                        popUpTo(nav.graph.startDestinationId) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) selectedIcon else unselectedIcon,
                                        contentDescription = label,
                                        tint = if (isSelected) ElectricCyan else TextSecondary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        label,
                                        color = if (isSelected) PureWhite else TextSecondary,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = Color(0x2006B6D4)
                                )
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = if (apiKey.isBlank()) "welcome" else "chat",
            modifier = Modifier.padding(padding)
        ) {
            composable("welcome") {
                WelcomeScreen(
                    apiKey = apiKey,
                    onApiKeyChange = { newKey ->
                        apiKey = newKey
                        prefs.edit().putString(KEY_API, newKey).apply()
                    },
                    onNavigateToMain = {
                        nav.navigate("chat") {
                            popUpTo("welcome") { inclusive = true }
                        }
                    }
                )
            }
            composable("chat") { ChatScreen(apiKey) }
            composable("studio") { ImageAndEditStudioScreen(apiKey) }
            composable("settings") {
                SettingsScreen(
                    apiKey = apiKey,
                    onUpdateKey = { newKey ->
                        apiKey = newKey
                        prefs.edit().putString(KEY_API, newKey).apply()
                    },
                    onResetKey = {
                        apiKey = ""
                        prefs.edit().remove(KEY_API).apply()
                        nav.navigate("welcome") {
                            popUpTo(nav.graph.id) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

// --- صفحه خوش‌آمدگویی نئونی و یخی ---
@Composable
fun WelcomeScreen(apiKey: String, onApiKeyChange: (String) -> Unit, onNavigateToMain: () -> Unit) {
    var buttonVisible by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current
    LaunchedEffect(Unit) {
        delay(400)
        buttonVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(BgDark, Color(0xFF1E1035), BgDark)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .glassmorphic(cornerRadius = 32.dp, bg = Color(0x1AFFFFFF), border = Color(0x358B5CF6))
                .padding(28.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .glassmorphic(cornerRadius = 28.dp, bg = Color(0x308B5CF6), border = ElectricCyan),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = "Logo",
                    tint = ElectricCyan,
                    modifier = Modifier.size(54.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("SalviaAIZ", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = PureWhite)
            Text(
                "Ultimate Z.ai Intelligence Suite",
                fontSize = 13.sp,
                color = ElectricCyan,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(30.dp))

            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChange,
                label = { Text("Enter Z.ai API Key", color = TextSecondary) },
                placeholder = { Text("Bearer token / API key", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite,
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = NeonViolet.copy(alpha = 0.5f),
                    focusedContainerColor = Color(0x10FFFFFF),
                    unfocusedContainerColor = Color(0x08FFFFFF)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(visible = buttonVisible, enter = fadeIn(tween(400)) + expandVertically()) {
                Button(
                    onClick = {
                        keyboard?.hide()
                        onNavigateToMain()
                    },
                    enabled = apiKey.isNotBlank(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonViolet,
                        disabledContainerColor = Color(0x408B5CF6)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Get Started", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        Spacer(Modifier.width(8.dp))
                        Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = PureWhite)
                    }
                }
            }
        }
    }
}

// --- صفحه چت هوشمند با پشتیبانی کامل از انواع فایل‌ها و Artifacts ---
@Composable
fun ChatScreen(apiKey: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var currentAttachment by remember { mutableStateOf<FileAttachment?>(null) }
    var previewArtifact by remember { mutableStateOf<GeneratedArtifact?>(null) }

    val messages = remember { mutableStateListOf<ChatMessage>() }
    val listState = rememberLazyListState()

    // انتخاب انواع فایل‌ها (عکس، پی دی اف، ورد، متنی و...)
    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            val attachment = extractFileAttachment(context, it)
            currentAttachment = attachment
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        containerColor = BgDark,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .glassmorphic(cornerRadius = 12.dp, bg = Color(0x308B5CF6), border = ElectricCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("SalviaAIZ Pro", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("Model: $CHAT_MODEL", color = TextSecondary, fontSize = 11.sp)
                    }
                }

                IconButton(
                    onClick = { messages.clear() },
                    modifier = Modifier.glassmorphic(cornerRadius = 14.dp, bg = Color(0x18FFFFFF))
                ) {
                    Icon(Icons.Outlined.DeleteSweep, "Clear", tint = TextSecondary, modifier = Modifier.size(20.dp))
                }
            }
        },
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                // نمایش پیش‌نمایش فایل انتخاب شده
                currentAttachment?.let { att ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .glassmorphic(cornerRadius = 16.dp, bg = Color(0x251E1035), border = ElectricCyan)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (att.isImage) Icons.Filled.Image else Icons.Filled.InsertDriveFile,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(att.name, color = PureWhite, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${att.mimeType} • ${att.sizeString}", color = TextSecondary, fontSize = 11.sp)
                        }
                        IconButton(onClick = { currentAttachment = null }, modifier = Modifier.size(26.dp)) {
                            Icon(Icons.Filled.Close, "Remove", tint = Color.Red, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // نوار ورودی شیشه‌ای
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphic(cornerRadius = 28.dp, bg = Color(0x221A182E), border = Color(0x408B5CF6))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { filePicker.launch("*/*") },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(Icons.Filled.AddCircle, "Attach", tint = ElectricCyan, modifier = Modifier.size(26.dp))
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Ask anything or request a document...", color = TextSecondary, fontSize = 14.sp) },
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp),
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )

                    FloatingActionButton(
                        onClick = {
                            if ((inputText.isNotBlank() || currentAttachment != null) && !isLoading) {
                                val userText = inputText.trim()
                                val attachment = currentAttachment
                                messages.add(ChatMessage(text = userText, isUser = true, attachment = attachment))
                                inputText = ""
                                currentAttachment = null
                                isLoading = true

                                scope.launch {
                                    val botMsgId = UUID.randomUUID().toString()
                                    messages.add(ChatMessage(id = botMsgId, text = "", isUser = false))

                                    streamUniversalZaiChat(
                                        context = context,
                                        userText = userText,
                                        attachment = attachment,
                                        apiKey = apiKey
                                    ) { chunk ->
                                        val idx = messages.indexOfFirst { it.id == botMsgId }
                                        if (idx != -1) {
                                            val newText = messages[idx].text + chunk
                                            val artifacts = parseArtifacts(newText)
                                            messages[idx] = messages[idx].copy(text = newText, artifacts = artifacts)
                                        }
                                    }
                                    isLoading = false
                                }
                            }
                        },
                        containerColor = if (isLoading) Color(0x508B5CF6) else NeonViolet,
                        contentColor = PureWhite,
                        shape = CircleShape,
                        modifier = Modifier.size(44.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), color = PureWhite, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Send, "Send", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (messages.isEmpty()) {
                EmptyStateWelcome()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        ChatMessageItem(
                            message = msg,
                            onPreviewArtifact = { previewArtifact = it },
                            onDownloadArtifact = { artifact ->
                                saveArtifactToStorage(context, artifact)
                            }
                        )
                    }
                }
            }

            // مودال پیش‌نمایش زنده سند/فایل (In-App Document Preview Modal)
            previewArtifact?.let { art ->
                ArtifactPreviewDialog(
                    artifact = art,
                    onDismiss = { previewArtifact = null },
                    onDownload = { saveArtifactToStorage(context, art) }
                )
            }
        }
    }
}

// --- کامپوننت حباب پیام با نمایش انواع فایل و کارت‌های دانلود داکیومنت ---
@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onPreviewArtifact: (GeneratedArtifact) -> Unit,
    onDownloadArtifact: (GeneratedArtifact) -> Unit
) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.End else Alignment.Start

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        // ۱. نمایش فایل پیوست شده توسط کاربر
        message.attachment?.let { att ->
            if (att.isImage) {
                AsyncImage(
                    model = att.uri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(200.dp)
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, Color(0x4006B6D4), RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Row(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .padding(bottom = 6.dp)
                        .glassmorphic(cornerRadius = 14.dp, bg = Color(0x301E1035), border = ElectricCyan)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.InsertDriveFile, null, tint = ElectricCyan, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(att.name, color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(att.sizeString, color = TextSecondary, fontSize = 10.sp)
                    }
                }
            }
        }

        // ۲. متن پیام
        if (message.text.isNotBlank()) {
            Box(
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .glassmorphic(
                        cornerRadius = 20.dp,
                        bg = if (isUser) DeepViolet.copy(alpha = 0.85f) else Color(0x281C1A36),
                        border = if (isUser) NeonViolet else Color(0x308B5CF6)
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    text = cleanArtifactCodeBlocks(message.text),
                    color = PureWhite,
                    fontSize = 14.5.sp,
                    lineHeight = 21.sp
                )
            }
        }

        // ۳. کارت‌های داکیومنت و فایل‌های خروجی هوش مصنوعی (PDF, Code, Word, Text)
        if (message.artifacts.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            message.artifacts.forEach { artifact ->
                ArtifactDownloadCard(
                    artifact = artifact,
                    onPreview = { onPreviewArtifact(artifact) },
                    onDownload = { onDownloadArtifact(artifact) }
                )
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

// کارت شکیل دانلود و پیش‌نمایش فایل
@Composable
fun ArtifactDownloadCard(
    artifact: GeneratedArtifact,
    onPreview: () -> Unit,
    onDownload: () -> Unit
) {
    Row(
        modifier = Modifier
            .widthIn(max = 310.dp)
            .glassmorphic(cornerRadius = 16.dp, bg = Color(0x35100E26), border = ElectricCyan)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .glassmorphic(cornerRadius = 10.dp, bg = Color(0x3006B6D4), border = ElectricCyan),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (artifact.extension) {
                        "pdf" -> Icons.Filled.PictureAsPdf
                        "doc", "docx" -> Icons.Filled.Description
                        "html", "js", "py", "kt", "json" -> Icons.Filled.Code
                        else -> Icons.Filled.InsertDriveFile
                    },
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(
                    artifact.title,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "${artifact.type.uppercase()} File • Ready to View",
                    color = CyberMint,
                    fontSize = 10.sp
                )
            }
        }

        Row {
            IconButton(onClick = onPreview, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Visibility, "Preview", tint = ElectricCyan, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = onDownload, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.Download, "Download", tint = CyberMint, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// --- استودیوی دوگانه تولید و ادیت تصویر (Text-to-Image & Image-to-Image) ---
@Composable
fun ImageAndEditStudioScreen(apiKey: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(0) } // 0: Create, 1: Edit/Remix
    var prompt by remember { mutableStateOf("") }
    var selectedSize by remember { mutableStateOf("1024x1024") }
    var isLoading by remember { mutableStateOf(false) }
    var generatedImageUrl by remember { mutableStateOf<String?>(null) }
    var editBaseImageUri by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val editImagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        editBaseImageUri = uri?.toString()
    }

    val sizes = listOf("1024x1024" to "1:1 Square", "768x1344" to "9:16 Tall", "1344x768" to "16:9 Wide")

    Scaffold(
        containerColor = BgDark,
        topBar = {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("AI Studio", color = PureWhite, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Spacer(Modifier.height(10.dp))
                // تب انتخاب حالت
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .glassmorphic(cornerRadius = 16.dp, bg = Color(0x20FFFFFF), border = Color(0x308B5CF6))
                        .padding(4.dp)
                ) {
                    listOf("Generate Image", "Edit & Remix").forEachIndexed { index, title ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) NeonViolet else Color.Transparent)
                                .clickable { selectedTab = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                title,
                                color = if (isSelected) PureWhite else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (selectedTab == 1) {
                // بخش انتخاب تصویر مبدا برای ادیت
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .glassmorphic(cornerRadius = 20.dp, bg = Color(0x18FFFFFF), border = ElectricCyan)
                        .clickable { editImagePicker.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (editBaseImageUri != null) {
                        AsyncImage(
                            model = editBaseImageUri,
                            contentDescription = "Source",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.AddPhotoAlternate, null, tint = ElectricCyan, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(6.dp))
                            Text("Select Base Image to Edit / Remix", color = PureWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it },
                placeholder = {
                    Text(
                        if (selectedTab == 0) "Describe your dream image in vivid detail..."
                        else "Describe how to transform this image (e.g., make it cyberpunk, add neon wings)...",
                        color = TextSecondary,
                        fontSize = 13.5.sp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 90.dp, max = 130.dp),
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite,
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = Color(0x358B5CF6),
                    focusedContainerColor = Color(0x14FFFFFF),
                    unfocusedContainerColor = Color(0x0AFFFFFF)
                )
            )

            Spacer(Modifier.height(14.dp))

            // انتخاب نسبت تصویر
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(sizes) { (size, label) ->
                    val isSelected = selectedSize == size
                    Box(
                        modifier = Modifier
                            .glassmorphic(
                                cornerRadius = 14.dp,
                                bg = if (isSelected) DeepViolet.copy(alpha = 0.8f) else Color(0x14FFFFFF),
                                border = if (isSelected) ElectricCyan else Color(0x20FFFFFF)
                            )
                            .clickable { selectedSize = size }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            label,
                            color = if (isSelected) PureWhite else TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = {
                    if (prompt.isNotBlank() && !isLoading) {
                        isLoading = true
                        generatedImageUrl = null
                        errorMessage = null
                        scope.launch {
                            val res = executeZaiImagePipeline(
                                context = context,
                                prompt = prompt.trim(),
                                size = selectedSize,
                                baseImageUri = if (selectedTab == 1) editBaseImageUri else null,
                                apiKey = apiKey
                            )
                            if (res.startsWith("http")) {
                                generatedImageUrl = res
                            } else {
                                errorMessage = res
                            }
                            isLoading = false
                        }
                    }
                },
                enabled = prompt.isNotBlank() && !isLoading,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonViolet,
                    disabledContainerColor = Color(0x308B5CF6)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = PureWhite, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Rendering via CogView Engine...", color = PureWhite)
                } else {
                    Icon(Icons.Filled.AutoAwesome, null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (selectedTab == 0) "Generate Artwork" else "Apply Transformation", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                }
            }

            Spacer(Modifier.height(20.dp))

            // باکس نمایش نتیجه تولید عکس
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .glassmorphic(cornerRadius = 24.dp, bg = Color(0x12FFFFFF), border = Color(0x308B5CF6)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
                        LinearProgressIndicator(
                            color = ElectricCyan,
                            trackColor = Color(0x308B5CF6),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(Modifier.height(14.dp))
                        Text("Synthesizing pixels with ultra quality...", color = TextSecondary, fontSize = 13.sp)
                    }
                } else if (generatedImageUrl != null) {
                    Column(Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = generatedImageUrl,
                            contentDescription = "Artwork",
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                            contentScale = ContentScale.Fit
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0x800A0914))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = {
                                    downloadImageDirectly(context, generatedImageUrl!!)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberMint)
                            ) {
                                Icon(Icons.Filled.Download, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Download Image", fontSize = 12.sp)
                            }
                            Button(
                                onClick = {
                                    shareImageLink(context, generatedImageUrl!!)
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                            ) {
                                Icon(Icons.Filled.Share, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Share", fontSize = 12.sp)
                            }
                        }
                    }
                } else if (errorMessage != null) {
                    Text(errorMessage ?: "", color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Brush, null, tint = TextSecondary, modifier = Modifier.size(54.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Your rendered art will appear here", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// --- پیش‌نمایش زنده اسناد و فایل‌های متنی داخل اپلیکیشن ---
@Composable
fun ArtifactPreviewDialog(
    artifact: GeneratedArtifact,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .fillMaxHeight(0.80f)
            .glassmorphic(cornerRadius = 24.dp, bg = BgCardDark, border = ElectricCyan),
        confirmButton = {
            Button(
                onClick = {
                    onDownload()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyberMint),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Filled.Download, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Save to Files")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(artifact.content))
                    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Copy Content", color = ElectricCyan)
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", color = TextSecondary)
                }
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Description, null, tint = ElectricCyan)
                Spacer(Modifier.width(8.dp))
                Text(artifact.title, color = PureWhite, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .glassmorphic(cornerRadius = 14.dp, bg = Color(0x20000000), border = Color(0x20FFFFFF))
                    .padding(12.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = artifact.content,
                    color = PureWhite,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 19.sp
                )
            }
        }
    )
}

// --- صفحه تنظیمات ---
@Composable
fun SettingsScreen(apiKey: String, onUpdateKey: (String) -> Unit, onResetKey: () -> Unit) {
    var tempKey by remember { mutableStateOf(apiKey) }
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Settings & Engine", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PureWhite)
        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .glassmorphic(cornerRadius = 20.dp, bg = Color(0x18FFFFFF), border = Color(0x308B5CF6))
                .padding(18.dp)
        ) {
            Text("Active API Key", color = TextSecondary, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = tempKey,
                onValueChange = { tempKey = it },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite,
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = Color(0x308B5CF6)
                )
            )
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    onUpdateKey(tempKey.trim())
                    Toast.makeText(context, "API Key updated successfully!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Key")
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onResetKey,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Logout, null)
            Spacer(Modifier.width(8.dp))
            Text("Logout / Reset All Data")
        }
    }
}

@Composable
fun EmptyStateWelcome() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .glassmorphic(cornerRadius = 22.dp, bg = Color(0x208B5CF6), border = ElectricCyan),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Psychology, null, tint = ElectricCyan, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Ready to Create & Solve", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PureWhite)
            Spacer(Modifier.height(6.dp))
            Text(
                "Ask questions, upload documents & images, or request full PDF/Code artifacts.",
                color = TextSecondary,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// --- موتور شبکه مالتی‌مدال و استریمینگ اختصاصی Z.ai ---
suspend fun streamUniversalZaiChat(
    context: Context,
    userText: String,
    attachment: FileAttachment?,
    apiKey: String,
    onToken: (String) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("model", CHAT_MODEL)
                put("stream", true)
                val messagesArray = JSONArray()

                val userObj = JSONObject().put("role", "user")
                if (attachment != null) {
                    val contentArray = JSONArray()
                    if (userText.isNotBlank()) {
                        contentArray.put(JSONObject().put("type", "text").put("text", userText))
                    }
                    if (attachment.isImage) {
                        val base64Img = uriToBase64(context, Uri.parse(attachment.uri))
                        if (base64Img != null) {
                            contentArray.put(
                                JSONObject().put("type", "image_url").put(
                                    "image_url", JSONObject().put("url", "data:${attachment.mimeType};base64,$base64Img")
                                )
                            )
                        }
                    } else {
                        val fileText = extractFileTextContent(context, Uri.parse(attachment.uri))
                        val promptWithFile = "[Attached Document: ${attachment.name}]\n\n$fileText\n\nUser Question: $userText"
                        contentArray.put(JSONObject().put("type", "text").put("text", promptWithFile))
                    }
                    userObj.put("content", contentArray)
                } else {
                    userObj.put("content", userText)
                }

                messagesArray.put(userObj)
                put("messages", messagesArray)
            }.toString()

            val body = jsonBody.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://api.z.ai/api/paas/v4/chat/completions")
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Accept", "text/event-stream")
                .post(body)
                .build()

            globalOkHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val errorStr = response.body?.string() ?: "HTTP ${response.code}"
                    withContext(Dispatchers.Main) { onToken("\n[Error: $errorStr]") }
                    return@withContext
                }

                val source = response.body?.source() ?: return@withContext
                while (!source.exhausted()) {
                    val line = source.readUtf8Line() ?: continue
                    if (line.startsWith("data:")) {
                        val data = line.removePrefix("data:").trim()
                        if (data == "[DONE]") break
                        try {
                            val json = JSONObject(data)
                            val choices = json.optJSONArray("choices")
                            if (choices != null && choices.length() > 0) {
                                val chunk = choices.getJSONObject(0).optJSONObject("delta")?.optString("content") ?: ""
                                if (chunk.isNotEmpty()) {
                                    withContext(Dispatchers.Main) { onToken(chunk) }
                                }
                            }
                        } catch (_: Exception) { }
                    }
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) { onToken("\n[Connection Error: ${e.localizedMessage}]") }
        }
    }
}

// پایپ‌لاین تولید و ادیت تصویر
suspend fun executeZaiImagePipeline(
    context: Context,
    prompt: String,
    size: String,
    baseImageUri: String?,
    apiKey: String
): String {
    return withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("model", IMAGE_MODEL)
                put("prompt", prompt)
                put("size", size)
                if (baseImageUri != null) {
                    val base64Img = uriToBase64(context, Uri.parse(baseImageUri))
                    if (base64Img != null) {
                        put("image_url", "data:image/jpeg;base64,$base64Img")
                    }
                }
            }.toString()

            val body = jsonBody.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://api.z.ai/api/paas/v4/images/generations")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(body)
                .build()

            globalOkHttpClient.newCall(request).execute().use { response ->
                val res = response.body?.string() ?: ""
                val json = JSONObject(res)
                if (json.has("data")) {
                    json.getJSONArray("data").getJSONObject(0).getString("url")
                } else if (json.has("error")) {
                    json.getJSONObject("error").optString("message")
                } else {
                    "Failed to render image."
                }
            }
        } catch (e: Exception) {
            "Network error: ${e.localizedMessage}"
        }
    }
}

// --- استخراج خودکار Artifacts و فایل‌ها از خروجی چت ---
fun parseArtifacts(text: String): List<GeneratedArtifact> {
    val artifacts = mutableListOf<GeneratedArtifact>()
    // الگوی شناسایی بلاک‌های کد نام‌گذاری شده و اسناد: ```language:filename.ext ... ``` یا ```ext ... ```
    val pattern = Pattern.compile("```([a-zA-Z0-9_-]+)?(?::([a-zA-Z0-9_.-]+))?\\s*([\\s\\S]*?)```")
    val matcher = pattern.matcher(text)

    var count = 1
    while (matcher.find()) {
        val langOrExt = matcher.group(1)?.lowercase() ?: "txt"
        val customName = matcher.group(2)
        val content = matcher.group(3)?.trim() ?: ""

        if (content.length > 30) { // اگر محتوا واقعاً یک فایل باشد
            val fileName = customName ?: "generated_file_$count.$langOrExt"
            val type = when (langOrExt) {
                "pdf" -> "PDF Document"
                "docx", "doc" -> "Word Document"
                "kt", "java", "py", "js", "html", "css", "json" -> "Source Code"
                else -> "Document"
            }
            artifacts.add(GeneratedArtifact(title = fileName, content = content, extension = langOrExt, type = type))
            count++
        }
    }
    return artifacts
}

fun cleanArtifactCodeBlocks(text: String): String {
    return text.replace(Regex("```([a-zA-Z0-9_-]+)?(?::([a-zA-Z0-9_.-]+))?\\s*([\\s\\S]*?)```"), "[📄 Artifact Ready Below]")
}

// ذخیره سند در حافظه و ارسال به سایر اپلیکیشن‌ها
fun saveArtifactToStorage(context: Context, artifact: GeneratedArtifact) {
    try {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(downloadsDir, artifact.title)
        FileOutputStream(file).use { out ->
            out.write(artifact.content.toByteArray())
        }
        Toast.makeText(context, "Saved to Downloads/${artifact.title}", Toast.LENGTH_LONG).show()

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, artifact.content)
            putExtra(Intent.EXTRA_TITLE, artifact.title)
        }
        context.startActivity(Intent.createChooser(sendIntent, "Share ${artifact.title}"))
    } catch (e: Exception) {
        Toast.makeText(context, "Error saving: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun downloadImageDirectly(context: Context, url: String) {
    try {
        val request = DownloadManager.Request(Uri.parse(url))
            .setTitle("SalviaAIZ Art")
            .setDescription("Downloading generated artwork...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "SalviaAIZ_${System.currentTimeMillis()}.png")

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        manager.enqueue(request)
        Toast.makeText(context, "Download started...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun shareImageLink(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Look at this image generated by SalviaAIZ:\n$url")
    }
    context.startActivity(Intent.createChooser(intent, "Share Image"))
}

// یوتیلیتی‌های استخراج فایل
fun extractFileAttachment(context: Context, uri: Uri): FileAttachment {
    var name = "Attachment"
    var size = "Unknown"
    val contentResolver = context.contentResolver
    val mimeType = contentResolver.getType(uri) ?: "application/octet-stream"

    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIdx != -1) name = cursor.getString(nameIdx)
            if (sizeIdx != -1) {
                val bytes = cursor.getLong(sizeIdx)
                size = if (bytes > 1024 * 1024) "${bytes / (1024 * 1024)} MB" else "${bytes / 1024} KB"
            }
        }
    }
    val isImg = mimeType.startsWith("image/")
    return FileAttachment(uri.toString(), name, mimeType, size, isImg)
}

fun uriToBase64(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val bytes = stream.readBytes()
            Base64.encodeToString(bytes, Base64.NO_WRAP)
        }
    } catch (_: Exception) {
        null
    }
}

fun extractFileTextContent(context: Context, uri: Uri): String {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader().use { it.readText().take(12000) } // خواندن امن تا ۱۲ هزار کاراکتر اول سند
        } ?: "[Empty File]"
    } catch (_: Exception) {
        "[Unable to read raw text from document]"
    }
}
