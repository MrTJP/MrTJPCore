/*
 * Copyright (c) 2015.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.fx

import mrtjp.core.fx.particles.CoreParticle

class DelayAction extends ParticleAction
{
    var delay = -1.0

    override def operate(p:CoreParticle, time:Double)
    {
        if (time > delay)
            isFinished = true
    }
}