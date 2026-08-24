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

// --- پالت رنگی کلاس جهانی: بنفش ویولت، مشکی ابسیدین، سفید کریستالی و سیان نئونی ---
val ObsidianBlack = Color(0xFF090814)
val SurfaceDark = Color(0xFF131126)
val NeonViolet = Color(0xFF8B5CF6)
val DeepViolet = Color(0xFF6D28D9)
val ElectricCyan = Color(0xFF00F2FE)
val CyberMint = Color(0xFF10B981)
val PureWhite = Color(0xFFFFFFFF)
val TextMuted = Color(0xFF94A3B8)
val GlassSurface = Color(0x18FFFFFF)
val GlassBorder = Color(0x358B5CF6)

const val DEFAULT_CHAT_MODEL = "glm-4.6"
const val DEFAULT_IMAGE_MODEL = "cogview-3"
const val PREFS_NAME = "SalviaAIZPrefs"
const val KEY_API = "api_key"
const val KEY_MODEL = "selected_model"

// کلاینت پایدار و سریع
val okHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
}

// اکستنشن اختصاصی تم شیشه‌ای و لبه‌های یخی
fun Modifier.frostedGlass(
    cornerRadius: Dp = 22.dp,
    bg: Color = GlassSurface,
    border: Color = GlassBorder
) = this
    .clip(RoundedCornerShape(cornerRadius))
    .background(bg)
    .border(
        width = 1.dp,
        brush = Brush.linearGradient(
            listOf(border, Color(0x05FFFFFF), ElectricCyan.copy(alpha = 0.3f))
        ),
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
                    background = ObsidianBlack,
                    surface = SurfaceDark
                )
            ) {
                SalviaApp()
            }
        }
    }
}

// --- ساختار مدل‌های داده ---
data class AttachedFile(
    val uri: String,
    val name: String,
    val mimeType: String,
    val sizeLabel: String,
    val isImage: Boolean
)

data class GeneratedArtifact(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val content: String,
    val extension: String,
    val typeLabel: String
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val isUser: Boolean,
    val attachment: AttachedFile? = null,
    val artifacts: List<GeneratedArtifact> = emptyList()
)

@Composable
fun SalviaApp() {
    val nav = rememberNavController()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) }
    var apiKey by remember { mutableStateOf(prefs.getString(KEY_API, "") ?: "") }
    var selectedModel by remember { mutableStateOf(prefs.getString(KEY_MODEL, DEFAULT_CHAT_MODEL) ?: DEFAULT_CHAT_MODEL) }

    Scaffold(
        containerColor = ObsidianBlack,
        bottomBar = {
            val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route
            if (currentRoute != "welcome") {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(68.dp)
                            .frostedGlass(cornerRadius = 32.dp, bg = Color(0x35131126), border = Color(0x508B5CF6))
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
                                        tint = if (isSelected) ElectricCyan else TextMuted,
                                        modifier = Modifier.size(24.dp)
                                    )
                                },
                                label = {
                                    Text(
                                        label,
                                        color = if (isSelected) PureWhite else TextMuted,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    indicatorColor = Color(0x2000F2FE)
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
                    onSaveApiKey = { key ->
                        apiKey = key
                        prefs.edit().putString(KEY_API, key).apply()
                    },
                    onNavigateToMain = {
                        nav.navigate("chat") {
                            popUpTo("welcome") { inclusive = true }
                        }
                    }
                )
            }
            composable("chat") {
                ChatScreen(apiKey = apiKey, model = selectedModel)
            }
            composable("studio") {
                ImageAndEditStudioScreen(apiKey = apiKey)
            }
            composable("settings") {
                SettingsScreen(
                    apiKey = apiKey,
                    selectedModel = selectedModel,
                    onSaveSettings = { key, model ->
                        apiKey = key
                        selectedModel = model
                        prefs.edit().putString(KEY_API, key).putString(KEY_MODEL, model).apply()
                    },
                    onLogout = {
                        apiKey = ""
                        prefs.edit().clear().apply()
                        nav.navigate("welcome") {
                            popUpTo(nav.graph.id) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}

// --- ۱. صفحه خوش‌آمدگویی نئونی و یخی ---
@Composable
fun WelcomeScreen(apiKey: String, onSaveApiKey: (String) -> Unit, onNavigateToMain: () -> Unit) {
    var keyInput by remember { mutableStateOf(apiKey) }
    var buttonVisible by remember { mutableStateOf(false) }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) {
        delay(300)
        buttonVisible = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(ObsidianBlack, Color(0xFF1E0E38), ObsidianBlack)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
                .frostedGlass(cornerRadius = 32.dp, bg = Color(0x18FFFFFF), border = Color(0x408B5CF6))
                .padding(28.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .frostedGlass(cornerRadius = 28.dp, bg = Color(0x308B5CF6), border = ElectricCyan),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = ElectricCyan,
                    modifier = Modifier.size(50.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text("SalviaAIZ", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = PureWhite)
            Text(
                "Next-Gen Z.ai AI Companion",
                fontSize = 13.sp,
                color = ElectricCyan,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(30.dp))

            OutlinedTextField(
                value = keyInput,
                onValueChange = { keyInput = it },
                label = { Text("Z.ai API Key", color = TextMuted) },
                placeholder = { Text("Enter your Bearer Token", color = Color.Gray) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite,
                    focusedBorderColor = ElectricCyan,
                    unfocusedBorderColor = NeonViolet.copy(alpha = 0.5f),
                    focusedContainerColor = Color(0x12FFFFFF),
                    unfocusedContainerColor = Color(0x08FFFFFF)
                )
            )

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(visible = buttonVisible, enter = fadeIn(tween(400))) {
                Button(
                    onClick = {
                        keyboard?.hide()
                        onSaveApiKey(keyInput.trim())
                        onNavigateToMain()
                    },
                    enabled = keyInput.isNotBlank(),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = NeonViolet,
                        disabledContainerColor = Color(0x308B5CF6)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("Launch Workspace", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.ArrowForward, contentDescription = null, tint = PureWhite)
                }
            }
        }
    }
}

// --- ۲. صفحه چت هوشمند با پشتیبانی از انواع فایل و استخراج Artifacts ---
@Composable
fun ChatScreen(apiKey: String, model: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var inputText by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isWebSearchEnabled by remember { mutableStateOf(false) }
    var currentAttachment by remember { mutableStateOf<AttachedFile?>(null) }
    var activePreviewArtifact by remember { mutableStateOf<GeneratedArtifact?>(null) }

    val messages = remember { mutableStateListOf<ChatMessage>() }
    val listState = rememberLazyListState()

    // انتخاب تمام انواع فایل‌ها (Photo, PDF, Doc, Text, Code, Audio)
    val universalFilePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            currentAttachment = extractAttachedFile(context, it)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.lastIndex)
        }
    }

    Scaffold(
        containerColor = ObsidianBlack,
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
                            .size(40.dp)
                            .frostedGlass(cornerRadius = 12.dp, bg = Color(0x308B5CF6), border = ElectricCyan),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Bolt, contentDescription = null, tint = ElectricCyan, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("SalviaAIZ Chat", color = PureWhite, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                        Text("Active: $model", color = TextMuted, fontSize = 11.sp)
                    }
                }

                // سوییچ وب سرچ و دکمه پاکسازی
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = isWebSearchEnabled,
                        onClick = { isWebSearchEnabled = !isWebSearchEnabled },
                        label = { Text("Web", fontSize = 11.sp, color = if (isWebSearchEnabled) PureWhite else TextMuted) },
                        leadingIcon = {
                            Icon(Icons.Filled.Language, null, modifier = Modifier.size(14.dp), tint = if (isWebSearchEnabled) ElectricCyan else TextMuted)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0x4000F2FE),
                            containerColor = Color(0x15FFFFFF)
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isWebSearchEnabled,
                            borderColor = if (isWebSearchEnabled) ElectricCyan else Color(0x20FFFFFF)
                        )
                    )
                    Spacer(Modifier.width(8.dp))
                    IconButton(
                        onClick = { messages.clear() },
                        modifier = Modifier.frostedGlass(cornerRadius = 14.dp, bg = Color(0x18FFFFFF))
                    ) {
                        Icon(Icons.Outlined.DeleteSweep, "Clear", tint = TextMuted, modifier = Modifier.size(20.dp))
                    }
                }
            }
        },
        bottomBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                // نمایش فایل پیوست شده قبل از ارسال
                currentAttachment?.let { att ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .frostedGlass(cornerRadius = 16.dp, bg = Color(0x301E0E38), border = ElectricCyan)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (att.isImage) Icons.Filled.Image else Icons.Filled.InsertDriveFile,
                            contentDescription = null,
                            tint = ElectricCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(att.name, color = PureWhite, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${att.mimeType} • ${att.sizeLabel}", color = TextMuted, fontSize = 11.sp)
                        }
                        IconButton(onClick = { currentAttachment = null }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Filled.Close, "Remove", tint = Color.Red, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // فیلد ورودی شیشه‌ای
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .frostedGlass(cornerRadius = 28.dp, bg = Color(0x281A1633), border = Color(0x408B5CF6))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { universalFilePicker.launch("*/*") },
                        modifier = Modifier.size(42.dp)
                    ) {
                        Icon(Icons.Filled.AddCircle, "Attach", tint = ElectricCyan, modifier = Modifier.size(26.dp))
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = { Text("Message or ask to generate files...", color = TextMuted, fontSize = 14.sp) },
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
                                val userMsg = inputText.trim()
                                val attachment = currentAttachment
                                messages.add(ChatMessage(text = userMsg, isUser = true, attachment = attachment))
                                inputText = ""
                                currentAttachment = null
                                isLoading = true

                                scope.launch {
                                    val botMsgId = UUID.randomUUID().toString()
                                    messages.add(ChatMessage(id = botMsgId, text = "", isUser = false))

                                    streamUniversalZaiChat(
                                        context = context,
                                        model = model,
                                        userText = userMsg,
                                        attachment = attachment,
                                        webSearch = isWebSearchEnabled,
                                        apiKey = apiKey
                                    ) { chunk ->
                                        val idx = messages.indexOfFirst { it.id == botMsgId }
                                        if (idx != -1) {
                                            val newText = messages[idx].text + chunk
                                            val parsedArtifacts = extractArtifactsFromText(newText)
                                            messages[idx] = messages[idx].copy(text = newText, artifacts = parsedArtifacts)
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
                EmptyStateView()
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    items(messages, key = { it.id }) { msg ->
                        MessageBubble(
                            message = msg,
                            onPreviewArtifact = { activePreviewArtifact = it },
                            onDownloadArtifact = { artifact ->
                                saveArtifactDirectly(context, artifact)
                            }
                        )
                    }
                }
            }

            // مودال پیش‌نمایش و بازخوانی سریع فایل
            activePreviewArtifact?.let { art ->
                ArtifactPreviewModal(
                    artifact = art,
                    onDismiss = { activePreviewArtifact = null },
                    onDownload = { saveArtifactDirectly(context, art) }
                )
            }
        }
    }
}

// --- نمایش حباب پیام و کارت‌های دانلود اسناد ---
@Composable
fun MessageBubble(
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
        // ۱. فایل پیوست ارسالی کاربر
        message.attachment?.let { att ->
            if (att.isImage) {
                AsyncImage(
                    model = att.uri,
                    contentDescription = null,
                    modifier = Modifier
                        .size(190.dp)
                        .padding(bottom = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .border(1.dp, Color(0x5000F2FE), RoundedCornerShape(18.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Row(
                    modifier = Modifier
                        .widthIn(max = 280.dp)
                        .padding(bottom = 6.dp)
                        .frostedGlass(cornerRadius = 14.dp, bg = Color(0x301E0E38), border = ElectricCyan)
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Filled.InsertDriveFile, null, tint = ElectricCyan, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(att.name, color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                        Text(att.sizeLabel, color = TextMuted, fontSize = 10.sp)
                    }
                }
            }
        }

        // ۲. متن پیام هوش مصنوعی یا کاربر
        if (message.text.isNotBlank()) {
            Box(
                modifier = Modifier
                    .widthIn(max = 310.dp)
                    .frostedGlass(
                        cornerRadius = 20.dp,
                        bg = if (isUser) DeepViolet.copy(alpha = 0.85f) else Color(0x281A1633),
                        border = if (isUser) NeonViolet else Color(0x308B5CF6)
                    )
                    .padding(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text(
                    text = cleanArtifactText(message.text),
                    color = PureWhite,
                    fontSize = 14.5.sp,
                    lineHeight = 21.sp
                )
            }
        }

        // ۳. کارت دانلود و پیش‌نمایش فایل‌های تولید شده (PDF, Word, Code, Markdown)
        if (message.artifacts.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            message.artifacts.forEach { artifact ->
                DocumentArtifactCard(
                    artifact = artifact,
                    onPreview = { onPreviewArtifact(artifact) },
                    onDownload = { onDownloadArtifact(artifact) }
                )
                Spacer(Modifier.height(6.dp))
            }
        }
    }
}

// کارت شیشه‌ای دانلود و پیش‌نمایش مستقیم فایل
@Composable
fun DocumentArtifactCard(
    artifact: GeneratedArtifact,
    onPreview: () -> Unit,
    onDownload: () -> Unit
) {
    Row(
        modifier = Modifier
            .widthIn(max = 310.dp)
            .frostedGlass(cornerRadius = 16.dp, bg = Color(0x35100E26), border = ElectricCyan)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .frostedGlass(cornerRadius = 10.dp, bg = Color(0x3000F2FE), border = ElectricCyan),
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
                    "${artifact.typeLabel.uppercase()} • Direct Ready",
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

// --- ۳. استودیوی حرفه‌ای ساخت و ادیت تصویر (Text-to-Image & Image-to-Image) ---
@Composable
fun ImageAndEditStudioScreen(apiKey: String) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var activeTab by remember { mutableStateOf(0) } // 0: Text-to-Image, 1: Edit & Remix
    var promptText by remember { mutableStateOf("") }
    var chosenRatio by remember { mutableStateOf("1024x1024") }
    var isProcessing by remember { mutableStateOf(false) }
    var generatedResultUrl by remember { mutableStateOf<String?>(null) }
    var sourceImageUri by remember { mutableStateOf<String?>(null) }
    var errorFeedback by remember { mutableStateOf<String?>(null) }

    val sourceImageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        sourceImageUri = uri?.toString()
    }

    val aspectRatios = listOf(
        "1024x1024" to "1:1 Square",
        "768x1344" to "9:16 Story",
        "1344x768" to "16:9 Cinema"
    )

    Scaffold(
        containerColor = ObsidianBlack,
        topBar = {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text("AI Visual Studio", color = PureWhite, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                Spacer(Modifier.height(10.dp))
                // انتخابگر تب
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .frostedGlass(cornerRadius = 16.dp, bg = Color(0x20FFFFFF), border = Color(0x308B5CF6))
                        .padding(4.dp)
                ) {
                    listOf("Text to Image", "Edit & Remix").forEachIndexed { idx, tabTitle ->
                        val isSelected = activeTab == idx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) NeonViolet else Color.Transparent)
                                .clickable { activeTab = idx }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                tabTitle,
                                color = if (isSelected) PureWhite else TextMuted,
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
            // در حالت ویرایش: باکس انتخاب عکس منبع
            if (activeTab == 1) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp)
                        .frostedGlass(cornerRadius = 20.dp, bg = Color(0x18FFFFFF), border = ElectricCyan)
                        .clickable { sourceImageLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    if (sourceImageUri != null) {
                        AsyncImage(
                            model = sourceImageUri,
                            contentDescription = "Source",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.AddPhotoAlternate, null, tint = ElectricCyan, modifier = Modifier.size(36.dp))
                            Spacer(Modifier.height(6.dp))
                            Text("Upload Base Image to Remix", color = PureWhite, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = promptText,
                onValueChange = { promptText = it },
                placeholder = {
                    Text(
                        if (activeTab == 0) "Describe your vision in full artistic detail..."
                        else "Describe transformation (e.g., convert to cybernetic neon art)...",
                        color = TextMuted,
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

            // انتخاب ابعاد
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(aspectRatios) { (ratio, label) ->
                    val isSelected = chosenRatio == ratio
                    Box(
                        modifier = Modifier
                            .frostedGlass(
                                cornerRadius = 14.dp,
                                bg = if (isSelected) DeepViolet.copy(alpha = 0.8f) else Color(0x14FFFFFF),
                                border = if (isSelected) ElectricCyan else Color(0x20FFFFFF)
                            )
                            .clickable { chosenRatio = ratio }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            label,
                            color = if (isSelected) PureWhite else TextMuted,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            Button(
                onClick = {
                    if (promptText.isNotBlank() && !isProcessing) {
                        isProcessing = true
                        generatedResultUrl = null
                        errorFeedback = null
                        scope.launch {
                            val res = executeZaiImageEngine(
                                context = context,
                                prompt = promptText.trim(),
                                size = chosenRatio,
                                sourceImage = if (activeTab == 1) sourceImageUri else null,
                                apiKey = apiKey
                            )
                            if (res.startsWith("http")) {
                                generatedResultUrl = res
                            } else {
                                errorFeedback = res
                            }
                            isProcessing = false
                        }
                    }
                },
                enabled = promptText.isNotBlank() && !isProcessing,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonViolet,
                    disabledContainerColor = Color(0x308B5CF6)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), color = PureWhite, strokeWidth = 2.dp)
                    Spacer(Modifier.width(10.dp))
                    Text("Rendering via CogView Engine...", color = PureWhite)
                } else {
                    Icon(Icons.Filled.AutoAwesome, null, tint = ElectricCyan, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (activeTab == 0) "Generate Artwork" else "Apply Remix", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                }
            }

            Spacer(Modifier.height(20.dp))

            // باکس نتیجه رندر تصویر
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .frostedGlass(cornerRadius = 24.dp, bg = Color(0x12FFFFFF), border = Color(0x308B5CF6)),
                contentAlignment = Alignment.Center
            ) {
                if (isProcessing) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(20.dp)) {
                        LinearProgressIndicator(
                            color = ElectricCyan,
                            trackColor = Color(0x308B5CF6),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(4.dp))
                        )
                        Spacer(Modifier.height(14.dp))
                        Text("Synthesizing pixels with ultra fidelity...", color = TextMuted, fontSize = 13.sp)
                    }
                } else if (generatedResultUrl != null) {
                    Column(Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = generatedResultUrl,
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
                                .background(Color(0x90090814))
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(
                                onClick = { downloadMediaFile(context, generatedResultUrl!!) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = CyberMint)
                            ) {
                                Icon(Icons.Filled.Download, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Save Image", fontSize = 12.sp)
                            }
                            Button(
                                onClick = { shareDirectLink(context, generatedResultUrl!!) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = ElectricCyan)
                            ) {
                                Icon(Icons.Filled.Share, null, modifier = Modifier.size(16.dp), tint = ObsidianBlack)
                                Spacer(Modifier.width(6.dp))
                                Text("Share Link", fontSize = 12.sp, color = ObsidianBlack)
                            }
                        }
                    }
                } else if (errorFeedback != null) {
                    Text(errorFeedback ?: "", color = Color.Red, fontSize = 13.sp, modifier = Modifier.padding(16.dp))
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Brush, null, tint = TextMuted, modifier = Modifier.size(54.dp))
                        Spacer(Modifier.height(8.dp))
                        Text("Your rendered art will appear here", color = TextMuted, fontSize = 13.sp)
                    }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// --- ۴. مودال پیش‌نمایش و خواندن اسناد داخل خود اپلیکیشن ---
@Composable
fun ArtifactPreviewModal(
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
            .frostedGlass(cornerRadius = 24.dp, bg = SurfaceDark, border = ElectricCyan),
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
                Text("Direct Download")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    clipboard.setText(AnnotatedString(artifact.content))
                    Toast.makeText(context, "Copied to clipboard!", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Copy Raw Text", color = ElectricCyan)
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", color = TextMuted)
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
                    .frostedGlass(cornerRadius = 14.dp, bg = Color(0x30000000), border = Color(0x20FFFFFF))
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

// --- ۵. صفحه تنظیمات و شخصی‌سازی مدل ---
@Composable
fun SettingsScreen(
    apiKey: String,
    selectedModel: String,
    onSaveSettings: (String, String) -> Unit,
    onLogout: () -> Unit
) {
    var keyState by remember { mutableStateOf(apiKey) }
    var modelState by remember { mutableStateOf(selectedModel) }
    val context = LocalContext.current

    val models = listOf("glm-4.6", "glm-4-plus", "glm-4-flash")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("System Settings", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = PureWhite)
        Spacer(Modifier.height(24.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .frostedGlass(cornerRadius = 20.dp, bg = Color(0x18FFFFFF), border = Color(0x308B5CF6))
                .padding(18.dp)
        ) {
            Text("Z.ai API Key", color = TextMuted, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))
            OutlinedTextField(
                value = keyState,
                onValueChange = { keyState = it },
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

            Spacer(Modifier.height(16.dp))
            Text("Language Model Selection", color = TextMuted, fontSize = 12.sp)
            Spacer(Modifier.height(6.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(models) { m ->
                    val isChosen = modelState == m
                    Box(
                        modifier = Modifier
                            .frostedGlass(
                                cornerRadius = 12.dp,
                                bg = if (isChosen) NeonViolet else Color(0x14FFFFFF),
                                border = if (isChosen) ElectricCyan else Color(0x20FFFFFF)
                            )
                            .clickable { modelState = m }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(m, color = PureWhite, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    onSaveSettings(keyState.trim(), modelState)
                    Toast.makeText(context, "Preferences saved successfully!", Toast.LENGTH_SHORT).show()
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonViolet),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Configuration")
            }
        }

        Spacer(Modifier.height(16.dp))

        Button(
            onClick = onLogout,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Logout, null)
            Spacer(Modifier.width(8.dp))
            Text("Reset Key / Logout")
        }
    }
}

@Composable
fun EmptyStateView() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .frostedGlass(cornerRadius = 22.dp, bg = Color(0x208B5CF6), border = ElectricCyan),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.Psychology, null, tint = ElectricCyan, modifier = Modifier.size(44.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text("Ready for Next Task", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = PureWhite)
            Spacer(Modifier.height(6.dp))
            Text(
                "Upload any document, ask questions, generate files, or toggle live web search.",
                color = TextMuted,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}

// --- ۶. توابع زیرساخت شبکه و استریم SSE برای Z.ai ---
suspend fun streamUniversalZaiChat(
    context: Context,
    model: String,
    userText: String,
    attachment: AttachedFile?,
    webSearch: Boolean,
    apiKey: String,
    onToken: (String) -> Unit
) {
    withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("model", model)
                put("stream", true)

                // ابزار سرچ وب
                if (webSearch) {
                    val tools = JSONArray()
                    tools.put(JSONObject().put("type", "web_search").put("web_search", JSONObject().put("enable", true)))
                    put("tools", tools)
                }

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
                        val docText = readDocumentRawText(context, Uri.parse(attachment.uri))
                        val formattedPrompt = "=== ATTACHED FILE: ${attachment.name} ===\n$docText\n\n=== USER INSTRUCTION ===\n$userText"
                        contentArray.put(JSONObject().put("type", "text").put("text", formattedPrompt))
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

            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string() ?: "HTTP ${response.code}"
                    withContext(Dispatchers.Main) { onToken("\n[Error: $err]") }
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

// موتور ساخت و ادیت تصویر با CogView
suspend fun executeZaiImageEngine(
    context: Context,
    prompt: String,
    size: String,
    sourceImage: String?,
    apiKey: String
): String {
    return withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("model", DEFAULT_IMAGE_MODEL)
                put("prompt", prompt)
                put("size", size)
                if (sourceImage != null) {
                    val base64 = uriToBase64(context, Uri.parse(sourceImage))
                    if (base64 != null) {
                        put("image_url", "data:image/jpeg;base64,$base64")
                    }
                }
            }.toString()

            val body = jsonBody.toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("https://api.z.ai/api/paas/v4/images/generations")
                .addHeader("Authorization", "Bearer $apiKey")
                .post(body)
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                val resStr = response.body?.string() ?: ""
                val json = JSONObject(resStr)
                if (json.has("data")) {
                    json.getJSONArray("data").getJSONObject(0).getString("url")
                } else if (json.has("error")) {
                    json.getJSONObject("error").optString("message")
                } else {
                    "Rendering failed. (HTTP ${response.code})"
                }
            }
        } catch (e: Exception) {
            "Network failure: ${e.localizedMessage}"
        }
    }
}

// استخراج Artifacts و فایل‌ها از متن تولید شده
fun extractArtifactsFromText(text: String): List<GeneratedArtifact> {
    val list = mutableListOf<GeneratedArtifact>()
    val pattern = Pattern.compile("```([a-zA-Z0-9_-]+)?(?::([a-zA-Z0-9_.-]+))?\\s*([\\s\\S]*?)```")
    val matcher = pattern.matcher(text)

    var count = 1
    while (matcher.find()) {
        val langOrExt = matcher.group(1)?.lowercase() ?: "txt"
        val customName = matcher.group(2)
        val content = matcher.group(3)?.trim() ?: ""

        if (content.length > 25) {
            val fileName = customName ?: "generated_file_$count.$langOrExt"
            val type = when (langOrExt) {
                "pdf" -> "PDF Document"
                "docx", "doc" -> "Word Document"
                "kt", "java", "py", "js", "html", "css", "json" -> "Code Artifact"
                else -> "Document"
            }
            list.add(GeneratedArtifact(title = fileName, content = content, extension = langOrExt, typeLabel = type))
            count++
        }
    }
    return list
}

fun cleanArtifactText(text: String): String {
    return text.replace(Regex("```([a-zA-Z0-9_-]+)?(?::([a-zA-Z0-9_.-]+))?\\s*([\\s\\S]*?)```"), "[📄 Artifact Ready Below]")
}

// ذخیره مستقیم فایل در پوشه دانلودها
fun saveArtifactDirectly(context: Context, artifact: GeneratedArtifact) {
    try {
        val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val target = File(downloads, artifact.title)
        FileOutputStream(target).use { it.write(artifact.content.toByteArray()) }

        Toast.makeText(context, "Saved to Downloads/${artifact.title}", Toast.LENGTH_LONG).show()

        val share = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, artifact.content)
            putExtra(Intent.EXTRA_TITLE, artifact.title)
        }
        context.startActivity(Intent.createChooser(share, "Share ${artifact.title}"))
    } catch (e: Exception) {
        Toast.makeText(context, "Save error: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun downloadMediaFile(context: Context, url: String) {
    try {
        val req = DownloadManager.Request(Uri.parse(url))
            .setTitle("SalviaAIZ Image")
            .setDescription("Downloading AI render...")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            .setDestinationInExternalPublicDir(Environment.DIRECTORY_PICTURES, "SalviaAIZ_${System.currentTimeMillis()}.png")

        val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        dm.enqueue(req)
        Toast.makeText(context, "Downloading image...", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        Toast.makeText(context, "Download failed: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

fun shareDirectLink(context: Context, url: String) {
    val intent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Generated by SalviaAIZ:\n$url")
    }
    context.startActivity(Intent.createChooser(intent, "Share Link"))
}

fun extractAttachedFile(context: Context, uri: Uri): AttachedFile {
    var name = "Document"
    var size = "Unknown"
    val cr = context.contentResolver
    val mime = cr.getType(uri) ?: "application/octet-stream"

    cr.query(uri, null, null, null, null)?.use { cursor ->
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
    val isImage = mime.startsWith("image/")
    return AttachedFile(uri.toString(), name, mime, size, isImage)
}

fun uriToBase64(context: Context, uri: Uri): String? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            Base64.encodeToString(stream.readBytes(), Base64.NO_WRAP)
        }
    } catch (_: Exception) { null }
}

fun readDocumentRawText(context: Context, uri: Uri): String {
    return try {
        context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader().use { it.readText().take(15000) }
        } ?: "[Empty Content]"
    } catch (_: Exception) {
        "[Binary Document]"
    }
}
