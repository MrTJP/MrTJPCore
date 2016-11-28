/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.data

import codechicken.lib.packet.PacketCustom
import mrtjp.core.handler.MrTJPCoreSPH
import net.minecraft.client.Minecraft
import net.minecraft.client.settings.KeyBinding
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.client.registry.ClientRegistry
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

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
            if (Minecraft.getMinecraft.getConnection != null)
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
        MinecraftForge.EVENT_BUS.register(this)
        this match
        {
            case kb:KeyBinding => ClientRegistry.registerKeyBinding(kb)
            case _ =>
        }
    }
}