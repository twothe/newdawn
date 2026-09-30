# Architecture and terrain contract

## Components

`terrain-core` is a Java 21 module with no external dependencies. `TerrainSampler` and
`BiomePalette` contain the generation rules. `TerrainColumn` and `TerrainChunk` are
reusable, caller-owned output buffers; `TerrainSample` remains available as an immutable
snapshot. This module compiles without any Minecraft or NeoForge classes.

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

## Preserved algorithm

- `java.util.Random(seed)` initializes 1,024 random bytes for the Simplex field as before.
  This historical field is **not** a standard 256-entry permutation. Replacing it with
  another Simplex library would produce different worlds.
- All 13 general noise fields, followed by four mountain fields, are initialized in the
  original order. Even the filler field affects later climate and mountain fields by
  consuming random numbers during initialization.
- Scales, offsets, weights, arithmetic order, rounding, height clamping and float conversion
  match the original. `BLOCK_SCALE=2` is part of the terrain shape and is deliberately
  independent of the modern build height.
- Ground level remains 64: Y=63 is the top water block; a terrain height of 64 means
  the first free block above solid ground at Y=63.
- Temperature and humidity fields, including altitude correction and forest patches,
  are preserved. Biome selection now covers all 51 non-river overworld biomes; climate,
  elevation and depth rules are documented in [biomes.md](biomes.md).
- Mountain shapes have deliberately not been redesigned. River generation is still absent.
  Horizontal scales that determine the original terrain variation within a typical view
  distance are also preserved.

A set of 30,720 reference points captured from the separately compiled original code
protects these rules. The original project is neither modified nor needed for normal builds.

## Seed ownership and concurrency

Minecraft calls `createState` with the world seed when setting up the chunk pipeline.
The BiomeSource is initialized once there; its immutable sampler is safely published
through a `volatile` field. Subsequent initialization with a different seed fails explicitly.
On restart, Minecraft decodes the generator and binds the saved world seed again.
The preset does not store a hard-coded seed.

Core sampling requires neither locks nor RNG calls. `sampleInto` and `sampleChunkInto`
write into exclusive caller-owned buffers without allocations; `sampleHeight` skips
climate and filler calculations. The previous snapshot methods remain available.
Altitude-dependent climate corrections are precomputed once for all 255 possible heights
using the original formula. The BiomeSource maintains a cache of at most 256 entries per
worker, using full coordinate keys; collisions cannot change results. This avoids repeated
climate calculations for vertical biome quarts and samples only the requested column for
isolated carver queries. Cache misses use a worker-owned `TerrainColumn`.

Possible biomes are sorted by registry ID. This is part of the deterministic world contract:
Minecraft assigns feature indices in biome encounter order and uses those indices in
random seeds. The unspecified iteration order of `Map.copyOf` could otherwise change
decoration after a JVM restart despite an identical world seed. The server check explicitly
verifies this order. Small differences in complete Minecraft decoration are acceptable
under the agreed requirements; our terrain and climate core remains deterministic.

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
- Surfaces are built in one pass from the biome palette. Changes limited to vanilla
  surface rules do not affect this surface. Modern structure terrain adjustment using
  vanilla Beardifier density is also not part of this height field. A generated village
  has been checked; suitable placement of every structure type and third-party mod
  structure is not guaranteed.
- Old chunk-blending data are not evaluated. The port is intended for new worlds.
  Mods that replace the noise router or the entire terrain pipeline require separate
  compatibility checks. Vanilla biomes provide an integration basis, not a blanket
  compatibility guarantee for all mods.

## Future versions

Within Minecraft 1.21.1, select NeoForge through `neo_version` in `gradle.properties`.
The core remains unchanged. For a new Minecraft version, review loader metadata,
pack format, preset/noise-settings data and the few direct Minecraft interfaces;
then run the build, reference tests and server checks.

Binary compatibility with unknown future Minecraft versions is not promised.
The architecture limits adaptation to the game integration without preemptively
introducing a separate abstraction layer for each version.
