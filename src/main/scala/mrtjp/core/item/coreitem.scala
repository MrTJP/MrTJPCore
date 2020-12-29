/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.item

import java.util
import java.util.List

import mrtjp.core.util.Enum
import net.minecraft.block.SoundType
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.{Item, ItemStack}
import net.minecraft.util.math.BlockPos
import net.minecraft.util._
import net.minecraft.world.World
import net.minecraftforge.fml.common.registry.GameRegistry
import net.minecraftforge.fml.relauncher.{Side, SideOnly}
import java.util.{List => JList}

import scala.collection.JavaConversions._

class ItemCore extends Item
{
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

    private var metaToDef = Map[Int, EnumVal]()

    def fromMeta(meta:Int):EnumVal = metaToDef.getOrElse(meta, null.asInstanceOf[EnumVal])

    def createStringList():JList[String] = {
        val l: JList[String] = new util.ArrayList[String](values.size)
        for (d <- values) {
            l.add(d.ordinal, d.getVariantName.toLowerCase)
        }
        l
    }

    class ItemDef(variantName:String) extends Value with IStringSerializable
    {
        val meta = ordinal

        metaToDef += meta -> this.asInstanceOf[EnumVal]

        override def name = getItem.getUnlocalizedName(makeStack)
        def getVariantName:String = variantName

        override def getName: String = variantName.toLowerCase

        def makeStack:ItemStack = makeStack(1)
        def makeStack(i:Int) = new ItemStack(getItem, i, meta)
    }
}

