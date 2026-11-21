package com.nexcode.ai

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val Bg = Color(0xFF0B1020)
private val Panel = Color(0xFF131B2E)
private val Panel2 = Color(0xFF1A2540)
private val Accent = Color(0xFF72E0C1)
private val Muted = Color(0xFF9AA8C4)
private val Ink = Color(0xFFEAF0FF)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { VexApp(this) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VexApp(context: Context) {
    val prefs = remember { context.getSharedPreferences("vex_settings", Context.MODE_PRIVATE) }
    val providerStore = remember { AiProviderStore(context) }
    val savedProviders = remember { providerStore.load() }
    val activeSavedProvider = remember { savedProviders.lastOrNull() }
    var code by remember { mutableStateOf("// Welcome to Vex\n// Start building something great.\n\nfun main() {\n    println(\"Hello, world!\")\n}\n") }
    var fileName by remember { mutableStateOf("Untitled.kt") }
    var projectRoot by remember { mutableStateOf<DocumentFile?>(null) }
    var currentFolder by remember { mutableStateOf<DocumentFile?>(null) }
    var currentFileUri by remember { mutableStateOf<Uri?>(null) }
    var showCreateFile by remember { mutableStateOf(false) }
    var newFileName by remember { mutableStateOf("NewFile.kt") }
    var prompt by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("Your Vex AI coding partner is ready. Configure a provider in Settings to enable live AI.") }
    var aiSuggestedCode by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }
    var showSettings by remember { mutableStateOf(false) }
    var showFind by remember { mutableStateOf(false) }
    var findText by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var endpoint by remember { mutableStateOf(activeSavedProvider?.endpoint ?: (prefs.getString("endpoint", "https://api.openai.com/v1/chat/completions") ?: "")) }
    var model by remember { mutableStateOf(activeSavedProvider?.model ?: (prefs.getString("model", "gpt-4o-mini") ?: "")) }
    var apiKey by remember { mutableStateOf(activeSavedProvider?.apiKey ?: (prefs.getString("key", "") ?: "")) }
    val scope = rememberCoroutineScope()

    val openFile = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> code = reader.readText() }
                fileName = it.lastPathSegment?.substringAfterLast('/') ?: "project-file"
            }.onFailure { answer = "Could not open file: ${it.message}" }
        }
    }
    val openProject = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )
                val root = DocumentFile.fromTreeUri(context, it) ?: error("Cannot access selected folder")
                projectRoot = root
                currentFolder = root
                currentFileUri = null
                answer = "Project folder opened: ${root.name ?: "Workspace"}"
            }.onFailure { answer = "Could not open project folder: ${it.message}" }
        }
    }
    val saveFile = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/plain")) { uri: Uri? ->
        uri?.let {
            runCatching {
                context.contentResolver.openOutputStream(it)?.bufferedWriter()?.use { writer -> writer.write(code) }
                answer = "Saved $fileName successfully."
            }.onFailure { answer = "Could not save file: ${it.message}" }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Bg, surface = Panel, primary = Accent, onPrimary = Bg,
        onBackground = Ink, onSurface = Ink, secondary = Muted
    )) {
        Scaffold(containerColor = Bg, topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("VEX", color = Ink, fontSize = 18.sp)
                        Text("AI DEVELOPMENT STUDIO", color = Accent, fontSize = 9.sp, letterSpacing = 2.sp)
                    }
                },
                actions = {
                    IconButton(onClick = { showFind = true }) { Icon(Icons.Default.Search, "Find and replace", tint = Ink) }
                    IconButton(onClick = { showSettings = true }) { Icon(Icons.Default.Settings, "Settings", tint = Ink) }
                    IconButton(onClick = { openProject.launch(null) }) { Icon(Icons.Default.FolderOpen, "Open project", tint = Ink) }
                    IconButton(onClick = { saveFile.launch(fileName) }) { Icon(Icons.Default.Save, "Save", tint = Accent) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg)
            )
        }, bottomBar = {
            NavigationBar(containerColor = Panel) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Code, contentDescription = "Editor") },
                    label = { Text("Editor") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Folder, contentDescription = "Files") },
                    label = { Text("Files") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.AutoAwesome, contentDescription = "AI") },
                    label = { Text("AI") }
                )
            }
        }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp)) {
                Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = Panel2, shape = RoundedCornerShape(8.dp)) {
                        Text(fileName, Modifier.padding(horizontal = 12.dp, vertical = 8.dp), color = Ink, fontSize = 12.sp)
                    }
                    Spacer(Modifier.weight(1f))
                    Text("${code.length} chars", color = Muted, fontSize = 11.sp)
                }
                when (selectedTab) {
                    0 -> {
                    Text("EDITOR", color = Muted, fontSize = 10.sp, letterSpacing = 2.sp)
                    Surface(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        color = Panel, shape = RoundedCornerShape(12.dp)
                    ) {
                        BasicTextField(
                            value = code,
                            onValueChange = { code = it },
                            modifier = Modifier.fillMaxSize().padding(14.dp).verticalScroll(rememberScrollState()),
                            textStyle = TextStyle(color = Color(0xFFD7E3FF), fontFamily = FontFamily.Monospace, fontSize = 13.sp, lineHeight = 20.sp),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(Accent)
                        )
                    }
                    }
                    1 -> {
                        ProjectExplorer(
                            root = projectRoot,
                            currentFolder = currentFolder,
                            onPickProject = { openProject.launch(null) },
                            onFolderSelected = { currentFolder = it },
                            onFileSelected = { file ->
                                runCatching {
                                    val content = context.contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { it.readText() }
                                        ?: error("Unable to read file")
                                    code = content
                                    fileName = file.name ?: "project-file"
                                    currentFileUri = file.uri
                                    selectedTab = 0
                                    answer = "Opened ${file.name}"
                                }.onFailure { answer = "Could not open ${file.name}: ${it.message}" }
                            },
                            onCreateFile = { showCreateFile = true },
                            onSaveCurrent = {
                                val target = currentFileUri
                                if (target == null) saveFile.launch(fileName)
                                else runCatching {
                                    context.contentResolver.openOutputStream(target, "wt")?.bufferedWriter()?.use { it.write(code) }
                                        ?: error("Unable to write file")
                                    answer = "Saved $fileName"
                                }.onFailure { answer = "Save failed: ${it.message}" }
                            },
                            fileName = fileName,
                            code = code
                        )
                    }
                    2 -> {
                    Row(
                        Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, null, tint = Accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("AI ASSISTANT", color = Accent, fontSize = 11.sp, letterSpacing = 1.5.sp)
                        Spacer(Modifier.weight(1f))
                        if (busy) CircularProgressIndicator(Modifier.size(16.dp), color = Accent, strokeWidth = 2.dp)
                    }
                    Surface(color = Panel, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text(answer, color = Muted, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.heightIn(max = 90.dp).verticalScroll(rememberScrollState()))
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = prompt, onValueChange = { prompt = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Ask AI to explain, fix, or improve this code…", fontSize = 12.sp) },
                                minLines = 1, maxLines = 3,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Ink, unfocusedTextColor = Ink,
                                    focusedBorderColor = Accent, unfocusedBorderColor = Panel2,
                                    cursorColor = Accent
                                )
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Button(
                                    enabled = !busy && prompt.isNotBlank(),
                                    onClick = {
                                        if (apiKey.isBlank()) {
                                            answer = "AI is not configured yet. Open Settings and add your compatible API endpoint, model, and API key."
                                        } else {
                                            busy = true
                                            val question = prompt
                                            scope.launch {
                                                answer = runCatching { askAi(endpoint, model, apiKey, question, code) }
                                                    .getOrElse { "AI request failed: ${it.message}" }
                                                aiSuggestedCode = extractCodeBlock(answer)
                                                busy = false
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Bg)
                                ) {
                                    Icon(Icons.Default.Send, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Ask AI")
                                }
                                if (aiSuggestedCode.isNotBlank()) {
                                    OutlinedButton(onClick = {
                                        code = aiSuggestedCode
                                        aiSuggestedCode = ""
                                        answer = "AI-generated code was applied to the current document."
                                    }) { Text("Apply code") }
                                }
                            }
                        }
                    }
                    }
                }
                Text("Vex 0.2.0  •  Local-first editor  •  Multi-provider AI", color = Muted, fontSize = 9.sp, modifier = Modifier.padding(vertical = 8.dp))
            }
        }

        if (showCreateFile) {
            AlertDialog(
                onDismissRequest = { showCreateFile = false },
                containerColor = Panel,
                title = { Text("Create project file", color = Ink) },
                text = {
                    OutlinedTextField(
                        value = newFileName,
                        onValueChange = { newFileName = it },
                        label = { Text("File name") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = newFileName.isNotBlank() && !newFileName.contains("/") && currentFolder != null,
                        onClick = {
                            runCatching {
                                val created = currentFolder?.createFile("application/octet-stream", newFileName.trim())
                                    ?: error("Select a project folder first")
                                code = ""
                                fileName = created.name ?: newFileName.trim()
                                currentFileUri = created.uri
                                answer = "Created $fileName"
                                showCreateFile = false
                                selectedTab = 0
                            }.onFailure { answer = "Could not create file: ${it.message}" }
                        }
                    ) { Text("Create", color = Accent) }
                },
                dismissButton = { TextButton(onClick = { showCreateFile = false }) { Text("Cancel") } }
            )
        }

        if (showFind) {
            AlertDialog(
                onDismissRequest = { showFind = false },
                containerColor = Panel,
                title = { Text("Find and replace", color = Ink) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = findText,
                            onValueChange = { findText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Find") },
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = replaceText,
                            onValueChange = { replaceText = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Replace with") },
                            singleLine = true
                        )
                        val matches = if (findText.isEmpty()) 0 else code.windowed(findText.length).count { it == findText }
                        Text("$matches match(es) in the current file", color = Muted, fontSize = 11.sp)
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = findText.isNotEmpty() && code.contains(findText),
                        onClick = {
                            val updated = code.replace(findText, replaceText)
                            val count = (code.length - updated.length) / findText.length
                            code = updated
                            answer = "Replaced $count occurrence(s)."
                            showFind = false
                        }
                    ) { Text("Replace all", color = Accent) }
                },
                dismissButton = {
                    TextButton(onClick = { showFind = false }) { Text("Close", color = Muted) }
                }
            )
        }

        if (showSettings) {
            AiProviderSettingsDialog(
                initialEndpoint = endpoint,
                initialModel = model,
                initialApiKey = apiKey,
                onDismiss = { showSettings = false },
                onStatus = { answer = it },
                onApply = { profile ->
                    endpoint = profile.endpoint
                    model = profile.model
                    apiKey = profile.apiKey
                    prefs.edit()
                        .putString("endpoint", profile.endpoint)
                        .putString("model", profile.model)
                        .remove("key")
                        .apply()
                    showSettings = false
                    answer = "Provider '${profile.name}' saved and activated. Its API key is encrypted with Android Keystore."
                }
            )
        }
    }
}


private suspend fun askAi(endpoint: String, model: String, key: String, prompt: String, code: String): String =
    withContext(Dispatchers.IO) {
        val parsedEndpoint = URL(endpoint)
        require(parsedEndpoint.protocol == "https") { "Only HTTPS AI endpoints are allowed." }
        require(model.isNotBlank() && key.isNotBlank()) { "Model name and API key are required." }
        val connection = (parsedEndpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20000
            readTimeout = 60000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $key")
        }
        try {
            val body = JSONObject()
                .put("model", model)
                .put("messages", org.json.JSONArray()
                    .put(JSONObject().put("role", "system").put("content", "You are Vex, a concise expert coding assistant. Explain changes and provide safe, practical code."))
                    .put(JSONObject().put("role", "user").put("content", "Request: $prompt\n\nCurrent code:\n$code")))
                .put("temperature", 0.2)
            connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val detail = runCatching {
                    JSONObject(response).optJSONObject("error")?.optString("message")
                }.getOrNull()?.takeIf { !it.isNullOrBlank() } ?: response.take(600)
                error("Provider returned HTTP $status: $detail")
            }
            val choices = JSONObject(response).optJSONArray("choices")
                ?: error("The provider response did not contain a choices array.")
            if (choices.length() == 0) error("The provider returned no answer.")
            choices.getJSONObject(0).getJSONObject("message").getString("content")
        } finally {
            connection.disconnect()
        }
    }

private fun extractCodeBlock(response: String): String {
    val start = response.indexOf("```")
    if (start < 0) return ""
    val contentStart = response.indexOf('\n', start)
    if (contentStart < 0) return ""
    val end = response.indexOf("```", contentStart + 1)
    if (end < 0) return ""
    return response.substring(contentStart + 1, end).trimEnd()
}
