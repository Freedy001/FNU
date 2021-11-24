package com.freedy.intranetPenetration.instruction;

import com.freedy.intranetPenetration.ForwardTask;
import com.freedy.intranetPenetration.OccupyState;
import com.freedy.intranetPenetration.Protocol;
import com.freedy.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;

import java.util.Queue;

/**
 * @author Freedy
 * @date 2021/11/23 19:48
 */
public class HeartBeatLocalNormalMsgHandler implements InstructionHandler {

    @Override
    public Boolean apply(ChannelHandlerContext ctx, String[] s) {
        //发送 roger表示服务端收到消息
        Channel intranetChannel = ctx.channel();
        ChannelUtils.setCmdAndSendIfAbsent(intranetChannel, Protocol.HEARTBEAT_REMOTE_NORMAL_MSG);

        OccupyState occupy = ChannelUtils.getOccupy(intranetChannel);
        if (occupy.isOccupy()) return true;
        Queue<ForwardTask> taskQueue = occupy.getTaskQueue();
        if (taskQueue.size() == 0) return true;
        ForwardTask task = taskQueue.poll();
        if (occupy.tryOccupy(task.receiverChannel())) {
            ctx.executor().execute(() -> task.execute(intranetChannel));
            occupy.executeSameReceiverTask(task);
        } else {
            occupy.submitTask(task);
        }

        return true;
    }
}
