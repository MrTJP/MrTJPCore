/*
 * Copyright (c) 2014.
 * Created by MrTJP.
 * All rights reserved.
 */
package mrtjp.core.data

import java.io.{BufferedReader, InputStreamReader}
import java.net.{HttpURLConnection, URL}

object IO
{
    def forceRead(url:String) =
    {
        val httpconn = new URL(url).openConnection().asInstanceOf[HttpURLConnection]
        httpconn.addRequestProperty("User-Agent", "Safari/8.0")

        val reader = new BufferedReader(new InputStreamReader(httpconn.getInputStream))
        val builder = new StringBuilder
        var in = ""
        while ({in = reader.readLine(); in} != null) builder.append(in)
        builder.result()
    }

    def readBuffer(url:String) = new BufferedReader(new InputStreamReader(new URL(url).openStream()))
}