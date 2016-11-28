/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.gui

import codechicken.lib.gui.GuiDraw
import codechicken.lib.render.CCRenderState
import codechicken.lib.texture.TextureUtils
import net.minecraft.client.renderer.GlStateManager._
import net.minecraft.client.renderer.vertex.DefaultVertexFormats
import net.minecraft.util.ResourceLocation
import org.lwjgl.opengl.GL11

object GuiLib
{
    val guiSlot = new ResourceLocation("mrtjpcore", "textures/gui/slot.png")
    val guiExtras = new ResourceLocation("mrtjpcore", "textures/gui/guiextras.png")
    val guiTex = new ResourceLocation("minecraft", "textures/gui/widgets.png")

    /**
     *
     * @param x x pos of grid
     * @param y y pos of grid
     * @param w width of grid
     * @param h height of grid
     * @param dx x spacing of slots (0 means touching like in inventories)
     * @param dy y spacing of slots (0 means touching like in inventories)
     * @return Sequence of x and
     */
    def createSlotGrid(x:Int, y:Int, w:Int, h:Int, dx:Int, dy:Int):Seq[(Int, Int)] =
        createGrid(x, y, w, h, dx+18, dy+18)

    def createGrid(x:Int, y:Int, w:Int, h:Int, dx:Int, dy:Int) =
    {
        var grid = Seq[(Int, Int)]()
        for (iy <- 0 until h) for (ix <- 0 until w)
            grid :+= ((x+ix*dx) -> (y+iy*dy))
        grid
    }

    def drawPlayerInvBackground(x:Int, y:Int)
    {
        for ((x, y) <- createSlotGrid(x, y, 9, 3, 0, 0))
            drawSlotBackground(x-1, y-1)
        for ((x, y) <- createSlotGrid(x, y+58, 9, 1, 0, 0))
            drawSlotBackground(x-1, y-1)
    }

    def drawSlotBackground(x:Int, y:Int)
    {
        color(1, 1, 1, 1)

        TextureUtils.changeTexture(guiSlot)

        val rs = CCRenderState.instance()
        val vb = rs.startDrawing(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX)
        vb.pos(x, y+18, 0).tex(0, 1).endVertex()
        vb.pos(x+18, y+18, 0).tex(1, 1).endVertex()
        vb.pos(x+18, y, 0).tex(1, 0).endVertex()
        vb.pos(x, y, 0).tex(0, 0).endVertex()
        rs.draw()
    }

    def drawGuiBox(x:Int, y:Int, width:Int, height:Int, zLevel:Float)
    {
        drawGuiBox(x, y, width, height, zLevel, true, true, true, true)
    }

    def drawGuiBox(x:Int, y:Int, width:Int, height:Int, zLevel:Float, top:Boolean, left:Boolean, bottom:Boolean, right:Boolean)
    {
        val u = 1
        val v = 29

        TextureUtils.changeTexture(guiExtras)

        GuiDraw.gui.setZLevel(zLevel)
        color(1, 1, 1, 1)
        pushMatrix()
        translate(x+2, y+2, 0)
        scale(width-4, height-4, 0)
        GuiDraw.drawTexturedModalRect(0, 0, u+19, v, 1, 1)
        popMatrix()
        if (top)
        {
            pushMatrix()
            translate(x+3, y, 0)
            scale(width-6, 1, 0)
            GuiDraw.drawTexturedModalRect(0, 0, u+4, v, 1, 3)
            popMatrix()
        }
        if (bottom)
        {
            pushMatrix()
            translate(x+3, y+height-3, 0)
            scale(width-6, 1, 0)
            GuiDraw.drawTexturedModalRect(0, 0, u+14, v, 1, 3)
            popMatrix()
        }
        if (left)
        {
            pushMatrix()
            translate(x, y+3, 0)
            scale(1, height-6, 0)
            GuiDraw.drawTexturedModalRect(0, 0, u, v+4, 3, 1)
            popMatrix()
        }
        if (right)
        {
            pushMatrix()
            translate(x+width-3, y+3, 0)
            scale(1, height-6, 0)
            GuiDraw.drawTexturedModalRect(0, 0, u+8, v, 3, 1)
            popMatrix()
        }

        if (top && left) GuiDraw.drawTexturedModalRect(x, y, u, v, 4, 4)
        if (top && right) GuiDraw.drawTexturedModalRect(x+width-3, y, u+5, v, 3, 3)
        if (bottom && left) GuiDraw.drawTexturedModalRect(x, y+height-3, u+11, v, 3, 3)
        if (bottom && right) GuiDraw.drawTexturedModalRect(x+width-4, y+height-4, u+15, v, 4, 4)
    }

    def drawVerticalTank(x:Int, y:Int, u:Int, v:Int, w:Int, h:Int, prog:Int)
    {
        GuiDraw.drawTexturedModalRect(x, y+h-prog, u, v+h-prog, w, prog)
    }
}