/*
 * Copyright (C) 2022 SPEIYOU, Inc. All Rights Reserved.
 */
package com.zzt.operationlog.interceptor;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.base.CaseFormat;
import com.zzt.operationlog.bo.ParamSupplier;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author Zhu Zhenting
 * @date 2022/7/13 14:14
 **/
final class LoggingContext {

    /**
     * 生命周期：容器
     */
    static Map<String, OperationLogging> TABLE_NAME_ANNOTATION = new ConcurrentHashMap<>();
    static Map<String, Class> TABLE_CLASS_MAPPING = new HashMap<>();
    public static ParamSupplier PARAM_SUPPLIER = null;

    public static ThreadLocal<String> TABLE_NAME = new ThreadLocal<>();
    public static ThreadLocal<Connection> CONNECTION = new ThreadLocal<>();
    public static ThreadLocal<Boolean> WILL_DO_EXECUTE = new ThreadLocal<>();
    public static ThreadLocal<ResultSet> BEFORE_OPERATION_RESULT = new ThreadLocal<>();

    @Data
    @Accessors(chain = true)
    public static class ContextParam {
        private Connection connection;
        private boolean willDoExecute;
        private String tableName;
    }

    public static void clear() {
        TABLE_NAME.remove();
        WILL_DO_EXECUTE.remove();
        BEFORE_OPERATION_RESULT.remove();
        CONNECTION.remove();
    }

    public static boolean backEnd() {
        return PARAM_SUPPLIER != null && PARAM_SUPPLIER.operatorId() != null;
    }

    static String camelToUnderScore(String source) {
        return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, source);
    }

    static String underScoreToCamel(String source) {
        return CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, source);
    }
}
