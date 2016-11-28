/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.item

import mrtjp.core.util.Enum
import net.minecraft.block.SoundType
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.util.math.BlockPos
import net.minecraft.util.{EnumActionResult, EnumFacing, EnumHand, SoundCategory}
import net.minecraft.world.World
import net.minecraftforge.fml.common.registry.GameRegistry
import net.minecraftforge.fml.relauncher.{Side, SideOnly}

class ItemCore(registryName:String) extends Item
{
    GameRegistry.register(this.setRegistryName(registryName))
    setUnlocalizedName(getRegistryName.toString)

    override def getUnlocalizedName(stack:ItemStack):String =
        if (hasSubtypes) getUnlocalizedName()+"|"+stack.getItemDamage
        else getUnlocalizedName()
}

/**
 * Object that collects defs for all subtypes of this item if it has any.
 * Extend ItemDefinition as enum object.
 */
abstract class ItemDefinition extends Enum
{
    type EnumVal <: ItemDef

    def getItem:Item

    /**
     *
     * Define items here
     *
     */

    private var metaToDef = Map[Int, ItemDef]()

    def getEnumFromMeta(meta:Int) = metaToDef.getOrElse(meta, null)

    class ItemDef extends Value
    {
        metaToDef += meta -> this

        val meta = ordinal

        override def name = getItem.getUnlocalizedName(makeStack)

        def makeStack:ItemStack = makeStack(1)
        def makeStack(i:Int) = new ItemStack(getItem, i, meta)
    }
}

