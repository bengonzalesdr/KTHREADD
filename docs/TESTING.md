# KTHREADD 4.1.0-beta.1 verification

Minecraft 1.20.4 / Java 17 / Fabric API 0.97.2+1.20.4. Production Loader: 0.19.5.

- 57 unit tests passed.
- 61 targeted local game checks passed.
- 28 combined production rendering/compatibility checks passed with unchanged Creepy SalHack 2.08.2-port.4-render.
- All 109 production class files match the standalone public source build byte-for-byte.
- Exact production-tested JAR: `KTHREADD-1.20.4-4.1.0-beta.1.jar`.
- SHA-256: `9566befbc6406f89301078fd83308c48b59abacf374817a5cb29432f6c9a14ab`.

New game checks cover actual placement on chest support without opening a container, slot/sneak restoration, configured bow charge and server projectile creation, protection ownership and cleanup, a hard 32-evaluation count in a 64-crystal client fixture, no attack on zero-damage targets and atomic preset application. Existing checks retain crystal placement/breaking/spawn reactions, offhand emergency retention, movement hooks, UI/profile behavior, report export, through-wall pixels, view-bob anchor math and far-coordinate interpolation.

The visual fixture was explicitly stabilized/unpaused after background UI interference; its final production run exited successfully with 28 passes. The actual client JAR was unchanged by that test-harness adjustment. Local combat fixtures protect the test player with Resistance V; this is not a general combat-damage calibration or survival guarantee.

Every setting/mode, every third-party mod/shader pack, remote-server/anti-cheat acceptance, variable-latency combat performance and competitive win rate remain unverified. Read [the audit](MODULE-AUDIT-4.1.md) and group reports for known bounds, including eventual storage refresh and conservative two-hit threat handling. No account data, personal worlds or raw machine logs are published.
