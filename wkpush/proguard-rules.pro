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
#-renamesourcefileattribute SourceFile

# 小米推送混淆配置 - 根据官方文档要求
# 保持自定义的BroadcastReceiver
-keep class com.chat.push.push.XiaoMiMessageReceiver {*;}

# 小米推送SDK混淆配置
-keep class com.xiaomi.mipush.** { *; }
-dontwarn com.xiaomi.mipush.**

# 小米推送额外混淆规则
-keep class com.xiaomi.push.** { *; }
-dontwarn com.xiaomi.push.**