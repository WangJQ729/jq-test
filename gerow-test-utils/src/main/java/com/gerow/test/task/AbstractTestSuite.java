package com.gerow.test.task;

import com.gerow.test.utils.TestUtils;
import com.gerow.test.utils.data.ConfigManager;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Data
public abstract class AbstractTestSuite implements ITestSuite {
    private File file;
    private List<ITestClass> testClass = new ArrayList<>();
    private Map<String, String> params = new HashMap<>();
    private List<ITestMethod> beforeSuite = new ArrayList<>();
    private List<ITestMethod> afterSuite = new ArrayList<>();
    private List<ITestMethod> heartbeat = new ArrayList<>();


    @Override
    public void addBeforeSuite(ITestMethod testMethod) {
        this.beforeSuite.add(testMethod);
    }

    @Override
    public void addAfterSuite(ITestMethod testMethod) {
        this.afterSuite.add(testMethod);
    }

    private ExecutorService executor = Executors.newFixedThreadPool(1);

    @Override
    public synchronized void setUp() {
        this.params.putAll(ConfigManager.getProperties());
        //只运行features下的前置条件
        beforeSuite.stream().filter(iTestMethod -> TestUtils.isRun(iTestMethod.getTestClass().getStory(), System.getProperty("story")) && TestUtils.isRun(iTestMethod.getTestClass().getFeature(), System.getProperty("features")) && TestUtils.isRun(System.getProperty("platform"), iTestMethod.getTestClass().getPlatform())).forEach(ITestMethod::doing);
        executor.submit((Runnable) () -> {
            while (true) {
                try {
                    heartbeat.get(0).doing();
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                }
            }
        });
    }


    @Override
    public synchronized void tearDown() {
        //只运行features下的前置条件
        afterSuite.stream().filter(iTestMethod -> TestUtils.isRun(iTestMethod.getTestClass().getStory(), System.getProperty("story")) && TestUtils.isRun(iTestMethod.getTestClass().getFeature(), System.getProperty("features")) && TestUtils.isRun(System.getProperty("platform"), iTestMethod.getTestClass().getPlatform())).forEach(ITestMethod::doing);
        executor.shutdown();
    }

    @Override
    public void save(String key, String value) {
        params.put(key, value);
    }

    @Override
    public String replace(String content) {
        return TestUtils.replace(content, this);
    }

    @Override
    public String getName() {
        return this.file.getName();
    }

    private boolean isTearDownRun = false;

    @Override
    public String getDirPath() {
        return getPath(file, "");
    }

    private String getPath(File file, String name) {
        File parentFile = file.getParentFile();
        if (parentFile != null) {
            String newName = StringUtils.isBlank(name) ? parentFile.getName() : parentFile.getName() + "/" + name;
            if (StringUtils.equals(parentFile.getName(), "testCase")) {
                return newName;
            } else {
                return getPath(parentFile, newName);
            }
        } else {
            return name;
        }
    }
}
