package com.gerow.test.client;

import com.gerow.test.utils.data.ConfigManager;
import io.qameta.allure.Allure;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.client.ResourceAccessException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class HttpUtils {
    /**
     * 请求client
     */
    private static RestTemplateTool templateTool = RestTemplateTool.getInstance();
    /**
     * 协议名称
     */
    private final static String protocol = StringUtils.upperCase(ConfigManager.getProtocol() + "/1.1");

    private final static Log logger = LogFactory.getLog(HttpUtils.class);

    /**
     * @param url          url地址
     * @param method       请求方法
     * @param body         请求体
     * @param responseType 响应体对象
     * @param uriVariables url参数
     * @param <T>          响应体的类型
     * @return 响应体
     */
    public static <T> ResponseEntity<T> composer(String url, HttpMethod method, Object body, Class<T> responseType, Object... uriVariables) {
        HttpHeaders headers = new HttpHeaders();
        HttpEntity entity = new HttpEntity<>(body, headers);
        return HttpUtils.composer(url, method, entity, responseType, uriVariables);
    }

    /**
     * @param url           url地址
     * @param method        请求方法
     * @param requestEntity 请求体
     * @param responseType  响应体对象
     * @param uriVariables  url参数
     * @param <T>           响应体的类型
     * @return 响应体
     */
    public static <T> ResponseEntity<T> composer(String url, HttpMethod method, @Nullable HttpEntity<?> requestEntity, Class<T> responseType, Object... uriVariables) {
        assert requestEntity != null;
        HttpEntity<?> buildHttpEntity = buildHttpEntity(requestEntity);
        addRequestAttachment(url, method, buildHttpEntity);

        ResponseEntity<T> responseEntity;
        try {
            responseEntity = templateTool.exchange(url, method, buildHttpEntity, responseType, uriVariables);
        } catch (ResourceAccessException e) {
            throw new RuntimeException("接口超时：" + url);
        } catch (Exception e) {
            throw new RuntimeException("接口响应错误：" + url, e);
        }
        addResponseAttachment(responseEntity);
        return responseEntity;
    }

    /**
     * 构造请求体string
     *
     * @param requestEntity 请求体
     * @return 请求体string
     */
    private static HttpEntity<?> buildHttpEntity(@Nullable HttpEntity<?> requestEntity) {
        assert requestEntity != null;
        HttpHeaders headers = new HttpHeaders();
        Map<String, String> customizeHeader = ConfigManager.getCustomizeHeader();
        HttpHeaders httpHeaders = requestEntity.getHeaders();
        for (String key : httpHeaders.keySet()) {
            headers.add(key, String.join(";", Objects.requireNonNull(httpHeaders.get(key))));
        }
        for (Map.Entry<String, String> entry : customizeHeader.entrySet()) {
            if (!headers.containsKey(entry.getKey())) {
                headers.add(entry.getKey(), entry.getValue());
            }
        }
        return new HttpEntity<>(requestEntity.getBody(), headers);
    }

    /**
     * 添加响应体到附件
     *
     * @param responseEntity 响应体
     * @param <T>            响应体类型
     */
    private static <T> void addResponseAttachment(ResponseEntity<T> responseEntity) {
        String resType = protocol + " " + responseEntity.getStatusCode().value();
        String entityString = buildEntityString(responseEntity);

        // 添加当前格式日志到 Allure
        Allure.addAttachment(resType, resType + "\n" + entityString);

        // 添加单行 CURL 格式日志到 Allure
        String curlResponseLog = "HTTP/1.1 " + responseEntity.getStatusCode().value() + " " + entityString;
        Allure.addAttachment("CURL Response", curlResponseLog);

        // 输出到控制台
        logger.debug("\n" + resType + "\n" + entityString);
        logger.debug("\n[CURL RESPONSE] " + curlResponseLog);
    }

    /**
     * 添加请求体到附件
     *
     * @param url        url地址
     * @param method     请求方法
     * @param httpEntity 请求体entity
     */
    private static void addRequestAttachment(String url, HttpMethod method, HttpEntity<?> httpEntity) {
        String reqType = method.name().toUpperCase() + " " + url + " " + protocol;
        String request = reqType + "\n" + buildEntityString(Objects.requireNonNull(httpEntity));

        // 添加当前格式日志到 Allure
        Allure.addAttachment(reqType, request);

        // 构建单行 CURL 格式请求日志
        StringBuilder curlLog = new StringBuilder("curl -X ").append(method.name().toUpperCase()).append(" '").append(url).append("'").append(" \\\n");

        // 添加 headers
        httpEntity.getHeaders().forEach((key, value) -> curlLog.append("  -H '").append(key).append(": ").append(String.join(", ", value)).append("' \\\n"));

        // 添加 body（如果存在）
        if (httpEntity.getBody() != null) {
            curlLog.append("  --data-raw '").append(httpEntity.getBody().toString().replace("'", "\\'")).append("'\\\n");
        }
        curlLog.append("  --insecure");
        // 添加单行 CURL 格式日志到 Allure
        Allure.addAttachment("CURL Request", curlLog.toString());
    }

    /**
     * @param entity 请求、响应实体
     * @return 实体对象的string
     */
    private static String buildEntityString(HttpEntity<?> entity) {
        HttpHeaders headers = entity.getHeaders();
        String header = headers.keySet().stream().map(name -> {
            List<String> values = headers.get(name);
            return name + ":" + String.join(",", Objects.requireNonNull(values));
        }).collect(Collectors.joining("\n")) + "\n\n";

        if (entity.hasBody()) {
            try {
                return header + Objects.requireNonNull(entity.getBody());
            } catch (Exception e) {
                return header + Objects.requireNonNull(entity.getBody());
            }
        }
        return header;
    }
}
