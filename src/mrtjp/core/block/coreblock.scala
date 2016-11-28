/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.block

import codechicken.lib.vec.Vector3
import mrtjp.core.item.ItemDefinition
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{Item, ItemBlock, ItemStack}
import net.minecraft.tileentity.TileEntity
import net.minecraft.util.EnumFacing
import net.minecraft.util.math.BlockPos
import net.minecraft.world.World
import net.minecraftforge.fml.common.registry.GameRegistry

class BlockCore(name:String, registryName:String, mat:Material) extends Block(mat)
{
    setUnlocalizedName(name)
    GameRegistry.register(this.setRegistryName(registryName))

    def getItemBlockClass:Class[_ <: ItemBlock] = classOf[ItemBlockCore]

    def bindTile[A <: TileEntity](c:Class[A])
    {
        GameRegistry.registerTileEntity(c, getRegistryName.toString)
    }
}

class ItemBlockCore(b:Block) extends ItemBlock(b)
{
    setHasSubtypes(true)
    setMaxDamage(0)
    GameRegistry.register(this.setRegistryName(b.getRegistryName))

    override def getMetadata(meta:Int) = meta

    override def getUnlocalizedName(stack:ItemStack) = super.getUnlocalizedName+"|"+stack.getItemDamage

    override def placeBlockAt(stack:ItemStack, player:EntityPlayer, w:World, pos:BlockPos, side:EnumFacing, hitX:Float, hitY:Float, hitZ:Float, newState:IBlockState) =
    {
        val a = super.placeBlockAt(stack, player, w, pos, side, hitX, hitY, hitZ, newState)
        block match {
            case b:MultiTileBlock =>
                b.postBlockSetup(w, pos, side.ordinal, player, stack, new Vector3(hitX, hitY, hitZ))
            case _ =>
        }
        a
    }
}

abstract class BlockDefinition extends ItemDefinition
{
    override type EnumVal <: BlockDef

    override def getItem = Item.getItemFromBlock(getBlock)
    def getBlock:Block

    class BlockDef extends ItemDef
}