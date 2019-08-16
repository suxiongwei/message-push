import com.google.common.base.Charsets;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import com.sxw.MessagePushApp;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.ArrayList;
import java.util.List;

/**
 * 测试redis 的布隆过滤器
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {MessagePushApp.class})
public class BloomFilterTest {
    private static final Logger logger = LoggerFactory.getLogger(BloomFilterTest.class);

    private BloomFilter<Integer> bf;

    private int size = 1000000;

    @Before
    public void init(){
        //不设置第三个参数时，误判率默认为0.03
        //bloomFilter = BloomFilter.create(Funnels.integerFunnel(), size);
        //进行误判率的设置，自动计算需要几个hash函数。bit数组的长度与size和fpp参数有关
        //过滤器内部会对size进行处理，保证size为2的n次幂。
        bf = BloomFilter.create(Funnels.integerFunnel(), size, 0.01);
        for(int i = 0; i < size; i++){
            bf.put(i);
        }
    }

    @Test
    public void test(){
        for(int i = 0; i < size; i++){
            if(!bf.mightContain(i)){
                //不会打印，因为不存在的情况不会出现误判
                logger.info("不存在的误判 {}", i);
            }
        }

        List<Integer> list = new ArrayList<>(1000);
        for (int i = size + 10000; i < size + 20000; i++) {
            if (bf.mightContain(i)) {
                list.add(i);
            }
        }
        //根据设置的误判率
        logger.info("存在的误判数量：{}", list.size());
    }

}
