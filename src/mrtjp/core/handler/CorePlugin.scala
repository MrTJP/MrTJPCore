/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.handler

import java.util.{Map => JMap}

import codechicken.core.launch.DepLoader
import cpw.mods.fml.relauncher.{IFMLCallHook, IFMLLoadingPlugin}

class CorePlugin extends IFMLLoadingPlugin with IFMLCallHook
{
    override def getASMTransformerClass = Array()

    override def getSetupClass = "mrtjp.core.handler.CorePlugin"

    override def getModContainerClass = null

    override def getAccessTransformerClass = null

    override def injectData(data:JMap[String, AnyRef]){}

    override def call():Void =
    {
        DepLoader.load()
        null
    }
}