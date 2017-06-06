/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.handler

import codechicken.lib.packet.PacketCustom
import mrtjp.core.world.Messenger
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

class MrTJPCoreProxy_server
{
    def preInit(){}

    def init()
    {
        PacketCustom.assignHandler(MrTJPCoreSPH.channel, MrTJPCoreSPH)
        //SimpleGenHandler.init()
    }

    def postInit(){}
}

class MrTJPCoreProxy_client extends MrTJPCoreProxy_server
{
    @SideOnly(Side.CLIENT)
    override def preInit()
    {
        super.preInit()
    }

    @SideOnly(Side.CLIENT)
    override def init()
    {
        super.init()
        PacketCustom.assignHandler(MrTJPCoreCPH.channel, MrTJPCoreCPH)
    }

    @SideOnly(Side.CLIENT)
    override def postInit()
    {
        MinecraftForge.EVENT_BUS.register(Messenger)
//        MinecraftForge.EVENT_BUS.register(RenderTicker)
    }
}

object MrTJPCoreProxy extends MrTJPCoreProxy_client