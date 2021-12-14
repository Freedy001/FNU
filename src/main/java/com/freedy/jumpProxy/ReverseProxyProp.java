package com.freedy.jumpProxy;

import com.freedy.Struct;
import com.freedy.loadBalancing.LoadBalance;
import com.freedy.loadBalancing.LoadBalanceFactory;
import com.freedy.tinyFramework.annotation.beanContainer.PostConstruct;
import com.freedy.tinyFramework.annotation.prop.InjectProperties;
import com.freedy.tinyFramework.annotation.prop.NonStrict;
import com.freedy.tinyFramework.annotation.prop.Skip;
import lombok.Data;

/**
 * @author Freedy
 * @date 2021/12/9 10:41
 */
@Data
@NonStrict
@InjectProperties("proxy.reverse")
public class ReverseProxyProp {

    private Boolean enabled=false;
    private Integer port=2000;
    private Boolean jumpEndPoint =false;

    private String BalanceAlgorithm="Round Robin";
    private String[] serverAddress;
    private String[] BalanceAlgorithmWeight;

    @Skip
    private LoadBalance<Struct.IpAddress> reverseProxyLB;


    @PostConstruct
    public void initConfigVal(){
        if (enabled)
        reverseProxyLB= LoadBalanceFactory.produceByAddressAndName(serverAddress,BalanceAlgorithm);
    }


}
