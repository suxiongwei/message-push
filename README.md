## 基于redis实现的消息推送服务 

> 在公司开发的一个项目中有推送的业务场景，也是我负责设计开发，在实际的使用中还没有遇到业务量大的场景，也就一直没有优化。
> 存在的问题就是当遇到业务高峰时不容易实现服务器的扩容，基于quartz的分布式定时任务复杂性也较高。
> 因此在看了[想不到吧？我是这样用Redis实现消息定时推送的！](https://www.cnblogs.com/linlinismine/p/9214299.html)这篇文章之后，决定自己再把推送系统重新设计。
>
> 基本上实现了文章中的推送流程。

### 运行环境

- jdk：1.8
- quartz版本：2.3.0
- spring-boot版本：2.1.1.RELEASE
- redis 版本：redis-5.0.2
- redis 集群（6个node —> 3个master + 3个slave）
- 开发工具：idea

### 运行步骤

1. 启动redis集群
2. 启动zookeeper服务、kafka服务
3. 运行测试类 [testPush](https://github.com/suxiongwei/message-push/blob/master/src/test/java/PushTest.java)，查看日志输出观察推送效果。

### 待开发
1. 队列数量的动态配置
  文中作者使用了淘宝的diamond进行队列数的动态配置，本项目是基于springboot实现，因此可以使用SpringCloud Config 分布式配置中心
  
2. 推送结果发送到消息队列后数据的持久化。


### redis 集群的搭建
参考文章:
-  [深入剖析Redis - Redis集群模式搭建与原理详解](https://www.jianshu.com/p/84dbb25cc8dc)
-  [redis常用集群方案](https://www.jianshu.com/p/1ecbd1a88924)
- [Redis集群搭建详细教程](https://jingyan.baidu.com/album/bea41d43aa8fe6b4c51be6b0.html?picindex=1)

以下是本人在搭建redis集群过程中的一些笔记

#### 启动redis命令

分别执行```sudo redis-server /usr/local/redis-cluster/6001/redis.conf```从6001 ~ 6006

启动后查看进程情况如下：

```sheel
suxiongwei@Mac  ~/program/redis-5.0.2/src  ps -ef | grep redis
    0 89493     1   0  2:09下午 ??         0:00.15 redis-server 127.0.0.1:6002 [cluster]
    0 89513     1   0  2:09下午 ??         0:00.11 redis-server 127.0.0.1:6001 [cluster]
    0 89523     1   0  2:09下午 ??         0:00.10 redis-server 127.0.0.1:6003 [cluster]
    0 89534     1   0  2:09下午 ??         0:00.08 redis-server 127.0.0.1:6004 [cluster]
    0 89544     1   0  2:09下午 ??         0:00.07 redis-server 127.0.0.1:6005 [cluster]
    0 89554     1   0  2:09下午 ??         0:00.06 redis-server 127.0.0.1:6006 [cluster]
```

#### 组建集群

```sheel
 suxiongwei@Mac  ~/program/redis-5.0.2/src  ./redis-trib.rb create --replicas 1 127.0.0.1:6001 127.0.0.1:6002 127.0.0.1:6003 127.0.0.1:6004 127.0.0.1:6005 127.0.0.1:6006
WARNING: redis-trib.rb is not longer available!
You should use redis-cli instead.

All commands and features belonging to redis-trib.rb have been moved
to redis-cli.
In order to use them you should call redis-cli with the --cluster
option followed by the subcommand name, arguments and options.

Use the following syntax:
redis-cli --cluster SUBCOMMAND [ARGUMENTS] [OPTIONS]

Example:
redis-cli --cluster create 127.0.0.1:6001 127.0.0.1:6002 127.0.0.1:6003 127.0.0.1:6004 127.0.0.1:6005 127.0.0.1:6006 --cluster-replicas 1

To get help about all subcommands, type:
redis-cli --cluster help

 ✘ suxiongwei@Mac  ~/program/redis-5.0.2/src  redis-cli --cluster create 127.0.0.1:6001 127.0.0.1:6002 127.0.0.1:6003 127.0.0.1:6004 127.0.0.1:6005 127.0.0.1:6006 --cluster-replicas 1
>>> Performing hash slots allocation on 6 nodes...
Master[0] -> Slots 0 - 5460
Master[1] -> Slots 5461 - 10922
Master[2] -> Slots 10923 - 16383
Adding replica 127.0.0.1:6004 to 127.0.0.1:6001
Adding replica 127.0.0.1:6005 to 127.0.0.1:6002
Adding replica 127.0.0.1:6006 to 127.0.0.1:6003
>>> Trying to optimize slaves allocation for anti-affinity
[WARNING] Some slaves are in the same host as their master
M: 68243e0d2058e28b4dee4b1070af31b3fdaf2c87 127.0.0.1:6001
   slots:[0-5460] (5461 slots) master
M: 2ae0c8314bbc894b7d007507cd1a5f3387e6bf8b 127.0.0.1:6002
   slots:[5461-10922] (5462 slots) master
M: a3411230e21c8a6b3d91b297319d8b1ca48ba485 127.0.0.1:6003
   slots:[10923-16383] (5461 slots) master
S: b612f3ce3bba3b781e3758fbbd10c57f1ff076f1 127.0.0.1:6004
   replicates 2ae0c8314bbc894b7d007507cd1a5f3387e6bf8b
S: b40fef9ee83e3c02fe39adc0b9f53444c952533b 127.0.0.1:6005
   replicates a3411230e21c8a6b3d91b297319d8b1ca48ba485
S: 1927ccc5581581b9050e34dc9dd02f391054ef31 127.0.0.1:6006
   replicates 68243e0d2058e28b4dee4b1070af31b3fdaf2c87
Can I set the above configuration? (type 'yes' to accept): yes
>>> Nodes configuration updated
>>> Assign a different config epoch to each node
>>> Sending CLUSTER MEET messages to join the cluster
Waiting for the cluster to join
........
>>> Performing Cluster Check (using node 127.0.0.1:6001)
M: 68243e0d2058e28b4dee4b1070af31b3fdaf2c87 127.0.0.1:6001
   slots:[0-5460] (5461 slots) master
   1 additional replica(s)
S: 1927ccc5581581b9050e34dc9dd02f391054ef31 127.0.0.1:6006
   slots: (0 slots) slave
   replicates 68243e0d2058e28b4dee4b1070af31b3fdaf2c87
M: 2ae0c8314bbc894b7d007507cd1a5f3387e6bf8b 127.0.0.1:6002
   slots:[5461-10922] (5462 slots) master
   1 additional replica(s)
S: b40fef9ee83e3c02fe39adc0b9f53444c952533b 127.0.0.1:6005
   slots: (0 slots) slave
   replicates a3411230e21c8a6b3d91b297319d8b1ca48ba485
M: a3411230e21c8a6b3d91b297319d8b1ca48ba485 127.0.0.1:6003
   slots:[10923-16383] (5461 slots) master
   1 additional replica(s)
S: b612f3ce3bba3b781e3758fbbd10c57f1ff076f1 127.0.0.1:6004
   slots: (0 slots) slave
   replicates 2ae0c8314bbc894b7d007507cd1a5f3387e6bf8b
[OK] All nodes agree about slots configuration.
>>> Check for open slots...
>>> Check slots coverage...
[OK] All 16384 slots covered.
```

#### 集群组建完毕我们用客户端连接任意一个节点

```sheel
suxiongwei@Mac  ~/program/redis-5.0.2/src  redis-cli -c -h 127.0.0.1 -p 6001
127.0.0.1:6001>
```

#### 查看集群状态

输入命令"cluster info" 或者"cluster nodes "查看集群状态。可以看到集群已经搭建完毕。

```
 suxiongwei@Mac  ~/program/redis-5.0.2/src  redis-cli -c -h 127.0.0.1 -p 6001
127.0.0.1:6001> cluster info
cluster_state:ok
cluster_slots_assigned:16384
cluster_slots_ok:16384
cluster_slots_pfail:0
cluster_slots_fail:0
cluster_known_nodes:6
cluster_size:3
cluster_current_epoch:6
cluster_my_epoch:1
cluster_stats_messages_ping_sent:138
cluster_stats_messages_pong_sent:143
cluster_stats_messages_sent:281
cluster_stats_messages_ping_received:138
cluster_stats_messages_pong_received:138
cluster_stats_messages_meet_received:5
cluster_stats_messages_received:281
127.0.0.1:6001> cluster nodes
1927ccc5581581b9050e34dc9dd02f391054ef31 127.0.0.1:6006@16006 slave 68243e0d2058e28b4dee4b1070af31b3fdaf2c87 0 1565763493986 6 connected
2ae0c8314bbc894b7d007507cd1a5f3387e6bf8b 127.0.0.1:6002@16002 master - 0 1565763491967 2 connected 5461-10922
b40fef9ee83e3c02fe39adc0b9f53444c952533b 127.0.0.1:6005@16005 slave a3411230e21c8a6b3d91b297319d8b1ca48ba485 0 1565763494993 5 connected
a3411230e21c8a6b3d91b297319d8b1ca48ba485 127.0.0.1:6003@16003 master - 0 1565763494000 3 connected 10923-16383
b612f3ce3bba3b781e3758fbbd10c57f1ff076f1 127.0.0.1:6004@16004 slave 2ae0c8314bbc894b7d007507cd1a5f3387e6bf8b 0 1565763492977 4 connected
68243e0d2058e28b4dee4b1070af31b3fdaf2c87 127.0.0.1:6001@16001 myself,master - 0 1565763493000 1 connected 0-5460
```

#### 进入redis

```
redis-cli -c -h 127.0.0.1 -p 6001
```

#### 模拟放十个key sqk_0 ~ sqk_9 观察值在集群中的分布情况

```
suxiongwei@Mac  ~/program/redis-5.0.2/src  redis-cli -c -h 127.0.0.1 -p 6001
127.0.0.1:6001> keys *
1) "sqk_5"
2) "sqk_1"
3) "sqk_4"
4) "sqk_8"
5) "sqk_0"
127.0.0.1:6001>
 suxiongwei@Mac  ~/program/redis-5.0.2/src  redis-cli -c -h 127.0.0.1 -p 6002
127.0.0.1:6002> keys *
1) "sqk_9"
2) "sqk_6"
3) "sqk_2"
127.0.0.1:6002>
 suxiongwei@Mac  ~/program/redis-5.0.2/src  redis-cli -c -h 127.0.0.1 -p 6003
127.0.0.1:6003> keys *
1) "sqk_7"
2) "sqk_3"
127.0.0.1:6003>
 suxiongwei@Mac  ~/program/redis-5.0.2/src  redis-cli -c -h 127.0.0.1 -p 6004
127.0.0.1:6004> keys *
1) "sqk_2"
2) "sqk_9"
3) "sqk_6"
127.0.0.1:6004>
 suxiongwei@Mac  ~/program/redis-5.0.2/src  redis-cli -c -h 127.0.0.1 -p 6005
127.0.0.1:6005> keys *
1) "sqk_7"
2) "sqk_3"
127.0.0.1:6005>
 suxiongwei@Mac  ~/program/redis-5.0.2/src  redis-cli -c -h 127.0.0.1 -p 6006
127.0.0.1:6006> keys *
1) "sqk_4"
2) "sqk_0"
3) "sqk_8"
4) "sqk_5"
5) "sqk_1"
```

**运行** cluster nodes 可以观察出集群中 有三个master，三个slave，master与slave的对应关系也和上面的执行结果相匹配

```
127.0.0.1:6006> cluster nodes
b612f3ce3bba3b781e3758fbbd10c57f1ff076f1 127.0.0.1:6004@16004 slave 2ae0c8314bbc894b7d007507cd1a5f3387e6bf8b 0 1565766142095 4 connected
b40fef9ee83e3c02fe39adc0b9f53444c952533b 127.0.0.1:6005@16005 slave a3411230e21c8a6b3d91b297319d8b1ca48ba485 0 1565766141000 5 connected
a3411230e21c8a6b3d91b297319d8b1ca48ba485 127.0.0.1:6003@16003 master - 0 1565766140000 3 connected 10923-16383
68243e0d2058e28b4dee4b1070af31b3fdaf2c87 127.0.0.1:6001@16001 master - 0 1565766141089 1 connected 0-5460
2ae0c8314bbc894b7d007507cd1a5f3387e6bf8b 127.0.0.1:6002@16002 master - 0 1565766140079 2 connected 5461-10922
1927ccc5581581b9050e34dc9dd02f391054ef31 127.0.0.1:6006@16006 myself,slave 68243e0d2058e28b4dee4b1070af31b3fdaf2c87 0 1565766137000 6 connected
```



