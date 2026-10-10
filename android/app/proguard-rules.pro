# Proguard rules for NovaVPN
# Room
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}

# Android Keystore & Security
-keep class com.novavpn.app.security.** { *; }
-keep class com.novavpn.app.domain.model.** { *; }
-keep class com.novavpn.app.data.local.** { *; }
