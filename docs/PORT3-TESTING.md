# KTHREADD port.3 preview testing

Version: `4.0-port.3-dev`. The remapped Java17/Fabric 1.20.4 JAR was built successfully, passed 19 unit tests and 41 targeted game checks. The exact same binary then passed 18 combined production checks with the other client on Fabric Loader 0.19.5. SHA256: `1bab4c7052f4e99a4797b29558fb4fad2de0056483ddefd11bb225a76b7791e7`.

SalHack's game checks cover GUI/profile/HUD behavior, actual inventory transactions, typed offhand policies, curse/elytra armor cases, cursor protection, Freecam camera/input cleanup and shulker tooltip NBT. KTHREADD's checks cover GUI/profile/HUD, offhand/armor, friend protection, velocity/particle/pop hooks, real crystal placement/breaking/explosion and a server-accepted melee attack. The damage fixture uses Resistance V to protect the temporary test player; it is not a general survival or damage-prediction accuracy guarantee.

Combined production checks cover both menus and world loading, all four NoSlow states, paired combat ownership/resumption, offhand ownership, aura critical arbitration and both velocity hooks with the preferred KTHREADD half-strength setting. QA code is a separate test mod and is absent from these release JARs.

These are targeted checks in isolated local game directories and temporary integrated worlds. Full original feature/setting parity, every mode, all server rejections, long sessions, external mod packs, remote-server PvP and anti-cheat acceptance have not been comprehensively verified. The full-parity audit and group fidelity reports retain the known gaps. This is a preview build.

Fresh logs, JUnit results and hashes are under `verification-port3/`. Older `verification/` and TESTING.md reports describe previous versions.
