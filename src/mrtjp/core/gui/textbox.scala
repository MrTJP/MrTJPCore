/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.gui

import codechicken.lib.gui.GuiDraw
import mrtjp.core.vec.{Point, Rect, Size}
import net.minecraft.client.gui.GuiScreen
import net.minecraft.util.ChatAllowedCharacters
import org.lwjgl.input.Keyboard

class SimpleTextboxNode(x:Int, y:Int, w:Int, h:Int, tq:String) extends TNode
{
    def this() = this(0, 0, 0, 0, "")
    def this(x:Int, y:Int, w:Int, h:Int) = this(x, y, w, h, "")

    position = Point(x, y)
    text = tq

    var enabled = true
    var focused = false

    var text = ""
    var phantom = ""
    var allowedcharacters = ""

    var textChangedDelegate = {() => }
    var textReturnDelegate = {() => }
    var focusChangeDelegate = {() => }

    private var cursorCounter = 0

    var size = Size(w, h)
    override def frame = Rect(position, size)

    def setText(t:String)
    {
        val old = text
        text = t
        if (old != text) textChangedDelegate()
    }

    def setFocused(flag:Boolean)
    {
        if (focused != flag)
        {
            focused = flag
            if (focused) cursorCounter = 0
            focusChangeDelegate()
        }
    }

    def canAddChar(c:Char) =
        if (allowedcharacters.isEmpty) ChatAllowedCharacters.isAllowedCharacter(c)
        else allowedcharacters.indexOf(c) >= 0

    override def update_Impl(){cursorCounter += 1}

    override def keyPressed_Impl(c:Char, keycode:Int, consumed:Boolean):Boolean =
    {
        if (enabled && focused && !consumed)
        {
            if (keycode == 1)//esc
            {
                setFocused(false)
                return true
            }

            if (c == '\u0016') //paste
            {
                val s = GuiScreen.getClipboardString
                if (s == null || s.isEmpty) return true
                for (c <- s) if (!tryAddChar(c)) return true
            }

            if (keycode == Keyboard.KEY_RETURN) //enter
            {
                setFocused(false)
                textReturnDelegate()
                return true
            }

            if (keycode == Keyboard.KEY_BACK) tryBackspace() else tryAddChar(c)

            true
        }
        else false
    }

    private def tryAddChar(c:Char):Boolean =
    {
        if (!canAddChar(c)) return false
        val ns = text+c
        if (GuiDraw.getStringWidth(ns) > size.width-8) return false
        setText(ns)
        true
    }

    private def tryBackspace():Boolean =
    {
        if (!text.isEmpty)
        {
            setText(text.substring(0, text.length-1))
            true
        }
        else false
    }

    override def mouseClicked_Impl(p:Point, button:Int, consumed:Boolean) =
    {
        if (!consumed && enabled && rayTest(p))
        {
            setFocused(true)
            if (button == 1) setText("")
            true
        }
        else
        {
            setFocused(false)
            false
        }
    }

    override def drawBack_Impl(mouse:Point, rframe:Float)
    {
        GuiDraw.drawRect(position.x-1, position.y-1, size.width+1, size.height+1, 0xFFA0A0A0)
        GuiDraw.drawRect(position.x, position.y, size.width, size.height, 0xFF000000)

        if (text.isEmpty && phantom.nonEmpty)
            GuiDraw.drawString(phantom, position.x+4, position.y+size.height/2-4, 0x404040)

        val drawText = text+(if (enabled && focused && cursorCounter/6%2 == 0) "_" else "")
        GuiDraw.drawString(drawText, position.x+4, position.y+size.height/2-4, if (enabled) 0xE0E0E0 else 0x707070)
    }
}