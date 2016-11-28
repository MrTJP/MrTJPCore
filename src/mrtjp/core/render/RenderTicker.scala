/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.render

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase

object RenderTicker
{
    var renderTime = 0
    var renderFrame = 0.0f

    @SubscribeEvent
    def clientTick(event:TickEvent.ClientTickEvent)
    {
        if (event.phase == Phase.END) renderTime += 1
    }

    @SubscribeEvent
    def renderTick(event:TickEvent.RenderTickEvent)
    {
        if (event.phase == Phase.START) renderFrame = event.renderTickTime
    }

    def getRenderFrame = renderFrame
    def getRenderTime = renderTime+renderFrame
}
