/*
 * Copyright (C) 2022 SPEIYOU, Inc. All Rights Reserved.
 */
package com.zzt.operationlog.interceptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Zhu Zhenting
 * @date 2022/7/13 11:05
 **/
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface OperationLogging {
    /**
     * 日志表名称
     */
    String logTable() default "operation_log";

    /**
     * 字段别名，单独指定日志表时，当前字段必填
     */
    String foreignKey() default "target_primary_id";

    String recordKey() default "id";
}