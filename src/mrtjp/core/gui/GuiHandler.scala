/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.gui

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.packet.PacketCustom
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.handler.MrTJPCoreSPH
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.{EntityPlayer, EntityPlayerMP}
import net.minecraft.inventory.Container

object GuiHandler
{
    /**
     * Called serverside to open a syncronized container GUI
     * @param player1 Serverside player
     * @param cont Serverside container
     * @param guiID Gui ID defined in GuiIDs object
     * @param dataWrite Partial function that adds data to a packet
     */
    def openSMPContainer(player1:EntityPlayer, cont:Container, guiID:Int, dataWrite:MCDataOutput => Unit)
    {
        if (!player1.isInstanceOf[EntityPlayerMP]) return
        val player = player1.asInstanceOf[EntityPlayerMP]
        player.getNextWindowId()
        player.closeContainer()
        val packet = new PacketCustom(MrTJPCoreSPH.channel, MrTJPCoreSPH.guiPacket)
        dataWrite(packet.writeByte(player.currentWindowId).writeShort(guiID))
        packet.sendToPlayer(player)
        if (cont != null)
        {
            player.openContainer = cont
            player.openContainer.windowId = player.currentWindowId
            player.openContainer.addCraftingToCrafters(player)
        }
    }

    /**
     * Called client side upon receiving a request to open a gui. Server requests client
     * to do so with the above method.
     * @param windowID The window ID the server defined to client
     * @param gui The gui created by the client
     */
    @SideOnly(Side.CLIENT)
    def openSMPContainer(windowID:Int, gui:GuiScreen)
    {
        val mc = Minecraft.getMinecraft
        mc.displayGuiScreen(gui)
        if (windowID != 0) mc.thePlayer.openContainer.windowId = windowID
    }

    /**
     * Internally called by the client when it receives a gui request from the
     * server with openSMPContainer.
     * @param data Raw data built by the server, includes windowID as byte and
     *             guiID as short, as well as custom gui data.
     */
    @SideOnly(Side.CLIENT)
    def receiveGuiPacket(data:MCDataInput)
    {
        val win = data.readUByte()
        val id = data.readUShort()
        val gui = guiMap.get(id) match
        {
            case Some(e) => e.buildGui(Minecraft.getMinecraft.thePlayer, data)
            case None => null
        }
        if (gui != null) openSMPContainer(win, gui)
    }

    private var guiMap = Map[Int, TGuiBuilder]()

    def register(b:TGuiBuilder, id:Int){guiMap += id -> b}
}

trait TGuiBuilder
{
    def getID:Int

    @SideOnly(Side.CLIENT)
    def buildGui(player:EntityPlayer, data:MCDataInput):GuiScreen

    def open(player:EntityPlayer, cont:Container){open(player, cont, {packet => })}
    def open(player:EntityPlayer, cont:Container, dataWrite:MCDataOutput => Unit)
    {
        GuiHandler.openSMPContainer(player, cont, getID, dataWrite)
    }
}