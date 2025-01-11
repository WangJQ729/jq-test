package com.gerow.test.task;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.annotation.JSONField;
import com.gerow.test.client.HttpUtils;
import com.gerow.test.entity.YmlHttpStepEntity;
import com.gerow.test.utils.TestUtils;
import com.gerow.test.utils.assertion.Assertion;
import com.gerow.test.utils.data.ConfigManager;
import com.gerow.test.utils.data.Extractor;
import com.gerow.test.utils.data.StepEditor;
import com.google.common.io.BaseEncoding;
import io.qameta.allure.Allure;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.Assertions;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.DigestUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.stream.Collectors;

@Data
public class YmlTestStep implements ITestStep {
    /**
     * 步骤内容
     */
    private YmlHttpStepEntity step;

    private Map<String, Integer> assertionLength = new HashMap<>();
    /**
     * 测试方法
     */
    @JSONField(serialize = false)
    private ITestMethod testMethod;

    /**
     * 步骤名称
     */
    private String name;

    /**
     * 字段检查工厂
     */
    private StepEditor factory;

    /**
     * 参数
     */
    private Map<String, String> params;

    private List<Assertion> assertions;

    public YmlTestStep() {

    }

    /**
     * 构造方法
     *
     * @param step       测试步骤内容
     * @param params     参数
     * @param testMethod 测试方法
     * @param factory    字段检查工厂
     */
    public YmlTestStep(YmlHttpStepEntity step, Map<String, String> params, ITestMethod testMethod, StepEditor factory) {
        this.step = step;
        this.testMethod = testMethod;
        this.params = params;
        this.factory = factory;
        this.name = step.getName();
    }

    @Override
    public void save(String key, String value) {

    }

    /**
     * 参数替换
     *
     * @param content 需要替换的内容
     * @return 替换后的内容
     */
    public String replace(String content) {
        return TestUtils.replace(content, getTestMethod(), this, 0);
    }

    @Override
    public boolean enable() {
        return false;
    }

    @Override
    public void addAssertionLength(String key, int value) {
        if (this.getAssertionLength().containsKey(key)) {
            this.getAssertionLength().put(key, this.getAssertionLength().get(key) + value);
        } else {
            this.getAssertionLength().put(key, value);
        }
    }

    @Override
    public void doing() {
        this.buildParams();
        this.step = buildStep(step);
        if (step.getIter().isEmpty()) {
            doWithWait();
        } else {
            Map<String, String[]> dataList = new HashMap<>();
            step.getIter().forEach((k, v) -> dataList.put(k, replace(v).split(",")));
            OptionalInt min = dataList.values().stream().map(s -> s.length).mapToInt(Integer::valueOf).min();
            if (min.isPresent()) {
                for (int i = 0; i < min.getAsInt(); i++) {
                    Map<String, String> newParams = new HashMap<>(params);
                    int finalI = i;
                    dataList.forEach((k, v) -> newParams.put(k, v[finalI]));
                    if (newParams.containsValue(StringUtils.EMPTY)) {
                        break;
                    }
                    YmlHttpStepEntity copy = step.copy();
                    copy.setIter(new HashMap<>());
                    YmlTestStep newStep = new YmlTestStep(copy, newParams, testMethod, factory);
                    newStep.doing();
                }
            }
        }
    }

    /**
     * 间隔执行
     */
    private void doWithWait() {
        if (Integer.parseInt(replace(step.getSleep())) != 0) {
            try {
                Thread.sleep(Integer.parseInt(replace(step.getSleep())));
            } catch (InterruptedException ignored) {
            }
        }
        String stepName = replace(TestUtils.firstNonEmpty(factory.getName(), step.getName(), step.getKeyWord()).orElse("testStep"));
        Allure.step(stepName, () -> doWithWait(System.currentTimeMillis()));
    }

    private void buildParams() {
        params.replaceAll((k, v) -> replace(params.get(k)));
    }

    /**
     * 异步接口处理，需要等待
     *
     * @param startTime 开始时间
     * @throws Exception 异常
     */
    private void doWithWait(long startTime) throws Exception {
        long nowTime = System.currentTimeMillis();
        if (step.getUntilWait() > 0) {
            try {
                doExecute();
            } catch (Exception | Error e) {
                //抛异常后重试
                if (nowTime - startTime < step.getUntilWait() * 1000L) {
                    Thread.sleep(step.getIntervals());
                    doWithWait(startTime);
                } else {
                    throw e;
                }
            }
        } else {
            doExecute();
        }
    }

    /**
     * 执行具体操作
     */
    private void doExecute() throws UnsupportedEncodingException, NoSuchAlgorithmException {
        if (StringUtils.isNotBlank(step.getMethod())) {
            String url = buildUrl();
            HttpEntity entity = buildHttpEntity();
            HttpMethod method = HttpMethod.valueOf(StringUtils.upperCase(step.getMethod()));
            Class<?> type = step.getResponseType().getType();
            ResponseEntity<?> response = HttpUtils.composer(url, method, entity, type);
            if (step.getAssertion().stream().anyMatch(assertion -> !assertion.getConstant().isEmpty())) {
                //如果断言中有需要计算的常数。先提取参数
                saveParam(response);
                check(response);
            } else {
                check(response);
                saveParam(response);
            }
            if (step.isExtractTaskState()) {
                extractTaskState();
            }
        } else {
            check(new ResponseEntity<>(HttpStatus.OK));
            saveParam();
        }
    }

    private void extractTaskState() {
        Map<String, String> testClassParams = this.getTestMethod().getTestClass().getParams();
        String buyer_id = testClassParams.get("buyer_id");
        String order_id = testClassParams.get("order_id");
        String task_id = testClassParams.get("task_id");
        Map<String, String> testSuitParams = this.getTestMethod().getTestClass().getTestSuite().getParams();
        String shop_id = testSuitParams.get("shop_id");
        String platform = testSuitParams.get("platform");
        String md5DigestAsHex = DigestUtils.md5DigestAsHex((buyer_id + ":" + order_id + ":" + task_id).getBytes());
        String result = platform + ":rmdsrv:state:taskstate:" + platform + ":" + shop_id + ":" + md5DigestAsHex;
        Allure.step("获取taskState", () -> Allure.step(result));
    }

    /**
     * @param step 测试步骤
     * @return 构建好的测步骤
     */
    public YmlHttpStepEntity buildStep(YmlHttpStepEntity step) {
        if (StringUtils.isNoneBlank(step.getKeyWord())) {
            return copyStep(getStepEntity(step.getKeyWord()), step);
        }
        return this.step;
    }

    /**
     * 通过keyWord查找测试步骤
     *
     * @param keyWord 所要查找的测试步骤名称
     * @return 查找到的测试步骤
     */
    private YmlHttpStepEntity getStepEntity(String keyWord) {
        return testMethod.getTestClass().getTestSuite().getTestClass().stream().flatMap(testClass -> {
            List<ITestMethod> testMethods = new ArrayList<>(testClass.getTestMethods());
            testMethods.addAll(testClass.getBeforeClass());
            testMethods.addAll(testClass.getAfterClass());
            testMethods.addAll(testClass.getBefore());
            testMethods.addAll(testClass.getAfter());
            testMethods.addAll(testClass.getClassHeartbeat());
            testMethods.addAll(testClass.getKeyWord());
            testMethods.addAll(testClass.getTestSuite().getBeforeSuite());
            testMethods.addAll(testClass.getTestSuite().getAfterSuite());
            testMethods.addAll(testClass.getTestSuite().getHeartbeat());
            return testMethods.stream();
        }).flatMap(method -> method.getTestSteps().stream()).filter(testStep -> StringUtils.equals(testStep.getName(), keyWord)).map(testStep -> ((YmlTestStep) testStep).getStep().copy()).findAny().orElseThrow(() -> new UnsupportedOperationException("未找到step：" + keyWord));
    }

    /**
     * @param newStep 所要copy的step
     * @param oldStep 老的step
     * @return copy结果
     */
    private YmlHttpStepEntity copyStep(YmlHttpStepEntity newStep, YmlHttpStepEntity oldStep) {
        if (!oldStep.getHeaders().isEmpty()) newStep.setHeaders(oldStep.getHeaders());
        if (StringUtils.isNotBlank(oldStep.getBody())) newStep.setBody(oldStep.getBody());
        if (!oldStep.getAssertion().isEmpty()) newStep.setAssertion(oldStep.getAssertion());
        if (!oldStep.getExtractor().isEmpty()) newStep.setExtractor(oldStep.getExtractor());
        if (!oldStep.getFile().isEmpty()) newStep.setFile(oldStep.getFile());
        if (!oldStep.getForm().isEmpty())
            for (String s : oldStep.getForm().keySet()) newStep.getForm().put(s, oldStep.getForm().get(s));
        if (!oldStep.getVariables().isEmpty()) for (String s : oldStep.getVariables().keySet())
            newStep.getVariables().put(s, oldStep.getVariables().get(s));
        if (StringUtils.isNotBlank(oldStep.getHost())) newStep.setHost(oldStep.getHost());
        if (!oldStep.getIter().isEmpty()) newStep.setIter(oldStep.getIter());
        if (oldStep.getUntilWait() != 0) newStep.setUntilWait(oldStep.getUntilWait());
        if (!StringUtils.equals(oldStep.getSleep(), "0")) newStep.setSleep(oldStep.getSleep());
        if (oldStep.getIntervals() != 1000) newStep.setIntervals(oldStep.getIntervals());
        if (oldStep.getBodyEditor() != null) newStep.setBodyEditor(oldStep.getBodyEditor());
        if (StringUtils.isNotBlank(oldStep.getName())) newStep.setName(oldStep.getName());
        //如果copyStep也是keyWord查找，执行递归
        if (StringUtils.isNotBlank(newStep.getKeyWord())) newStep = buildStep(newStep);
        return newStep;
    }

    /**
     * 构造url
     *
     * @return url
     */
    private String buildUrl() throws UnsupportedEncodingException {
        String host = ConfigManager.getUrl();
        if (step.getHost() != null) {
            host = replace(step.getHost());
        }
        host = ConfigManager.getProtocol() + "://" + host;
        StringBuilder builder = new StringBuilder(host).append(replace(step.getUrl()));
        Map<String, String> variables = step.getVariables();
        String result = variables.keySet().stream().map(key -> {
            String value = replace(variables.get(key));
            return key + "=" + value;
        }).collect(Collectors.joining("&"));
        if (StringUtils.isNotBlank(result)) {
            builder.append("?").append(result);
        }
        String url = StringUtils.remove(builder.toString(), "\n");
        return URLDecoder.decode(url, StandardCharsets.UTF_8);
    }

    /**
     * 构造请求对象
     *
     * @return 请求对象
     */
    private HttpEntity buildHttpEntity() throws NoSuchAlgorithmException {
        HttpHeaders httpHeaders = new HttpHeaders();
        Map<String, String> header = step.getHeaders();
        for (String key : header.keySet()) {
            httpHeaders.add(replace(key), replace(header.get(key)));
        }
        if (!step.getFile().isEmpty()) {
            return getMultiValueMapHttpEntity(httpHeaders);
        }
        if (!step.getForm().isEmpty()) {
            return getValueMapHttpEntity(httpHeaders);
        }
        String body = buildBody();
        if (MediaType.APPLICATION_FORM_URLENCODED.equals(httpHeaders.getContentType())) {
            return new HttpEntity<>(body, httpHeaders);
        }
        Object parse = body;
        if (!StringUtils.containsIgnoreCase(header.get("Content-Type"), "form")) {
            try {
                parse = JSONObject.parse(body);
            } catch (Exception e) {
                Assertions.fail("JSON 格式错误:\n" + body);
            }
        }
        sign(httpHeaders, header, body);
        return new HttpEntity<>(parse, httpHeaders);
    }

    private String buildBody() {
        String body = step.getBody();
        body = replace(body);
        if (step.getBodyEditor() != null) {
            //根据bodyBuilder构造请求体
            body = step.getBodyEditor().builderBody(body, this);
        }
        //根据fieldCheck构造请求体
        body = factory.builderBody(body, this);
        //替换请求体里边的参数
        body = replace(body);
        return body;
    }

    /**
     * form表单
     */
    private HttpEntity<MultiValueMap<String, Object>> getValueMapHttpEntity(HttpHeaders httpHeaders) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        Map<String, String> stepForm = step.getForm();
        for (String key : stepForm.keySet()) {
            form.add(replace(key), replace(stepForm.get(key)));
        }
        return new HttpEntity<>(form, httpHeaders);
    }

    /**
     * 文件上传
     */
    private HttpEntity<MultiValueMap<String, Object>> getMultiValueMapHttpEntity(HttpHeaders httpHeaders) {
        MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
        step.getFile().forEach((k, v) -> {
            k = StringUtils.isBlank(k) ? "file" : k;
            FileSystemResource fileSystemResource = new FileSystemResource(new File(v));
            form.add(k, fileSystemResource);
        });
        Map<String, String> stepForm = step.getForm();
        for (String key : stepForm.keySet()) {
            form.add(replace(key), replace(stepForm.get(key)));
        }
        return new HttpEntity<>(form, httpHeaders);
    }

    /**
     * 验签字段
     */
    private void sign(HttpHeaders httpHeaders, Map<String, String> header, String body) throws NoSuchAlgorithmException {
        if (step.isNeedTanmaSign()) {
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            String needSignStr = replace(header.get("appId") + header.get("timestamp") + body + header.get("appKey"));
            byte[] digest = sha256.digest(needSignStr.getBytes(StandardCharsets.UTF_8));
            httpHeaders.add("sign", BaseEncoding.base16().lowerCase().encode(digest));
        }
    }

    @Override
    public <T> void saveParam(ResponseEntity<T> entity) {
        List<Extractor> extractorList = new LinkedList<>(step.getExtractor());
        for (Extractor extractor : extractorList) {
            extractor.save(this, entity, testMethod);
        }
    }

    @Override
    public void saveParam() {
        List<Extractor> extractorList = new LinkedList<>(step.getExtractor());
        for (Extractor extractor : extractorList) {
            extractor.save(testMethod);
        }
    }

    @Override
    public String getKeyWord() {
        return step.getKeyWord();
    }

    @Override
    public <T> void check(ResponseEntity<T> entity) {
        List<Assertion> assertionList = step.getAssertion();
        List<Assertion> assertions = factory.builderAssertion();
        if (!assertions.isEmpty()) {
            assertionList = assertions;
        }
        List<Error> errors = new ArrayList<>();
        for (Assertion assertion : assertionList) {
            //assertion的value类型不定，所以这里统一转换为json来处理变量
            String content = replace(JSONObject.toJSONString(assertion));
            Assertion newAssert = JSONObject.parseObject(content, Assertion.class);
            try {
                newAssert.check(entity, this);
            } catch (Error e) {
                errors.add(e);
            }
        }
        if (!errors.isEmpty()) {
            Assertions.fail(errors.toString());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        YmlTestStep that = (YmlTestStep) o;
        return this.hashCode() == that.hashCode();
    }

    @Override
    public int hashCode() {
        return Objects.hash(step);
    }
}
