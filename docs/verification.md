# Verification evidence

As of September 30, 2026. Windows 11, Oracle JDK 21.0.2, Gradle 9.2.1,
ModDevGradle 2.0.147, Minecraft 1.21.1, NeoForge 21.1.250.

## Legacy fidelity and concurrency

`terrain-core:regressionTest` checks 30,720 stored reference points captured by
`tools/capture-legacy-fixtures.py` from the old checkout. The original
`generateChunkInformation` method, noise initialization, Simplex implementation and
selectors are compiled separately. Only Minecraft biome/block objects unrelated to
the calculation are replaced. The new terrain code is not loaded during capture.

Source hashes are recorded in `terrain-core/src/test/resources/legacy-terrain.provenance.txt`.
The golden file contains six seeds: 0, 1, −1, 123456789, `Long.MIN_VALUE` and `Long.MAX_VALUE`.
It covers contiguous chunks around the origin, negative coordinates, randomly sampled
remote areas and positions near the world border.

Assertions cover height, regional height, mountain flags, exact temperature/humidity bits
and filler depth. Since biome selection was expanded, historical biome names in the CSV
are no longer expected modern selections; the golden file itself remains unchanged.
The same terrain/climate data are checked with eight threads sharing samplers.
Additional assertions cover chunk indexing, climate thresholds, shore/ocean precedence,
seed dependence and coordinate overflow. Reference points are also checked through
single-column buffers, chunk buffers and height-only queries. Buffer tests verify reuse,
independent storage, immutable snapshots, invalid indices and invalid inputs.

`terrain-core:biomeTest` separately protects modern selection: 51 non-river vanilla
overworld biomes, fixed climate/elevation/depth boundaries and reachability of all
51 biomes in 1,048,576 actual terrain columns across four seeds.
See [biomes.md](biomes.md) for details.

`terrain-core:allocationTest` is also part of `check`. After warmup, reusable APIs must
not allocate result objects regularly. Small measurement deviations up to 16 bytes per
chunk or 1 byte per individual query are tolerated; there is no machine-dependent time
limit. JVMs without thread allocation counters explicitly report `SKIP`; the test ran
and passed on the JDK used here.

To regenerate reference data deliberately, as a separate operation:

```powershell
python tools/capture-legacy-fixtures.py C:/Work/Java/Projekte/mc-forge-1710-newdawn --jdk 'C:/Program Files/Java/jdk-21'
```

Do not regenerate these data during normal regression tests: updated expectations could
hide a defect. Changes to the original sources require review.

## Minecraft integration

`tools/verify.ps1 -Smoke` builds the mod JAR and starts two real dedicated development
servers in sequence: a fresh world, then a restart of that world. The initial
`server.properties` deliberately omits `level-type` to verify default selection.
Each test world has its own directory under `build/`. Servers bind only to localhost
and stop themselves after verification.

Checks include:

- Registry/preset loading, the correct generator and world-seed binding.
- New Dawn through Minecraft's default-preset path and server configuration without an
  explicit world type; `newdawn:new_dawn` remains valid. The menu tag contains both
  the default and Vanilla choices.
- Explicit `level-type=newdawn:vanilla` resolution through Minecraft's actual configuration
  code to a vanilla noise generator with vanilla noise settings and biome preset.
- A generator codec roundtrip through Minecraft registries and JSON.
- Rejection of a subsequent attempt to bind a different seed.
- Stable biome encounter order for Minecraft's derived feature seeds.
- 12,288 concurrent biome queries, including cache collisions and vertical reuse of
  the same horizontal position at six heights.
- 1,024 columns in four fresh ProtoChunks: all 384 block positions compared with
  `getBaseColumn`, solid/water heights compared with heightmaps and structure height queries.
- Another 4,096 columns across four concurrent workers and different height slices:
  all six height-query types compared with Minecraft's own heightmap recalculation,
  column/fill consistency, preserved biome containers and section block/tick counts.
- Explicit expected material intervals for bedrock, deepslate, stone, sandstone, sand,
  grass, submerged ground and water, including negative Y values and zero filler depth.
- 25 fully generated chunks: ore blocks and underground air from carvers are required;
  tree blocks are counted as well.
- Locating and generating a village with a valid StructureStart.
- Comparing a block-state checksum after saving and restarting.
- The aquifer boundary contract and, on the reported seed, 81 fully generated chunks in
  the water-hole reproduction area: 0 air blocks inside the original water envelope,
  including after restart, compared with 402 before the fix.

The combined performance/build/server verification is recorded locally in
`build/performance-verification.log`. Each test world also contains
`newdawn-smoke-result.txt` and `newdawn-smoke-checksum.txt`.
The subsequent default-selection check with mod version 1.0.0 is in
`build/default-preset-verification.log`. A graphical menu test was not performed;
the actual default-preset path, selection registration and selectable generator data
were checked.
The full biome-expansion and water-fix verification, including benchmarks, is in
`build/biomes-final-verification.log`. The earlier generation-stage diagnosis on the
reported seed is in `build/water-user-seed-before.log`. The original modpack world was
opened only to read its seed and was not modified.
Biome initialization and sorting are tested explicitly because height comparisons alone
would not detect changes to decoration seeds.

An additional diagnostic comparison of independently created worlds showed small
differences in complete Minecraft decoration. Exact agreement of those details was
explicitly excluded from the requirements. That stricter comparison is therefore not
an acceptance criterion or part of the permanent verification command. Exact core
fixtures, concurrent biome queries and unchanged persisted chunks remain required checks.

## Performance

Reproducible core and raw-fill benchmarks, allocation measurements, before/after values
and their limitations are documented in [performance.md](performance.md).
The four raw fills in the normal server test remain a functional check without stable
JIT warmup; their timings are not a reliable throughput comparison.

## Distribution and remaining limits

`verifyJar` opens the actual built JAR: terrain classes, presets, the menu tag and the
expanded mod version must be present; test hooks and original fixtures must be absent.
The task is part of `check` and `build`.

Not performed: graphical client/menu tests, multiplayer connections, a modpack
compatibility matrix, checks of every structure type, long-term memory profiling, or
builds against other Minecraft versions. Server checks load development artifacts;
the packaged JAR is checked for contents but is not additionally launched in a separate
production installation.

Development logs include the usual vanilla command-ambiguity messages and
environment-specific OSHI/WMI query warnings. These did not prevent startup or generation.
Detailed limits concerning caves, surface rules, structure terrain adjustment and biome
mods are documented in [architecture.md](architecture.md).
