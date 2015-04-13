/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.world

import codechicken.lib.vec.BlockCoord
import mrtjp.core.math.{PerlinNoiseGenerator, MathLib}
import net.minecraft.block.{BlockGrass, BlockGlass, Block, IGrowable}
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.ItemStack
import net.minecraft.tileentity.TileEntity
import net.minecraft.world
import net.minecraft.world.chunk.storage.ExtendedBlockStorage
import net.minecraft.world._
import net.minecraftforge.common.IPlantable
import net.minecraftforge.oredict.OreDictionary

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

    def centerEject(w:World, bc:BlockCoord, stack:ItemStack, dir:Int, vel:Double)
    {
        centerEject(w, bc.x, bc.y, bc.z, stack, dir, vel)
    }
    def centerEject(w:World, x:Int, y:Int, z:Int, stack:ItemStack, dir:Int, vel:Double)
    {
        val bc = new BlockCoord(x, y, z).offset(dir)
        val item = new EntityItem(w, bc.x+0.5D, bc.y+0.5D, bc.z+0.5D, stack)

        item.motionX = 0; item.motionY = 0; item.motionZ = 0
        item.delayBeforeCanPickup = 10
        dir match
        {
            case 0 => item.motionY = -vel
            case 1 => item.motionY =  vel
            case 2 => item.motionZ = -vel
            case 3 => item.motionZ =  vel
            case 4 => item.motionX = -vel
            case 5 => item.motionX =  vel
        }
        w.spawnEntityInWorld(item)
    }

    def packCoords(bc:BlockCoord):Long = packCoords(bc.x, bc.y, bc.z)
    def packCoords(x:Int, y:Int, z:Int):Long = (x+30000000).toLong|(z+30000000).toLong<<26|y.toLong<<52

    def unpackX(c:Long):Int = (c&((1<<26)-1)).toInt-30000000
    def unpackZ(c:Long):Int = ((c>>26)&((1<<26)-1)).toInt-30000000
    def unpackY(c:Long):Int = ((c>>52)&((1<<12)-1)).toInt
    def unpackCoords(c:Long) = new BlockCoord(unpackX(c), unpackY(c), unpackZ(c))

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

    def getBlockMetaPair(world:World, x:Int, y:Int, z:Int) =
        (world.getBlock(x, y, z), world.getBlockMetadata(x, y, z))

    def getBlockInfo(world:World, x:Int, y:Int, z:Int) =
        (world.getBlock(x, y, z), world.getBlockMetadata(x, y, z),
                world.getTileEntity(x, y, z))

    def isLeafType(world:World, x:Int, y:Int, z:Int, b:Block) =
        b.isLeaves(world, x, y, z) || OreDictionary.getOreIDs(new ItemStack(b)).contains(OreDictionary.getOreID("treeLeaves"))

    def isPlantType(world:World, x:Int, y:Int, z:Int, b:Block) = b match
    {
        case b:IGrowable => !b.isInstanceOf[BlockGrass]
        case b:IPlantable => true
        case _ => b.isFoliage(world, x, y, z)
    }

    def isBlockSoft(world:World, x:Int, y:Int, z:Int, b:Block) =
        b.isAir(world, x, y, z) || b.isReplaceable(world, x, y, z) ||
                isLeafType(world, x, y, z, b) || isPlantType(world, x, y, z, b) ||
                b.canBeReplacedByLeaves(world, x, y, z)

    def isAssociatedTreeBlock(world:World, x:Int, y:Int, z:Int, b:Block) =
    {
        import net.minecraft.init.Blocks._
        Seq(log, log2, leaves, leaves2, vine, cocoa).contains(b) || isLeafType(world, x, y, z, b) ||
                OreDictionary.getOreIDs(new ItemStack(b)).contains(OreDictionary.getOreID("logWood"))
    }

    def findSurfaceHeight(world:World, x:Int, z:Int) =
    {
        var y = world.getHeightValue(x, z)+1
        do y -= 1 while (y >= 0 && {val b = world.getBlock(x, y, z); isBlockSoft(world, x, y, z, b) || isAssociatedTreeBlock(world, x, y, z, b)})
        y
    }

    def isBlockTouchingAir(world:World, b:BlockCoord):Boolean =
    {
        for (s <- 0 until 6)
        {
            val bc = b.copy.offset(s)
            if (world.isAirBlock(bc.x, bc.y, bc.z)) return true
        }
        false
    }

    def isBlockUnderTree(world:World, x:Int, y:Int, z:Int):Boolean =
    {
        if (world.canBlockSeeTheSky(x, y, z)) return false
        for (h <- y until world.getHeight)
        {
            val b = world.getBlock(x, h, z)
            if (isLeafType(world, x, h, z, b) || isAssociatedTreeBlock(world, x, h, z, b)) return true
        }
        false
    }

    def getSkyLightValue(w:World, x:Int, y:Int, z:Int) =
        w.getSavedLightValue(EnumSkyBlock.Sky, x, y, z)-w.skylightSubtracted

    def getBlockLightValue(w:World, x:Int, y:Int, z:Int) = w.getSavedLightValue(EnumSkyBlock.Block, x, y, z)

    private val noise = new PerlinNoiseGenerator(2576710L)
    def getWindSpeed(w:World, x:Int, y:Int, z:Int):Double =
    {
        if (w.provider.isHellWorld) return 0.5D
        var nv = noise.noise(w.getWorldTime*0.00000085D, 0, 0, 5, 7.5D, 5.0D, true)

        nv = math.max(0.0D, 1.6D*(nv-0.006D)+0.06D)*math.sqrt(y)/16.0D

        val bgb = w.getBiomeGenForCoords(x, z)
        if (bgb.canSpawnLightningBolt)
            if (w.isThundering) return 2.5D*nv
            else if (w.isRaining) return 0.5D+0.5D*nv

        nv
    }
}