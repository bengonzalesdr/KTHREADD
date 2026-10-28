# KTHREADD

Open-source Minecraft **1.20.4** Fabric client, with a focus on crystal PvP. Requires **Java 17**. Authored source is MIT licensed with original notices preserved.

**[Download 4.1.0-beta.1](https://github.com/bengonzalesdr/KTHREADD/releases/tag/v4.1.0-beta.1)** · [Module audit](docs/MODULE-AUDIT-4.1.md) · [Upgrade guide](docs/4.1-UPGRADE.md) · [Testing](docs/TESTING.md) · [Issues](https://github.com/bengonzalesdr/KTHREADD/issues)

## Install

1. Use Minecraft Java **1.20.4**, Fabric Loader **0.15.11 or later** (production-tested with **0.19.5**), and Java **17**.
2. Download `KTHREADD-1.20.4-4.1.0-beta.1.jar` and `fabric-api-0.97.2+1.20.4.jar` from the release. Put both into your instance's `mods` folder. Move older KTHREADD versions out first; keep one copy of Fabric API.
3. Start Fabric 1.20.4. Press **Delete** for the menu; right-click a module for settings. **Ctrl+F** searches module and setting names. **Right Shift** opens the HUD editor.

The default Windows instance is `%APPDATA%\.minecraft`; other launchers can use their own instance folders. Source ZIPs and development/QA JARs do not go into `mods`. The release ZIP contains the client, Fabric API, instructions and checksums.

## Crystal PvP setup

Run `/kthread preset balanced`, then enable **CrystalAura**, **AutoTotem**, **AutoArmor** and **AntiFriendHit** as desired. Presets change settings atomically and **do not enable modules**. `aggressive` and `defensive` are alternative starting values; all retain self/friend protections. Add teammates with `/kthread friend add NAME`.

`/kthread set MODULE "SETTING NAME" VALUE` allows exact tuning. `/kthread combatstats` shows live crystal diagnostics; `/kthread combatreport` exports a bounded local recording after a fight. These settings are starting points, not measured best settings for every server.

## What changed in 4.1

- Bounded crystal work, confirmation-based retry timing/backoff, motion history, latency-adjusted conservative prediction, stable pressure focus and damage-trade filtering.
- Stronger offhand fallback and two-hit threat assessment, durability-aware armor, stack-aware looting, suitable tools and safe container-supported block placement.
- Melee weapon/cooldown ownership, guarded critical timing, charged bows, and renewed/cleaned-up protection after totem pops.
- Deterministic movement priority with environment/input guards and correct key cleanup.
- Incremental bounded storage scans, ESP/tracer filters and caps, richer combat HUD, setting search and profile backup recovery.

All **54 registry entries** were audited. Seven already-correct hooks were retained rather than given cosmetic changes; the audit distinguishes them. No AI component is included. Full historical IngrosWare parity and universal competitive superiority are not claimed.

## With Creepy SalHack

KTHREADD runs independently. The optional [community SalHack port](https://github.com/bengonzalesdr/creepy-salhack) remains unchanged and opens with **Left Arrow**. With both clients installed, matching KTHREADD combat and ESP modules take priority by default. Keep one copy of Fabric API and maintain the friend lists for the clients you use.

## Build and contribute

Install JDK 17. Run `./gradlew build` (Windows: `.\gradlew.bat build`). The checksum-pinned wrapper uses Gradle 8.6; the installable remapped JAR is in `build/libs/`. Unit tests run with `build`; `runQaClient` uses temporary test worlds in the configured isolated QA directory.

See [CONTRIBUTING.md](CONTRIBUTING.md), [LICENSE](LICENSE) and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md). This is an independently authored reconstruction of gameplay concepts; the legacy IngrosWare binary, decompiled code, fonts and assets are excluded. Report bugs with exact versions, reproduction steps, settings and sanitized logs.

This beta passed 57 unit tests, 61 targeted local game checks and 28 combined production checks. Remote-server performance, all settings, every shader/modpack and anti-cheat acceptance have not been comprehensively measured. Previous release tags and assets remain available for rollback.
