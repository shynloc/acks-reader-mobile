# Room — keep database classes and annotated methods
-keep class * extends androidx.room.RoomDatabase { *; }
-keepclassmembers class * { @androidx.room.* <methods>; }

# AndroidBridge — WebView JS interface names must not be obfuscated
-keepclassmembers class studio.acks.reader.PreviewWebView$Bridge {
    public *;
}
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# DataStore — keep generated classes
-keep class androidx.datastore.** { *; }

# Kotlin coroutines
-keepclassmembers class kotlinx.coroutines.** { *; }
-keep class kotlinx.coroutines.android.AndroidExceptionPreHandler { *; }

# Jetpack Compose — suppress warnings for internal APIs
-dontwarn androidx.compose.**

# Keep R8 from stripping Kotlin metadata
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepattributes SourceFile, LineNumberTable

# Keep Kotlin reflection metadata (needed for serialization, data classes)
-keep class kotlin.Metadata { *; }
