// proguard-rules.pro: ProGuard optimization rules
# Keep model classes
-keep class com.example.goujicardcounter.model.** { *; }
-keep class com.example.goujicardcounter.logic.** { *; }

# Keep service classes
-keep class com.example.goujicardcounter.service.** { *; }

# Keep recognition classes
-keep class com.example.goujicardcounter.recognition.** { *; }

# Keep UI classes
-keep class com.example.goujicardcounter.ui.** { *; }

# OkHttp and Retrofit
-dontwarn okhttp3.**
-dontwarn okio.**
-keepattributes Signature
-keepattributes Exceptions

# PaddleOCR
-dontwarn com.baidu.paddle.**
-keep class com.baidu.paddle.** { *; }

# General optimizations
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
