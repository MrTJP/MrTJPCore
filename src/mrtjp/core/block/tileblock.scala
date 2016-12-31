/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.block

import java.util.{Random, ArrayList => JArrayList, List => JList}

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.packet.{ICustomPacketTile, PacketCustom}
import codechicken.lib.render.block.{BlockRenderingRegistry, ICCBlockRenderer}
import codechicken.lib.vec.{Cuboid6, Rotation, Vector3}
import mrtjp.core.handler.MrTJPCoreSPH
import mrtjp.core.world.WorldLib
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.properties.{IProperty, PropertyInteger}
import net.minecraft.block.state.{BlockStateContainer, IBlockState}
import net.minecraft.client.renderer.VertexBuffer
import net.minecraft.client.renderer.texture.{TextureAtlasSprite, TextureMap}
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.entity.{Entity, EntityLivingBase}
import net.minecraft.init.Enchantments
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.network.NetworkManager
import net.minecraft.network.play.server.SPacketUpdateTileEntity
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.math.{AxisAlignedBB, BlockPos, RayTraceResult}
import net.minecraft.util._
import net.minecraft.world.{Explosion, IBlockAccess, World}
import net.minecraftforge.fml.common.registry.GameRegistry
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

object MultiTileBlock
{
    val TILE_INDEX:IProperty[Integer] = PropertyInteger.create("tile_idx", 0, 15)
}

class MultiTileBlock(mat:Material) extends Block(mat)
{
    import MultiTileBlock._

    private val tiles = new Array[Class[_ <: MTBlockTile]](16)

    override def createBlockState() = new BlockStateContainer(this, TILE_INDEX)

    override def getActualState(state:IBlockState, world:IBlockAccess, pos:BlockPos) =
        world.getTileEntity(pos) match {
            case t:MTBlockTile => state.withProperty(TILE_INDEX, tiles.indexOf(t.getClass).asInstanceOf[Integer])
            case _ => state
        }

    override def getMetaFromState(state:IBlockState) = state.getValue(TILE_INDEX)

    override def getStateFromMeta(meta:Int) = getDefaultState.withProperty(TILE_INDEX, (meta%16).asInstanceOf[Integer])

    def addTile[A <: MTBlockTile](t:Class[A], index:Int)
    {
        tiles(index) = t
        GameRegistry.registerTileEntity(t, getRegistryName.toString+"|"+index)
    }

    override def hasTileEntity(state:IBlockState) = true

    override def createTileEntity(world:World, state:IBlockState) =
    {
        var t:MTBlockTile = null
        try { t = tiles(getMetaFromState(state)).newInstance }
        catch {case e:Exception => e.printStackTrace()}
        t
    }

    override def damageDropped(state:IBlockState) = getMetaFromState(state)

    override def isBlockNormalCube(state:IBlockState) = false

    override def isOpaqueCube(state:IBlockState) = false

    override def isFullCube(state:IBlockState) = false

    override def isFullBlock(state:IBlockState) = false

    override def getTickRandomly = true

    override def addCollisionBoxToList(state:IBlockState, world:World, pos:BlockPos, entityBox:AxisAlignedBB, collidingBoxes:JList[AxisAlignedBB], entity:Entity)
    {
        world.getTileEntity(pos) match {
            case t:MTBlockTile =>
                val bounds = t.getCollisionBounds.aabb()
                val mask = entityBox.offset(-pos.getX, -pos.getY, -pos.getZ)
                if (bounds.intersectsWith(mask))
                    collidingBoxes.add(bounds.offset(pos))
            case _ =>
        }
    }

    override def getBoundingBox(state:IBlockState, world:IBlockAccess, pos:BlockPos) =
        world.getTileEntity(pos) match {
            case t:MTBlockTile => t.getBlockBounds.aabb()
            case _ => super.getBoundingBox(state, world, pos)
        }

    override def isBlockSolid(world:IBlockAccess, pos:BlockPos, side:EnumFacing) =
        world.getTileEntity(pos) match {
            case t:MTBlockTile => t.isSolid(side.ordinal())
            case _ => false
        }

    override def isSideSolid(state:IBlockState, world:IBlockAccess, pos:BlockPos, side:EnumFacing) = isBlockSolid(world, pos, side)

    override def canPlaceTorchOnTop(state:IBlockState, world:IBlockAccess, pos:BlockPos) =
        world.getTileEntity(pos) match {
            case t:MTBlockTile => t.canPlaceTorchOnTop
        }

    override def getExplosionResistance(world:World, pos:BlockPos, exploder:Entity, explosion:Explosion) =
        world.getTileEntity(pos) match {
            case t:MTBlockTile => t.getExplosionResistance
            case _ => 0F
        }

    override def getLightValue(state:IBlockState, world:IBlockAccess, pos:BlockPos) =
        world.getTileEntity(pos) match {
            case t:MTBlockTile => t.getLightValue
            case _ => super.getLightValue(state, world, pos)
        }

    override def getPlayerRelativeBlockHardness(state:IBlockState, player:EntityPlayer, world:World, pos:BlockPos) =
        world.getTileEntity(pos) match {
            case t:MTBlockTile => t.getHardness
            case _ => super.getPlayerRelativeBlockHardness(state, player, world, pos)
        }

    override def removedByPlayer(state:IBlockState, world:World, pos:BlockPos, player:EntityPlayer, willHarvest:Boolean) =
    {
        if (world.isRemote) true
        else {
            val b = state.getBlock
            if (b.canHarvestBlock(world, pos, player) && !player.capabilities.isCreativeMode) {
                val stacks = getDrops(world, pos, state, EnchantmentHelper.getEnchantmentLevel(Enchantments.FORTUNE, player.getHeldItemMainhand))
                for (stack <- stacks) WorldLib.dropItem(world, pos, stack)
            }
            world.setBlockToAir(pos)
            true
        }
    }

    override def breakBlock(world:World, pos:BlockPos, state:IBlockState)
    {
        world.getTileEntity(pos) match {
            case t:MTBlockTile => t.onBlockRemoval()
            case _ =>
        }
        super.breakBlock(world, pos, state)
    }

    override def harvestBlock(worldIn:World, player:EntityPlayer, pos:BlockPos, state:IBlockState, te:TileEntity, stack:ItemStack){}

    override def getDrops(world:IBlockAccess, pos:BlockPos, state:IBlockState, fortune:Int) =
    {
        val list = new ListBuffer[ItemStack]
        world.getTileEntity(pos) match {
            case t:MTBlockTile => t.addHarvestContents(list)
            case _ =>
        }
        new JArrayList[ItemStack](list)
    }

    override def getPickBlock(state:IBlockState, target:RayTraceResult, world:World, pos:BlockPos, player:EntityPlayer) =
        world.getTileEntity(pos) match {
            case t:MTBlockTile => t.getPickBlock
            case _ => super.getPickBlock(state, target, world, pos, player)
        }

    override def onBlockActivated(world:World, pos:BlockPos, state:IBlockState, player:EntityPlayer, hand:EnumHand, held:ItemStack, side:EnumFacing, hitX:Float, hitY:Float, hitZ:Float) =
        world.getTileEntity(pos) match {
            case t:MTBlockTile => t.onBlockActivated(player, side.ordinal)
            case _ => false
        }

    override def onBlockClicked(world:World, pos:BlockPos, player:EntityPlayer)
    {
        world.getTileEntity(pos) match {
            case t:MTBlockTile => t.onBlockClicked(player)
            case _ =>
        }
    }

    override def onEntityCollidedWithBlock(world:World, pos:BlockPos, state:IBlockState, entity:Entity)
    {
        world.getTileEntity(pos) match {
            case t:MTBlockTile => t.onEntityCollision(entity)
            case _ =>
        }
    }

    override def onEntityWalk(world:World, pos:BlockPos, entity:Entity) =
        world.getTileEntity(pos) match {
            case t:MTBlockTile => t.onEntityWalk(entity)
        }


    override def neighborChanged(state:IBlockState, world:World, pos:BlockPos, block:Block)
    {
        world.getTileEntity(pos) match {
            case t:MTBlockTile => t.onNeighborBlockChange()
            case _ =>
        }
    }

    override def onNeighborChange(world:IBlockAccess, pos:BlockPos, neighbor:BlockPos)
    {
        world.getTileEntity(pos) match {
            case t:MTBlockTile => t.onNeighborTileChange(neighbor)
            case _ =>
        }
    }

    override def onBlockPlaced(world:World, pos:BlockPos, facing:EnumFacing, hitX:Float, hitY:Float, hitZ:Float, meta:Int, placer:EntityLivingBase) =
    {
        if (tiles.isDefinedAt(meta) && tiles(meta) != null)
            super.onBlockPlaced(world, pos, facing, hitX, hitY, hitZ, meta, placer)
        else
            throw new RuntimeException("MultiTileBlock "+this.getRegistryName+" was placed w/ invalid metadata. Most likely an invalid return value on this block's ItemBlock#getMetadata")
    }

    def postBlockSetup(w:World, pos:BlockPos, side:Int, player:EntityPlayer, stack:ItemStack, hit:Vector3)
    {
        w.getTileEntity(pos) match {
            case t:MTBlockTile => t.onBlockPlaced(side, player, stack)
            case _ =>
        }
    }

    override def getWeakChanges(world:IBlockAccess, pos:BlockPos) =
        world.getTileEntity(pos) match {
            case t:MTBlockTile => t.getWeakChanges
            case _ => false
        }

    override def canProvidePower(state:IBlockState) = true

    override def canConnectRedstone(state:IBlockState, world:IBlockAccess, pos:BlockPos, side:EnumFacing) =
        world.getTileEntity(pos) match {
            case t:MTBlockTile => t.canConnectRS
            case _ => super.canConnectRedstone(state, world, pos, side)
        }

    override def getStrongPower(state:IBlockState, world:IBlockAccess, pos:BlockPos, side:EnumFacing) =
        world.getTileEntity(pos) match {
            case t:MTBlockTile => t.strongPower(side.ordinal)
            case _ => 0
        }

    override def getWeakPower(state:IBlockState, world:IBlockAccess, pos:BlockPos, side:EnumFacing) =
        world.getTileEntity(pos) match {
            case t:MTBlockTile => t.weakPower(side.ordinal)
            case _ => 0
        }

    override def isFireSource(world:World, pos:BlockPos, side:EnumFacing) =
        world.getTileEntity(pos) match {
            case t:MTBlockTile => t.isFireSource(side.ordinal)
            case _ => super.isFireSource(world, pos, side)
        }

    override def updateTick(world:World, pos:BlockPos, state:IBlockState, rand:Random) =
        world.getTileEntity(pos) match {
            case t:MTBlockTile => t.randomTick(rand)
            case _ => super.updateTick(world, pos, state, rand)
        }

    @SideOnly(Side.CLIENT)
    override def getSubBlocks(item:Item, tab:CreativeTabs, list:JList[ItemStack])
    {
        for (i <- 0 until tiles.length) if (tiles(i) != null)
            list.add(new ItemStack(item, 1, i))
    }

    @SideOnly(Side.CLIENT)
    override def randomDisplayTick(state:IBlockState, world:World, pos:BlockPos, rand:Random)
    {
        world.getTileEntity(pos) match {
            case t:MTBlockTile => t.randomDisplayTick(rand)
            case _ =>
        }
    }
}

trait TTileOrient extends MTBlockTile
{
    var orientation:Byte = 0

    def side = orientation>>2

    def setSide(s:Int)
    {
        val oldOrient = orientation
        orientation = (orientation&0x3|s<<2).toByte
        if (oldOrient != orientation) onOrientChanged(oldOrient)
    }

    def rotation = orientation&0x3

    def setRotation(r:Int)
    {
        val oldOrient = orientation
        orientation = (orientation&0xFC|r).toByte
        if (oldOrient != orientation) onOrientChanged(oldOrient)
    }

    //def position = new BlockCoord(getPos)

    def rotationT = Rotation.sideOrientation(side, rotation).at(Vector3.center)

    def onOrientChanged(oldOrient:Int){}

    // internal r from absRot
    def toInternal(absRot:Int) = (absRot+6-rotation)%4

    // absRot from internal r
    def toAbsolute(r:Int) = (r+rotation+2)%4

    // absDir from absRot
    def absoluteDir(absRot:Int) = Rotation.rotateSide(side, absRot)

    // absRot from absDir
    def absoluteRot(absDir:Int) = Rotation.rotationTo(side, absDir)
}

abstract class MTBlockTile extends TileEntity with ICustomPacketTile with ITickable
{
    protected var schedTick = -1L

    def onBlockPlaced(side:Int, player:EntityPlayer, stack:ItemStack){}

    def onBlockRemoval(){}

    def onNeighborBlockChange(){}

    def onNeighborTileChange(neighbor:BlockPos){}

    def getWeakChanges = false

    def canConnectRS = false
    def strongPower(side:Int) = 0
    def weakPower(side:Int) = strongPower(side)

    def getLightValue = 0

    def isFireSource(side:Int) = false

    def isSolid(side:Int) = true

    def canPlaceTorchOnTop = true

    def getExplosionResistance = 0

    def getHardness = 1F

    def onBlockActivated(player:EntityPlayer, side:Int) = false

    def onBlockClicked(player:EntityPlayer) = false

    def onEntityCollision(ent:Entity){}

    def onEntityWalk(ent:Entity){}

    def getBlockBounds = Cuboid6.full

    def getCollisionBounds = Cuboid6.full

    def onScheduledTick(){}

    def updateClient(){}

    def updateServer(){}

    def randomTick(rand:Random){}

    @SideOnly(Side.CLIENT)
    def randomDisplayTick(rand:Random){}

    def getBlock:Block

    def getPickBlock = new ItemStack(getBlock, 1, getBlockMetadata)

    def addHarvestContents(ist:ListBuffer[ItemStack])
    {
        ist += getPickBlock
    }

    def world = worldObj
    def x = getPos.getX
    def y = getPos.getY
    def z = getPos.getZ

    def scheduleTick(time:Int)
    {
        val tn = world.getTotalWorldTime+time
        if (schedTick > 0L && schedTick < tn) return
        schedTick = tn
        markDirty()
    }

    def isTickScheduled = schedTick >= 0L

    def breakBlock_do()
    {
        val il = new ListBuffer[ItemStack]
        addHarvestContents(il)
        for (stack <- il) WorldLib.dropItem(world, pos, stack)
        world.setBlockToAir(pos)
    }

    override def markDirty()
    {
        world.markChunkDirty(pos, this)
    }

    final def markRender()
    {
        world.markBlockRangeForRenderUpdate(pos, pos)
    }

    final def markLight()
    {
        world.checkLight(pos)
    }

    final def markDescUpdate()
    {
//        val state = world.getBlockState(pos)
//        world.notifyBlockUpdate(pos, state, state, 3)
        val packet = writeStream(0)
        writeDesc(packet)
        sendWriteStream(packet)
    }

    final override def update()
    {
        if (world.isRemote) {
            updateClient()
            return
        }
        else updateServer()

        if (schedTick < 0L) return
        val time = world.getTotalWorldTime
        if (schedTick <= time) {
            schedTick = -1L
            onScheduledTick()
            markDirty()
        }
    }

    final override def writeToNBT(tag:NBTTagCompound) =
    {
        super.writeToNBT(tag)
        tag.setLong("sched", schedTick)
        save(tag)
        tag
    }

    final override def readFromNBT(tag:NBTTagCompound)
    {
        super.readFromNBT(tag)
        schedTick = tag.getLong("sched")
        load(tag)
    }


    override def getUpdateTag =
    {
        val tag = super.getUpdateTag
        writeToNBT(tag)
        tag
    }

    override def handleUpdateTag(tag:NBTTagCompound)
    {
        super.handleUpdateTag(tag)
        readFromNBT(tag)
    }

    override def getUpdatePacket =
    {
//        val packet = new PacketCustom(MrTJPCoreSPH.channel, MrTJPCoreSPH.tilePacket)
//        writeToPacket(packet)
//        packet.toTilePacket(getPos)
        null
    }

    override def onDataPacket(net:NetworkManager, pkt:SPacketUpdateTileEntity)
    {
        //readFromPacket(PacketCustom.fromTilePacket(pkt))
    }

    def save(tag:NBTTagCompound){}

    def load(tag:NBTTagCompound){}

    def writeDesc(out:MCDataOutput){}

    def readDesc(in:MCDataInput){}

    final override def readFromPacket(packet:MCDataInput)
    {
        packet.readUByte() match {
            case 0 => readDesc(packet)
            case key => read(packet, key)
        }
    }

    final override def writeToPacket(packet:MCDataOutput)
    {
        writeDesc(packet.writeByte(0))
    }

    def read(in:MCDataInput, key:Int){}

    final def writeStream(key:Int):PacketCustom =
    {
        val stream = new PacketCustom(MrTJPCoreSPH.channel, MrTJPCoreSPH.tilePacket)
        stream.writePos(pos).writeByte(key)
        stream
    }

    final def sendWriteStream(packet:PacketCustom)
    {
        packet.sendToChunk(world, x>>4, z>>4)
    }
}
