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
                init(address, weightedRoundRobin);
                return weightedRoundRobin;
            }case "Random"->{
                LoadBalance<Struct.IpAddress> random = new Random<>();
                init(address, random);
                return random;
            }case "Source IP Hash"->{
                LoadBalance<Struct.IpAddress> random = new SourceIPHash<Struct.IpAddress>();
                init(address, random);
                return random;
            }
            default -> {
                LoadBalance<Struct.IpAddress> robin = new RoundRobin<Struct.IpAddress>();
                init(address, robin);
                return robin;
            }
        }
    }

    private static void init(String[] address, LoadBalance<Struct.IpAddress> weightedRoundRobin) {
        for (String adder : address) {
            String[] split = adder.split(":");
            weightedRoundRobin.addElement(new Struct.IpAddress(split[0], Integer.parseInt(split[1])));
        }
    }


    @SafeVarargs
    public static <T> LoadBalance<T> produce(String name, T... element) {
        switch (name) {
            case "Weighted Round Robin" -> {
                LoadBalance<T> weightedRoundRobin = new WeightedRoundRobin<>();
                weightedRoundRobin.setElement(element);
                return weightedRoundRobin;
            }
            case "Random" -> {
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
