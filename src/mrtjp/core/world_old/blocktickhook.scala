/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.world

import java.util.{HashSet => JHSet, Random, Set => JSet}

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent.{Phase, WorldTickEvent}
import cpw.mods.fml.relauncher.Side
import net.minecraft.block.Block
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.server.MinecraftServer
import net.minecraft.util.MathHelper
import net.minecraft.world._

object BlockUpdateHandler
{
    private var updateLCG = new Random().nextInt
    private var handlers = Array.empty[IBlockEventHandler]
    private var registered = false
    private var chunkSet = new JHSet[ChunkCoordIntPair]()

    def register(handler:IBlockEventHandler)
    {
        if (!registered)
        {
            FMLCommonHandler.instance().bus().register(this)
            registered = true
        }

        handlers :+= handler
    }

    def getActiveChunkSet(w:World):JSet[ChunkCoordIntPair] =
    {
        chunkSet.clear()
        chunkSet.addAll(w.getPersistentChunks.keySet)

        var i = 0
        while (i < w.playerEntities.size)
        {
            val entityplayer = w.playerEntities.get(i).asInstanceOf[EntityPlayer]
            val j = MathHelper.floor_double(entityplayer.posX/16.0D)
            val k = MathHelper.floor_double(entityplayer.posZ/16.0D)
            val l = MinecraftServer.getServer.getConfigurationManager.getViewDistance

            var i1 = -l
            while (i1 <= l)
            {
                var j1 = -l
                while (j1 <= l)
                {
                    chunkSet.add(new ChunkCoordIntPair(i1+j, j1+k))
                    j1 += 1
                }
                i1 += 1
            }
            i += 1
        }
        chunkSet
    }

    @SubscribeEvent
    def onTick(event:WorldTickEvent)
    {
        if (event.side != Side.SERVER || event.phase != Phase.END) return

        //Reproduces same algorithm used for random block updates
        val world = event.world.asInstanceOf[WorldServer]

        val cIt = getActiveChunkSet(world).iterator()
        while (cIt.hasNext)
        {
            val chunkPos = cIt.next()
            val chunk = world.getChunkFromChunkCoords(chunkPos.chunkXPos, chunkPos.chunkZPos)
            val ebstorage = chunk.getBlockStorageArray

            var k = 0
            while (k < ebstorage.length)
            {
                val ebs = ebstorage(k)
                if (ebs != null)
                {
                    var i = 0
                    while (i < 3)
                    {
                        updateLCG = updateLCG*3+1013904223
                        val i2 = updateLCG>>2
                        val j2 = i2&15
                        val k2 = i2>>8&15
                        val l2 = i2>>16&15
                        val block = ebs.getBlockByExtId(j2, l2, k2)

                        var j = 0
                        while(j < handlers.length)
                        {
                            handlers(j).onBlockUpdate(world, j2+chunk.xPosition*16, l2+ebs.getYLocation,
                                k2+chunk.zPosition*16, block)
                            j += 1
                        }
                        i += 1
                    }
                }
                k += 1
            }
        }
    }
}

trait IBlockEventHandler
{
    def onBlockUpdate(w:World, x:Int, y:Int, z:Int, b:Block)
}