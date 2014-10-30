/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.handler

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.event.{FMLInitializationEvent, FMLPostInitializationEvent, FMLPreInitializationEvent}

@Mod(modid = "MrTJPCoreMod", useMetadata = true, modLanguage = "scala")
object MrTJPCoreMod
{
    @Mod.EventHandler
    def preInit(event:FMLPreInitializationEvent)
    {
        MrTJPCoreProxy.preInit()
    }

    @Mod.EventHandler
    def init(event:FMLInitializationEvent)
    {
        MrTJPCoreProxy.init()
    }

    @Mod.EventHandler
    def postInit(event:FMLPostInitializationEvent)
    {
        MrTJPCoreProxy.postInit()
    }
}
