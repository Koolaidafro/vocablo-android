// Vocablo: Spanish flashcards for Android.
//
// The generated artwork: scenes, badges and game tiles (FR-19, FR-20, FR-22).
package app.vocablo.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.vocablo.R
import app.vocablo.play.Badge
import app.vocablo.play.Game

/**
 * The illustrated Tuzo scenes, each with the words a screen reader says for it.
 *
 * Design: an enum so screens name a scene by meaning and the drawable and its description
 * always travel together. The art itself was generated (SRS section 6) and ships as WebP.
 */
enum class Scene(@param:DrawableRes val drawable: Int, val description: String) {
    SESSION_COMPLETE(R.drawable.scene_session_complete, "Tuzo celebrating with flying flash cards"),
    ALL_DONE(R.drawable.scene_all_done, "Tuzo relaxing in a hammock"),
    WELCOME_BACK(R.drawable.scene_welcome_back, "Tuzo popping out of the burrow to say hello"),
    UPDATE_READY(R.drawable.scene_update_ready, "Tuzo carrying a gift"),
    EMPTY_DECK(R.drawable.scene_empty_deck, "Tuzo ready to write the first card"),
    REMINDERS(R.drawable.scene_notify, "Tuzo ringing a little bell"),
    ONBOARDING(R.drawable.scene_onboarding, "Tuzo walking toward a colourful Latin American town"),
}

/** Shows a [Scene] at [size], keeping its proportions. */
@Composable
fun SceneArt(scene: Scene, modifier: Modifier = Modifier, size: Dp = 180.dp) {
    Image(
        painter = painterResource(scene.drawable),
        contentDescription = scene.description,
        contentScale = ContentScale.Fit,
        modifier = modifier.size(size),
    )
}

/** The medallion drawn for each badge. */
@DrawableRes
fun badgeArt(badge: Badge): Int = when (badge) {
    Badge.FIRST_STEPS -> R.drawable.badge_first_session
    Badge.STREAK_7 -> R.drawable.badge_streak_7
    Badge.STREAK_30 -> R.drawable.badge_streak_30
    Badge.WORDS_100 -> R.drawable.badge_words_100
    Badge.GOLD_VERB -> R.drawable.badge_gold_verb
    Badge.TREASURE -> R.drawable.badge_chest
}

/**
 * A badge medallion. A locked badge is drawn grey and faded, so the learner can see what is
 * still to come without it looking earned.
 */
@Composable
fun BadgeArt(badge: Badge, earned: Boolean, modifier: Modifier = Modifier, size: Dp = 72.dp) {
    Image(
        painter = painterResource(badgeArt(badge)),
        contentDescription = if (earned) "${badge.title} badge, earned" else "${badge.title} badge, not earned yet",
        colorFilter = if (earned) null else ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) }),
        modifier = modifier.size(size).alpha(if (earned) 1f else LOCKED_ALPHA),
    )
}

/** The illustrated tile that leads each game's row on the Play screen (FR-19). */
@DrawableRes
fun gameArt(game: Game): Int = when (game) {
    Game.VERB_RAIN -> R.drawable.game_verb_rain
    Game.TUG_OF_WAR -> R.drawable.game_tug_of_war
    Game.SI_O_NO -> R.drawable.game_si_o_no
    Game.PAREJAS -> R.drawable.game_parejas
    Game.CUATRO -> R.drawable.game_cuatro
}

/** A square piece of art with rounded corners, used for the game, sentence and tutor tiles. */
@Composable
fun ArtTile(@DrawableRes art: Int, modifier: Modifier = Modifier, size: Dp = 56.dp) {
    Image(
        painter = painterResource(art),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.size(size).clip(RoundedCornerShape(size * TILE_CORNER)),
    )
}

/** How see-through a locked badge is. */
private const val LOCKED_ALPHA = 0.45f

/** Tile corner radius as a share of the tile's size, so big and small tiles look alike. */
private const val TILE_CORNER = 0.22f
