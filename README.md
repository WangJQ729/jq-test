# 一、项目结构

##### 1、gerow-test-utils相关工具类
##### 2、gerow-test-api-yml测试框架实现代码
##### 3、gerow-interface-test 测试用例


# 二、环境配置

### 1、maven、java 

    参考：https://www.runoob.com/maven/maven-setup.html maven
          https://www.runoob.com/java/java-environment-setup.html
    
### 2、allure安装

    https://www.jianshu.com/p/5735d388faa2

# 三、测试执行

    1、在根目录下运行命令：mvn clean install -DskipTests将代码编译到本地的maven仓库
    
    2、进入gerow-interface-test直接运行
    
       -Dspring.profiles.active=bitunix-testa -DtestDir=bitunix -Dfeatures= -Denv= -Dstory=partners -Dcomponent=用户交易,新用户交易 -Dtest.name=
    
## 可选参数
    
###  1、执行环境 
      
    -Dspring.profiles.active=xxx   
    
    需要对应的配置文件

### 2、单独用例执行
    
    1、根据场景选择  -Dstory=xxx,xxx
    
        example:    -Dstory=partners
    
    2、根据组件选择     -Dcomponent=xxx,xxx
    
        example:    -Dcomponent=用户交易,新用户交易
                
### example
       
    执行partners场景“用户交易”、“新用户交易”
    
    -Dspring.profiles.active=bitunix-testa -DtestDir=bitunix -Dfeatures= -Denv= -Dstory=partners -Dcomponent=用户交易,新用户交易 -Dtest.name=
                
    执行完成后会在gerow-interface-test\target\allure-results生成allure测试结果
    
    注：powershell会将“.”拆分，用cmd执行，在powershell先执行命令cmd即可。

# 四、生成测试结果

    执行命令：allure serve xxxx\target\allure-results
 
# 五、多环境配置

    1、同springboot配置
    
    2、application-xxx.properties 即为各个环境的配置
    
    3、在测试用例中可调用的参数格式为data.test.xxxx，在测试中调用格式为：${xxxx}
    
### example 
       - name: 总后台平台收益
      host: ${partnersadmin_host}
      url: /partner/root/statistics/rebate
      method: GET
      headers:
        Token: ${partnersadmin_token}
      variables:
        startTime: ${__timeShift(yyyy-MM-dd,,P-1D,zh_CN,)}T16:00:00.000Z
        endTime: ${__timeShift(yyyy-MM-dd,,,zh_CN,)}T16:00:00.000Z
      extractor:
        - json:
            totalRebate: $.result.totalRebate
      assertion: [ json: { $.code: '0' } ]
    - name: 总后台平台收益-开仓
      keyWord: 总后台平台收益
      assertion:
        - json:
            $.result.totalRebate: ${__groovy(${totalRebate} + ${openDealFee} * (100 - ${futureSelfRatio}) / 100)}
          assertionType: EIGHTDECIMALPLACES
    - name: 总后台平台收益-平仓
      keyWord: 总后台平台收益
      assertion:
        - json:
            $.result.totalRebate: ${__groovy(${totalRebate} + ${closeDealFee} * (100 - ${futureSelfRatio}) / 100)}
          assertionType: EIGHTDECIMALPLACES
    - name: 总后台交易额
      host: ${partnersadmin_host}
      url: /partner/root/statistics/tradeAmount
      method: GET
      headers:
        Token: ${partnersadmin_token}
      variables:
        startTime: ${__timeShift(yyyy-MM-dd,,P-1D,zh_CN,)}T16:00:00.000Z
        endTime: ${__timeShift(yyyy-MM-dd,,,zh_CN,)}T16:00:00.000Z
      extractor:
        - json:
            statisticsTradeAmount: $.result.totalAmount
      assertion: [ json: { $.code: '0' } ]
    - name: 总后台交易额-开仓
      keyWord: 总后台交易额
      assertion:
        - json:
            $.result.totalAmount: ${__groovy(${statisticsTradeAmount} + ${openAvgPrice} * ${openAmount})}
          assertionType: EIGHTDECIMALPLACES
    - name: 总后台交易额-平仓
      keyWord: 总后台交易额
      assertion:
        - json:
            $.result.totalAmount: ${__groovy(${statisticsTradeAmount} + ${closeAvgPrice} * ${closeAmount})}
          assertionType: EIGHTDECIMALPLACES
    - name: 总后台交易额-现货
      keyWord: 总后台交易额
      assertion:
        - json:
            $.result.totalAmount: ${__groovy(${statisticsTradeAmount} + ${dealAmount})}
          assertionType: EIGHTDECIMALPLACES
####  参数配置
    
    data:
      test:
        amount: "xxxxxx" -------------------全局变量通过${amount}调用
    
#####    调用格式

######      example1：
      
# 六、JMeter Functions支持

### 1、function调用
    
####    example

        给customerRemark赋值一个随机字符串并保存，后续可以调用：
         
###### 赋值格式
       
        ${__RandomString(8,zxcvbnmlkjhgfdsaqwertyuiopQWERTYUIOPASDFGHJKLZXCVBNM1234567890,customerRemark)}
         
###### 调用格式

        ${customerRemark}
        
### 2、自定义function
        
        Module：gerow-test-utils
        package：com.gerow.test.jmeter.functions

    
#### RandomDate代码示例如下：
        
````
public class RandomDate extends AbstractFunction {

    private static final String KEY = "__RandomDay";

    private CompoundVariable plusDay;

    private CompoundVariable format;

    private CompoundVariable varName;

    private String result;

    @Override
    public String execute(SampleResult previousResult, Sampler currentSampler) {
        build();
        TestUtils.saveVariables(varName, result);
        return result;
    }

    private void build() {
        String fm = format.execute().trim();
        if (StringUtils.isBlank(fm)) {
            fm = "yyyy-MM-dd";
        }
        int plusDay = new Random().nextInt(Integer.valueOf(this.plusDay.execute().trim()));
        LocalDate result = LocalDate.now().plusDays(plusDay);
        this.result = result.format(DateTimeFormatter.ofPattern(fm));
    }

    @Override
    public void setParameters(Collection<CompoundVariable> parameters) throws InvalidVariableException {
        checkParameterCount(parameters, 3, 3);
        CompoundVariable[] compoundVariables = parameters.toArray(new CompoundVariable[0]);
        this.format = compoundVariables[0];
        this.plusDay = compoundVariables[1];
        this.varName = compoundVariables[2];
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
````   

        调用${__RandomDay(yyyy-MM-dd,180,endDate)}：取当前时间开始往后180天随机的某一天，并赋值给endDate

# 七、测试用例编写
    
   [脚本编写介绍](gerow-interface-test "介绍")
   [催单脚本环境搭建](gerow-interface-test/README_CONF.md "环境搭建")
