package com.laowan.localsaga;

import io.seata.saga.engine.AsyncCallback;
import io.seata.saga.engine.StateMachineEngine;
import io.seata.saga.proctrl.ProcessContext;
import io.seata.saga.statelang.domain.ExecutionStatus;
import io.seata.saga.statelang.domain.StateMachineInstance;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.util.Assert;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@SpringBootTest
@Slf4j
class LocalSagaApplicationTests {
    @Autowired
    StateMachineEngine stateMachineEngine;

    @Test
    void testSaga() {
        Map<String, Object> startParams = new HashMap<>(3);
        //唯一健
        String businessKey = String.valueOf(System.currentTimeMillis());
        startParams.put("businessKey", businessKey);
        startParams.put("count", 1);
        startParams.put("amount", new BigDecimal("1"));

        //sync test
        StateMachineInstance inst = stateMachineEngine.startWithBusinessKey("reduceInventoryAndBalance", null, businessKey, startParams);

        Assert.isTrue(ExecutionStatus.SU.equals(inst.getStatus()), "saga transaction execute failed. XID: " + inst.getId());
        System.out.println("saga transaction commit succeed. XID: " + inst.getId());

        inst = stateMachineEngine.getStateMachineConfig().getStateLogStore().getStateMachineInstanceByBusinessKey(businessKey, null);
        Assert.isTrue(ExecutionStatus.SU.equals(inst.getStatus()), "saga transaction execute failed. XID: " + inst.getId());

        //async test
      /*  businessKey = String.valueOf(System.currentTimeMillis());
        inst = stateMachineEngine.startWithBusinessKeyAsync("reduceInventoryAndBalance", null, businessKey, startParams, CALL_BACK);

        waittingForFinish(inst);

        Assert.isTrue(ExecutionStatus.SU.equals(inst.getStatus()), "saga transaction execute failed. XID: " + inst.getId());
        System.out.println("saga transaction commit succeed. XID: " + inst.getId());*/
    }

    private static volatile Object lock = new Object();
    private static AsyncCallback CALL_BACK = new AsyncCallback() {
        @Override
        public void onFinished(ProcessContext context, StateMachineInstance stateMachineInstance) {
            synchronized (lock){
                lock.notifyAll();
            }
        }

        @Override
        public void onError(ProcessContext context, StateMachineInstance stateMachineInstance, Exception exp) {
            synchronized (lock){
                lock.notifyAll();
            }
        }
    };

    private static void waittingForFinish(StateMachineInstance inst){
        synchronized (lock){
            if(ExecutionStatus.RU.equals(inst.getStatus())){
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }


    private static void transactionCommittedDemo(StateMachineEngine stateMachineEngine) {

        Map<String, Object> startParams = new HashMap<>(3);
        String businessKey = String.valueOf(System.currentTimeMillis());
        startParams.put("businessKey", businessKey);
        startParams.put("count", 10);
        startParams.put("amount", new BigDecimal("100"));

        //sync test
        StateMachineInstance inst = stateMachineEngine.startWithBusinessKey("reduceInventoryAndBalance", null, businessKey, startParams);

        Assert.isTrue(ExecutionStatus.SU.equals(inst.getStatus()), "saga transaction execute failed. XID: " + inst.getId());
        System.out.println("saga transaction commit succeed. XID: " + inst.getId());

        inst = stateMachineEngine.getStateMachineConfig().getStateLogStore().getStateMachineInstanceByBusinessKey(businessKey, null);
        Assert.isTrue(ExecutionStatus.SU.equals(inst.getStatus()), "saga transaction execute failed. XID: " + inst.getId());

        //async test
        businessKey = String.valueOf(System.currentTimeMillis());
        inst = stateMachineEngine.startWithBusinessKeyAsync("reduceInventoryAndBalance", null, businessKey, startParams, CALL_BACK);

        waittingForFinish(inst);

        Assert.isTrue(ExecutionStatus.SU.equals(inst.getStatus()), "saga transaction execute failed. XID: " + inst.getId());
        System.out.println("saga transaction commit succeed. XID: " + inst.getId());
    }

    private static void transactionCompensatedDemo(StateMachineEngine stateMachineEngine) {
        Map<String, Object> startParams = new HashMap<>(4);
        String businessKey = String.valueOf(System.currentTimeMillis());
        startParams.put("businessKey", businessKey);
        startParams.put("count", 10);
        startParams.put("amount", new BigDecimal("100"));
        startParams.put("mockReduceBalanceFail", "true");

        //sync test
        StateMachineInstance inst = stateMachineEngine.startWithBusinessKey("reduceInventoryAndBalance", null, businessKey, startParams);

        //async test
        businessKey = String.valueOf(System.currentTimeMillis());
        inst = stateMachineEngine.startWithBusinessKeyAsync("reduceInventoryAndBalance", null, businessKey, startParams, CALL_BACK);

        waittingForFinish(inst);

        Assert.isTrue(ExecutionStatus.SU.equals(inst.getCompensationStatus()), "saga transaction compensate failed. XID: " + inst.getId());
        System.out.println("saga transaction compensate succeed. XID: " + inst.getId());
    }
}
