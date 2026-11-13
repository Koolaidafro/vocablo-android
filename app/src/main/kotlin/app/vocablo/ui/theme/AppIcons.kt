// Vocablo: Spanish flashcards for Android.
package app.vocablo.ui.theme

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.composables.icons.lucide.R as Lucide

/**
 * Every icon the app shows, named for what it means in Vocablo (FR-13).
 *
 * Design: the only file that knows the icons come from Lucide. Screens ask for
 * AppIcons.Streak, not a Lucide name, so the set can be swapped in one place. Lucide was
 * chosen over Material Symbols for a more distinctive line style, and over hand-drawn icons
 * because it covers every meaning the app needs in one consistent set. The Lucide package
 * ships each icon as a vector drawable, so R8 keeps only the ones listed here.
 */
object AppIcons {
    // Navigation.
    @DrawableRes val Home = Lucide.drawable.lucide_ic_house
    @DrawableRes val Decks = Lucide.drawable.lucide_ic_layers
    @DrawableRes val Play = Lucide.drawable.lucide_ic_gamepad_2
    @DrawableRes val Tutor = Lucide.drawable.lucide_ic_message_circle
    @DrawableRes val Me = Lucide.drawable.lucide_ic_user_round

    // Actions.
    @DrawableRes val Back = Lucide.drawable.lucide_ic_arrow_left
    @DrawableRes val Close = Lucide.drawable.lucide_ic_x
    @DrawableRes val Add = Lucide.drawable.lucide_ic_plus
    @DrawableRes val Edit = Lucide.drawable.lucide_ic_pencil
    @DrawableRes val Delete = Lucide.drawable.lucide_ic_trash_2
    @DrawableRes val Settings = Lucide.drawable.lucide_ic_settings
    @DrawableRes val Send = Lucide.drawable.lucide_ic_send_horizontal
    @DrawableRes val Listen = Lucide.drawable.lucide_ic_volume_2

    // States and rewards.
    @DrawableRes val Locked = Lucide.drawable.lucide_ic_lock
    @DrawableRes val Selected = Lucide.drawable.lucide_ic_check
    @DrawableRes val QuestDone = Lucide.drawable.lucide_ic_circle_check
    @DrawableRes val QuestOpen = Lucide.drawable.lucide_ic_circle
    @DrawableRes val Streak = Lucide.drawable.lucide_ic_flame
    @DrawableRes val Freeze = Lucide.drawable.lucide_ic_snowflake
    @DrawableRes val Chest = Lucide.drawable.lucide_ic_gift
    @DrawableRes val ChestOpened = Lucide.drawable.lucide_ic_package_open
    @DrawableRes val Life = Lucide.drawable.lucide_ic_heart
    @DrawableRes val Medal = Lucide.drawable.lucide_ic_medal
}

/**
 * Draws one of [AppIcons], tinted like Material's Icon.
 *
 * @param contentDescription what the icon means, or null when a label next to it says so.
 */
@Composable
fun AppIcon(
    @DrawableRes icon: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    size: Dp = 24.dp,
) {
    Icon(painterResource(icon), contentDescription, modifier.size(size), tint)
}

/**
 * An icon on a soft rounded tile, used to lead list rows such as games and the weekly chest.
 *
 * Design: replaces the emoji the rows used before, so every row reads in the app's own colours
 * and line style instead of each phone maker's emoji art.
 */
@Composable
fun IconTile(
    @DrawableRes icon: Int,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.primaryContainer,
    content: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.medium, modifier = modifier) {
        Box(Modifier.padding(Spacing.m), contentAlignment = Alignment.Center) {
            AppIcon(icon, contentDescription = null, size = 24.dp)
        }
    }
}

/**
 * An icon followed by a short text, such as the streak count, drawn at the text's size.
 *
 * Design: replaces "emoji plus text" strings, so the icon takes the app's colours and the
 * text stays plain for screen readers.
 */
@Composable
fun IconLabel(
    @DrawableRes icon: Int,
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    tint: Color = LocalContentColor.current,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
        AppIcon(icon, contentDescription = null, tint = tint, size = with(LocalDensity.current) { style.fontSize.toDp() * ICON_TO_TEXT })
        Text(text, style = style, color = tint)
    }
}

/** How much bigger than its text an inline icon is drawn, so its strokes look as heavy as the letters. */
private const val ICON_TO_TEXT = 1.15f
