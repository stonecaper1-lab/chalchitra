package io.chalchitra

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Feed.pixabayKey = Prefs(this).pixabayKey
        Auto.ensure(this)
        setContent { ChalchitraTheme { Root() } }
    }
}

/**
 * Puja palette: sindoor red, zari gold, ivory (the white of a laal-paar saree), indigo accents.
 * Dark mode is pure black for AMOLED, with the same red and gold glowing on it.
 */
private val LightScheme = lightColorScheme(
    primary = Color(0xFF9E1B1B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD4),
    onPrimaryContainer = Color(0xFF410002),
    secondary = Color(0xFF7A5C00),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE08A),
    onSecondaryContainer = Color(0xFF261A00),
    tertiary = Color(0xFF2E4A7D),
    onTertiary = Color.White,
    background = Color(0xFFFFFBF5),
    onBackground = Color(0xFF211A17),
    surface = Color(0xFFFFFBF5),
    onSurface = Color(0xFF211A17),
    surfaceVariant = Color(0xFFF5DEDA),
    onSurfaceVariant = Color(0xFF534340),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFBF3E8),
    surfaceContainer = Color(0xFFF7EDDF),
    surfaceContainerHigh = Color(0xFFF1E6D6),
    surfaceContainerHighest = Color(0xFFEBDFCE),
    outline = Color(0xFF85736F),
    outlineVariant = Color(0xFFD8C2BD),
    error = Color(0xFFBA1A1A),
)

private val DarkScheme = darkColorScheme(
    primary = Color(0xFFFFB4A8),
    onPrimary = Color(0xFF690003),
    primaryContainer = Color(0xFF8C1313),
    onPrimaryContainer = Color(0xFFFFDAD4),
    secondary = Color(0xFFE9C349),
    onSecondary = Color(0xFF3E2E00),
    secondaryContainer = Color(0xFF584400),
    onSecondaryContainer = Color(0xFFFFE08A),
    tertiary = Color(0xFFAEC6FF),
    onTertiary = Color(0xFF0B2F65),
    background = Color.Black,
    onBackground = Color(0xFFEDE0DC),
    surface = Color.Black,
    onSurface = Color(0xFFEDE0DC),
    surfaceVariant = Color(0xFF534340),
    onSurfaceVariant = Color(0xFFD8C2BD),
    surfaceContainerLowest = Color.Black,
    surfaceContainerLow = Color(0xFF0C0A09),
    surfaceContainer = Color(0xFF14100E),
    surfaceContainerHigh = Color(0xFF1F1A17),
    surfaceContainerHighest = Color(0xFF2A2420),
    outline = Color(0xFFA08C88),
    outlineVariant = Color(0xFF534340),
)

/** Serif headings (system Noto Serif) for a printed-poster feel; no font files shipped. */
private val Type = Typography().let { t ->
    t.copy(
        headlineSmall = t.headlineSmall.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
        titleLarge = t.titleLarge.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.Bold),
        titleMedium = t.titleMedium.copy(fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold),
    )
}

@Composable
fun ChalchitraTheme(content: @Composable () -> Unit) {
    // Big arched-feeling corners everywhere.
    val shapes = Shapes(
        extraSmall = RoundedCornerShape(12.dp),
        small = RoundedCornerShape(16.dp),
        medium = RoundedCornerShape(24.dp),
        large = RoundedCornerShape(32.dp),
        extraLarge = RoundedCornerShape(40.dp),
    )
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkScheme else LightScheme,
        shapes = shapes,
        typography = Type,
        content = content,
    )
}
