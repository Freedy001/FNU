package com.freedy;

import com.freedy.utils.ChannelUtils;
import com.freedy.utils.EncryptUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.util.Objects;

/**
 * 报文样式: <br/>
 * <pre>
 *      +------------------------------------------------------------------+
 *      |      4 byte    |     32 byte    |   default 28 byte   |  n byte  |
 *      |----------------|----------------|---------------------|----------|
 *      | message length | authentication |         cmd         |   data   |
 *      +------------------------------------------------------------------+
 * </pre>
 * message length: 报文长度  <br/>
 * authentication: 认证信息 通过对AES KEY进行3次MD5加密得出。 <br/>
 * cmd: 指令消息
 * message length: 真正数据 需要被转发的报文数据,并使用AES对称加密 <br/>
 *
 * @author Freedy
 * @date 2021/11/10 16:00
 */
public class AuthenticAndEncrypt extends MessageToByteEncoder<ByteBuf> {

    public final static byte[] emptyCmd = new byte[28];

    private final String aesKey;
    private final byte[] authenticationToken;

    public AuthenticAndEncrypt(String aesKey, byte[] authenticationToken) {
        this.aesKey = aesKey;
        this.authenticationToken = authenticationToken;
    }


    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf msg, ByteBuf out) {
        // authentication
        out.writeBytes(authenticationToken);
        // cmd
        Channel channel = ctx.channel();
        byte[] cmd = ChannelUtils.getCmd(channel);
//        System.out.log.println(new String(Objects.requireNonNullElse(cmd, emptyCmd)));
        out.writeBytes(Objects.requireNonNullElse(cmd, emptyCmd));
        ChannelUtils.clearCmd(channel);

        //空数据不写回
        int readableBytes = msg.readableBytes();
        if (readableBytes == 0) return;
        byte[] bytes = new byte[readableBytes];
        msg.readBytes(bytes);
        // data
        out.writeBytes(aesKey == null ? bytes : EncryptUtil.Encrypt(bytes, aesKey));
    }
}
