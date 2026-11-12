package app.vocablo.design

/** Colors are 0xAARRGGBB so this module stays free of Android and Compose. */
data class Palette(
    val primary: Long,
    val onPrimary: Long,
    val primaryContainer: Long,
    val onPrimaryContainer: Long,
    val secondary: Long,
    val secondaryContainer: Long,
    val onSecondaryContainer: Long,
    val tertiary: Long,
    val tertiaryContainer: Long,
    val onTertiaryContainer: Long,
    val surface: Long,
    val surfaceContainer: Long,
    val surfaceContainerHigh: Long,
    val onSurface: Long,
    val onSurfaceVariant: Long,
    val outline: Long,
)

data class AppTheme(val id: ThemeId, val displayName: String, val light: Palette, val dark: Palette)

enum class ThemeId { JADE, OCEANO, ATARDECER, LAVANDA, GRAFITO }

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Picks the palette to show for the user's theme and mode. */
fun AppTheme.paletteFor(mode: ThemeMode, systemIsDark: Boolean): Palette = when (mode) {
    ThemeMode.LIGHT -> light
    ThemeMode.DARK -> dark
    ThemeMode.SYSTEM -> if (systemIsDark) dark else light
}

object Themes {
    val default: ThemeId = ThemeId.JADE

    val all: List<AppTheme> = listOf(
        AppTheme(
            ThemeId.JADE, "Jade",
            light = Palette(
                0xFF1E6B58, 0xFFFFFFFF, 0xFFBDEBDA, 0xFF002118,
                0xFF8F5A00, 0xFFFFDDB3, 0xFF2A1800,
                0xFFA8395F, 0xFFFFD9E2, 0xFF3E0020,
                0xFFFFFFFF, 0xFFF1F5F2, 0xFFE2EAE5, 0xFF16201C, 0xFF5A6661, 0xFFDCE3DF,
            ),
            dark = Palette(
                0xFF8ED5BE, 0xFF00382B, 0xFF00513F, 0xFFAAF2DA,
                0xFFFFB95F, 0xFF5A3900, 0xFFFFDDB3,
                0xFFFFB0C6, 0xFF872248, 0xFFFFD9E2,
                0xFF111816, 0xFF1A2320, 0xFF24302B, 0xFFDCE5E0, 0xFF9DABA5, 0xFF2A3632,
            ),
        ),
        AppTheme(
            ThemeId.OCEANO, "Océano",
            light = Palette(
                0xFF1959C6, 0xFFFFFFFF, 0xFFD8E2FF, 0xFF001A43,
                0xFF00696F, 0xFF9EF1F6, 0xFF002022,
                0xFFC2412E, 0xFFFFDAD3, 0xFF3D0600,
                0xFFFFFFFF, 0xFFF0F4FA, 0xFFE0E8F4, 0xFF141B26, 0xFF556072, 0xFFD8E0EC,
            ),
            dark = Palette(
                0xFFAFC6FF, 0xFF002D6C, 0xFF0B4398, 0xFFD8E2FF,
                0xFF80D4DA, 0xFF004F53, 0xFF9EF1F6,
                0xFFFFB4A6, 0xFF8D2010, 0xFFFFDAD3,
                0xFF0F141C, 0xFF171E29, 0xFF212A37, 0xFFDDE3EE, 0xFF9AA5B6, 0xFF2A3341,
            ),
        ),
        AppTheme(
            ThemeId.ATARDECER, "Atardecer",
            light = Palette(
                0xFFB7410E, 0xFFFFFFFF, 0xFFFFDBCC, 0xFF370E00,
                0xFF7A4F9A, 0xFFF1DAFF, 0xFF2E0B49,
                0xFF8A6A00, 0xFFFFE08A, 0xFF2A2000,
                0xFFFFFFFF, 0xFFFBF2EE, 0xFFF3E2DA, 0xFF231A16, 0xFF6B5A52, 0xFFEBDDD6,
            ),
            dark = Palette(
                0xFFFFB596, 0xFF5A1B00, 0xFF7F2C00, 0xFFFFDBCC,
                0xFFDDB8FA, 0xFF5E3780, 0xFFF1DAFF,
                0xFFEBC248, 0xFF5A4700, 0xFFFFE08A,
                0xFF1A1210, 0xFF241A17, 0xFF30241F, 0xFFF0E0D9, 0xFFB5A096, 0xFF3A2C27,
            ),
        ),
        AppTheme(
            ThemeId.LAVANDA, "Lavanda",
            light = Palette(
                0xFF6246C2, 0xFFFFFFFF, 0xFFE7DEFF, 0xFF1D0061,
                0xFF9A3D78, 0xFFFFD8EC, 0xFF3D0030,
                0xFF006A60, 0xFF9EF2E3, 0xFF00201C,
                0xFFFFFFFF, 0xFFF4F1FB, 0xFFE7E2F4, 0xFF1C1A24, 0xFF5F5A6E, 0xFFE0DBEC,
            ),
            dark = Palette(
                0xFFCBBEFF, 0xFF33159A, 0xFF4A2EAA, 0xFFE7DEFF,
                0xFFFFAEDC, 0xFF7C255E, 0xFFFFD8EC,
                0xFF82D5C7, 0xFF005048, 0xFF9EF2E3,
                0xFF13111A, 0xFF1C1925, 0xFF272331, 0xFFE6E1F0, 0xFFA9A2BB, 0xFF312C3D,
            ),
        ),
        AppTheme(
            ThemeId.GRAFITO, "Grafito",
            light = Palette(
                0xFF1F1F1F, 0xFFFFFFFF, 0xFFE4E4E1, 0xFF161616,
                0xFF8C5A00, 0xFFFFE2A8, 0xFF2B1B00,
                0xFFC2185B, 0xFFFFD9E2, 0xFF3E0020,
                0xFFFFFFFF, 0xFFF3F3F2, 0xFFE6E6E4, 0xFF161616, 0xFF5E5E5B, 0xFFE0E0DD,
            ),
            dark = Palette(
                0xFFF2F2F0, 0xFF161616, 0xFF3A3A3A, 0xFFF2F2F0,
                0xFFFFC24D, 0xFF5E4200, 0xFFFFE2A8,
                0xFFFFB0C6, 0xFF872248, 0xFFFFD9E2,
                0xFF101010, 0xFF1A1A1A, 0xFF262626, 0xFFECECEA, 0xFFA3A3A0, 0xFF2F2F2F,
            ),
        ),
    )

    fun byId(id: ThemeId): AppTheme = all.first { it.id == id }
}
