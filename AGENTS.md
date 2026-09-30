# Behaviour

- User communication and project documentation are German; source code and tool output are English.
- Keep the mod version at 1.0.0 until the final release unless the user explicitly changes this decision.
- Read `docs/architecture.md` before changing generation. Preserve the legacy noise initialization order and arithmetic; golden fixtures guard this contract.
- Minor generation/detail variations are accepted by the user. Preserve terrain character, performance and thread safety; exact full-world decoration equality is not an acceptance criterion.
- Keep possible-biome traversal sorted: Minecraft uses feature encounter indices in decoration seeds.
- Modern biome selection intentionally differs from legacy biome fixtures. Preserve terrain/climate fixtures; verify biome coverage and water boundaries using `docs/biomes.md`.
- Keep `terrain-core` independent of Minecraft, NeoForge and mutable global state. Minecraft integration belongs in the main project.
- Use caller-owned terrain buffers in hot paths. Never share mutable buffers across concurrent or reentrant generation calls. See `docs/performance.md` for benchmarks and ownership contracts.
- Use Java 21. Run `./gradlew :terrain-core:check build` and the dedicated server smoke check for generation/lifecycle changes.
- Do not modify the original Forge project. Do not introduce per-version adapter hierarchies or reflection for speculative compatibility.

# Project Overview

New Dawn ports Stefan Feldbinder's Forge 1.7.10 terrain generator to NeoForge / Minecraft 1.21.1. Vanilla biome registry entries retain normal decoration, structures and mod biome modifiers.

# Documentation Index

- `README.md`: build, installation and world creation.
- `docs/architecture.md`: terrain fidelity, integration boundary, concurrency and limitations.
- `docs/verification.md`: regression and runtime evidence.
- `docs/performance.md`: reusable sampling APIs, allocation budgets and measured optimization results.
- `docs/biomes.md`: climate/elevation/depth selection, all non-river overworld biomes and ocean-hole regression.

# Glossary

- Terrain height: first non-solid block Y, before decoration and carving.
- Ground level: legacy first air block above sea water, Y=64.
- Terrain core: immutable seed-specific Java sampler, independent of the game.
