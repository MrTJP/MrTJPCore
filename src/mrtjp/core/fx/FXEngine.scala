/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.fx

import codechicken.lib.render.TextureUtils
import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent
import cpw.mods.fml.common.gameevent.TickEvent.Phase
import mrtjp.core.fx.particles.CoreParticle
import net.minecraft.client.Minecraft
import net.minecraft.client.particle.EntityFX
import net.minecraft.client.renderer.{ActiveRenderInfo, Tessellator}
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.event.world.WorldEvent
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL11._

import scala.collection.mutable.{ListBuffer, Map => MMap}

object FXEngine
{
    val layers = Array(
        MMap[Int, ListBuffer[CoreParticle]](),
        MMap[Int, ListBuffer[CoreParticle]](),
        MMap[Int, ListBuffer[CoreParticle]](),
        MMap[Int, ListBuffer[CoreParticle]]()
    )

    private var registered = false
    def register()
    {
        if (!registered)
        {
            MinecraftForge.EVENT_BUS.register(this)
            FMLCommonHandler.instance().bus().register(this)
        }
        registered = true
    }

    def addEffect(effect:CoreParticle)
    {
        if (!registered)
            throw new RuntimeException("FXEngine has not been registered.")
        val dim = effect.worldObj.provider.dimensionId
        val dimList = layers(effect.getFXLayer).getOrElseUpdate(dim, ListBuffer[CoreParticle]())
        if (dimList.size > 1600) dimList.remove(0)
        dimList += effect
    }

    @SubscribeEvent
    def onRenderWorldLast(event:RenderWorldLastEvent)
    {
        val player = Minecraft.getMinecraft.renderViewEntity
        val partialTicks = event.partialTicks
        val dim = Minecraft.getMinecraft.theWorld.provider.dimensionId

        EntityFX.interpPosX = player.lastTickPosX + (player.posX - player.lastTickPosX) * partialTicks
        EntityFX.interpPosY = player.lastTickPosY + (player.posY - player.lastTickPosY) * partialTicks
        EntityFX.interpPosZ = player.lastTickPosZ + (player.posZ - player.lastTickPosZ) * partialTicks
        TextureUtils.bindAtlas(1)

        renderParticles(dim, partialTicks)
    }

    @SubscribeEvent
    def onWorldUnload(event:WorldEvent.Unload)
    {
        val dim = event.world.provider.dimensionId
        for (map <- layers) if (map.contains(dim))
            map(dim).clear()
    }

    @SubscribeEvent
    def tickEnd(event:TickEvent.ClientTickEvent)
    {
        if(event.phase == Phase.END)
        {
            for (layer <- layers) for (dim <- layer.keys)
            {
                layer(dim) = layer(dim).filterNot { p =>
                    p.onUpdate()
                    p.isDead
                }
            }
        }
    }

    private def renderParticles(dim:Int, frame:Float)
    {
        val f1 = ActiveRenderInfo.rotationX
        val f2 = ActiveRenderInfo.rotationZ
        val f3 = ActiveRenderInfo.rotationYZ
        val f4 = ActiveRenderInfo.rotationXY
        val f5 = ActiveRenderInfo.rotationXZ

        glPushMatrix()
        glColor4f(1.0F, 1.0F, 1.0F, 1.0F)
        glDepthMask(false)
        glEnable(GL_BLEND)
        glAlphaFunc(GL_GREATER, 0.003921569F)
        glDisable(GL_LIGHTING)

        for (layer <- 0 until 4)
        {
            val layermap = layers(layer)
            if (layermap.contains(dim))
            {
                val list = layermap(dim)
                if (list.nonEmpty)
                {
                    glPushMatrix()

                    layer match
                    {
                        case 0 =>
                        case 1 =>
                        case 2 => GL11.glBlendFunc(GL_SRC_ALPHA, GL_ONE)
                        case 3 => GL11.glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
                    }

                    val tess = Tessellator.instance
                    tess.startDrawingQuads()
                    for (particle <- list)
                    {
                        tess.setBrightness(particle.getBrightnessForRender(frame))
                        particle.renderParticle(tess, frame, f1, f5, f2, f3, f4)
                    }
                    tess.draw()

                    glPopMatrix()
                }
            }
        }

        glDisable(GL_BLEND)
        glDepthMask(true)
        glAlphaFunc(GL_GREATER, 0.1F)
        glEnable(GL_LIGHTING)
        glPopMatrix()
    }
}