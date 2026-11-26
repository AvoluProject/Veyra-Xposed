#!/usr/bin/env python3
"""Offline structural checks for Veyra; does not replace an Android Gradle build."""
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
errors: list[str] = []
checks: list[str] = []


def require(condition: bool, message: str) -> None:
    (checks if condition else errors).append(message)


def read(rel: str) -> str:
    path = ROOT / rel
    try:
        return path.read_text(encoding="utf-8")
    except OSError as exc:
        errors.append(f"Read {rel}: {exc}")
        return ""


# Manifest and module metadata.
manifest_text = read("app/src/main/AndroidManifest.xml")
try:
    tree = ET.fromstring(manifest_text)
    app = tree.find("application")
    require(app is not None, "Android application manifest parses")
    if app is not None:
        names = {
            node.attrib.get("{http://schemas.android.com/apk/res/android}name", "")
            for node in app
        }
        require("dev.veyra.dashboard.MainActivity" in names, "Dashboard activity is declared")
        require("io.github.libxposed.service.XposedProvider" in names, "libxposed provider is declared")
except ET.ParseError as exc:
    errors.append(f"Android application manifest parses: {exc}")

entry = ROOT / "app/src/main/resources/META-INF/xposed/java_init.list"
scope = ROOT / "app/src/main/resources/META-INF/xposed/scope.list"
prop = ROOT / "app/src/main/resources/META-INF/xposed/module.prop"
require(entry.is_file() and entry.read_text().strip() == "dev.veyra.xposed.VeyraModule",
        "Modern Xposed entry point matches source class")
require(scope.is_file() and scope.read_text().splitlines() == ["com.android.settings"],
        "Scope is restricted to Samsung Settings")
properties = {}
if prop.is_file():
    properties = dict(
        line.split("=", 1) for line in prop.read_text().splitlines() if "=" in line
    )
require(properties.get("minApiVersion") == "102" and properties.get("targetApiVersion") == "102",
        "libxposed module metadata targets API 102")
require(properties.get("staticScope") == "true", "Static scope metadata is enabled")

# One coherent Android toolchain: API 37.0 / AGP 9.1.1 / Gradle 9.3.1 / JDK 17.
root_gradle = read("build.gradle.kts")
app_gradle = read("app/build.gradle.kts")
workflow = read(".github/workflows/build.yml")
props = read("gradle.properties")
require('version "9.1.1"' in root_gradle, "AGP is pinned to 9.1.1")
require("version = release(37)" in app_gradle and "minorApiLevel = 0" in app_gradle,
        "compileSdk is pinned to Android API 37.0")
require("targetSdk = 36" in app_gradle, "targetSdk remains 36")
require('version "9.3.1"' not in root_gradle and "gradle-version: '9.3.1'" in workflow,
        "GitHub Actions uses Gradle 9.3.1")
require("java-version: '17'" in workflow and "targetCompatibility = JavaVersion.VERSION_17" in app_gradle,
        "Java target is JDK 17; AGP built-in Kotlin inherits that target")
require('id("org.jetbrains.kotlin.android")' not in root_gradle + app_gradle,
        "AGP built-in Kotlin is used without the legacy Kotlin Android plugin")
require("android.newDsl=false" not in props and "android.builtInKotlin=false" not in props,
        "Deprecated AGP 9 opt-out flags are absent")
require('io.github.libxposed:api:102.0.0' in app_gradle,
        "libxposed API dependency is pinned to published 102.0.0")
require('io.github.libxposed:service:102.0.0' in app_gradle,
        "libxposed service dependency is pinned to published 102.0.0")
require("platforms;android-37.0" in workflow and "build-tools;36.0.0" in workflow,
        "CI installs the API 37.0 platform package and compatible Build Tools")
require("packages: tools" not in workflow, "CI does not request removed SDK package 'tools'")

# Expected source files and resource contracts.
required_files = [
    "app/src/main/kotlin/dev/veyra/xposed/VeyraModule.kt",
    "app/src/main/kotlin/dev/veyra/xposed/VeyraServiceManager.kt",
    "app/src/main/kotlin/dev/veyra/xposed/hooks/SettingsHookInstaller.kt",
    "app/src/main/kotlin/dev/veyra/xposed/ui/SettingsUiEngine.kt",
    "app/src/main/kotlin/dev/veyra/xposed/ui/GlassSurfaceDrawable.kt",
    "app/src/main/kotlin/dev/veyra/xposed/ui/VeyraMotionEngine.kt",
    "app/src/main/kotlin/dev/veyra/dashboard/MainActivity.kt",
]
for rel in required_files:
    require((ROOT / rel).is_file(), f"Required source exists: {rel}")

all_source = "\n".join(p.read_text(encoding="utf-8", errors="ignore")
                      for p in ROOT.rglob("*.kt"))
require("de.robv.android.xposed" not in all_source and "XposedBridge" not in all_source,
        "No legacy XposedBridge API references")
require("androidx.preference.PreferenceGroupAdapter" in all_source,
        "AndroidX preference binding hook is present")
require("getRemotePreferences" in all_source,
        "RemotePreferences bridge is present")
ui_engine = read("app/src/main/kotlin/dev/veyra/xposed/ui/SettingsUiEngine.kt")
classifier = read("app/src/main/kotlin/dev/veyra/xposed/target/SettingsPageClassifier.kt")
require("snapshot(root)" in ui_engine,
        "Category descendants are snapshotted before restyling")
require("restoreTree(itemView)" in ui_engine,
        "Recycled preference rows restore their full prior view tree")
require("hasSwitchThumbTint" in ui_engine and "restoreSwitchTint" in ui_engine,
        "Switch tint changes can be restored when styling is disabled")
require("visited[0] >= 600" in ui_engine and "visited[0] >= 260" in classifier,
        "Settings view-tree styling and classification have traversal limits")

for message in checks:
    print(f"PASS  {message}")
for message in errors:
    print(f"FAIL  {message}")
print(f"\n{len(checks)} passed, {len(errors)} failed")
sys.exit(1 if errors else 0)
