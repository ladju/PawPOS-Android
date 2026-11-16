package com.petshop.pos.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.petshop.pos.ui.components.formatCurrency
import com.petshop.pos.ui.theme.AppLanguage
import com.petshop.pos.ui.theme.AppStrings
import com.petshop.pos.ui.theme.LocalAppStrings
import com.petshop.pos.ui.theme.OneUiAmber
import com.petshop.pos.ui.theme.OneUiBlue
import com.petshop.pos.ui.viewmodel.AnalyticsViewModel
import com.petshop.pos.ui.viewmodel.InventoryViewModel
import com.petshop.pos.ui.viewmodel.PosViewModel

enum class MainNavigationTab(
    val icon: ImageVector
) {
    POS(Icons.Default.PointOfSale),
    INVENTORY(Icons.Default.Inventory2),
    SCANNER(Icons.Default.QrCodeScanner),
    ANALYTICS(Icons.Default.Assessment),
    SETTINGS(Icons.Default.Settings);

    fun localizedTitle(strings: AppStrings): String = when (this) {
        POS -> strings.navCashier
        INVENTORY -> strings.navInventory
        SCANNER -> strings.navScanner
        ANALYTICS -> strings.navRecap
        SETTINGS -> strings.navSettings
    }

    fun localizedSubtitle(strings: AppStrings): String = when (this) {
        POS -> strings.subCashier
        INVENTORY -> strings.subInventory
        SCANNER -> strings.subScanner
        ANALYTICS -> strings.subRecap
        SETTINGS -> strings.subSettings
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    posViewModel: PosViewModel,
    inventoryViewModel: InventoryViewModel,
    analyticsViewModel: AnalyticsViewModel,
    isDarkTheme: Boolean = true,
    onToggleTheme: (Boolean) -> Unit = {},
    appLanguage: AppLanguage = AppLanguage.EN,
    onSelectLanguage: (AppLanguage) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf(MainNavigationTab.POS) }
    val lowStockCount by inventoryViewModel.lowStockCount.collectAsState()
    val posUiState by posViewModel.uiState.collectAsState()
    val strings = LocalAppStrings.current

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            // Samsung One UI 7 / 8 Viewing Area Header
            Surface(
                color = MaterialTheme.colorScheme.background,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = selectedTab.localizedTitle(strings),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            if (lowStockCount > 0) {
                                Surface(
                                    color = OneUiAmber.copy(alpha = 0.18f),
                                    shape = CircleShape,
                                    modifier = Modifier.clickable {
                                        selectedTab = MainNavigationTab.INVENTORY
                                    }
                                ) {
                                    Text(
                                        text = "⚠️ $lowStockCount ${strings.lowLabel}",
                                        color = OneUiAmber,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = selectedTab.localizedSubtitle(strings),
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Live Cart Total Pill in Viewing Area
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = CircleShape
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = strings.cartLabel,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = formatCurrency(posUiState.grandTotal),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = OneUiBlue
                                )
                            }
                        }

                        // Quick EN / ID Language Toggle Pill
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = CircleShape,
                            modifier = Modifier
                                .height(38.dp)
                                .clip(CircleShape)
                                .clickable {
                                    onSelectLanguage(
                                        if (appLanguage == AppLanguage.EN) AppLanguage.ID else AppLanguage.EN
                                    )
                                }
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 10.dp)
                            ) {
                                Text(
                                    text = if (appLanguage == AppLanguage.ID) "🇮🇩 ID" else "🇬🇧 EN",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = OneUiBlue
                                )
                            }
                        }

                        // One UI OLED Dark / Soft Light Theme Toggle
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = CircleShape,
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .clickable { onToggleTheme(!isDarkTheme) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(text = if (isDarkTheme) "☀️" else "🌙", fontSize = 15.sp)
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Samsung One UI 7 / 8 Floating Pill Navigation Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(26.dp),
                    tonalElevation = 0.dp,
                    shadowElevation = 12.dp
                ) {
                    NavigationBar(
                        containerColor = Color.Transparent,
                        tonalElevation = 0.dp,
                        modifier = Modifier.height(64.dp)
                    ) {
                        MainNavigationTab.entries.forEach { tab ->
                            val isSelected = selectedTab == tab
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    selectedTab = tab
                                    if (tab == MainNavigationTab.ANALYTICS) {
                                        analyticsViewModel.refreshAnalytics()
                                    }
                                },
                                icon = {
                                    BadgedBox(
                                        badge = {
                                            if (tab == MainNavigationTab.INVENTORY && lowStockCount > 0) {
                                                Badge(containerColor = OneUiAmber) { Text("$lowStockCount") }
                                            } else if (tab == MainNavigationTab.POS && posUiState.totalItemsCount > 0) {
                                                Badge(containerColor = OneUiBlue) { Text("${posUiState.totalItemsCount}") }
                                            }
                                        }
                                    ) {
                                        Icon(tab.icon, contentDescription = tab.localizedTitle(strings))
                                    }
                                },
                                label = {
                                    Text(
                                        text = tab.localizedTitle(strings),
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color.White,
                                    selectedTextColor = MaterialTheme.colorScheme.primary,
                                    indicatorColor = OneUiBlue,
                                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }
                }
            }
        },
        modifier = modifier
    ) { innerPadding ->
        when (selectedTab) {
            MainNavigationTab.POS -> {
                PosScreen(
                    viewModel = posViewModel,
                    onNavigateToScanner = { selectedTab = MainNavigationTab.SCANNER },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            MainNavigationTab.INVENTORY -> {
                InventoryScreen(
                    viewModel = inventoryViewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
            MainNavigationTab.SCANNER -> {
                ScannerScreen(
                    posViewModel = posViewModel,
                    onNavigateBack = { selectedTab = MainNavigationTab.POS },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            MainNavigationTab.ANALYTICS -> {
                AnalyticsScreen(
                    viewModel = analyticsViewModel,
                    qrisMdrRate = posUiState.qrisMdrRate,
                    onRestockClick = { selectedTab = MainNavigationTab.INVENTORY },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            MainNavigationTab.SETTINGS -> {
                SettingsScreen(
                    onNavigateToInventory = { selectedTab = MainNavigationTab.INVENTORY },
                    isDarkTheme = isDarkTheme,
                    onToggleTheme = onToggleTheme,
                    appLanguage = appLanguage,
                    onSelectLanguage = onSelectLanguage,
                    qrisMdrRate = posUiState.qrisMdrRate,
                    onSelectMdrRate = { posViewModel.setQrisMdrRate(it) },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

