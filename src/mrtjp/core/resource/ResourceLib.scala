/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.resource

import net.minecraft.util.ResourceLocation

object ResourceLib
{
    val guiSlot = registerCore("textures/gui/slot.png")
    val guiExtras = registerCore("textures/gui/guiextras.png")
    val guiTex = register("textures/gui/widgets.png")

    val soundButton = register("gui.button.press")

    def register(path:String) = new ResourceAction(new ResourceLocation(path))
    def registerCore(path:String) = new ResourceAction(new ResourceLocation("mrtjpcore", path))
}
