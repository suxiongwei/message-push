package com.sxw.common.quartz;

import com.sxw.common.redis.RedisService;
import org.quartz.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Quartz 定时任务服务类
 */
@Component
public class QuartzService {
    private static final Logger logger = LoggerFactory.getLogger(QuartzService.class);
    /**
     * 注入任务调度器
     */
    @Autowired private Scheduler scheduler;
    @Autowired private RedisService redisService;

    @Value("${push-timer-cron}")
    private String pushTimerCron;

    @Value("${retry-push-time-cron}")
    private String retryPushTimeCron;

    /**
     * 创建推送的定时任务
     * @return
     * @throws SchedulerException
     */
    public boolean buildPushTimer() throws SchedulerException {
        //任务名称
        String name = UUID.randomUUID().toString();
        //任务所属分组
        String group = PushTimer.class.getName();
        CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(pushTimerCron);
        //创建任务
        JobDetail jobDetail = JobBuilder.newJob(PushTimer.class).withIdentity(name,group).build();
        //创建任务触发器
        Trigger trigger = TriggerBuilder.newTrigger().withIdentity(name,group).withSchedule(scheduleBuilder).build();
        //将触发器与任务绑定到调度器内
        scheduler.scheduleJob(jobDetail, trigger);
        logger.info("初始化推送定时任务");
        return true;
    }

}
