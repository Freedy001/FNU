# FNU

## 介绍

一个网络工具集。具有内网穿透(INTRANET PENETRATION)、反向代理(REVERSE PROXY)、HTTP代理(HTTP PROXY)等功能。

## 工作原理(有时间再写)

<pre>
  ______  ____                          ____             _____ _   _ _   _ 
 / / / / |  _ \ _____      _____ _ __  | __ ) _   _     |  ___| \ | | | | |
/ / / /  | |_) / _ \ \ /\ / / _ \ '__| |  _ \| | | |    | |_  |  \| | | | |
\ \ \ \  |  __/ (_) \ V  V /  __/ |    | |_) | |_| |    |  _| | |\  | |_| |
 \_\_\_\ |_|   \___/ \_/\_/ \___|_|    |____/ \__, |    |_|   |_| \_|\___/ 
                                              |___/                        
Author:Freedy Version:1.0.0           GigHub:https://github.com/Freedy001/FNU
</pre>

## 使用

1. 下载jar包
    ```shell
    wget https://github.com/Freedy001/FNU/releases/download/1.0.0/netUtils-1.0.0.jar
    ```

2. 下载配置文件
    ```shell
    wget https://github.com/Freedy001/FNU/releases/download/1.0.0/conf.properties
    ```

3. 修改配置文件(详细配置见下面配置文件专栏)
4. 安装jdk17
5. java -jar netUtils-1.0.0.jar

## 开发

1. 拉取代码

    ```shell
    git clone https://github.com/Freedy001/FNU.git
    ```

2. 项目结构

    <pre>
    |------errorProcessor          错误处理
    |
    |------intranetPenetration     内网穿透
    |      |
    |      |------instruction      指令
    |      |    
    |      |------local            内网穿透客户端
    |      |
    |      |------remote           内网穿透服务端
    |
    |------jumpProxy               反向代理、Http代理
    |      |    
    |      |------local            反向代理
    |      |
    |      |------remote           HTTP代理
    |
    |------loadBalancing           负载均衡
    |
    |------log                     日志样式
    |
    |------utils                   工具集
    |   
    |------AuthenticAndDecrypt     消息解密/权限认证
    |
    |------AuthenticAndEncrypt     消息加密/权限认证
    |
    |------Context                 全局配置
    |
    |------Start                   主启动类
    </pre>


3. 在Start类点击运行

## 配置

### 反向代理

```properties
# 是否启用
reverse.proxy.start=false
# 反向代理服务器启动端口
reverse.proxy.port=8900
# 需要被反向代理服务的地址多个用(,)隔开
reverse.proxy.server.address=127.0.0.1:1345
# 负载均衡算法，默认轮询(Round Robin,Weighted Round Robin,Source IP Hash,Random)
reverse.proxy.loadBalancing.algorithm=Round Robin
# 加权轮询时每个地址的权重,其数量必须和reverse.proxy.server.address的数量一致
reverse.proxy.loadBalancing.algorithm.weight=1,1,1,1
```

### HTTP代理

- 普通代理

```properties
#是否启用
proxy.start=false
#启动端口
proxy.port=9090
```

- 有跳板的http代理

跳板节点配置

```properties
#是否启用
jump.local.start=true
#启动端口
jump.local.server.port=8900
#下一个跳板或者终点服务地址,多个用(,)分割
jump.local.connect.address=127.0.0.1:8100
# 负载均衡算法，默认轮询(Round Robin,Weighted Round Robin,Source IP Hash,Random)
jump.local.loadBalancing.algorithm=Random
# 加权轮询时每个地址的权重,其数量必须和reverse.proxy.server.address的数量一致
jump.local.loadBalancing.algorithm.weight=1,1,1,1
```

终点节点配置

```properties
#是否启用
jump.remote.start=true
#启动端口
jump.remote.server.port=8100
```

### 内网穿透

- 客户端配置

```properties
#是否启用
intranet.local.start=true
#管道缓存的最小数量
intranet.local.cache.channel.minSize=30  	
#管道缓存的最大数量
intranet.local.cache.channel.maxSize=10000 
#需要穿透的本地服务地址
intranet.local.group.localServerAddress=127.0.0.1:4567  
#内网穿透服务端的地址
intranet.local.group.remoteIntranetAddress=127.0.0.1:7777 
#期望被穿透的服务在远程启用的端口
intranet.local.group.remoteServerPort=7892 
```

`管道缓存的最小数量`   `管道缓存的最大数量` 一般就用默认设置.除非并发量比较大且你的服务器足够好,则可以把初始容量设置很大,这样就不会应为管道数量扩容而导致服务访问缓慢.

`intranet.local.group.localServerAddress,intranet.local.group.remoteIntranetAddress,intranet.local.group.remoteServerPort`

可以配置多组,每组用逗号分割开,且每组的数量必须是一致的,例如我想启动两个本地服务对应远程服务端的两个端口就可以用如下配置.

```properties
#本地启动两个服务
intranet.local.group.localServerAddress=127.0.0.1:1234,127.0.0.1:5678
# 对应同一个服务端
intranet.local.group.remoteIntranetAddress=127.0.0.1:7777,127.0.0.1:7777
#在服务端的2345,3456端口分别对外提供本地端对应的127.0.0.1:1234与127.0.0.1:5678的服务
intranet.local.group.remoteServerPort=2345,3456 
```

- 服务端配置

服务端配置比较简单就三个参数

```properties
# 是否启用
intranet.remote.start=true
# 服务端启动的端口
intranet.remote.port=7777
# 对管道缓存池中的管道采用的负载均衡算法
intranet.remote.channel.loadBalancing=Round Robin
```

负载均衡算法在以下可选

1. Round Robin (轮询算法,默认为该算法)

2. Random (随机算法)
3. Source IP Hash (源ip的hash取模算法,保证同一ip每次都负载到相同的管道)   在这里不能使用此算法
4. Weighted Round Robin (加权轮询) 暂未实现


### 加密配置

```properties
#是否启用
encryption.start=true
#采用aes对称加密  密钥
encryption.aes.key=$@!;POI{}2?.++=-
# 认证头信息MD5加密次数,其工作原理是对aes密钥进行多次MD5加密
encryption.authenticationTime=3
```

### 完整配置

```properties
#============================== 本地跳跃Http代理 ==============================
jump.local.start=true
jump.local.server.port=8900
jump.local.connect.address=127.0.0.1:8100
# 负载均衡算法，默认轮询(Round Robin,Weighted Round Robin,Source IP Hash,Random)
jump.local.loadBalancing.algorithm=Random
jump.local.loadBalancing.algorithm.weight=1,1,1,1
#============================== 远程JumpHttp代理 ==============================
jump.remote.start=true
jump.remote.server.port=8100
#============================== Http代理 ==============================
proxy.start=false
proxy.port=9090
#============================== 内网穿透客户端 ==============================
intranet.local.start=true
intranet.local.cache.channel.minSize=1
intranet.local.cache.channel.maxSize=999999
intranet.local.group.localServerAddress=127.0.0.1:4567
intranet.local.group.remoteIntranetAddress=127.0.0.1:7777
intranet.local.group.remoteServerPort=7892
#============================== 内网穿透服务端 ==============================
intranet.remote.start=true
intranet.remote.port=7777
intranet.remote.channel.loadBalancing=Round Robin
#============================== 反向代理 ==============================
reverse.proxy.start=false
reverse.proxy.port=8900
reverse.proxy.server.address=127.0.0.1:1345
# 负载均衡算法，默认轮询(Round Robin,Weighted Round Robin,Source IP Hash,Random)
reverse.proxy.loadBalancing.algorithm=Round Robin
reverse.proxy.loadBalancing.algorithm.weight=1,1,1,1
#============================== 加密配置 ==============================
encryption.start=true
encryption.aes.key=$@@@POI{}2?.++=-
encryption.authenticationTime=3
```
