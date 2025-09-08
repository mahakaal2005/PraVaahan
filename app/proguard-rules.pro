# Add project specific ProGuard rules here.

# Keep logging interfaces and classes
-keep class com.example.pravaahan.core.logging.** { *; }
-keep interface com.example.pravaahan.core.logging.Logger { *; }

# Supabase and Ktor
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.example.pravaahan.**$$serializer { *; }
-keepclassmembers class com.example.pravaahan.** {
    *** Companion;
}
-keepclasseswithmembers class com.example.pravaahan.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Hilt
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keepclasseswithmembers class * {
    @dagger.hilt.android.AndroidEntryPoint <methods>;
}

# Optimize logging calls in release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int d(...);
}

# SLF4J logging framework
-keep class org.slf4j.** { *; }
-keep class org.slf4j.impl.** { *; }
-dontwarn org.slf4j.**

# Keep line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile