# port.2 compatibility update — 2026-10-22

Fixed the crash when running Creepy SalHack and KTHREADD together. Both clients formerly used exclusive `@ModifyConstant` hooks for the same item-use slowdown value. They now use MixinExtras `@ModifyExpressionValue`, which composes with the other client's value rather than competing for the instruction. Both client JARs must be updated.

The actual remapped release JARs were launched together with **Minecraft 1.20.4, Java 17, Fabric Loader 0.19.5 and Fabric API 0.97.2+1.20.4** in an isolated production game directory. Both menus opened, both clients entered/rendered a temporary integrated world, and all four NoSlow states were checked against actual item-use input: both off = 0.2; either one on = 1.0; both on = 1.0. All **9 combined-client regression checks passed**. Singleplayer smoke testing is not multiplayer/anti-cheat validation.

Evidence: `verification/coexistence-results.txt` and `verification/coexistence-launch.log`. Original per-client smoke records remain below/as separate evidence; current release validation additionally includes this production compatibility run. The two installed JARs were backed up before replacement; no user configuration/world was changed.

# KTHREADD 4.0-port.1 — 2026-10-22

- Reconstructed a maintainable Java 17 / Fabric 1.20.4 subset from static IngrosWare 4.0 feature references. Original binary remains untouched.
- Renamed runtime branding, mod ID (`kthreadd`), Java package (`dev.kthreadd`), commands (`/kthread`), profiles directory and release artifacts to KTHREADD. No old watermark/font is carried over.
- Restored original five categories, crimson category panels and Delete GUI bind; added configurable theme/scale/rounding/opening animation, search, bind capture, draggable/resettable panels and HUD editor.
- Reimplemented selected original combat/movement/player/render modules, including centralized offhand priority, basic CrystalAura/KillAura/AutoFeetObby, normal AutoLog, local NoVelocity/NoPush, FullBright, NoHurtcam, NoBossBar, particle-budget NoLag and TotemPopCounter.
- Added modern profiles/autosave/per-server-save identities, local friend commands, Privacy, waypoints/Nether conversion, entity/storage overlays, bounded scanning and diagnostic logging. Added actual TargetHUD, armor and inventory elements.
- Excluded original auth/session/TLS manipulation, external plugin loading, crash/spam and obsolete invulnerability behavior. No legacy libraries or decompiled code included.
- Built the remapped JAR, tested core logic and an actual isolated Minecraft client/integrated world, and reviewed screenshots and important code paths. Full compatibility and validation limits are in TESTING.md and MODULE-COMPATIBILITY.md.

This is an independent **partial port / preview**, not full original feature parity or an official upstream release. Freecam and many original modules remain absent; CrystalAura and several equivalents are deliberately reduced.
