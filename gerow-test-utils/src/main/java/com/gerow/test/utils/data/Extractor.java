package com.gerow.test.utils.data;

import com.alibaba.fastjson.JSONObject;
import com.gerow.test.task.ITest;
import com.gerow.test.task.ITestMethod;
import com.gerow.test.task.ITestStep;
import com.gerow.test.utils.TestUtils;
import com.gerow.test.utils.json.JsonPathUtils;
import com.jayway.jsonpath.Option;
import io.qameta.allure.Allure;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.springframework.http.ResponseEntity;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.*;
import java.util.stream.Collectors;

@Setter
@Getter
public class Extractor {
    /**
     * 保存参数的位置
     */
    private Site site = Site.DEFAULT;
    /**
     * 数据类型
     */
    private DataType type = DataType.JSON;
    /**
     * 数据来源
     */
    private DataSources sources = DataSources.BODY;
    /**
     * 提取参数个数
     */
    private int size = 1;
    /**
     * 参数的key
     */
    private String name;
    /**
     * 值的path或者具体值
     */
    private String value;
    /**
     * 数据
     */
    private String data;
    private boolean need_decode = false;
    private boolean ignore = false;
    /**
     * 数据类型参数的分隔符
     */
    private String separator = ",";
    /**
     * jsonPath配置
     */
    private Option[] options = new Option[]{};

    private LinkedHashMap<String, String> json = new LinkedHashMap<>();
    private LinkedHashMap<String, String> headers = new LinkedHashMap<>();

    /**
     * 保存参数
     *
     * @param entity 响应实体
     * @param test   测试方法
     * @param <T>    响应体类型
     */
    public <T> void save(ITestStep testStep, ResponseEntity<T> entity, ITestMethod test) {
        for (String key : json.keySet()) {
            Extractor extractor = buildExtractorByType(DataSources.BODY, key, json.get(key));
            extractor.save(testStep, entity, test);
        }
        for (String key : headers.keySet()) {
            Extractor extractor = buildExtractorByType(DataSources.HEADER, key, headers.get(key));
            extractor.save(testStep, entity, test);
        }
        Extractor extractor = replace(testStep);
        if (StringUtils.isNotBlank(name)) {
            Allure.step("获取参数:" + name, () -> extractor.doSave(entity, test));
        }
    }

    private <T> void doSave(ResponseEntity<T> entity, ITestMethod test) throws NoSuchPaddingException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, IllegalBlockSizeException, BadPaddingException, InvalidKeyException, InvalidKeySpecException, UnsupportedEncodingException {
        ITest save = getTest(test);
        if (StringUtils.isNotBlank(data)) {
            sources = DataSources.DATA;
        }
        switch (sources) {
            case DATA:
                switch (type) {
                    case JSON:
                        saveJsonPath(save, JsonPathUtils.read(data, value, options));
                        break;
                    case XML:
                    case DEFAULT:
                    default:
                        break;
                }
                break;
            case BODY:
                switch (type) {
                    case JSON:
                        if (need_decode) {
                            String platform = System.getProperty("platform");
                            String json;
                            if (StringUtils.equals(platform, "融合版")) {
                                json = Objects.requireNonNull(entity.getBody()).toString();
                                saveJsonPath(save, JsonPathUtils.read(json, value.replace("$", "$.answer"), options));
                            } else {
                                json = TestUtils.des3Cipher("828d1bc65eefc6c88ca1a5d4", "828d1bc6", 2, Objects.requireNonNull(entity.getBody()).toString());
                                Allure.addAttachment("解密结果：", json);
                                saveJsonPath(save, JsonPathUtils.read(json, value, options));
                            }
                        } else saveJsonPath(save, JsonPathUtils.read(entity.getBody(), value, options));
                        break;
                    case XML:
                    case DEFAULT:
                    default:
                        String value = Objects.requireNonNull(entity.getBody()).toString();
                        Allure.step(value);
                        save.save(name, value);
                        break;
                }
                break;
            case HEADER:
                String value = entity.getHeaders().get(this.value).get(0);
                Allure.step(value);
                save.save(name, value);
                break;
            case DEFAULT:
                Allure.step(this.value);
                save.save(name, this.value);
                break;
            default:
                break;
        }
    }

    /**
     * 保存json类型的数据
     *
     * @param save   保存位置的实体对象
     * @param result 实际参数
     * @param <T>    参数的类型
     */
    private <T> void saveJsonPath(ITest save, T result) {
        if (result == null) {
            if (Arrays.asList(options).contains(Option.DEFAULT_PATH_LEAF_TO_NULL)) {
                save.save(name, "");
            } else {
                if (!ignore) {
                    Assertions.fail("请求响应中未找到：" + value);
                } else {
                    save.save(name, "0");
                }
            }
        } else {
            String value;
            if (result instanceof Collection) {
                value = buildListResult((Collection<?>) result);
            } else {
                value = result.toString();
            }
            Allure.step(value);
            save.save(name, value);
        }
    }

    /**
     * 构造结果数组
     *
     * @param result 结果
     * @return 结果数组的字符串
     */
    private String buildListResult(Collection<?> result) {
        String value;
        ArrayList<?> list = new ArrayList<>(result);
//        Collections.shuffle(list);
        value = list.stream().limit(size).map(o -> {
            if (o instanceof Map) {
                return JSONObject.toJSONString(o);
            } else {
                return o.toString();
            }
        }).collect(Collectors.joining(separator));
        if (!Arrays.asList(options).contains(Option.DEFAULT_PATH_LEAF_TO_NULL)) {
            if (ignore) {
                return "0";
            }
            Assertions.assertThat(value).as("参数未找到: " + this.value).isNotBlank();
        }
        return value;
    }

    /**
     * 保存参数
     *
     * @param test 保存的测试方法
     */
    public void save(ITestMethod test) {
        ITest save = getTest(test);
        for (String key : json.keySet()) {
            Extractor extractor = buildExtractorByType(DataSources.BODY, key, json.get(key));
            extractor.save(test);
        }
        if (StringUtils.isNotBlank(name)) {
            Allure.step("获取参数:" + name, () -> save.save(name, value));
        }
    }

    /**
     * 获取所要保存的位置
     *
     * @param test 测试方法
     * @return 保存位置的实体对象
     */
    private ITest getTest(ITestMethod test) {
        ITest save = test;
        switch (site) {
            case SHEET:
            case DEFAULT:
            case TESTCLASS:
                save = test.getTestClass();
                break;
            case EXCEL:
            case ALL:
            case TESTSUITE:
                save = test.getTestClass().getTestSuite();
                break;
            case TESTCASE:
                break;
        }
        return save;
    }

    @Override
    public String toString() {
        return JSONObject.toJSONString(this);
    }

    private Extractor buildExtractorByType(DataSources sources, String key, String value) {
        Extractor extractor = new Extractor();
        extractor.setName(key);
        extractor.setValue(value);
        extractor.setData(data);
        extractor.setNeed_decode(isNeed_decode());
        extractor.setIgnore(isIgnore());
        extractor.setOptions(options);
        extractor.setSeparator(separator);
        extractor.setSite(site);
        extractor.setSize(size);
        extractor.setType(DataType.JSON);
        extractor.setSources(sources);
        return extractor;
    }

    public Extractor replace(ITestStep step) {
        Extractor extractor = new Extractor();
        extractor.setSite(getSite());
        extractor.setType(getType());
        extractor.setOptions(getOptions());
        extractor.setSize(getSize());
        extractor.setNeed_decode(isNeed_decode());
        extractor.setIgnore(isIgnore());
        extractor.setSeparator(getSeparator());
        extractor.setData(step.replace(getData()));
        extractor.setName(step.replace(getName()));
        extractor.setValue(step.replace(getValue()));
        extractor.setSources(getSources());
        LinkedHashMap<String, String> json = new LinkedHashMap<>();
        for (String key : this.json.keySet()) {
            json.put(step.replace(key), step.replace(this.json.get(key)));
        }
        extractor.setJson(json);
        return extractor;
    }
}
