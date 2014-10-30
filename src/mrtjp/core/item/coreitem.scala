/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.item

import cpw.mods.fml.common.registry.GameRegistry
import mrtjp.core.util.Enum
import net.minecraft.block.Block
import net.minecraft.block.Block.SoundType
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.world.World

class ItemCore(name:String) extends Item
{
    setUnlocalizedName(name)
    GameRegistry.registerItem(this, name)

    override def getUnlocalizedName(stack:ItemStack):String =
        if (hasSubtypes) getUnlocalizedName()+"|"+stack.getItemDamage
        else getUnlocalizedName()
}

trait TItemSound extends Item
{
    def getSoundType:SoundType

    abstract override def onItemUse(stack:ItemStack, player:EntityPlayer, w:World, x:Int, y:Int, z:Int, side:Int, f:Float, f2:Float, f3:Float) =
    {
        if (super.onItemUse(stack, player, w, x, y, z, side, f, f2, f3))
        {
            w.playSoundEffect(x+0.5, y+0.5, z+0.5, getSoundType.func_150496_b(),
                getSoundType.getVolume*5.0F, getSoundType.getPitch*0.9F)
            true
        }
        else false
    }
}

trait TItemGlassSound extends TItemSound
{
    override def getSoundType = Block.soundTypeGlass
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

    class ItemDef extends Value
    {
        val meta = ordinal

        override def name = getItem.getUnlocalizedName(makeStack)

        def makeStack:ItemStack = makeStack(1)
        def makeStack(i:Int) = new ItemStack(getItem, i, meta)
    }
}

