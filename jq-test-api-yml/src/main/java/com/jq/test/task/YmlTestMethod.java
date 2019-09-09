package com.jq.test.task;

import com.jq.test.utils.TestUtils;
import io.qameta.allure.Allure;
import org.apache.commons.lang3.StringUtils;

public class YmlTestMethod extends AbstractTestMethod {
    @Override
    public boolean enable() {
        return TestUtils.isRun(this.getName(), System.getProperty("test.name"));
    }

    @Override
    protected void setAllure() {
        Allure.epic(getTestClass().getFile().getParentFile().getParentFile().getName());
        if (StringUtils.isNotBlank(getTestClass().getFeature())) {
            Allure.feature(getTestClass().getFeature());
        } else {
            Allure.feature(getTestClass().getFile().getParentFile().getName());
        }
        Allure.story(getTestClass().getName());
        for (String key : getLinks().keySet()) {
            Allure.link(key, getLinks().get(key));
        }
        if (StringUtils.isNotBlank(getDescription())) {
            Allure.descriptionHtml(getDescription());
        }
    }
}
