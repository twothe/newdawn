# Biome, Höhenlagen und Wassergrenze

New Dawn verwendet alle **51 Vanilla-Oberweltbiome von Minecraft 1.21.1 außer River und
Frozen River**. Die Auswahl ist deterministisch und verwendet die vorhandenen Temperatur-,
Feuchtigkeits-, Höhen-, Regionalhöhen- und Bergwerte. Es gibt keine zusätzlichen Noise-
Abfragen für die Auswahl und keine Zufallsentscheidung je Block. Nether und Ende bleiben Vanilla.

## Klimatische Verteilung

Die Werte sind dimensionslose Noise-Werte, keine Grad-Celsius-Angaben. Temperatur und
Feuchtigkeit enthalten weiterhin die ursprüngliche höhenabhängige Korrektur.

| Bereich | Auswahl |
| --- | --- |
| Ozean: Temperatur ≤ −0,5 | Frozen Ocean / Deep Frozen Ocean |
| Ozean: −0,5 < Temperatur < −0,2 | Cold Ocean / Deep Cold Ocean |
| Ozean: −0,2 ≤ Temperatur < 0,25 | Ocean / Deep Ocean |
| Ozean: 0,25 ≤ Temperatur < 0,55 | Lukewarm Ocean / Deep Lukewarm Ocean |
| Ozean: Temperatur ≥ 0,55 | Warm Ocean; Vanilla besitzt kein Deep Warm Ocean |
| Kaltes Land | Snowy Plains, Snowy Taiga; bei sehr trockener Kälte Ice Spikes |
| Kühles Land | Plains, Taiga, Old Growth Pine/Spruce Taiga mit zunehmender Feuchtigkeit |
| Gemäßigtes Land | Sunflower Plains, Plains, Flower Forest, Birch Forest, Old Growth Birch Forest, Forest, Dark Forest entlang der Feuchtigkeitskurve |
| Warmes/heißes Land | Trockene Savannen/Wüsten, Sparse Jungle, Jungle und feuchter Bamboo Jungle |
| Feuchtes Tiefland auf Höhe 64–68 | Swamp, bei Temperatur ≥ 0,4 Mangrove Swamp |
| Seltene gemäßigte, sehr feuchte Landflächen in tiefen regionalen Becken | Mushroom Fields |

Ozeanbiome gelten unter Terrainhöhe 63; unter Höhe 60 wird die tiefe Variante gewählt.
Das folgt der relativ flachen alten Höhenlandschaft: „Deep“ ist eine relative Einordnung
und erzeugt keine zusätzliche Vertiefung. Höhe 63/64 bildet den Strandbereich; tiefe
Feuchtgebiete und seltene Pilzflächen können diesen auf Land überlagern. Niedrige
Bergflanken bis Höhe 68 erhalten Stony Shore.

Oberflächen passen zum Biom: unter anderem Sand in warmen Ozeanen, Kies in kühleren,
Myzel auf Pilzflächen, Podzol in alten Taigawäldern, Schlamm in Mangrovengebieten,
roter Sand/Terrakotta in Badlands sowie Schnee-/Eisblöcke in kalten Höhenbiomen.

## Höhenbiome

Die Schwellen passen zum bestehenden Terrain, dessen interessante Berge deutlich unter
den hohen Vanilla-Gipfeln liegen. Die Geländeform selbst wird nicht geändert.

- Ab Höhe 82, beziehungsweise ab 76 bei vorhandener Bergmarkierung, gilt die Höhenauswahl.
- Kalte, feuchte mittlere Höhen tragen Grove; trockenere oder höhere kalte Lagen Snowy Slopes.
- Gemäßigte Höhen tragen Meadow oder bei passender Wärme/Feuchtigkeit Cherry Grove.
  Trockene und sehr feuchte Varianten werden Windswept Hills, Gravelly Hills oder Forest.
- Warme Höhen werden je nach Feuchtigkeit Badlands, Eroded/Wooded Badlands,
  Windswept Savanna oder Savanna Plateau.
- Ab Höhe 104 liegen die Gipfelbiome: wärmeres Stony Peaks, ansonsten trockenes Frozen
  Peaks beziehungsweise feuchteres Jagged Peaks.

Die Namen bedeuten keine neue Bergform: Gipfel bleiben durch das bisherige Höhenfeld
geformt. Flussbiome fehlen bewusst, solange kein tatsächliches Flusssystem existiert.

## Höhlenbiome ohne zusätzliche 3D-Noises

Oberhalb der Höhlengrenze bleibt das Oberflächenbiom erhalten. Die Grenze liegt bei
`min(40, Terrainhöhe − 20)`: Mindestens 20 Blöcke Gestein trennen die Höhlenbiome von
der Oberfläche, auch unter tiefen Ozeanen. Die Minecraft-Quart-Auflösung und die normale
Biome-Abfrageglättung gelten weiterhin.

- Feuchte, nicht zu kalte Spalten erhalten Lush Caves.
- Mäßig trockene bis mäßig feuchte, nicht zu kalte Spalten erhalten Dripstone Caves.
- Unter bergigem Terrain ab Höhe 92 liegt bis Y=−24 Deep Dark; darüber kann ein anderes
  Höhlenbiom liegen. In den übrigen Spalten bleibt bei ungeeignetem Klima das Oberflächenbiom.

Der Worker-Cache speichert pro horizontaler Position das Oberflächenbiom, den
Höhlenkandidaten, die Höhlengrenze und die Deep-Dark-Markierung. Vertikale Abfragen
benötigen dadurch nur Grenzvergleiche. Der Cache bleibt auf 256 Positionen pro Worker
begrenzt; Mutable-Sample-Objekte werden nicht pro Position angelegt.

Die registrierten Vanilla-Features laufen auch für die Höhlenbiome. Der Mod verwendet
weiterhin Carver-Höhlen; die großen modernen 3D-Noise-Höhlen werden dadurch nicht ergänzt.

## Ursache und Korrektur der Wasserlöcher

Im gemeldeten Seed `-8458999313514431577` im Bereich X=−2584, Z=−601 wurde der Fehler
nachgestellt. Das Rohterrain war vollständig mit Wasser gefüllt. Minecrafts Carver dürfen
laut Vanilla-Tag auch Wasser ersetzen. Die verwendeten Vanilla-Aquifere bestimmen ihren
Flüssigkeitsstand anhand einer Vanilla-Oberflächenschätzung, die nicht zum New-Dawn-Relief
passt, und lieferten dort Luft. Im Chunk [−162, −38] entstanden allein durch diese Stufe
71 Luftblöcke. Nach kompletter Generierung blieben im geprüften Bereich von 81 Chunks
402 Luftblöcke innerhalb der ursprünglich gefüllten Wassersäulen.

`TerrainCarvers` führt dieselben registrierten Carver mit derselben Start-Chunk-Reihenfolge
und Seed-Ableitung aus. Ein aufrufereigener Aquifer-Wrapper erhält die ursprüngliche
Wasserfüllung zwischen Terrainhöhe und Y=63. Außerhalb dieses Bereichs entscheidet
weiterhin der normale Aquifer. Weder alle unterirdischen Höhlen noch andere Welttypen
werden pauschal geflutet. Die Übergabe erfolgt vor dem Carving; es gibt keinen nachträglichen
Reparaturscan über generierte Weltblöcke.

Mit der Korrektur enthält derselbe Bereich nach kompletter Generierung und Neustart
**0 statt 402 Luftblöcke** in der ursprünglichen Wasserfüllung. Ein Grenztest prüft zusätzlich
Wassererhalt, positives Density-Verhalten und unveränderte Entscheidungen unter dem
Meeresboden und oberhalb des Meeresspiegels.

## Bestehende Welten und Prüfung

Diese Änderungen gelten für **neu generierte Chunks**. Bereits gespeicherte Biome und
Wasserlöcher werden nicht automatisch umgeschrieben. An Grenzen zu älteren Chunks können
Biome und Oberflächenmaterialien wechseln; das zugrunde liegende Höhenfeld bleibt gleich.
Der zur Diagnose gelesene Spielstand wurde nicht verändert.

```powershell
.\tools\verify.ps1 -Smoke -Benchmark -Offline -Seed -8458999313514431577 -JavaHome 'C:\Program Files\Java\jdk-21'
```

`terrain-core:biomeTest` prüft feste Klima-/Höhengrenzen, die vollständige 51er-Auswahl und
ihre Erreichbarkeit in 1.048.576 echten Terrainspalten über vier Seeds. Die ursprünglichen
30.720 Terrain-/Klima-Fixtures bleiben unverändert; deren alte Biomnamen sind historische
Referenzdaten, keine Vorgabe für die neue Auswahl. Die Serverprüfung enthält den
Wasserloch-Reproduktionsbereich auf dem gemeldeten Seed sowie parallele vertikale Cache-
Abfragen und Höhengrenzen für alle verwendeten Oberflächenmaterialien.
