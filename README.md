# FNU(Freedy's net utils)

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

1. 下载zip包(或手动下载)
    ```shell
    wget https://github.com/Freedy001/FNU/releases/download/2.0.0/fnu2.0.0.zip
    ```

2. 解压(或手动解压)
    ```shell
    wget https://github.com/Freedy001/FNU/releases/download/1.0.0/conf.properties
    ```

3. 修改配置文件(详细配置见下面配置文件专栏)
4. 运行start.sh/start.bat启动

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
proxy.reverse.enabled=false
# 反向代理服务器启动端口
proxy.reverse.port=2020
# 表示是否是反向代理的终端节点
# 当配置有多个反向代理的时候只有终端节点需要对数据进行加密与解密
# 若为单机反向代理请务必设为false应该设为true会对代理的数据进行加密与解密，可能导致服务器收到的数据为加密数据
proxy.reverse.jumpEndPoint=true
# 需要被反向代理服务的地址多个用(,)隔开
proxy.reverse.serverAddress=127.0.0.1:1010
# 负载均衡算法，默认轮询(Round Robin,Weighted Round Robin,Source IP Hash,Random)
proxy.reverse.BalanceAlgorithm=Round Robin
# 加权轮询时每个地址的权重,其数量必须和reverse.proxy.server.address的数量一致
proxy.reverse.BalanceAlgorithmWeight=1,1,1,1
```

### HTTP代理

- 普通代理

```properties
#是否启用
proxy.http.enabled=false
#启动端口
proxy.http.port=1010
# 表示是否是HTTP代理的终端节点
# 同上proxy.reverse.jumpEndPoint属性
proxy.http.jumpEndPoint=true
```

### 内网穿透

- 客户端配置

```properties
#是否启用
intranet.local.enabled=false
#管道缓存的最小数量
intranet.local.minChannelCount=5
#管道缓存的最大数量
intranet.local.maxChannelCount=999999
#内网穿透服务端的地址(多个用英文逗号(,)分割)
intranet.local.group.localServerAddress=106.14.177.142:80
#内网穿透服务端的地址(多个用英文逗号(,)分割)
intranet.local.group.remoteIntranetAddress=127.0.0.1:7777
#期望被穿透的服务在远程启用的端口(多个用英文逗号(,)分割)
intranet.local.group.remoteServerPort=9999
```

`管道缓存的最小数量`   `管道缓存的最大数量` 一般就用默认设置,他会根据访问量进行动态扩容与缩容。

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
intranet.remote.enabled=false
# 服务端启动的端口
intranet.remote.port=7777
# 对管道缓存池中的管道采用的负载均衡算法
intranet.remote.loadBalancing=Round Robin
```

负载均衡算法在以下可选(一般都用第一个，其他的没做测试可能有bug)

1. Round Robin (轮询算法,默认为该算法)

2. Random (随机算法)
3. Source IP Hash (源ip的hash取模算法,保证同一ip每次都负载到相同的管道)   在这里不能使用此算法
4. Weighted Round Robin (加权轮询) 暂未实现


### 加密配置

```properties
#是否启用
encryption.enabled=false
#采用aes对称加密  密钥
encryption.aesKey=$@!;POI{}2?.+fs=
# 认证头信息MD5加密次数,其工作原理是对aes密钥进行多次MD5加密
encryption.authenticationTime=3
```

### 开发者选项(启动远程调试功能)
```properties
#是否启动
expression.enabled=true
#启动端口
expression.port=98
#对称加密aesKey
expression.aesKey=abcdefrtgszxcsqw
#消息头认证key
expression.auth=asfasfasfasfasx
```

### 完整配置

```properties
# suppress inspection "UnusedProperty" for whole file
#============================== 内网穿透客户端 ==============================
intranet.local.enabled=false
intranet.local.minChannelCount=5
intranet.local.maxChannelCount=999999
intranet.local.group.localServerAddress=106.14.177.142:80,127.0.0.1:98
intranet.local.group.remoteIntranetAddress=127.0.0.1:7777,127.0.0.1:7777
intranet.local.group.remoteServerPort=9999,7788
#============================== 内网穿透服务端 ==============================
intranet.remote.enabled=false
intranet.remote.port=7777
intranet.remote.loadBalancing=Round Robin

#============================== 反向代理 ==============================
proxy.reverse.enabled=false
proxy.reverse.port=2020
proxy.reverse.jumpEndPoint=true
proxy.reverse.serverAddress=127.0.0.1:1010
proxy.reverse.BalanceAlgorithm=Round Robin
proxy.reverse.BalanceAlgorithmWeight=1,1,1,1

#============================== http代理 ==============================
proxy.http.enabled=false
proxy.http.port=1010
proxy.http.jumpEndPoint=true

#============================== 加密配置 ==============================
encryption.enabled=false
encryption.aesKey=$@!;POI{}2?.+fs=
encryption.authenticationTime=3


#============================== expression ==============================
expression.enabled=true
expression.port=98
expression.aesKey=abcdefrtgszxcsqw
expression.auth=asfasfasfasfasx
```
