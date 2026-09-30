# Architektur und Terrain-Vertrag

## Aufteilung

`terrain-core` ist ein Java-21-Modul ohne externe Abhängigkeiten. `TerrainSampler` und
`BiomePalette` enthalten die Generierungsregeln. `TerrainColumn` und `TerrainChunk` sind
wiederverwendbare, aufrufereigene Ergebnispuffer; `TerrainSample` bleibt als unveränderlicher
Snapshot verfügbar. Der Compiler dieses Moduls kennt keine Minecraft- oder NeoForge-Klassen.

`NewDawnBiomeSource` übersetzt die Auswahl in Vanilla-Biome-Holder aus der Registry.
`NewDawnChunkGenerator` erweitert die vorhandene `NoiseBasedChunkGenerator`-Pipeline und
ersetzt Biombefüllung, Rohterrain, Oberfläche und Höhenabfragen. Die Vererbung ist hier
auch funktional wichtig: Minecraft erzeugt für Noise-Generatoren den `RandomState` aus
den zugehörigen Noise-Settings. Carver, Strukturen, Features und Mob-Generierung bleiben
in der vorhandenen Pipeline. Die Carver-Anbindung übergibt zusätzlich eine an das eigene
Relief gebundene Wassergrenze; siehe [biomes.md](biomes.md).

Nur `NewDawn` registriert die Codecs über NeoForge. Es gibt keine Mixins, Reflection,
Access Transformer, globalen World-Cache oder Versionsadapter-Hierarchie.

## Standard-Welttyp und Vanilla-Auswahl

Minecraft 1.21.1 verwendet beim Öffnen einer neuen Welt `minecraft:normal`. Derselbe
Schlüssel ist der Standardwert für `level-type` auf dedizierten Servern. Die Datei
`data/minecraft/worldgen/world_preset/normal.json` hinterlegt dort deshalb den
New-Dawn-Generator. `newdawn:new_dawn` bleibt als expliziter, kompatibler Schlüssel erhalten.

Das unveränderte Vanilla-Preset liegt zusätzlich unter `newdawn:vanilla` und wird dem
normalen Auswahl-Tag hinzugefügt. Der Standard heißt im Menü „New Dawn“, die Alternative
„Vanilla“. Der ursprüngliche New-Dawn-Schlüssel wird nicht zusätzlich im Menü aufgeführt,
damit kein doppelter Eintrag entsteht. Andere Vanilla-Welttypen bleiben erhalten.

Diese Änderung betrifft Presets für neue Welten. Bereits gespeicherte Dimensionen
behalten ihren Generator. Nether und Ende verwenden in beiden Presets Vanilla.
Datapacks oder Mods, die ebenfalls `minecraft:normal` ersetzen, konkurrieren um
denselben Eintrag; dann entscheidet die Datenpaket-Priorität. Es wird kein globaler
Generator zur Laufzeit ausgetauscht und keine Client-API benötigt.

## Erhaltener Algorithmus

- `java.util.Random(seed)` initialisiert wie bisher 1024 Zufallsbytes für das Simplex-Feld.
  Dieses historische Feld ist **keine** gewöhnliche 256er-Permutation. Ein Austausch
  gegen eine andere Simplex-Bibliothek würde andere Welten erzeugen.
- Alle 13 allgemeinen Noise-Felder und anschließend vier Berg-Felder werden in derselben
  Reihenfolge initialisiert. Selbst das Filler-Feld beeinflusst durch seinen Verbrauch von
  Zufallszahlen die späteren Klima- und Berg-Felder.
- Skalen, Offsets, Gewichte, Rechenreihenfolge, Rundung, Höhenbegrenzung und Float-Konversion
  entsprechen dem Original. Der Faktor `BLOCK_SCALE=2` gehört zur Geländeform und wird
  deshalb nicht aus der heutigen Bauhöhe abgeleitet.
- Ground Level bleibt 64: Y=63 ist der oberste Wasserblock; eine Terrainhöhe von 64
  bezeichnet den ersten freien Block über festem Boden bei Y=63.
- Temperatur- und Feuchtigkeitsfelder einschließlich Höhenabsenkung und Waldinseln bleiben
  erhalten. Die Biomauswahl wurde auf alle 51 Nicht-Fluss-Biome der heutigen Oberwelt
  erweitert; ihre Klima-, Höhen- und Tiefenregeln stehen in [biomes.md](biomes.md).
- Die Bergform wurde absichtlich nicht neu entworfen. Es gibt weiterhin keine Flusslogik.
  Ebenso bleiben die horizontalen Skalen erhalten, die den ursprünglichen Terrainwechsel
  innerhalb der üblichen Sichtweite bestimmen.

30.720 Referenzpunkte aus dem separat kompilierten Originalcode sichern diese Regeln ab.
Das Originalprojekt wird weder verändert noch beim normalen Build benötigt.

## Seed und Nebenläufigkeit

Minecraft ruft `createState` bei der Einrichtung der Chunk-Pipeline mit dem Weltseed auf.
Dort wird die BiomeSource einmalig initialisiert; ihr unveränderlicher Sampler wird über
ein `volatile`-Feld sicher veröffentlicht. Eine spätere Initialisierung mit anderem Seed
schlägt ausdrücklich fehl. Bei einem Neustart dekodiert Minecraft den Generator und bindet
erneut den gespeicherten Weltseed. Kein hart codierter Seed wird im Preset gespeichert.

Der Kern benötigt beim Sampling weder Locks noch RNG-Aufrufe. `sampleInto` und
`sampleChunkInto` schreiben ohne Allokationen in exklusive Puffer des Aufrufers;
`sampleHeight` überspringt Klima und Filler. Die bisherigen Snapshot-Methoden bleiben
verfügbar. Höhenabhängige Klimakorrekturen werden einmal für alle 255 möglichen Höhen
mit der ursprünglichen Formel vorberechnet. Die BiomeSource besitzt je Worker einen auf 256 Einträge
begrenzten Cache mit vollständigen Koordinatenschlüsseln; Kollisionen verändern keine
Ergebnisse. Er vermeidet wiederholte Klimaberechnung für vertikale Biome-Quarts und berechnet
bei isolierten Carver-Abfragen nur die tatsächlich abgefragte Spalte. Cache-Misses verwenden
einen eigenen `TerrainColumn` pro Worker.

Die Liste möglicher Biome wird nach Registry-ID sortiert. Das ist Teil des deterministischen
Weltvertrags: Minecraft weist Features in Biome-Reihenfolge Indizes zu und verwendet diese
als Teil der Zufallsseeds. Die nicht zugesicherte Iterationsreihenfolge von `Map.copyOf`
würde sonst trotz identischem Weltseed nach einem JVM-Neustart andere Dekoration erzeugen.
Der Servertest sichert diese Reihenfolge ausdrücklich ab. Kleine Unterschiede bei der
vollständigen Minecraft-Dekoration sind nach der abgestimmten Anforderung zulässig;
der eigene Terrain-/Klimakern bleibt deterministisch.

Minecraft verwaltet die Chunk-Tasks. Der Mod startet keine zusätzliche Thread-Pipeline.
Ein Task schreibt nur seinen eigenen Chunk. Sein primitiver Chunk-Puffer wird pro Aufruf
angelegt; er verwendet keinen gemeinsamen Pool und keinen wiederverwendeten ThreadLocal-
Chunk-Puffer, der bei verschachtelten Aufrufen überschrieben werden könnte.

Die Rohbefüllung bestimmt Materialintervalle einmal pro Spalte. Vollständig gleichförmige
Gesteins-Sections oberhalb der Bedrock-Schicht und unterhalb aller Filler-Anfänge werden
als kompakte Eintrag-Paletten angelegt. Der öffentliche `LevelChunkSection`-Konstruktor
berechnet ihre Blockzähler; vorhandene Biome-Container bleiben erhalten. Die exklusiv
besessenen Sections werden vor den verbleibenden Einzelblock-Schreibzugriffen gesperrt.
Leere Sections oberhalb des Geländes bleiben unberührt. Höhenkarten und Base-Column-Abfragen
verwenden dieselben Materialgrenzen. Die direkte Höhenabfrage benötigt nur das Höhenfeld,
weil alle aktuellen Palettenmaterialien von sämtlichen Vanilla-Höhenprädikaten erfasst
werden; die Behandlung von Wasser hängt vom angefragten Heightmap-Typ ab.
Biome-Dekoration und andere Minecraft-/Fremdmod-Aufrufe werden nicht eigenständig parallelisiert.

API-Beispiele, Messmethodik und Ergebnisse stehen in [performance.md](performance.md).

## Bewusste Unterschiede und Kompatibilitätsgrenzen

- Die Oberfläche entspricht den alten Höhen; darunter reicht die Welt jetzt bis Y=−64.
  Unter Y=0 liegt Deepslate, bei Y=−64 eine geschlossene Bedrock-Schicht. Der unregelmäßige
  alte Bedrock-Bereich bei Y=0 entfällt. Oben gilt die normale Baugrenze 320; das eigentliche
  Legacy-Terrain bleibt wie früher auf Höhen 1–255 begrenzt.
- Minecraft speichert Biome heute auf einem Quart-Raster mit geglätteter Abfrage. Die
  Oberflächenmaterialien werden weiterhin blockgenau aus dem ursprünglichen Klima gewählt.
  Biome, Features, Bäume, Wetter und Höhlen sind daher keine blockidentische 1.7.10-Kopie.
- `BiomePalette` wählt heutige Vanilla-Biome anhand der alten Terrain-/Klimafelder und
  ergänzt tiefenabhängige Höhlenbiome. Historische Biomnamen in den Original-Fixtures
  werden nicht mehr als erwartete moderne Auswahl geprüft.
- Normale Vanilla-Biome-Holder behalten NeoForge-Biome-Modifier und deren Features.
  Eigene Mod-Biome werden nicht automatisch in die Auswahl aufgenommen. Die alte
  Thaumcraft-Anbindung und das alte Forge-Erweiterungs-API werden nicht mitportiert.
- Die normalen Carver und Aquifere werden genutzt; die große moderne 3D-Noise-Höhlenpassage
  und Noise-Erzadern aus `fillFromNoise` werden nicht ausgeführt. Reguläre Erz-Features laufen.
  Unterirdische Aquifere verwenden moderne Vanilla-Noise-Felder. Im ursprünglichen
  Oberflächenwasserbereich hat die eigene Terrainhöhe Vorrang, damit Carver dort kein
  Wasser aufgrund der abweichenden Vanilla-Oberflächenschätzung durch Luft ersetzen.
- Oberflächen werden in einem Durchgang aus der Legacy-Palette aufgebaut. Änderungen
  ausschließlich an Vanilla-Surface-Rules verändern diese Oberfläche nicht. Moderne
  Struktur-Terrainanpassung durch die Vanilla-Beardifier-Dichte ist ebenfalls nicht Teil
  dieses Höhenfeldes; ein generiertes Dorf ist geprüft, die Einbettung aller Strukturtypen
  und Fremdmods ist keine zugesicherte Eigenschaft.
- Alte Chunk-Blending-Daten werden nicht ausgewertet. Der Port ist für neue Welten gedacht.
  Beliebige Mods, die den Noise-Router oder die komplette Terrainpipeline ersetzen, brauchen
  eine eigene Kompatibilitätsprüfung. Vanilla-Biome sind eine gute Integrationsbasis,
  aber keine pauschale Garantie für alle Mods.

## Künftige Versionen

Innerhalb Minecraft 1.21.1 wird NeoForge über `neo_version` in `gradle.properties` gewählt.
Der Kern bleibt unverändert. Bei einer neuen Minecraft-Version sind Loader-Metadaten,
Pack-Format, Preset-/Noise-Settings-Daten und die wenigen direkten Minecraft-Schnittstellen
zu prüfen; danach Build, Referenztests und Serverprüfung ausführen.

Eine unveränderte Binärdatei für unbekannte zukünftige Minecraft-Versionen wird nicht
versprochen. Die Architektur begrenzt den Anpassungsaufwand auf die Spielanbindung,
ohne vorsorglich für jede Version eine eigene Abstraktionsschicht anzulegen.
