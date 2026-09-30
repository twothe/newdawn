# Prüfnachweise

Stand: 30. September 2026. Windows 11, Oracle JDK 21.0.2, Gradle 9.2.1,
ModDevGradle 2.0.147, Minecraft 1.21.1, NeoForge 21.1.250.

## Originaltreue und Nebenläufigkeit

`terrain-core:regressionTest` prüft 30.720 gespeicherte Referenzpunkte. Sie wurden durch
`tools/capture-legacy-fixtures.py` aus dem alten Checkout erzeugt: Die ursprüngliche
`generateChunkInformation`-Methode, Noise-Initialisierung, Simplex-Implementierung und
Selektoren werden separat kompiliert. Nur die nicht zur Berechnung gehörenden Minecraft-
Biome-/Blockobjekte werden ersetzt. Der neue Terrain-Code wird dabei nicht geladen.

Die Quellenhashes stehen in `terrain-core/src/test/resources/legacy-terrain.provenance.txt`.
Die Golden-Datei enthält sechs Seeds: 0, 1, −1, 123456789, `Long.MIN_VALUE` und `Long.MAX_VALUE`.
Abgedeckt sind zusammenhängende Chunks um den Ursprung, negative Koordinaten,
zufällige entfernte Gebiete und Positionen nahe der Weltgrenze.

Geprüft werden Höhe, regionale Höhe, Bergmarkierung, exakte Temperatur-/Feuchtigkeitsbits,
Filler-Dicke. Die historischen Biomnamen in der CSV sind seit der Erweiterung der Auswahl
keine erwarteten modernen Biome mehr; die Golden-Datei selbst bleibt unverändert.
Dieselben Terrain-/Klimadaten werden mit acht Threads
gegen gemeinsam verwendete Sampler geprüft. Weitere Assertions betreffen Chunk-Indizierung,
Klimagrenzen, Küsten-/Ozeanpriorität, Seed-Abhängigkeit und Koordinatenüberlauf.
Die Referenzpunkte werden zusätzlich über Einzelpuffer, Chunk-Puffer und reine
Höhenabfragen geprüft. Pufferprüfungen sichern Wiederverwendung, unabhängigen Speicher,
unveränderliche Snapshots sowie ungültige Indizes und Eingaben ab.

`terrain-core:biomeTest` sichert separat die moderne Auswahl ab: 51 Vanilla-Oberweltbiome
ohne Flüsse, feste Klima-/Höhen-/Tiefengrenzen und Erreichbarkeit aller 51 Biome in
1.048.576 echten Terrainspalten über vier Seeds. Details stehen in [biomes.md](biomes.md).

`terrain-core:allocationTest` gehört ebenfalls zu `check`: Nach Warmup dürfen die
wiederverwendeten APIs keine regelmäßigen Ergebnisobjekte anlegen. Kleine Messabweichungen
bis 16 Byte pro Chunk bzw. 1 Byte pro Einzelabfrage werden toleriert; es gibt kein
maschinenabhängiges Zeitlimit. JVMs ohne Thread-Allokationszähler melden ausdrücklich
`SKIP`; auf dem verwendeten JDK wurde der Test ausgeführt und bestanden.

Zum bewusst getrennten Neuerzeugen der Referenzdatei:

```powershell
python tools/capture-legacy-fixtures.py C:/Work/Java/Projekte/mc-forge-1710-newdawn --jdk 'C:/Program Files/Java/jdk-21'
```

Nicht während normaler Regressionstests neu erzeugen; sonst könnten geänderte Erwartungen
einen Fehler verdecken. Änderungen der alten Quellen erfordern eine fachliche Prüfung.

## Minecraft-Integration

`tools/verify.ps1 -Smoke` baut das Mod-JAR und startet zwei echte dedizierte
Entwicklungsserver hintereinander: neue Welt und Neustart dieser Welt. Die frische
`server.properties` enthält absichtlich keinen `level-type`, um die Vorauswahl zu prüfen.
Jede Testwelt liegt in einem eigenen Ordner unter `build/`;
die Server verwenden ausschließlich localhost und beenden sich nach der Prüfung.

Die Prüfung umfasst:

- Registry-/Preset-Laden, richtigen Generator und Bindung des Weltseeds.
- New Dawn über Minecrafts Standard-Preset-Pfad und Server-Konfiguration ohne Welttyp;
  explizites `newdawn:new_dawn` bleibt gültig. Der Menü-Tag enthält Standard und Vanilla.
- Explizites `level-type=newdawn:vanilla` wird durch Minecrafts echte Konfigurationsauflösung
  in einen Vanilla-Noise-Generator mit Vanilla-Noise-Settings und Vanilla-Biom-Preset übersetzt.
- Codec-Roundtrip über Minecraft-Registries und JSON.
- Ablehnung einer erneuten Seed-Bindung mit anderem Seed.
- Stabile Biome-Reihenfolge für die von Minecraft abgeleiteten Feature-Seeds.
- 12.288 parallele Biomanfragen einschließlich Cache-Kollisionen und vertikaler Wiederverwendung
  derselben horizontalen Position über sechs Höhen.
- 1.024 Spalten in vier frischen ProtoChunks: alle 384 Blockpositionen gegen
  `getBaseColumn`, feste/wasserführende Höhen gegen Heightmaps und Struktur-Höhenabfragen.
- Weitere 4.096 Spalten mit vier parallelen Workern und unterschiedlichen Höhenausschnitten:
  Vergleich aller sechs Höhenabfragen mit Minecrafts eigener Heightmap-Neuberechnung,
  Spalten-/Füllkonsistenz, unveränderte Biome-Container sowie Block-/Tickzähler der Sections.
- Explizite erwartete Materialintervalle für Bedrock, Deepslate, Stein, Sandstein,
  Sand, Gras, überfluteten Boden und Wasser; inklusive negativer Y-Werte und Filler-Dicke null.
- 25 vollständig generierte Chunks: vorhandene Erzblöcke und unterirdische Luft aus Carvern;
  Baumblöcke werden ebenfalls erfasst.
- Finden und Generieren eines Dorfes mit gültigem StructureStart.
- Vergleich eines Blockzustands-Checksums nach Speichern/Neustart.
- Aquifer-Grenzvertrag und auf dem gemeldeten Seed 81 vollständig generierte Chunks im
  Wasserloch-Reproduktionsgebiet: 0 Luftblöcke innerhalb der ursprünglichen Wasserfüllung,
  auch nach Neustart; vor der Korrektur waren es 402.

Die abschließende Performance-/Build-/Serverprüfung steht lokal unter
`build/performance-verification.log`; jede Testwelt
enthält zusätzlich `newdawn-smoke-result.txt` und `newdawn-smoke-checksum.txt`.
Die spätere Prüfung der Standardauswahl mit Mod-Version 1.0.0 steht unter
`build/default-preset-verification.log`. Ein grafischer Menütest wurde nicht durchgeführt;
geprüft sind der tatsächlich verwendete Standard-Preset-Pfad, die Auswahlregistrierung
und die Daten der auswählbaren Generatoren.
Die aktuelle vollständige Prüfung der Biomerweiterung und Wasserkorrektur einschließlich
Benchmarks steht in `build/biomes-final-verification.log`; die vorherige Stufendiagnose
mit dem gemeldeten Seed in `build/water-user-seed-before.log`. Die ursprüngliche
Modpack-Welt wurde ausschließlich zum Lesen des Seeds geöffnet, nicht verändert.
Die Initialisierung und Sortierung der Biome sind ausdrücklich getestet, weil reine
Höhenvergleiche Fehler bei Seeds für Dekoration nicht aufdecken würden.

Ein zusätzlicher Diagnosevergleich unabhängiger neuer Welten zeigte kleine Unterschiede
bei der vollständigen Minecraft-Dekoration. Exakte Übereinstimmung dieser Details wurde
vom Auftraggeber ausdrücklich als nicht erforderlich eingeordnet. Dieser strengere
Vergleich ist daher kein Abnahmekriterium und nicht Teil des dauerhaften Prüfbefehls.
Die exakten Kern-Fixtures, nebenläufige Biomanfragen und unveränderte gespeicherte Chunks
bleiben verbindlich geprüft.

## Performance

Die reproduzierbaren Kern- und Rohfüllungsbenchmarks einschließlich Allokationsmessung,
Vorher-/Nachher-Werten und Einschränkungen stehen in [performance.md](performance.md).
Die vier Rohfüllungen im normalen Servertest bleiben ein Funktionstest ohne stabilen
JIT-Warmup; ihre Zeitwerte sind kein belastbarer Durchsatzvergleich.

## Distribution und verbleibende Grenzen

`verifyJar` öffnet das tatsächlich gebaute JAR: Terrain-Klassen, Preset, Menü-Tag und
expandierte Mod-Version müssen enthalten sein; Test-Hooks und Original-Fixtures dürfen
nicht enthalten sein. Der Task ist Teil von `check` und `build`.

Nicht ausgeführt: grafischer Client-/Menütest, Multiplayer-Verbindung, Modpack-Matrix,
alle Strukturtypen, Langzeit-/Speicherprofiling und Builds gegen andere Minecraft-Versionen.
Die Serverprüfung lädt die Entwicklungsartefakte; das verpackte JAR wird auf Inhalt geprüft,
aber nicht zusätzlich in einer separaten produktiven Installation gestartet.

Im Entwicklungslog erscheinen die üblichen Vanilla-Kommandomehrdeutigkeiten sowie
umgebungsbedingte OSHI/WMI-Abfragewarnungen. Sie verhinderten weder Start noch Generierung.
Die ausführlichen Grenzen zu Höhlen, Surface-Rules, Struktur-Terrainanpassung und Biome-Mods
stehen in [architecture.md](architecture.md).
