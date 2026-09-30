"""Capture golden data by compiling the original generator method and selectors with game-only stubs.

This tool is intentionally separate from regression tests: it reads the original checkout and never
imports the port. The generated oracle sources remain under build/, not in the shipping project.
"""
from pathlib import Path
import argparse
import hashlib
import re
import subprocess

parser = argparse.ArgumentParser()
parser.add_argument("legacy", type=Path)
parser.add_argument("--jdk", type=Path, required=True)
args = parser.parse_args()
root = Path(__file__).resolve().parent.parent
original = args.legacy / "src/main/java"
output = root / "build/legacy-oracle"
sources = output / "src"


def write(name, text):
    path = sources / name
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text, encoding="utf-8")


copied = ["API/ChunkInformation.java", "API/WorldBaseValues.java", "API/NewDawnBiomeSelector.java",
          "API/noise/SimplexNoise.java", "API/noise/NoiseStretch.java"]
copied += ["worldgen/vanilla/Vanilla" + name + "Selector.java" for name in ["Mountain", "Beach", "Ocean", "Base"]]
texts = {}
for name in copied:
    content = (original / "two/newdawn" / name).read_text(encoding="utf-8")
    texts[name] = content
    write("two/newdawn/" + name, content)

generator = (original / "two/newdawn/worldgen/NewDawnTerrainGenerator.java").read_text(encoding="utf-8")
method = generator[generator.index("  protected ChunkInformation generateChunkInformation("):generator.index("  protected void generateNewDawnTerrain(")]
initialization = generator[generator.index("    this.baseValues ="):generator.index("    biomeSelectors =")]
initialization = initialization.replace("world.provider.getAverageGroundLevel()", "64")
fields = generator[generator.index("  protected final Random seedRandom;"):generator.index("//  private final TimeCounter")]
fields = fields.replace("  protected final List<NewDawnBiomeSelector> biomeSelectors;\n", "")
write("two/newdawn/worldgen/LegacyOracle.java", """
package two.newdawn.worldgen;
import java.util.*;
import two.newdawn.API.*;
import two.newdawn.API.noise.*;
import two.newdawn.worldgen.vanilla.*;
public final class LegacyOracle {
""" + fields + "\npublic LegacyOracle(long worldSeed) {\n" + initialization + """
terrainModifiers = List.of(new VanillaMountainSelector(worldNoise, 0));
}
""" + method + """
public static void main(String[] args) {
 System.out.println("seed,x,z,height,region,mountain,temperatureBits,humidityBits,fillerDepth,biome");
 for (long seed : new long[]{0L,1L,-1L,123456789L,Long.MIN_VALUE,Long.MAX_VALUE}) {
  LegacyOracle oracle = new LegacyOracle(seed);
  List<NewDawnBiomeSelector> selectors = List.of(oracle.terrainModifiers.getFirst(),
    new VanillaBeachSelector(oracle.worldNoise,0), new VanillaOceanSelector(oracle.worldNoise,0),
    new VanillaBaseSelector(oracle.worldNoise,0));
  Random locations = new Random(448811L);
  for (int location=0; location<1040; location++) {
   int cx,cz;
   if (location<16) { cx=location-8; cz=7-location; }
   else if (location<20) { cx=location%2==0 ? -1874999 : 1874998; cz=-cx; }
   else { cx=locations.nextInt(2048)-1024; cz=locations.nextInt(2048)-1024; }
   ChunkInformation info=oracle.generateChunkInformation(cx,cz);
   int count=location<16 ? 256 : 1;
   for (int index=0;index<count;index++) {
    int x=cx*16+(index&15),z=cz*16+(index>>4),i=ChunkInformation.blockToChunk(x,z);
    NewDawnBiome selected=null;
    for (NewDawnBiomeSelector selector:selectors) {
     selected=selector.selectBiome(x,z,info); if(selected!=null) break;
    }
    int filler=(int)Math.round((oracle.fillerNoise.getNoise(x,z)+1.0)*1.5*ChunkInformation.BLOCK_SCALE);
    System.out.println(seed+","+x+","+z+","+info.height[i]+","+info.regionHeight[i]+","+info.isMountain[i]
     +","+Float.floatToIntBits(info.temperature[i])+","+Float.floatToIntBits(info.humidity[i])+","+filler+","+selected.vanillaBiome.name);
   }
  }
 }
}
}
""")
biome_names = sorted(set(re.findall(r"BiomeGenBase\.(\w+)", "\n".join(texts.values()))))
block_names = sorted(set(re.findall(r"Blocks\.(\w+)", "\n".join(texts.values()))))
write("net/minecraft/world/biome/BiomeGenBase.java", """
package net.minecraft.world.biome;
public final class BiomeGenBase {
 public final String name;
 private BiomeGenBase(String name) { this.name=name; }
""" + "\n".join(f'public static final BiomeGenBase {name}=new BiomeGenBase("{name}");' for name in biome_names) + "\n}")
write("net/minecraft/init/Blocks.java", "package net.minecraft.init; public final class Blocks {\n" +
      "\n".join(f'public static final Object {name}=new Object();' for name in block_names) + "\n}")
write("two/newdawn/API/NewDawnBiome.java", """
package two.newdawn.API;
import net.minecraft.world.biome.BiomeGenBase;
public final class NewDawnBiome {
 public final BiomeGenBase vanillaBiome;
 public NewDawnBiome(BiomeGenBase biome,Object top,Object filler) { vanillaBiome=biome; }
 public static NewDawnBiome copyVanilla(BiomeGenBase biome) { return new NewDawnBiome(biome,null,null); }
}
""")
classes = output / "classes"
classes.mkdir(parents=True, exist_ok=True)
subprocess.run([str(args.jdk / "bin/javac.exe"), "-encoding", "UTF-8", "-d", str(classes),
                *map(str, sources.rglob("*.java"))], check=True)
fixture = root / "terrain-core/src/test/resources/legacy-terrain.csv"
fixture.parent.mkdir(parents=True, exist_ok=True)
with fixture.open("w", encoding="utf-8", newline="") as handle:
    subprocess.run([str(args.jdk / "bin/java.exe"), "-cp", str(classes), "two.newdawn.worldgen.LegacyOracle"], stdout=handle, check=True)
manifest = {name: hashlib.sha256((original / "two/newdawn" / name).read_bytes()).hexdigest()
            for name in [*copied, "worldgen/NewDawnTerrainGenerator.java"]}
fixture.with_suffix(".provenance.txt").write_text(
    "Original source SHA-256 (fixture captured without loading port code):\n" +
    "\n".join(f"{digest}  {name}" for name, digest in manifest.items()) + "\n", encoding="utf-8")
print(f"Captured {len(fixture.read_text().splitlines()) - 1} legacy samples: {fixture}")
