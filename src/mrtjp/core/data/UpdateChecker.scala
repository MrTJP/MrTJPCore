/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.data

import java.io.File
import java.net.URL
import javax.xml.bind.Element
import javax.xml.parsers.DocumentBuilderFactory

import scala.xml.XML

trait UpdateChecker
{
    def mavenRoot:String
    def group:String
    def project:String

    def changelog:String

    def shouldRun:Boolean

    def projectRoot = mavenRoot+"/"+group+"/"+project
    def projectMD = projectRoot+"/"+"maven-metadata.xml"

    def test
    {
        val url = new URL(projectMD)
        val is = url.openStream()
        val parser = XML.load(is)
        parser.foreach(cat =>
        {
            val v = (cat \ "versioning").foreach(v =>
            {

            })
        })
    }
}
