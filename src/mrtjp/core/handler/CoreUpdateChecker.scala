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
    override def changelogURL = null

    override def group = "mrtjp"
    override def project = "MrTJPCore"

    override def currentVersion = MrTJPCoreMod.version+"."+MrTJPCoreMod.build

    override def checkUnstable = MrTJPConfig.check_unstable

    override def shouldRun = MrTJPConfig.check_versions && !MrTJPCoreMod.version.contains("@")
}