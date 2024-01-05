package com.knowledgespike

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.sync.Mutex
import java.lang.Exception
import java.net.Inet4Address
import java.net.Inet6Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousSocketChannel
import kotlin.math.abs

class HappyEyeballs {

    var winningSocket: TcpSocket? = null
    val lock = Mutex()
    suspend fun openTcpSocket(hostname: String, port: Int, maxWaitTime: Long = 250) {

        val sortedAddress = getSortedAddress(hostname)
        val channels = List(sortedAddress.size) { Channel<Boolean>() }

        coroutineScope {
            attempt(this, 0, maxWaitTime, sortedAddress, port, channels)
        }

    }

    private fun getSortedAddress(hostname: String): List<InetAddress> {
        val addresses = InetAddress.getAllByName(hostname).toList()
        val inet4Addresses = mutableListOf<Inet4Address?>()
        val inet6Addresses = mutableListOf<Inet6Address?>()

        addresses.forEach {
            when (it) {
                is Inet4Address -> {
                    inet4Addresses.add(it)
                }

                is Inet6Address -> {
                    inet6Addresses.add(it)
                }

                else -> {}
            }
        }

        val diffInSize = abs(inet4Addresses.size - inet6Addresses.size)

        if (diffInSize != 0) {
            if (inet4Addresses.size > inet6Addresses.size) {
                val nullList = List<Inet6Address?>(diffInSize) { null }
                inet6Addresses.addAll(nullList)
            } else if (inet4Addresses.size < inet6Addresses.size) {
                val nullList = List<Inet4Address?>(diffInSize) { null }
                inet4Addresses.addAll(nullList)
            }
        }
        check(inet4Addresses.size == inet6Addresses.size)

        val sortedAddress =
            inet6Addresses.zip(inet4Addresses) { i6, i4 -> listOf(i6, i4) }.flatten().filter { it != null }.map { it!! }
        return sortedAddress
    }

private suspend fun attempt(
    scope: CoroutineScope,
    which: Int,
    maxWaitTime: Long,
    addresses: List<InetAddress>,
    port: Int,
    channels: List<Channel<Boolean>>
) {
    // wait for previous to fail or timeout has expired
    if (which > 0) {
        val res = withTimeoutOrNull(maxWaitTime) {
            channels[which - 1].receive()
        }
        if (res == null) {
            println("Attempt $which timed-out")
        } else {
            println("Attempt $which signalled $res")
        }
    }


    // start next attempt
    if (which + 1 < channels.size) {
        scope.launch { attempt(scope, which + 1, maxWaitTime, addresses, port, channels) }
    }

    // try connecting
    try {
        val tcpsocket = TcpSocket(AsynchronousSocketChannel.open())
        tcpsocket.connect(InetSocketAddress(addresses[which], port))

        lock.lock()
        winningSocket = if (winningSocket == null) {
            tcpsocket
        } else {
            tcpsocket.close()
            null
        }
        lock.unlock()
    } catch (e: Throwable) {
        println("Try connecting failed on atttempt $which with exception $e")
        channels[which].send(true)
        channels[which].close()
    }
    if (winningSocket != null) {
        println("On $which, socket is $winningSocket")
        scope.cancel()
    }
}


}