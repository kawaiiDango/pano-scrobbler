# https://github.com/JetBrains/compose-multiplatform/issues/4883

#-mergeinterfacesaggressively
#
#-overloadaggressively
#
#-repackageclasses



-keep class androidx.datastore.preferences.** { *; }

-keep class io.ktor.** { *; }

-keep class coil3.** { *; }

-keep class ui.navigation.** { *; }

# for desktop TextField

-keepclasseswithmembernames class androidx.compose.foundation.text.** { *; }



# Kotlin & java

-assumenosideeffects class kotlin.jvm.internal.Intrinsics {

	public static void check*(...);

	public static void throw*(...);

}

-assumenosideeffects public class kotlin.coroutines.jvm.internal.DebugMetadataKt {

   private static ** getDebugMetadataAnnotation(...);

}

-assumenosideeffects class java.util.Objects {

    public static ** requireNonNull(...);

}



####################################################################################################



# slf4j

-assumenosideeffects interface org.slf4j.Logger {

    public void trace(...);

    public void debug(...);

    public void info(...);

    public void warn(...);

    public void error(...);



    public boolean isTraceEnabled(...);

    public boolean isDebugEnabled(...);

    public boolean isWarnEnabled(...);

}



-assumenosideeffects class org.slf4j.LoggerFactory {

    public static ** getLogger(...);

}



-dontwarn org.slf4j.**



####################################################################################################



# kotlinx.coroutines

# https://github.com/Kotlin/kotlinx.coroutines/blob/master/kotlinx-coroutines-core/jvm/resources/META-INF/proguard/coroutines.pro

# ServiceLoader support

-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}

-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}



# Most of volatile fields are updated with AFU and should not be mangled

-keepclassmembers class kotlinx.coroutines.** {

    volatile <fields>;

}



# Same story for the standard library's SafeContinuation that also uses AtomicReferenceFieldUpdater

-keepclassmembers class kotlin.coroutines.SafeContinuation {

    volatile <fields>;

}



# These classes are only required by kotlinx.coroutines.debug.AgentPremain, which is only loaded when

# kotlinx-coroutines-core is used as a Java agent, so these are not needed in contexts where ProGuard is used.

-dontwarn java.lang.instrument.ClassFileTransformer

-dontwarn sun.misc.SignalHandler

-dontwarn java.lang.instrument.Instrumentation

-dontwarn sun.misc.Signal



# Only used in `kotlinx.coroutines.internal.ExceptionsConstructor`.

# The case when it is not available is hidden in a `try`-`catch`, as well as a check for Android.

-dontwarn java.lang.ClassValue



# An annotation used for build tooling, won't be directly accessed.

-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement



# https://github.com/Kotlin/kotlinx.coroutines/issues/4025

-keep class kotlinx.coroutines.internal.MainDispatcherFactory { *; }

-keep class kotlinx.coroutines.swing.SwingDispatcherFactory { *; }



####################################################################################################



# kotlinx.serialization

# https://github.com/Kotlin/kotlinx.serialization/blob/master/rules/common.pro

# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
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

# Keep all @Serializable classes and their members
-keep @kotlinx.serialization.Serializable class * { *; }

# javafx

# Keep JavaFX toolkit classes
-keep class com.sun.javafx.** { *; }
-keep class com.sun.prism.** { *; }
-keep class com.sun.glass.** { *; }
-keep class com.sun.webkit.** { *; }
-keep class com.sun.scenario.effect.** { *; }
-keep class javafx.css.** { *; }
-keep class javafx.scene.** { *; }
-keep class com.jogamp.** { *; }

# Suppress warnings from the Newt JavaFX classes
-dontwarn com.jogamp.newt.javafx.**

# Suppress warnings from the io.ktor network sockets classes
-dontwarn io.ktor.network.sockets.**

# PanoNativeComponents
-keep class com.arn.scrobble.PanoNativeComponents { *; }