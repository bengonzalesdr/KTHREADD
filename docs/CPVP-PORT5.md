# KTHREADD 4.0-port.5-cpvp

This update adds deterministic combat decisions and local diagnostics. It contains no AI model and makes no competitive win-rate claim. Minecraft remains 1.20.4 / Fabric / Java 17.

## Changes

**Reactive breaking.** With ReactOnSpawn enabled, a loaded server crystal can trigger a break-only decision on the client thread. The engine rechecks its current range, visibility, damage, target eligibility, eating/mining pauses, cooldown, and action ownership. Emergency offhand needs, health at or below 8, or armor at or below 10% durability defer the reaction. No placement occurs inside the entity-load callback. Reentrant decisions are rejected, and reaction scans are limited to four per 50 ms window. Missed or deferred opportunities remain eligible for the normal tick scan.

**Urgent action priority.** Offhand decisions still run first. CrystalAura precedes routine armor/surround maintenance while healthy. At 8 health points or lower, or with critically damaged worn armor, armor and surround get first opportunity. A claimed action continues to block competing KTHREADD combat routines for that scheduler pass. This is not a global scheduler for arbitrary third-party mods.

**Damage reservations across cycles.** ReservePendingDamage includes pending placements, tracked confirmed placements, and outstanding attacks when evaluating the next crystal. Centers are deduplicated, and the candidate's own center is excluded so breaking an owned crystal does not double-charge its damage. Damage is recalculated against current armor, health, effects, movement forecast, and terrain. Placement rejection cancels its reservation; unconfirmed placement timeout and entity removal release their relevant tracking. Reservations deliberately err toward caution. They do not predict all enemy explosions, server damage immunity, or packet ordering between health and removal updates.

**Stable target and broader search.** TargetLockMs defaults to 750 ms and keeps a valid focus despite small changes in nearest-player order. Invalid/dead/out-of-range/friend targets are dropped. Placement shortlisting favors the focus's forecast position while interleaving candidates near other eligible targets. Final damage ranking can still favor a lethal opportunity on another target. Prediction collision checks are cached within a decision, not across world changes or ticks.

**Fight recorder.** FightRecorder defaults on and keeps at most 4096 events in memory. It records decisions, action attempts, correlated placement confirmations/removals, deferrals, and per-decision rejected-candidate totals. It does not collect names, server addresses, coordinates, chat, or credentials. Older events roll out; dropped-event count and summary counts are retained. Enabling the module starts a new recording. Disabling preserves recorded events for export, although the live counters reset.

## Using the diagnostics

1. Open KTHREADD with **Delete**, then right-click **CrystalAura**.
2. Leave ReactOnSpawn and ReservePendingDamage enabled initially. Enable Diagnostics for the on-screen readout.
3. After a fight, run `/kthread combatreport`. It writes a JSON file into `kthreadd-reports` inside that Minecraft instance's game directory and prints its full path in chat.
4. `/kthread combatstats` shows the live summary without writing a file.

In reports, `attack:spawn reaction` distinguishes event-triggered attacks from scheduled attacks. `rejected candidates` events carry the number of evaluations rejected for their reason in `value`; the `counts` map counts events, not individual rejected candidates. `decision` values are evaluation durations in milliseconds. Placement confirmation delay is not the same as network ping. Entity-load correlation alone cannot prove ownership if another player places at the same base simultaneously.

Existing profiles preserve their old settings. AttackSpeed 20, PlaceDelay 1, CandidatesPerTick 32, and PredictionTicks 1 are reasonable starting values for experimentation; they are not a measured best configuration for your server. Keep MultiPlace off while diagnosing timing. Retain self/friend safety protections.

## Verification and limits

The release bundle includes unit results, isolated-game logs, and combined SalHack/KTHREADD production-loader checks. New game checks prove that an entity-load callback causes an attack which damages a real local-server target, and that a local report contains those actual events. Core tests cover reservation lifetime, rejected placement cleanup, recorder bounds, and urgency selection.

Tests use temporary local worlds; combat fixtures use Resistance V for controlled player protection. Remote-server performance, variable-latency fights, all settings, and anti-cheat acceptance have not been benchmarked. This update does not add inventory acknowledgments where the protocol provides none, speculative entity-ID attacks, or an AI component. Older port.4 documentation describes the preceding implementation.
