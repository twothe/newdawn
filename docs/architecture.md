# Architecture and terrain contract

## Components

`terrain-core` is a Java 21 module with no external dependencies. The components
are ordinary immutable objects and pure functions, with direct calls rather than a
configurable pipeline, runtime plugin registry or per-column context allocation.

| Component | Responsibility / tuning location |
| --- | --- |
| `TerrainSampler` | Public sampling API and composition of relief, final climate and filler |
| `TerrainNoise` | Seed-specific noise fields, spatial scales and initialization order |
| `ClimateRules` | Broad climate, final altitude correction, dryness and frost weathering |
| `TerrainRelief` | Base height amplitudes, domain warping, mountain/cliff composition and local biome-height variation |
| `MountainProfile` | Mountain distribution threshold, body/ridge weights, amplitude and detail |
| `CliffProfile` | Climate activation, face height/width, aprons and fading ends |
| `NoiseMath` | Shared bounded interpolation |
| `BiomePalette` | Climate/elevation choices, surface materials and shared vertical precedence |

`TerrainColumn` and `TerrainChunk` are reusable, caller-owned output buffers;
`TerrainSample` is an immutable convenience snapshot. No component allocates a
result object per column on the reusable path. The module compiles without Minecraft
or NeoForge classes.

### Changing or extending generation

Change each rule in its owning component. Principal visual controls are named constants
next to the formula; small climate decision trees remain readable comparisons. There is
no universal parameter bag or inheritance hierarchy to update before adding a rule.

For a new overlay, append its field in `TerrainNoise`, compose its pointwise contribution
in `TerrainRelief`, and give a substantial independent profile its own cohesive component.
Reuse existing noise values where appropriate. Cheap activation should precede expensive
noise queries. A rule affecting geometry must participate in both full-column and
height-only sampling; both already go through the same relief composition. Extend output
buffers only when a downstream consumer needs the additional value. Shared inputs such
as dryness and frost are calculated once and passed as primitives.

Broad climate is independent of height. Final biome climate is applied after height has
been computed. Height-only queries request broad climate lazily inside mountain regions;
that decision is explicit and independent of whether an output buffer was supplied.

Keep the surface and its top material on the same column selection. Cached game biome
holders and direct core queries both use `BiomePalette.selectVertical`, so changing
underground precedence does not require a second implementation in the integration layer.

Visual acceptance belongs in the Minecraft client with the intended modpack. Core tests
and benchmarks check contracts and cost; they cannot judge the overall landscape or
third-party decoration. No standalone terrain preview tool is part of this workflow.

### Source boundaries

- `terrain-core/src/main`: production sampling and rules.
- `terrain-core/src/test`: short contract tests and optional extensive surveys.
- `terrain-core/src/benchmark`: allocation checks, detailed benchmarks and profiling workloads.
- `src/main`: Minecraft/NeoForge production integration and resources.
- `src/integrationTest`: the isolated server test entry point, integration checks and game benchmarks.

The development `smokeServer` run combines main and integration-test sources as one mod;
normal client/server runs load only main. The release JAR embeds only main plus the
core main output. Measurements and test event subscribers never enter the release.

`NewDawnBiomeSource` resolves selections to registry-owned vanilla biome holders.
`NewDawnChunkGenerator` extends the existing `NoiseBasedChunkGenerator` pipeline and
replaces biome filling, raw terrain, surface generation and height queries. Inheritance
also serves a functional purpose: Minecraft creates a noise generator's `RandomState`
from its associated noise settings. Carvers, structures, features and mob generation
remain in the existing pipeline. The carver integration additionally supplies a water
boundary based on our terrain; see [biomes.md](biomes.md).

Only `NewDawn` registers codecs through NeoForge. There are no mixins, reflection,
access transformers, global world cache or hierarchy of version adapters.

## Default world type and vanilla selection

Minecraft 1.21.1 uses `minecraft:normal` when opening the new-world screen. The same key
is the default `level-type` on dedicated servers. The file
`data/minecraft/worldgen/world_preset/normal.json` therefore assigns the New Dawn
generator to that preset. `newdawn:new_dawn` remains available as an explicit,
backward-compatible key.

The unchanged vanilla preset is also available as `newdawn:vanilla` and is added to the
normal selection tag. The default is labeled "New Dawn" in the menu; the alternative
is labeled "Vanilla". The original New Dawn key is not listed separately, avoiding a
duplicate menu entry. Other vanilla world types remain available.

This changes presets for new worlds. Saved dimensions keep their existing generator.
Both presets use vanilla generation for the Nether and End. Datapacks or mods that
also replace `minecraft:normal` compete for the same entry; datapack priority determines
which one wins. No global generator is replaced at runtime, and no client API is needed.

## Preserved terrain and redesigned mountains

- `java.util.Random(seed)` initializes 1,024 random bytes for the Simplex field as before.
  This historical field is **not** a standard 256-entry permutation. Replacing it with
  another Simplex library would produce different worlds.
- All 13 general noise fields, followed by four mountain fields, are initialized in the
  original order. Even the filler field affects later climate and mountain fields by
  consuming random numbers during initialization. The dedicated cliff field follows
  these original fields, retaining all existing offsets.
- Base-terrain scales, offsets and weights retain their original meaning. Noise scales
  are converted once to reciprocals, replacing two divisions per field query with
  multiplication. Floating-point rounding may therefore differ slightly. Rounding,
  height clamping and float conversion are also retained. `BLOCK_SCALE=2` remains
  part of the terrain shape and is deliberately independent of the modern build height.
- Ground level remains 64: Y=63 is the top water block; a terrain height of 64 means
  the first free block above solid ground at Y=63.
- Temperature and humidity fields, including forest patches and altitude correction through height 128,
  are preserved. Above 128 the correction continues linearly to suit taller mountains. Biome selection now covers all 51 non-river overworld biomes; climate,
  elevation and depth rules are documented in [biomes.md](biomes.md).
- Mountain distribution uses the existing field and threshold, with the user-tuned
  1.8 scale multiplier. Its height profile combines a broad body, subordinate coupled
  ridges, local noise overlays and direct climate-dependent
  cliff shaping; see [mountains.md](mountains.md). River generation is still absent.
- Independent cliff noise is sampled only where mountain strength and the direct
  climate/patch mask permit it. Its bounded deformation preserves the broad mountain
  body; no neighboring heights or environment calculations are needed.
- Upland biome and top-material bands reuse the already sampled 23 × 27-block terrain
  noise for a bounded ±6-block threshold offset. Physical terrain height, sea-level
  boundaries and heightmaps are unaffected.

The original project is neither modified nor needed for builds. Tests exercise the
current generator's seed handling, concurrent sampling and buffer agreement without
requiring any sampled terrain to match a historical version. The historical noise
description above documents the present implementation, not a permanent shape contract.

## Seed ownership and concurrency

Minecraft calls `createState` with the world seed when setting up the chunk pipeline.
The BiomeSource is initialized once there; its immutable sampler is safely published
through a `volatile` field. Subsequent initialization with a different seed fails explicitly.
On restart, Minecraft decodes the generator and binds the saved world seed again.
The preset does not store a hard-coded seed.

Core sampling requires neither locks nor RNG calls. `sampleInto` and `sampleChunkInto`
write into exclusive caller-owned buffers without allocations. `sampleHeight` skips
local climate and filler calculations; inside mountain regions it evaluates the four
broad climate fields needed by the pointwise mountain profile. Full sampling reuses
those climate values without additional noise queries. The previous snapshot methods
remain available.
Altitude-dependent climate corrections are precomputed once for all 255 possible heights
using the original formula through 128 and a linear continuation above it. The BiomeSource maintains a cache of at most 256 entries per
worker, using full coordinate keys; collisions cannot change results. This avoids repeated
climate calculations for vertical biome quarts and samples only the requested column for
isolated carver queries. Cache misses use a worker-owned `TerrainColumn`.

Possible biomes are sorted by registry ID. This is part of the deterministic world contract:
Minecraft assigns feature indices in biome encounter order and uses those indices in
random seeds. The unspecified iteration order of `Map.copyOf` could otherwise change
decoration after a JVM restart despite an identical world seed. The server check explicitly
verifies this order. Small differences in complete Minecraft decoration are acceptable
under the agreed requirements; our terrain and climate core remains deterministic.

The ocean aquifer wrapper owns a 256-entry lazy height cache per carving invocation.
Construction does not sample terrain. Only non-solid queries below sea level request
heights, and each requested column is sampled once. Zero is an unsampled sentinel because
valid terrain heights are 1�255. Underground fluid decisions and update scheduling still
delegate to Minecraft; the cache is neither shared nor instrumented.

Minecraft manages chunk tasks. The mod does not start another thread pipeline.
Each task writes only its own chunk. Its primitive chunk buffer is allocated per invocation;
there is no shared pool or reused thread-local chunk buffer that nested calls could overwrite.

Raw fill determines material intervals once per column. Entirely uniform rock sections
above the bedrock layer and below every column's filler start are created as compact
single-entry palettes. The public `LevelChunkSection` constructor computes their block
counts; existing biome containers are preserved. Exclusively owned sections are locked
before the remaining individual block writes. Empty sections above the terrain remain
untouched. Heightmaps and base-column queries use the same material boundaries.
Direct height queries need only the height field because all current palette materials
satisfy every vanilla solid-height predicate; water inclusion depends on the requested
heightmap type. Biome decoration and other Minecraft or third-party mod calls are not
parallelized independently.

API examples, measurement methods and results are in [performance.md](performance.md).

## Intentional differences and compatibility limits

- The surface follows the old heights, while the world now extends down to Y=−64.
  Deepslate lies below Y=0, with a continuous bedrock layer at Y=−64. The old irregular
  bedrock region at Y=0 is omitted. The upper build limit is the normal 320; legacy
  terrain heights remain clamped to 1–255.
- Minecraft now stores biomes on a quart grid with smoothed queries. Surface materials
  are still selected per block from the original climate. Biomes, features, trees,
  weather and caves are therefore not a block-for-block copy of 1.7.10.
- `BiomePalette` selects modern vanilla biomes using the legacy terrain/climate fields
  and adds depth-dependent cave biomes. Historical biome names in the original fixtures
  are no longer asserted as the expected modern selection.
- Registry-owned vanilla biome holders retain NeoForge biome modifiers and their features.
  Custom mod biomes are not selected automatically. The old Thaumcraft integration and
  Forge extension API are not ported.
- Standard carvers and aquifers are used; the modern large 3D noise-cave pass and noise
  ore veins from `fillFromNoise` are not executed. Regular ore features still run.
  Underground aquifers use modern vanilla noise fields. Within the original surface-water
  envelope, our terrain height takes precedence so carvers cannot replace water with air
  based on the different vanilla surface estimate.
- Surfaces are built in one pass from the biome palette, with rock exposed on climate-shaped cliff faces. Changes limited to vanilla
  surface rules do not affect this surface. Modern structure terrain adjustment using
  vanilla Beardifier density is also not part of this height field. A generated village
  has been checked; suitable placement of every structure type and third-party mod
  structure is not guaranteed. Investigation of the 1.21.1 sources confirms that
  `Beardifier.forStructuresInChunk` evaluates three-dimensional density around opted-in
  structure pieces and jigsaw junctions. This is not a heightmap placement switch;
  integrating it requires a deliberate local density/terrain adaptation design, with
  material, heightmap, carving and chunk-border checks. Arbitrary placed features do
  not necessarily opt into structure adaptation at all. No blanket foundation fill
  or third-party template rewrite is applied. The floating garden in the reported
  screenshot has not been conclusively identified from saved structure starts.
- Old chunk-blending data are not evaluated. The port is intended for new worlds.
  Mods that replace the noise router or the entire terrain pipeline require separate
  compatibility checks. Vanilla biomes provide an integration basis, not a blanket
  compatibility guarantee for all mods.

## Future versions

Within Minecraft 1.21.1, select NeoForge through `neo_version` in `gradle.properties`.
The core remains unchanged. For a new Minecraft version, review loader metadata,
pack format, preset/noise-settings data and the few direct Minecraft interfaces;
then run the build, current-generator tests and server checks.

Binary compatibility with unknown future Minecraft versions is not promised.
The architecture limits adaptation to the game integration without preemptively
introducing a separate abstraction layer for each version.
