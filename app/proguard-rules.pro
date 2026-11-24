# ============================================================
# إعدادات عامة
# ============================================================

# إبقاء معلومات التتبع (مفيد لتحليل الأعطال في الإصدار النهائي)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ============================================================
# OkHttp - تجاهل مزوّدي TLS الاختياريين (سبب فشل البناء)
# ============================================================
-dontwarn org.bouncycastle.**
-dontwarn org.conscrypt.**
-dontwarn org.openjsse.**

# ============================================================
# OkHttp / Retrofit
# ============================================================
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep interface okhttp3.** { *; }
-keep class okio.** { *; }

# Retrofit
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-keep,allowobfuscation,allowshrinking class retrofit2.Response
-keep,allowobfuscation,allowshrinking class kotlin.coroutines.Continuation

# ============================================================
# Kotlin Coroutines
# ============================================================
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}
-dontwarn kotlinx.coroutines.**

# ============================================================
# Room Database
# ============================================================
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-dontwarn androidx.room.paging.**

# ============================================================
# Jetpack Compose
# ============================================================
-dontwarn androidx.compose.**
-keep class androidx.compose.runtime.** { *; }

# ============================================================
# Gson (إن كنت تستخدمه)
# ============================================================
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# ============================================================
# أبقِ نماذج البيانات (data classes) الخاصة بالشبكة
# عدّل اسم الحزمة حسب مشروعك
# ============================================================
-keep class gamalsolutions.gamalprojects.contactmanager.network.** { *; }
-keep class gamalsolutions.gamalprojects.contactmanager.database.** { *; }
