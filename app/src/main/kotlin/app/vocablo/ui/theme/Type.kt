// Vocablo: Spanish flashcards for Android.
package app.vocablo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import app.vocablo.R

/**
 * Nunito, bundled with the app (FR-14). Its rounded letters suit Tuzo and read well at card sizes.
 *
 * Design: the font files ship inside the APK instead of being downloaded from Google Fonts,
 * because the app works fully offline and must look the same on phones without Play services.
 */
val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_medium, FontWeight.Medium),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold),
)

/**
 * The app's type scale: Material 3 sizes, set in Nunito, with heavier headings.
 *
 * Design: keeps Material's sizes and line heights so every existing screen keeps its layout,
 * and changes only the family and heading weights. One place sets the look for every screen.
 */
val VocabloTypography: Typography = Typography().let { base ->
    Typography(
        displayLarge = base.displayLarge.heading(),
        displayMedium = base.displayMedium.heading(),
        displaySmall = base.displaySmall.heading(),
        headlineLarge = base.headlineLarge.heading(),
        headlineMedium = base.headlineMedium.heading(),
        headlineSmall = base.headlineSmall.heading(),
        titleLarge = base.titleLarge.nunito(FontWeight.Bold),
        titleMedium = base.titleMedium.nunito(FontWeight.Bold),
        titleSmall = base.titleSmall.nunito(FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.nunito(),
        bodyMedium = base.bodyMedium.nunito(),
        bodySmall = base.bodySmall.nunito(),
        labelLarge = base.labelLarge.nunito(FontWeight.Bold),
        labelMedium = base.labelMedium.nunito(FontWeight.SemiBold),
        labelSmall = base.labelSmall.nunito(FontWeight.SemiBold),
    )
}

/** Sets a style in Nunito, keeping its weight unless one is given. */
private fun TextStyle.nunito(weight: FontWeight? = fontWeight): TextStyle = copy(fontFamily = Nunito, fontWeight = weight)

/** Display and headline styles: Nunito ExtraBold. */
private fun TextStyle.heading(): TextStyle = nunito(FontWeight.ExtraBold)
