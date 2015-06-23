/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.fx.particles

import codechicken.lib.render.CCRenderState
import net.minecraft.client.Minecraft
import net.minecraft.client.particle.EntityFX
import net.minecraft.client.renderer.Tessellator
import net.minecraft.util.MathHelper
import net.minecraft.world.World
import org.lwjgl.opengl.GL11._

class BeamPulse(world:World) extends CoreParticle(world)
{
    var particle:Int = 16

    private var tX = 0.0D
    private var tY = 0.0D
    private var tZ = 0.0D

    private var ptX = 0.0D
    private var ptY = 0.0D
    private var ptZ = 0.0D
    private var length = 0.0F
    private var opacity = 0.3F
    private var prevSize = 0.0F

    particleRed = 0.5F
    particleGreen = 0.5F
    particleBlue = 0.5F

    setSize(0.02F, 0.02F)

    def updateBeam(xx:Double, yy:Double, zz:Double, x:Double, y:Double, z:Double)
    {
        setPosition(xx, yy, zz)
        tX = x
        tY = y
        tZ = z
        while(particleMaxAge-particleAge < 4) particleMaxAge += 1
    }

    override def onUpdate()
    {
        super.onUpdate()

        ptX = tX
        ptY = tY
        ptZ = tZ

        val xd = (posX - tX).toFloat
        val yd = (posY - tY).toFloat
        val zd = (posZ - tZ).toFloat
        length = MathHelper.sqrt_float(xd * xd + yd * yd + zd * zd)
        val var7 = MathHelper.sqrt_double(xd * xd + zd * zd)
        rotationYaw = (Math.atan2(xd, zd) * 180.0D / 3.141592653589793D).toFloat
        rotationPitch = (Math.atan2(yd, var7) * 180.0D / 3.141592653589793D).toFloat

        if(opacity > 0.3F) opacity -= 0.025F
        if(opacity < 0.3F) opacity = 0.3F
    }

    def setRGB(r:Float, g:Float, b:Float)
    {
        particleRed = r
        particleGreen = g
        particleBlue = b
    }

    def setPulse(pulse:Boolean, r:Float, g:Float, b:Float)
    {
        setRGB(r, g, b)
        if(pulse) opacity = 0.8F
    }

    override def renderParticle(tess:Tessellator, f:Float, f1:Float, f2:Float, f3:Float, f4:Float, f5:Float)
    {
        tess.draw()

        glPushMatrix()

        val var9 = 1.0F
        val slide = Minecraft.getMinecraft.thePlayer.ticksExisted
        val size = 0.7F

        CCRenderState.changeTexture("mrtjpcore:textures/particles/beam1.png")

        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, 10497.0F)
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, 10497.0F)
        glDisable(GL_CULL_FACE)

        val var11 = slide + f
        val var12 = -var11 * 0.2F - MathHelper.floor_float(-var11 * 0.1F)

        glBlendFunc(GL_SRC_ALPHA, GL_ONE)
        glDepthMask(false)

        val xx = (prevPosX + (posX - prevPosX) * f - EntityFX.interpPosX).toFloat
        val yy = (prevPosY + (posY - prevPosY) * f - EntityFX.interpPosY).toFloat
        val zz = (prevPosZ + (posZ - prevPosZ) * f - EntityFX.interpPosZ).toFloat
        glTranslated(xx, yy, zz)

        val ry = prevRotationYaw + (rotationYaw - prevRotationYaw) * f
        val rp = prevRotationPitch + (rotationPitch - prevRotationPitch) * f

        glRotatef(90.0F, 1.0F, 0.0F, 0.0F)
        glRotatef(180.0F + ry, 0.0F, 0.0F, -1.0F)
        glRotatef(rp, 1.0F, 0.0F, 0.0F)
        val var44 = -0.15D * size
        val var17 = 0.15D * size

        for (t <- 0 until 2)
        {
            val var29 = length * var9
            val var31 = 0.0D
            val var33 = 1.0D
            val var35 = -1.0F + var12 + t / 3.0F
            val var37 = length * var9 + var35

            glRotatef(90.0F, 0.0F, 1.0F, 0.0F)
            tess.startDrawingQuads()
            tess.setBrightness(200)
            tess.setColorRGBA_F(particleRed, particleGreen, particleBlue, opacity)
            tess.addVertexWithUV(var44, var29, 0.0D, var33, var37)
            tess.addVertexWithUV(var44, 0.0D, 0.0D, var33, var35)
            tess.addVertexWithUV(var17, 0.0D, 0.0D, var31, var35)
            tess.addVertexWithUV(var17, var29, 0.0D, var31, var37)
            tess.draw
        }

        glColor4f(1.0F, 1.0F, 1.0F, 1.0F)
        glDepthMask(true)
        glBlendFunc(GL_SRC_ALPHA, 771)
        glEnable(GL_CULL_FACE)
        glPopMatrix()

        renderFlare(tess, f, f1, f2, f3, f4, f5)

        tess.startDrawingQuads()
        prevSize = size
    }

    def renderFlare(tess:Tessellator, f:Float, f1:Float, f2:Float, f3:Float, f4:Float, f5:Float)
    {
        glPushMatrix()
        glDepthMask(false)
        glBlendFunc(GL_SRC_ALPHA, GL_ONE)
        glColor4f(1.0F, 1.0F, 1.0F, 0.66F)

        CCRenderState.changeTexture("mrtjpcore:textures/particles/beamflare.png")

        val part = particleAge%16

        val var8 = part/16.0
        val var9 = var8+0.0624375
        val var10 = 0.0
        val var11 = var10+0.0624375
        val var12 = 0.66*opacity
        val var13 = (ptX + (tX - ptX) * f - EntityFX.interpPosX).toFloat
        val var14 = (ptY + (tY - ptY) * f - EntityFX.interpPosY).toFloat
        val var15 = (ptZ + (tZ - ptZ) * f - EntityFX.interpPosZ).toFloat

        tess.startDrawingQuads()
        tess.setBrightness(200)
        tess.setColorRGBA_F(particleRed, particleGreen, particleBlue, opacity)
        tess.addVertexWithUV(var13 - f1 * var12 - f4 * var12, var14 - f2 * var12, var15 - f3 * var12 - f5 * var12, var9, var11)
        tess.addVertexWithUV(var13 - f1 * var12 + f4 * var12, var14 + f2 * var12, var15 - f3 * var12 + f5 * var12, var9, var10)
        tess.addVertexWithUV(var13 + f1 * var12 + f4 * var12, var14 + f2 * var12, var15 + f3 * var12 + f5 * var12, var8, var10)
        tess.addVertexWithUV(var13 + f1 * var12 - f4 * var12, var14 - f2 * var12, var15 + f3 * var12 - f5 * var12, var8, var11)
        tess.draw()

        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA)
        glDepthMask(true)
        glPopMatrix()
    }
}