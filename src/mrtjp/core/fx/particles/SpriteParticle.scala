/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.fx.particles

import codechicken.lib.render.CCRenderState
import mrtjp.core.fx._
import net.minecraft.client.particle.EntityFX
import net.minecraft.client.renderer.Tessellator
import net.minecraft.world.World

class SpriteParticle(w:World) extends CoreParticle(w) with TColourParticle with TAlphaParticle with TPositionedParticle with TTextureParticle with TScalableParticle
{
    override def renderParticle(t:Tessellator, frame:Float, cosyaw:Float, cospitch:Float, sinyaw:Float, sinsinpitch:Float, cossinpitch:Float)
    {
        super.renderParticle(t, frame, cosyaw, cospitch, sinyaw, sinsinpitch, cossinpitch)

        t.draw()

        t.startDrawingQuads()
        CCRenderState.changeTexture(texture)

        val f11 = (prevPosX+(posX-prevPosX)*frame-EntityFX.interpPosX).toFloat
        val f12 = (prevPosY+(posY-prevPosY)*frame-EntityFX.interpPosY).toFloat
        val f13 = (prevPosZ+(posZ-prevPosZ)*frame-EntityFX.interpPosZ).toFloat

        t.setBrightness(251658480)
        t.setColorRGBA_F(red.toFloat, green.toFloat, blue.toFloat, alpha.toFloat)

        val min_u = 0
        val min_v = 0
        val max_u = 1
        val max_v = 1

        t.addVertexWithUV(f11-cosyaw*scaleX-sinsinpitch*scaleX, f12-cospitch*scaleY, f13-sinyaw*scaleZ-cossinpitch*scaleZ, max_u, max_v)
        t.addVertexWithUV(f11-cosyaw*scaleX+sinsinpitch*scaleX, f12+cospitch*scaleY, f13-sinyaw*scaleZ+cossinpitch*scaleZ, max_u, min_v)
        t.addVertexWithUV(f11+cosyaw*scaleX+sinsinpitch*scaleX, f12+cospitch*scaleY, f13+sinyaw*scaleZ+cossinpitch*scaleZ, min_u, min_v)
        t.addVertexWithUV(f11+cosyaw*scaleX-sinsinpitch*scaleX, f12-cospitch*scaleY, f13+sinyaw*scaleZ-cossinpitch*scaleZ, min_u, max_v)
        t.draw()

        t.startDrawingQuads()
    }
}