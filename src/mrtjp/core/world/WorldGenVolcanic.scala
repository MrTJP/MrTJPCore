/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.world

import java.util.Random

import mrtjp.core.math.MathLib
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.world.World

import scala.collection.mutable.{HashMap => MHashMap, Queue => MQueue}

class WorldGenVolcanic extends TWorldGenerator
{
    var ashCluster = Set[((Block, Int), Int)]()
    var conduitCluster = Set[((Block, Int), Int)]()
    var liq:(Block, Int) = null
    var material = Set[(Block, Int)]()
    var materialStart = Set[(Block, Int)]()

    var sizeMin = 32000
    var sizeMax = 64000

    private val stack = MQueue[(Int, Int, Int)]()
    private val test = MHashMap[(Int, Int), Int]()

    override def generate(w:World, rand:Random, x:Int, y:Int, z:Int):Boolean =
    {
        if (!canSetBlock(w, x, y, z, materialStart)) return false
        stack.clear()
        test.clear()

        val swh = WorldLib.findSurfaceHeight(w, x, z)
        var n = swh

        for (i <- y until swh)
        {
            setBlock(w, x, i, z, liq, material)
            setBlock(w, x-1, i, z, conduitCluster, material)
            setBlock(w, x+1, i, z, conduitCluster, material)
            setBlock(w, x, i, z-1, conduitCluster, material)
            setBlock(w, x, i, z+1, conduitCluster, material)
        }

        val head = 3+rand.nextInt(4)
        val spread = rand.nextInt(3)

        var size = MathLib.randomFromIntRange(sizeMin until sizeMax, rand)

        import scala.util.control.Breaks.{break, breakable}
        breakable(while (size > 0)
        {
            while (stack.size == 0)
            {
                setBlock(w, x, n, z, liq, material)
                test.clear()
                enqueueBlocks(x, n, z, head, rand)
                n += 1
                if (n > 125) break()
            }

            val (i, j, k) = stack.dequeue()
            w.getBlock(i, 64, k) //force chunk generation
            if (w.getChunkProvider.chunkExists(i>>4, k>>4) && test.contains((i, k)))
            {
                var pow = test((i, k))
                var hm = w.getHeightValue(i, k)+1
                while (hm > 0 && isUnimportant(w, i, hm-1, k)) hm -= 1

                if (hm <= j) if (isUnimportant(w, i, hm, k))
                {
                    purgeArea(w, i, hm, k)
                    setBlock(w, i, hm, k, ashCluster, material)
                    if (j > hm) pow = math.max(pow, spread)
                    enqueueBlocks(i, hm, k, pow, rand)
                    size -= 1
                }
            }
        })

        setBlock(w, x, n, z, liq, material)
        while (n >= swh && liq._1 == w.getBlock(x, n, z))
        {
            w.markBlockForUpdate(x, n, z)
            w.notifyBlocksOfNeighborChange(x, n, z, liq._1)
            w.notifyBlockOfNeighborChange(x, n, z, liq._1)
            w.scheduledUpdatesAreImmediate = true
            liq._1.updateTick(w, x, n, z, w.rand)
            w.scheduledUpdatesAreImmediate = false
            n -= 1
        }
        true
    }

    private def purgeArea(w:World, x:Int, y:Int, z:Int)
    {
        if (w.isAirBlock(x, y, z)) return
        for (i <- -1 to 1) for (j <- -1 to 1)
        {
            val b = w.getBlock(x+i, y, z+j)
            if ((b == Blocks.snow) || WorldLib.isAssociatedTreeBlock(w, x+i, y, z+j, b))
                w.setBlockToAir(x+i, y, z+j)
        }
        purgeArea(w, x, y+1, z)
    }

    private def enqueueBlocks(x:Int, y:Int, z:Int, p:Int, rand:Random)
    {
        val seed = rand.nextInt(16)
        enq(x-1, y, z, if ((seed&1) != 0) p-1 else p)
        enq(x+1, y, z, if ((seed&2) != 0) p-1 else p)
        enq(x, y, z-1, if ((seed&4) != 0) p-1 else p)
        enq(x, y, z+1, if ((seed&8) != 0) p-1 else p)

        def enq(x:Int, y:Int, z:Int, p:Int)
        {
            if (p > 0)
            {
                val o = test.getOrElse((x, z), -1)
                if (p > o)
                {
                    stack.enqueue((x, y, z))
                    test += (x, z) -> p
                }
            }
        }
    }

    private def isUnimportant(w:World, x:Int, y:Int, z:Int):Boolean =
    {
        val b = w.getBlock(x, y, z)
        if (WorldLib.isBlockSoft(w, x, y, z, b) || WorldLib.isAssociatedTreeBlock(w, x, y, z, b)) return true
        if (b == Blocks.flowing_water || b == Blocks.water ||b == Blocks.snow || b == Blocks.ice) return true
        false
    }
}
