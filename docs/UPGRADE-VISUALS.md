# Visual, HUD, UI and profile upgrade

## Module audit

| Module | Result |
|---|---|
| Colors | Click GUI header foreground switches between dark/white for bright/dark chosen accents. RGB/rainbow theme remains intact. |
| ClickGui | Searches setting names as well as module names/descriptions; matching settings expand automatically. Ctrl+F focuses/selects search, Escape clears a focused query. Full setting values/ranges are available in tooltips. Configuration-only panels no longer reserve an empty keybind row. |
| Overlay | Combat panel includes crystal/obsidian reserves, minimum armor durability and configurable low-armor warning; optional nearby-crystal count and offhand totem state. Target panel adds equipment, armor/minimum durability/range/pops and max-health-aware bar. Stats adds latency. Nearby crystal count is situational information, not a damage or safety prediction. |
| Privacy | Existing coordinate/waypoint/stash suppression preserved; new highway axis/offset remains suppressed. Notices are still cleared on activation by existing lifecycle. |
| Notifications | Consistent opaque readable background and remaining-life progress bar, retaining existing five-second bounded queue/lifecycle. |
| FullBright | Existing lightmap-only override retained unchanged; no potion or persistent gamma mutation introduced. |
| EntityESP | Friend/invisible filters, spectator exclusion, nearest-first bounded candidates/render count. Existing interpolated camera-relative boxes, line width and through-wall selection preserved. |
| StorageESP | Incremental loaded-chunk scan (two chunks/tick, at most 512 block entities/chunk); nearest-first refresh; separate container/ender-chest filters; cached positions revalidated before drawing. Unloaded/out-of-range/world-change cache cleanup. |
| Tracers | Friend/invisible filters, spectator exclusion, nearest-first render count cap. Existing inverse-view no-bob camera origin and through-wall pipeline preserved. |
| Nametags | Friend/invisible filters, spectator exclusion, count cap. Distance plus optional armor/minimum durability/pops. Existing interpolated world labels retained. |
| Waypoints | Configurable shared HUD/world range; HUD shows nearest eight matching-dimension markers first. Privacy gating unchanged. |
| StashFinder | Shares bounded incremental storage scan, counts storage only inside its own configured range, dimension/world-scoped notification deduplication capped at 4096 chunks. |
| Search | Overflow-safe repeating cube cursor, resets on changed block/range/origin/world; immediate cached-hit validation removes broken/unloaded blocks. Configurable line width and through-wall rendering using existing render layers. |
| HighwayNavigation | Nearest highway axis plus offset; signed turn toward nearest 45-degree heading. Location-derived axis/offset hidden in privacy mode. |
| NoWeather | Existing client-only rain/thunder render hooks retained unchanged; no server weather mutation. |

## Profiles and menus

Atomic profile replacement is retained. Each overwrite now validates the old file and atomically rotates a `.json.bak` containing the previous valid save. A corrupt primary loads a valid backup when available and preserves the corrupt file/evidence, keeping saves to that damaged name blocked until explicitly repaired or saved under another name. A later successful load clears the old error/block. Backups do not appear as profile names. Profile selection is clickable and scrollable, and listing no longer reads the directory every rendered frame.

## Bounds and limitations

Storage is eventually refreshed rather than synchronously rescanned. At maximum StashFinder radius a sweep covers at most 289 chunks / 145 ticks; chunks containing more than 512 block entities are intentionally truncated. Draw cache is capped at 4096 positions. Search holds at most 2048 positions and still obeys its existing per-tick scan budget, with bounded additional hit revalidation. Entity candidates use separate bounded nearest-candidate heaps for ESP, tracers and names; iterating loaded entities remains proportional to the loaded entity population. No hidden server chunk data is read. World labels retain the existing through-terrain text behavior. No external network services, personal worlds or private logs are added.

## Integration and verification

Registry additions are in ModuleManager (integrated by primary): EntityESP/Tracers/Nametags IgnoreFriends, IgnoreInvisible, MaxEntities; Nametags Equipment; StorageESP Containers/EnderChests; Waypoints Range; Search ThroughWalls/LineWidth (maximum 3, matching existing layer cache); Overlay ArmorWarning/CrystalThreat. Every setting is consumed. No command or lifecycle API additions required; existing reset/tick/render hooks suffice.

Profile regression tests were observed failing for backup recovery and stale-error/block clearing before implementation, then passing. Isolated visual/profile Gradle verification passed 14 tests (10 profile, 4 visual policy), covering cube wrap/coverage/range reset, highway diagonal projection, invalid/max-health/absorption bar values and profile recovery. Java compilation passed. Subsequent small rendering/filter/menu adjustments require primary aggregate compilation and runtime QA. Unit checks do not establish on-screen placement or live server behavior; primary owns screenshots, game QA and release verification.
