/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.handler

import codechicken.lib.packet.PacketCustom
import cpw.mods.fml.client.registry.RenderingRegistry
import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.block.TileRenderRegistry
import mrtjp.core.render.RenderTicker
import mrtjp.core.world.{Messenger, SimpleGenHandler}
import net.minecraftforge.common.MinecraftForge

class MrTJPCoreProxy_server
{
    def preInit(){}

    def init()
    {
        PacketCustom.assignHandler(MrTJPCoreSPH.channel, MrTJPCoreSPH)
        SimpleGenHandler.init()
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

        TileRenderRegistry.renderID = RenderingRegistry.getNextAvailableRenderId
        RenderingRegistry.registerBlockHandler(TileRenderRegistry)
    }

    @SideOnly(Side.CLIENT)
    override def postInit()
    {
        MinecraftForge.EVENT_BUS.register(Messenger)
        FMLCommonHandler.instance.bus.register(RenderTicker)
        MinecraftForge.EVENT_BUS.register(RenderTicker)
    }
}

object MrTJPCoreProxy extends MrTJPCoreProxy_client