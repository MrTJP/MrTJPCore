/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.world

import java.util.Random

import mrtjp.core.math.MathLib
import net.minecraft.block.Block
import net.minecraft.util.MathHelper
import net.minecraft.world.World
import net.minecraft.world.gen.feature.WorldGenerator

class WorldGenClusterizer extends TWorldGenerator
{
    var cluster = Set[((Block, Int), Int)]()
    var material = Set[(Block, Int)]()
    var clusterSize = 1

    override def generate(w:World, rand:Random, x:Int, y:Int, z:Int) =
        if (clusterSize < 4) generateSmall(w, rand, x, y, z) else generateNormal(w, rand, x, y, z)

    def generateSmall(w:World, rand:Random, x:Int, y:Int, z:Int):Boolean =
    {
        var generated = false
        for (i <- 0 until clusterSize)
        {
            val dx = x+rand.nextInt(2)
            val dy = y+rand.nextInt(2)
            val dz = z+rand.nextInt(2)
            generated |= setBlock(w, dx, dy, dz, cluster, material)
        }
        generated
    }

    def generateNormal(w:World, rand:Random, x:Int, y:Int, z:Int):Boolean =
    {
        val f = rand.nextFloat*Math.PI.toFloat
        val xNDir = x+8+(MathHelper.sin(f)*clusterSize)/8F
        val xPDir = x+8-(MathHelper.sin(f)*clusterSize)/8F
        val zNDir = z+8+(MathHelper.cos(f)*clusterSize)/8F
        val zPDir = z+8-(MathHelper.cos(f)*clusterSize)/8F
        val yNDir = (y+rand.nextInt(3))-2
        val yPDir = (y+rand.nextInt(3))-2

        val dx = xPDir-xNDir
        val dy = yPDir-yNDir
        val dz = zPDir-zNDir

        var generated = false
        for (i <- 0 until clusterSize)
        {
            val xCenter = xNDir+(dx*i)/clusterSize
            val yCenter = yNDir+(dy*i)/clusterSize
            val zCenter = zNDir+(dz*i)/clusterSize

            val size = (rand.nextDouble.toFloat*clusterSize)/16f

            val hMod = ((MathHelper.sin((i*Math.PI.toFloat)/clusterSize)+1f)*size+1f)*0.5f
            val vMod = ((MathHelper.sin((i*Math.PI.toFloat)/clusterSize)+1f)*size+1f)*0.5f

            val x0 = MathHelper.floor_float(xCenter-hMod)
            val y0 = MathHelper.floor_float(yCenter-vMod)
            val z0 = MathHelper.floor_float(zCenter-hMod)

            val x1 = MathHelper.floor_float(xCenter+hMod)
            val y1 = MathHelper.floor_float(yCenter+vMod)
            val z1 = MathHelper.floor_float(zCenter+hMod)

            for (blockX <- x0 to x1)
            {
                var xDistSq = ((blockX+0.5f)-xCenter)/hMod
                xDistSq *= xDistSq
                if (xDistSq < 1f) for (blockY <- y0 to y1)
                {
                    var yDistSq = ((blockY+0.5f)-yCenter)/vMod
                    yDistSq *= yDistSq
                    val xyDistSq = yDistSq+xDistSq
                    if (xyDistSq < 1f) for (blockZ <- z0 to z1)
                    {
                        var zDistSq = ((blockZ+0.5f)-zCenter)/hMod
                        zDistSq *= zDistSq
                        if (zDistSq+xyDistSq < 1f)
                            generated |= setBlock(w, blockX, blockY, blockZ, cluster, material)
                    }
                }
            }
        }
        generated
    }
}