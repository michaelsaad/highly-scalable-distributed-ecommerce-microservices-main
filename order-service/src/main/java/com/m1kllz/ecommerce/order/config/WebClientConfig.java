package com.m1kllz.ecommerce.order.config;

import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

import io.micrometer.observation.ObservationRegistry;
import reactor.netty.http.client.HttpClient;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(WebClient.Builder builder,
            InventoryClientProperties inventoryProperties,
            ObservationRegistry observationRegistry) {
        HttpClient httpClient = HttpClient.create().responseTimeout(Duration.ofSeconds(3));
        return builder
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .baseUrl(inventoryProperties.url())
                .observationRegistry(observationRegistry)
                .build();
    }
}
