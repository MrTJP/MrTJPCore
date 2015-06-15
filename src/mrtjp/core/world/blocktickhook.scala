/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.world

import java.util.{List => JList, Random}

import cpw.mods.fml.common.FMLCommonHandler
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.gameevent.TickEvent.{Phase, WorldTickEvent}
import cpw.mods.fml.relauncher.Side
import net.minecraft.block.Block
import net.minecraft.world.chunk.Chunk
import net.minecraft.world.{World, WorldServer}

import scala.collection.JavaConversions._
import scala.collection.mutable.{Set => MSet}

object BlockUpdateHandler
{
    private var updateLCG = new Random().nextInt
    private var handlers = MSet[IBlockEventHandler]()
    private var registered = false

    def register(handler:IBlockEventHandler)
    {
        if (!registered)
        {
            FMLCommonHandler.instance().bus().register(this)
            registered = true
        }

        handlers += handler
    }

    @SubscribeEvent
    def onTick(event:WorldTickEvent)
    {
        if (event.side != Side.SERVER || event.phase != Phase.END) return

        //Reproduces same algorithm used for random block updates
        val world = event.world.asInstanceOf[WorldServer]
        val chunks = world.theChunkProviderServer.loadedChunks.asInstanceOf[JList[Chunk]]
        for (chunk <- chunks)
        {
            val extendedblockstorage = chunk.getBlockStorageArray
            for (ebs <- extendedblockstorage) if (ebs != null)
            {
                for (i <- 0 until 3)
                {
                    updateLCG = updateLCG*3+1013904223
                    val i2 = updateLCG>>2
                    val j2 = i2&15
                    val k2 = i2>>8&15
                    val l2 = i2>>16&15
                    val block = ebs.getBlockByExtId(j2, l2, k2)

                    for (h <- handlers)
                        h.onBlockUpdate(world, j2+chunk.xPosition*16, l2+ebs.getYLocation, k2+chunk.zPosition*16, block)
                }
            }
        }
    }
}

trait IBlockEventHandler
{
    def onBlockUpdate(w:World, x:Int, y:Int, z:Int, b:Block)
}