# Climate rebalancing: implementation brief

Status: implemented, 2026-10-01. This document retains the design and initial tuning
candidates; final rules and measured outcomes are in `biomes.md` and the follow-up
section of `climate-distribution.md`. Read `AGENTS.md`, `architecture.md`, `biomes.md`,
`climate-distribution.md`, and `performance.md` before implementation.

## Intended result and priorities

New Dawn should provide recognizable, varied landscapes with understandable biome
transitions and useful resources within ordinary exploration distances. There are
no poles or equator: cold and warm regions should have similar overall availability.
Moderate climates dominate; extremes and special destinations remain uncommon.

The forest overlay is intentional gameplay design. Its one-sided humidity increase
creates local variety and access to wood. Preserve it. Do not center, remove, weaken,
or statistically cancel this overlay to improve a climate histogram.

Resolve competing goals in this order:

1. Pointwise deterministic generation, performance and thread safety.
2. Useful local wooded areas, coherent biome transitions and terrain eligibility.
3. Approximately balanced cold/cool versus warm/hot land, with a moderate center.
4. Less accidental humidity concentration, within the vanilla biome palette.
5. Individual biome percentages; these must not override the preceding goals.

These are design objectives, not a guarantee of a forest at every visible location
or of every biome within a fixed radius. Judge the overall result in the Minecraft
client with the intended modpack. Statistical measurements support that judgment.

## Baseline and corrected interpretation

The existing game-runtime survey covers 2,097,152 positions and eight seeds:

- Cold plus cool land: 36.20%; warm plus hot: 12.16%; mild: 51.65%.
- Humid land (`downfall > 0.6`): 54.05%; moderate: 16.12%; dry plus arid: 29.83%.
- Mountains: 68.56% cold and 91.51% humid.
- Non-mountain hills: 73.64% humid, partly because Meadow is the default.

The cold/cool versus warm/hot comparison confirms the reported imbalance under
the current descriptive bins. Comparing only cold against warm plus hot misses it.
Noise amplitude is not uniformly distributed, and a sum of Simplex fields is not
automatically a normal distribution. Measure the translation instead of assuming it.

The earlier conversational suggestion to remove the forest overlay is superseded
by the user's explicit instruction to retain it. The baseline's numerical results
remain useful. Keep the historical report separate from post-change results.

## Output targets and feasibility

Use the same registered-temperature bins as the baseline; do not relabel biomes or
move measurement boundaries to make a result appear balanced. Count area, not IDs.

| Land temperature | Reference share | Initial tuning corridor |
| --- | ---: | ---: |
| Cold, below 0.15 | 8% | 6–10% |
| Cool, 0.15 to below 0.5 | 20% | 16–24% |
| Mild, 0.5 to below 0.9 | 44% | 40–48% |
| Warm, 0.9 to below 1.5 | 18% | 14–22% |
| Hot, at least 1.5 | 10% | 8–12% |

The reference distribution gives 28% on each side and 44% in the middle. The first
objective is a cold/cool versus warm/hot difference of at most eight percentage
points over the combined sample, with improvement across seeds. These are proposed
review criteria, not permanent build assertions or guaranteed achievable quotas.

For humidity, provisionally explore 25% dry/arid, 28% moderate and 47% humid, with
humid roughly 45–50%. This deliberately revises the earlier 35–40% humid proposal:
increasing warm jungles while retaining wooded cool regions makes that lower target
questionable. Do not promise a normal distribution of vanilla downfall values.

Vanilla's palette has missing climate combinations. Taiga is cool and humid; Jungle
and Sparse Jungle are warm and humid; Savanna is hot and rainless. Many mountain
biomes are cold and humid. Filling a missing cool/dry lowland cell with mountain
terrain, or removing forests, would be a worse result than missing a percentage.
Build a joint temperature/downfall table from the actual runtime registry first.
Report trade-offs if the corridors conflict. No custom climate overrides, duplicate
biome registrations, or runtime quota correction are part of this change.

Keep land, water, lowlands, hills and mountains separate in reporting. Do not force
mountain or ocean distributions to match the all-land target. Also report actual
snow/rain eligibility: base biome temperature alone does not describe surface weather.

## Production changes

### 1. Preserve the forest mechanism and geometry inputs

In `ClimateRules.complete`, retain the existing forest field, spatial scale, offset,
positive increment and climate-dependent trigger:

```text
forest threshold = final temperature >= 0.5 ? 0.85 : 0.65
forest humidity increment = forest noise > threshold ? 0.5 : 0.0
```

The trigger continues to use final selection temperature. Its geographic footprint
can change when temperature is deliberately retuned; retain the rule and evaluate
the resulting wood access rather than asserting identical old coordinates.

Do not subtract the mean forest increment elsewhere. Tune biome selection with the
overlay enabled. In suitable lowlands and lower uplands, increasing humidity through
the overlay should normally lead toward a wooded climate-compatible biome. Avoid
broad rules that consume the extra humidity but still default to treeless Meadow.
Peaks, ice, ocean and genuinely arid extremes need not become forests.

Keep `broadTemperature` and `broadHumidity`, noise initialization order, field scales,
and their use by `TerrainRelief`, `MountainProfile`, and `CliffProfile` unchanged in
the first implementation. This avoids changing the successful mountain/cliff geometry
while tuning the final biome climate. Reuse existing noise evaluations.

### 2. Separate altitude cooling from humidity

Replace the combined altitude correction with temperature-only cooling. Start with
this simple candidate, using physical terrain height:

```text
h = max(0, height - coolingStartHeight)
cooling = coolingRate * h * h / (h + onsetHeight)
final temperature = broad temperature + existing local temperature - cooling
final humidity = broad humidity + existing local humidity + forest increment
```

Initial exploration values: `coolingStartHeight = 80`, `onsetHeight = 48`,
`coolingRate = 0.004`. They are tuning seeds, not approved final constants. This
gives cooling of approximately 0.016 at Y=96, 0.096 at Y=128, 0.314 at Y=192 and
0.549 at Y=255. The curve starts smoothly and approaches a linear gradient. It is
a compressed gameplay model, not a Celsius simulation or an assertion that an
exponential is physically required.

Precompute all valid height entries as today. Name and document these three controls
in `ClimateRules`; no per-column powers, exponentials or new allocations. Do not
apply the local biome-height offset to physical temperature: it already varies the
elevation eligibility bands. No generic altitude drying or wetting in this pass.

Check Minecraft's additional elevation-dependent weather behavior in the survey.
Tuning the selection climate does not replace Minecraft's own biome weather rules.

### 3. Rebalance the surface palette

Keep readable pure decision methods in `BiomePalette`, with named principal climate
boundaries next to the owning rules. No weighted random selection or generic scoring
framework is necessary. A shared climate transition must use the same threshold
across related branches unless an explicit ecological difference justifies it.

Start by narrowing the cool lowland interval and allowing warm biomes to begin
earlier. Candidate lowland temperature boundaries for the first survey are
`-0.50 / -0.22 / 0.22 / 0.60` instead of `-0.50 / -0.15 / 0.40 / 0.65`.
The final boundary is a hot/dry eligibility threshold, not an automatic hot biome
for all humidity values. Keep the unchanged forest trigger separate from these
biome thresholds. Calibrate against output, including all terrain strata.

For warm lowlands, initially try moving the Savanna-to-Sparse-Jungle humidity
boundary from `0.15` toward `-0.10`. Preserve a distinctly hot/dry Desert window.
This is an experiment: wider warm eligibility also affects the amount of Savanna.
Do not blindly lower every humidity boundary; that would worsen humid dominance.

Use the following ecological ordering to guide the selection table, not as a second
implementation of the rules:

| Climate | Drier side | Intermediate | Wetter/wooded side |
| --- | --- | --- | --- |
| Cold lowland | Snowy Plains; rare Ice Spikes | Snowy Plains | Snowy Taiga |
| Cool lowland | Open temperate transition where needed | Taiga transition | Taiga and old-growth variants |
| Mild lowland | Plains / Sunflower Plains | Plains, Flower/Birch Forest transitions | Forest / Dark Forest |
| Warm lowland | Savanna | Sparse Jungle transition | Jungle / Bamboo Jungle |
| Hot and dry | Desert | Savanna toward less arid climate | Warm wooded family as moisture rises |

The table is constrained by actual biome values. For example, Plains is not cool
according to the output bins; measure that effect. Never call Sparse Jungle
numerically moderate in humidity: its actual registered downfall is high.

For highlands, retain true mountain eligibility and the existing local elevation
offset. Ordinary hills can use suitable lowland forests/plains. Restrict Meadow to
a deliberate open upland niche rather than a large fallback interval. Allow wooded
lower uplands, including warm wooded terrain; high altitude alone must not force
warm/humid hills into Savanna Plateau. Reserve bare peak rules for real mountains.

Preserve rare Cherry Grove as a small temperate/moist upland window. Its current
window overlaps the candidate warm boundary: resolve precedence deliberately, or
move the narrow window within the final mild band. Do not accidentally remove it
or make it common. Retain all 51 supported non-river overworld biomes and cave
eligibility; audit Lush/Dripstone coverage after removing altitude drying.

### 4. Coherent transitions and local variation

Retain continuous existing climate fields and their small local overlays. Adjacent
climate intervals should normally lead through compatible intermediate vegetation:
open ground to sparse trees to forest, temperate forest toward taiga and snow, and
dry warm ground through Savanna/Sparse Jungle toward Jungle. A pointwise selection
cannot eliminate every contact, especially where terrain and several boundaries
meet; evaluate frequencies and visible severity rather than asserting zero contacts.

Do not introduce per-column RNG, checkerboard biome mixing, a new microclimate field,
or the short-scale block noise as the main climate selector. Retain the existing
forest overlay's intentional local variation. Its hard trigger is not being smoothed
away in this pass. Retain the bounded six-block elevation-band variation.

Initially preserve regional climate scales and weights. If access measurements show
that common climate families still require long journeys, propose a second bounded
adjustment to final-biome regional/area weighting. That would require reusing the
already sampled field values while leaving relief's broad climate contract intact;
do not resample or change mountain inputs silently. Do not add this complexity before
the first palette/cooling result is measured and inspected.

## Diagnostics and acceptance evidence

Extend the existing opt-in `BiomeClimateSurvey`; do not build another viewer or a
parallel terrain implementation. Keep the baseline eight seeds and coordinates.
Save the baseline JSON before a run overwrites its output. Report:

- Existing area shares, per-seed ranges and precipitation eligibility.
- Joint output temperature/downfall counts, retaining original bin boundaries.
- Per-biome shares, with rare destinations listed individually.
- Shares for wooded biome families by climate and terrain stratum. Define the ID
  set explicitly; include appropriate tree-bearing Savanna/Grove variants and do
  not present these proxies as measured tree density or usable wood inventories.
- Cave coverage changes and the already supported ocean climate progression.

Add only a bounded, opt-in local-access diagnostic if needed to quantify the stated
gameplay objective. Suggested reproducible sample: 16 land origins per seed, chosen
independently of the resulting biome, and a 16-block grid within 1,024 blocks.
Report the fraction with a sampled wooded biome within 160, 320 and 640 blocks, and
common climate-family availability within 512 and 1,024 blocks. Report unresolved
searches explicitly. These are sampled straight-line proximity estimates, not
walkability, actual visibility or guaranteed nearest-biome distances. Do not report
only successful searches. Never exclude barren starts after seeing their biomes.
The diagnostic may inspect surrounding samples; production generation may not.

Run the same access diagnostic on the preserved before and after revisions. If a
comparable baseline cannot be reconstructed, say so; do not claim wood access was
preserved merely because the overlay arithmetic was retained. Actual decoration,
modded trees, sight lines and traversability require client inspection.

Short default tests should exercise stable contracts through production methods:
finite and monotone cooling; unchanged one-sided forest increment/trigger behavior;
representative new palette boundaries; mountain eligibility; local elevation offsets;
water boundaries; current-run determinism, concurrency and sampling-path agreement.
Extract small pure climate helpers if needed for direct tests rather than copying
the formulas into tests. Update obsolete expected palette examples deliberately.
Do not add fixed seed-coordinate snapshots or distribution quotas to normal builds.

## Implementation sequence and file ownership

| Step | Files / evidence | Completion condition |
| --- | --- | --- |
| 1. Capture current baseline | Existing survey, benchmark and ignored build outputs | Current revision/settings recorded; before JSON/logs preserved |
| 2. Cooling and humidity separation | `ClimateRules.java`, focused core tests | Pointwise contracts hold; forest rule retained; short checks pass |
| 3. Palette and elevation niches | `BiomePalette.java`, `BiomeRegressionTest.java` | Explicit coherent rules; wood-friendly niches; rare/cave/ocean eligibility reviewed |
| 4. Calibrate in small batches | `BiomeClimateSurvey.java`; constants beside owning rules | Joint output evidence supports changes; unmet targets/trade-offs documented |
| 5. Verify and benchmark | Core checks, extended checks, real server fresh/reload | Determinism/coverage pass; allocation and CPU comparison reviewed |
| 6. Document and hand off | `biomes.md`, `architecture.md`, `climate-distribution.md`, `performance.md` | Before/after results and client inspection guidance complete |

Implementation stays primarily in the two existing core rule components. Avoid new
buffer fields unless a real production consumer requires them. No new dependencies,
version adapters, global mutable state or instrumentation in production. Inspect
the working tree and preserve unrelated changes before beginning.

Use Java 21 and the existing workflow:

```powershell
.\gradlew.bat :terrain-core:benchmark :terrain-core:componentBenchmark
.\gradlew.bat runSmokeServer -PclimateSurvey -PsmokeDirectory=build/climate-survey
.\gradlew.bat :terrain-core:check build
.\gradlew.bat :terrain-core:extendedCheck
.\tools\verify.ps1 -Smoke -Benchmark -JavaHome 'C:\Program Files\Java\jdk-21'
```

Capture warmed before/after benchmarks on identical seeds, coordinates, JVM settings
and workload sizes, including mixed and mountain-heavy sampling and column-plus-biome
selection. Reused paths should remain allocation-free. Investigate a repeatable CPU
regression around 5% or more; this is an investigation trigger, not a noisy wall-clock
build gate. Retain detailed profiling as opt-in. Verify diagnostic classes remain
absent from the release JAR. Preserve existing local build-copy settings and secrets.

The final implementation report must distinguish measured developer-runtime climate
from unverified modpack visuals. Supply before/after distributions and performance,
explain any target deviation, and identify client checks: nearby wood, coherent cold/
warm transitions, varied hills, plausible snow lines and continued rare destinations.
Changes affect newly generated chunks; old/new biome boundaries are not blended by
this work. Do not delete user worlds or regions as part of implementation.
