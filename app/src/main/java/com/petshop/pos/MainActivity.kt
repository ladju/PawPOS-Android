package com.petshop.pos

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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

    private val posViewModel: PosViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PosViewModel(repository) as T
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

        val prefs = getSharedPreferences("pawpos_prefs", Context.MODE_PRIVATE)
        val savedLangCode = prefs.getString("app_language", AppLanguage.EN.code) ?: AppLanguage.EN.code
        val initialLanguage = AppLanguage.entries.firstOrNull { it.code == savedLangCode } ?: AppLanguage.EN

        setContent {
            var isDarkTheme by remember { mutableStateOf(true) }
            var appLanguage by remember { mutableStateOf(initialLanguage) }

            CompositionLocalProvider(LocalAppStrings provides stringsFor(appLanguage)) {
                PetShopPOSTheme(darkTheme = isDarkTheme) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        MainAppScreen(
                            posViewModel = posViewModel,
                            inventoryViewModel = inventoryViewModel,
                            analyticsViewModel = analyticsViewModel,
                            isDarkTheme = isDarkTheme,
                            onToggleTheme = { isDarkTheme = it },
                            appLanguage = appLanguage,
                            onSelectLanguage = { newLang ->
                                appLanguage = newLang
                                prefs.edit().putString("app_language", newLang.code).apply()
                            }
                        )
                    }
                }
            }
        }
    }
}
