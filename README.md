# KTHREADD 4.1 development

The `upgrade/4.1-cpvp` branch contains the comprehensive upgrade. See [changes and verification status](docs/4.1-UPGRADE.md). The existing tested release remains available during validation.

---

# KTHREADD

Independent, open-source PvP client for **Minecraft Java 1.20.4**, **Fabric**, and **Java 17**.

[Download the latest release](https://github.com/bengonzalesdr/KTHREADD/releases) · [Report an issue](https://github.com/bengonzalesdr/KTHREADD/issues) · [Rendering changes](docs/RENDER-UPDATE.md) · [Testing](docs/TESTING.md)

## Install

1. Install Minecraft Java **1.20.4** with Fabric Loader (**0.15.11 or later**; tested together on **0.19.5**) and Java **17**.
2. Download **`KTHREADD-1.20.4-4.0-port.6-render.jar`** from Releases, plus **`fabric-api-0.97.2+1.20.4.jar`** from the same release or the official [Fabric API project](https://github.com/FabricMC/fabric).
3. Put the client JAR and one copy of Fabric API into your instance's `mods` folder. Move older versions of this client out first. Source ZIPs and development/QA JARs do not belong in `mods`.
4. Start Fabric 1.20.4. Press **Delete** to open the menu. Right-click a module for its settings.

The default Minecraft instance is `%APPDATA%\.minecraft` on Windows; other launchers may use their own instance folder. Exit code 1 is generic: attach the relevant crash report or `latest.log` with a bug report after removing personal/server details.

## Features

Storage ESP and Entity ESP can display through blocks in loaded chunks. Tracers use thin, configurable lines and compensate for view bob. Geometry uses camera-relative coordinates and interpolated entity positions. Profiles, friend lists and HUD settings are stored locally.

KTHREADD focuses on crystal combat, melee, offhand protection, armor and surround. CrystalAura includes damage ranking, forecast checks, server-spawn reactions, pending-damage reservations and a bounded local fight recorder. `/kthread combatstats` shows diagnostics; `/kthread combatreport` saves a local JSON report. See [CPvP behavior](docs/CPVP-PORT5.md). It does not contain an AI model.

This is an independently authored reconstruction of IngrosWare gameplay concepts, not a redistribution of its code or assets. No verified license for the legacy IngrosWare binary was found, so the original JAR, decompiled source, fonts and assets are excluded. Original authors are credited in [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).

## With Creepy SalHack

KTHREADD works independently. To use the companion [Creepy SalHack modernization](https://github.com/bengonzalesdr/creepy-salhack/tree/fabric-1.20.4-modernization/fabric-1.20.4), install both client JARs with one copy of Fabric API. SalHack opens with Left Arrow; KTHREADD opens with Delete. Enabled matching KTHREADD combat/ESP modules take priority by default. Maintain the friend lists for the clients you use.

## Build from source

Install a JDK **17**. The checked-in wrapper downloads **Gradle 8.6** with a pinned checksum. Run from this repository:

```sh
./gradlew build
```

Windows PowerShell:

```powershell
.\gradlew.bat build
```

The installable remapped JAR is written to `build/libs/`. For this version, use `KTHREADD-1.20.4-4.0-port.6-render.jar`. Unit tests run as part of `build`. `runQaClient` launches an isolated local test game and creates temporary test worlds under the configured QA directory.

## Contributing and license

See [CONTRIBUTING.md](CONTRIBUTING.md). Authored source is distributed under **MIT**, with inherited notices preserved in [LICENSE](LICENSE). Dependency and original-project attribution is documented separately. Minecraft itself is not included.

This is a development release with targeted tests, not complete historical parity or a guarantee for every server. Please report reproducible issues with Minecraft/Fabric versions, the exact client build, relevant settings, and sanitized logs.
