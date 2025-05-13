# https://github.com/JetBrains/compose-multiplatform/issues/4883

-dontnote **

-printmapping dist/mappings/mapping.txt

# Coil
# https://github.com/coil-kt/coil/blob/main/samples/shared/shrinker-rules-android.pro
# https://coil-kt.github.io/coil/faq/#how-to-i-use-proguard-with-coil

# For native methods, see https://proguard.sourceforge.net/manual/examples.html#native
-keepclasseswithmembernames class * { native <methods>; }

# For enumeration classes, see https://proguard.sourceforge.net/manual/examples.html#enumerations
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# Understand the @Keep support annotation.
-keep class androidx.annotation.Keep
-keep @androidx.annotation.Keep class * { *; }
-keepclasseswithmembers class * { @androidx.annotation.Keep <methods>; }
-keepclasseswithmembers class * { @androidx.annotation.Keep <fields>; }
-keepclasseswithmembers class * { @androidx.annotation.Keep <init>(...); }

-keep class * extends coil3.util.DecoderServiceLoaderTarget { *; }
-keep class * extends coil3.util.FetcherServiceLoaderTarget { *; }

# ktor https://github.com/ktorio/ktor/blob/main/ktor-utils/jvm/resources/META-INF/proguard/ktor.pro
# Most of volatile fields are updated with AtomicFU and should not be mangled/removed
-keepclassmembers class io.ktor.** {
    volatile <fields>;
}

-keepclassmembernames class io.ktor.** {
    volatile <fields>;
}

# client engines are loaded using ServiceLoader so we need to keep them
-keep class io.ktor.client.engine.** implements io.ktor.client.HttpClientEngineContainer

####################################################################################################

# kotlinx.serialization

# https://github.com/Kotlin/kotlinx.serialization/blob/master/rules/common.pro

# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$* Companion;
}

# Keep names for named companion object from obfuscation
# Names of a class and of a field are important in lookup of named companion in runtime
-keepnames @kotlinx.serialization.internal.NamedCompanion class *
-if @kotlinx.serialization.internal.NamedCompanion class *
-keepclassmembernames class * {
    static <1> *;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Don't print notes about potential mistakes or omissions in the configuration for kotlinx-serialization classes
# See also https://github.com/Kotlin/kotlinx.serialization/issues/1900
-dontnote kotlinx.serialization.**

# Serialization core uses `java.lang.ClassValue` for caching inside these specified classes.
# If there is no `java.lang.ClassValue` (for example, in Android), then R8/ProGuard will print a warning.
# However, since in this case they will not be used, we can disable these warnings
-dontwarn kotlinx.serialization.internal.ClassValueReferences

# disable optimisation for descriptor field because in some versions of ProGuard, optimization generates incorrect bytecode that causes a verification error
# see https://github.com/Kotlin/kotlinx.serialization/issues/2719
-keepclassmembers public class **$$serializer {
    private ** descriptor;
}


# others ====================================================================================================

# okhttp https://raw.githubusercontent.com/square/okhttp/master/okhttp/src/main/resources/META-INF/proguard/okhttp3.pro

# JSR 305 annotations are for embedding nullability information.
-dontwarn javax.annotation.**

# Animal Sniffer compileOnly dependency to ensure APIs are compatible with older versions of Java.
-dontwarn org.codehaus.mojo.animal_sniffer.*

# OkHttp platform used only on JVM and when Conscrypt and other security providers are available.
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# graalvm stuff
-dontwarn org.graalvm.nativeimage.**
-dontwarn com.oracle.svm.**

# Keep the okhttp3.internal.Util class
-keep class okhttp3.internal.Util* { *; }
-dontwarn okhttp3.internal.Util

# room

-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep class androidx.sqlite.** { *; }

# Keep generated serializer classes in com.arn.scrobble and subpackages
-keep class com.arn.scrobble.**$$serializer { *; }

# Keep important class metadata for serialization
-keepattributes *Annotation*, Signature, InnerClasses#, EnclosingMethod, EnclosingClass

# javafx

-keep class com.sun.javafx.tk.quantum.QuantumToolkit { *; }
-keep class com.sun.javafx.font.** { *; }
-keep class com.sun.javafx.geom.** { *; }
-keep class com.sun.javafx.scene.** { *; }
-keep class com.sun.prism.sw.** { *; }
-keep class com.sun.glass.** { *; }
-keep class com.sun.pisces.** { *; }
-keep class com.sun.webkit.** { *; }
-keep class com.sun.scenario.effect.** { *; }
-keep class javafx.scene.** { *; }
-dontwarn com.jogamp.**

# Suppress warnings from the io.ktor network sockets classes
-dontwarn io.ktor.network.sockets.**
