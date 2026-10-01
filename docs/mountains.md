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

Cliffs now have a dedicated 181 × 163 noise field, appended after the existing seeded
fields. Its contour is warped with existing terrain and ridge signals; block-scale
noise adds a small irregularity to the edge. The cliff field is independent of the
ridge contour, so exposed faces no longer require one particular flank of a ridge.

Activation combines mountain influence with a smooth patch mask from existing broad
terrain and ridge noise. Dryness and cool, moist weathering both lower the patch
threshold (more eligible locations) and increase the height change. Warm, humid
regions retain occasional weak cliffs. Permanent cold does not automatically maximize
weathering. These are artistic approximations, not erosion or a geological simulation.

The dedicated field is sampled only after activation is known to be positive. Mountain
influence at or below 0.08 disables cliffs, and a smooth ramp through 0.40 protects the
lower foothills. The profile combines a steep face with a shorter head/foot apron,
then subtracts the broad shoulder transition:

```text
cliffRelief = 44 * activation * (0.80 * faceTransition + 0.20 * apronTransition - broadTransition)
```

This lowers one side of the contour and raises the other, concentrating the height
change into a short face. Deformation returns to zero outside the broad shoulders and
is bounded to less than ±22 blocks at full activation, scaled down with mountain
influence. It cannot hollow out the approved broad body. Ridge noise varies the face's
half-width from 0.075 to 0.100 in noise space. Below activation 0.55, an additional
smooth widening reaches 0.10 at activation 0.15: terminating cliff patches lose
sharpness as well as height. The apron is 2.5 times wider than the face and carries
only 20% of its transition. The main wall therefore stays steep, while its head and
foot connect through a short ledge/slope instead of an almost single-column cut.
Local noise breaks up the edge. There are no discrete height jumps,
quantized terraces or repeated altitude bands. Spatial width depends on the local
noise gradient, rather than a guaranteed number of blocks.

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
The original noise initialization order and caller-owned buffer contracts are preserved.
The additional cliff field uses the same immutable Simplex source with its own appended
offsets and scales. It costs one extra noise query only in eligible mountain patches;
activation and deformation otherwise use scalar arithmetic. No per-column objects or
neighbor samples are introduced.

After geometry, altitude correction uses the original curve through height 128 and
continues linearly at -0.004 per block above it. The correction table is precomputed.
The `mountain` flag requires at least 24 blocks of added relief. Peak biomes additionally
require effective height 128, including the local biome-height offset. Cold Grove and
Snowy Slopes require mountain relief; ordinary hills retain their lowland climate
biomes. Meadow and the rare Cherry Grove window remain available on highland hills.

Tests check body dominance over ridge valleys, nonzero but subordinate local detail,
continuous boundaries, climate-dependent cliff frequency/strength, bounded cliff
deformation, head/foot transitions and softened ends, deterministic concurrent sampling and agreement
between height, column and chunk paths. No fixed seed-coordinate heights or visual
snapshots restrict future tuning. See [performance.md](performance.md) and
[verification.md](verification.md) for measurements and runtime evidence.

## Client inspection

Use a fresh world or newly generated areas. Existing chunks retain their previous
shape; there is no blending at old/new chunk borders. The mod version remains 1.0.0.

The current eight-block-grid survey found these raw-terrain examples:

| Seed | Mountain X/Y/Z | Exposed cliff X/Y/Z |
| --- | --- | --- |
| -8458999313514431577 | -2008 / 230 / -712 | -680 / 110 / 392 |
| 123456789 | -2128 / 215 / -1576 | -176 / 107 / 0 |

These are inspection aids, not expected test outputs or guaranteed world maxima.
Decoration can change the top block. A denser diagnostic found a pronounced cliff
near X=-2876, Z=1340 on seed -8458999313514431577. The user's screenshot there exposed
overly narrow cliff transitions. In the surrounding 169 × 169-column area, widening
the face and adding the apron reduced the largest adjacent-column step from 25 to 12
blocks, without changing cliff eligibility, amplitude, noise sampling or the mountain
body. These diagnostic values are not visual snapshot requirements. The user approved
the broad mountain body; the revised cliff transitions still need live inspection
at the usual ten-chunk view distance.
