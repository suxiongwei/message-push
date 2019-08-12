package com.sxw.init;

import com.sxw.common.quartz.QuartzService;
import com.sxw.util.CommonUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class QuartzApplicationRunner implements ApplicationRunner {
    @Autowired private QuartzService quartzService;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        quartzService.buildPushTimer();
    }
}
