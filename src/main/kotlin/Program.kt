package com.knowledgespike

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


fun main() = runBlocking {
    var eyballs: HappyEyeballs? = null

    val host = "debian.org"
//    val host = "facebook.com"
//    val host = "identityserver.com"

    eyballs = HappyEyeballs()
    val job = launch(Dispatchers.IO) {
        eyballs.apply {
            openTcpSocket(host, 443)
        }
    }
    job.join()
    println("winning socket ${eyballs.winningSocket}")

}

