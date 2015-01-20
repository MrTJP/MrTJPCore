/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.world

import java.util.Random

import codechicken.lib.vec.BlockCoord
import net.minecraft.block.Block
import net.minecraft.world.World

import scala.annotation.tailrec
import scala.collection.immutable.Queue

class WorldGenCaveReformer extends TWorldGenerator
{
    var cluster = Set[((Block, Int), Int)]()
    var material = Set[(Block, Int)]()
    var clusterSize = 1
    var depth = 5
    var searchRadius = 16

    override def generate(w:World, rand:Random, x:Int, y:Int, z:Int):Boolean =
    {
        if (!w.isAirBlock(x, y, z)) return false

        var dy = 0
        while (dy < searchRadius && !canSetBlock(w, x, dy+y, z, material)) dy+=1
        if (!canSetBlock(w, x, dy+y, z, material)) return false

        val start = Node(new BlockCoord(x, dy+y, z))
        start.depth = depth
        iterate(w, Queue(start))
    }

    @tailrec
    private def iterate(w:World, open:Seq[Node], closed:Set[Node] = Set.empty, generated:Boolean = false):Boolean = open match
    {
        case Seq() => generated

        case Seq(next, rest@_*) =>
            if (closed.size > clusterSize) return generated
            val g2 = setBlock(w, next.bc.x, next.bc.y, next.bc.z, cluster, material)
            val upNext = Vector.newBuilder[Node]
            if (next.depth > 0) for (s <- 0 until 6)
            {
                val to = next --> s
                if (!open.contains(to) && !closed.contains(to))
                {
                    to.depth = if (WorldLib.isBlockTouchingAir(w, to.bc)) depth else next.depth-1
                    upNext += to
                }
            }
            iterate(w, rest++upNext.result(), closed+next, generated || g2)

        case _ => generated
    }

    private object Node
    {
        def apply(bc:BlockCoord):Node = new Node(bc, 0)
        def apply(bc:BlockCoord, dir:Int):Node = new Node(bc.copy.offset(dir), 1)
    }
    private class Node(val bc:BlockCoord, val dist:Int) extends Ordered[Node]
    {
        var depth = 0

        def -->(toDir:Int, distAway:Int):Node = new Node(bc.copy.offset(toDir), dist+distAway)
        def -->(toDir:Int):Node = this -->(toDir, 1)

        override def compare(that:Node) = dist-that.dist

        override def equals(other:Any) = other match
        {
            case that:Node => bc == that.bc
            case _ => false
        }

        override def hashCode = bc.hashCode

        override def toString = "@"+bc.toString
    }
}
