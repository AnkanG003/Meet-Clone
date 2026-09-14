package com.meetclone.identity.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class OAuth2Config {

    @Bean
    public RestClient restClient() {
        return RestClient.builder().build();
    }
}
