# KTHREADD — IngrosWare 4.0 modernization

The user requests the same Minecraft 1.20.4 Fabric / Java 17 modernization workflow as the previous port, with the identity KTHREADD. Work is authorized through implementation, compilation, isolated runtime testing and packaging. Deliver the largest genuinely verified subset with explicit omissions.

## Reference findings and design
Only ingrosware-v4.0.jar is supplied. Its initializer identifies 4.0; mcmod.info reports 1.0. Static decompilation is reference-only; no legacy code is executed or copied. No upstream repository/license was located in the initial public search or JAR notices. Do not redistribute the decompiled source, fonts/assets or binary. Identify original authors x3, Xenforu and carroteater in notices.

Five original categories: Combat, Movement, Player, Other, Render. Preserve actual module labels, compact dark category panels and original GUI crimson RGB 179/0/46; HUD RGB 234/38/38. Brand runtime UI, IDs, commands, config, artifacts and packages KTHREADD/dev.kthreadd. Delete opens GUI (original LWJGL bind 211); Right Shift opens the added HUD editor. No SalHack watermark/assets.

## Execution
1. Inventory every annotated original module/persistent service and original settings; statically inspect initializer, external loading, authentication/TLS, render/network and file paths. Preserve evidence/hash; exclude account/session/TLS manipulation and packet crashers.
2. Reuse previously authored/tested Fabric module/settings, atomic profiles, rendering, commands and build foundation with attribution. Adapt Ingros labels/categories. No decompiled method bodies are imported.
3. Rebuild shared modules: AutoTotem/AutoArmor, AutoFeetObby, basic CrystalAura, AutoTool, movement, FullBright/Overlay/ClickGui. Add selected Ingros-specific offhand utilities, AutoLog, KillAura with cooldown/friends, Flight/Strafe/FastIce/InvWalk, NoPush/NoVelocity, NoBossBar/NoHurtcam/NoLag and TotemPopCounter using modern hooks. Registered toggles must perform real behavior.
4. Extend profiles with friends and draggable category/HUD positions. Add TargetHUD, armor and inventory overlays. Keep profiles/search/privacy/waypoints labeled as additions. Automation defaults off; settings groups are explicitly non-toggleable.
5. Run core regressions, wrapper build and artifact validation, then isolated real Minecraft smoke tests covering GUI, profiles, offhand priority, inventory actions, mixins and mixed rendering. Existing worlds/launcher profiles remain untouched.
6. Independently review defects, fix/retest, and package release-kthreadd/KTHREADD-1.20.4.jar, complete source ZIP, optional dependency bundle and documentation/audit/testing evidence.

Development tests do not prove multiplayer/anti-cheat, full original combat algorithms or production-launcher install. Preserve the original JAR and previous project/output. No external publishing.
