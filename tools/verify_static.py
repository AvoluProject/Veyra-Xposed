#!/usr/bin/env python3
"""Offline structural checks for the Veyra Xposed source package."""
from pathlib import Path
import sys
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
errors: list[str] = []
checks: list[str] = []


def require(condition: bool, message: str) -> None:
    if condition:
        checks.append(message)
    else:
        errors.append(message)


manifest = ROOT / "app/src/main/AndroidManifest.xml"
try:
    tree = ET.parse(manifest)
    app = tree.getroot().find("application")
    require(app is not None, "Android application manifest parses")
    if app is not None:
        names = {node.attrib.get("{http://schemas.android.com/apk/res/android}name", "") for node in app}
        require("dev.veyra.dashboard.MainActivity" in names, "Dashboard activity is declared")
        require("io.github.libxposed.service.XposedProvider" in names, "libxposed service provider is declared")
except (ET.ParseError, OSError) as exc:
    errors.append(f"Android manifest parses: {exc}")

entry = ROOT / "app/src/main/resources/META-INF/xposed/java_init.list"
scope = ROOT / "app/src/main/resources/META-INF/xposed/scope.list"
prop = ROOT / "app/src/main/resources/META-INF/xposed/module.prop"
require(entry.exists() and entry.read_text().strip() == "dev.veyra.xposed.VeyraModule", "Modern Xposed entry point matches source class")
require(scope.exists() and scope.read_text().splitlines() == ["com.android.settings"], "Scope is restricted to Samsung Settings")
props = dict(line.split("=", 1) for line in prop.read_text().splitlines() if "=" in line)
require(props.get("minApiVersion") == "102" and props.get("targetApiVersion") == "102", "libxposed API metadata targets 102")
require(props.get("staticScope") == "true", "Static scope metadata is enabled")

required_files = [
    "app/src/main/kotlin/dev/veyra/xposed/VeyraModule.kt",
    "app/src/main/kotlin/dev/veyra/xposed/hooks/SettingsHookInstaller.kt",
    "app/src/main/kotlin/dev/veyra/xposed/ui/SettingsUiEngine.kt",
    "app/src/main/kotlin/dev/veyra/xposed/ui/GlassSurfaceDrawable.kt",
    "app/src/main/kotlin/dev/veyra/xposed/ui/VeyraMotionEngine.kt",
    "app/src/main/kotlin/dev/veyra/dashboard/MainActivity.kt",
    ".github/workflows/build.yml",
]
for name in required_files:
    require((ROOT / name).is_file(), f"Required source exists: {name}")

all_source = "\n".join(p.read_text(errors="ignore") for p in ROOT.rglob("*.kt"))
require("de.robv.android.xposed" not in all_source and "XposedBridge" not in all_source,
        "No legacy XposedBridge API references")
require("androidx.preference.PreferenceGroupAdapter" in all_source,
        "Real AndroidX preference binding hook is present")
require("getRemotePreferences" in all_source,
        "RemotePreferences bridge is present")

for message in checks:
    print(f"PASS  {message}")
for message in errors:
    print(f"FAIL  {message}")
print(f"\n{len(checks)} passed, {len(errors)} failed")
sys.exit(1 if errors else 0)
