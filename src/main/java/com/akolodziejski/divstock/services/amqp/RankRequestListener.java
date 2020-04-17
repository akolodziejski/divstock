package com.akolodziejski.divstock.services.amqp;

import com.akolodziejski.divstock.services.RankService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class RankRequestListener {

    @Autowired
    private RankService rankService;

    public void receiveMessage(String message) {
        log.info("Received <" + message + ">");
        rankService.generateAsync(message);
    }
}