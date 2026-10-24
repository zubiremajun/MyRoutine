# Keep the JS <-> Kotlin bridge methods (R8 would otherwise strip/rename them
# and the WebView's JavaScript calls would silently fail).
-keepclassmembers class com.myroutine.app.MainActivity$Bridge {
    public *;
}
-keepattributes JavascriptInterface
-keepattributes *Annotation*
# Keep anything referenced from AndroidManifest (activities/receivers) - R8 does this
# automatically for manifest components, this is just a safety net.
-keep class com.myroutine.app.MainActivity { *; }
-keep class com.myroutine.app.SplashActivity { *; }
-keep class com.myroutine.app.CallActivity { *; }
-keep class com.myroutine.app.AlarmReceiver { *; }
-keep class com.myroutine.app.BootReceiver { *; }
