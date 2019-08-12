## 基于redis实现的消息推送服务 

> 在公司开发的一个项目中有推送的业务场景，也是我负责设计开发，在实际的使用中还没有遇到业务量大的场景，也就一直没有优化。
> 存在的问题就是当遇到业务高峰时不容易实现服务器的扩容，基于quartz的分布式定时任务复杂性也较高。基本上实现了文章中的推送流程。
> 因此在看了[想不到吧？我是这样用Redis实现消息定时推送的！](www.cnblogs.com/linlinismine/p/9214299.html)这篇文章之后，决定自己再把推送系统重新设计。

### 运行步骤
1. 开启 redis服务、zookeeper服务、kafka服务
2. 运行测试类 [testPush](https://github.com/suxiongwei/message-push/blob/master/src/test/java/PushTest.java)

