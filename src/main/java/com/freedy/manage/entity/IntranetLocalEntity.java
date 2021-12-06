package com.freedy.manage.entity;

import com.freedy.Struct;
import com.freedy.tinyFramework.annotation.beanContainer.Part;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Freedy
 * @date 2021/12/1 16:59
 */

@Data
@NoArgsConstructor
@Part
public class IntranetLocalEntity {
    private Boolean isLocalStart;
    private Long startTime;
    private Integer intranetChannelCacheMinSize;
    private Integer intranetChannelCacheMaxSize;
    private String portChannelCacheLbName;
    private Integer intranetChannelRetryTimes;
    private Integer intranetReaderIdleTime;
    private Integer intranetReaderIdleTimes;
    private Integer intranetMaxBadConnectTimes;
    private Integer intranetServerZeroChannelIdleTime;
    private Struct.ConfigGroup[] intranetGroups;
}
