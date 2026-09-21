# Consumer R8 / ProGuard rules for foliate-kmp-core.
# The AAR carries this file, so a consumer application needs no extra configuration.

# The web view calls the bridge method from JavaScript through reflection.
-keepclassmembers class io.github.asadullah012.foliate.ui.AndroidEpubBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# kotlinx.serialization looks up the generated serializer of each model class.
-keepclassmembers class io.github.asadullah012.foliate.model.** {
    *** Companion;
}
-keepclasseswithmembers class io.github.asadullah012.foliate.model.** {
    kotlinx.serialization.KSerializer serializer(...);
}
