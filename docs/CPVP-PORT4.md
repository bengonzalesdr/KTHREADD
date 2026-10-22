# KTHREADD 4.0-port.4-cpvp

This update concentrates on CrystalAura and survival coordination on Minecraft 1.20.4. It retains the existing KTHREADD modules, settings, profiles, and SalHack priority integration. It is a tested development release, not a claim of competitive superiority or complete historical Ingrosware parity.

## Crystal decisions

- Break candidates are ranked, instead of attacking the first acceptable entity. Placement and breaking use the same lethal-priority and damage-trade policy. Overkill is capped at target health when scoring.
- Placement shortlisting considers all four nearest eligible targets, with a configurable evaluation budget (32 by default, up to 128).
- Damage/exposure results are reused within one decision cycle. Nothing is cached across ticks, so changed blocks, armor, effects, and health are reconsidered.
- PredictionTicks forecasts player motion for up to three ticks, caps displacement at 1.5 blocks, rejects teleport-sized samples, and checks block collisions along the forecast path. Translated-box exposure does not mutate entities. Enemy scoring uses the lower of current and forecast damage; defensive checks use the higher. This is a conservative estimate, not knowledge of future movement.
- LethalPriority allows a lethal hit below MinimumDamage. ArmorFaceplace permits low-damage pressure when player armor is missing or below the selected durability percentage. Both retain self/friend safety checks.
- SafetyMargin defaults to one health point. Break plus placement and multiple placements share a cumulative damage budget for the current cycle, including protected friends. This does not model every possible incoming enemy explosion or damage across future ticks.

## Confirmation and scheduling

Accepted local interaction is recorded as pending, not as server success. Fabric entity-load events correlate nearby crystal spawns to pending bases. Removal/unload frees that base immediately. A spawn cannot prove ownership when another player places at the same base simultaneously.

Unconfirmed placements expire after RetryDelay, optionally extended by twice the reported player-list latency plus 150 ms (capped at 3000 ms). Break attempts suppress duplicates until removal or AttackRetry/adaptive timeout. World changes and disabling the module clear tracking. Timeouts mean unconfirmed attempts; they are not proof of server rejection.

PlaceDelay now defaults to one tick. The normal client tick still bounds execution to about 50 ms at 20 TPS. This version does not attack directly from network callbacks, predict entity IDs, or remove live crystals speculatively.

The module scheduler prioritizes offhand transactions, armor, surround, then CrystalAura. Other combat modules and AutoTool yield for a tick after an action owner claims inventory/rotation use. This covers KTHREADD's integrated routines; it cannot coordinate arbitrary third-party mods. SalHack retains its existing paired-module priority behavior.

AutoTotem's new CrystalDanger option evaluates loaded crystal explosion damage within 12 blocks. A potentially lethal crystal can request a totem above the usual health threshold and bypass the ordinary swap delay. It requires an available inventory totem and a safe inventory state.

## Controls and diagnostics

Open KTHREADD with **Delete**, then right-click CrystalAura to change its settings. Existing saved settings remain in effect; the new defaults do not overwrite an existing PlaceDelay or candidate budget. Creepy SalHack still opens with **Left Arrow**.

Enable CrystalAura > Diagnostics for a movable HUD readout, or run `/kthread combatstats`. It shows current activity, place/attack attempts, correlated confirmations, pending/expired attempts, last selected damage, cycle duration, damage evaluations/cache hits, and last confirmation delay. The `rays` field counts damage/exposure evaluations, not individual raycast samples.

Start with MinimumDamage 4, MaxSelfDamage 4, SafetyMargin 1, PlaceDelay 1, AttackSpeed 20, CandidatesPerTick 32, PredictionTicks 1, MultiPlace off, ProtectFriends on. These are starting values, not a server-specific competitive preset. Add teammates to KTHREADD's friend list with `/kthread friend add NAME`.

## Verification scope

The accompanying verification directory contains fresh unit, isolated game, and combined-client results. The tests cover policy ranking, duplicate suppression, delayed confirmations, actual server crystal placement/breaking, non-mutating forecast exposure, and emergency totem behavior. Simulated confirmation delays test state logic; they are not a remote-server latency benchmark. Remote-server combat, every anti-cheat configuration, and competitive win rate remain unmeasured.
