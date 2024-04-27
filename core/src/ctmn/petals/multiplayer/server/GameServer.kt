package ctmn.petals.multiplayer.server

import com.badlogic.gdx.utils.Logger
import ctmn.petals.Const
import ctmn.petals.multiplayer.JsonMessageDecoder
import ctmn.petals.multiplayer.JsonMessageEncoder
import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.*
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioServerSocketChannel

val logger = Logger(GameServer::class.java.simpleName, Logger.DEBUG)

class GameServer(private val port: Int = ConnectionData.port) {

    object ConnectionData {
        var port: Int = Const.SERVER_PORT
    }

    val clientRequestsQueue = ClientRequestsQueue()
    val clientsController = ClientsController(clientRequestsQueue)

    lateinit var channelFuture: ChannelFuture

    fun run() {
        // Create the event loop group
        val eventLoopGroup = NioEventLoopGroup()

        // Start the server channel
        val bootstrap = ServerBootstrap()
        bootstrap.group(eventLoopGroup)
            .channel(NioServerSocketChannel::class.java)
            .childHandler(object : ChannelInitializer<SocketChannel>() {
                public override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(
                        JsonMessageEncoder(),
                        JsonMessageDecoder(),
                        clientsController.addClient(ClientHandler(clientsController))
                    )
                }
            }).option(ChannelOption.SO_BACKLOG, 128)
            .childOption(ChannelOption.SO_KEEPALIVE, true)

        // o(￣▽￣)ｄ
        channelFuture = bootstrap.bind(port).addListener { future ->
            if (future.isSuccess) {
                logger.info("Server Channel is active.")
                futureResult(eventLoopGroup)
            } else {
                logger.error("Failed to start server channel. Trying port ${port + 1}", future.cause())

                channelFuture = bootstrap.bind(port + 1).addListener { future ->
                    if (future.isSuccess) {
                        logger.info("Server Channel is active.")
                        futureResult(eventLoopGroup)
                    } else {
                        logger.error("Failed to start server channel. Trying port ${port + 2}", future.cause())

                        channelFuture = bootstrap.bind(port + 2).addListener { future ->
                            if (future.isSuccess) {
                                logger.info("Server Channel is active.")
                                futureResult(eventLoopGroup)
                            } else {
                                logger.error("Failed to start server channel.", future.cause())

                                futureResult(eventLoopGroup)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun futureResult(eventLoopGroup: EventLoopGroup) {
        // Start the request listener
        val executor = eventLoopGroup.next()
        executor.execute {
            logger.info("Looking for requests...")
            while (!channelFuture.isDone) {
                Thread.sleep(100)

                try {
                    if (!clientRequestsQueue.requestsQueue.isEmpty()) {
                        clientRequestsQueue.performRequest(
                            clientRequestsQueue.requestsQueue.first().first,
                            clientRequestsQueue.requestsQueue.first().second
                        )
                        clientRequestsQueue.requestsQueue.removeFirst()
                    }
                } catch (e: Exception) {
                    logger.error("Failed to perform request.", e)
                    clientRequestsQueue.requestsQueue.removeFirst()
                }
            }
        }

        // Close the event loop group when the channel is done
        channelFuture.channel().closeFuture().addListener {
            eventLoopGroup.shutdownGracefully()
            logger.info("Server channel closed. Event loop group shutdown gracefully.")
        }
    }

    fun shutdown() {
        clientsController.disconnectAllClients(ClientsController.REASON_SHUTDOWN)
        channelFuture.channel().close()
    }
}