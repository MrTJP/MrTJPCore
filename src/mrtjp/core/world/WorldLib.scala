/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.world

import mrtjp.core.math.PerlinNoiseGenerator
import net.minecraft.block.state.IBlockState
import net.minecraft.block.{Block, BlockGrass, IGrowable}
import net.minecraft.entity.item.EntityItem
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world._
import net.minecraft.world.chunk.storage.ExtendedBlockStorage
import net.minecraftforge.common.IPlantable
import net.minecraftforge.fml.common.FMLLog
import net.minecraftforge.oredict.OreDictionary

object WorldLib
{
//    def getTileEntity[T](world:IBlockAccess, pos:BlockPos, clazz:Class[T]):T =
//    {
//        val tile = world.getTileEntity(pos)
//        if (!clazz.isInstance(tile)) null.asInstanceOf[T] else tile.asInstanceOf[T]
//    }

    def bulkBlockUpdate(world:World, pos:BlockPos, bl:Block)
    {
        for (a <- -3 to 3) for (b <- -3 to 3) for (c <- -3 to 3)
        {
            val md = (if (a < 0) -a else a) + (if (b < 0) -b else b) + (if (c < 0) -c else c)
            if (md <= 3)
            {
                val pos2 = new BlockPos(pos.getX+a, pos.getY+b, pos.getZ+c)
                val state = world.getBlockState(pos2)
                state.getBlock.onNeighborChange(world, pos2, pos)
            }
        }
    }

    def dropItem(world:World, pos:BlockPos, stack:ItemStack)
    {
        if (!world.isRemote && world.getGameRules.getBoolean("doTileDrops"))
        {
            val d = 0.7D
            val dx = world.rand.nextFloat*d+(1.0D-d)*0.5D
            val dy = world.rand.nextFloat*d+(1.0D-d)*0.5D
            val dz = world.rand.nextFloat*d+(1.0D-d)*0.5D
            val item = new EntityItem(world, pos.getX+dx, pos.getY+dy, pos.getZ+dz, stack)
            item.setPickupDelay(10)
            world.spawnEntityInWorld(item)
        }
    }

    def centerEject(w:World, pos:BlockPos, stack:ItemStack, dir:Int, vel:Double)
    {
        val pos2 = pos.offset(EnumFacing.values()(dir))
        val item = new EntityItem(w, pos2.getX+0.5D, pos2.getY+0.5D, pos2.getZ+0.5D, stack)

        item.motionX = 0; item.motionY = 0; item.motionZ = 0
        item.setPickupDelay(10)
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

    def uncheckedSetBlock(world:World, pos:BlockPos, state:IBlockState)
    {
        val ch = world.getChunkFromBlockCoords(pos)
        val arr = ch.getBlockStorageArray
        val x = pos.getX
        val y = pos.getY
        val z = pos.getZ

        if (arr(y>>4) == null)
            arr(y>>4) = new ExtendedBlockStorage(y&(~0xF),!world.provider.getHasNoSky)
        val oldState = arr(y>>4).get(x, y, z)
        arr(y>>4).set(x, y, z, state)
        world.markBlockRangeForRenderUpdate(pos, pos)
        world.notifyBlockUpdate(pos, oldState, state, 3)
    }

    def uncheckedRemoveTileEntity(world:World, pos:BlockPos)
    {
        val ch = world.getChunkFromBlockCoords(pos)
        ch.getTileEntityMap.remove(pos)
    }

    def uncheckedSetTileEntity(world:World, pos:BlockPos, tile:TileEntity)
    {
        val ch = world.getChunkFromBlockCoords(pos)
        ch.getTileEntityMap.put(pos, tile)
    }

    def uncheckedGetTileEntity(world:World, pos:BlockPos):TileEntity =
    {
        val ch = world.getChunkFromBlockCoords(pos)
        ch.getTileEntityMap.get(pos)
    }

    def hasItem(state: IBlockState) : Boolean = {
        Item.getItemFromBlock(state.getBlock) != null
    }

    def isLeafType(world:World, pos:BlockPos, state:IBlockState) =
        state.getBlock.isLeaves(state, world, pos) || (hasItem(state) && OreDictionary.getOreIDs(new ItemStack(state.getBlock)).contains(OreDictionary.getOreID("treeLeaves")))
    def isWoodType(world: World, pos:BlockPos, state:IBlockState) =
        state.getBlock.isWood(world, pos) || (hasItem(state) && OreDictionary.getOreIDs(new ItemStack(state.getBlock)).contains(OreDictionary.getOreID("logWood")))

    def isPlantType(world:World, pos:BlockPos, state:IBlockState) = state.getBlock match
    {
        case b:IGrowable => !b.isInstanceOf[BlockGrass]
        case b:IPlantable => true
        case _ => state.getBlock.isFoliage(world, pos)
    }

    def isBlockSoft(world:World, pos:BlockPos, state:IBlockState) =
        state.getBlock.isAir(state, world, pos) || state.getBlock.isReplaceable(world, pos) ||
                isLeafType(world, pos, state) || isPlantType(world, pos, state) ||
                    state.getBlock.canBeReplacedByLeaves(state, world, pos)

    def isAssociatedTreeBlock(world:World, pos:BlockPos, state:IBlockState) =
    {
        import net.minecraft.init.Blocks._
        Seq(LOG, LOG2, LEAVES, LEAVES2, VINE, COCOA).contains(state.getBlock) || isLeafType(world, pos, state) || isWoodType(world, pos, state)
    }

    def findSurfaceHeight(world:World, pos:BlockPos) =
    {
        var pos2 = world.getHeight(pos).up()
        do pos2 = pos2.down() while (pos2.getY >= 0 && {val b = world.getBlockState(pos2); isBlockSoft(world, pos2, b) || isAssociatedTreeBlock(world, pos2, b)})
        pos2
    }

    def isBlockTouchingAir(world:World, pos:BlockPos):Boolean =
    {
        for (s <- 0 until 6)
            if (world.isAirBlock(pos.offset(EnumFacing.values.apply(s))))
                return true

        false
    }

    def isBlockUnderTree(world:World, pos:BlockPos):Boolean =
    {
        if (world.canBlockSeeSky(pos)) return false
        for (h <- pos.getY until world.getHeight)
        {
            val pos2 = pos.up(h)
            val b = world.getBlockState(pos2)
            if (isLeafType(world, pos2, b) || isAssociatedTreeBlock(world, pos2, b)) return true
        }
        false
    }

    def getSkyLightValue(world:World, pos:BlockPos) =
        world.getLightFor(EnumSkyBlock.SKY, pos)-world.getSkylightSubtracted

    def getBlockLightValue(w:World, pos:BlockPos) = w.getLightFor(EnumSkyBlock.BLOCK, pos)

    private val noise = new PerlinNoiseGenerator(2576710L)
    def getWindSpeed(world:World, pos:BlockPos):Double =
    {
        if (world.provider.isSurfaceWorld) return 0.5D
        var nv = noise.noise(world.getWorldTime*0.00000085D, 0, 0, 5, 7.5D, 5.0D, true)

        nv = math.max(0.0D, 1.6D*(nv-0.006D)+0.06D)*math.sqrt(pos.getY)/16.0D

        val bgb = world.getBiomeGenForCoords(pos)
        if (bgb.canRain)
            if (world.isThundering) return 2.5D*nv
            else if (world.isRaining) return 0.5D+0.5D*nv

        nv
    }
}
