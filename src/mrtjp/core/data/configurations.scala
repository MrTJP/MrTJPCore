/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.data

import java.io.File
import java.util.{ArrayList => JAList}

import cpw.mods.fml.client.IModGuiFactory
import cpw.mods.fml.client.IModGuiFactory.RuntimeOptionCategoryElement
import cpw.mods.fml.client.config.DummyConfigElement.DummyCategoryElement
import cpw.mods.fml.client.config.{GuiConfig, IConfigElement}
import cpw.mods.fml.client.event.ConfigChangedEvent
import cpw.mods.fml.common.eventhandler.SubscribeEvent
import cpw.mods.fml.common.{FMLCommonHandler, Loader}
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.common.config.{ConfigElement, Configuration, Property}

import scala.collection.JavaConversions._

abstract class ModConfig(modID:String)
{
    var config:Configuration = null

    protected case class BaseCategory(key:String, comment:String = "")
    {
        def cat = config.getCategory(key)
        cat.setComment(comment)

        def put[T](key:String, value:T, force:Boolean):T =
            put(key, value, "", force)

        def put[T](key:String, value:T, comment:String = ""):T =
            put(key, value, comment, false)

        def put[T](key:String, value:T, comment:String, force:Boolean):T =
        {
            import net.minecraftforge.common.config.Property.Type._
            def getType(value:Any):Property.Type = value match
            {
                case xs:Array[_] => getType(xs.head)
                case b:Boolean   => BOOLEAN
                case i:Int       => INTEGER
                case s:String    => STRING
                case d:Double    => DOUBLE
                case _           => STRING
            }

            val propType = getType(value)
            var prop = value match
            {
                case t:Array[_] => new Property(key, t.map(_.toString), propType)
                case _ => new Property(key, value.toString, propType)
            }

            prop.comment = comment
            if (force || !cat.containsKey(key)) cat.put(key, prop)
            prop = cat.get(key)

            val reslult = value match
            {
                case xs:Array[_]    => propType match
                {
                    case BOOLEAN    => prop.getBooleanList
                    case INTEGER    => prop.getIntList
                    case STRING     => prop.getStringList
                    case DOUBLE     => prop.getDoubleList
                    case _          => prop.getStringList
                }
                case b:Boolean      => prop.getBoolean
                case i:Int          => prop.getInt
                case s:String       => prop.getString
                case d:Double       => prop.getDouble
                case _              => prop.getString
            }
            reslult.asInstanceOf[T]
        }

        def containsKey(key:Any) = cat.containsKey(key.toString)
    }

    def getFileName = modID

    private var registered = false
    def loadConfig()
    {
        config = new Configuration(new File(Loader.instance.getConfigDir, getFileName+".cfg"))
        initValues()
        if (config.hasChanged) config.save()

        if (!registered)
        {
            FMLCommonHandler.instance.bus.register(this)
            registered = true
        }
    }

    @SubscribeEvent
    def onConfigChanged(event:ConfigChangedEvent.OnConfigChangedEvent)
    {
        if (event.modID == modID)
        {
            initValues()
            config.save()
        }
    }

    protected def initValues()
}

object SpecialConfigGui
{
    def buildCategories(config:Configuration) =
        new JAList[IConfigElement[_]](config.getCategoryNames.map(s =>
        {
            new DummyCategoryElement(s, "", new ConfigElement(config.getCategory(s)).getChildElements)
            {
                override def getComment = config.getCategory(s).getComment
            }
        }))
}

class SpecialConfigGui(parent:GuiScreen, modid:String, config:Configuration) extends GuiConfig(parent, SpecialConfigGui.buildCategories(config), modid, false, false, GuiConfig.getAbridgedConfigPath(config.toString))

trait TModGuiFactory extends IModGuiFactory
{
    override def initialize(minecraftInstance: Minecraft){}
    override def runtimeGuiCategories() = null
    override def getHandlerFor(element:RuntimeOptionCategoryElement) = null
}