/*
 */
package two.newdawn.commands;

import java.util.LinkedList;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.command.SyntaxErrorException;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S26PacketMapChunkBulk;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.ChunkCoordIntPair;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import two.newdawn.util.SpiralPatternGenerator;

/**
 * @author Two
 */
public class CommandGenerateWorld extends CommandBase {

  @Override
  public String getCommandName() {
    return "generateWorld";
  }

  @Override
  public int getRequiredPermissionLevel() {
    return 3; // OP
  }

  @Override
  public String getCommandUsage(ICommandSender icommandsender) {
    return "[OP-only] /generateWorld <chunkRadius|0-100> - generates <chunkRadius> blocks";
  }

  @Override
  public void processCommand(final ICommandSender icommandsender, final String[] params) {
    final EntityPlayerMP player = getCommandSenderAsPlayer(icommandsender);
    if (params.length != 1) {
      throw new SyntaxErrorException();
    }
    final int rangeMax = parseInt(icommandsender, params[0]);
    if ((rangeMax < 0) || (rangeMax > 100)) {
      throw new SyntaxErrorException();
    }

    final int chunksMax = (int) Math.pow(rangeMax * 2 + 1, 2) - 1;
    final World world = player.getEntityWorld();
    final SpiralPatternGenerator spiralPattern = new SpiralPatternGenerator(new ChunkCoordIntPair(player.chunkCoordX, player.chunkCoordZ), rangeMax);
    int count = 0;

    player.addChatMessage(new ChatComponentText("Generating " + chunksMax + " chunks..."));
    final LinkedList<Chunk> chunks = new LinkedList<Chunk>();
    for (ChunkCoordIntPair coord : spiralPattern) {
      chunks.add(world.getChunkFromChunkCoords(coord.chunkXPos, coord.chunkZPos));
      ++count;
      if (count % 50 == 0) {
        player.addChatMessage(new ChatComponentText(count + " chunks generated..."));
        player.playerNetServerHandler.sendPacket(new S26PacketMapChunkBulk(chunks));
        chunks.clear();
      }
    }
    player.addChatMessage(new ChatComponentText(count + " chunks have been generated."));
  }
}
