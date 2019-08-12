import com.sxw.MessagePushApp;
import com.sxw.common.constants.RedisKeyConstants;
import com.sxw.common.redis.RedisService;
import com.sxw.service.MessagePushService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.RedisZSetCommands;
import org.springframework.data.redis.core.DefaultTypedTuple;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

@RunWith(SpringRunner.class)
@SpringBootTest(classes={MessagePushApp.class})// 指定启动类
public class PushTest {
    private static final Logger logger = LoggerFactory.getLogger(PushTest.class);
    @Autowired RedisService redisService;
    @Autowired private MessagePushService messagePushService;
    @Autowired private RedisTemplate redisTemplate;

    @Test
    public void testPush(){
        // 模拟 在 2019-08-11 17:44:52 --> 1565516692 时间有5000个用户订阅
        double subscribeTime = 1565516692;
        int userCount = 5000;
        // 一次从 队列中取出执行的数量
        int batchCount = 2000;
        Set<ZSetOperations.TypedTuple> sets = new HashSet<>(userCount);
        // 循环模拟用户id
        for (int i = 1; i <= userCount; i++) {
            ZSetOperations.TypedTuple typedTuple = new DefaultTypedTuple(i, subscribeTime);
            sets.add(typedTuple);
        }
        Set set = null;
        // 将元素插入有序集合zset
        redisTemplate.opsForZSet().add("zset", sets);

        // 已经推送的数量
        int pushCount = 0;
        for (int j = 1; j <= 6; j++){
            logger.info("-------------push batch：{}-------------",j);
            /**
             * 获取之后 执行删除 避免下一秒的定时任务取到已经消费掉的数据（能否实现？）
             *
             * 不能实现的话：可以定义一个redis 分布式锁
             */
            set = redisTemplate.opsForZSet().reverseRangeByScore("zset",subscribeTime,subscribeTime,0,batchCount);
            redisTemplate.opsForZSet().remove("zset",set.toArray());
            if (0 == set.size()){
                logger.info("------sort is empty------");
                break;
            }
            set.forEach(push -> messagePushService.pushMsg(push));
            pushCount = pushCount + set.size();
        }
        logger.info("push end:{}",pushCount);
    }

    @Test
    public void clearRedis(){
        Set<String> keys = redisService.keys(RedisKeyConstants.EXERCISE_PUSH_KEY +"*");
        for(String ss:keys){
            System.out.println(ss);
        }
    }

    @Test
    public void testSortSet(){
        // Spring提供接口 TypedTuple操作有序集合
        Set<ZSetOperations.TypedTuple> set1 = new HashSet<ZSetOperations.TypedTuple>();
        Set<ZSetOperations.TypedTuple> set2 = new HashSet<ZSetOperations.TypedTuple>();
        int j = 9;
        for (int i = 1; i <= 9; i++) {
            j--;
            // 计算分数和值
            Double score1 = Double.valueOf(i);
            String value1 = "x" + i;
            Double score2 = Double.valueOf(j);
            String value2 = j % 2 == 1 ? "y" + j : "x" + j;
            // 使用 Spring 提供的默认 TypedTuple--DefaultTypedTuple
            ZSetOperations.TypedTuple typedTuple1 = new DefaultTypedTuple(value1, score1);
            set1.add(typedTuple1);
            ZSetOperations.TypedTuple typedTuple2 = new DefaultTypedTuple(value2, score2);
            set2.add(typedTuple2);
        }
        // 将元素插入有序集合zset1
        redisTemplate.opsForZSet().add("zset1", set1);
        redisTemplate.opsForZSet().add("zset2", set2);
        // 统计总数
        Long size = null;
        size = redisTemplate.opsForZSet().zCard("set1");
        // 计分数为score，那么下面的方法就是求 3<=score<=6的元素
        size = redisTemplate.opsForZSet().count("zset1", 3, 6);
        Set set = null;
        // 从下标一开始截取5个元素，但是不返回分数，每一个元索是String
        set = redisTemplate.opsForZSet().range("zset1", 1, 5);
        printSet(set);
        // 截取集合所有元素，并且对集合按分数排序，并返回分数，每一个元素是TypedTuple
        set = redisTemplate.opsForZSet().rangeWithScores("zset1", 0, -1);
        printTypedTuple(set);
        // 将zset1和zset2两个集合的交集放入集合inter_zset
        size = redisTemplate.opsForZSet().intersectAndStore("zset1", "zset2","inter_zset");
        // 区间
        RedisZSetCommands.Range range = RedisZSetCommands.Range.range();
        range.lt("x8");// 小于
        range.gt("x1"); // 大于
        set = redisTemplate.opsForZSet().rangeByLex("zset1", range);
        printSet(set);
        range.lte("x8"); // 小于等于
        range.gte("xl"); // 大于等于
        set = redisTemplate.opsForZSet().rangeByLex("zset1", range);
        printSet(set);
        // 限制返回个数
        RedisZSetCommands.Limit limit = RedisZSetCommands.Limit.limit();
        // 限制返回个数
        limit.count(4);
        // 限制从第五个开始截取
        limit.offset(5);
        // 求区间内的元素，并限制返回4条
        set = redisTemplate.opsForZSet().rangeByLex("zset1", range, limit);
        printSet(set);
        // 求排行，排名第1返回0，第2返回1
        Long rank = redisTemplate.opsForZSet().rank("zset1", "x4");
        System.err.println("rank = " + rank);
        // 删除元素，返回删除个数
        size = redisTemplate.opsForZSet().remove("zset1", "x5", "x6");
        System.err.println("delete = " + size);
        // 按照排行删除从0开始算起，这里将删除第排名第2和第3的元素
        size = redisTemplate.opsForZSet().removeRange("zset2", 1, 2);
        // 获取所有集合的元素和分数，以-1代表全部元素
        set = redisTemplate.opsForZSet().rangeWithScores("zset2", 0, -1);
        printTypedTuple(set);
        // 删除指定的元素
        size = redisTemplate.opsForZSet().remove("zset2", "y5", "y3");
        System.err.println(size);
        // 给集合中的一个元素的分数加上11
        Double dbl = redisTemplate.opsForZSet().incrementScore("zset1", "x1",11);
        redisTemplate.opsForZSet().removeRangeByScore("zset1", 1, 2);
        set = redisTemplate.opsForZSet().reverseRangeWithScores("zset2", 1, 10);
        printTypedTuple(set);
    }

    /**
     * 打印TypedTuple集合
     * @param set
     * -- Set<TypedTuple>
     */
    public static void printTypedTuple(Set<ZSetOperations.TypedTuple> set) {
        if (set != null && set.isEmpty()) {
            return;
        }

        Iterator iterator = set.iterator();
        while (iterator.hasNext()) {
            ZSetOperations.TypedTuple val = (ZSetOperations.TypedTuple) iterator.next();
            System.err.print("{value = " + val.getValue() + ", score = "
                    + val.getScore() + "}\n");
        }
    }
    /**
     * 打印普通集合
     * @param set 普通集合
     */
    public static void printSet(Set set) {
        if (set != null && set.isEmpty()) {
            return;
        }
        Iterator iterator = set.iterator();
        while (iterator .hasNext()) {
            Object val = iterator.next();
            System. out.print (val +"\t");
        }
        System.out.println();
    }
}
