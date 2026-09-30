# Performance und wiederverwendbare Terrain-Puffer

## Ergebnis

Messung vom 30. September 2026 auf dem lokalen Windows-Rechner mit Oracle JDK 21.0.2
und NeoForge 21.1.250. Die Werte sind Mediane aus jeweils drei Messungen nach drei
Warmup-Durchläufen. Byte-Angaben messen neu angelegten Heap-Speicher pro Operation,
nicht den dauerhaft belegten Speicher und nicht die maximale Heap-Größe.

| Operation | Vorher | Nachher | Allokationen vorher → nachher |
| --- | ---: | ---: | ---: |
| Vollständiges Chunk-Sampling, eigenes Ergebnis pro Aufruf | 72,3 µs | 66,9 µs | 11.280 → 5.552 B/Chunk |
| Vollständiges Chunk-Sampling, wiederverwendeter Puffer | – | 68,6 µs | 0 B/Chunk nach Pufferanlage |
| Reine Höhenabfrage | 0,292 µs | 0,128 µs | 40 → 0 B/Abfrage |
| Minecraft-Rohbefüllung | 0,660 ms | 0,329 ms | 33.058 → 13.905 B/Chunk |

Damit benötigt die Rohbefüllung in diesem Vergleich etwa **50 % weniger Zeit** und
**58 % weniger Allokationen**. Der vollständige Terrain-Kern gewinnt vor allem bei
Allokationen; seine CPU-Zeiten liegen weiterhin in ähnlicher Größenordnung.
Wiederverwendung ist keine Zusage für höhere Geschwindigkeit als ein neuer Puffer:
Die beiden Varianten liegen innerhalb der beobachteten Laufzeitschwankungen.

Die Ausgangswerte stehen lokal in `build/performance-baseline.log`, die Kernmessung
nach dem Umbau in `build/performance-core.log`, die Rohfüllungsmessung nach dem Umbau
in `build/performance-bulk.log`. Die abschließende kombinierte Prüfung einschließlich
erneuter Benchmarks steht in `build/performance-verification.log`.
Die beiden abschließenden Serverläufe lagen bei 0,220–0,258 ms und etwa 13,7–13,9 KB
pro Rohfüllung. Unterschiedliche JVM-Aufwärmung und Last erklären mit, warum diese
Werte niedriger liegen; die Tabelle verwendet den konservativeren separaten Vergleichslauf.

## Änderungen

- Sechs primitive Arrays ersetzen 256 Ergebnisobjekte plus Referenzarray. Ein neuer
  `TerrainChunk` benötigt im gemessenen JVM-Layout 5.552 Byte einschließlich Arrays
  und internem Spaltenpuffer. Seine Wiederverwendung allokiert nichts pro Abfrage.
- `sampleHeight` berechnet ausschließlich das Relief. Struktur-Höhenabfragen sparen
  sämtliche Klima-/Filler-Noises und das Abwärtsscannen der Blockspalte.
- Die höhenabhängige Klimakorrektur wird einmal für Höhen 1–255 vorberechnet. Formel,
  Rundung, Noise-Reihenfolge und gemessene Float-Bits bleiben erhalten.
- Cache-Misses der BiomeSource nutzen einen kleinen Spaltenpuffer pro Worker.
- Materialintervalle werden einmal pro Spalte bestimmt. Vollständig gleichförmige
  Stein-/Deepslate-Sections erhalten direkt eine kompakte Palette, statt jeweils
  4.096 Einzelblock-Schreiboperationen auszuführen. Biome-Container bleiben erhalten;
  Minecraft berechnet die Section-Zähler. Übergangsschichten werden blockgenau geschrieben.
- Alte `sample`-/`sampleChunk`-Methoden bleiben für bequeme unveränderliche Ergebnisse
  verfügbar. Minecrafts Generierung verwendet die neuen APIs.

## Biomerweiterung

Nach der Erweiterung auf 51 Biome bleibt auch vollständiges Sampling einschließlich
tiefenabhängiger Biomauswahl allokationsfrei: **0 B/Abfrage**, im letzten Lauf etwa
**0,284–0,295 µs/Abfrage**. Der Serverlauf mit dem Wasserloch-Seed erreichte Medianwerte
von **0,243 bzw. 0,246 ms/Chunk** bei etwa **13,8–13,9 KB/Chunk** für die Rohbefüllung.
Das ist wegen des anderen Seeds kein direkter Zeitvergleich mit der obigen Tabelle.
Die Carver-Korrektur benötigt zusätzlich einen aufrufereigenen Höhenpuffer von 256
Integern und 256 reine Höhenabfragen pro Carving-Durchgang; die Rohfüllungsmessung
enthält diesen separaten Pipeline-Schritt nicht.

## API und Besitzregeln

```java
TerrainSampler sampler = new TerrainSampler(seed); // Share freely between workers.
TerrainColumn column = new TerrainColumn();       // Exclusive to this operation/worker.
sampler.sampleInto(blockX, blockZ, column);
BiomePalette.Entry biome = BiomePalette.select(column);
TerrainSample retained = column.snapshot();       // Allocates only when a snapshot is needed.

TerrainChunk chunk = new TerrainChunk();
sampler.sampleChunkInto(chunkX, chunkZ, chunk);
int height = chunk.height(localX + 16 * localZ);
chunk.copyColumn(localX + 16 * localZ, column);      // Independent reusable destination.
int heightOnly = sampler.sampleHeight(blockX, blockZ);
```

Ein Puffer wird vollständig überschrieben. Er darf während des Schreibens nicht von
anderen Aufrufen oder Threads benutzt werden. Nach sicherer Veröffentlichung sind
gemeinsame Lesezugriffe möglich, solange niemand den Puffer wieder beschreibt.
Uninitialisierte Puffer und ungültige Indizes werden abgewiesen. Chunk-Koordinaten,
deren Blockursprung außerhalb eines `int` liegt, werden vor der Mutation abgewiesen.

Die Minecraft-Integration legt ihren Chunk-Puffer pro Generierungsaufruf an. Dadurch
funktionieren parallele und verschachtelte Aufrufe ohne Pool, globale Sperre oder
gemeinsam überschriebenen Arbeitsspeicher. Der Terrain-Kern bleibt unabhängig von
Minecraft und NeoForge.

## Reproduzieren und Grenzen

```powershell
.\tools\verify.ps1 -Smoke -Benchmark -Offline -JavaHome 'C:\Program Files\Java\jdk-21'
```

Ohne `-Smoke` misst `-Benchmark` nur den Kern. Ohne `-Offline` dürfen fehlende
Build-Abhängigkeiten heruntergeladen werden. Der normale Build enthält bereits den
kleineren Allokations-Regressionscheck; Zeitmessungen haben bewusst keine feste Bestehensgrenze.

Der Kernbenchmark nutzt pro Messung 8.192 Chunks beziehungsweise 2.097.152 Einzelabfragen.
Checksums konsumieren die Ergebnisse; Chunk-Ergebnisse werden zusätzlich außerhalb
der Schleife sichtbar gehalten. Die Allokationszähler stammen vom JVM-ThreadMXBean.
JVMs ohne diese Funktion melden negative Bytewerte bzw. `SKIP` beim Allokationscheck.

Der Minecraft-Benchmark befüllt pro Messung 256 neue ProtoChunks. Konstruktion und
Biomvorbereitung erfolgen vor der Messung. Gemessen wird die synchrone Rohbefüllung
einschließlich Ergebnispuffer, Paletten und Heightmaps. Höhlen, Dekoration, Strukturen,
Lichtberechnung, Speichern und Netzwerk sind nicht enthalten. Daraus folgt **keine
Verdopplung der gesamten Weltgenerierung oder der Client-FPS**. JIT, GC und Rechnerlast
beeinflussen die Zahlen; Langzeitprofiling eines kompletten Modpacks steht noch aus.
