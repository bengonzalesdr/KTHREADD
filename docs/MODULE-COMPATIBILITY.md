# IngrosWare 4.0 → KTHREADD compatibility

Inventory: **82 annotated modules/services** found in the supplied JAR by static decompilation. Original uses reflective discovery of toggleable modules plus explicit Commands/Keybinds/Overlay services. An annotation is evidence of presence, not proof that original discovery succeeded.

Binary SHA-256: `078622b4bdc9a11f4a32dadce2263bca8d00bc37999147bb0027faeb8f13c44f`. Constructor identifies version 4.0; old mcmod.info says 1.0.

“Implemented equivalent” means a working modern implementation at the documented reduced scope, not original settings/algorithm parity. Runtime verification is separate in TESTING.md. Original annotated setting names are in original-module-inventory.json.

| Original category | Module / service | Status | Note |
|---|---|---|---|
| COMBAT | AntiChainPop | Not implemented | No placeholder registered. |
| COMBAT | AutoArmor | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| COMBAT | AutoFeetObby | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| COMBAT | AutoLog | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| COMBAT | AutoOffhandCrystal | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| COMBAT | AutoOffhandGap | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| COMBAT | AutoTotem | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| COMBAT | AutoTrap | Not implemented | No placeholder registered. |
| COMBAT | CrystalAura | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| COMBAT | FastBow | Not implemented | No placeholder registered. |
| COMBAT | FastCrystal | Not implemented | No placeholder registered. |
| COMBAT | KillAura | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| COMBAT | NoVelocity | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| COMBAT | SelfTrap | Not implemented | No placeholder registered. |
| MOVEMENT | ElytraFly | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| MOVEMENT | EntitySpeed | Not implemented | No placeholder registered. |
| MOVEMENT | FastIce | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| MOVEMENT | Flight | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| MOVEMENT | HoleTP | Not implemented | No placeholder registered. |
| MOVEMENT | Jesus | Not implemented | No placeholder registered. |
| MOVEMENT | Noclip | Not implemented | No placeholder registered. |
| MOVEMENT | NoPush | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| OTHER | NoRotate | Not implemented | No placeholder registered. |
| MOVEMENT | NoSlowDown | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| MOVEMENT | Phase | Not implemented | No placeholder registered. |
| MOVEMENT | Safewalk | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| MOVEMENT | Scaffold | Not implemented | No placeholder registered. |
| MOVEMENT | Speed | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| MOVEMENT | Sprint | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| MOVEMENT | Step | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| MOVEMENT | Strafe | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| MOVEMENT | Timer | Not implemented | No placeholder registered. |
| OTHER | AntiTabComplete | Not implemented | No placeholder registered. |
| OTHER | AutoCrystalArmor | Not implemented | No placeholder registered. |
| OTHER | BuildHeight | Not implemented | No placeholder registered. |
| OTHER | Crasher | Excluded | Packet flooding/server crashing excluded. |
| OTHER | FakeVanilla | Excluded | Client identity spoofing omitted. |
| OTHER | FastBreak | Not implemented | No placeholder registered. |
| OTHER | Freecam | Not implemented | No placeholder registered. |
| OTHER | GhostHand | Not implemented | No placeholder registered. |
| OTHER | MCF | Not implemented | No placeholder registered. |
| OTHER | NoDismount | Not implemented | No placeholder registered. |
| OTHER | NoEntityTrace | Not implemented | No placeholder registered. |
| OTHER | NoHurtcam | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| OTHER | NoLag | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| OTHER | Notifications | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| OTHER | PortalGod | Excluded | Obsolete invulnerability exploit omitted. |
| OTHER | PortalGui | Not implemented | No placeholder registered. |
| OTHER | Spammer | Excluded | Automatic message spam excluded. |
| OTHER | Speedmine | Not implemented | No placeholder registered. |
| OTHER | Suffixes | Not implemented | No placeholder registered. |
| OTHER | Timestamps | Not implemented | No placeholder registered. |
| OTHER | TotemPopCounter | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| OTHER | XCarry | Not implemented | No placeholder registered. |
| PLAYER | AntiEffects | Not implemented | No placeholder registered. |
| OTHER | AntiFriendHit | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| PLAYER | AntiHunger | Not implemented | No placeholder registered. |
| PLAYER | AntiVoid | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| PLAYER | AutoMine | Not implemented | No placeholder registered. |
| PLAYER | AutoTool | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| PLAYER | Blink | Not implemented | No placeholder registered. |
| PLAYER | FastEXP | Not implemented | No placeholder registered. |
| PLAYER | Godmode | Excluded | Obsolete server exploit omitted. |
| PLAYER | HoleFiller | Not implemented | No placeholder registered. |
| PLAYER | InvWalk | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| PLAYER | ItemStacker | Not implemented | No placeholder registered. |
| PLAYER | NoBreakAnimation | Not implemented | No placeholder registered. |
| RENDER | AntiArmorRender | Not implemented | No placeholder registered. |
| RENDER | ChunkUpdates | Not implemented | No placeholder registered. |
| RENDER | ClickGui | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| RENDER | FullBright | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| RENDER | Holes | Not implemented | No placeholder registered. |
| RENDER | LogoutSpots | Not implemented | No placeholder registered. |
| RENDER | NoBossBar | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| RENDER | PlayerList | Not implemented | No placeholder registered. |
| RENDER | PortalESP | Not implemented | No placeholder registered. |
| RENDER | ShulkerPreview | Not implemented | No placeholder registered. |
| RENDER | Trajectories | Not implemented | No placeholder registered. |
| RENDER | Visuals | Not implemented | No placeholder registered. |
| OTHER | Commands | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| OTHER | Keybinds | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |
| RENDER | Overlay | Implemented equivalent (reduced scope) | See TESTING.md and README for precise scope; no original binary code copied. |

## Added utilities

Profiles, local autosave, per-server/save identities, friend commands, Privacy, draggable HUD editor, Search, Waypoints, EntityESP/StorageESP/Tracers/Nametags, AutoWalk, Yaw, HighwayNavigation, NoWeather, AutoRespawn and generic ChestStealer are modern additions or reused utility implementations. They are not counted as ports of absent IngrosWare modules.

## Excluded non-module paths

AltCommand, TheAltening service switching/session replacement, global TLS trust/hostname overrides, arbitrary external module/command loading, old Mixin/ASM/ReflectASM and event libraries are excluded. No legacy code/assets or decompiled files are shipped.
