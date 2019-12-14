import com.google.common.collect.Lists;
import com.sxw.MessagePushApp;
import com.sxw.common.constants.RedisKeyConstants;
import com.sxw.common.redis.RedisService;
import com.sxw.service.MessagePushService;
import com.sxw.util.ResultJson;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

@RunWith(SpringRunner.class)
@SpringBootTest(classes={MessagePushApp.class})// 指定启动类
@Slf4j
public class PushTest {
    @Autowired RedisService redisService;
    @Autowired private MessagePushService messagePushService;
    @Autowired private RedisTemplate redisTemplate;

    /**
     * 队列数量
     */
    @Value("${subscribe-queue-size}")
    private Integer SUBSCRIBE_QUEUE_SIZE;

    /**
     * 一次从队列中取出执行的数量
     */
    @Value("${subscribe-queue-pool-size}")
    private Integer SUBSCRIBE_QUEUE_POOL_SIZE;

    @Test
    public void testPush(){
        // 模拟 在 2019-08-11 17:44:52 --> 1565516692 时间有 userCount 个用户订阅
        double subscribeTime = 1565516692;
        addMessageToSubscribeQueue(subscribeTime, getUserIds());
        Set set = null;
        // 已经为空的队列数量
        int emptyQueue = 0;
        // 已经推送的数量
        int pushCount = 0;
        // 模拟每秒的定时任务
        for (;;){
            log.info("-------------push batch：{}-------------",System.currentTimeMillis());
            if (SUBSCRIBE_QUEUE_SIZE <= emptyQueue){
                break;
            }

            String queueKey = getSubscribeQueue();
            log.info("------拉取的队列：{}------",queueKey);

            /**
             * 设置每次的拉取数量时，如果所有的节点执行完某一时间戳的定时任务后，队列中仍有[queueKey]推送任务即任务堆积，
             * 则这些推送任务按目前的推送逻辑将永远无法推送
             *
             * 解决思路
             * 1、一次从队列中取出执行的数量设置的可以大一点 或 启动多个推送的服务
             *   假设一次从队列中取出执行的数量设置为：2000
             *   推送的服务：3个推送的服务节点
             *   定时任务执行间隔为 1秒
             *   一分钟内可以实际推送的数量：2000 * 60 * 3 = 360000(理想情况下：多个推送队列subscribe-queue的推送任务分布均匀)
             * 这个数量已经可以满足绝大部分的需求
             *
             * 2、确保不会有任务堆积，可以再启动一个定时任务，从所有的推送队列中取出小于当前时间戳的推送任务进行推送，
             * 并统计数量作为 调整 推送服务和一次从队列中取出执行的数量 和 推送服务节点数
             */
            set = redisTemplate.opsForZSet().reverseRangeByScore(queueKey, subscribeTime, subscribeTime, 0, SUBSCRIBE_QUEUE_POOL_SIZE);
//            set = redisTemplate.opsForZSet().reverseRangeByScore(queueKey, subscribeTime, subscribeTime);

            if (0 == set.size()){
                log.info("------{} is empty------",queueKey);
                emptyQueue ++;
            }else {
                redisTemplate.opsForZSet().remove(queueKey, set.toArray());

                // 具体的推送逻辑
                set.forEach(push -> messagePushService.pushMsg(push));
                pushCount = pushCount + set.size();
                // 睡眠一秒 模拟每秒的定时任务
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    log.error("error", e);
                }
            }
        }
        log.info("push end:{}",pushCount);
    }

    /**
     * 获取用户id
     * @return
     */
    private List<Integer> getUserIds() {
        // 模拟50个用户
        int userCount = 50;
        List<Integer> userIdList = Lists.newArrayListWithExpectedSize(userCount);
        for (int i = 0; i < 50; i++) {
            userIdList.add(i);
        }
        return userIdList;
    }

    /**
     *
     * @param subscribeTime
     * @param userIdList
     */
    private ResultJson addMessageToSubscribeQueue(double subscribeTime, List<Integer> userIdList) {
        // 循环模拟用户id
        userIdList.forEach(userId ->{
            // 计算存放的队列
            int num = userId % SUBSCRIBE_QUEUE_SIZE;
            // 在一个redis中放多个队列，在实际运行中可配置多个redis，分别放对应的队列
            String subscribeKey = String.format("%s%s", RedisKeyConstants.SUBSCRIBE_QUEUE_KEY, num);
            redisTemplate.opsForZSet().add(subscribeKey, userId, subscribeTime);
        });
        return ResultJson.ok();
    }

    /**
     * 获取需要拉取执行的队列
     * 用一个自增的key确保从不同的队列中拉取消息
     * @return
     */
    private String getSubscribeQueue(){
        return RedisKeyConstants.SUBSCRIBE_QUEUE_KEY + (redisTemplate.opsForValue().increment(RedisKeyConstants.SUBSCRIBE_QUEUE_INCR_KEY) % SUBSCRIBE_QUEUE_SIZE);
    }


}
