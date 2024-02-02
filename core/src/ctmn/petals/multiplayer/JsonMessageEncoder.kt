package ctmn.petals.multiplayer

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToByteEncoder

class JsonMessageEncoder : MessageToByteEncoder<JsonMessage>() {

    private val charset = Charsets.UTF_8

    override fun encode(ctx: ChannelHandlerContext, jsonMessage: JsonMessage, out: ByteBuf) {
        val msg = jsonMessage.message
        out.writeInt(msg.length)
        out.writeCharSequence(msg, charset)
    }
}
