package com.sxw.common.quartz;

import com.sxw.common.redis.RedisService;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.Date;


public class PushTimer extends QuartzJobBean {
    static Logger logger = LoggerFactory.getLogger(PushTimer.class);

    @Autowired private RedisService redisService;

    /**
     * 开启另一个定时任务，每隔 1s 执行一次推送
     */
    @Override
    protected void executeInternal(JobExecutionContext jobExecutionContext) throws JobExecutionException {
            logger.info("执行推送，任务时间：{}",new Date());
    }
}
