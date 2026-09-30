# Biomes, elevations and water boundaries

New Dawn uses all **51 non-river vanilla overworld biomes in Minecraft 1.21.1**; River
and Frozen River are excluded. Selection is deterministic and uses the existing
temperature, humidity, height, regional-height and mountain values. It requires no
additional noise queries or per-block random decisions. The Nether and End remain vanilla.

## Climate distribution

These are dimensionless noise values, not degrees Celsius. Temperature and humidity
still include the original altitude correction.

| Region | Selection |
| --- | --- |
| Ocean: temperature ≤ −0.5 | Frozen Ocean / Deep Frozen Ocean |
| Ocean: −0.5 < temperature < −0.2 | Cold Ocean / Deep Cold Ocean |
| Ocean: −0.2 ≤ temperature < 0.25 | Ocean / Deep Ocean |
| Ocean: 0.25 ≤ temperature < 0.55 | Lukewarm Ocean / Deep Lukewarm Ocean |
| Ocean: temperature ≥ 0.55 | Warm Ocean; vanilla has no Deep Warm Ocean |
| Cold land | Snowy Plains, Snowy Taiga; Ice Spikes in very dry, cold areas |
| Cool land | Plains, Taiga, Old Growth Pine/Spruce Taiga as humidity increases |
| Temperate land | Sunflower Plains, Plains, Flower Forest, Birch Forest, Old Growth Birch Forest, Forest, Dark Forest along the humidity curve |
| Warm/hot land | Dry savannas/deserts, Sparse Jungle, Jungle and humid Bamboo Jungle |
| Humid lowlands at heights 64–68 | Swamp; Mangrove Swamp at temperature ≥ 0.4 |
| Rare temperate, very humid land in low regional basins | Mushroom Fields |

Ocean biomes apply below terrain height 63; below height 60, the deep variant is selected.
This follows the relatively shallow legacy terrain: "deep" is a relative classification
and does not make the terrain deeper. Heights 63/64 form the beach band; low wetlands and
rare mushroom areas can override that band on land. Low mountain flanks up to height 68
receive Stony Shore.

Surface materials match the biome: sand in warm oceans, gravel in cooler oceans, mycelium
in mushroom fields, podzol in old-growth taigas, mud in mangrove areas, red sand/terracotta
in badlands, and snow/ice blocks in cold mountain biomes.

## Mountain biomes

Thresholds fit the existing terrain, whose mountains are considerably lower than tall
vanilla peaks. The terrain shape itself is unchanged.

- Highland selection starts at height 82, or at 76 when the mountain flag is set.
- Cold, humid intermediate elevations receive Grove; drier or higher cold areas receive
  Snowy Slopes.
- Temperate highlands receive Meadow, or Cherry Grove with suitable warmth and humidity.
  Dry and very humid variants become Windswept Hills, Gravelly Hills or Forest.
- Warm highlands become Badlands, Eroded/Wooded Badlands, Windswept Savanna or Savanna
  Plateau depending on humidity.
- Peak biomes start at height 104: warmer areas receive Stony Peaks; other areas receive
  Frozen Peaks when dry or Jagged Peaks when more humid.

These names do not introduce new mountain shapes: peaks still follow the existing height
field. River biomes are deliberately excluded until actual river generation exists.

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
older chunks; the underlying height field remains the same. The save read for diagnosis
was not modified.

```powershell
.\tools\verify.ps1 -Smoke -Benchmark -Offline -Seed -8458999313514431577 -JavaHome 'C:\Program Files\Java\jdk-21'
```

`terrain-core:biomeTest` checks fixed climate/elevation boundaries, the complete set of
51 biomes and their reachability in 1,048,576 actual terrain columns across four seeds.
The original 30,720 terrain/climate fixtures remain unchanged; their old biome names are
historical reference data, not requirements for the new selection. The server check
covers the water-hole reproduction area on the reported seed, concurrent vertical cache
queries and height predicates for every surface material used.
