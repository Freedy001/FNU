package com.freedy.tinyFramework.beanFactory;

import com.freedy.tinyFramework.BeanDefinitionScanner;
import com.freedy.tinyFramework.annotation.PackageScan;
import com.freedy.tinyFramework.annotation.WebApplication;
import com.freedy.tinyFramework.beanDefinition.BeanDefinition;
import com.freedy.tinyFramework.beanDefinition.NormalBeanDefinition;
import com.freedy.tinyFramework.exception.ApplicationHasStartedException;
import com.freedy.tinyFramework.processor.RestProcessor;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.util.concurrent.DefaultThreadFactory;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Freedy
 * @date 2021/12/2 15:39
 */
@Slf4j
public class Application extends AbstractApplication {

    private final BeanDefinitionScanner scanner;
    private RestProcessor restProcessor;
    private String[] packageName;
    private String[] excludePackage;
    private int port=-1;


    public Application(Class<?> startClass) {
        scanner = new BeanDefinitionScanner(this);
        parseStartClass(startClass);
        setBean(this);
    }


    public Application run() {
        if (packageName == null && excludePackage == null)
            throw new ApplicationHasStartedException("!!!!!!!!!!!!!!application has started!!!!!!!!!!!!!!");
        scan(packageName, excludePackage);
        productBean();
        if (port>=0) {
            restProcessor = new RestProcessor(this);
            for (BeanDefinition definition : beanDefinition.values()) {
                if (definition instanceof NormalBeanDefinition nbd && nbd.getIsRest()) {
                    restProcessor.registerController(getBean(definition.getBeanName()));
                }
            }
            registerRestInner(this);
            startServer();
        }
        packageName = null;
        excludePackage = null;
        return this;
    }


    private void startServer() {
        new ServerBootstrap().group(new NioEventLoopGroup(1),
                        new NioEventLoopGroup(0, new DefaultThreadFactory("manager-server")))
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG, 10240)
                .childHandler(new ChannelInitializer<>() {
                    @Override
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addLast(
                                new HttpRequestDecoder(),
                                new HttpResponseEncoder(),
                                new HttpObjectAggregator(Integer.MAX_VALUE),
                                restProcessor
                        );
                    }
                })
                .bind(port);
        log.info("manager server start succeed on address: http://127.0.0.1:{}/", port);
    }


    public void registerRestInner(Object obj) {
        if (restProcessor == null) throw new IllegalArgumentException("please start rest server first!");
        restProcessor.registerInnerObj(obj);
    }


    public void scan(String... packageNames) {
        scanner.scan(packageNames);
    }

    public void refresh(){
        productBean();
        for (BeanDefinition definition : beanDefinition.values()) {
            if (definition instanceof NormalBeanDefinition nbd && nbd.getIsRest()) {
                restProcessor.registerController(getBean(definition.getBeanName()));
            }
        }
    }

    public void scan(String[] PackageName, String[] exclude) {
        scanner.scan(PackageName, exclude);
    }


    private void parseStartClass(Class<?> startClass) {
        PackageScan packageScan = startClass.getAnnotation(PackageScan.class);
        if (packageScan != null) {
            packageName = packageScan.value();
            excludePackage = packageScan.exclude();
        } else {
            String fullClassName = startClass.getName();
            packageName = new String[]{fullClassName.substring(0, fullClassName.lastIndexOf("."))};
        }
        WebApplication webApplication = startClass.getAnnotation(WebApplication.class);
        if (webApplication != null) {
            int port = webApplication.port();
            if (port <0){
                throw new IllegalArgumentException("server port must between 0 and 65535");
            }
            this.port=port;
        }
    }


    @Override
    protected void beanDefinitionPostProcess(BeanDefinition beanTypeDefinition) {

    }

    @Override
    protected Object beanBeforeCreatedPostProcess(BeanDefinition definition) {
        return null;
    }
}
