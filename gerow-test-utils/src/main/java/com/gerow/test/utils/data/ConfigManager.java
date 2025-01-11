package com.gerow.test.utils.data;

import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ConfigManager {

    private static Config config;
    private static ClientInfo clientInfo;

    @Autowired
    public ConfigManager(Config config, ClientInfo clientInfo) {
        ConfigManager.config = config;
        ConfigManager.clientInfo = clientInfo;
    }

    @Autowired
    public void setConfig(Config config) {
        ConfigManager.config = config;
    }

    @Autowired
    public void setHeaders(ClientInfo headers) {
        ConfigManager.clientInfo = headers;
    }

    /**
     * @return 默认请求头
     */
    public static Map<String, String> getCustomizeHeader() {
        return clientInfo.getHeaders();
    }

    /**
     * @return http协议
     */
    public static String getProtocol() {
        return clientInfo.getProtocol();
    }

    /**
     * @return 默认地址
     */
    public static String getUrl() {
        return config.getTest().get("host");
    }

    /**
     * @return 超时时间
     */
    public static int getTimeOut() {
        return clientInfo.getTimeOut();
    }


    /**
     * @return 参数
     */
    public static Map<String, String> getProperties() {
        return config.getTest();
    }


    /**
     * data下的参数
     */
    @Configuration
    @Data
    @ConfigurationProperties(prefix = "data")
    public static class Config {
        private Map<String, String> test = new HashMap<>();
    }

    /**
     * httpclient的属性
     */
    @Configuration
    @Data
    @ConfigurationProperties(prefix = "client")
    public static class ClientInfo {
        private Map<String, String> headers = new HashMap<>();
        private String protocol = "";
        private int timeOut = 5;
    }
}
