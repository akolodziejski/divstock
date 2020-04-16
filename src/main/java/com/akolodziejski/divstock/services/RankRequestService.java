package com.akolodziejski.divstock.services;

import com.akolodziejski.divstock.RabbitMqConfig;
import com.akolodziejski.divstock.model.RankRequest;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RankRequestService {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void queueRequest(RankRequest request) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.topicExchangeName, "foo.bar.baz", request.getTicker());
    }
}
