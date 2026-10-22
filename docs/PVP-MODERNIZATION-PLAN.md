# KTHREADD PvP expansion

The user now authorizes making KTHREADD the PvP-focused companion to Creepy SalHack, improving all combat/crystal PvP behavior and removing redundant utility features when SalHack covers them. The earlier subset is not the new acceptance target.

## Source changes staged

- Independent crystal damage evaluation: integer vanilla base damage, sampled block exposure, difficulty, armor/toughness, protection enchantments and resistance. Friend exclusions, lethal-friend rejection, maximum self damage and estimated suicide checks.
- Separate placement/break timing, bounded damage candidates, placement retry tracking, main/offhand crystals, weakness weapon selection, damage/nearest ranking, faceplace threshold, optional visible rotations, burst self-damage limits and placement outline.
- KillAura target selection: FOV, health and distance ordering; persistent/switch target modes; normal/smoothed rotations, modern cooldown, optional server-TPS pacing, category/invisibility/friend filters and weapon selection.
- AutoTrap, SelfTrap, FastCrystal, FastBow, Criticals, local-pop-triggered temporary surround, and crystal-proximity armor selection. Armor compares estimated blast/melee damage with the enchantment cap and preserves swapped equipment in inventory.
- Typed mode/text settings and JSON compatibility infrastructure. Existing GUI panel positions retained.
- SalHack CombatCoordinator defaults to deferring equivalent combat actions to enabled KTHREADD modules, including offhand automation. Each client remains independently loadable. This interoperability is not runtime-verified yet.

## Acceptance still required

1. Compile both projects and run the complete meaningful unit suite. New whole-project changes have not been built since approval review became unavailable.
2. Exercise inventory cursor/server-rejection paths, target/friend protection, actual crystal placement/breaking and damage predictions in an isolated integrated server.
3. Exercise every new mixin in the game and verify disabled behavior stays vanilla.
4. Test both remapped production JARs together: one combat owner, no duplicate hotbar/slot transactions, correct NoSlow/velocity composition and clean disconnect/respawn.
5. Compare all original Ingros combat/settings individually. The new implementations change some algorithms; module names alone are not parity evidence. Original pSilent/event rotation, anti-stuck timing, announcer behavior and packet/server exploit behavior are not fully reproduced.
6. Review utility dependencies, commands and HUD before trimming duplicate KTHREADD utility modules. No utility removal has been accepted as verified yet.
7. Package new JAR/source archives only after verification; keep the installed tested port.2 builds until then.

## Current verification

The earlier approval-review availability blocker is resolved. Both projects now compile and pass their unit suites. KTHREADD passed its expanded isolated-server game test, including real crystal placement/breaking/explosion and a server-accepted melee attack. Both remapped JARs passed the combined production regression, including combat ownership and velocity composition. See PORT3-TESTING.md for the fresh counts and evidence. This validates the tested preview scope; it does not complete every original setting or establish remote-server/anti-cheat acceptance. Utility trimming and the remaining fidelity work above have not been completed.
