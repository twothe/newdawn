package two.newdawn.util;

import cpw.mods.fml.common.registry.GameData;
import net.minecraft.block.Block;
import net.minecraft.item.Item;

public class Utils {

  public static String getClassName(final Object o) {
    return o == null ? "null" : o.getClass().getName();
  }

  public static String getSimpleClassName(final Object o) {
    return o == null ? "null" : o.getClass().getSimpleName();
  }

  public static String blockToString(final Block block) {
    if (block == null) {
      return "null";
    }
    final String blockIDName = GameData.getBlockRegistry().getNameForObject(block);
    return blockIDName == null ? block.getUnlocalizedName() : blockIDName;
  }

  public static String itemToString(final Item item) {
    if (item == null) {
      return "null";
    } else {
      final String itemIDName = GameData.getItemRegistry().getNameForObject(item);
      return itemIDName == null ? item.getUnlocalizedName() : itemIDName;
    }
  }

  public static int withinBounds(final int value, final int lowerBound, final int upperBound) {
    if (value < lowerBound) {
      return lowerBound;
    } else if (value > upperBound) {
      return upperBound;
    } else {
      return value;
    }
  }
}
