package com.novavpn.app.config

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ImportConfigScreen(
    onBack: () -> Unit,
    onConfigImported: (ServerConfig) -> Unit
) {
    val context = LocalContext.current
    var configText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C233A))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(22.dp))
                Spacer(modifier = Modifier.size(10.dp))
                Text("Import config", color = Color.White, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }

            OutlinedTextField(
                value = configText,
                onValueChange = { configText = it; statusMessage = null; errorMessage = null },
                modifier = Modifier.fillMaxWidth().height(180.dp),
                label = { Text("Paste one or many VLESS / VMess / Trojan / Shadowsocks links") },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF102B46),
                    unfocusedContainerColor = Color(0xFF102B46),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Color(0xFF7BD0FF),
                    focusedIndicatorColor = Color(0xFF7BD0FF),
                    unfocusedIndicatorColor = Color(0xFF294E73)
                )
            )

            Button(
                onClick = {
                    val parser = ServerConfigParser()
                    val shareLinkRegex = Regex("(?i)(?:vless|vmess|trojan|ss)://[^\\s]+")
                    val links = shareLinkRegex.findAll(configText)
                        .map { it.value.trim().trimEnd(',', ';') }
                        .distinct()
                        .toList()

                    val parsed = if (links.isNotEmpty()) {
                        links.map { parser.parse(it) }
                    } else {
                        listOf(parser.parse(configText))
                    }

                    val valid = parsed.mapNotNull { (it as? ConfigParseResult.Success)?.server }
                    val failures = parsed.mapNotNull { (it as? ConfigParseResult.Failure)?.message }

                    if (valid.isEmpty()) {
                        statusMessage = null
                        errorMessage = failures.firstOrNull() ?: "هیچ لینک پشتیبانی‌شده‌ای پیدا نشد."
                    } else {
                        val repository = ConfigRepository(context)
                        val saved = valid.map { repository.addFromConfig(it) }
                        statusMessage = "${saved.size} سرور جداگانه ذخیره شد."
                        errorMessage = if (failures.isNotEmpty()) "${failures.size} مورد نامعتبر نادیده گرفته شد." else null
                        val first = valid.first()
                        onConfigImported(first.copy(name = saved.first().name ?: saved.first().host))
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4AA7FF))
            ) {
                Text("Save server(s)")
            }

            statusMessage?.let { message ->
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF123B5E), RoundedCornerShape(16.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF7CFFB2), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(message, color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
            errorMessage?.let { message ->
                Row(
                    modifier = Modifier.fillMaxWidth().background(Color(0xFF492A2E), RoundedCornerShape(16.dp)).padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Error, contentDescription = null, tint = Color(0xFFFFB3B3), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(message, color = Color(0xFFFFE5E5), style = MaterialTheme.typography.bodyMedium, fontSize = 12.sp)
                }
            }
        }
    }
}
