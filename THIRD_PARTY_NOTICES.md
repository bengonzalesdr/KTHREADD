# Attribution / license scope

KTHREADD is an independent reconstruction of gameplay concepts from the supplied **IngrosWare 4.0** binary. Its initializer identifies the original authors as **x3, carroteater, Xenforu**; mcmod.info lists x3, Xenforu and carroteater. Original architecture includes the `me.xenforu.kelo` framework. The binary's initializer says 4.0 while its old metadata says 1.0.

No canonical source repository or license granting reuse of IngrosWare's implementation was verified. The JAR contained no identified project license. Consequently **no decompiled IngrosWare Java, binary, font or asset is distributed here**. Module names/categories/settings and observed interface conventions are reference material. All target behavior is freshly authored or adapted from our previously authored Fabric foundation. Original authorship is attributed, not erased by the KTHREADD branding; no endorsement is implied.

The reusable Fabric foundation came from the separate prior modernization project in this workspace. Its included MIT notice is preserved in LICENSE. The actual reused core/modules were newly authored there; no vendored old Forge/KAMI/Slick/Schematica/RPC code or SalHack watermark/font was imported. This project's authored source is distributed under those MIT terms. That notice does **not** grant any license over the supplied IngrosWare binary or static reference output.

Dependencies / tooling (not shaded into the KTHREADD JAR):

- **Fabric Loader 0.15.11**, **Fabric Loom 1.6.12**, **Fabric API 0.97.2+1.20.4** — FabricMC, Apache-2.0: https://github.com/FabricMC/fabric-loader ; https://github.com/FabricMC/fabric-loom ; https://github.com/FabricMC/fabric . The optional official API JAR retains its embedded notices.
- **Yarn mappings 1.20.4+build.3**, FabricMC, CC0-1.0: https://github.com/FabricMC/yarn .
- **Gradle 8.6**, Apache-2.0: https://github.com/gradle/gradle . Wrapper generated from the official distribution; its official SHA-256 is pinned. The full distribution is not packaged.
- **JUnit Jupiter 5.10.2**, EPL-2.0, test-only: https://junit.org/junit5/ .
- **Minecraft 1.20.4**, Mojang/Microsoft, proprietary game license. No Minecraft JAR, assets, libraries, natives or test world is redistributed. Minecraft supplies runtime fonts, Gson, SLF4J and JOML.
- **CFR 0.152**, Lee Benfield, MIT: https://www.benf.org/other/cfr/ . Used only to statically inspect the supplied binary; the decompiler and full decompiled files are not in the release/source ZIP.
- **Eclipse Temurin Java 17**, build/runtime tooling: https://adoptium.net/temurin/releases/ . Not packaged.
