package com.novavpn.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.novavpn.app.config.ConfigRepository
import com.novavpn.app.config.ConnectionStateManager
import com.novavpn.app.config.ImportConfigScreen
import com.novavpn.app.config.ServerListScreen
import com.novavpn.app.ui.theme.NovaVPNTheme
import com.novavpn.app.vpn.VpnConnectionController
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NovaVPNTheme {
                NovaVpnApp()
            }
        }
    }
}

enum class AppScreen {
    HOME,
    LOCATION,
    SETTINGS,
    THEME,
    AUTO_CONNECT,
    IMPORT_CONFIG,
    SERVER_LIST,
    AD
}

private const val ADSTERRA_SUPPORT_URL = "https://asiafilm.org/4/0a72bc9d378be74f04a1ec6cc70cc6e7"

@Composable
fun NovaVpnApp() {
    val context = LocalContext.current
    val connectionManager = remember { ConnectionStateManager(context) }
    val configRepository = remember { ConfigRepository(context) }
    val vpnController = remember { VpnConnectionController(context as android.app.Activity, connectionManager) }

    var currentScreen by rememberSaveable { mutableStateOf(AppScreen.HOME) }
    var connected by rememberSaveable { mutableStateOf(connectionManager.isConnected()) }
    var selectedServerLabel by rememberSaveable { mutableStateOf(connectionManager.getSelectedServerLabel()) }
    var themeMode by rememberSaveable { mutableStateOf("Dark") }
    var autoConnect by rememberSaveable { mutableStateOf(false) }
    var killSwitch by rememberSaveable { mutableStateOf(true) }
    var adGateOpen by rememberSaveable { mutableStateOf(false) }
    var adCountdown by rememberSaveable { mutableStateOf(5) }
    var adCanClose by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(adGateOpen) {
        if (!adGateOpen) {
            adCountdown = 5
            adCanClose = false
            return@LaunchedEffect
        }

        adCountdown = 5
        adCanClose = false
        while (adCountdown > 0 && adGateOpen) {
            delay(1000)
            adCountdown -= 1
        }
        if (adGateOpen) {
            adCanClose = true
        }
    }

    LaunchedEffect(connectionManager.isConnected()) {
        connected = connectionManager.isConnected()
        selectedServerLabel = connectionManager.getSelectedServerLabel()
    }

    if (adGateOpen) {
        SponsoredAdScreen(
            countdownSeconds = adCountdown,
            canClose = adCanClose,
            onClose = {
                if (selectedServerLabel != "No server selected") {
                    adGateOpen = false
                    connected = vpnController.startSelectedConnection()
                    currentScreen = AppScreen.HOME
                }
            },
            onOpenSupport = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ADSTERRA_SUPPORT_URL))
                context.startActivity(intent)
            }
        )
        return
    }

    when (currentScreen) {
        AppScreen.HOME -> HomeScreen(
            connected = connected,
            selectedServerLabel = selectedServerLabel,
            onConnectToggle = {
                if (!connected && !adGateOpen) {
                    if (selectedServerLabel == "No server selected") {
                        currentScreen = AppScreen.SERVER_LIST
                        return@HomeScreen
                    }
                    adGateOpen = true
                    adCountdown = 5
                    adCanClose = false
                } else if (connected) {
                    vpnController.stopConnection()
                    connected = false
                }
            },
            onOpenLocation = { currentScreen = AppScreen.LOCATION },
            onOpenSettings = { currentScreen = AppScreen.SETTINGS },
            onOpenTheme = { currentScreen = AppScreen.THEME },
            onOpenAutoConnect = { currentScreen = AppScreen.AUTO_CONNECT },
            onOpenAd = { currentScreen = AppScreen.AD }
        )

        AppScreen.LOCATION -> LocationScreen(onBack = { currentScreen = AppScreen.HOME })

        AppScreen.SETTINGS -> SettingsScreen(
            onBack = { currentScreen = AppScreen.HOME },
            themeMode = themeMode,
            onTheme = { currentScreen = AppScreen.THEME },
            autoConnect = autoConnect,
            killSwitch = killSwitch,
            onToggleAuto = { autoConnect = !autoConnect },
            onToggleKill = { killSwitch = !killSwitch },
            onOpenImportConfig = { currentScreen = AppScreen.IMPORT_CONFIG },
            onOpenServerList = { currentScreen = AppScreen.SERVER_LIST },
            onOpenSupport = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ADSTERRA_SUPPORT_URL))
                context.startActivity(intent)
            }
        )

        AppScreen.THEME -> ThemeScreen(
            onBack = { currentScreen = AppScreen.SETTINGS },
            selected = themeMode,
            onSelect = {
                themeMode = it
                currentScreen = AppScreen.SETTINGS
            }
        )

        AppScreen.AUTO_CONNECT -> AutoConnectScreen(
            onBack = { currentScreen = AppScreen.SETTINGS },
            autoConnect = autoConnect,
            killSwitch = killSwitch,
            onToggleAuto = { autoConnect = !autoConnect },
            onToggleKill = { killSwitch = !killSwitch }
        )

        AppScreen.IMPORT_CONFIG -> ImportConfigScreen(onBack = { currentScreen = AppScreen.SETTINGS })

        AppScreen.SERVER_LIST -> ServerListScreen(
            onBack = { currentScreen = AppScreen.SETTINGS },
            onServerSelected = { selected ->
                connectionManager.saveSelectedServer(selected)
                selectedServerLabel = selected.name ?: selected.host
                connected = false
                currentScreen = AppScreen.HOME
            }
        )

        AppScreen.AD -> SponsoredAdScreen(
            countdownSeconds = adCountdown,
            canClose = adCanClose,
            onClose = {
                if (selectedServerLabel != "No server selected") {
                    connected = vpnController.startSelectedConnection()
                }
                currentScreen = AppScreen.HOME
                adGateOpen = false
                adCountdown = 5
                adCanClose = false
            },
            onOpenSupport = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(ADSTERRA_SUPPORT_URL))
                context.startActivity(intent)
            }
        )
    }
}

@Composable
fun HomeScreen(
    connected: Boolean,
    selectedServerLabel: String,
    onConnectToggle: () -> Unit,
    onOpenLocation: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenTheme: () -> Unit,
    onOpenAutoConnect: () -> Unit,
    onOpenAd: () -> Unit
) {
    PhoneFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF081A2C))
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = null,
                    tint = Color(0xFFD7E7FF),
                    modifier = Modifier.size(24.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LogoMarkSmall()
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "NovaVPN",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                SmallStatusDot()
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PillButton(text = "Auto", selected = true, onClick = onOpenAutoConnect)
                LocationChip(onClick = onOpenLocation)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                NeonConnectButton(
                    connected = connected,
                    onClick = onConnectToggle
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = if (selectedServerLabel == "No server selected") "Select a server first" else "Selected: $selectedServerLabel",
                        color = Color(0xFFB5D8FF),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(0xFF0D2A45).copy(alpha = 0.75f))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color(0xFF86D8FF),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Column {
                            Text(
                                text = "Your privacy is our priority",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Secure • Fast • Reliable",
                                color = Color(0xFFB3C7E9),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                GhostTextButton("Settings", onClick = onOpenSettings)
                GhostTextButton("Theme", onClick = onOpenTheme)
                GhostTextButton("Ad", onClick = onOpenAd)
            }
        }
    }
}

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    themeMode: String,
    onTheme: () -> Unit,
    autoConnect: Boolean,
    killSwitch: Boolean,
    onToggleAuto: () -> Unit,
    onToggleKill: () -> Unit,
    onOpenImportConfig: () -> Unit,
    onOpenServerList: () -> Unit,
    onOpenSupport: () -> Unit
) {
    PhoneFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF081A2C))
                .padding(horizontal = 18.dp, vertical = 16.dp)
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
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = "Settings",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            SettingsCard(
                title = "Theme",
                subtitle = if (themeMode == "Dark") "Dark mode" else "Light mode",
                onClick = onTheme
            )

            Spacer(modifier = Modifier.height(12.dp))
            SettingsToggleCard(
                title = "Auto-Connect",
                subtitle = "Connect on app startup",
                checked = autoConnect,
                onCheckedChange = { onToggleAuto() }
            )
            Spacer(modifier = Modifier.height(12.dp))
            SettingsToggleCard(
                title = "Kill Switch",
                subtitle = "Prevent traffic leak",
                checked = killSwitch,
                onCheckedChange = { onToggleKill() }
            )

            Spacer(modifier = Modifier.height(18.dp))
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF0F2740))
                    .padding(14.dp)
            ) {
                SupportRow(label = "Import Config", onClick = onOpenImportConfig)
                HorizontalDivider(color = Color(0xFF1A3A59), thickness = 1.dp)
                SupportRow(label = "Saved Servers", onClick = onOpenServerList)
                HorizontalDivider(color = Color(0xFF1A3A59), thickness = 1.dp)
                SupportRow(label = "Support", onClick = onOpenSupport)
                HorizontalDivider(color = Color(0xFF1A3A59), thickness = 1.dp)
                SupportRow(label = "Share App", onClick = onOpenSupport)
                HorizontalDivider(color = Color(0xFF1A3A59), thickness = 1.dp)
                SupportRow(label = "Privacy Policy", onClick = onOpenSupport)
            }
        }
    }
}

@Composable
fun ThemeScreen(onBack: () -> Unit, selected: String, onSelect: (String) -> Unit) {
    PhoneFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF081A2C))
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
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = "Theme",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            ThemeOption(
                title = "Light Mode",
                subtitle = "Bright and clear",
                selected = selected == "Light",
                onClick = { onSelect("Light") }
            )
            Spacer(modifier = Modifier.height(12.dp))
            ThemeOption(
                title = "Dark Mode",
                subtitle = "Easy on your eyes",
                selected = selected == "Dark",
                onClick = { onSelect("Dark") }
            )
        }
    }
}

@Composable
fun AutoConnectScreen(
    onBack: () -> Unit,
    autoConnect: Boolean,
    killSwitch: Boolean,
    onToggleAuto: () -> Unit,
    onToggleKill: () -> Unit
) {
    PhoneFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF081A2C))
                .padding(horizontal = 18.dp, vertical = 16.dp)
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
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = "Auto / Kill Switch",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(28.dp))
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
                ShieldLogoSmall()
            }
            Spacer(modifier = Modifier.height(28.dp))

            SettingsToggleCard(
                title = "Auto Connect",
                subtitle = "Automatically connect to the best server when you open the app.",
                checked = autoConnect,
                onCheckedChange = { onToggleAuto() }
            )
            Spacer(modifier = Modifier.height(14.dp))
            SettingsToggleCard(
                title = "Kill Switch",
                subtitle = "Keep your internet safe when VPN is disconnected.",
                checked = killSwitch,
                onCheckedChange = { onToggleKill() }
            )
        }
    }
}

@Composable
fun SponsoredAdScreen(
    countdownSeconds: Int,
    canClose: Boolean,
    onClose: () -> Unit,
    onOpenSupport: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020D19))
            .padding(18.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(700.dp)
                .shadow(18.dp, RoundedCornerShape(35.dp))
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFF3B1E42),
                            Color(0xFF1B2D4B),
                            Color(0xFF4F6C89)
                        )
                    ),
                    shape = RoundedCornerShape(35.dp)
                )
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .background(Color(0xFF0B2237).copy(alpha = 0.7f), CircleShape)
                            .clickable(enabled = canClose) { onClose() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            tint = if (canClose) Color.White else Color.White.copy(alpha = 0.35f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (canClose) "Skip ad" else "Skip in ${countdownSeconds}s",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(18.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(360.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF3F2E1A),
                                    Color(0xFF2C4E68),
                                    Color(0xFF071820)
                                )
                            )
                        ),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    MountainBackdrop()
                    Column(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Explore the World",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 40.sp
                        )
                        Text(
                            text = "Without Limits",
                            color = Color.White,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 40.sp
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                        Text(
                            text = "Get the best deals, travel more, and enjoy your freedom.",
                            color = Color(0xFFE3EAFB),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                        Spacer(modifier = Modifier.height(22.dp))
                        Button(
                            onClick = onOpenSupport,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4AA7FF))
                        ) {
                            Text("Learn More", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PhoneFrame(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(720.dp)
            .shadow(18.dp, RoundedCornerShape(36.dp)),
        shape = RoundedCornerShape(36.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF071B2D)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1D3D5F))
    ) {
        content()
    }
}

@Composable
fun SearchField() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF0D2A45).copy(alpha = 0.6f))
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = Color(0xFF9FB8D8),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = "Search country...",
                color = Color(0xFF9DB7D6),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun LocationRow(item: LocationItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF0D2440))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = item.flag, fontSize = 24.sp)
            Spacer(modifier = Modifier.size(12.dp))
            Column {
                Text(item.country, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(item.city, color = Color(0xFF9BB5D9), style = MaterialTheme.typography.bodySmall)
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.SignalCellularAlt,
                contentDescription = null,
                tint = Color(0xFF82D1FF),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(item.ping, color = Color(0xFFBFD8FF), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
fun SettingsCard(title: String, subtitle: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F2740))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Color(0xFFB3C9EA), style = MaterialTheme.typography.bodyMedium)
            }
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = null,
                tint = Color(0xFF8CC9FF),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun SettingsToggleCard(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFF0F2740))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color(0xFFB3C9EA), style = MaterialTheme.typography.bodyMedium)
        }
        Switch(
            checked = checked,
            onCheckedChange = { onCheckedChange() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF4EB6FF),
                checkedTrackColor = Color(0xFF7FCBFF),
                uncheckedThumbColor = Color(0xFFB0CADF),
                uncheckedTrackColor = Color(0xFF203754)
            )
        )
    }
}

@Composable
fun SupportRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White, fontWeight = FontWeight.Medium)
        Icon(
            imageVector = Icons.Default.ArrowBack,
            contentDescription = null,
            tint = Color(0xFF8CC9FF),
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
fun ThemeOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) Color(0xFF1B3350) else Color(0xFF0D2440)
            )
            .border(
                1.dp,
                if (selected) Color(0xFF56C7FF) else Color(0xFF1E3A5A),
                RoundedCornerShape(20.dp)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Color(0xFFB5CAE3), style = MaterialTheme.typography.bodyMedium)
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF7AD0FF)
                )
            }
        }
    }
}

@Composable
fun GhostTextButton(text: String, onClick: () -> Unit) {
    Text(
        text = text,
        color = Color(0xFFB5D8FF),
        modifier = Modifier.clickable { onClick() },
        style = MaterialTheme.typography.labelLarge
    )
}

@Composable
fun PillButton(text: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) Color(0xFF0C2A45) else Color(0xFF0A2035))
            .border(1.dp, Color(0xFF3176B7), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = null,
                tint = Color(0xFF7DD7FF),
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text(text, color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun LocationChip(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFF0D2740))
            .border(1.dp, Color(0xFF2D5F8C), RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = Color(0xFF7ED4FF),
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.size(6.dp))
            Text("Select Location", color = Color.White, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun SmallStatusDot() {
    Box(
        modifier = Modifier
            .size(10.dp)
            .background(Color(0xFF5DE1FF), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.6f), CircleShape)
    )
}

@Composable
fun LogoMarkSmall() {
    Box(
        modifier = Modifier
            .size(22.dp)
            .background(
                brush = Brush.linearGradient(listOf(Color(0xFF4CC9FF), Color(0xFF7A4BFF))),
                shape = RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text("N", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun NeonConnectButton(connected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(220.dp)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFF53C8FF),
                        Color(0xFF3F99FF),
                        Color(0xFF0E1F34)
                    )
                ),
                shape = CircleShape
            )
            .border(6.dp, Color(0xFF8BE3FF).copy(alpha = 0.8f), CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .background(
                    brush = Brush.radialGradient(
                        listOf(
                            Color(0xFF0A2341),
                            Color(0xFF143A62),
                            Color(0xFF071C2D)
                        )
                    ),
                    shape = CircleShape
                )
                .border(8.dp, Color(0xFF9EDBFF).copy(alpha = 0.85f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (connected) Icons.Default.Close else Icons.Default.Shield,
                contentDescription = null,
                tint = if (connected) Color(0xFFFF5C5C) else Color(0xFF92E5FF),
                modifier = Modifier.size(72.dp)
            )
        }
    }
}

@Composable
fun ShieldLogoSmall() {
    Box(
        modifier = Modifier
            .size(180.dp)
            .background(
                brush = Brush.linearGradient(listOf(Color(0xFF6A5CF6), Color(0xFF4CC5FF))),
                shape = RoundedCornerShape(30.dp)
            )
            .border(4.dp, Color(0xFF9EC9FF), RoundedCornerShape(30.dp)),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("N", color = Color.White, fontSize = 100.sp, fontWeight = FontWeight.Bold)
            Text("VPN", color = Color.White, fontSize = 28.sp, letterSpacing = 2.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun MountainBackdrop() {
    Box(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            drawCircle(color = Color(0xFF7ED6FF).copy(alpha = 0.25f), radius = 200f, center = Offset(width * 0.5f, height * 0.2f))
            drawPath(
                path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, height * 0.85f)
                    lineTo(width * 0.18f, height * 0.66f)
                    lineTo(width * 0.33f, height * 0.82f)
                    lineTo(width * 0.52f, height * 0.56f)
                    lineTo(width * 0.72f, height * 0.86f)
                    lineTo(width * 0.88f, height * 0.62f)
                    lineTo(width, height * 0.85f)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                },
                color = Color(0xFF0B2738).copy(alpha = 0.85f)
            )
        }
    }
}

private data class LocationItem(
    val country: String,
    val city: String,
    val ping: String,
    val flag: String
)


@Composable
private fun LocationScreen(onBack: () -> Unit) {
    PhoneFrame {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF081A2C))
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White,
                    modifier = Modifier.size(22.dp).clickable { onBack() }
                )
                Spacer(modifier = Modifier.size(12.dp))
                Text(
                    text = "Locations",
                    color = Color.White,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Choose a server from Saved Servers to connect.",
                color = Color(0xFFB5D8FF),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}
