package com.freedy.intranetPenetration;

import io.netty.channel.Channel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author Freedy
 * @date 2021/11/19 17:00
 */
@NoArgsConstructor
@AllArgsConstructor
public abstract class ForwardTask{
    @Getter
    @Setter
    protected Channel receiverChannel;

    public abstract void Execute();
}
