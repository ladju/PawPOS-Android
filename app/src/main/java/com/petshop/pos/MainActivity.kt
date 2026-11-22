package com.petshop.pos

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.petshop.pos.ui.screens.MainAppScreen
import com.petshop.pos.ui.theme.AppLanguage
import com.petshop.pos.ui.theme.LocalAppStrings
import com.petshop.pos.ui.theme.PetShopPOSTheme
import com.petshop.pos.ui.theme.stringsFor
import com.petshop.pos.ui.viewmodel.AnalyticsViewModel
import com.petshop.pos.ui.viewmodel.InventoryViewModel
import com.petshop.pos.ui.viewmodel.PosViewModel

class MainActivity : ComponentActivity() {

    private val repository by lazy { (application as PetShopApp).repository }
    private val settings by lazy { (application as PetShopApp).settings }

    private val posViewModel: PosViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PosViewModel(repository, settings) as T
            }
        }
    }

    private val inventoryViewModel: InventoryViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return InventoryViewModel(repository) as T
            }
        }
    }

    private val analyticsViewModel: AnalyticsViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AnalyticsViewModel(repository) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Theme and language come from the persisted SettingsStore, so they survive rotation and restarts.
            val isDarkTheme by settings.darkTheme.collectAsState()
            val languageCode by settings.languageCode.collectAsState()
            val appLanguage = AppLanguage.entries.firstOrNull { it.code == languageCode } ?: AppLanguage.EN

            CompositionLocalProvider(LocalAppStrings provides stringsFor(appLanguage)) {
                PetShopPOSTheme(darkTheme = isDarkTheme) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        MainAppScreen(
                            posViewModel = posViewModel,
                            inventoryViewModel = inventoryViewModel,
                            analyticsViewModel = analyticsViewModel,
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = { settings.setDarkTheme(it) },
                            appLanguage = appLanguage,
                            onSelectLanguage = { newLang -> settings.setLanguageCode(newLang.code) }
                        )
                    }
                }
            }
        }
    }
}
