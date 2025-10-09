# ==================== GENERAL ====================

# Keep line numbers for debugging stack traces
-keepattributes SourceFile,LineNumberTable

# Keep annotations
-keepattributes *Annotation*

# Keep generic signatures for reflection
-keepattributes Signature

# Keep exception messages
-keepattributes Exceptions

# ==================== KOTLIN ====================

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep Kotlin intrinsics
-dontwarn kotlin.**
-dontwarn kotlinx.**

# ==================== ANDROID ====================

# Keep native methods
-keepclasseswithmembernames class * {
    native <methods>;
}

# Keep custom View constructors
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet);
}
-keepclasseswithmembers class * {
    public <init>(android.content.Context, android.util.AttributeSet, int);
}

# Keep Activity and Service classes
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider

# Keep Parcelables
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# Keep Serializable classes
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ==================== YOUR APP ====================

# Keep your main application class
-keep public class it.teateonair.app.MainActivity { *; }
-keep public class it.teateonair.app.RadioService { *; }

# Keep your custom views
-keep public class it.teateonair.app.AnimatedBackgroundView { *; }
-keep public class it.teateonair.app.PlayerAnimatedBackgroundView { *; }

# ==================== ANDROIDX ====================

# AppCompat
-keep public class androidx.appcompat.widget.** { *; }
-keep public class androidx.appcompat.view.menu.** { *; }

# ConstraintLayout
-keep class androidx.constraintlayout.** { *; }
-keep interface androidx.constraintlayout.** { *; }

# LocalBroadcastManager
-keep class androidx.localbroadcastmanager.content.LocalBroadcastManager { *; }

# ==================== MEDIA ====================

# Keep media session classes
-keep class androidx.media.** { *; }

# ==================== JSOUP ====================

# Keep Jsoup classes (for web scraping)
-keep class org.jsoup.** { *; }
-keeppackagenames org.jsoup.nodes

# ==================== JSON ====================

# Keep JSON classes
-keepclassmembers class * {
    @org.json.** <fields>;
}
-keep class org.json.** { *; }

# ==================== WEBVIEW ====================

# Keep WebView JavaScript interfaces
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# Keep WebView classes
-keep class android.webkit.** { *; }

# ==================== REMOVE LOGGING ====================

# Remove all Log statements in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
    public static int i(...);
    public static int w(...);
    public static int e(...);
}

# ==================== WARNINGS ====================

# Suppress warnings for missing classes (if needed)
-dontwarn org.jsoup.**
-dontwarn android.webkit.**
