/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.handler

import codechicken.lib.packet.PacketCustom.{IClientPacketHandler, IServerPacketHandler}
import codechicken.lib.packet.{ICustomPacketTile, PacketCustom}
import codechicken.lib.vec.BlockCoord
import mrtjp.core.data.KeyTracking
import mrtjp.core.gui.GuiHandler
import mrtjp.core.world.{Messenger, WorldLib}
import net.minecraft.client.Minecraft
import net.minecraft.entity.player.EntityPlayerMP
import net.minecraft.network.play.{INetHandlerPlayClient, INetHandlerPlayServer}
import net.minecraft.world.World

class MrTJPCorePH
{
    val channel = MrTJPCoreMod

    val tilePacket = 1
    val messagePacket = 2
    val guiPacket = 3
    val keyBindPacket = 4

    def handleTilePacket(world:World, packet:PacketCustom, pos:BlockCoord)
    {
        val t = WorldLib.getTileEntity(world, pos, classOf[ICustomPacketTile])
        if (t != null) t.handleDescriptionPacket(packet)
    }
}

object MrTJPCoreCPH extends MrTJPCorePH with IClientPacketHandler
{
    def handlePacket(packet:PacketCustom, mc:Minecraft, nethandler:INetHandlerPlayClient)
    {
        val world = mc.theWorld
        packet.getType match
        {
            case this.tilePacket => handleTilePacket(world, packet, packet.readCoord)
            case this.messagePacket => Messenger.addMessage(packet.readDouble, packet.readDouble, packet.readDouble, packet.readString)
            case this.guiPacket => GuiHandler.receiveGuiPacket(packet)
        }
    }
}

object MrTJPCoreSPH extends MrTJPCorePH with IServerPacketHandler
{
    override def handlePacket(packet:PacketCustom, sender:EntityPlayerMP, nethandler:INetHandlerPlayServer)
    {
        packet.getType match
        {
            case this.tilePacket =>
                handleTilePacket(sender.theItemInWorldManager.theWorld, packet, packet.readCoord())
            case this.keyBindPacket =>
                KeyTracking.updatePlayerKey(packet.readUByte(), sender, packet.readBoolean())
        }
    }
}