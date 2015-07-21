/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.block

import java.util.{ArrayList => JArrayList, List => JList, Random}

import codechicken.lib.data.{MCDataInput, MCDataOutput}
import codechicken.lib.packet.{ICustomPacketTile, PacketCustom}
import codechicken.lib.render.TextureUtils
import codechicken.lib.vec.{BlockCoord, Cuboid6, Rotation, Vector3}
import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler
import cpw.mods.fml.common.registry.GameRegistry
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.handler.MrTJPCoreSPH
import mrtjp.core.world.WorldLib
import net.minecraft.block.material.Material
import net.minecraft.block.{Block, BlockContainer}
import net.minecraft.client.renderer.RenderBlocks
import net.minecraft.client.renderer.texture.{TextureUtil, IIconRegister}
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{Item, ItemBlock, ItemStack}
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.{IIcon, MovingObjectPosition}
import net.minecraft.world.{EnumSkyBlock, IBlockAccess, World}
import net.minecraftforge.common.util.ForgeDirection

import scala.collection.JavaConversions._
import scala.collection.mutable.ListBuffer

object TileRenderRegistry extends ISimpleBlockRenderingHandler
{
    var renderID = -1

    var renders = Map[String, Array[TInstancedBlockRender]]()
        .withDefaultValue(new Array[TInstancedBlockRender](16))

    def setRenderer(b:Block, meta:Int, r:TInstancedBlockRender)
    {
        val name = b.getUnlocalizedName
        val a = renders.get(name) match
        {
            case Some(e) => e
            case None => new Array[TInstancedBlockRender](16)
        }
        a(meta) = r
        renders += name -> a
    }

    def getRenderer(b:Block, meta:Int) = renders.get(b.getUnlocalizedName) match
    {
        case Some(e) => e(meta)
        case None => NullRenderer
    }

    def registerIcons(b:Block, reg:IIconRegister)
    {
        for (r <- renders(b.getUnlocalizedName))
            if (r != null) r.registerIcons(reg)
    }

    def getIcon(b:Block, side:Int, meta:Int) = getRenderer(b, meta).getIcon(side, meta)

    override def getRenderId = renderID

    override def shouldRender3DInInventory(modelId:Int) = true

    override def renderInventoryBlock(b:Block, meta:Int, rID:Int, r:RenderBlocks)
    {
        if (rID != renderID) return

        val render = getRenderer(b, meta)
        if (render == null)
        {
            println("No render mapping found for "+b.getUnlocalizedName+":"+meta)
            return
        }

        render.renderInvBlock(r, meta)
    }

    override def renderWorldBlock(w:IBlockAccess, x:Int, y:Int, z:Int, b:Block, rID:Int, r:RenderBlocks):Boolean =
    {
        if (rID != renderID) return false

        val meta = w.getBlockMetadata(x, y, z)
        val render = getRenderer(b, meta)

        if (render == null)
        {
            println("No render mapping found for "+b.getUnlocalizedName+":"+meta)
            return true
        }

        if (r.hasOverrideBlockTexture) render.renderBreaking(w, x, y, z, r.overrideBlockTexture)
        else render.renderWorldBlock(r, w, x, y, z, meta)
        true
    }
}

trait TInstancedBlockRender
{
    def renderWorldBlock(r:RenderBlocks, w:IBlockAccess, x:Int, y:Int, z:Int, meta:Int)

    def renderInvBlock(r:RenderBlocks, meta:Int)

    def renderBreaking(w:IBlockAccess, x:Int, y:Int, z:Int, icon:IIcon){}

    def randomDisplayTick(w:World, x:Int, y:Int, z:Int, r:Random){}

    def registerIcons(reg:IIconRegister)

    def getIcon(side:Int, meta:Int):IIcon
}

object NullRenderer extends TInstancedBlockRender
{
    override def renderWorldBlock(r:RenderBlocks, w:IBlockAccess, x:Int, y:Int, z:Int, meta:Int){}
    override def getIcon(side:Int, meta:Int) = null
    override def renderInvBlock(r:RenderBlocks, meta:Int){}
    override def registerIcons(reg:IIconRegister){}
}

class InstancedBlock(name:String, mat:Material) extends BlockContainer(mat)
{
    setBlockName(name)
    GameRegistry.registerBlock(this, getItemBlockClass, name)
    def getItemBlockClass:Class[_ <: ItemBlock] = classOf[ItemBlockCore]

    private var singleTile = false
    private val tiles = new Array[Class[_ <: InstancedBlockTile]](16)

    def addTile[A <: InstancedBlockTile](t:Class[A], meta:Int)
    {
        tiles(meta) = t
        GameRegistry.registerTileEntity(t, getUnlocalizedName+"|"+meta)
    }

    def addSingleTile[A <: InstancedBlockTile](t:Class[A]){addTile(t, 0); singleTile = true}

    override def hasTileEntity(metadata:Int) = tiles.exists(_ != null)

    override def isOpaqueCube = false

    override def renderAsNormalBlock = false

    override def damageDropped(damage:Int) = damage

    override def harvestBlock(w:World, player:EntityPlayer, x:Int, y:Int, z:Int, l:Int){}

    override def getRenderType = TileRenderRegistry.renderID

    @SideOnly(Side.CLIENT)
    override def registerBlockIcons(reg:IIconRegister){TileRenderRegistry.registerIcons(this, reg)}

    override def getIcon(side:Int, meta:Int) = TileRenderRegistry.getIcon(this, side, meta)

    @SideOnly(Side.CLIENT)
    override def randomDisplayTick(w:World, x:Int, y:Int, z:Int, rand:Random)
    {
        val md = w.getBlockMetadata(x, y, z)
        val r = TileRenderRegistry.getRenderer(this, md)
        if (r != null) r.randomDisplayTick(w, x, y, z, rand)
    }

    override def createNewTileEntity(var1:World, var2:Int) = null

    override def createTileEntity(world:World, meta:Int) =
    {
        var t:InstancedBlockTile = null
        try {t = if (singleTile) tiles(0).newInstance else tiles(meta).newInstance}
        catch {case e:Exception => e.printStackTrace()}
        if (t != null) t.prepair(meta)
        t
    }

    override def removedByPlayer(world:World, player:EntityPlayer, x:Int, y:Int, z:Int) =
    {
        if (world.isRemote) true
        else
        {
            val b = world.getBlock(x, y, z)
            val md = world.getBlockMetadata(x, y, z)
            if (b.canHarvestBlock(player, md) && !player.capabilities.isCreativeMode)
            {
                val il = getDrops(world, x, y, z, md, EnchantmentHelper.getFortuneModifier(player))
                for (it <- il) WorldLib.dropItem(world, x, y, z, it)
            }
            world.setBlockToAir(x, y, z)
            true
        }
    }

    override def getDrops(w:World, x:Int, y:Int, z:Int, meta:Int, fortune:Int) =
    {
        val list = new ListBuffer[ItemStack]
        w.getTileEntity(x, y, z) match
        {
            case t:InstancedBlockTile => t.addHarvestContents(list)
            case _ =>
        }
        new JArrayList[ItemStack](list)
    }

    override def getPickBlock(target:MovingObjectPosition, w:World, x:Int, y:Int, z:Int, p:EntityPlayer) =  w.getTileEntity(x, y, z) match
    {
        case t:InstancedBlockTile => t.getPickBlock
        case _ => super.getPickBlock(target, w, x, y, z, p)
    }

    override def onNeighborBlockChange(w:World, x:Int, y:Int, z:Int, b:Block)
    {
        w.getTileEntity(x, y, z) match
        {
            case t:InstancedBlockTile => t.onNeighborChange(b)
            case _ =>
        }
    }

    def postBlockSetup(w:World, x:Int, y:Int, z:Int, side:Int, meta:Int, player:EntityPlayer, stack:ItemStack, hit:Vector3)
    {
        w.getTileEntity(x, y, z) match
        {
            case t:InstancedBlockTile => t.onBlockPlaced(side, meta, player, stack, hit)
            case _ =>
        }
    }

    override def breakBlock(w:World, x:Int, y:Int, z:Int, b:Block, meta:Int)
    {
        w.getTileEntity(x, y, z) match
        {
            case t:InstancedBlockTile => t.onBlockRemoval()
            case _ =>
        }
        super.breakBlock(w, x, y, z, b, meta)
    }

    override def canConnectRedstone(w:IBlockAccess, x:Int, y:Int, z:Int, side:Int) = w.getTileEntity(x, y, z) match
    {
        case t:InstancedBlockTile => t.canConnectRS
        case _ => super.canConnectRedstone(w, x, y, z, side)
    }

    override def isProvidingStrongPower(w:IBlockAccess, x:Int, y:Int, z:Int, side:Int) = w.getTileEntity(x, y, z) match
    {
        case t:InstancedBlockTile => t.strongPower(side)
        case _ => 0
    }

    override def isProvidingWeakPower(w:IBlockAccess, x:Int, y:Int, z:Int, side:Int) =
        w.getTileEntity(x, y, z) match
        {
            case t:InstancedBlockTile => t.weakPower(side)
            case _ => 0
        }

    override def onBlockActivated(w:World, x:Int, y:Int, z:Int, player:EntityPlayer, side:Int, hx:Float, hy:Float, hz:Float) = w.getTileEntity(x, y, z) match
    {
        case t:InstancedBlockTile => t.onBlockActivated(player, side)
        case _ => false
    }

    override def onBlockClicked(w:World, x:Int, y:Int, z:Int, player:EntityPlayer) = w.getTileEntity(x, y, z) match
    {
        case t:InstancedBlockTile => t.onBlockClicked(player)
        case _ => super.onBlockClicked(w, x, y, z, player)
    }

    override def onEntityCollidedWithBlock(w:World, x:Int, y:Int, z:Int, ent:Entity) = w.getTileEntity(x, y, z) match
    {
        case t:InstancedBlockTile => t.onEntityCollision(ent)
        case _ =>
    }

    override def setBlockBoundsBasedOnState(w:IBlockAccess, x:Int, y:Int, z:Int)
    {
        w.getTileEntity(x, y, z) match
        {
            case t:InstancedBlockTile => t.getBlockBounds.setBlockBounds(this)
            case _ => super.setBlockBoundsBasedOnState(w, x, y, z)
        }
    }

    override def getCollisionBoundingBoxFromPool(w:World, x:Int, y:Int, z:Int) = w.getTileEntity(x, y, z) match
    {
        case t:InstancedBlockTile =>
            val box = t.getCollisionBounds
            if (box != null) box.copy.add(new Vector3(x, y, z)).toAABB
            else null
        case _ => super.getCollisionBoundingBoxFromPool(w, x, y, z)
    }

    override def getLightValue(w:IBlockAccess, x:Int, y:Int, z:Int) = w.getTileEntity(x, y, z) match
    {
        case t:InstancedBlockTile => t.getLightValue
        case _ => super.getLightValue(w, x, y, z)
    }

    override def isFireSource(w:World, x:Int, y:Int, z:Int, side:ForgeDirection) = w.getTileEntity(x, y, z) match
    {
        case t:InstancedBlockTile => t.isFireSource(side.ordinal)
        case _ => super.isFireSource(w, x, y, z, side)
    }

    override def isSideSolid(w:IBlockAccess, x:Int, y:Int, z:Int, side:ForgeDirection) = w.getTileEntity(x, y, z) match
    {
        case t:InstancedBlockTile => t.isSolid(side.ordinal)
        case _ => super.isSideSolid(w, x, y, z, side)
    }

    override def updateTick(w:World, x:Int, y:Int, z:Int, rand:Random) = w.getTileEntity(x, y, z) match
    {
        case t:InstancedBlockTile => t.randomTick(rand)
        case _ => super.updateTick(w, x, y, z, rand)
    }

    @SideOnly(Side.CLIENT)
    override def getSubBlocks(thisItem:Item, tab:CreativeTabs, list:JList[_])
    {
        val itemList = list.asInstanceOf[JList[ItemStack]]
        itemList.add(new ItemStack(thisItem, 1, 0))
        for (i <- 1 until tiles.length) if (tiles(i) != null)
            itemList.add(new ItemStack(thisItem, 1, i))
    }
}

trait TTileOrient extends InstancedBlockTile
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

    def position = new BlockCoord(xCoord, yCoord, zCoord)

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

abstract class InstancedBlockTile extends TileEntity with ICustomPacketTile
{
    protected var schedTick = -1L

    def prepair(meta:Int){}

    def onBlockPlaced(side:Int, meta:Int, player:EntityPlayer, stack:ItemStack, hit:Vector3){}

    def onBlockRemoval(){}

    def onNeighborChange(b:Block){}

    def canConnectRS = false
    def strongPower(side:Int) = 0
    def weakPower(side:Int) = strongPower(side)

    def getLightValue = 0

    def isFireSource(side:Int) = false

    def isSolid(side:Int) = true

    def onBlockActivated(player:EntityPlayer, side:Int) = false

    def onBlockClicked(player:EntityPlayer) = false

    def onEntityCollision(ent:Entity){}

    def getBlockBounds = Cuboid6.full

    def getCollisionBounds = Cuboid6.full

    def onScheduledTick(){}

    def updateClient(){}

    def update(){}

    def randomTick(rand:Random){}

    def getBlock:Block

    def getMetaData = getBlockMetadata

    def getPickBlock = new ItemStack(getBlock, 1, getMetaData)

    def addHarvestContents(ist:ListBuffer[ItemStack])
    {
        ist += getPickBlock
    }

    def world = worldObj
    def x = xCoord
    def y = yCoord
    def z = zCoord

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
        for (stack <- il) WorldLib.dropItem(world, x, y, z, stack)
        world.setBlockToAir(x, y, z)
    }

    override def markDirty()
    {
        world.markTileEntityChunkModified(x, y, z, this)
    }

    final def markRender()
    {
        world.func_147479_m(x, y, z)
    }

    final def markLight()
    {
        world.func_147451_t(x, y, z)
    }

    final def markDescUpdate()
    {
        world.markBlockForUpdate(x, y, z)
    }

    final override def updateEntity()
    {
        if (world.isRemote)
        {
            updateClient()
            return
        }
        else update()
        if (schedTick < 0L) return
        val time = world.getTotalWorldTime
        if (schedTick <= time)
        {
            schedTick = -1L
            onScheduledTick()
            markDirty()
        }
    }

    final override def readFromNBT(tag:NBTTagCompound)
    {
        super.readFromNBT(tag)
        schedTick = tag.getLong("sched")
        load(tag)
    }

    final override def writeToNBT(tag:NBTTagCompound)
    {
        super.writeToNBT(tag)
        tag.setLong("sched", schedTick)
        save(tag)
    }

    def save(tag:NBTTagCompound){}
    def load(tag:NBTTagCompound){}

    final override def getDescriptionPacket =
    {
        val packet = writeStream(0)
        writeDesc(packet)
        if (compressDesc) packet.compress()
        packet.toPacket
    }

    def compressDesc = false

    final def handleDescriptionPacket(packet:PacketCustom) = packet.readUByte() match
    {
        case 0 => readDesc(packet)
        case key => read(packet, key)
    }

    def read(in:MCDataInput, key:Int){}

    def readDesc(in:MCDataInput){}
    def writeDesc(out:MCDataOutput){}

    final def writeStream(key:Int):PacketCustom =
    {
        val stream = new PacketCustom(MrTJPCoreSPH.channel, MrTJPCoreSPH.tilePacket)
        stream.writeCoord(x, y, z).writeByte(key)
        stream
    }

    implicit def streamToSend(out:PacketCustom) = StreamSender(out)
    implicit def sendToStream(send:StreamSender) = send.out

    case class StreamSender(out:PacketCustom)
    {
        def sendToChunk()
        {
            out.sendToChunk(world, x>>4, z>>4)
        }
    }
}