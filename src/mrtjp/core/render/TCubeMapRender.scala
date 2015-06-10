/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.render

import codechicken.lib.render._
import codechicken.lib.render.uv.{IconTransformation, UVTransformation}
import codechicken.lib.vec.{Cuboid6, Rotation, Translation, Vector3}
import mrtjp.core.block.TInstancedBlockRender
import net.minecraft.client.renderer.RenderBlocks
import net.minecraft.util.IIcon
import net.minecraft.world.IBlockAccess

object TCubeMapRender
{
    val models =
    {
        val array = Array.ofDim[CCModel](6, 4)
        val box = CCModel.quadModel(24).generateBlock(0, Cuboid6.full)
        for (s <- 0 until 6) for (r <- 0 until 4)
        {
            val m = box.copy.apply(Rotation.sideOrientation(s, r).at(Vector3.center))
            m.computeNormals()
            array(s)(r) = m
        }
        array
    }

    val invTranslation = new Translation(-0.5, -0.5, -0.5)
}

trait TCubeMapRender extends TInstancedBlockRender
{
    import mrtjp.core.render.TCubeMapRender._

    override def renderWorldBlock(r:RenderBlocks, w:IBlockAccess, x:Int, y:Int, z:Int, meta:Int)
    {
        val (s, rot, icon) = getData(w, x, y, z)
        TextureUtils.bindAtlas(0)
        CCRenderState.reset()
        CCRenderState.lightMatrix.locate(w, x, y, z)
        models(s)(rot).render(new Translation(x, y, z), icon, CCRenderState.lightMatrix)
    }

    override def renderBreaking(w:IBlockAccess, x:Int, y:Int, z:Int, icon:IIcon)
    {
        val b = w.getBlock(x, y, z)
        CCRenderState.reset()
        CCRenderState.setPipeline(new Translation(x, y, z), new IconTransformation(icon))
        BlockRenderer.renderCuboid(new Cuboid6(b.getBlockBoundsMinX, b.getBlockBoundsMinY, b.getBlockBoundsMinZ,
            b.getBlockBoundsMaxX, b.getBlockBoundsMaxY, b.getBlockBoundsMaxZ), 0)
    }

    override def renderInvBlock(r:RenderBlocks, meta:Int)
    {
        val (s, r, icon) = getInvData

        TextureUtils.bindAtlas(0)
        CCRenderState.reset()
        CCRenderState.setDynamic()
        CCRenderState.pullLightmap()
        CCRenderState.startDrawing()
        models(s)(r).render(invTranslation, icon)
        CCRenderState.draw()
    }

    def getData(w:IBlockAccess, x:Int, y:Int, z:Int):(Int, Int, UVTransformation)
    def getInvData:(Int, Int, UVTransformation)
}