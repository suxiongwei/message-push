package com.sxw.service.impl;

import com.sxw.common.dto.PushMessageDto;
import com.sxw.mq.KafkaProducer;
import com.sxw.service.MessagePushService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class MessagePushServiceImpl implements MessagePushService {
    static Logger logger = LoggerFactory.getLogger(MessagePushServiceImpl.class);
    @Autowired KafkaProducer<PushMessageDto> kafkaSender;

    @Override
    public boolean pushMsg(Object userId) {
        PushMessageDto pushMessageDto;
        pushMessageDto = PushMessageDto.builder()
                .userId((Integer) userId)
                .message("这里是推送信息内容")
                .build();
        logger.info("执行推送，用户id:{}",pushMessageDto.getUserId());
        kafkaSender.send(pushMessageDto);
        return true;
    }
}
