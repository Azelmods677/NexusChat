# ==========================================
# AZELGRAM - ProGuard/R8 Rules
# ==========================================

# ── Keep Application Entry Points ─────────
-keep public class com.Azelmods.App.MainActivity
-keep public class com.Azelmods.App.NexusChatApplication

# ── Hilt ──────────────────────────────────
-keep class dagger.hilt.** { *; }
-keep class javax.inject.** { *; }
-keep @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keepclassmembers class * {
    @javax.inject.Inject <init>(...);
    @javax.inject.Inject <fields>;
}

# ── Firebase ──────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keepattributes Signature
-keepattributes *Annotation*

# ── Data Models (no obfuscar - usados por Firebase deserialization) ─
-keep class com.Azelmods.App.data.model.** { *; }
-keep class com.Azelmods.App.data.file.FileEncryptionMetadata { *; }
-keep class com.Azelmods.App.data.file.EncryptedChunk { *; }

# ── Kotlin ────────────────────────────────
-keep class kotlin.** { *; }
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
-keepclassmembers class kotlin.Lazy {
    public <methods>;
}

# ── Coroutines ────────────────────────────
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**

# ── OkHttp ────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# ── WebRTC ────────────────────────────────
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# ── BouncyCastle ──────────────────────────
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# ── Signal Protocol ───────────────────────
-keep class org.signal.libsignal.** { *; }
-dontwarn org.signal.libsignal.**

# ── Compose ───────────────────────────────
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**

# ── Coil ──────────────────────────────────
-dontwarn coil.**

# ── Serialization ─────────────────────────
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class com.Azelmods.App.**$$serializer { *; }
-keepclassmembers class com.Azelmods.App.** {
    *** Companion;
}
-keepclasseswithmembers class com.Azelmods.App.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# ── Remove Logging in Release ─────────────
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
}

# ── Security: Keep TamperDetection ────────
-keep class com.Azelmods.App.security.TamperDetection { *; }
-keep class com.Azelmods.App.security.RootDetection { *; }

# ── App Startup ───────────────────────────
-keep class androidx.startup.** { *; }
-keep class com.Azelmods.App.startup.** { *; }

# ── Enums ─────────────────────────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Parcelable ────────────────────────────
-keep class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}

# ── Serializable ──────────────────────────
-keepclassmembers class * implements java.io.Serializable {
    static final long serialVersionUID;
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ── R8 Aggressive Optimizations ───────────
-optimizationpasses 5
-allowaccessmodification
-repackageclasses 'a'
-overloadaggressively
