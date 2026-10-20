package ua.aerostat

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import ua.aerostat.ui.AerostatApp
import java.util.Locale

class MainActivity : ComponentActivity() {

    /** Інтерфейс завжди українською, незалежно від мови системи. */
    override fun attachBaseContext(newBase: Context) {
        val locale = Locale("uk", "UA")
        Locale.setDefault(locale)
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(locale)
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }

    private var lastDark: Boolean? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            AerostatApp(onThemeResolved = { dark -> applySystemBars(dark) })
        }
    }

    private fun applySystemBars(dark: Boolean) {
        if (lastDark == dark) return
        lastDark = dark
        val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT)
        else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
    }
}
