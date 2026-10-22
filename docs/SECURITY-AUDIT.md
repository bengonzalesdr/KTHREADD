# IngrosWare 4.0 / KTHREADD security review

Date: 2026-10-22. **Static review is not a safety certification.** The supplied legacy JAR was never launched; only the freshly authored KTHREADD implementation was compiled and run.

## Inputs / method

Supplied `ingrosware-v4.0.jar`, SHA-256 **078622b4bdc9a11f4a32dadce2263bca8d00bc37999147bb0027faeb8f13c44f**. The preceding audit parsed all **800 class constant pools**, with zero parser errors, and enumerated native/embedded executable entries. This task additionally used **CFR 0.152 static decompilation** to inspect 203 Java reference files in the original client namespace, enumerate all **82 annotated module/persistent entries**, and read initializer, internal/external loading, session/authentication/TLS, module configuration and selected gameplay/render paths.

Full decompiled output is workspace reference material only and is not redistributed. Pattern evidence is in `reference-security-hits.txt`; the prior constant-pool scan and sensitive `javap` disassembly are retained as audit evidence. No dynamic/sandbox execution of the legacy code, complete control-flow audit, exhaustive dependency review or independent forensic certification occurred. Dynamically assembled/encrypted behavior may escape pattern-based inspection.

## Findings in the supplied binary

| Finding | Static evidence | KTHREADD disposition |
|---|---|---|
| Authentication/session endpoint replacement | `util/thealtening/AltService`: reflection modifies Yggdrasil auth/session URL fields and domain whitelist; TheAltening URLs use HTTP | Entire service and account/session replacement excluded |
| Unencrypted third-party API | `util/thealtening/TheAltening`: `http://api.thealtening.com/v1/` | Excluded; no HTTP client in the target mod |
| Process-wide TLS weakening | `SSLVerification`: X509TrustManager's checks immediately return; installs default SSL socket factory and hostname verifier accepting TheAltening auth/session host names | Excluded. Static presence establishes the capability, not that it executes at every startup; its activation path was not proven |
| Replaceable Minecraft sessions | `commands/AltCommand`, Minecraft session accessor/mixin | Excluded; no alternate-account system |
| Arbitrary external code loading | `ClassUtil`, ModuleManager and CommandManager discover/load/instantiate classes from local external JARs | Excluded; modules/commands explicitly registered and compiled |
| Crash/packet flood behavior | `module/modules/other/Crasher` | Excluded |
| Automatic message spam | `Spammer` | Excluded |
| Invalid-packet disconnect | `AutoLog` used invalid held-slot packet | Rewritten to ordinary multiplayer disconnect; does not send malformed packets |
| Obsolete invulnerability/client identity features | Godmode, PortalGod, FakeVanilla and related legacy hooks | Omitted |
| Broad configuration deletion | Original ModuleManager.save deletes existing files in its module configuration directory before writing | Replaced with validated names, per-profile atomic writes and corruption preservation |

The scanned archive contained no nested JAR/ZIP, native DLL/SO/dylib, EXE, batch or PowerShell payload. The scoped source/constant scans did not identify an explicit Discord webhook, startup persistence installer, OS shell-spawn chain or demonstrated credential-exfiltration flow to a hidden collector. **This does not establish absence of malware.** The alternate-account/TLS paths are enough reason not to reuse the old runtime as the basis of this port.

## KTHREADD implementation

- No old bytecode, decompiled methods, original packaged libraries, native RPC, updater, hidden downloader, telemetry, HTTP client, auth/session mutation, TLS overrides, subprocess execution, arbitrary class loader or OS persistence.
- Client initialization explicitly registers known modules and client commands. Dependencies are pinned; Gradle 8.6 checksum matches the official value. Standard Fabric/Minecraft loaders and game libraries remain external dependencies, not an independently audited trust guarantee.
- Gameplay network interactions go through the game interaction manager or targeted client-side inbound handlers: ordinary slot swaps, placement, attacks and disconnect. No malformed packets or crash/flood modules are included. Multiplayer/anti-cheat behavior is unverified.
- Runtime writes are local JSON under Fabric's `config/kthreadd/profiles`. Server/save identities are hashed into filenames, but friends/coordinates remain plaintext locally. Profiles are validated, sanitized, atomically replaced where supported, and damaged originals protected from overwriting.
- Privacy hides this mod's coordinate displays and stale location notifications. It does not sanitize F3, another mod, chat history or stored files.
- Friends are local case-insensitive account names; no remote UUID/profile API. AntiFriendHit and KillAura exclude direct friend attacks. CrystalAura's partial distance logic does not protect nearby friends from blast damage.
- Independent review found a persisted GUI header could be dragged behind the search strip; bounded placement, regression tests and a Reset panels control address it. Offhand fallback now uses the delay of the requesting utility instead of a disabled AutoTotem setting.

No claim is made that KTHREADD or its dependencies are entirely safe. Runtime smoke tests cover a limited isolated integrated world; long-duration, multiplayer, plugin compatibility and full attack-surface testing remain outstanding.
