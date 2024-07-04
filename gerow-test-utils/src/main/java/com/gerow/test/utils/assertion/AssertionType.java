package com.gerow.test.utils.assertion;

public enum AssertionType {
    /**
     * 等于
     */
    EQ,
    /**
     * 实际包含期望值
     */
    CONTAINS,
    /**
     * 所有结果等于
     */
    ALLIS,
    /**
     * 所有结果包含
     */
    AllCONTAINS,
    /**
     * 大于
     */
    GREATEROREQUALTO,
    /**
     * 小于
     */
    LESSTHANOREQUALTO,
    /**
     * 保留8位小数比较大小
     */
    EIGHTDECIMALPLACES,
    /**
     * 保留8位小数比较大小
     */
    TWODECIMALPLACES,
    /**
     * 所有结果大于
     */
    ALLGREATEROREQUALTO,
    /**
     * 所有结果小于
     */
    ALLLESSTHANOREQUALTO,
    TOTAL,
    ONEOF,
    NOT,
}
