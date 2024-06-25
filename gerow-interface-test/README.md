#   介绍
    1、通用的接口测试框架，测试的host和账号密码相关通过maven导入具体的module
    2、测试用例使用yml编写
    3、测试报告使用Allure
    4、运行mvn test 
        可选参数：
            -Dspring.profiles.active={环境} 
            -Dtest.file.name={className}
            -Dtest.name={methodName} 
        
       example：
       
       -Dspring.profiles.active=ipa-test -DtestDir=淘宝 -Dfeatures=催单 -Dplatform=融合版 -Dtest.severity=ALL -Denv=mini-test -DshopName=wangjq_1990 -Dstory=下单未付款,已付款 -Dcomponent=任务开关,催单消息 -Dtest.name=  

       -Dspring.profiles.active=ipa-test 被测服务环境 ipa-test
       
       -Dfeatures=催单                    测的功能点为催单
       
       -Dplatform=融合版                  平台为融合版
       
       -Dtest.severity=ALL 执行的用例等级为所有等级，可选参数：    BLOCKER
                                                            CRITICAL
                                                            NORMAL
                                                            MINOR
                                                            TRIVIAL
       
#   测试用例编写

##  定义

####    Suite

    整个测试套件，包含所有的测试
    
####    class

    一个yml文件为一个testClass
|字段名称|描述|数据类型
|---|---|---|
|name|class名称|String
|dataProvider|测试数据|List\<Map>
|invocationCount|运行次数|int
|beforeSuite|所有测试运行前运行|TestMethod
|beforeClass|此class下所有测试运行前运行|TestMethod
|beforeMethod|此class每个测试前运行|TestMethod
|testMethod|测试方法|List\<TestMethod>
|afterSuite|所有测试运行后运行|TestMethod
|afterClass|此class下所有测试运行后运行|TestMethod
|afterMethod|此class每个测试后运行|TestMethod
|heartbeat|测试过程中每间隔5s执行一次的请求|TestMethod

####    testMethod

    一个class下可以有多个testMethod
    
|字段名称|描述|数据类型
|---|---|---|
|name|测试名称|String
|dataProvider|测试数据|List\<Map>
|invocationCount|运行次数|int
|step|测试步骤|testStep
    
####    testStep

    一个testMethod下可以有多个testStep
|字段名称|描述|数据类型
|---|---|---|
|name|步骤名称|String
|keyWord|通过step名称复制step|String
|dataProvider|测试数据|List\<Map>
|host|主机地址（配置文件中默认配置，可修改）|String
|url|请求地址|String
|variables|url参数|Map
|method|请求方式|String
|headers|请求头|Map
|form|form表单|Map
|file|所要上传的文件|key:(默认file)form表单的key path:文件路径
|body|请求体|String
|assertion|判断|Assertion
|extractor|保存参数|Extractor

###### example

    - name: 获取MP后台地址
      url: /api/auth/mp_switcher
      variables:
        subnick: ${shopName}
      method: GET
      assertion:
        - json:
            $.code: 0
      extractor:
        - json:
            auth_url: $.url

####    Assertion

|字段名称|描述|数据类型
|---|---|---|
|type|判断的数据类型(默认为响应体)|DataType
|valueType|值的类型（可不填，当值的类型不明确时填写）|ValueType
|assertionType|判断类型（EQ,CONTAINS等）|AssertionType
|key|获取数据的key，比如jsonPath（$.msg）|String
|value|预期结果|Object

###### example

      - keyWord: 查看trace详情
        assertion:
          - json:
              $.data.traces[?(@.phase=='TaskReject')].task_end_rule: [send_limit]             
              
       调用查看trace详情的方法，并判断拒绝原因为“send_limit”
       
       - name: 查看trace详情
         #获取trace详情，会在测试报告中体现，用于定位任务为什么失败
         untilWait: 10
         url: /api/client/reminder/v2/trace/info
         variables:
           trace_id: ${trace_id}
           shop_id: ${shop_id}
         method: GET
         
####    Extractor 参数提取器

|字段名称|描述|数据类型
|---|---|---|
|site|保存数据在哪里，默认class(可选step、method、class、suite)|SaveType
|type|数据源类型，默认json|DataType
|sources|数据源，默认响应体|String
|name|保存值的key|String
|value|获取值的方法，比如jsonPath（$.data.id）|String

###### example

    - name: 获取shop_id
      url: /api/admin/user/logined
      method: GET
      extractor:
        - json:
            shop_id: $.user.shop_id
            shop_category_id: $.default_shop.category_id
          site: TESTSUIT
      assertion: [json: {$.code: 0}]
      
     
    获取shop_id和类目，并保存再测试套件中
  
####    dataProvider

    测试数据，List<Map>数组，list长度即为测试运行的次数
    在class下定义，会实例化多个class
    在testMethod下定义，会实例化多个testMethod
    在testStep下定义，会实例化多个testStep
    
    dataProvider:
      - node_state: asked
        reminder_type: 咨询未下单
        
    调用格式：见beforeClass里的${node_state}

####    beforeSuite

    所有测试之前运行，只运行1次
    格式同testMethod
    
####    beforeClass

    当前yml文件下的所有测试之前运行
    格式同testMethod
    
        example:
        
        beforeClass:
          name: 获取taskID和模板
          step:
            - name: 获取所有的task_id
              url: /api/admin/reminder/v2/manage/task/list
              variables:
                template_keys: 0
                node_type: ${node_state}
              method: GET
              assertion: [json: {$.code: 0}]
              extractor:
                - json:
                    task_list: $.data.tasks[*].id
                  size: 100
                  options: [DEFAULT_PATH_LEAF_TO_NULL]
            - keyWord: 删除催单任务
              name: 删除其他催单任务
              iter:
                task_id: ${task_list}
            - name: 获取task模板
              url: /api/admin/reminder/v2/manage/task/template
              variables:
                node_type: ${node_state}
                template_key: 0
              method: GET
              #断言响应体中jsonPath为$.code的值为0
              assertion: [json: {$.code: 0}]
              extractor:
                - json:
                    #提取响应体中的task模板内容,保存为参数task_info
                    task_info: $.data
            - name: 使用模板新建一个task
              url: /api/admin/reminder/v2/manage/task/create
              method: POST
              body: ${task_info}
              assertion: [json: {$.code: 0}]
              extractor:
                - json:
                    task_id: $.data.id
            - name: 修改咨询未下单内容
              url: /api/admin/reminder/v2/manage/task/update
              method: POST
              #task_info为测试开始前获取到的任务模板
              body: ${task_info}
              #bodyEditor根据设置的jsonPath修改对应的参数
              bodyEditor:
                json:
                  #id为beforeClass中提取到的task_id
                  $.id: ${task_id}
                  #开启任务
                  $.enable: true
                  #设置shop_id(配置文件中设置的)
                  $.shop_id: ${shop_id}
                  #设置消息发送时间为0,马上触发
                  $.rules[?(@.type=='state_delay')].args.delay: 0
                  $.rules[?(@.type=='send_message_by_intent')].args[?(@.intent=='')].enable: true
                  $.rules[?(@.type=='send_message_by_intent')].args[?(@.intent=='想要优惠')].enable: true
                  $.rules[?(@.type=='send_message_by_intent')].args[?(@.intent=='想要快点发货')].enable: true
                  $.rules[?(@.type=='send_message_by_intent')].args[?(@.intent=='想要快点发货')].replies:
                    - ageing_id: ""
                      message: 你好~喜欢就不要犹豫啦，下午6点前拍下并付款，快递当天就能来拿货，您的包裹将以最快的速度投奔到您的怀里
                  $.rules[?(@.type=='send_message_by_intent')].args[?(@.intent=='关注商品质量')].enable: true
                  $.rules[?(@.type=='send_message_by_intent')].args[?(@.intent=='想要包邮')].enable: true
                  $.rules[?(@.type=='send_message_by_intent')].args[?(@.intent=='想要赠品')].enable: true
                  $.rules[?(@.type=='send_message_by_intent')].args[?(@.intent=='关注快递类型')].enable: true
                  $.rules[?(@.type=='send_message_by_intent')].args[?(@.intent=='关注快递类型')].replies:
                    - ageing_id: ""
                      message: 您好～喜欢就不要犹豫啦，我们会给您安排尽快发货，让你早点收到宝贝哦～
              assertion: [json: {$.code: 0}]
    
####    AfterSuite

    有测试运行完成之后运行
    格式同testMethod

####    AfterClass

    当前yml文件下的所有测试运行完成之后运行
    格式同testMethod
    
    example:
    
    afterClass:
      name: 测试结束后删除任务
      step:
        - name: 删除催单任务
          url: /api/admin/reminder/v2/manage/task/delete
          method: POST
          body: |
            {"id":"${task_id}"}
            
    测试结束后删除催单任务

#### classHeartbeat

    整个测试类运行过程中，每5秒运行一次
    
    example:
    classHeartbeat:
      name: 心跳检测表示客服在线
      step:
        - name: 主账号发送心跳检测
          url: /log/hb
          variables:
            spin: cntaobao${seller_name}
            platid: tb
            hangup: false
            platstatus: 1
            shopid: ${shop_id}
            version: 4.33.0
            sign: ${__HmacMD5(spin=cntaobao${seller_name})}
          method: GET
          assertion: [json: {$.code: 0}]

### idea中运行

    1、运行一次gerowAIInterfaceTest
    2、编辑gerowAIInterfaceTest, 加入运行参数
     
     -ea -Dspring.profiles.active=ipa-test -Dplatform=test -Dfeatures=mytest -Dtest.severity=ALL -Denv=mini-test -DshopName=wanggerow_1990 -Dstory= -Dcomponent= -Dtest.name=
     运行/gerow-interface-test/src/test/resources/my目录下的test模块测试用例
    
    
    -ea -Dspring.profiles.active=ipa-test -DtestDir=淘宝 -Dplatform=融合版 -Dfeatures=催单 -Dtest.severity=ALL -Denv=mini-test -DshopName=wanggerow_1990 -Dstory=下单未付款,已付款 -Dcomponent=任务开关,催单消息 -Dtest.name=
    运行/gerow-interface-test/src/test/resources/淘宝目录下的催单模块测试用例
    
#### 相关参考资料

    JSONPath: https://blog.csdn.net/lwg_1540652358/article/details/84111339
    
    Ymal：https://www.runoob.com/w3cnote/yaml-intro.html
   
   [框架介绍](.. "介绍")
   [催单脚本环境搭建](README_CONF.md "环境搭建")