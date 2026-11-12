# Vocablo release rules. Room, kotlinx.serialization and Compose ship their own consumer rules;
# these cover what reflection or resources need beyond that.

# The backup format (core:cards) uses kotlinx.serialization; keep generated serializers.
-keepclassmembers class app.vocablo.** {
    *** Companion;
}
-keepclasseswithmembers class app.vocablo.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Broadcast receivers and the widget are named in the manifest.
-keep class app.vocablo.notify.** extends android.content.BroadcastReceiver
-keep class app.vocablo.widget.** extends android.appwidget.AppWidgetProvider
