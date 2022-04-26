package com.atguigu.gmall.oms.listener;

import com.atguigu.gmall.oms.mapper.OrderMapper;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.impl.AMQImpl;
import org.apache.commons.lang.StringUtils;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OrderListener {

    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("OMS_FAIL_QUEUE"),
            exchange = @Exchange(value = "ORDER_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"order.fail"}
    ))
    public void disableOrder(String orderToken, Channel channel, Message message) throws IOException {
        if (StringUtils.isBlank(orderToken)){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }

        // 标记为无效订单
        this.orderMapper.updateStatus(orderToken, 0, 5);

        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }



    @RabbitListener(queues = {"ORDER_DEAD_QUEUE"})
    public void closeOrder(String orderToken, Channel channel, Message message) throws IOException {
        if (StringUtils.isBlank(orderToken)){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }

        // 标记为已关闭订单
        if (this.orderMapper.updateStatus(orderToken, 0, 4) == 1) {
            // 给wms发送消息，解锁该订单对应的库存
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "stock.unlock", orderToken);
        }

        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }



    @RabbitListener(bindings = @QueueBinding(
            value = @Queue("OMS_PAY_QUEUE"),
            exchange = @Exchange(value = "ORDER_EXCHANGE", ignoreDeclarationExceptions = "true", type = ExchangeTypes.TOPIC),
            key = {"order.pay"}
    ))
    public void payOrder(String orderToken, Channel channel, Message message) throws IOException {
        if (StringUtils.isBlank(orderToken)){
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            return;
        }

        // 标记为无效订单
        if (this.orderMapper.updateStatus(orderToken, 0, 1) == 1) {
            // 发送消息给wms 减库存
            this.rabbitTemplate.convertAndSend("ORDER_EXCHANGE", "stock.minus", orderToken);
        }

        channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
    }
}
