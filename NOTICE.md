# Herkunft

Der Terrain-Algorithmus stammt aus Stefan Feldbinders (Two) New-Dawn-Mod für Forge 1.7.10.
Die ursprünglichen Quellen liegen außerhalb dieses Projekts und wurden nicht verändert.

Die Simplex-Implementierung beruht auf dem von Stefan Gustavson als Public Domain
veröffentlichten Code mit Optimierungen von Peter Eastman. Die ursprüngliche Attribution
bleibt in `SimplexNoise.java` erhalten. New Dawn verwendet die historische abgewandelte
1024er-Tabelle dieses Mods.

Die Noise-Settings basieren auf den mit Minecraft 1.21.1 ausgelieferten Overworld-Daten;
der Meeresspiegel wurde auf den Legacy-Wert 64 gesetzt. Die Daten gehören zur Minecraft-
Integration, nicht zum unabhängigen Terrain-Kern.

Die Gradle-Wrapper-Dateien stammen aus dem NeoForge-MDK; ihre vorhandenen Copyright- und
Apache-2.0-Hinweise bleiben erhalten. Für neuen projektspezifischen Code gilt zunächst
die in den Mod-Metadaten angegebene Lizenz „All Rights Reserved“.
