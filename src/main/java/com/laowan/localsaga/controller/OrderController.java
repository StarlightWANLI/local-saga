package com.laowan.localsaga.controller;

import io.seata.saga.engine.StateMachineEngine;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateMachineInstance;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * @Description
 * @Date 2021/5/19 9:41 上午
 * @Version 1.0
 * @Copyright 2019-2021
 */
@RestController
@Slf4j
public class OrderController {
    @Autowired
    StateMachineEngine stateMachineEngine;


    /**
     * 用户用这个路径进行访问：
     * http://localhost:8080/create
     * @return
     */
    @GetMapping("/create")
    public String create() {
        log.info("=========开始创建订单============");
        Map<String, Object> startParams = new HashMap<>(3);
        //唯一健
        String businessKey = String.valueOf(System.currentTimeMillis());
        startParams.put("businessKey", businessKey);
        startParams.put("count", 10);
        startParams.put("amount", new BigDecimal("400"));

        //同步执行
        StateMachineInstance inst = stateMachineEngine.startWithBusinessKey("reduceInventoryAndBalance", null, businessKey, startParams);

        if(ExecutionStatus.SU.equals(inst.getStatus())){
            log.info("创建订单成功,saga transaction execute Succeed. XID: " + inst.getId());
            return "创建订单成功";
        }else{
            log.info("创建订单失败 ,saga transaction execute failed. XID: " + inst.getId());
            return "创建订单失败";
        }
    }
}
