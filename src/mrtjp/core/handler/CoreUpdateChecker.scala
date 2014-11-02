/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.handler

import mrtjp.core.data.UpdateChecker

class CoreUpdateChecker extends UpdateChecker
{
    override def mavenRootURL = "http://projectredwiki.com/maven"
    override def changelogURL = "http://raw.githubusercontent.com/MrTJP/MrTJPCore/master/resources/Changelog"

    override def group = "mrtjp"
    override def project = "MrTJPCore"

    override def currentVersion = MrTJPCoreMod.version+"."+MrTJPCoreMod.build

    override def checkUnstable = true //TODO

    override def shouldRun = !MrTJPCoreMod.version.contains("@")
}