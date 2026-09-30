# New Dawn – NeoForge 1.21.1

Port des ursprünglichen Forge-1.7.10-Terraingenerators von Stefan Feldbinder (Two).
Die Simplex-Überlagerung, Höhen, Berge und Klimawerte bleiben erhalten. Ein unabhängiger
Java-Kern berechnet das Terrain; Minecraft übernimmt die übliche Weiterverarbeitung mit
Vanilla-Biomen, Höhlen-Carvern, Dekoration, Erzen, Strukturen und Tieren.
Alle 51 Oberweltbiome außer den beiden Flussbiomen werden passend zu Klima, Höhenlage
und Tiefe ausgewählt. Geänderte Biome und die Korrektur der Ozean-Wasserlöcher gelten
für neu generierte Chunks; vorhandene Weltbereiche werden nicht automatisch umgeschrieben.

## Bauen und starten

Voraussetzung: JDK 21. Der Gradle Wrapper ist enthalten. Beim ersten Build werden
Gradle und die Minecraft-/NeoForge-Abhängigkeiten heruntergeladen.

```powershell
# Nur für diese PowerShell-Sitzung, falls JAVA_HOME auf ein anderes JDK zeigt:
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:GRADLE_USER_HOME = "$PWD\.gradle-user-home"
.\gradlew.bat build
.\gradlew.bat runClient
```

Das fertige Mod-JAR liegt unter `build/libs/newdawn-1.0.0.jar`. Die Mod-Version bleibt
bis zum finalen Release bei **1.0.0**. Den separaten
Terrain-Core-JAR muss man nicht installieren; seine Klassen sind im Mod enthalten.
Zielplattform: **Minecraft 1.21.1, NeoForge 21.1.250 oder neuer innerhalb 21.1.x, Java 21**.
Andere Minecraft-Versionen sind nicht als binär kompatibel deklariert.

Das Buildgerüst orientiert sich am [offiziellen 1.21.1-MDK](https://github.com/NeoForgeMDKs/MDK-1.21.1-ModDevGradle).
Die Integration von Java-Bibliotheken in Entwicklungsstarts folgt
[ModDevGradle](https://github.com/neoforged/ModDevGradle#additional-runtime-dependencies).

## Eine New-Dawn-Welt erstellen

Bei der Welterstellung ist **New Dawn bereits vorausgewählt**. Wer das normale
Minecraft-Terrain möchte, kann auf der Seite „Welt“ den Welttyp **Vanilla** auswählen.
Die anderen Vanilla-Welttypen bleiben ebenfalls verfügbar.

Auch ein neuer dedizierter Server verwendet ohne besondere Konfiguration New Dawn.
`level-type=minecraft:normal` und die ältere Angabe `level-type=default` verwenden
ebenfalls New Dawn. Die explizite Auswahl bleibt möglich:

```properties
level-type=newdawn:new_dawn
level-seed=123456789
```

Für normales Minecraft-Terrain vor dem ersten Start der neuen Serverwelt stattdessen
`level-type=newdawn:vanilla` setzen.

Nether und Ende bleiben Vanilla. Eine bestehende Welt wird durch Änderung von `level-type`
nicht umgestellt. Dies ist kein Konverter für alte 1.7.10-Spielstände.

## Prüfen

```powershell
# Build, Originaldaten-Vergleich und Thread-Tests:
.\tools\verify.ps1 -JavaHome 'C:\Program Files\Java\jdk-21'

# Zusätzlich Servergenerierung, Speichern und Neustart mit identischem gespeichertem Chunk-Inhalt:
.\tools\verify.ps1 -Smoke -JavaHome 'C:\Program Files\Java\jdk-21'

# Reiner Terrain-Benchmark:
.\gradlew.bat :terrain-core:benchmark

# Zusätzlich CPU-/Allokationsmessung für Kern und Minecraft-Rohbefüllung:
.\tools\verify.ps1 -Smoke -Benchmark -JavaHome 'C:\Program Files\Java\jdk-21'
```

`-Offline` verwendet bereits vorhandene Build-Abhängigkeiten. Die Serverprüfung bindet
nur an localhost auf einem freien Port, verwendet eine neue Welt unter `build/` und beendet
sich selbst. Prüfergebnisse und Logs verbleiben dort. Entwicklungsprüfungen werden nicht
in das ausgelieferte Mod-JAR aufgenommen.

Details: [Architektur und Grenzen](docs/architecture.md), [Performance und Puffer-API](docs/performance.md),
[Prüfnachweise](docs/verification.md).
Die Auswahlregeln und der Wasserloch-Regressionstest stehen in [Biome und Wassergrenze](docs/biomes.md).
