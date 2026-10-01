package com.example.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessMedium
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AccentColor
import com.example.model.AppSettings
import com.example.model.AppThemeMode
import com.example.model.Sensitivity
import com.example.ui.theme.SentryAmber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onBack: () -> Unit,
    onUpdateTheme: (AppThemeMode) -> Unit,
    onUpdateAccentColor: (AccentColor) -> Unit,
    onUpdateSensitivity: (Sensitivity) -> Unit,
    onUpdateDistance: (Int) -> Unit,
    onToggleFullDark: (Boolean) -> Unit,
    onToggleSound: (Boolean) -> Unit,
    onToggleVibration: (Boolean) -> Unit,
    onTestAlarm: () -> Unit,
    onReplayOnboarding: () -> Unit
) {
    BackHandler { onBack() }
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        containerColor = colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Ayarlar & Tercihler",
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("settings_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri",
                            tint = colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // THEME SELECTION SECTION
            Card(
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Palette,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Arayüz Teması",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppThemeMode.values().forEach { mode ->
                            val isSelected = settings.themeMode == mode
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) colorScheme.primary.copy(alpha = 0.18f) else colorScheme.surfaceVariant,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, colorScheme.primary) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onUpdateTheme(mode) }
                                    .testTag("theme_option_${mode.name.lowercase()}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = when (mode) {
                                            AppThemeMode.DARK -> Icons.Default.DarkMode
                                            AppThemeMode.LIGHT -> Icons.Default.LightMode
                                            AppThemeMode.SYSTEM -> Icons.Default.BrightnessMedium
                                        },
                                        contentDescription = null,
                                        tint = if (isSelected) colorScheme.primary else colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = when (mode) {
                                            AppThemeMode.DARK -> "Koyu"
                                            AppThemeMode.LIGHT -> "Açık"
                                            AppThemeMode.SYSTEM -> "Sistem"
                                        },
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 12.sp,
                                        color = if (isSelected) colorScheme.primary else colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ACCENT COLOR CUSTOMIZATION SECTION
            Card(
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ColorLens,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Vurgu Rengi",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Radar çizgileri, butonlar ve göstergeler için tema rengi:",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AccentColor.values().forEach { accent ->
                            val isSelected = settings.accentColor == accent
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) accent.darkPrimary.copy(alpha = 0.2f) else colorScheme.surfaceVariant,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, accent.darkPrimary) else null,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onUpdateAccentColor(accent) }
                                    .testTag("accent_color_${accent.name.lowercase()}")
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Surface(
                                        shape = CircleShape,
                                        color = accent.darkPrimary,
                                        modifier = Modifier.size(22.dp)
                                    ) {}
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = when (accent) {
                                            AccentColor.BLUE -> "Mavi"
                                            AccentColor.EMERALD -> "Zümrüt"
                                            AccentColor.AMBER -> "Kehribar"
                                            AccentColor.PURPLE -> "Mor"
                                        },
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 11.sp,
                                        color = if (isSelected) accent.darkPrimary else colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // SENSITIVITY SECTION
            Card(
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Speed,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Hareket Hassasiyeti",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Sensitivity.values().forEach { sensitivity ->
                        val isSelected = settings.sensitivity == sensitivity
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) colorScheme.primary.copy(alpha = 0.15f) else colorScheme.surfaceVariant,
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, colorScheme.primary) else null,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { onUpdateSensitivity(sensitivity) }
                                .testTag("sensitivity_option_${sensitivity.name.lowercase()}")
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = sensitivity.title,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = if (isSelected) colorScheme.primary else colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = sensitivity.description,
                                    fontSize = 12.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // APPROXIMATE DISTANCE SECTION
            Card(
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Yaklaşık Takip Mesafesi",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tek kameradan perspektif analiziyle yaklaşık hedef mesafe:",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(3, 5, 10, 15, 20).forEach { meters ->
                            val isSelected = settings.approximateDistanceMeters == meters
                            FilterChip(
                                selected = isSelected,
                                onClick = { onUpdateDistance(meters) },
                                label = { Text("${meters} m") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = colorScheme.primary,
                                    selectedLabelColor = colorScheme.onPrimary,
                                    containerColor = colorScheme.surfaceVariant,
                                    labelColor = colorScheme.onSurface
                                )
                            )
                        }
                    }
                }
            }

            // ALERTS & POWER TOGGLES
            Card(
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                shape = RoundedCornerShape(18.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "Alarm & Güç Tercihleri",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Sound switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "Sesli Alarm", color = colorScheme.onSurface)
                        }
                        Switch(
                            checked = settings.soundAlertEnabled,
                            onCheckedChange = onToggleSound,
                            colors = SwitchDefaults.colors(checkedThumbColor = colorScheme.primary)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Vibration switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Vibration, contentDescription = null, tint = colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(text = "Güçlü Titreşim", color = colorScheme.onSurface)
                        }
                        Switch(
                            checked = settings.vibrationEnabled,
                            onCheckedChange = onToggleVibration,
                            colors = SwitchDefaults.colors(checkedThumbColor = colorScheme.primary)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // AMOLED Full Dark Mode switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.DarkMode, contentDescription = null, tint = colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(text = "AMOLED Tam Karartma", color = colorScheme.onSurface)
                                Text(
                                    text = "Nöbet sırasında ekranı #000000 yaparak pil tasarrufu sağlar",
                                    fontSize = 11.sp,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = settings.fullDarkModeEnabled,
                            onCheckedChange = onToggleFullDark,
                            colors = SwitchDefaults.colors(checkedThumbColor = colorScheme.primary)
                        )
                    }
                }
            }

            // TEST ALARM BUTTON
            Button(
                onClick = onTestAlarm,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("test_alarm_button"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colorScheme.surfaceVariant,
                    contentColor = SentryAmber
                )
            ) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Alarmı Test Et", fontWeight = FontWeight.Bold)
            }

            // REPLAY ONBOARDING BUTTON
            OutlinedButton(
                onClick = onReplayOnboarding,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("replay_onboarding_button"),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(Icons.Default.HelpOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "Tanıtım Turunu Tekrar Başlat", fontWeight = FontWeight.SemiBold)
            }

            // PRIVACY & BATTERY DISCLAIMER
            Card(
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Gizlilik Garantisi: Kamera görüntüsü asla kaydedilmez, fotoğraflanmaz veya internete gönderilmez. Tüm görüntü analizi gerçek zamanlı olarak cihaz üzerinde donanım ivmelendiricisiyle çalışır.",
                        fontSize = 12.sp,
                        color = colorScheme.onSurfaceVariant,
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
