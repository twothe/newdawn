# Attribution

New Dawn is developed by Two. This version continues the terrain algorithm from
the project's Forge 1.7.10 version.

The Simplex implementation is based on code released into the public domain by
Stefan Gustavson, with optimizations by Peter Eastman. The original attribution is
preserved in `SimplexNoise.java`. New Dawn uses the mod's historical modified
1,024-entry table.

The noise settings are based on the overworld data shipped with Minecraft 1.21.1;
the sea level has been set to the legacy value of 64. These data belong to the
Minecraft integration, not the independent terrain core.

The Gradle Wrapper files come from the NeoForge MDK; their existing copyright and
Apache 2.0 notices are preserved. New project-specific code currently uses the
"All Rights Reserved" license declared in the mod metadata.
