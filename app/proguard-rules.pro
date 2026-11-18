# Add project specific ProGuard rules here.
-keepattributes SourceFile,LineNumberTable

# Keep Room database entities, DAO contracts, and KSP-generated implementations
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# Keep Kotlin Enum values/entries used with rememberSaveable in Compose
-keepclassmembers enum com.example.** {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Keep BroadcastReceiver & AppWidgetProvider entrypoints
-keep class com.example.notifications.** { *; }
-keep class com.example.widget.** { *; }
