# Shrinking is off for release (see app/build.gradle.kts). These rules exist
# so that turning it on later is a one-line change rather than a debugging
# session: every class below is instantiated by the system from its manifest
# name, never from code, so nothing here looks used to the shrinker.
-keep class com.devclip.app.ClipboardAccessibilityService { *; }
-keep class com.devclip.app.OverlayService { *; }
-keep class com.devclip.app.BootReceiver { *; }
