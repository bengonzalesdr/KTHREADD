# KTHREADD comprehensive upgrade

User intent: focus exclusively on KTHREADD, improve every existing module with emphasis on crystal PvP, work autonomously, and publish verified progress to GitHub. The user explicitly opted out of further design/permission questions. Implementation choices below are engineering decisions, not claims of universal competitive superiority.

## Constraints and success criteria

- Preserve Minecraft 1.20.4 / Fabric / Java 17 and existing settings/profile compatibility; retain optional SalHack interoperability without editing SalHack.
- Preserve the tested release and GitHub tags for rollback. Make verified incremental commits on the public KTHREADD repository.
- No AI component in this iteration; previous agreement reserves that for later.
- Prefer executable improvements to module count inflation or settings without implementations. Document actual changes and any already-correct module behavior separately for all 54 entries.
- Normal inventory/interaction packets, bounded main-thread work, no speculative entity-ID attacks or guaranteed server/anti-cheat claims.
- Source, tests and downloadable JAR must correspond. No secrets, worlds, caches or raw personal logs published.

## Architecture

Maintain the existing module registry and names. Improve the common lifecycle/scheduler so all modules get reliable cleanup, action ownership, settings and diagnostics. CrystalAura receives deterministic work limits, adaptive confirmation timing, negative-feedback placement backoff, stronger target/forecast policy and a conservative defensive threat reserve. Existing melee, armor, offhand and placement helpers receive correct cooldown/item handling and recovery. Movement modules gain arbitration and environmental guards. Rendering, HUD and utilities gain correct filtering, bounded scanning, usable controls and richer information.

## Independent work ownership

1. Primary agent: ModuleManager, KthreaddClient, ClientCommands, core infrastructure excluding agent-owned helpers, CrystalCombatModule, CrystalTracker, common tests/QA, versioning, packaging/publication.
2. Combat worker: InventoryModules, CombatModules, PvpModules, combat package except CrystalCombatModule/CombatActions, CombatInteractionMixin/PvpClientMixin; combat-specific new helpers/tests.
3. Movement worker: MovementModules, IngrosModules, movement and miscellaneous mixins; movement-specific helpers/tests.
4. Visual/utility worker: render and gui packages, ProfileStore; visual/utility-specific helpers/tests. Registry additions requested via integration notes, not direct ModuleManager edits.

Public Java APIs already used by other workers must remain compatible. No worker pushes or commits. Primary integrates, tests and publishes verified milestones.

## Verification

Meaningful regression tests for pure policies before implementation, standalone Gradle build/unit tests, local QA game with real inventory/interaction/explosion checks, render screenshots/pixel checks, release binary inspection, production-loader launch with the unchanged SalHack companion, public GitHub CI, and anonymous release download checksum verification. Do not broaden tests without a new change or unresolved concern. Report local-test limits honestly.
