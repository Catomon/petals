package ctmn.petals.multiplayer.client

import com.badlogic.gdx.utils.Logger
import ctmn.petals.GameConst
import ctmn.petals.multiplayer.JsonMessageDecoder
import ctmn.petals.multiplayer.JsonMessageEncoder
import ctmn.petals.multiplayer.json.serverres.Disconnected
import ctmn.petals.multiplayer.toJsonMessage
import io.netty.bootstrap.Bootstrap
import io.netty.channel.ChannelFuture
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelOption
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.util.concurrent.DefaultEventExecutor

val logger = Logger(GameClient::class.java.simpleName, Logger.DEBUG)

class GameClient(val host: String = ConnectionData.host, val port: Int = ConnectionData.port) {

    object ConnectionData {
        var host: String = "localhost"
        var port: Int = GameConst.SERVER_PORT
    }

    val clientManager = ClientManager(this)
    val handler = ServerHandler(clientManager)
    private var isRunning = false

    @Synchronized
    fun running(): Boolean {
        return isRunning
    }

    lateinit var channelFuture: ChannelFuture

    fun run() {
        // Create the event loop group
        val workerGroup = NioEventLoopGroup()

        // Start the client channel
        val bootstrap = Bootstrap()
        bootstrap.group(workerGroup)
            .channel(NioSocketChannel::class.java)
            .option(ChannelOption.SO_KEEPALIVE, true)
            .handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    ch.pipeline().addLast(
                        JsonMessageEncoder(),
                        JsonMessageDecoder(),
                        handler
                    )
                }
            })

        channelFuture = bootstrap.connect(host, port)

        // Use a listener to be notified when the client channel becomes connected
        channelFuture.addListener { future ->
            isRunning = if (future.isSuccess) {
                logger.info("Client Channel is connected.")
                true
            } else {
                logger.error("Failed to connect client channel.", future.cause())
                false
            }
        }

        // Use SingleThreadEventExecutor to wait for the client channel to connect
        val executor = DefaultEventExecutor(workerGroup)
        executor.execute {
            while (!channelFuture.isDone) {
                Thread.sleep(100)
            }

            isRunning = if (channelFuture.isSuccess) {
                logger.info("Client Channel is connected.")
                true
            } else {
                logger.error("Failed to connect client channel.", channelFuture.cause())
                false
            }
        }

        // Close the event loop group when the channel is done
        channelFuture.channel().closeFuture().addListener {
            workerGroup.shutdownGracefully()

            isRunning = false

            logger.info("Client Channel closed. Event loop group shutdown gracefully.")
        }
    }

    fun disconnect() {
        clientManager.sendMessage(Disconnected("return_button").toJsonMessage())?.addListener {
            channelFuture.channel().close()
        }
    }
}