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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ImportConfigScreen(
    onBack: () -> Unit,
    onConfigImported: (ServerConfig) -> Unit
) {
    var configText by remember { mutableStateOf("") }
    var validation by remember { mutableStateOf<ConfigParseResult?>(null) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0C233A))
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = "Import config",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            OutlinedTextField(
                value = configText,
                onValueChange = { configText = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                label = { Text("Paste VLESS / VMess / Trojan / Shadowsocks / WireGuard link") },
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
                    val result = ServerConfigParser().parse(configText)
                    validation = result
                    if (result is ConfigParseResult.Success) {
                        onConfigImported(result.server)
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4AA7FF))
            ) {
                Text("Save and select config")
            }

            when (val result = validation) {
                is ConfigParseResult.Success -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF123B5E), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF7CFFB2),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Column {
                            Text(
                                text = "Valid configuration",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Type: ${result.server.type} • Host: ${result.server.host} • Port: ${result.server.port}",
                                color = Color(0xFFB9D7F6),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                is ConfigParseResult.Failure -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF492A2E), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Error,
                            contentDescription = null,
                            tint = Color(0xFFFFB3B3),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = result.message,
                            color = Color(0xFFFFE5E5),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                null -> {
                    Text(
                        text = "No config submitted yet",
                        color = Color(0xFF9FB9D8),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
