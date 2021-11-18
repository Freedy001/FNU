package com.freedy;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

/**
 * 报文样式: <br/>
 * <pre>
 *      +------------------------------------------+
 *      |      4 byte    |      32byte    | n byte |
 *      |----------------|----------------|--------|
 *      | message length | authentication |  data  |
 *      +------------------------------------------+
 * </pre>
 * message length: 报文长度  <br/>
 * authentication: 认证信息 通过对AES KEY进行3次MD5加密得出。 <br/>
 * message length: 真正数据 需要被转发的报文数据,并使用AES对称加密 <br/>
 *
 * @author Freedy
 * @date 2021/11/10 16:00
 */
public class AuthenticAndEncrypt extends MessageToByteEncoder<ByteBuf> {

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
        byte[] bytes = new byte[msg.readableBytes()];
        msg.readBytes(bytes);
        // authentication
        out.writeBytes(Context.AUTHENTICATION);
        // data
        out.writeBytes(EncryptUtil.Encrypt(bytes, Context.AES_KEY));
    }
}
