# Combat and inventory upgrade audit

Scope: Minecraft 1.20.4 / Java 17. Existing module identities, profile keys and public helpers remain compatible. No module registration changes are required.

| Module | Executable change |
| --- | --- |
| AutoTotem | Shares one safe offhand transaction with utility offhands; caches loaded-crystal exposure by player/world/tick and reserves the two strongest hits from at most sixteen nearest crystals within twelve blocks. Choice changes, player changes and blocked screens reset delay state. |
| AutoOffhandGap | Uses the stronger enabled utility health threshold; accepts regular or enchanted golden apples; preserves an already held totem when apples are absent; can fall back to an available totem when AutoTotem is enabled. |
| AutoOffhandCrystal | Shares utility health/crystal checks, fallback, cursor guard and one-transaction ownership; preserves current offhand if a requested protection item is unavailable. |
| AutoArmor | Retains whole-set armor/toughness/enchantment ranking and safe source-slot replacement. Adds a progressive durability penalty below fifteen percent, pauses item use, schedules delay with player ticks and resets across player changes/toggle. Missing armor with an inventory replacement now counts as urgent maintenance. |
| AutoCrystalArmor | Runs the same armor decision around live crystals using a spatially bounded query, respects screens/item-use guards and avoids duplicate work while AutoArmor is enabled. |
| AutoTool | Ranks suitability before raw speed on tool-required blocks, includes Efficiency, avoids configured low durability, skips air/unbreakable targets and item use, claims actual slot changes, and restores only its own selection. Manual selection and player changes release ownership. New settings: MinDurability (3), Restore (true). |
| ChestStealer | Can merge matching items into partially filled stacks even when every inventory slot is occupied; respects item/component compatibility and stack limits. Skips untakeable/unreceivable items, rotates container search after a transfer, resets on screen-handler changes and claims each transfer. |
| AutoFeetObby | Uses the common collision/reach/support/ownership guard, adds optional floor scaffold support, pauses during item use and resets pacing on disable or invalid state. New settings: FloorSupport (true), Rotate (true). |
| AutoTrap | Keeps a valid target stable; orders lower supports before upper rings, scaffolds low ring floors, adds the missing tall-mode roof support, falls back to obsidian when requested ender chests are missing, pauses item use and disables only after observed world completion. |
| SelfTrap | Preserves supported roof/cage building but resets invalid pacing, pauses item use, and waits for the actual roof/cage before DisableOnPlace; accepting a scaffold interaction no longer disables the module prematurely. |
| KillAura | Checks vanilla cooldown/TPS pacing before taking the action owner; SwordOnly influences weapon selection. AutoSwitch keeps the owned weapon through vanilla attribute updates, restores on target loss/disable and yields restoration to manual selection. Adds deterministic target ties, eye-to-box range checks, nonpositive-damage guards and proper Jump critical postponement. |
| Aimbot | Remains the shared Packet/Client rotation service; look updates used by guarded melee/placement continue to honor its selected mode. No independent unsolicited target tracking was added. |
| Criticals | Excludes crystal/nonliving targets, flying/liquid/riding/climbing/sprinting/blind/slow-fall cases; requires usable attack damage. Jump mode postpones attack until natural descent; packet mode checks cooldown/headroom and emits at most one offset pair per player tick even when Aura and mixin both call it. Packet attempts are not server confirmation. |
| AntiChainPop | Extends its temporary surround window on repeated local pops, schedules by player ticks, releases owned protection on module disable/disconnect/player change, and preserves an independently enabled surround. |
| FastCrystal | Clears local use delay only while use is pressed, a live player/world exists, no screen is open and the player is not already consuming an item. |
| FastBow | Replaces the fixed three-tick shot with configurable ChargeTicks (default 20; range 3–40), checks usable ammunition/creative state, screens/cursors and action ownership before normal bow release. |

## Shared support details

Block placement validates world border/build height/loaded target chunk, item collision fit, occupied target, reach and a collidable support. Sneak is represented both locally and with ordinary press/release packets for the interaction, preventing chest/furnace supports from opening. Prior sneak and selected slot are restored in finally. Partial collidable supports (including chests) are usable. At most one support face is attempted per target invocation; a locally accepted interaction means only an attempted action, never authoritative placement confirmation.

CombatSupport.weapon() remains available and now excludes unusable/near-broken weapons, accounts for vanilla Strength/Weakness arithmetic, and prefers the held slot on equal damage. Additional helpers for CrystalAura integration: weapon(boolean swordOnly), weaknessRequiresWeapon(), weaponCanBreakCrystal(int hotbarSlot), canAttackWithCurrentItem(). Existing explosion damage APIs are unchanged. CombatInventoryPolicy contains the game-independent regression-tested decisions.

## Verification and limits

- Wrote six policy regressions covering clustered damage, missing utility fallback, full-inventory stack merging, configured bow charge, armor durability pressure and status-effect damage arithmetic.
- Confirmed red: the old single-hit threat/three-tick charge behaviors failed two assertions. Implemented the corrected policy; targeted tests passed.
- Subsequent full offline Gradle test/compile run succeeded. The primary integration pass must rerun after the final small support/range/urgency source changes; workers deliberately stop concurrent builds to prevent shared-report races.
- No live-server acceptance, anti-cheat compatibility, real-inventory transactions or combat performance is claimed by these unit tests. Dedicated game QA should exercise container supports, inventory lag/rejection, repeated totem pops, charged bows and Jump critical timing.
- Two-hit threat is deliberately conservative, with no hurt-resistance reduction assumption; it is bounded to sixteen nearest crystals and can omit a farther exposed threat in a very crowded scene. It can keep a totem selected until loaded threats disappear. It is not an unbounded sum of every loaded crystal.
- Armor swaps use normal pickup transactions and never drop inventory items. An unexpected nonempty cursor causes subsequent inventory automation to wait for the user/server rather than guessing recovery clicks.
- Supports must already exist in local world state; normal reach, collision and server restrictions apply. AutoTrap/SelfTrap can require multiple ticks and cannot complete without a reachable scaffold path.
- Strength/Weakness weapon ranking covers vanilla sword/mining-tool damage; arbitrary server/custom attribute modifiers are not predicted. The current-hand guard uses the live attack attribute.
