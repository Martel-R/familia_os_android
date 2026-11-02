-dontoptimize
-dontobfuscate
-keepattributes *
-dontwarn org.slf4j.**
-dontwarn io.ktor.**
-dontwarn kotlinx.**

# Preservar 100% de todas as classes do app e bibliotecas exceto ícones não utilizados do material-icons-extended
-keep class !androidx.compose.material.icons.** { *; }
-keep interface !androidx.compose.material.icons.** { *; }
-keep enum !androidx.compose.material.icons.** { *; }
