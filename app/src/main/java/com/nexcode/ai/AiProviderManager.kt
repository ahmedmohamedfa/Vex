package com.nexcode.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal data class AiProviderProfile(
    val id: String,
    val name: String,
    val endpoint: String,
    val model: String,
    val apiKey: String
)

internal class AiProviderStore(context: Context) {
    private val prefs = context.getSharedPreferences("vex_secure_providers", Context.MODE_PRIVATE)
    private val alias = "vex_provider_key_v1"

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                alias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun encrypt(value: String): String {
        if (value.isEmpty()) return ""
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(value.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        if (value.isEmpty()) return ""
        return runCatching {
            val bytes = Base64.decode(value, Base64.NO_WRAP)
            val iv = bytes.copyOfRange(0, 12)
            val payload = bytes.copyOfRange(12, bytes.size)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(128, iv))
            String(cipher.doFinal(payload), Charsets.UTF_8)
        }.getOrDefault("")
    }

    fun load(): List<AiProviderProfile> {
        val array = runCatching { JSONArray(prefs.getString("profiles", "[]")) }.getOrDefault(JSONArray())
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.optJSONObject(i) ?: continue
                add(
                    AiProviderProfile(
                        id = item.optString("id"),
                        name = item.optString("name"),
                        endpoint = item.optString("endpoint"),
                        model = item.optString("model"),
                        apiKey = decrypt(item.optString("key"))
                    )
                )
            }
        }
    }

    fun save(profiles: List<AiProviderProfile>) {
        val array = JSONArray()
        profiles.forEach { profile ->
            array.put(
                JSONObject()
                    .put("id", profile.id)
                    .put("name", profile.name)
                    .put("endpoint", profile.endpoint)
                    .put("model", profile.model)
                    .put("key", encrypt(profile.apiKey))
            )
        }
        prefs.edit().putString("profiles", array.toString()).apply()
    }
}

private fun modelsUrl(endpoint: String): String {
    val trimmed = endpoint.trim().trimEnd('/')
    return when {
        trimmed.endsWith("/chat/completions") -> trimmed.removeSuffix("/chat/completions") + "/models"
        trimmed.endsWith("/completions") -> trimmed.removeSuffix("/completions") + "models"
        else -> "$trimmed/models"
    }
}

internal suspend fun discoverProviderModels(profile: AiProviderProfile): List<String> =
    withContext(Dispatchers.IO) {
        val url = URL(modelsUrl(profile.endpoint))
        require(url.protocol == "https") { "Only HTTPS provider URLs are allowed." }
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 25000
            setRequestProperty("Authorization", "Bearer ${profile.apiKey}")
            setRequestProperty("Accept", "application/json")
        }
        try {
            val status = connection.responseCode
            val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching {
                    JSONObject(body).optJSONObject("error")?.optString("message")
                }.getOrNull().orEmpty()
                error("HTTP $status: ${message.ifBlank { body.take(300) }}")
            }
            val data = JSONObject(body).optJSONArray("data")
                ?: error("Provider response has no 'data' models array.")
            buildList {
                for (i in 0 until data.length()) {
                    val id = data.optJSONObject(i)?.optString("id").orEmpty()
                    if (id.isNotBlank()) add(id)
                }
            }.distinct().sorted()
        } finally {
            connection.disconnect()
        }
    }

@Composable
internal fun AiProviderSettingsDialog(
    initialEndpoint: String,
    initialModel: String,
    initialApiKey: String,
    onDismiss: () -> Unit,
    onApply: (AiProviderProfile) -> Unit,
    onStatus: (String) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { AiProviderStore(context) }
    val scope = rememberCoroutineScope()
    var profiles by remember { mutableStateOf(store.load()) }
    var selectedId by remember { mutableStateOf(profiles.firstOrNull()?.id.orEmpty()) }
    var name by remember { mutableStateOf("My AI Provider") }
    var endpoint by remember { mutableStateOf(initialEndpoint) }
    var model by remember { mutableStateOf(initialModel) }
    var apiKey by remember { mutableStateOf(initialApiKey) }
    var expanded by remember { mutableStateOf(false) }
    var models by remember { mutableStateOf(emptyList<String>()) }
    var loading by remember { mutableStateOf(false) }

    LaunchedEffect(selectedId) {
        profiles.firstOrNull { it.id == selectedId }?.let {
            name = it.name
            endpoint = it.endpoint
            model = it.model
            apiKey = it.apiKey
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("AI Provider Manager") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().height(440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Saved providers", fontSize = 12.sp)
                Row {
                    OutlinedButton(onClick = { expanded = true }, modifier = Modifier.weight(1f)) {
                        Text(profiles.firstOrNull { it.id == selectedId }?.name ?: "New provider")
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        profiles.forEach { profile ->
                            DropdownMenuItem(
                                text = { Text(profile.name) },
                                onClick = { selectedId = profile.id; expanded = false }
                            )
                        }
                    }
                    Spacer(Modifier.width(6.dp))
                    OutlinedButton(onClick = {
                        selectedId = ""
                        name = "New Provider"
                        endpoint = "https://api.openai.com/v1/chat/completions"
                        model = ""
                        apiKey = ""
                        models = emptyList()
                    }) { Text("New") }
                }
                OutlinedTextField(name, { name = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Provider name") }, singleLine = true)
                OutlinedTextField(endpoint, { endpoint = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Chat Completions URL (HTTPS)") }, singleLine = true)
                OutlinedTextField(model, { model = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Model ID") }, singleLine = true)
                OutlinedTextField(apiKey, { apiKey = it }, modifier = Modifier.fillMaxWidth(), label = { Text("API key") }, singleLine = true, visualTransformation = PasswordVisualTransformation(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = !loading && endpoint.isNotBlank() && apiKey.isNotBlank(),
                        onClick = {
                            loading = true
                            val candidate = AiProviderProfile(selectedId.ifBlank { java.util.UUID.randomUUID().toString() }, name, endpoint, model, apiKey)
                            scope.launch {
                                runCatching { discoverProviderModels(candidate) }
                                    .onSuccess { found ->
                                        models = found
                                        onStatus("Connected. Found ${found.size} models.")
                                    }
                                    .onFailure { onStatus("Provider check failed: ${it.message}") }
                                loading = false
                            }
                        }
                    ) { Text(if (loading) "Checking…" else "Test / Load models") }
                    OutlinedButton(
                        enabled = models.isNotEmpty(),
                        onClick = { if (models.isNotEmpty()) model = models.first() }
                    ) { Text("Use first") }
                }
                if (models.isNotEmpty()) {
                    Text("Models returned by provider", fontSize = 12.sp)
                    models.take(80).forEach { modelId ->
                        TextButton(onClick = { model = modelId }) {
                            Text(modelId, color = Color(0xFF72E0C1))
                        }
                    }
                }
                Text("Keys are encrypted with Android Keystore on this device. Model discovery uses the provider's OpenAI-compatible /models endpoint.", fontSize = 10.sp)
            }
        },
        confirmButton = {
            TextButton(
                enabled = name.isNotBlank() && endpoint.startsWith("https://") && model.isNotBlank() && apiKey.isNotBlank(),
                onClick = {
                    val id = selectedId.ifBlank { java.util.UUID.randomUUID().toString() }
                    val profile = AiProviderProfile(id, name.trim(), endpoint.trim(), model.trim(), apiKey.trim())
                    profiles = (profiles.filterNot { it.id == id } + profile)
                    store.save(profiles)
                    onApply(profile)
                }
            ) { Text("Save and activate") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Close") } }
    )
}
