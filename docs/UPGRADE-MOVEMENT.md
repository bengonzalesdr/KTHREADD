# Movement and general module upgrade audit

Existing module names, public movement API and all legacy settings are retained. New settings below have actual consumers. This is ordinary client input/velocity control, not a packet bypass or a promise that multiplayer servers will accept movement.

## Movement ownership and input

A single shared policy selects horizontal velocity ownership from current enabled modules and player context: Flight, then active ElytraFly, then FastIce on ice, then grounded Speed, then airborne Strafe. This does not depend on alphabetical tick order. The ordinary speed modules yield while riding, spectating, dead, swimming, in lava, climbing, using an item without NoSlowDown, sneaking, or using vanilla flight. All direct controls pause in menus and when the window is unfocused. AntiVoid is an emergency vertical intervention and excludes flight/gliding.

Physical key restoration supports keyboard and mouse bindings. AutoWalk and InvWalk release owned input on disable or invalid contexts and restore actually held keys on returning to the game. InvWalk permits inventory/container screens only; this deliberately excludes sign/book editors and arbitrary text-entry menus in addition to chat, anvil and creative search fields.

| Module | Actual behavior change or audited existing behavior |
| --- | --- |
| Sprint | Requires meaningful forward input, hunger/flight permission, no collision/sneak/liquids, living unmounted player and game focus. `WhileUsingItem` defaults false. Tracks its own sprint request and releases when conditions end. |
| AutoWalk | Stops requesting forward in menus, on focus loss, death, spectator or riding; restores physical keyboard/mouse binding instead of polling mouse codes as keyboard codes. |
| Speed | Shared ownership and ordinary movement guards. Diagonal normalization retained through a tested pure vector policy. |
| ElytraFly | Applies only during existing gliding, yields to Flight; `Sink` controls neutral descent. Simultaneous jump+sneak neutralize vertical input. |
| Step | Existing `Height` applies only to the living local player during ordinary grounded non-sneaking movement. |
| Safewalk | Ordinary grounded guard; `WhileJumping` defaults false so intentional jumping can leave an edge. |
| NoSlowDown | Both active-item movement multipliers retained as mixin targets with exact match count 2. Player/riding/death guards; `WhileSneaking` controls use while crouched. Exact named game bytecode confirmed sideways and forward multipliers, with no other 0.2 constants in tickMovement. |
| Yaw | `OnlyMoving` defaults true, avoiding forced idle aiming; aligns head and body yaw and respects focus/player context. Existing Increment retained. |
| AutoRespawn | `Delay` in ticks, default 20; one request per death instead of repeated requests every second; resets on recovery/toggle. |
| Flight | Highest movement priority; dead/riding/spectator/focus guards. Jump and sneak cancel each other. Disabling no longer wipes existing vertical/horizontal velocity, so it cannot erase knockback or create an abrupt stop unrelated to the next controller. |
| Strafe | Shared airborne ownership; no liquid/lava/climbing/sneak/item-use interference. |
| FastIce | Wins over Speed only on grounded ice; retains player-only changes rather than global friction mutation. |
| InvWalk | Physical key handling and owned cleanup; inventory/container screen allowlist excludes non-widget text editors. `Jump` and `Sprint` separately control those inputs. |
| AntiVoid | Spectator/dead/riding/vanilla flight/gliding/Flight exclusions. `Margin` and `Boost` configure the existing rescue, with no safe-position teleport or guarantee. |
| AutoLog | Living multiplayer player/network guard, consecutive danger `Delay`, reset on recovery/toggle or a found totem. Existing health+absorption and CheckTotems retained. |
| NoVelocity | Existing scaling math retained; independent EntityVelocity/Explosions toggles require NetworkMixin integration by primary worker. |
| AntiFriendHit | Existing direct friend attack cancellation audited; already local and name-based, no cosmetic settings added. |
| NoPush | Existing local-only collision cancellation audited; other entities/global physics untouched, no cosmetic settings added. |
| NoBossBar | Existing cancellable boss rendering hook audited; no redundant settings added. |
| NoHurtcam | Existing cancellable hurt tilt hook audited; no redundant settings added. |
| NoLag | Existing per-tick particle cap retained; `MaxDistance` culls distant particles before they consume the nearby budget. |
| TotemPopCounter | Existing connection-scoped counter retained; `Notify` and `Self` toggles require NetworkMixin integration by primary worker. |
| AutoOffhandGap / AutoOffhandCrystal | Registration and legacy Health/CrystalCheck/Delay untouched; actual offhand upgrades belong to combat worker. |

## Verification and integration

- MovementPolicyTest covers mutually enabled movement priority, unsupported ground/air cases, diagonal speed normalization, empty input, once-per-death delayed respawn, and spectator/dead/rider/flying void exclusions.
- Test source was written before its production policy; first compile correctly reported the absent MovementPolicy. Production compilation passed. Targeted and full Gradle invocations returned success, but parallel workers overwrite shared reports, so primary reserves the final sequential aggregate run. Independently compiled the movement policy and test class with JDK 17, invoked all five JUnit assertion methods in an isolated output directory, and confirmed all five passed. Existing Gradle/API deprecation notices remain.
- No new mixin declarations or ModuleManager begin-tick hook are needed.
- Primary NetworkMixin integration: guard local entity redirect with NoVelocity.EntityVelocity; explosion redirect with NoVelocity.Explosions; check TotemPopCounter.Self before incrementing local player's count; guard notification with Notify without suppressing count updates.
- Runtime QA remains required for toggling input modules in inventory/creative/anvil/sign/book screens, mouse-bound forward input, focus loss/disconnect, all-enabled movement on ice/ground/air/gliding, and mixin transformation. Static build/tests do not establish server acceptance.
