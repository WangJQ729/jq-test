package com.gerow.test.jmeter.functions;

import com.gerow.test.utils.TestUtils;
import org.apache.jmeter.engine.util.CompoundVariable;
import org.apache.jmeter.functions.AbstractFunction;
import org.apache.jmeter.functions.InvalidVariableException;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.samplers.Sampler;

import java.util.Collection;
import java.util.List;
import java.util.Random;

public class RandomEmail extends AbstractFunction {

    protected CompoundVariable vars;

    private static final String KEY = "__RandomEmail";


    public static String getChineseName() {
        String[] domains = {"gmail.com", "yahoo.com", "hotmail.com", "outlook.com", "aol.com", "bitunix.com", "bitunix.io", "126.com", "163.com", "qq.com", "gmail.com", "yahoo.com", "icloud.com", "mail.com", "zoho.com", "yandex.com", "protonmail.com", "gmx.com", "me.com", "live.com", "fastmail.com", "hushmail.com", "sina.com", "aliyun.com"};
        String username = "gerow" + System.currentTimeMillis(); // 生成8位长度的随机用户名
        String domain = domains[new Random().nextInt(domains.length)]; // 随机选择一个域名
        return username + "@" + domain;
    }

    @Override
    public String execute(SampleResult previousResult, Sampler currentSampler) {
        String chineseName = getChineseName();
        if (vars != null) {
            TestUtils.saveVariables(vars, chineseName);
        }
        return chineseName;
    }


    @Override
    public void setParameters(Collection<CompoundVariable> parameters) throws InvalidVariableException {
        checkMinParameterCount(parameters, 0);
        CompoundVariable[] compoundVariables = parameters.toArray(new CompoundVariable[0]);
        if (compoundVariables.length > 0) {
            vars = compoundVariables[0];
        }
    }

    @Override
    public String getReferenceKey() {
        return KEY;
    }

    @Override
    public List<String> getArgumentDesc() {
        return null;
    }
}
