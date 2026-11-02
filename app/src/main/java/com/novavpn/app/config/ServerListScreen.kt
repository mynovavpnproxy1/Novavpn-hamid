package com.novavpn.app.config

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
fun ServerListScreen(
    onBack: () -> Unit,
    onServerSelected: (SavedServerProfile) -> Unit = {}
) {
    val context = LocalContext.current
    var servers by remember { mutableStateOf<List<SavedServerProfile>>(emptyList()) }

    LaunchedEffect(Unit) {
        servers = ConfigRepository(context).loadAll()
    }

    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(8.dp),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1E30))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onBack() }
                )
                Spacer(modifier = Modifier.size(10.dp))
                Text(
                    text = "Server list",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (servers.isEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF102B46))
                ) {
                    Text(
                        text = "No configured servers yet. Import a config first.",
                        color = Color(0xFFBFD7F5),
                        modifier = Modifier.padding(18.dp)
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(servers) { server ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onServerSelected(server) },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF102B46))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF7DD7FF),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.size(10.dp))
                                    Column {
                                        Text(
                                            text = server.name ?: server.host,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "${server.type} • ${server.host}:${server.port}",
                                            color = Color(0xFFB9D7F6),
                                            fontSize = 12.sp
                                        )
                                    }
                                }

                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = null,
                                    tint = Color(0xFFFFA7A7),
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clickable {
                                            ConfigRepository(context).remove(server.id)
                                            servers = ConfigRepository(context).loadAll()
                                        }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
