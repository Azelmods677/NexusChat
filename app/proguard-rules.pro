# ─────────────────────────────────────────────────────────────────
#  PROGUARD RULES — Azelgram Messenger
#  Previene ClassNotFoundException causado por R8 eliminando
#  clases usadas vía reflexión (Hilt, Firebase, etc.)
# ─────────────────────────────────────────────────────────────────

# ── Dagger / Hilt ───────────────────────────────────────────────
-keep class dagger.** { *; }
-keep class javax.inject.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }
-keep class dagger.hilt.** { *; }
-keep class dagger.hilt.android.** { *; }
-keep class androidx.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponentManager { *; }
-keep class * extends dagger.hilt.internal.GeneratedComponent { *; }
-keep class * extends dagger.hilt.internal.aggregatedroot.AggregatedRoot { *; }
-keep class * extends dagger.hilt.android.components.ActivityComponent { *; }
-keep class * extends dagger.hilt.android.components.FragmentComponent { *; }
-keep class * extends dagger.hilt.android.components.ViewModelComponent { *; }
-keep class * extends dagger.hilt.android.components.ServiceComponent { *; }
-keep class * extends dagger.hilt.components.SingletonComponent { *; }
-keep class dagger.hilt.internal.aggregatedroot.codegen.** { *; }
-keep class dagger.hilt.android.internal.managers.** { *; }
-keep class dagger.hilt.processor.internal.aggregatedroot.codegen.** { *; }

# Hilt generated classes
-keep class com.Azelmods.App.**_Factory { *; }
-keep class com.Azelmods.App.**_MembersInjector { *; }
-keep class com.Azelmods.App.**_Impl { *; }
-keep class com.Azelmods.App.Dagger* { *; }
-keep class com.Azelmods.App.Hilt_* { *; }

# Keep Hilt Application and Activity base classes
-keep class * extends dagger.hilt.android.internal.managers.HiltWrapper_ApplicationComponentManager { *; }
-keepnames @dagger.hilt.android.HiltAndroidApp class * { *; }
-keepnames @dagger.hilt.android.AndroidEntryPoint class * { *; }
-keepnames @dagger.hilt.android.lifecycle.HiltViewModel class * { *; }
-keepnames @dagger.hilt.InstallIn class * { *; }
-keepnames @dagger.Module class * { *; }
-keepnames @dagger.Provides class * { *; }
-keepnames @dagger.Binds class * { *; }

# ── Firebase ────────────────────────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-keep class com.google.firebase.crashlytics.** { *; }
-keep class com.google.firebase.database.** { *; }
-keep class com.google.firebase.auth.** { *; }
-keep class com.google.firebase.storage.** { *; }
-keep class com.google.firebase.messaging.** { *; }
-keep class com.google.firebase.installations.** { *; }
-keep class com.google.firebase.provider.** { *; }
-keep class com.google.firebase.FirebaseApp { *; }
-keep class com.google.firebase.FirebaseOptions { *; }
-keep class com.google.android.gms.tasks.** { *; }

# Firebase generated classes
-keep class com.Azelmods.App.** { *; }
-keep class com.google.firebase.components.** { *; }

# ── Kotlin Serialization ────────────────────────────────────────
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class kotlinx.serialization.json.** { *; }
-keepclassmembers class * implements kotlinx.serialization.Serializable { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable <fields>;
}

# Keep @Serializable annotated classes
-keepclassmembers @kotlinx.serialization.Serializable class * { *; }

# ── Kotlin Coroutines ───────────────────────────────────────────
-keepnames class kotlinx.coroutines.internal.** { *; }
-keepclassmembers class kotlinx.coroutines.** { *; }
-keep class kotlinx.coroutines.** { *; }

# ── Compose ─────────────────────────────────────────────────────
-keep class androidx.compose.** { *; }
-keep class androidx.compose.runtime.** { *; }
-keep class androidx.compose.ui.** { *; }
-keep class androidx.compose.material3.** { *; }
-keep class androidx.compose.foundation.** { *; }
-keep class androidx.compose.animation.** { *; }
-keep class androidx.compose.material.icons.** { *; }
-keep class androidx.navigation.compose.** { *; }
-keep class androidx.activity.compose.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable <methods>;
}

# ── Navigation ──────────────────────────────────────────────────
-keep class androidx.navigation.** { *; }
-keep class * extends androidx.navigation.NavHostController { *; }

# ── Lifecycle ───────────────────────────────────────────────────
-keep class androidx.lifecycle.** { *; }
-keep class * extends androidx.lifecycle.ViewModel { *; }

# ── Room ────────────────────────────────────────────────────────
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Database class * { *; }
-keep @androidx.room.Query class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# ── DataStore ───────────────────────────────────────────────────
-keep class androidx.datastore.** { *; }

# ── OkHttp ──────────────────────────────────────────────────────
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**

# ── Coil 3 ──────────────────────────────────────────────────────
-keep class coil3.** { *; }
-keep class coil3.compose.** { *; }

# ── WebView / AndroidX WebKit (ProxyController) ────────────────
-keep class androidx.webkit.** { *; }
-keepclassmembers class androidx.webkit.** { *; }

# ── BouncyCastle ────────────────────────────────────────────────
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# ── Signal Protocol ─────────────────────────────────────────────
-keep class org.signal.** { *; }
-keep class org.whispersystems.** { *; }

# ── NetCipher (Orbot proxy) ────────────────────────────────────
-keep class info.guardianproject.netcipher.** { *; }

# ── libsu (Root) ────────────────────────────────────────────────
-keep class com.topjohnwu.superuser.** { *; }
-keep class com.topjohnwu.libsu.** { *; }

# ── sora-editor ─────────────────────────────────────────────────
-keep class io.github.Rosemoe.sora.** { *; }
-keep class io.github.Rosemoe.sora.editor.** { *; }

# ── Media3 / ExoPlayer ──────────────────────────────────────────
-keep class androidx.media3.** { *; }

# ── AndroidX Core / Activity ────────────────────────────────────
-keep class androidx.core.** { *; }
-keep class androidx.activity.** { *; }
-keep class androidx.fragment.app.** { *; }

# ── WorkManager ─────────────────────────────────────────────────
-keep class androidx.work.** { *; }

# ── SplashScreen ────────────────────────────────────────────────
-keep class androidx.core.splashscreen.** { *; }

# ── Accompanist ─────────────────────────────────────────────────
-keep class com.google.accompanist.** { *; }

# ── WebRTC ──────────────────────────────────────────────────────
-keep class org.webrtc.** { *; }
-dontwarn org.webrtc.**

# ── Model classes (used by Firebase/Gson serialization) ────────
-keep class com.Azelmods.App.domain.model.** { *; }
-keep class com.Azelmods.App.data.model.** { *; }

# ── Keep Parcelable implementations ─────────────────────────────
-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator CREATOR;
}

# ── Keep Serializable classes ───────────────────────────────────
-keepclassmembers class * implements java.io.Serializable {
    private static final java.io.ObjectStreamField[] serialPersistentFields;
    private void writeObject(java.io.ObjectOutputStream);
    private void readObject(java.io.ObjectInputStream);
    java.lang.Object writeReplace();
    java.lang.Object readResolve();
}

# ── Keep R (resources) ──────────────────────────────────────────
-keep class com.Azelmods.App.R$* { *; }

# ── Keep R8 from stripping generic signatures ───────────────────
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes EnclosingMethod
-keepattributes InnerClasses
-keepattributes Exceptions

# ── General reflection ──────────────────────────────────────────
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

# ── Keep custom WebViewClient implementations ───────────────────
-keepclassmembers class * extends android.webkit.WebViewClient {
    *;
}
-keepclassmembers class * extends android.webkit.WebChromeClient {
    *;
}

# ── App classes ─────────────────────────────────────────────────
-keep class com.Azelmods.App.ui.screens.security.** { *; }
-keep class com.Azelmods.App.data.security.** { *; }
-keep class com.Azelmods.App.data.security.tor.** { *; }

# ── Prevent R8 from removing entire packages ────────────────────
-keep class com.Azelmods.App.** { *; }

# ── zip4j ────────────────────────────────────────────────────────
-keep class net.lingala.zip4j.** { *; }
-dontwarn net.lingala.zip4j.**

# ── smali / dexlib2 ─────────────────────────────────────────────
-keep class org.smali.** { *; }
-keep class org.jf.** { *; }
-dontwarn org.smali.**
-dontwarn org.jf.**
