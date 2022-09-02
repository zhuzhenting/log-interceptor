/*
 * Copyright (C) 2022 SPEIYOU, Inc. All Rights Reserved.
 */
package com.zzt.operationlog.interceptor;

import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.mapping.ParameterMode;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.TypeHandlerRegistry;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Zhu Zhenting
 * @date 2022/7/29 10:59
 **/
@Slf4j
class ParameterHandlerWrapper {
    private final Invocation invocation;
    private final MetaObject metaObject;
    private SqlCommandType sqlCommandType;
    private MappedStatement mappedStatement;
    private String sql = null;

    public ParameterHandlerWrapper(Invocation invocation) {
        this.invocation = invocation;
        if (!(invocation.getTarget() instanceof ParameterHandler)) {
            throw new UnsupportedOperationException();
        }
        metaObject = SystemMetaObject.forObject(invocation.getTarget());
    }

    //    BoundSql boundSql = (BoundSql) metaObjectParameterHandler.getValue("boundSql");
    //    Configuration configuration = (Configuration) metaObjectParameterHandler.getValue("configuration");
    //    TypeHandlerRegistry typeHandlerRegistry = (TypeHandlerRegistry) metaObjectParameterHandler.getValue("typeHandlerRegistry");

    public MappedStatement mappedStatement() {
        if (mappedStatement != null) {
            return mappedStatement;
        }
        mappedStatement = (MappedStatement) metaObject.getValue("mappedStatement");
        return mappedStatement;
    }

    public String sql() {
        if (sql != null) {
            return sql;
        }
        ParameterHandler parameterHandler = (ParameterHandler) invocation.getTarget();
        Object parameterObject = parameterHandler.getParameterObject();

        MetaObject metaObjectParameterHandler = SystemMetaObject.forObject(parameterHandler);

        BoundSql boundSql = (BoundSql) metaObjectParameterHandler.getValue("boundSql");
        Configuration configuration = (Configuration) metaObjectParameterHandler.getValue("configuration");
        TypeHandlerRegistry typeHandlerRegistry = (TypeHandlerRegistry) metaObjectParameterHandler.getValue("typeHandlerRegistry");

        List<Object> paramValues = new ArrayList<>();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        if (parameterMappings != null) {
            for (int i = 0; i < parameterMappings.size(); i++) {
                ParameterMapping parameterMapping = parameterMappings.get(i);
                if (parameterMapping.getMode() != ParameterMode.OUT) {
                    Object value;
                    String propertyName = parameterMapping.getProperty();
                    if (boundSql.hasAdditionalParameter(propertyName)) {
                        value = boundSql.getAdditionalParameter(propertyName);
                    } else if (parameterObject == null) {
                        value = null;
                    } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
                        value = parameterObject;
                    } else {
                        MetaObject metaObject = configuration.newMetaObject(parameterObject);
                        value = metaObject.getValue(propertyName);
                    }
                    paramValues.add(value);
                }
            }
        }

        sql = toStringSql(paramValues, boundSql.getSql());
        log.info("[final sql]:{}", sql);
        return sql;
    }

    private String toStringSql(List<Object> keys, String sql) {
        if (keys.isEmpty()) {
            return sql;
        }
        StringBuilder returnSQL = new StringBuilder();
        String[] subSQL = sql.split("\\?");
        for (int i = 0; i < keys.size(); i++) {
            returnSQL.append(subSQL[i]).append(" '").append(keys.get(i)).append("' ");
            if (i == keys.size() - 1) {
                try {
                    returnSQL.append(subSQL[keys.size()]);
                } catch (Exception ignored) {
                }
            }
        }
        return returnSQL.toString();
    }

    public SqlCommandType sqlCommandType() {
        if (sqlCommandType != null) {
            return sqlCommandType;
        }
        sqlCommandType = mappedStatement().getSqlCommandType();
        return sqlCommandType;
    }

}
