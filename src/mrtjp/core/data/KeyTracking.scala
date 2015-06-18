/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.data

import codechicken.lib.packet.PacketCustom
import cpw.mods.fml.client.registry.ClientRegistry
import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.handler.MrTJPCoreSPH
import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.player.EntityPlayer

import scala.collection.mutable.{HashMap => MHashMap, Map => MMap}

object KeyTracking
{
    private var idPool = 0
    private val map = MHashMap[Int, MMap[EntityPlayer, Boolean]]()

    def updatePlayerKey(id:Int, player:EntityPlayer, state:Boolean)
    {
        map(id) += player -> state
    }

    def registerTracker(tracker:TServerKeyTracker)
    {
        tracker.id = idPool
        idPool += 1
        map.getOrElseUpdate(tracker.id,
            MHashMap[EntityPlayer, Boolean]().withDefaultValue(false))
    }

    def isKeyDown(id:Int, player:EntityPlayer) = map(id)(player)
}

trait TServerKeyTracker
{
    var id = -1

    def isKeyDown(p:EntityPlayer) = KeyTracking.isKeyDown(id, p)

    def register()
    {
        KeyTracking.registerTracker(this)
    }
}

trait TClientKeyTracker
{
    private var wasPressed = false

    def getTracker:TServerKeyTracker

    def getIsKeyPressed:Boolean

    @SubscribeEvent
    @SideOnly(Side.CLIENT)
    def tick(event:ClientTickEvent)
    {
        val pressed = getIsKeyPressed
        if(pressed != wasPressed)
        {
            wasPressed = pressed
            if (Minecraft.getMinecraft.getNetHandler != null)
            {
                KeyTracking.updatePlayerKey(getTracker.id, Minecraft.getMinecraft.thePlayer, pressed)
                val packet = new PacketCustom(MrTJPCoreSPH.channel, MrTJPCoreSPH.keyBindPacket)
                packet.writeByte(getTracker.id)
                packet.writeBoolean(pressed)
                packet.sendToServer()
            }
        }
    }

    @SideOnly(Side.CLIENT)
    def register()
    {
        FMLCommonHandler.instance().bus().register(this)
        this match
        {
            case kb:KeyBinding => ClientRegistry.registerKeyBinding(kb)
            case _ =>
        }
    }
}