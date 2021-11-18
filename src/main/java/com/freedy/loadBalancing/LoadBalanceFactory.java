package com.freedy.loadBalancing;

import com.freedy.Struct;

/**
 * @author Freedy
 * @date 2021/11/16 20:17
 */
public class LoadBalanceFactory {
    public static LoadBalance<Struct.IpAddress> produceByAddressAndName(String[] address, String name){
        switch (name){
            case "Weighted Round Robin"->{
                LoadBalance<Struct.IpAddress> weightedRoundRobin = new WeightedRoundRobin<>();
                weightedRoundRobin.loadAddress(address);
                return weightedRoundRobin;
            }case "Random"->{
                LoadBalance<Struct.IpAddress> random = new Random<>();
                random.loadAddress(address);
                return random;
            }case "Source IP Hash"->{
                LoadBalance<Struct.IpAddress> random = new SourceIPHash<Struct.IpAddress>();
                random.loadAddress(address);
                return random;
            }
            default -> {
                LoadBalance<Struct.IpAddress> robin = new RoundRobin<Struct.IpAddress>();
                robin.loadAddress(address);
                return robin;
            }
        }
    }

    @SafeVarargs
    public static <T> LoadBalance<T> produce(String name, T ...element){
        switch (name){
            case "Weighted Round Robin"->{
                LoadBalance<T> weightedRoundRobin = new WeightedRoundRobin<>();
                weightedRoundRobin.setElement(element);
                return weightedRoundRobin;
            }case "Random"->{
                LoadBalance<T> random = new Random<>();
                random.setElement(element);
                return random;
            }case "Source IP Hash"->{
                LoadBalance<T> random = new SourceIPHash<>();
                random.setElement(element);
                return random;
            }
            default -> {
                LoadBalance<T> robin = new RoundRobin<>();
                robin.setElement(element);
                return robin;
            }
        }
    }
}
