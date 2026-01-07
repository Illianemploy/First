# Add project specific ProGuard rules here.
-keep class com.uktobacco.** { *; }
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}
