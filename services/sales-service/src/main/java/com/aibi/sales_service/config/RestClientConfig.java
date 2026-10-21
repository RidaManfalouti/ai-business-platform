package com.aibi.sales_service.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient productRestClient(
            @Value("${services.product-service.url:http://localhost:8082}") String baseUrl,
            @Value("${services.product-service.connect-timeout-ms:3000}") int connectTimeout,
            @Value("${services.product-service.read-timeout-ms:3000}") int readTimeout) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        factory.setReadTimeout(Duration.ofMillis(readTimeout));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }

    @Bean
    public RestClient customerRestClient(
            @Value("${services.customer-service.url:http://localhost:8083}") String baseUrl,
            @Value("${services.customer-service.connect-timeout-ms:3000}") int connectTimeout,
            @Value("${services.customer-service.read-timeout-ms:3000}") int readTimeout) {

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeout));
        factory.setReadTimeout(Duration.ofMillis(readTimeout));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(factory)
                .build();
    }
}
