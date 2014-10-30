/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.resource

import net.minecraft.client.Minecraft
import net.minecraft.client.audio.PositionedSoundRecord
import net.minecraft.util.ResourceLocation

class ResourceAction(loc:ResourceLocation)
{
    def mc = Minecraft.getMinecraft

    def bind(){mc.renderEngine.bindTexture(loc)}

    def play(){play(1.0F)}
    def play(volume:Float){mc.getSoundHandler.playSound(PositionedSoundRecord.func_147674_a(loc, volume))}
}