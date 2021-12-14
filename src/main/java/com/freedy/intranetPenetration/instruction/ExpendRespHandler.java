package com.freedy.intranetPenetration.instruction;

import com.freedy.intranetPenetration.ForwardTask;
import com.freedy.intranetPenetration.OccupyState;
import com.freedy.loadBalancing.LoadBalance;
import com.freedy.tinyFramework.annotation.beanContainer.Inject;
import com.freedy.tinyFramework.annotation.beanContainer.Part;
import com.freedy.utils.ChannelUtils;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Queue;

/**
 * @author Freedy
 * @date 2021/11/23 19:58
 */
@Slf4j
@Part
public class ExpendRespHandler implements InstructionHandler {

    @Inject("portChannelCache")
    private Map<Integer, LoadBalance<Channel>> portChannelCache;

    @Override
    public Boolean apply(ChannelHandlerContext ctx, String[] param) {
        OccupyState occupy = ChannelUtils.getOccupy(ctx.channel());
        if (param[0].equals("success")) {
            Queue<ForwardTask> taskQueue = occupy.getTaskQueue();
            int port = occupy.getServerPort();
            int totalTasks = taskQueue.size();
            int wakeUpCount = 0;

            doWakeUp:
            if (totalTasks > 0) {
                LoadBalance<Channel> loadBalance = portChannelCache.get(port);
                ForwardTask task = taskQueue.poll();

                if (task == null) break doWakeUp;

                for (Channel channel : loadBalance.getAllSafely()) {
                    if (task == null) break;
                    if (ChannelUtils.getOccupy(channel).tryOccupy(task.receiverChannel())) {
                        ForwardTask finalTask = task;
                        channel.eventLoop().execute(() -> finalTask.execute(channel));
                        wakeUpCount++;
                        //执行队列中所有与该task的receiverChannel同的task
                        wakeUpCount += occupy.executeSameReceiverTask(task);

                        task = taskQueue.poll();
                    }
                }

                if (task != null) {
                    //放回剩余没执行完的任务
                    occupy.submitTask(task);
                }
            }
            log.info("expend {} channels on server {},total tasks:{} wake up tasks:{}.", param[1], port, totalTasks, wakeUpCount);
            /*
             * 在发送扩容命令后，会让channelCache短时间无法发送扩容命令。
             * 主要是防止重复提交扩容命令。
             */
            occupy.unlockExpandCheck();
        } else {
            log.error("expend refused! because channel size has risen to the extreme({})", param[1]);
            //扩容失败 锁定扩容检测
            occupy.lockExpandCheck();
        }
        //无论是否成功都，解锁缩容检测
        occupy.unlockShrinkCheck();
        return true;
    }

}
