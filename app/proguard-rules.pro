# ProGuard rules for SalviaAIZ

# Keep Android framework classes
-dontwarn android.**
-dontwarn androidx.**

# Keep Compose classes
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# Keep OkHttp classes
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# Keep Coil classes
-dontwarn coil.**
-keep class coil.** { *; }

# Keep Kotlin coroutines
-dontwarn kotlinx.coroutines.**
-keep class kotlinx.coroutines.** { *; }

# Keep Kotlin reflection
-dontwarn kotlin.reflect.**
-keep class kotlin.reflect.** { *; }

# Keep navigation classes
-dontwarn androidx.navigation.**
-keep class androidx.navigation.** { *; }

# Keep app classes
-keep class com.salvia.aiz.** { *; }

# Remove logging
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep generic signatures
-keepattributes Signature

# Keep annotation attributes
-keepattributes *Annotation*,InnerClasses

# Keep enums
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
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
