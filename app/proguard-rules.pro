-dontobfuscate
-dontwarn org.slf4j.**
-dontwarn io.ktor.**
-dontwarn kotlinx.**

# Preservar integralmente todas as classes do app, DTOs, ViewModels e repositórios
-keep class br.com.raevan.familialm.** { *; }
-keep interface br.com.raevan.familialm.** { *; }
-keep enum br.com.raevan.familialm.** { *; }

# Preservar Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses, EnclosingMethod, Signature
-keep @kotlinx.serialization.Serializable class * { *; }
-keepclassmembers class * {
    @kotlinx.serialization.* <fields>;
    static ** Companion;
     kotlinx.serialization.KSerializer serializer(...);
}

# Preservar Supabase, Ktor e Settings
-keep class io.github.jan.supabase.** { *; }
-keep class io.ktor.** { *; }
-keep class com.russhwolf.settings.** { *; }
-keep class org.kotlincrypto.** { *; }
