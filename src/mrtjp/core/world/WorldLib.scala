/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.world

import codechicken.lib.vec.BlockCoord
import net.minecraft.block.{IGrowable, Block, BlockLeavesBase}
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.chunk.storage.ExtendedBlockStorage
import net.minecraft.world.{ChunkPosition, IBlockAccess, World}
import net.minecraftforge.common.IPlantable

object WorldLib
{
    def getTileEntity(w:IBlockAccess, bc:BlockCoord):TileEntity =
        getTileEntity(w, bc, classOf[TileEntity])

    def getTileEntity[T](w:IBlockAccess, bc:BlockCoord, clazz:Class[T]):T =
        getTileEntity(w, bc.x, bc.y, bc.z, clazz)

    def getTileEntity[T](w:IBlockAccess, x:Int, y:Int, z:Int, clazz:Class[T]):T =
    {
        if (y < 0) null.asInstanceOf[T]
        else
        {
            val tile = w.getTileEntity(x, y, z)
            if (!clazz.isInstance(tile)) null.asInstanceOf[T] else tile.asInstanceOf[T]
        }
    }

    def getBlock(w:IBlockAccess, bc:BlockCoord) = w.getBlock(bc.x, bc.y, bc.z)

    def bulkBlockUpdate(w:World, x:Int, y:Int, z:Int, bl:Block)
    {
        for (a <- -3 to 3) for (b <- -3 to 3) for (c <- -3 to 3)
        {
            val md = (if (a < 0) -a else a) + (if (b < 0) -b else b) + (if (c < 0) -c else c)
            if (md <= 3)
            {
                val block = w.getBlock(x+a, y+b, z+c)
                if (block != null) block.onNeighborBlockChange(w, x+a, y+b, z+c, bl)
            }
        }
    }

    def dropItem(w:World, bc:BlockCoord, stack:ItemStack)
    {
        dropItem(w, bc.x, bc.y, bc.z, stack)
    }
    def dropItem(w:World, x:Int, y:Int, z:Int, stack:ItemStack)
    {
        if (!w.isRemote && w.getGameRules.getGameRuleBooleanValue("doTileDrops"))
        {
            val d = 0.7D
            val dx = w.rand.nextFloat*d+(1.0D-d)*0.5D
            val dy = w.rand.nextFloat*d+(1.0D-d)*0.5D
            val dz = w.rand.nextFloat*d+(1.0D-d)*0.5D
            val item = new EntityItem(w, x+dx, y+dy, z+dz, stack)
            item.delayBeforeCanPickup = 10
            w.spawnEntityInWorld(item)
        }
    }

    def packCoords(x:Int, y:Int, z:Int):Long =
        (x+30000000).toLong|(z+30000000).toLong<<26|y.toLong<<52

    def unpackX(c:Long):Int = (c&((1<<26)-1)).toInt-30000000

    def unpackZ(c:Long):Int = ((c>>26)&((1<<26)-1)).toInt-30000000

    def unpackY(c:Long):Int = ((c>>52)&((1<<12)-1)).toInt

    def uncheckedSetBlock(world:World, x:Int, y:Int, z:Int, block:Block, meta:Int)
    {
        val ch = world.getChunkFromBlockCoords(x, z)
        val arr = ch.getBlockStorageArray
        if (arr(y>>4) == null)
            arr(y>>4) = new ExtendedBlockStorage(y&(~0xF),!world.provider.hasNoSky)
        arr(y>>4).func_150818_a(x&0xF, y&0xF, z&0xF, block)
        arr(y>>4).setExtBlockMetadata(x&0xF, y&0xF, z&0xF, meta)
        world.markBlockForUpdate(x, y, z)
    }

    def uncheckedRemoveTileEntity(world:World, x:Int, y:Int, z:Int)
    {
        val ch = world.getChunkFromBlockCoords(x, z)
        ch.chunkTileEntityMap.remove(new ChunkPosition(x&0xF, y, z&0xF))
    }

    def uncheckedSetTileEntity(world:World, x:Int, y:Int, z:Int, te:TileEntity)
    {
        val ch = world.getChunkFromBlockCoords(x, z)
        ch.chunkTileEntityMap.asInstanceOf[java.util.Map[ChunkPosition, TileEntity]].
            put(new ChunkPosition(x&0xF, y, z&0xF), te)
    }

    def uncheckedGetTileEntity(world:World, x:Int, y:Int, z:Int) =
    {
        val ch = world.getChunkFromBlockCoords(x, z)
        ch.chunkTileEntityMap.get(new ChunkPosition(x&0xF, y, z&0xF)).asInstanceOf[TileEntity]
    }

    def getBlockInfo(world:World, x:Int, y:Int, z:Int) =
        (world.getBlock(x, y, z), world.getBlockMetadata(x, y, z),
            world.getTileEntity(x, y, z))

    def isLeafType(world:World, x:Int, y:Int, z:Int, b:Block) =
        b.isLeaves(world, x, y, z) || b.isInstanceOf[BlockLeavesBase]

    def isPlantType(world:World, x:Int, y:Int, z:Int, b:Block) = b match
    {
        case b:IGrowable => true
        case b:IPlantable => true
        case _ => false
    }
}
