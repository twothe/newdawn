# New Dawn – NeoForge 1.21.1

New Dawn is a terrain generation mod for Minecraft, now available for NeoForge 1.21.1.
It continues the Forge 1.7.10 version, retaining its distinctive base terrain and climate
fields while adding ridged multifractal mountains and climate-shaped cliffs.
An independent Java core computes the terrain; Minecraft handles the subsequent stages
using vanilla biomes, cave carvers, decoration, ores, structures and mobs.
All 51 non-river overworld biomes are selected according to climate, elevation and depth.
Mountain changes, biome changes and the ocean water-hole fix apply to newly generated chunks; existing
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

### Build from VS Code

Open the project root folder, save your changes, then press **Ctrl+Shift+B** or choose
**Terminal > Run Build Task**. The default **Build New Dawn** task runs the Gradle
Wrapper's `build` task, including checks, and produces `build/libs/newdawn-1.0.0.jar`.
Wait for `BUILD SUCCESSFUL` before using the JAR. No VS Code extension is required
for this task. The existing Client/Server launch configurations start Minecraft.

The Windows task uses JDK 21 at `C:\Program Files\Java\jdk-21`. If your installation
is elsewhere, update `windows.options.env.JAVA_HOME` in `.vscode/tasks.json`.
Other platforms use their configured Java environment. Gradle's cache is kept in
the project's `.gradle-user-home` directory.

### Optional copy to a test instance

Copy `build.local.properties.example` to `build.local.properties` in the project root
and set `NEWDAWN_MODS_DIR` to the existing absolute `mods` directory of your test instance.
Use forward slashes in this properties file, including on Windows, and no surrounding
quotes. Spaces in the path are supported. The local file is ignored by Git; keep
machine-specific values there rather than in committed VS Code settings.

You can alternatively set the `NEWDAWN_MODS_DIR` environment variable. It takes
precedence over the local file. A missing or blank value disables copying.
The existing VS Code build task and `gradlew build` both use this setting.

Only after the build checks succeed, the release JAR is staged and copied into the
configured directory, replacing the same filename. Other mods and older differently
named JARs are left untouched. An invalid destination or copy failure fails the build
with an explanation. Close the test client before replacing a JAR it has loaded;
restart it to use the new code. Building only `jar` does not install anything.

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
# Build with short contract, concurrency and allocation checks:
.\tools\verify.ps1 -JavaHome 'C:\Program Files\Java\jdk-21'

# Also generate, save and reload a server world, checking persisted chunk contents:
.\tools\verify.ps1 -Smoke -JavaHome 'C:\Program Files\Java\jdk-21'

# Optional larger geographic and biome-coverage surveys:
.\gradlew.bat :terrain-core:extendedCheck

# Standalone terrain benchmark:
.\gradlew.bat :terrain-core:benchmark

# Isolated component costs or sampled JVM call stacks:
.\gradlew.bat :terrain-core:componentBenchmark
.\gradlew.bat :terrain-core:profile

# Also measure CPU time and allocations in the core and Minecraft raw fill:
.\tools\verify.ps1 -Smoke -Benchmark -JavaHome 'C:\Program Files\Java\jdk-21'
```

`-Offline` uses cached build dependencies. The server check binds only to localhost on
an available port, creates a fresh world under `build/` and shuts itself down.
Reports and logs remain there. `-Smoke` includes the extensive core surveys. Add
`-Profile` for JFR recordings of core sampling and the first server run; profiling is
optional and changes timing, so use separate runs for benchmark comparisons. Development checks are excluded from the distributable JAR.

Further reading: [Architecture and limitations](docs/architecture.md),
[Performance and buffer API](docs/performance.md), [Verification evidence](docs/verification.md),
[Biome selection and water boundaries](docs/biomes.md), and
[Mountain shapes and cliffs](docs/mountains.md).
