# KTHREADD comprehensive upgrade implementation plan

> Agentic execution: use focused parallel workers for independent domains, then integrate and verify centrally.

**Goal:** Produce a substantially improved, tested KTHREADD release with a truthful audit of every module and emphasis on crystal combat.

**Architecture:** Retain module identities and standalone Fabric project. Introduce small policy helpers where they support deterministic tests. Improve module internals and shared orchestration rather than replacing the entire client.

**Tech stack:** Minecraft 1.20.4, Yarn 1.20.4+build.3, Fabric API 0.97.2+1.20.4, Java 17, Gradle 8.6, JUnit 5.

**Spec:** ../specs/2026-10-28-comprehensive-upgrade.md

## Global constraints

- Work in publication/KTHREADD. Preserve original active source/release until verification; synchronize at the end.
- Preserve module names and existing settings. Implement every added setting. No AI or speculative entity-ID attacks.
- Do not edit SalHack or remove prior release artifacts/tags. Publish only sanitized source/assets.

## Review focus

- Simultaneously enabled movement and combat modules must not overwrite critical state unpredictably.
- Placement rejection, delayed spawn/removal, slot restoration, missing resources and disconnect/respawn must recover.
- GUI/cursor/chat states must not trigger unsafe inventory actions or stuck keys.
- Large coordinates, unloaded chunks, crowded scenes and particle bursts must remain bounded and render correctly.
- Old profiles, shared keybinds, invalid configuration values and toggling during active actions must not corrupt state.

## Tasks

- [ ] Combat worker: write/run policy regressions; improve offhand threat handling, armor decisions, melee selection/cooldown, block placement/surround/trap, criticals and bow timing. Return file/module audit and test evidence.
- [ ] Movement worker: write/run movement arbitration regressions; improve each movement/utility module in owned files, including cleanup and environmental conditions. Return file/module audit and integration notes.
- [ ] Visual/utility worker: improve render filters/stash/search/waypoints, HUD/UI/profile reliability and performance without undoing through-wall fixes. Return file/module audit and integration notes.
- [ ] Primary: add tested confirmation/backoff and work-budget policies, improve CrystalAura decision pipeline and diagnostics; improve registry lifecycle/keybind/settings/presets.
- [ ] Integrate worker changes; populate a 54-entry audit with concrete behavior changes/verification.
- [ ] Build and test verified milestones; commit and push progress. Run targeted game/render/production compatibility checks after integration.
- [ ] Inspect exact release JAR, source package and publication contents; publish beta release, verify CI and anonymous downloads, preserve rollback and update local documentation.
