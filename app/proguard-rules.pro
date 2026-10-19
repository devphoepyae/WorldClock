# Add project specific ProGuard rules here.
# Minification is disabled for all build types at this stage, so this file is
# intentionally empty. Rules will be added when release shrinking is enabled.

# Proto DataStore generated classes are serialized reflectively by the
# generated lite runtime; keep them if R8 shrinking is turned on later.
# -keep class com.worldclock.app.data.prefs.** { *; }
