package com.talentmatch.config;

import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Embedded Tomcat configuration to support bulk resume file uploads.
 * Overrides Tomcat's default part count limit (introduced in Tomcat 10.1)
 * so recruiters can upload folders with up to 500 resumes in a single batch.
 */
@Configuration
public class TomcatUploadConfig {

    @Value("${server.tomcat.max-part-count:500}")
    private int maxPartCount;

    @Value("${server.tomcat.max-part-header-size:4096}")
    private int maxPartHeaderSize;

    @Bean
    public WebServerFactoryCustomizer<TomcatServletWebServerFactory> tomcatCustomizer() {
        return factory -> factory.addConnectorCustomizers((Connector connector) -> {
            connector.setMaxPartCount(maxPartCount);
            connector.setMaxPartHeaderSize(maxPartHeaderSize);
        });
    }
}
