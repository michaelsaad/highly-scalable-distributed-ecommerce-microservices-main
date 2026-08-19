package com.m1kllz.ecommerce.notification.service;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.mail.javamail.MimeMessagePreparator;
import org.springframework.stereotype.Service;

import com.m1kllz.ecommerce.order.event.OrderPlacedEvent;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final JavaMailSender javaMailSender;

    @KafkaListener(topics = "order-placed")
    @Observed(name = "notification.orderPlaced")
    public void listen(OrderPlacedEvent orderPlacedEvent) {
        log.info("Received OrderPlacedEvent orderNumber={}", orderPlacedEvent.getOrderNumber());
        MimeMessagePreparator messagePreparator = mimeMessage -> {
            MimeMessageHelper messageHelper = new MimeMessageHelper(mimeMessage);
            messageHelper.setFrom("dist.sys@allu.com");
            messageHelper.setTo(orderPlacedEvent.getEmail().toString());
            messageHelper.setSubject(String.format("Your Order with OrderNumber %s is placed successfully",
                    orderPlacedEvent.getOrderNumber()));
            messageHelper.setText(String.format("""
                            Hi %s, %s

                            Your order with order number %s is now placed successfully.

                            Best Regards
                            Spring Shop
                            """,
                    orderPlacedEvent.getFirstName().toString(),
                    orderPlacedEvent.getLastName().toString(),
                    orderPlacedEvent.getOrderNumber()));
        };
        try {
            javaMailSender.send(messagePreparator);
            log.info("Order notification email sent orderNumber={}", orderPlacedEvent.getOrderNumber());
        } catch (MailException e) {
            log.error("Failed to send order notification email orderNumber={}",
                    orderPlacedEvent.getOrderNumber(), e);
            throw new IllegalStateException("Failed to send order notification email", e);
        }
    }
}
