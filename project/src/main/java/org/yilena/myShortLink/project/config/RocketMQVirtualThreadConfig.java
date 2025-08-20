//package org.yilena.myShortLink.project.config;
//
//import com.mysql.cj.protocol.MessageListener;
//import lombok.extern.slf4j.Slf4j;
//import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
//import org.apache.rocketmq.spring.support.DefaultRocketMQListenerContainer;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.util.ReflectionUtils;
//import org.yilena.myShortLink.project.common.constant.MQConstant;
//
//import java.lang.reflect.Field;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//@Slf4j
//@Configuration
//public class RocketMQVirtualThreadConfig {
//
//    @Bean(destroyMethod = "shutdown")
//    public ExecutorService rocketMQVirtualThreadExecutor() {
//        return Executors.newVirtualThreadPerTaskExecutor();
//    }
//
//    @Bean
//    public DefaultRocketMQListenerContainer customMQListenerContainer(
//            @Autowired ExecutorService rocketMQVirtualThreadExecutor) {
//
//        return new DefaultRocketMQListenerContainer() {
//            public void setupMessageListener(MessageListener messageListener) {
//                super.setupMessageListener(messageListener);
//
//                if (MQConstant.SHORT_LINK_STATS_STREAM_CONSUMER_GROUP_KEY.equals(getConsumerGroup())) {
//                    log.info("为消费者组 [{}] 设置虚拟线程池", getConsumerGroup());
//                    customizeExecutor();
//                }
//            }
//
//            private void customizeExecutor() {
//                try {
//                    // 使用反射获取底层组件
//                    Field consumerField = ReflectionUtils.findField(getClass(), "consumer");
//                    ReflectionUtils.makeAccessible(consumerField);
//                    DefaultMQPushConsumer consumer = (DefaultMQPushConsumer) consumerField.get(this);
//
//                    // 使用原生 API 设置线程池
//                    consumer.setConsumeThreadPool(rocketMQVirtualThreadExecutor);
//                    log.info("成功设置虚拟线程池");
//
//                } catch (Exception e) {
//                    log.error("设置虚拟线程池失败", e);
//                }
//            }
//        };
//    }
//}