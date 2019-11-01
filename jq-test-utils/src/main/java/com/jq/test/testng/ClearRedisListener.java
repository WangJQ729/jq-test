package com.jq.test.testng;

import com.jq.test.client.RestTemplateTool;
import com.jq.test.utils.ConfigManager;
import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class ClearRedisListener implements ITestListener {


    @Override
    public void onTestStart(ITestResult result) {
        RestTemplateTool.getInstance().getForObject(ConfigManager.getProperties().get("mock_host") + "/api/clearCache", String.class);
    }

    @Override
    public void onTestSuccess(ITestResult result) {

    }

    @Override
    public void onTestFailure(ITestResult result) {

    }

    @Override
    public void onTestSkipped(ITestResult result) {

    }

    @Override
    public void onTestFailedButWithinSuccessPercentage(ITestResult result) {

    }

    @Override
    public void onStart(ITestContext context) {

    }

    @Override
    public void onFinish(ITestContext context) {

    }
}
