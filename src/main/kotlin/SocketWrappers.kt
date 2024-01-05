package com.knowledgespike

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * this.read and this.write take an 'attachment' as the 2nd parameter, this is essentially a 'context' and we
 * can use that attachment later on as the completion handler.
 *
 * In the completion handler we grab the attachment asA continuation and continue the coroutine
 */
class TcpSocket(private val socket: AsynchronousSocketChannel) {
    suspend fun read(buffer: ByteBuffer): Int {
        return socket.asyncRead(buffer)
    }

    suspend fun write(buffer: ByteBuffer): Int {
        return socket.asyncWrite(buffer)
    }

    suspend fun connect(addr: InetSocketAddress): Void? {
        return socket.asyncConnect(addr)
    }

    fun close() {
        socket.close()
    }


    suspend fun AsynchronousSocketChannel.asyncRead(buffer: ByteBuffer): Int {
        return suspendCoroutine { continuation ->
            this.read(buffer, continuation, ReadCompletionHandler)
        }
    }

    suspend fun AsynchronousSocketChannel.asyncWrite(buffer: ByteBuffer): Int {
        return suspendCoroutine { continuation ->
            this.write(buffer, continuation, ReadCompletionHandler)
        }
    }

    suspend fun AsynchronousSocketChannel.asyncConnect(address: InetSocketAddress): Void? {
        return suspendCoroutine { continuation ->
            this.connect(address, continuation, ConnectCompletionHandler)
        }
    }


    object ReadCompletionHandler : CompletionHandler<Int, Continuation<Int>> {
        override fun completed(result: Int, attachment: Continuation<Int>) {
            attachment.resume(result)
        }

        override fun failed(exc: Throwable, attachment: Continuation<Int>) {
            attachment.resumeWithException(exc)
        }
    }

    object ConnectCompletionHandler : CompletionHandler<Void?, Continuation<Void?>> {
        override fun completed(result: Void?, attachment: Continuation<Void?>) {
            attachment.resume(result)
        }

        override fun failed(exc: Throwable, attachment: Continuation<Void?>) {
            attachment.resumeWithException(exc)
        }

    }

    override fun toString(): String {
        return socket.toString()
    }

}

suspend fun AsynchronousServerSocketChannel.asyncAccept(): AsynchronousSocketChannel {
    return suspendCoroutine { continuation ->
        this.accept(continuation, AcceptCompletionHandler(this))
    }
}


class AcceptCompletionHandler(val serverChannel: AsynchronousServerSocketChannel) :
    CompletionHandler<AsynchronousSocketChannel, Continuation<AsynchronousSocketChannel>> {
    override fun completed(result: AsynchronousSocketChannel, attachment: Continuation<AsynchronousSocketChannel>) {
        attachment.resume(result)
    }

    override fun failed(exc: Throwable, attachment: Continuation<AsynchronousSocketChannel>) {
        // process error
    }
}