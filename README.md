# New Dawn – NeoForge 1.21.1

A port of Stefan Feldbinder's (Two) original Forge 1.7.10 terrain generator.
The Simplex noise composition, terrain heights, mountains and climate values are preserved.
An independent Java core computes the terrain; Minecraft handles the subsequent stages
using vanilla biomes, cave carvers, decoration, ores, structures and mobs.
All 51 non-river overworld biomes are selected according to climate, elevation and depth.
Biome changes and the ocean water-hole fix apply to newly generated chunks; existing
areas are not rewritten automatically.

## Build and run

Requires JDK 21. The Gradle Wrapper is included. The first build downloads Gradle and
the Minecraft/NeoForge dependencies.

```powershell
# For this PowerShell session only, if JAVA_HOME points to a different JDK:
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:GRADLE_USER_HOME = "$PWD\.gradle-user-home"
.\gradlew.bat build
.\gradlew.bat runClient
```

The distributable mod JAR is `build/libs/newdawn-1.0.0.jar`. The mod version stays at
**1.0.0** until the final release. You do not need to install the separate terrain-core
JAR; its classes are included in the mod.
Target platform: **Minecraft 1.21.1, NeoForge 21.1.250 or later within 21.1.x, Java 21**.
Other Minecraft versions are not declared binary compatible.

The build setup follows the [official 1.21.1 MDK](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle).
Development launches include Java libraries as described by
[ModDevGradle](https://github.com/neoforged/ModDevGradle#additional-runtime-dependencies).

## Create a New Dawn world

**New Dawn is selected by default** when creating a world. To use standard Minecraft
terrain, select the **Vanilla** world type on the **World** tab.
The other vanilla world types remain available as well.

A new dedicated server also uses New Dawn without special configuration.
`level-type=minecraft:normal` and the legacy value `level-type=default` both select
New Dawn. You can still select it explicitly:

```properties
level-type=newdawn:new_dawn
level-seed=123456789
```

For standard Minecraft terrain, set `level-type=newdawn:vanilla` instead, before
starting the new server world for the first time.

The Nether and End remain vanilla. Changing `level-type` does not convert an existing
world. This mod does not convert old 1.7.10 saves.

## Verification

```powershell
# Build, original-data regression checks and concurrency tests:
.\tools\verify.ps1 -JavaHome 'C:\Program Files\Java\jdk-21'

# Also generate, save and reload a server world, checking persisted chunk contents:
.\tools\verify.ps1 -Smoke -JavaHome 'C:\Program Files\Java\jdk-21'

# Standalone terrain benchmark:
.\gradlew.bat :terrain-core:benchmark

# Also measure CPU time and allocations in the core and Minecraft raw fill:
.\tools\verify.ps1 -Smoke -Benchmark -JavaHome 'C:\Program Files\Java\jdk-21'
```

`-Offline` uses cached build dependencies. The server check binds only to localhost on
an available port, creates a fresh world under `build/` and shuts itself down.
Reports and logs remain there. Development checks are excluded from the distributable JAR.

Further reading: [Architecture and limitations](docs/architecture.md),
[Performance and buffer API](docs/performance.md), [Verification evidence](docs/verification.md),
and [Biome selection and water boundaries](docs/biomes.md).
