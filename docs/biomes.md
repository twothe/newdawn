# Biomes, elevations and water boundaries

New Dawn selects **50 non-river Vanilla overworld biomes**. Sparse Jungle remains
in the 51-entry possible-biome set for saved chunks and stable feature ordering, but
is no longer selected on new surfaces. River and Frozen River are excluded. Selection is deterministic and uses the existing
temperature, humidity, height, regional-height and mountain values. It requires no
additional noise queries or per-block random decisions. The Nether and End remain vanilla.

Upland biome thresholds use the actual terrain height plus an offset of up to six
blocks in either direction. The offset reuses the existing block-scale Simplex
value (23 × 27 blocks), so peak, slope and highland bands follow irregular lines
instead of a single contour. Deep Dark's elevated-terrain condition uses the same
offset. Ocean, beach, wetland and shore choices use physical height to preserve
their relationship to sea level. This offset does not move blocks or change
heightmaps. The biome choice also determines the raw top material, so the two
remain consistent for each sampled column.

## Climate distribution

For measured area shares of the **selected game's biome climates**, including
Minecraft's snow/rain eligibility at surface height, see
[climate-distribution.md](climate-distribution.md). The selection variables below
are a separate model and are not the temperature/downfall values of the chosen biome.

These are dimensionless selection values, not degrees Celsius. Temperature cooling
starts smoothly above Y=80: with `h = max(0, physicalHeight - 80)`, subtract
`0.004 * h * h / (h + 48)`. The values are precomputed. Humidity retains its broad/local
noises and the intentional +0.5 forest overlay, without generic altitude drying.
The overlay triggers above forest noise 0.65, or 0.85 at selection temperature >=0.5.
Its spatial scale and positive direction support local variety and wood access.

| Region | Selection |
| --- | --- |
| Ocean: temperature ≤ −0.5 | Frozen Ocean / Deep Frozen Ocean |
| Ocean: −0.5 < temperature < −0.2 | Cold Ocean / Deep Cold Ocean |
| Ocean: −0.2 ≤ temperature < 0.25 | Ocean / Deep Ocean |
| Ocean: 0.25 ≤ temperature < 0.55 | Lukewarm Ocean / Deep Lukewarm Ocean |
| Ocean: temperature ≥ 0.55 | Warm Ocean; vanilla has no Deep Warm Ocean |
| Cold land: temperature <= -0.55 | Snowy Plains, Snowy Taiga; Ice Spikes in very dry, cold areas |
| Cool land: -0.55 < temperature < -0.22 | Plains on the dry edge, Taiga, Old Growth Pine/Spruce Taiga as humidity increases |
| Temperate land: -0.22 <= temperature < 0.22 | Sunflower Plains, Plains, Flower Forest, Birch Forest, Old Growth Birch Forest, Forest, Dark Forest along the humidity curve |
| Warm land: temperature >= 0.22 | Savanna Plains below humidity -0.10, Vanilla Savanna below 0.40, Jungle below 0.65, then Bamboo Jungle |
| Hot/dry lowland: temperature >= 0.60 and humidity < -0.20 | Desert |
| Humid lowlands at heights 64–68 | Swamp; Mangrove Swamp at temperature >= 0.22 |
| Rare temperate, very humid land in low regional basins | Mushroom Fields |

Ocean biomes apply below terrain height 63; below height 60, the deep variant is selected.
This follows the relatively shallow legacy terrain: "deep" is a relative classification
and does not make the terrain deeper. Heights 63/64 form the beach band; low wetlands and
rare mushroom areas can override that band on land. Low mountain flanks up to height 68
receive Stony Shore.

Surface materials normally match the biome; exposed cliff faces use stone, sandstone or
terracotta according to the surface material. Away from cliffs: sand in warm oceans, gravel in cooler oceans, mycelium
in mushroom fields, podzol in old-growth taigas, mud in mangrove areas, red sand/terracotta
in badlands, and snow/ice blocks in cold mountain biomes.

## Mountain biomes

Mountain geometry and biome eligibility are separate: a high base-terrain hill must
not become a peak merely because of its elevation. See [mountains.md](mountains.md).

- The mountain flag requires at least 24 blocks of added mountain relief.
- Highland selection starts around height 82, or around 76 when the mountain flag is set.
- Cold, humid mountain flanks receive Grove; drier or higher cold mountain areas receive
  Snowy Slopes. Cold hills without substantial mountain relief retain lowland climate biomes.
- Meadow occupies an open temperate upland niche: temperature [-0.10, 0.15) and
  humidity [-0.15, 0.20). Increasing humidity through a forest patch can lead to
  woodland rather than staying in a broad Meadow fallback.
- Rare Cherry Grove retains temperature [0.15, 0.25) and humidity [0.25, 0.40), with
  explicit precedence over the adjacent warm climate transition.
- Dry temperate uplands become Windswept Hills or Gravelly Hills. Very humid true
  mountain flanks can receive Windswept Forest; ordinary hills retain matching
  lowland woodland rather than acquiring a cool mountain biome solely from humidity.
- Warm, dry highlands become Windswept Savanna in mountain regions or Savanna Plateau
  on hills. Hotter dry areas receive Badlands or Eroded/Wooded Badlands. Warm humid
  highlands can receive Savanna/Jungle instead of being forced into a dry plateau.
- Peak biomes require the mountain flag and start around height 128: warmer areas receive Stony Peaks; other areas receive
  Frozen Peaks when dry or Jagged Peaks when more humid.

Biome names do not determine mountain geometry: the terrain profile is calculated first. River biomes are deliberately excluded until actual river generation exists.

Historically, the rare Cherry Grove window replaced the former temperature >= 0.05 / humidity
0.10–0.55 selection. In the four-seed, 1,048,576-column production survey, Cherry
Grove occupied 1,514 positions (0.144% of all sampled positions) before the taller
mountain follow-up, down from
20,061. At that time, positions excluded by the narrower window became Meadow;
height eligibility retained the same numerical thresholds. With the taller mountain profile the count
was 1,598 (0.152%). The game-resolved eight-seed climate rebalancing survey measures
0.235% of land, versus 0.214% before this change. These surveys use different sampling
grids and denominators; their counts are not interchangeable. This measures area,
not the number or size of connected groves.

## Cave biomes without additional 3D noise

The surface biome is retained above the cave ceiling, which is
`min(40, terrainHeight − 20)`. Cave biomes start at least 20 blocks below the terrain
surface, including beneath deep oceans. Minecraft's quart resolution and normal biome
query smoothing still apply.

- Humid columns that are not too cold receive Lush Caves.
- Moderately dry to moderately humid columns that are not too cold receive Dripstone Caves.
- Beneath mountainous terrain at height 92 or above, Deep Dark extends up to Y=−24;
  another cave biome may exist above it. Other columns retain their surface biome
  where the climate is unsuitable for either cave biome.

For each horizontal position, the worker cache stores the surface biome, cave candidate,
cave ceiling and Deep Dark flag. Vertical queries therefore need only boundary comparisons.
The cache remains limited to 256 positions per worker; it does not allocate a mutable
sample object for each position.

Registered vanilla features also run for cave biomes. The mod continues to use carver
caves; this does not add the large modern 3D noise caves.

## Water-hole cause and fix

The issue was reproduced on the reported seed `-8458999313514431577` near X=−2584, Z=−601.
Raw terrain was completely filled with water. Vanilla's replaceable-block tag allows
Minecraft carvers to replace water. The vanilla aquifers estimated fluid levels from a
vanilla surface that differed from New Dawn's terrain, returning air at those locations.
This stage alone introduced 71 air blocks in chunk [−162, −38]. After full generation,
402 air blocks remained inside the original water columns across the 81 inspected chunks.

`TerrainCarvers` runs the same registered carvers with the same start-chunk traversal
order and seed derivation. An invocation-owned aquifer wrapper preserves the original
water envelope between terrain height and Y=63. Outside that envelope, the normal aquifer
still decides. This does not flood all underground caves or other world types.
The boundary is supplied before carving; there is no subsequent repair scan over
already generated world blocks.

With the fix, the same area contains **0 rather than 402 air blocks** inside its original
water envelope after full generation and restart. A boundary test also checks water
preservation, positive-density behavior and unchanged decisions below the seabed and
above sea level.

## Existing worlds and verification

These changes apply to **newly generated chunks**. Saved biomes and water holes are not
rewritten automatically. Biomes and surface materials can change at boundaries with
older chunks. The mountain redesign also changes heights inside mountain regions;
see [mountains.md](mountains.md). The save read for diagnosis
was not modified.

```powershell
.\tools\verify.ps1 -Smoke -Benchmark -Offline -Seed -8458999313514431577 -JavaHome 'C:\Program Files\Java\jdk-21'
```

`terrain-core:biomeTest` checks fixed climate/elevation boundaries, the complete set of
51 possible biomes and the reachability of all 50 selected biomes in 1,048,576 actual terrain columns across four seeds.
The tests check current biome rules and reachability without comparing terrain shapes
to the Forge 1.7.10 generator. The server check covers the water-hole reproduction
area on the reported seed, concurrent vertical cache
queries and height predicates for every surface material used.

## Open vegetation without new biome identities

Savanna is not treeless in Vanilla: `trees_savanna` attempts 1.1 trees per chunk on
average. Sparse Jungle attempts 2.1, Windswept Savanna 2.1, Plains 0.05, Snowy Plains
and Windswept Hills 0.1, and Meadow one tree attempt per 100 chunks. These are attempts,
not guaranteed successfully grown trees. A biome-ID-only woodland statistic therefore
cannot describe actual canopy coverage or distinguish open terrain from scattered trees.

New Dawn keeps these Vanilla registry holders, climate, colors, mobs and structures.
The `newdawn:tree_patches` NeoForge biome modifier appends a placement filter to tree
features. In a New Dawn world, tree candidates in `newdawn:open_vegetation` biomes must
lie inside the existing pointwise forest overlay. No extra noise field, neighbor scan,
random draw or terrain/climate change is introduced. Genuine forests, taigas, jungles,
Grove, Cherry Grove, swamps and Wooded Badlands keep their original tree placement.
The filter passes immediately for other generators, including the Vanilla preset.
Flowers, grass, ores, carvers and unrelated placed features retain their original holders.
Tree crowns may naturally extend over a nearby biome or patch boundary; this controls
placement origins rather than clipping already grown trees.

The default open tag contains Plains, Sunflower Plains, Snowy Plains, Ice Spikes,
Meadow, Windswept Hills and Windswept Gravelly Hills. Savanna-family biomes use the
separate `newdawn:savanna_vegetation` tag and humidity rule described below. Sparse
Jungle retains Vanilla tree decoration when present in saved chunks or other sources. Change this tag in a datapack to change the affected biome
set. The forest threshold is owned by `ClimateRules`; `TerrainSampler.isForestPatch`
uses the same climate-dependent trigger as the positive woodland humidity overlay.

The modifier runs in `AFTER_EVERYTHING`, after ordinary feature additions/removals.
It recognizes configured `TreeFeature` implementations recursively through selectors.
Mods using other tree implementations can opt their placed features into the
`newdawn:tree_features` tag. A tagged or mixed selector is filtered as a whole; do not
tag a selector mixing trees with unrelated decoration unless that is desired.
Features directly placed outside biome decoration, player-grown saplings, structures,
and features added by a later `AFTER_EVERYTHING` modifier are outside this mechanism.
Vanilla biome IDs and biome tags are unchanged, preserving identity-based mod integration.
The wrappers use direct placed-feature holders, so compatibility with mods requiring
original placed-feature registry keys *after* this phase is not guaranteed.

Each original tree feature gets one shared wrapper across all biomes. Original order,
configured features and placement modifiers stay intact. This avoids duplicating shared
features, disrupting encounter indices or creating feature-order cycles. Wrapper caching
belongs to a loaded modifier instance and is used only during registry setup; generation
uses immutable placement data and the thread-safe sampler.

The server fixture audits actual features for all 51 selected biomes and writes
`tree-decoration-audit.json` into its private server directory. It checks original order,
shared wrappers, unchanged non-tree holders, the Vanilla bypass and unchanged random
state. It also searches the current seed for homogeneous dry Savanna regions, generates full
chunks and verifies that their interiors contain no logs. Moderately humid Savanna is
checked for real native trees outside the forest overlay.
Client inspection with the intended modpack remains necessary for visual acceptance.

The mechanism follows the [NeoForge 1.21.1 biome modifier API](https://docs.neoforged.net/docs/1.21.1/worldgen/biomemodifier/).

For an unfiltered development comparison, override
`data/newdawn/neoforge/biome_modifier/tree_patches.json` with
`{"type":"neoforge:none"}` in a private test-world datapack, then run the fixture with
`-PdecorationBaseline`. This option verifies that no tree filters remain before reporting
the same open-chunk log counts; it does not weaken the normal server checks.
In the measured seed, the Savanna chunk at -544/-8192 changed from 9 to 0 log blocks,
and the Sparse Jungle chunk at 4352/-8128 from 1 to 0. These are illustrative current-run
observations, not fixed snapshots or an estimate of global tree cover.

## Savanna Plains and Vanilla Savanna (2026-10-02)

Both vegetation variants resolve to `minecraft:savanna`: they have identical Vanilla
grass/foliage color, biome tags, structures and mod identity. "Savanna Plains" is a
design name for the dry placement rule, not a new registry biome or translated biome name.
At final selection humidity below -0.10, Savanna tree placements are rejected entirely.
From -0.10 to below 0.40, the original Vanilla Savanna tree attempts are allowed, even
outside the forest overlay. Jungle starts at 0.40 as before, with Bamboo Jungle at 0.65.
The hot/dry Desert exception, wetlands, rare destinations and mountain rules retain their
precedence. Savanna Plateau and Windswept Savanna use the same tree humidity threshold.

The existing +0.5 forest humidity increment can turn dry land into wooded Savanna
without changing its grass color. Naturally more humid land can do the same without
requiring a forest patch. Already moist patches may still enter Jungle; the progression
retains climate-dependent destinations rather than promising one color across all humidity.
No artificial Sparse Jungle transition is introduced in this three-stage design.

`BiomePalette.allowsSavannaTrees` owns the shared threshold. The placement filter uses
`TerrainSampler.sampleHumidity`, a zero-allocation query agreeing exactly with full-column
humidity. It skips unused relief/filler/material outputs and computes altitude only when
needed to resolve the climate-dependent forest trigger. This does not add any work to
normal terrain sampling or introduce another noise field.

The previous open-Sparse-Jungle log counts above describe the superseded placement rule.
Earlier climate statistics likewise describe the prior selection: replacing Sparse Jungle
with Savanna changes registered downfall/color statistics, although selection noise and
physical heights remain unchanged. Those historical figures are not current distribution
measurements. Final visual acceptance belongs in the modpack client.
