# Mountain bodies, ridges and climate-shaped cliffs

## Generation contract

Every column is a direct function of its X/Z coordinates and seed-specific noise.
There are no neighboring height queries, environment scans, carvers, iterative erosion
or shared mutable buffers in the mountain profile. Temperature and humidity enter
before altitude correction, so geometry cannot feed back into its own climate inputs.

The mountain distribution field retains its existing threshold and the user-tuned
1.8 multiplier on its 897 × 957 scales. No additional field controls range length.
Large connected regions emerge from that field. `MountainProfile.influence` subtracts
the activation threshold from the legacy cosine/power curve and broadens the normalized
result with `u * (2 - u)`. All relief fades continuously to zero at the boundary.
Base terrain outside active mountain regions is unchanged.

## Broad bodies with subordinate ridges

Folding noise with `1 - abs(noise)` places ridges along its zero contours. Using that
signal for nearly the entire height created narrow walls with deep depressions between
them. The body now receives most of the height budget independently of those contours:

```text
profile = 0.60 + 0.15 * broadVariation + 0.25 * ridgeProfile * summitVariation
uplift = influence * (144 * profile + surfaceRelief) + cliffRelief
```

`broadVariation` is the normalized, bounded combination of the already sampled signed
913 × 967 and 413 × 467 base-terrain fields. It is not folded into ridges. At equal
regional influence, valleys therefore retain a substantial mountain body.

The existing 127 × 119 and 41 × 45 mountain fields provide coupled ridges. Broad
base-terrain noise bends their coordinates; the main ridge also bends the secondary
field. Dryness and a cool, moist weathering band favor sharper ridges. Signed secondary
noise varies crest height along the main ridge, producing summit segments and saddles.
Fine ridges still follow the main relief, but cannot excavate the broad body.

The 144-block amplitude is a profile scale, not a minimum height or a guaranteed summit
height. Distribution influence, broad variation, base terrain and local overlays all
affect the result. The final terrain remains bounded to heights 1–255.

## Local variation and cliffs

The already sampled 23 × 27 block noise adds up to ±5 blocks of coherent surface
variation at full mountain influence. The existing 2 × 2.2 mountain detail field adds
at most ±1.25 blocks, with its strength depending on climate. Both fade toward the
mountain boundary. This adds no noise queries, fields or output-buffer storage.
The separate ±6-block biome-height offset continues to move material/elevation bands;
it does not change physical height.

Cliffs replace a broad flank transition with a narrow smooth transition. Dryness and
cool, moist weathering strengthen them. A smooth mask from signed secondary noise
breaks their strength into patches rather than following an entire contour at equal
strength. The deformation returns to zero beyond the affected band. These are artistic
approximations of weathering, not erosion or a geological simulation.

The analytical cliff-band position and amplitude produce an `exposedRock` flag. This
indicates the profile's cliff band, not the final terrain gradient. Such columns omit
soil/filler and use stone, sandstone, red sandstone or terracotta according to the
biome's material palette. Raw fill and base-column queries share those intervals.
Vanilla features may subsequently modify the surface. Height fields cannot create
overhangs, and noise alone does not guarantee connected drainage or eliminate every
closed basin.

## Climate, biomes and performance

Mountain shaping uses regional/area temperature and humidity; local climate noise
and forest patches are excluded. Full sampling reuses those climate values. Height-only
queries evaluate the four broad climate fields inside active mountain regions.
Noise initialization order, noise-query count and caller-owned buffer contracts are
unchanged by the body redesign. The additional work is scalar arithmetic.

After geometry, altitude correction uses the original curve through height 128 and
continues linearly at -0.004 per block above it. The correction table is precomputed.
The `mountain` flag requires at least 24 blocks of added relief. Peak biomes additionally
require effective height 128, including the local biome-height offset. Cold Grove and
Snowy Slopes require mountain relief; ordinary hills retain their lowland climate
biomes. Meadow and the rare Cherry Grove window remain available on highland hills.

Tests check body dominance over ridge valleys, nonzero but subordinate local detail,
continuous boundaries, climate response, deterministic concurrent sampling and agreement
between height, column and chunk paths. No fixed seed-coordinate heights or visual
snapshots restrict future tuning. See [performance.md](performance.md) and
[verification.md](verification.md) for measurements and runtime evidence.

## Client inspection

Use a fresh world or newly generated areas. Existing chunks retain their previous
shape; there is no blending at old/new chunk borders. The mod version remains 1.0.0.

The current eight-block-grid survey found the following raw-terrain examples:

| Seed | Mountain X/Y/Z | Exposed cliff X/Y/Z |
| --- | --- | --- |
| -8458999313514431577 | -1744 / 223 / -568 | -680 / 109 / 400 |
| 123456789 | -2040 / 214 / -1512 | -128 / 95 / 64 |

These are inspection aids, not expected test outputs or guaranteed world maxima.
Decoration can change the top block. The appearance at the usual ten-chunk view
distance remains subject to live client inspection.
