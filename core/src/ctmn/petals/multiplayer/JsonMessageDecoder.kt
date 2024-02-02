package ctmn.petals.multiplayer

import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.ReplayingDecoder

class JsonMessageDecoder : ReplayingDecoder<JsonMessage>() {

    private val charset = Charsets.UTF_8

    override fun decode(ctx: ChannelHandlerContext, inBuf: ByteBuf, out: MutableList<Any>) {

        val length: Int = inBuf.readInt()
        val msg = inBuf.readCharSequence(length, charset).toString()
        out.add(JsonMessage(msg))
    }
}
