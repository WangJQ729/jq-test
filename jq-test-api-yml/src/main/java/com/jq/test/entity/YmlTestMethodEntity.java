package com.jq.test.entity;

import com.jq.test.task.ITestClass;
import com.jq.test.task.ITestMethod;
import com.jq.test.task.YmlTestMethod;
import com.jq.test.utils.StepEditor;
import com.jq.test.utils.TestUtils;
import io.qameta.allure.SeverityLevel;
import lombok.Data;

import java.util.*;
import java.util.stream.Collectors;

@Data
public class YmlTestMethodEntity {
    /**
     * 参数化
     */
    private List<LinkedHashMap<String, Object>> dataProvider = new ArrayList<>();
    /**
     * 测试名称
     */
    private String name;
    private String description;
    private String author = "王健强";
    private SeverityLevel severity = SeverityLevel.NORMAL;
    /**
     * 测试步骤
     */
    private List<YmlHttpStepEntity> step = new ArrayList<>();
    private Map<String, String> links = new HashMap<>();
    /**
     * 执行次数
     */
    private int invocationCount = 1;
    /**
     * 字段检查构造工厂
     */
    private List<StepEditor> editor = new ArrayList<>();

    /**
     * @param testClass 测试类
     * @return 测试方法
     */
    public List<ITestMethod> build(ITestClass testClass) {
        if (dataProvider.isEmpty()) {
            dataProvider = Collections.singletonList(new LinkedHashMap<>());
        }
        if (editor.isEmpty()) {
            editor = Collections.singletonList(new StepEditor());
        }
        List<ITestMethod> result = new ArrayList<>();
        for (int i = 0; i < invocationCount; i++) {
            for (Map<String, Object> data : dataProvider) {
                for (StepEditor factory : editor) {
                    if (data.containsKey("dataProvider")) {
                        List<LinkedHashMap<String, String>> dataProviders = (List<LinkedHashMap<String, String>>) data.get("dataProvider");
                        for (LinkedHashMap<String, String> provider : dataProviders) {
                            Map<String, String> newData = new HashMap<>();
                            data.forEach((k, v) -> newData.put(k, v.toString()));
                            provider.putAll(newData);
                            YmlTestMethod testMethod = buildTestMethod(testClass, newData, i, factory);
                            result.add(testMethod);
                        }
                    } else {
                        Map<String, String> newData = new HashMap<>();
                        data.forEach((k, v) -> newData.put(k, v.toString()));
                        YmlTestMethod testMethod = buildTestMethod(testClass, newData, i, factory);
                        result.add(testMethod);
                    }
                }
            }
        }
        return result;
    }

    /**
     * @param testClass 测试类
     * @param data      参数
     * @param i         第几次执行
     * @param factory   字段检查构造工厂
     * @return 测试方法
     */
    private YmlTestMethod buildTestMethod(ITestClass testClass, Map<String, String> data, Integer i, StepEditor factory) {
        Map<String, String> map = new LinkedHashMap<>(data);
        map.put("methodNum", i.toString());
        YmlTestMethod testMethod = new YmlTestMethod();
        testMethod.setTestClass(testClass);
        testMethod.setParams(map);
        testMethod.setDescription(description);
        testMethod.setAuthor(author);
        testMethod.setSeverityLevel(severity);
        testMethod.setLinks(links);
        String name = TestUtils.firstNonEmpty(factory.getName()).orElse("");
        if (name.isEmpty()) {
            testMethod.setName(this.name);
        } else {
            testMethod.setName(this.name + "-" + name);
        }
        testMethod.setTestSteps(step.parallelStream().flatMap(entity
                -> entity.build(testMethod, factory).stream()).collect(Collectors.toList()));
        return testMethod;
    }
}
