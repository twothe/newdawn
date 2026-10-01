# Verification evidence

## Upland biome boundary variation (2026-10-01)

Upland biome bands now use a bounded ±6-block offset derived from the existing
block-scale noise. Synthetic boundary checks cover peak, snowy-slope and highland
transitions as well as unchanged physical ocean/beach boundaries. The production
biome survey checks the offset bound and confirms all 51 non-river biomes remain
reachable. A separate 65,536-column sample measured offsets from −5.979 to +5.975
blocks. `:terrain-core:check build --offline` passed on Java 21, including current-run
determinism and 0 B/op allocation checks. Build installation was redirected to a
workspace test directory rather than the external modpack.

The dedicated server smoke check passed both fresh generation and reload on seed
`-8458999313514431577`. The persisted chunk checksum matched across runs;
all six heightmap types, 4,096 parallel clipped columns, exposed-rock surfaces
and 81 ocean chunks with zero water holes passed.


## Iterative terrain tuning (2026-10-01)

Fixed mountain snapshots and exact Forge 1.7.10 terrain comparisons were removed
from the build. The legacy CSV, its capture script and provenance file were also
removed. `:terrain-core:regressionTest` now compares 30,720 current-run samples
across serial, buffered and eight-worker paths, without stored heights or climate
bits. `:terrain-core:mountainTest` checks mathematical validity, climate response,
sampling-path agreement and mountain/cliff reachability without fixed geometry.
Biome behavior, allocation budgets and integration checks remain in place.

Java 21 `:terrain-core:check build --offline` passed after this change. The build
was redirected to a workspace test-mod directory, so the configured external
modpack installation was not altered. No generation code changed in this test
cleanup; the September 30 server smoke results below remain the latest server
runtime evidence.

## Mountain follow-up verification (2026-09-30)

The reported seed and coordinates reproduce the biome error through the production
sampler: X=-426/Z=2709, height 106, `mountain=false`, previously Frozen Peaks.
It now remains height 106 and selects Meadow. X=-703/Z=2887 increases from 118
to 162 and selects Jagged Peaks. A fixed 30,000-column local survey grows from 23
to 980 samples at height >=128. These are observations from the September 30 build,
not fixed acceptance values for future visual tuning.

`:terrain-core:check build` passed with Java 21, including all 51 non-river biomes
in 1,048,576 production samples, seeded eight-worker determinism and zero bytes
per reused sampling operation. Two 589,824-column
mountain surveys reach maximum heights 198 and 212 without clipping at 255.
The rare Cherry Grove temperature/humidity window and user-edited windswept humidity
thresholds are unchanged. No new noise calls or per-column fields were introduced.

Structure placement was inspected in the Minecraft 1.21.1 source and saved structure
starts near the reported mountain. Vanilla terrain adaptation is a density operation,
not an automatic foundation option for all features. The screenshot's floating garden
could not be identified conclusively; no speculative structure modification was made.
Visual assessment in the modpack remains a client check.

The dedicated server smoke workflow passed fresh generation and saved-world reload
for seed -8458999313514431577 (`build/smoke-054903bf0b5f46e5accf8f6f5bfa07e7`).
Both runs report checksum 7072917576458293388, matching terrain/layer queries,
4,096 parallel clipped columns, all six heightmap types, and zero ocean air holes
across 81 fully generated chunks. The checked release remains `newdawn-1.0.0.jar`.
Installation was redirected to the workspace test directory during verification;
the external modpack installation was not changed by these runs.

As of September 30, 2026. Windows 11, Oracle JDK 21.0.2, Gradle 9.2.1,
ModDevGradle 2.0.147, Minecraft 1.21.1, NeoForge 21.1.250.

## Optional test-instance installation

The build copy hook in `gradle/test-install.gradle` was exercised through real Gradle
builds using temporary directories under `build/`. Checks covered an unset target,
an explicitly blank environment override, environment precedence over the local file,
local-file fallback, replacement of an existing JAR, an invalid directory, and an
intentionally failed `check` task. Copied bytes matched the release JAR; unrelated
files and the installed JAR after a failed check remained unchanged. Staging files
were removed. The actual external test instance is configured locally; the copy
into that instance is performed by the user's next build.

## Current-generator consistency and concurrency

`terrain-core:regressionTest` samples 30,720 positions from six seeds in the current
implementation, including contiguous negative/positive coordinates and distant
locations. It checks serial results against eight concurrent workers, point and chunk
buffers, height-only queries, and biome selection. The serial results exist only in
memory during that test run; no fixed terrain samples or old Forge outputs are loaded.
Additional assertions cover chunk indexing, climate bands, shore/ocean precedence,
seed dependence and coordinate overflow. Buffer tests verify reuse, independent
storage, immutable snapshots, invalid indices and invalid inputs.

`terrain-core:biomeTest` separately protects modern selection: 51 non-river vanilla
overworld biomes, fixed climate/elevation/depth boundaries and reachability of all
51 biomes in 1,048,576 actual terrain columns across four seeds.
See [biomes.md](biomes.md) for details.

`terrain-core:allocationTest` is also part of `check`. After warmup, reusable APIs must
not allocate result objects regularly. Small measurement deviations up to 16 bytes per
chunk or 1 byte per individual query are tolerated; there is no machine-dependent time
limit. JVMs without thread allocation counters explicitly report `SKIP`; the test ran
and passed on the JDK used here.

## Mountain profile checks

`terrain-core:mountainTest` checks finite, bounded, monotone influence, finite
nonnegative profile relief, a climate-dependent cliff response and buffer propagation.
Two production surveys cover 589,824 columns each, checking height-only agreement
and mountain/cliff reachability. Heights, exact climate bits, fixed peak locations,
mountain area and precise cliff positions remain open to visual tuning. Climate affects
shape through direct arithmetic; no neighboring columns are sampled.

## Minecraft integration

`tools/verify.ps1 -Smoke` builds the mod JAR and starts two real dedicated development
servers in sequence: a fresh world, then a restart of that world. The initial
`server.properties` deliberately omits `level-type` to verify default selection.
Each test world has its own directory under `build/`. Servers bind only to localhost
and stop themselves after verification.

Checks include:

- An actual exposed land-cliff chunk: raw fill/base-column agreement, rock surface, and full generation.
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
current-run sampling consistency, concurrent biome queries and unchanged persisted
chunks remain required checks.

## Performance

Reproducible core and raw-fill benchmarks, allocation measurements, before/after values
and their limitations are documented in [performance.md](performance.md).
The four raw fills in the normal server test remain a functional check without stable
JIT warmup; their timings are not a reliable throughput comparison.

## Distribution and remaining limits

`verifyJar` opens the actual built JAR: terrain classes, presets, the menu tag and the
expanded mod version must be present; test hooks and development data must be absent.
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

## Mountain redesign integration result

`build/mountains-verification.log` records a successful build, core allocation/biome/
mountain/regression checks, and fresh plus reloaded dedicated servers using seed
`-8458999313514431577`. Both runs checked an exposed cliff in chunk [48, -192],
base-column/raw-fill consistency, normal decoration/structures and 81 ocean chunks
with zero unexpected air blocks. Persisted chunk checksums matched after restart.
The complete modpack and the appearance in the live client remain user validation steps.

## Broad mountain body and local detail result

The follow-up passed `:terrain-core:check build --offline --console=plain` and
`tools/verify.ps1 -Smoke -Benchmark -Offline` with Java 21 and seed
`-8458999313514431577`. The latter is recorded in `build/massif-verification.log`.

- The core checked substantial bodies between ridges, subordinate local detail,
  continuous profile boundaries, climate response, all 51 non-river biomes, 30,720
  current-run sampling points and eight-worker determinism. Reused paths allocated 0 B/op.
- Fresh and reloaded servers checked 1,024 full columns, 4,096 concurrent clipped
  columns, all six heightmap types and an exposed cliff in chunk [-178, -152].
- The 25 complete chunks contained 10,745 ore blocks, 1,400 log blocks and 4,102
  underground air blocks; a village was generated at X=-416, Z=768.
- Both passes found zero air holes in the water envelope of 81 ocean chunks. The
  persisted block checksum was 92492445344216611 before and after restart.

The fixture is `build/smoke-350e080b7a0b4bf0a7641fa4a1199b95`. These numbers record
this run, not fixed expectations for future shape tuning. Visual approval in the
client and complete modpack testing remain outstanding.
