/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.render

import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent
import cpw.mods.fml.common.gameevent.TickEvent.Phase

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
