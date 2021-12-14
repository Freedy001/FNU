package com.freedy;

import com.freedy.utils.EncryptUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;

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
 * @date 2021/11/10 16:26
 */
@Slf4j
public class AuthenticAndDecrypt extends ByteToMessageDecoder {

    private final BiFunction<ChannelHandlerContext, String, Boolean> cmdInterception;

    private final String aesKey;
    private final byte[] authenticationToken;

    public AuthenticAndDecrypt(String aesKey, byte[] authenticationToken) {
        this.aesKey=aesKey;
        this.authenticationToken=authenticationToken;
        cmdInterception = null;
    }

    public AuthenticAndDecrypt(String aesKey, byte[] authenticationToken,BiFunction<ChannelHandlerContext, String, Boolean>cmdInterception) {
        this.aesKey=aesKey;
        this.authenticationToken=authenticationToken;
        this.cmdInterception = cmdInterception;
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        int readableBytes = in.readableBytes();

        //身份认证
        byte[] authentication = new byte[32];
        in.readBytes(authentication);
        if (!Arrays.equals(authentication, authenticationToken)) {
            log.error("remote channel{} authentic fail!", ctx.channel().remoteAddress());
            ctx.channel().close();
            return;
        }

        //指令解析
        byte[] cmd = new byte[28];
        in.readBytes(cmd);
        Boolean apply = true;
        if (cmdInterception != null && !Arrays.equals(cmd, AuthenticAndEncrypt.emptyCmd)) {
            apply = cmdInterception.apply(ctx, new String(cmd));
        }
        if (apply == null || !apply || readableBytes - Context.CMD_LENGTH - 32 <= 0) return;


        //数据
        byte[] data = new byte[readableBytes - Context.CMD_LENGTH - 32];
        in.readBytes(data);
        out.add(Unpooled.wrappedBuffer(aesKey == null ? data : EncryptUtil.Decrypt(data, aesKey)));
    }
}
