/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.color

import codechicken.lib.colour.ColourRGBA
import codechicken.lib.render.ColourMultiplier
import cpw.mods.fml.relauncher.{Side, SideOnly}
import mrtjp.core.util.Enum
import net.minecraft.init.Items
import net.minecraft.item.ItemStack
import net.minecraftforge.oredict.OreDictionary
import org.lwjgl.opengl.GL11

object Colors extends Enum
{
    override type EnumVal = Color

    val WHITE = new Color("White", 0xFFFFFF)
    val ORANGE = new Color("Orange", 0xC06300)
    val MAGENTA = new Color("Magenta", 0xB51AB5)
    val LIGHT_BLUE = new Color("Light Blue", 0x6F84F1)
    val YELLOW = new Color("Yellow", 0xBFBF00)
    val LIME = new Color("Lime", 0x6BF100)
    val PINK = new Color("Pink", 0xF14675)
    val GREY = new Color("Grey", 0x535353)
    val LIGHT_GREY = new Color("Light Grey", 0x939393)
    val CYAN = new Color("Cyan", 0x008787)
    val PURPLE = new Color("Purple", 0x5E00C0)
    val BLUE = new Color("Blue", 0x1313C0)
    val BROWN = new Color("Brown", 0x4F2700)
    val GREEN = new Color("Green", 0x088700)
    val RED = new Color("Red", 0xA20F06)
    val BLACK = new Color("Black", 0x1F1F1F)

    private val dyeDictionary = Seq("dyeBlack", "dyeRed", "dyeGreen", "dyeBrown", "dyeBlue", "dyePurple", "dyeCyan",
        "dyeLightGray", "dyeGray", "dyePink", "dyeLime", "dyeYellow", "dyeLightBlue", "dyeMagenta", "dyeOrange", "dyeWhite")

    private val mixMap = Map(
        //???               -> WHITE
        Set(YELLOW, RED)    -> ORANGE,
        Set(PINK, PURPLE)   -> MAGENTA,
        Set(WHITE, BLUE)    -> LIGHT_BLUE,
        //???               -> YELLOW
        Set(WHITE, GREEN)   -> LIME,
        Set(WHITE, RED)     -> PINK,
        Set(WHITE, BLACK)   -> GREY,
        Set(WHITE, GREY)    -> LIGHT_GREY,
        Set(BLUE, GREEN)    -> CYAN,
        Set(BLUE, RED)      -> PURPLE,
        //???               -> BLUE
        Set(ORANGE, RED)    -> BROWN,
        Set(YELLOW, BLUE)   -> GREEN
        //???               -> RED
        //???               -> BLACK
    )

    def mcMix(c1:Color, c2:Color) =
    {
        if (c1 == c2) c1
        else mixMap.getOrElse(Set(c1, c2), null)
    }

    def fromWoolID(id:Int) = apply(id)
    def fromDyeID(id:Int) = apply(15-id)
    def fromOreDict(id:String) = if (dyeDictionary.contains(id))
        fromDyeID(dyeDictionary.indexOf(id)) else null
    def fromStack(stack:ItemStack) =
    {
        val ids = OreDictionary.getOreIDs(stack)
        ids.map(id => fromOreDict(OreDictionary.getOreName(id))).find(_ != null).orNull
    }

    class Color(override val name:String, val rgb:Int) extends Value
    {
        val rgba:Int = rgba(0xFF)
        val argb:Int = argb(0xFF)
        val c = new ColourRGBA(rgba)

        val rF = (rgb>>16&255)/255.0F
        val gF = (rgb>>8&255)/255.0F
        val bF = (rgb&255)/255.0F

        def rgba(a:Int):Int = rgb<<8|a&0xFF
        def argb(a:Int):Int = (a&0xFF)<<24|rgb

        lazy val dyeID = 15-ordinal
        lazy val woolID = ordinal

        lazy val oreDict = dyeDictionary(dyeID)

        def makeDyeStack:ItemStack = makeDyeStack(1)
        def makeDyeStack(i:Int):ItemStack = new ItemStack(Items.dye, i, dyeID)

        @SideOnly(Side.CLIENT)
        def setGL11Color(alpha:Float)
        {
            GL11.glColor4f(rF, gF, bF, alpha)
        }

        @SideOnly(Side.CLIENT)
        def getVertOp = ColourMultiplier.instance(rgba)

        def mcMix(that:Color) = Colors.mcMix(this, that)
    }
}