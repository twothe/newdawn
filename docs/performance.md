# Performance and reusable terrain buffers

## Measurement workflow

Normal `build`/`check` runs short contracts and a compact allocation/timing check:
256 chunks or 65,536 queries per trial, including mountain-focused queries. It reports
one median per workload and fails on per-query allocation regressions, not on elapsed
time. Broad geographic surveys are explicit through `:terrain-core:extendedCheck`.

Detailed diagnostics are opt-in, entirely outside production sources:

```powershell
# Full warmed CPU/allocation workloads, including expensive mountain regions:
.\gradlew.bat :terrain-core:benchmark

# Actual component methods with varying precomputed inputs:
.\gradlew.bat :terrain-core:componentBenchmark

# Sampled call stacks, allocation sites and GC; creates terrain-core/build/terrain-profile.jfr:
.\gradlew.bat :terrain-core:profile

# Full core and game benchmarks, fresh generation and restart:
.\tools\verify.ps1 -Smoke -Benchmark -Offline -JavaHome 'C:\Program Files\Java\jdk-21'

# Optional core and first-server JFR capture (server output: build/server-generation.jfr):
.\tools\verify.ps1 -Smoke -Profile -Offline -JavaHome 'C:\Program Files\Java\jdk-21'

# JDK 21 can summarize the recording without installing a profiler UI:
jfr view hot-methods terrain-core/build/terrain-profile.jfr
jfr view allocation-by-site terrain-core/build/terrain-profile.jfr
jfr view cpu-load terrain-core/build/terrain-profile.jfr
```

Component measurements isolate Simplex sampling, four-field broad climate, weathering,
mountain distribution/body, cliff activation/profile and biome selection. The server
benchmark separately times biome filling and raw block filling. These isolated costs
include harness overhead and are **not additive percentages** of full generation:
activation rates, caches and JIT inlining differ. Use JFR execution stacks to locate
cost in the complete workload; use its allocation and GC views to distinguish memory
pressure from computation. JFR is sampled evidence, not an exact timer for each call.
Its recording includes startup and warmup, so inspect the steady-state interval when
attributing workload costs. Repeat with the real modpack for end-to-end conclusions.

Profiling itself affects execution. Keep instrumented runs separate from before/after
CPU comparisons. No production class imports a measurement API or checks a profiling
flag. The JVM recording is enabled only on the requested development launch.
Recordings stay in ignored build directories and are replaced by the next matching run.

## Climate rebalancing measurements (2026-10-01)

The existing warmed benchmarks ran before and after the climate/palette change,
with Java 21, seed 123456789, the same coordinates and three-trial medians. No new
noise fields, queries or output-buffer fields were added. Cooling remains a table
lookup; the forest overlay retains its positive increment and trigger.

| Reused workload | Before | After | Allocation |
| --- | ---: | ---: | ---: |
| Chunk buffer | 70.231 us/chunk | 66.748 us/chunk | 0 B/op |
| Mixed height | 0.173 us/query | 0.149 us/query | 0 B/op |
| Mixed column | 0.314 us/query | 0.275 us/query | 0 B/op |
| Column plus biome | 0.289 us/query | 0.285 us/query | 0 B/op |
| Mountain height | 0.361 us/query | 0.365 us/query | 0 B/op |
| Mountain column | 0.469 us/query | 0.451 us/query | 0 B/op |

No material CPU regression appeared. Several unchanged or lightly changed paths
also appear faster, and the before trials varied substantially; these timings do
not establish a precise speedup. Height checksums match before/after, consistent
with retaining geometry inputs. The column/biome checksums deliberately change.
The normal allocation check independently passed for every reused workload.

The old core was preserved under `build/climate-before/source` and compiled into
separate build output for baseline server measurements without replacing production
sources. The same reported world seed `-8458999313514431577` was used for fresh and
reloaded server checks before and after:

| Server stage median | Before | After |
| --- | ---: | ---: |
| Fresh biome fill | 0.044 ms/chunk | 0.044 ms/chunk |
| Fresh raw fill | 0.243 ms/chunk | 0.242 ms/chunk |
| Reloaded biome fill | 0.045 ms/chunk | 0.044 ms/chunk |
| Reloaded raw fill | 0.315 ms/chunk | 0.232 ms/chunk |

The baseline reloaded raw-fill trials span 0.229–0.324 ms/chunk; final trials span
0.231–0.308. Their spread prevents interpreting the median difference as a stable
speedup. Fresh raw-fill allocation is approximately 15.4 KB/chunk on both revisions;
reloaded output is approximately 15.5 KB/chunk. These allocations include Minecraft
output data, unlike the allocation-free reusable core. End-to-end modpack throughput
and decoration cost remain outside these measurements.

Evidence: `build/climate-before/benchmark.log`, `build/climate-before/server-fresh.log`,
`build/climate-before/server-reload.log`, and `build/climate-final-verification.log`.
The comparison source/classes and temporary Gradle initialization file are ignored
local measurement artifacts, not permanent compatibility fixtures. Climate and
woodland-access outcomes are in [climate-distribution.md](climate-distribution.md).

## Component refactor measurements (2026-10-01)

The final comparison ran the **same saved benchmark class** sequentially against the
pre-refactor and final core on Java 21, with the same seeds, coordinates, warmup and
trial counts. The original sampling APIs remain available. Three-trial medians:

| Operation | Before | After | Reused output allocation |
| --- | ---: | ---: | ---: |
| Reused chunk buffer | 70.189 µs/chunk | 66.655 µs/chunk | 0 B/op |
| Height only, mixed terrain | 0.157 µs/query | 0.149 µs/query | 0 B/op |
| Full column, mixed terrain | 0.286 µs/query | 0.274 µs/query | 0 B/op |
| Full column plus biome | 0.295 µs/query | 0.311 µs/query | 0 B/op |
| Height only, strong mountains | 0.385 µs/query | 0.397 µs/query | 0 B/op |
| Full column, strong mountains | 0.491 µs/query | 0.452 µs/query | 0 B/op |

An earlier version of the split showed increased sampling cost. A separate JFR run
attributed most sampled execution to Simplex and field-coordinate scaling; it also
reported 71–83% machine CPU load. Preparing reciprocal noise scales once removed two
divisions per field query. The shared climate component avoids calculating dryness
and frost twice when mountains and cliffs are active. Both remain ordinary editable
rules, without measurement branches or special generated code in production.

Timing is noisy on this host. The full verification's final core run measured
66.011 µs/chunk, 0.149 µs/mixed height, 0.270 µs/full column,
0.361 µs/mountain height and 0.451 µs/mountain column. The paired biome and mountain-height
results above are slightly slower, while the latter is faster in the other final run;
these measurements do not justify claiming every path improved or a precise global
speedup. All reusable API allocation budgets passed at 0 B/op.

Uninstrumented raw-fill medians were 0.239/0.272 ms per chunk before and
0.253/0.281 ms after (fresh/reloaded server); output allocation remained approximately
15.4/15.5 KB per chunk. This workload does not measure the lazy aquifer improvement:
the aquifer contract check proves zero eager sampling and one height evaluation per
actually queried column. Biome fill is now measured separately (0.042/0.048 ms per
chunk in the final runs). No end-to-end modpack speedup is inferred from these stages.

Local evidence: `build/architecture-final-paired-before.log`,
`build/architecture-final-paired-after.log`, `build/architecture-final.log`,
`build/architecture-profile.log` and `build/architecture-server-profile.log`.
The pre-refactor comparison classes and temporary geometry checks are local ignored
artifacts, not permanent compatibility fixtures or build gates.

## Initial buffer optimization results

Measured on September 30, 2026 on the local Windows machine with Oracle JDK 21.0.2
and NeoForge 21.1.250. Values are medians of three measurements following three warmup
passes. Byte counts measure newly allocated heap memory per operation, not retained
memory or maximum heap size.

| Operation | Before | After | Allocations before → after |
| --- | ---: | ---: | ---: |
| Full chunk sampling, independently owned result per call | 72.3 µs | 66.9 µs | 11,280 → 5,552 B/chunk |
| Full chunk sampling, reused buffer | – | 68.6 µs | 0 B/chunk after buffer construction |
| Height-only query | 0.292 µs | 0.128 µs | 40 → 0 B/query |
| Minecraft raw fill | 0.660 ms | 0.329 ms | 33,058 → 13,905 B/chunk |

In this comparison, raw fill takes approximately **50% less time** and allocates
**58% fewer bytes**. Full terrain-core sampling mainly benefits from reduced allocations;
its CPU timings remain in a similar range. Reuse does not guarantee faster execution than
a newly allocated buffer: both variants fall within the observed timing variation.

Local baseline results are in `build/performance-baseline.log`; post-change core results
are in `build/performance-core.log`, and post-change raw-fill results are in
`build/performance-bulk.log`. The final combined verification, including repeat benchmarks,
is in `build/performance-verification.log`.
The two final server runs measured 0.220–0.258 ms and roughly 13.7–13.9 KB per raw fill.
Differences in JVM warmup and load contribute to those lower values; the table uses
the more conservative separate comparison run.

## Changes

- Six primitive arrays replace 256 result objects and their reference array. In the
  measured JVM layout, a new `TerrainChunk` requires 5,552 bytes including its arrays
  and internal column buffer. Reusing it allocates nothing per query.
- `sampleHeight` computes only the relief. Structure height queries avoid all climate
  and filler noises, as well as scanning down the block column.
- Altitude-dependent climate correction is precomputed once for heights 1–255.
  The formula, rounding, noise order and measured float bits are preserved.
- BiomeSource cache misses use a small column buffer per worker.
- Material intervals are determined once per column. Entirely uniform stone/deepslate
  sections receive a compact palette directly instead of 4,096 individual block writes.
  Biome containers are preserved; Minecraft computes section counts. Transition layers
  are written block by block.
- The existing `sample`/`sampleChunk` methods remain available for convenient immutable
  results. Minecraft generation uses the new APIs.

## Biome expansion

After expanding selection to 51 biomes, full sampling with depth-dependent biome
selection still allocates **0 B/query**, taking approximately **0.284–0.295 µs/query**
in the latest run. Server runs on the water-hole seed reached median raw-fill timings
of **0.243 and 0.246 ms/chunk**, allocating roughly **13.8–13.9 KB/chunk**.
The different seed prevents a direct timing comparison with the table above.
The carver fix additionally requires an invocation-owned buffer of 256 integer heights
and 256 height-only queries per carving pass; raw-fill measurements do not include
that separate pipeline stage.

## Mountain redesign

The multifractal/cliff profile keeps full-column noise-query counts unchanged. Height-only
queries evaluate four additional broad-climate fields inside active mountain regions.
No surrounding heights or chunks are queried. The extra rock flag raises a newly allocated
chunk buffer from 5,552 to 5,832 bytes; reused chunk, column, height and biome paths still
measured 0 B/op.

The local variation of upland biome boundaries also reuses an existing sampled noise
value, adding no noise evaluation. It adds one float to a retained sample and one
256-element float array to an owned chunk buffer. Reused sampling still measured
0 B/op after this change; no additional result object is created per column.

The initial baseline and subsequent measurements differed noticeably in machine speed.
To avoid comparing those runs directly, the previous committed core was compiled into an
isolated `build/mountain-baseline` directory and benchmarked immediately before the new
core, using the same JDK, benchmark and seed. Three-trial medians after warmup:

| Operation | Previous core | Mountain core |
| --- | ---: | ---: |
| Owned chunk buffer | 113.339 µs | 113.342 µs |
| Reused chunk buffer | 106.101 µs | 113.302 µs |
| Height-only query | 0.202 µs | 0.234 µs |
| Reused full column | 0.467 µs | 0.455 µs |
| Reused column plus biome | 0.457 µs | 0.482 µs |

The observed reused-chunk overhead is about 7%, and height-only overhead about 16%, for
this sampled region. Individual trials still varied; the small full-column improvement
is not evidence of a speedup. Mountain-heavy regions can have different costs. Logs:
`build/mountains-baseline-controlled.log` and `build/mountains-current-controlled.log`.

The fresh/reloaded server runs measured median raw-fill times of 0.440/0.424 ms per chunk,
with approximately 14.1 KB allocated per chunk. These are integration measurements, not
a matched old/new server comparison or a whole-world-generation throughput claim.
Evidence: `build/mountains-verification.log`.

## Broad mountain body and local detail follow-up

The body/ridge redesign reuses every existing noise sample. It adds scalar arithmetic,
without new fields, neighboring queries or output-buffer allocations. The same warmed
benchmark immediately before and after this change produced these three-trial medians:

| Operation | Previous ridge-dominated profile | Broad body with local detail |
| --- | ---: | ---: |
| Reused chunk buffer | 67.829 µs | 68.061 µs |
| Height-only query | 0.147 µs | 0.147 µs |
| Reused full column | 0.278 µs | 0.276 µs |
| Reused column plus biome | 0.287 µs | 0.285 µs |

All four reused paths measured 0 B/op. These small timing differences do not establish
a meaningful speed change. The sampled region is not a worst-case mountain benchmark.
Fresh/reloaded server raw-fill medians were 0.245/0.223 ms per chunk with approximately
15.4/15.5 KB allocated, including Minecraft chunk output data. These are stage timings,
not whole-world throughput or an old/new server comparison.
Current-run evidence: `build/massif-verification.log`.

## Independent cliff noise: before/after comparison

The same benchmark (including the new mountain reservoir) ran before and after the
cliff change, on Java 21 with the same seeds and coordinates. The initial full core
measurements produced these three-trial medians:

| Operation | Before | After |
| --- | ---: | ---: |
| Reused mixed-terrain chunk | 72.663 µs | 70.047 µs |
| Mixed height query | 0.161 µs | 0.159 µs |
| Mixed full-column query | 0.299 µs | 0.290 µs |
| Strong-mountain height query | 0.358 µs | 0.372 µs |
| Strong-mountain full-column query | 0.444 µs | 0.478 µs |

All reused paths measured 0 B/op. The focused mountain trials suggest approximately
4–8% additional query cost; mixed-terrain differences are within observed variability.
Cliff noise is skipped outside eligible mountain/climate patches, and no buffers grew.

Initial fresh/reloaded server raw-fill medians changed from 0.247/0.222 to 0.316/0.311
ms per chunk. A repeated current-code run returned to 0.244 ms (trials 0.297, 0.244,
0.237), so the large slowdown was not stable. Raw-fill allocations remained about
15.4–15.5 KB per chunk, including Minecraft output data.

An additional sequential old/new core control also slowed down substantially even on
the unchanged baseline (mountain height median 0.600 µs), making cross-process timing
ratios unreliable in that session. A diagnostic alternating both compiled revisions
within one JVM used identical method-handle wrappers and baseline-selected highland
coordinates. Full-column relative differences ranged from -4.88% to +7.53%; height
trials remained noisy, including an 81.60% outlier. These controls do not justify a
precise universal overhead or a whole-world throughput claim. No per-call allocation
regression was found; controlled profiling on a quiet machine remains useful for
more precise CPU attribution.

Evidence: `build/cliffs-before.log`, `build/cliffs-after.log`,
`build/cliffs-before-control.log`, `build/cliffs-after-control.log`,
`build/cliffs-server-control.log` and `build/cliffs-paired.log`.

## Cliff transition follow-up

Widening the steep core, adding short head/foot aprons and easing fading ends adds
only scalar arithmetic. No noise fields, queries, neighborhood probes or buffers were
added. The same full benchmark ran before and after the profile edit on Java 21:

| Operation | Before | After |
| --- | ---: | ---: |
| Reused mixed-terrain chunk | 69.466 µs | 69.406 µs |
| Mixed height query | 0.154 µs | 0.155 µs |
| Mixed full-column query | 0.288 µs | 0.283 µs |
| Strong-mountain height query | 0.374 µs | 0.378 µs |
| Strong-mountain full-column query | 0.476 µs | 0.471 µs |

These three-trial median differences are small relative to measurement variability;
they do not establish a meaningful speed change. All reused paths measured 0 B/op.
Logs: `build/cliff-transition-before.log` and `build/cliff-transition-after.log`.
The subsequent fresh/reloaded raw-fill medians were 0.237/0.225 ms per chunk, with
approximately 15.4/15.5 KB of Minecraft output allocation. These are integration
stage measurements rather than whole-world throughput.

## API and ownership

```java
TerrainSampler sampler = new TerrainSampler(seed); // Share freely between workers.
TerrainColumn column = new TerrainColumn();       // Exclusive to this operation/worker.
sampler.sampleInto(blockX, blockZ, column);
BiomePalette.Entry biome = BiomePalette.select(column);
TerrainSample retained = column.snapshot();       // Allocates only when a snapshot is needed.

TerrainChunk chunk = new TerrainChunk();
sampler.sampleChunkInto(chunkX, chunkZ, chunk);
int height = chunk.height(localX + 16 * localZ);
chunk.copyColumn(localX + 16 * localZ, column);      // Independent reusable destination.
int heightOnly = sampler.sampleHeight(blockX, blockZ);
```

Each sampling call overwrites the entire output buffer. Other calls or threads must not
use it while it is being written. Once safely published, a completed buffer may be read
by multiple threads as long as nobody writes to it again. Uninitialized buffers and
invalid indices are rejected. Chunk coordinates whose block origin falls outside the
`int` range are rejected before any mutation.

The Minecraft integration allocates its chunk buffer per generation invocation.
Concurrent and nested calls therefore work without a pool, global lock or overwritten
shared workspace. The terrain core remains independent of Minecraft and NeoForge.

## Reproducing measurements and their limits

For every generation change, record a benchmark before editing the algorithm and
repeat it afterwards with the same Java version, seed, coordinates and JVM settings.
Keep CPU-heavy tools and other generation runs out of the measurement window.
Investigate material slowdowns rather than hiding them behind a whole-region average.
Fixed wall-clock limits are not build gates because machine load and JIT behavior vary.

```powershell
.\tools\verify.ps1 -Smoke -Benchmark -Offline -JavaHome 'C:\Program Files\Java\jdk-21'
```

Without `-Smoke`, `-Benchmark` measures only core workloads and components. Omitting `-Offline` allows missing
build dependencies to be downloaded. Normal builds already include the smaller allocation
regression check; timing measurements deliberately have no fixed pass/fail threshold.

Each core measurement uses 8,192 chunks or 2,097,152 individual queries. Checksums consume
the results; chunk outputs are additionally kept observable outside the loop.
Allocation counters come from the JVM's ThreadMXBean. JVMs without that capability
report negative byte values, or `SKIP` for the allocation check.

The core benchmark additionally selects 4,096 strong mountain coordinates from a
deterministic reservoir over X/Z=[-3072,3072), on an eight-block grid. Selection uses
only distribution influence (at least 0.35), outside timing. The same points therefore
exercise height-only and full-column mountain paths before and after profile changes.
The allocation check includes both paths. A distribution change requires reviewing
coordinate comparability; profile and cliff changes preserve this workload.

Each Minecraft measurement fills 256 fresh ProtoChunks. Construction stays outside both timers. Biome setup is measured separately before
the raw-fill timer starts. The measurement covers synchronous raw fill, including
output buffers, palettes and heightmaps. It excludes caves, decoration, structures,
lighting, saving and networking. It therefore **does not imply twice the overall world
generation speed or client FPS**. JIT compilation, GC and machine load affect the results;
long-term profiling of a complete modpack remains outstanding.

## Open-biome tree placement (2026-10-01)

The production change adds no work to terrain or biome sampling. Its forest-patch query
is called only for tree candidates in tagged open biomes, after original placement
filters. Most positions exit after one forest noise query; the ambiguous climate-dependent
threshold evaluates the existing temperature and height paths. The opt-in component
benchmark measured a median 0.025 microseconds/query and 0 B/op on its mixed input grid.
This isolates the pointwise mask, not the complete Minecraft placement pipeline.

Initial Gradle benchmark timings fluctuated, including a roughly 11% chunk-sampling
increase while column and mountain costs decreased. A consecutive direct-Java recheck
compiled the saved pre-change core source separately and used identical Java 21 defaults,
benchmark inputs and warmups. All terrain/biome checksums agreed:

| Reused path | Before | After | Allocation |
| --- | ---: | ---: | ---: |
| Chunk buffer | 68.374 us/chunk | 66.654 us/chunk | 0 B/op |
| Mixed height | 0.150 us/query | 0.151 us/query | 0 B/op |
| Mixed column | 0.282 us/query | 0.292 us/query | 0 B/op |
| Column plus biome | 0.286 us/query | 0.282 us/query | 0 B/op |
| Mountain height | 0.369 us/query | 0.367 us/query | 0 B/op |
| Mountain column | 0.448 us/query | 0.450 us/query | 0 B/op |

The isolated recheck does not reproduce a material terrain regression. Small differences
are not evidence of a speedup. Sources/results are under `build/decoration-before`,
`build/decoration-after-recheck.log`, and `build/decoration-verify.log`.

The integration comparison used seed -8458999313514431577 and a private datapack that
disables only the tree modifier for the baseline. Fresh-server raw-fill medians were
0.451 ms/chunk without filtering and 0.419 with filtering; allocation was approximately
15.3 KB/chunk in both cases. Biome-fill medians were 0.081 and 0.080 ms/chunk.
Raw-fill checksums agree. These timers exclude decoration and cannot quantify reduced
tree-generation cost or imply a total-world-generation speedup. Fresh and reload checks
also passed with filtering; reload raw-fill median was 0.442 ms/chunk. Baseline evidence
is `build/decoration-baseline-server.log`. The modifier-instance wrapper cache is used
only during registry construction; no cache lock or instrumentation runs during placement.

## Savanna vegetation bands (2026-10-02)

Replacing the Sparse Jungle surface transition with Savanna retains physical relief and
climate noise. A humidity-only query supplies the dry/wooded placement decision, using
the same forest increment and altitude-dependent trigger as full sampling. On the existing
mixed component workload it measured 0.087 us/query and 0 B/op. This is more work than
a forest-only predicate, but is evaluated only for candidate trees in Savanna-family biomes;
normal column generation receives no additional noise queries or result allocations.

Warmed Java 21 Gradle benchmarks before/after, with identical seeds and coordinates:

| Reused path | Before | After | Allocation |
| --- | ---: | ---: | ---: |
| Chunk buffer | 65.725 us/chunk | 63.628 us/chunk | 0 B/op |
| Mixed height | 0.146 us/query | 0.143 us/query | 0 B/op |
| Mixed column | 0.279 us/query | 0.267 us/query | 0 B/op |
| Column plus biome | 0.277 us/query | 0.278 us/query | 0 B/op |
| Mountain height | 0.361 us/query | 0.355 us/query | 0 B/op |
| Mountain column | 0.437 us/query | 0.429 us/query | 0 B/op |

No material sampling regression was observed. Height/column checksums agree; biome
checksums intentionally change. Evidence is in `build/savanna-before/core.log` and
`build/savanna-verify.log`. Both fresh and reloaded server checks passed, including native
trees in moderately humid Savanna outside forest patches and a treeless dry Savanna chunk.
Fresh/reload raw-fill medians were both 0.228 ms/chunk, with approximately 15.4/15.5 KB
allocated per chunk. A fresh private-world comparison with only the decoration filter
disabled is recorded in `build/savanna-baseline-server.log`; raw-fill checksums agree.
Those raw-fill timers exclude decoration and do not measure total tree-generation cost.
