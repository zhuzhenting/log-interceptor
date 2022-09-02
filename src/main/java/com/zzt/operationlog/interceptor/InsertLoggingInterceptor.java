/*
 * Copyright (C) 2022 SPEIYOU, Inc. All Rights Reserved.
 */
package com.zzt.operationlog.interceptor;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.keygen.Jdbc3KeyGenerator;
import org.apache.ibatis.executor.keygen.NoKeyGenerator;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.session.defaults.DefaultSqlSession;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Zhu Zhenting
 * @date 2022/7/13 11:40
 **/
@Slf4j
class InsertLoggingInterceptor extends AbstractLoggingInterceptor implements InnerLoggingInterceptor {
    @Override
    Object beforeUpdate(Invocation invocation) {
        try {
            MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
            Field keyGenerator = ms.getClass().getDeclaredField("keyGenerator");
            keyGenerator.setAccessible(true);
            Object kg = keyGenerator.get(ms);
            if (kg == null || kg == NoKeyGenerator.INSTANCE) {
                keyGenerator.set(ms, Jdbc3KeyGenerator.INSTANCE);
            }

            Field keyProperties = ms.getClass().getDeclaredField("keyProperties");
            keyProperties.setAccessible(true);
            Object o = keyProperties.get(ms);
            if (o == null) {
                keyProperties.set(ms, new String[] {"id"});
            }
        } catch (Exception e) {
            log.error("[InsertLoggingInterceptor.willDoIntercept error]keyGenerator assign keyGenerator failed ", e);
        }
        return null;
    }

    @Override
    public boolean willDoIntercept(Invocation invocation) {
        boolean result = willDoIntercept(invocation, SqlCommandType.INSERT);
        LoggingContext.CONNECTION.remove();
        LoggingContext.WILL_DO_EXECUTE.remove();
        return result;
    }

    private Collection<?> getParamList(Object parameter) {
        if (parameter instanceof MapperMethod.ParamMap || parameter instanceof DefaultSqlSession.StrictMap) {
            HashMap map = (HashMap) parameter;
            if (map.isEmpty()) {
                return null;
            }
            return collectionize(map.values().iterator().next());
        }

        if (parameter instanceof ArrayList && !((ArrayList<?>) parameter).isEmpty()
                && ((ArrayList<?>) parameter).get(0) instanceof MapperMethod.ParamMap) {
            return (ArrayList<MapperMethod.ParamMap<?>>) parameter;
        }
        return collectionize(parameter);
    }

    private static Collection<?> collectionize(Object param) {
        if (param instanceof Collection) {
            return (Collection<?>) param;
        }
        if (param instanceof Object[]) {
            return Arrays.asList((Object[]) param);
        }
        return Arrays.asList(param);
    }

    @Override
    void afterUpdate(Object proceedResult, Invocation invocation, Object beforeUpdate) {
        Object params = invocation.getArgs()[1];

        Collection<?> paramList = getParamList(params);
        if (CollectionUtils.isEmpty(paramList)) {
            return;
        }
        OperationLogging annotation = paramList.iterator().next().getClass().getAnnotation(OperationLogging.class);

        executeSql(invocation, SqlAppender.insertSql(annotation, paramList));
    }
}
