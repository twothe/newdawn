# Behaviour

- Communicate with the user in German in this chat. Use English for all project documentation, end-user communication, user-facing text, source code and tool output.
- Credit the project author as Two; do not include the author's real name in project files or user-facing metadata.
- Keep the mod version at 1.0.0 until the final release unless the user explicitly changes this decision.
- Read `docs/architecture.md` before changing generation. The legacy noise initialization and base-terrain arithmetic are the current starting point, not a permanent visual compatibility contract. See `docs/mountains.md` for the current mountain design.
- Minor generation/detail variations are accepted by the user. Preserve terrain character, performance and thread safety; exact full-world decoration equality is not an acceptance criterion.
- Keep possible-biome traversal sorted: Minecraft uses feature encounter indices in decoration seeds.
- Terrain shape and climate may evolve during visual tuning. Do not add fixed seed/coordinate terrain snapshots or exact comparisons against the Forge 1.7.10 generator. Verify current-run determinism, sampling-path agreement, biome coverage and water boundaries using `docs/biomes.md`.
- Keep upland biome thresholds and raw top-material selection on the same sampled column; sea-level decisions use physical terrain height. The existing block noise supplies a bounded local threshold offset.
- Keep `terrain-core` independent of Minecraft, NeoForge and mutable global state. Minecraft integration belongs in the main project.
- Use caller-owned terrain buffers in hot paths. Never share mutable buffers across concurrent or reentrant generation calls. See `docs/performance.md` for benchmarks and ownership contracts.
- For generation changes, capture warmed CPU and allocation benchmarks before and after with identical seeds, coordinates and JVM settings. Include mountain-focused queries as well as mixed terrain; investigate material regressions before completion. Use the server raw-fill benchmark when generation integration is affected.
- Keep production sources free of test entry points, timing and allocation instrumentation. Core tests live in `terrain-core/src/test`, opt-in benchmarks in `terrain-core/src/benchmark`, and game checks in `src/integrationTest`.
- Normal builds run short contract/allocation checks. Use `:terrain-core:extendedCheck` for broad surveys and `tools/verify.ps1 -Smoke -Benchmark` for integration/performance verification. Judge visual quality in the Minecraft client with the intended modpack; do not build a standalone terrain viewer.
- Keep generation components directly composable and easy to edit. Put tuning controls next to the rules they govern; avoid rigid stage frameworks or speculative extension layers.
- Preserve the one-sided forest humidity overlay: nearby wooded areas serve visual variety and wood availability. Rebalance climate selection around this intentional gameplay bias; do not remove or neutralize it to fit statistics.
- When running Gradle from the Codex sandbox, pass `--no-daemon` (or `tools/verify.ps1 -NoDaemon`) and override test installation to a writable project-local directory. A reused sandbox daemon cannot write to external Minecraft instances when later invoked by VS Code. Do not change the user's private install setting.
- Use Java 21. Run `./gradlew :terrain-core:check build` and the dedicated server smoke check for generation/lifecycle changes.
- Do not modify the original Forge project. Do not introduce per-version adapter hierarchies or reflection for speculative compatibility.

# Project Overview

New Dawn is a terrain generation mod for NeoForge / Minecraft 1.21.1, continuing the project's Forge 1.7.10 version. Vanilla biome registry entries retain their identities, structures and mod biome modifiers. Scattered trees in open biomes use forest patches; Savanna uses dry/wooded humidity bands, both with Vanilla biome identity, only in New Dawn worlds; see `docs/biomes.md`.

# Documentation Index

- `README.md`: build, installation and world creation.
- `docs/architecture.md`: terrain fidelity, integration boundary, concurrency and limitations.
- `docs/verification.md`: regression and runtime evidence.
- `docs/performance.md`: reusable sampling APIs, allocation budgets and measured optimization results.
- `docs/mountains.md`: multifractal mountain profiles, climate-shaped cliffs and client inspection.
- `docs/biomes.md`: climate/elevation/depth selection, all non-river overworld biomes and ocean-hole regression.
- `docs/climate-distribution.md`: game-resolved climate statistics, translation effects and the opt-in repeatable survey.
- `docs/climate-rebalancing-plan.md`: implemented design for balanced climate availability, preserved forest patches, coherent transitions and measured verification; final values/results are in the biome and distribution docs.

# Glossary

- Terrain height: first non-solid block Y, before decoration and carving.
- Ground level: legacy first air block above sea water, Y=64.
- Terrain core: immutable seed-specific Java sampler, independent of the game.
