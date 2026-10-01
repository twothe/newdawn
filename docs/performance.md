# Performance and reusable terrain buffers

## Results

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

```powershell
.\tools\verify.ps1 -Smoke -Benchmark -Offline -JavaHome 'C:\Program Files\Java\jdk-21'
```

Without `-Smoke`, `-Benchmark` measures only the core. Omitting `-Offline` allows missing
build dependencies to be downloaded. Normal builds already include the smaller allocation
regression check; timing measurements deliberately have no fixed pass/fail threshold.

Each core measurement uses 8,192 chunks or 2,097,152 individual queries. Checksums consume
the results; chunk outputs are additionally kept observable outside the loop.
Allocation counters come from the JVM's ThreadMXBean. JVMs without that capability
report negative byte values, or `SKIP` for the allocation check.

Each Minecraft measurement fills 256 fresh ProtoChunks. Construction and biome setup
happen before timing starts. The measurement covers synchronous raw fill, including
output buffers, palettes and heightmaps. It excludes caves, decoration, structures,
lighting, saving and networking. It therefore **does not imply twice the overall world
generation speed or client FPS**. JIT compilation, GC and machine load affect the results;
long-term profiling of a complete modpack remains outstanding.
