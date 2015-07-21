/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.item

import net.minecraftforge.oredict.OreDictionary

class ItemEquality
{
    var matchMeta = true
    var matchNBT = true
    var matchOre = false

    var damageGroup = -1

    def apply(key:ItemKey) =
    {
        val c = new AppliedItemEquality(key)
        c.setFlags(matchMeta, matchNBT, matchOre, damageGroup)
        c
    }

    def setFlags(meta:Boolean, nbt:Boolean, ore:Boolean, group:Int)
    {
        matchMeta = meta
        matchNBT = nbt
        matchOre = ore
        damageGroup = group
    }

    def matches(key1:ItemKey, key2:ItemKey):Boolean =
    {
        if (key1 == null || key1 == null) return key1 == key1

        val stack1 = key1.makeStack(0)
        val stack2 = key2.makeStack(0)

        if (matchOre)
        {
            val a = OreDictionary.getOreIDs(stack1)
            val b = OreDictionary.getOreIDs(stack2)
            if (a.exists(b.contains)) return true
        }

        if (key1.item == key2.item)
        {
            if (matchNBT && key1.tag != key2.tag) return false
            if (matchMeta)
            {
                if (stack1.isItemStackDamageable && stack2.isItemStackDamageable && damageGroup > -1)
                {
                    val percentDamage1 = stack1.getItemDamage.toDouble/stack1.getMaxDamage*100
                    val percentDamage2 = stack2.getItemDamage.toDouble/stack2.getMaxDamage*100
                    val isUpperGroup1 = percentDamage1 >= damageGroup
                    val isUpperGroup2 = percentDamage2 >= damageGroup
                    return isUpperGroup1 == isUpperGroup2
                }
                else return stack1.getItemDamage == stack2.getItemDamage
            }
            return true
        }
        false
    }
}

object ItemEquality
{
    val standard = new ItemEquality
}

class AppliedItemEquality(val key:ItemKey) extends ItemEquality
{
    def matches(key2:ItemKey):Boolean = matches(key, key2)
}