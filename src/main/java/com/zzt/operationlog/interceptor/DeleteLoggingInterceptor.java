/*
 * Copyright (C) 2022 SPEIYOU, Inc. All Rights Reserved.
 */
package com.zzt.operationlog.interceptor;

import java.sql.ResultSet;
import java.util.List;

import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.reflection.MetaObject;
import org.springframework.util.CollectionUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Zhu Zhenting
 * @date 2022/7/19 10:24
 **/
@Slf4j
class DeleteLoggingInterceptor extends AbstractUpdateOperation implements InnerLoggingInterceptor {

    @Override
    public boolean willDoIntercept(Invocation invocation) {
        return willDoIntercept(invocation, SqlCommandType.DELETE);
    }

    @Override
    void afterUpdate(Object proceedResult, Invocation invocation, Object beforeUpdate) {
        ResultSet resultSet = LoggingContext.BEFORE_OPERATION_RESULT.get();
        if (resultSet == null) {
            return;
        }

        List<MetaObject> metaObjects = convert(resultSet);
        if (CollectionUtils.isEmpty(metaObjects)) {
            return;
        }
        String sql = SqlAppender.deleteSql(LoggingContext.TABLE_NAME_ANNOTATION.get(LoggingContext.TABLE_NAME.get()), metaObjects);
        executeSql(invocation, sql);
    }

    //    @Override
    //    Object beforeUpdate(Invocation invocation) {
    //        //        String parsedSelect = parseToSelect(invocation);
    //        //        if (parsedSelect == null) {
    //        //            return null;
    //        //        }
    //        //
    //        //        Optional<ResultSet> optional = executeSql(invocation, parsedSelect);
    //        //
    //        //        if (optional.isPresent()) {
    //        //            ResultSet resultSet = optional.get();
    //        //            List<MetaObject> metaObjects = convert(resultSet);
    //        //            if (CollectionUtils.isEmpty(metaObjects)) {
    //        //                return null;
    //        //            }
    //        //            String sql = SqlAppender.update(LoggingContext.TABLE_NAME_ANNOTATION.get(LoggingContext.TABLE_NAME.get()), metaObjects);
    //        //            executeSql(invocation, sql);
    //        //        }
    //        //        return null;
    //        //    }
    //        return null
}
