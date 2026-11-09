package com.fleetops.mcp.api;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class FleetOpsApiConfig {
    @Bean("flotaRestClient")
    RestClient flotaRestClient(@Value("${fleetops.api.flota-base-url}") String baseUrl,
                               @Value("${fleetops.api.flota-bearer-token:}") String serviceToken,
                               @Value("${fleetops.api.connect-timeout}") Duration connectTimeout,
                               @Value("${fleetops.api.read-timeout}") Duration readTimeout) {
        return createClient(baseUrl, serviceToken, connectTimeout, readTimeout);
    }

    @Bean("alertaRestClient")
    RestClient alertaRestClient(@Value("${fleetops.api.alerta-base-url}") String baseUrl,
                                @Value("${fleetops.api.alerta-bearer-token:}") String serviceToken,
                                @Value("${fleetops.api.connect-timeout}") Duration connectTimeout,
                                @Value("${fleetops.api.read-timeout}") Duration readTimeout) {
        return createClient(baseUrl, serviceToken, connectTimeout, readTimeout);
    }

    private RestClient createClient(String baseUrl, String serviceToken, Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.toIntExact(connectTimeout.toMillis()));
        factory.setReadTimeout(Math.toIntExact(readTimeout.toMillis()));
        RestClient.Builder builder = RestClient.builder().baseUrl(baseUrl).requestFactory(factory);
        if (!serviceToken.isBlank()) {
            builder.defaultHeader("Authorization", "Bearer " + serviceToken);
        }
        return builder.build();
    }
}
