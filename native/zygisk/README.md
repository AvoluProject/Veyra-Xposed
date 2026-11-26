# Optional native extension

Veyra 1.0.0 does not ship a Zygisk binary. The initial target is the Java/Kotlin UI layer inside `com.android.settings`, which is fully addressable through the LSPosed module. A native companion would add attack surface and maintenance cost without improving the current view/layout hooks. Add it only when a measured requirement cannot be met through the supported LSPosed process hook.
