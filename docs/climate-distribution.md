# Game-resolved climate distribution (2026-10-01)

The first sections retain the pre-change measurement. The follow-up implementation
and comparison are documented below. The [implementation brief](climate-rebalancing-plan.md)
explains the design and preserves the intentional forest overlay for local variety
and wood availability.

## Method and scope

The opt-in development survey samples **2,097,152 surface positions across eight
seeds**, including the reported seed `-8458999313514431577`. Each seed uses a
512 × 512 jittered grid with 192-block cells, covering 98,304 × 98,304 blocks.
Land accounts for 1,379,852 positions; water accounts for 717,300. Percentages below
describe land area, not biome counts or the number of connected biome regions.

Selections go through the production `NewDawnBiomeSource` and Minecraft's
`BiomeManager` quart interpolation. Climate comes from the registry's modified
biome settings after server initialization. Snow/rain eligibility uses Minecraft's
`getPrecipitationAt` at the sampled ground/water surface, including its temperature
modifier and height adjustment. No millions-of-chunks generation is needed.

This measures New Dawn with the development runtime's vanilla biome climates.
Third-party modpack climate/weather changes, decoration, sky exposure, lighting,
and client color blending are outside this sample. `downfall` is Minecraft's
humidity/color parameter, not a probability of rain or a measured rainfall rate.
Snow/rain percentages describe weather eligibility, not active weather frequency.

## Land results

| Registered biome temperature | Area | Range across seeds |
| --- | ---: | ---: |
| Cold: < 0.15 | 13.09% | 12.07–13.67% |
| Cool: 0.15 to < 0.5 | 23.11% | 22.77–23.72% |
| Mild: 0.5 to < 0.9 | 51.65% | 50.92–52.40% |
| Warm: 0.9 to < 1.5 | 3.77% | 3.65–3.87% |
| Hot: >= 1.5 | 8.39% | 8.26–8.53% |

| Registered biome downfall | Area | Range across seeds |
| --- | ---: | ---: |
| Arid: <= 0.2 | 8.39% | 8.26–8.53% |
| Dry: > 0.2 to 0.4 | 21.44% | 21.21–21.60% |
| Moderate: > 0.4 to 0.6 | 16.12% | 15.84–16.39% |
| Humid: > 0.6 | 54.05% | 53.68–54.41% |

At the actual sampled elevation, 13.18% permit snow, 78.43% rain and 8.39% no
precipitation. The most common land biomes are Taiga (14.34%), Meadow (12.94%),
Plains (9.75%), Birch Forest (7.15%) and Savanna (6.88%).

Observations are spatially correlated. Seed ranges show stability; treating all
two million positions as independent trials would overstate precision. The bin
boundaries are explicit descriptive choices, not a universal definition of balance.
Equal percentages per climate class or biome were never an agreed target.

## Translation effects and intervention points

The resulting climate is not an even split of dry/wet or of temperature categories.
That alone does not prove an unwanted bias. Noise is not uniformly distributed over
its numerical range, and weighted fields, altitude correction, and forest patches
also affect its distribution. The following mapping effects are directly measurable:

- **Moist output around neutral input:** for selection humidity between -0.2 and
  +0.2, 55.71% of land resolves to humid game biomes. Taiga and Meadow both carry
  downfall 0.8; their broad selection windows account for much of the wet output.
  Among non-mountain high hills, humid output reaches 73.64%. To reduce this,
  adjust the lowland/highland dry-to-forest/meadow selection boundaries.
- **Warm areas are mostly arid:** 69.01% of warm/hot land has no precipitation.
  The warm lowland rule selects Savanna below selection humidity 0.15. In the actual
  1.21.1 registry it has temperature 2.0 and downfall 0.0; Jungle and Sparse Jungle
  instead have temperature 0.95 and downfall 0.9/0.8. Moving that humidity boundary
  downward would increase rainy warm biomes and reduce the jump into hot/arid climate.
- **Mountains are predominantly cold and moist in game:** 68.56% of mountain land
  has cold base temperature and 91.51% humid downfall. Frozen Peaks occupies 46.03%
  of mountain land. The source temperature and humidity both receive a negative
  altitude correction; 76.89% of mountain selection humidity is negative. Yet Frozen
  Peaks and Jagged Peaks both have downfall 0.9 and temperature -0.7. Separating
  humidity's altitude response can improve peak selection, but swapping these two
  biomes cannot reduce their game humidity. Mountain temperature/biome eligibility
  should be tuned separately if less snowy mountain area is desired.

A blanket temperature offset would change many already reasonable lowlands.
First choose the desired output mixture, then change the relevant selection boundary
and repeat this survey. Generation rules were not changed during this analysis.

## Reported world and local sampling

Land climate in three windows of the reported seed:

| Window center / width | Cold | Warm + hot | Humid |
| --- | ---: | ---: | ---: |
| (0, 0), 16,384 blocks | 13.61% | 12.03% | 54.44% |
| (-703, 2887), 4,096 blocks | 11.07% | 18.67% | 58.11% |
| (5823, 2233), 4,096 blocks | 9.94% | 9.35% | 55.05% |

These windows explain some variation in impressions, but do not reveal a large
seed-specific anomaly. A ten-chunk view can still lie almost entirely in one patch.

## Repeating the diagnostic

Use Java 21 and a private development server directory:

```powershell
.\gradlew.bat runSmokeServer -PclimateSurvey -PsmokeDirectory=build/climate-survey
```

The server shuts down after the survey. It writes
`build/biome-climate-survey.json`, containing seed/group/biome counts, source-to-game
cross-tabs and the registered climate settings. The survey is in `src/integrationTest`;
it is absent from normal builds' execution and the release JAR. The usual short checks
and release archive verification passed; the survey itself passed in a real server.
Local run evidence is in `build/biome-climate-survey-run.log` and
`build/biome-climate-survey-build.log`.

## Rebalanced implementation and matched measurement

The final implementation retains the forest increment, trigger, field scales and
geometry inputs. Temperature cooling starts smoothly at Y=80; generic altitude
drying is removed. Warm climates begin earlier, Meadow is an explicit upland niche,
and hills can retain climate-compatible woodland. Birch woodland bridges intermediate
moisture without expanding every moderate area into a high-downfall forest.

The same eight seeds and 2,097,152 game-resolved surface positions give:

| Land climate | Before | After |
| --- | ---: | ---: |
| Cold | 13.09% | 9.32% |
| Cool | 23.11% | 21.32% |
| Mild | 51.65% | 40.23% |
| Warm | 3.77% | 18.79% |
| Hot | 8.39% | 10.34% |
| Cold + cool | 36.20% | 30.64% |
| Warm + hot | 12.16% | 29.13% |
| Arid | 8.39% | 10.34% |
| Dry | 21.44% | 21.65% |
| Moderate downfall | 16.12% | 18.89% |
| Humid | 54.05% | 49.13% |

The temperature-side difference falls from 24.04 to 1.51 percentage points. Across
individual seeds, cold/cool occupies 30.16–31.19% and warm/hot 28.72–29.32%; every
seed improves. Cold stays at 8.41–9.85%. Humid output stays at 48.81–49.48%.
At the sampled surface, snow eligibility falls from 13.18% to 9.35%; rain eligibility
is 80.32%, and rainless land is 10.34%. Surface weather is measured by Minecraft,
not inferred solely from registered temperature.

The reference targets are tuning guidance. The combined mild share is 40.23%, close
to the lower corridor; two individual seeds are slightly below 40%. Moderate downfall
does not reach the proposed 28%, and dry/arid totals 31.98% rather than 25%.
The joint matrix explains the constraint: 20.64% of all land is both cool and humid,
and 17.88% is both warm and humid. These are ordinary tree-bearing vanilla families.
Their fixed climate values cannot be made moderate by moving a humidity threshold.
Further forcing of a humidity histogram would require more birch dominance, fewer
appropriate taiga/jungle regions, or biome climate overrides. Those are outside the
agreed priorities. Humid output is reduced while woodland access improves.

| Terrain stratum | Humid before | Humid after |
| --- | ---: | ---: |
| Lowlands | 45.10% | 45.55% |
| Non-mountain hills | 73.64% | 55.15% |
| Mountains | 91.51% | 69.32% |

The principal reduction occurs in uplands, where the previous default Meadow and
altitude corrections were influential. Mountain biomes remain colder than ordinary
land: 54.17% cold after rebalancing. Geometry itself retains the same inputs and
benchmark height checksums. Meadow covers 1.94% of land, down from 12.94%; Cherry
Grove remains rare at 0.235%, versus 0.214%. Ice Spikes remains rare at 0.151%, versus
0.243%. All 51 supported non-river biomes are reached in the extended core survey.

## Woodland and local climate access

The extended survey defines a wooded proxy as an explicit set of tree-bearing biome
IDs, including forests, taigas, jungles, swamps, savannas, Grove, Cherry Grove,
Windswept Forest/Savanna and Wooded Badlands. It measures biome presence, not actual
trees or wood yield. Proxy area rises from 59.13% to 70.96% of land.

For each seed, a fixed independent RNG selects 16 origins over the survey domain;
only physical water is rejected. Terrain heights are unchanged, so the same 128
land origins are used before and after. A 16-block grid searches within a 1,024-block
circle, using actual game biome queries at the sampled land surface.

| Sampled proximity | Before | After |
| --- | ---: | ---: |
| Wooded biome within 160 blocks | 127/128 | 128/128 |
| Wooded biome within 320 blocks | 128/128 | 128/128 |
| Wooded biome within 640 blocks | 128/128 | 128/128 |
| Cold/cool land within 512 blocks | 127/128 | 112/128 |
| Mild land within 512 blocks | 128/128 | 128/128 |
| Warm/hot land within 512 blocks | 84/128 | 105/128 |
| Cold/cool land within 1,024 blocks | 128/128 | 128/128 |
| Mild land within 1,024 blocks | 128/128 | 128/128 |
| Warm/hot land within 1,024 blocks | 126/128 | 127/128 |

Counts retain failures, including the remaining unresolved warm search. These are
bounded sampled straight-line estimates, not exact nearest distances, spawn guarantees,
visibility or traversability. The improvement does not justify altering regional
noise scales or weights in this pass. Client inspection with modded decoration remains
the visual acceptance step.

Fixed-elevation game queries also retain cave-biome coverage: Lush Caves at Y=0 grows
from 293,401 to 304,885 positions, Dripstone Caves from 1,394,060 to 1,418,026; Deep
Dark at Y=-48 remains 87,897. Each uses the same 2,097,152 horizontal positions.
These count biome assignments, not air volume or generated cave sizes.

To include proximity diagnostics:

```powershell
.\gradlew.bat runSmokeServer -PclimateSurvey -PclimateAccess -PsmokeDirectory=build/climate-survey
```

The JSON adds joint temperature/downfall matrices, wooded counts by temperature and
terrain stratum, underground queries, and each origin's distances and unresolved
searches. Baseline evidence is in `build/climate-before/survey.json`; final measurement
is `build/climate-candidate2.json` and `build/biome-climate-survey.json`. Candidate 1
was measured before the final cold threshold and temperate woodland adjustment.

Short contracts, the 1,048,576-column all-biome survey, extended concurrency/sampling
checks, release-archive checks, and fresh/reloaded server checks all passed. The
reported-seed water-hole check still found zero holes across 81 fully generated chunks.
Before/after CPU and allocation evidence is in [performance.md](performance.md).
No overall visual/modpack acceptance is claimed from these diagnostics.

## Decoration-aware interpretation

The earlier 70.96% tree-bearing-biome proxy includes Savanna and Sparse Jungle, whose
scattered trees motivated the open-vegetation correction in [biomes.md](biomes.md).
That correction changes placement, not biome assignments: the climate percentages
above remain valid, but the proxy and 160-block proximity counts cannot prove actual
tree availability or visual forest coverage. Use the real configured-feature audit and
client inspection for that question. No canopy percentage is inferred from biome IDs.

The 2026-10-02 Savanna bands supersede the Sparse Jungle surface transition used by
this survey. Treat these numbers as historical until rerun: selection noise is unchanged,
but substituting Vanilla Savanna changes game-resolved downfall and vegetation.
