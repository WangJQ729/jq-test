package com.jq.test.utils;

import com.alibaba.fastjson.JSONObject;
import com.jayway.jsonpath.Option;
import com.jq.test.json.JsonPathUtils;
import com.jq.test.task.ITest;
import com.jq.test.task.ITestMethod;
import com.jq.test.task.ITestStep;
import io.qameta.allure.Allure;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.springframework.http.ResponseEntity;

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
    /**
     * 数据类型参数的分隔符
     */
    private String separator = ",";
    /**
     * jsonPath配置
     */
    private Option[] options = new Option[]{};

    private Map<String, String> json = new HashMap<>();

    /**
     * 保存参数
     *
     * @param entity 响应实体
     * @param test   测试方法
     * @param <T>    响应体类型
     */
    public <T> void save(ResponseEntity<T> entity, ITestMethod test) {
        for (String key : json.keySet()) {
            Extractor extractor = buildJsonExtractor(key);
            extractor.save(entity, test);
        }
        if (StringUtils.isNotBlank(name)) {
            Allure.step("获取参数:" + name, () -> doSave(entity, test));
        }
    }

    private <T> void doSave(ResponseEntity<T> entity, ITestMethod test) {
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
                        saveJsonPath(save, JsonPathUtils.read(entity.getBody(), value, options));
                        break;
                    case XML:
                    case DEFAULT:
                    default:
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
                Assertions.fail("请求响应中未找到：" + value);
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
        Collections.shuffle(list);
        value = list.stream().limit(size).map(o -> {
            if (o instanceof Map) {
                return JSONObject.toJSONString(o);
            } else {
                return o.toString();
            }
        }).collect(Collectors.joining(separator));
        if (!Arrays.asList(options).contains(Option.DEFAULT_PATH_LEAF_TO_NULL)) {
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
            Extractor extractor = buildJsonExtractor(key);
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
            case TESTSUIT:
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

    private Extractor buildJsonExtractor(String key) {
        Extractor extractor = new Extractor();
        extractor.setName(key);
        extractor.setValue(json.get(key));
        extractor.setData(data);
        extractor.setOptions(options);
        extractor.setSeparator(separator);
        extractor.setSite(site);
        extractor.setSize(size);
        extractor.setType(DataType.JSON);
        return extractor;
    }

    public Extractor replace(ITestStep step) {
        Extractor extractor = new Extractor();
        extractor.setSite(getSite());
        extractor.setType(getType());
        extractor.setOptions(getOptions());
        extractor.setSize(getSize());
        extractor.setSeparator(getSeparator());
        extractor.setData(step.replace(getData()));
        extractor.setName(step.replace(getName()));
        extractor.setValue(step.replace(getValue()));
        Map<String, String> json = new HashMap<>();
        for (String key : this.json.keySet()) {
            json.put(step.replace(key), step.replace(this.json.get(key)));
        }
        extractor.setJson(json);
        return extractor;
    }
}