package com.freedy;

import com.freedy.utils.EncryptUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.Arrays;
import java.util.List;

/**
 * @author Freedy
 * @date 2021/11/10 16:26
 */
public class AuthenticAndDecrypt extends ByteToMessageDecoder {

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        int readableBytes = in.readableBytes();
        byte[] authentication=new byte[32];
        byte[] data=new byte[readableBytes-32];
        in.readBytes(authentication).readBytes(data);
        if (!Arrays.equals(authentication,Context.AUTHENTICATION)){
            ctx.channel().close();
            return;
        }
        out.add(Unpooled.wrappedBuffer(EncryptUtil.Decrypt(data,Context.AES_KEY)));
    }
}
