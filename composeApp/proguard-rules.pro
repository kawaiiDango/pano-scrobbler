# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
-renamesourcefileattribute SourceFile

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.slf4j.impl.StaticLoggerBinder

# SIGSEGV https://issuetracker.google.com/issues/436756681
-keep class okhttp3.internal.idn.IdnaMappingTable { *; }
-keep class okhttp3.internal.idn.IdnaMappingTableInstanceKt{ *; }

# A resource is loaded with a relative path so the package of this class must be preserved.
#-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

#-dontobfuscate