/*
 * Copyright (C) 2022 SPEIYOU, Inc. All Rights Reserved.
 */
package com.zzt.operationlog.interceptor;


import static com.zzt.operationlog.interceptor.LoggingContext.CONNECTION;
import static com.zzt.operationlog.interceptor.LoggingContext.TABLE_NAME;
import static com.zzt.operationlog.interceptor.LoggingContext.WILL_DO_EXECUTE;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Invocation;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.update.Update;

/**
 * @author Zhu Zhenting
 * @date 2022/7/13 11:29
 **/
@Slf4j
abstract class AbstractLoggingInterceptor implements Interceptor {
    abstract Object beforeUpdate(Invocation invocation);

    abstract void afterUpdate(Object proceedResult, Invocation invocation, Object beforeUpdate);

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        Object beforeUpdate = beforeUpdate(invocation);
        Object proceed = invocation.proceed();
        runWithTryCatch(() -> afterUpdate(proceed, invocation, beforeUpdate), "afterUpdate");
        return proceed;
    }

    protected void runWithTryCatch(Runnable r, String methodName) {
        try {
            r.run();
        } catch (Exception e) {
            log.error("{} error.", methodName, e);
        }
    }

    protected Optional<ResultSet> executeSql(Invocation invocation, String sql) {
        try {
            Connection connection = CONNECTION.get();
            if (connection == null && invocation.getTarget() instanceof Executor) {
                Executor executor = (Executor) invocation.getTarget();
                connection = executor.getTransaction().getConnection();
            }
            PreparedStatement preparedStatement = connection.prepareStatement(sql);
            preparedStatement.execute();
            return Optional.ofNullable(preparedStatement.getResultSet());
        } catch (Exception e) {
            log.error("executeSql error. sql:{}", sql, e);
        }
        return Optional.empty();
    }

    @SneakyThrows
    public boolean willDoIntercept(Invocation invocation, SqlCommandType commandType) {
        if (invocation.getTarget() instanceof Executor && LoggingContext.backEnd()) {
            LoggingContext.clear();

            Object[] args = invocation.getArgs();
            Executor executor = (Executor) invocation.getTarget();
            MappedStatement ms = (MappedStatement) args[0];
            if (ms.getSqlCommandType() != commandType) {
                return false;
            }
            String tableName = withAnnotation(ms.getBoundSql(args[1]).getSql());
            if (tableName != null) {
                TABLE_NAME.set(tableName);
                CONNECTION.set(executor.getTransaction().getConnection());
                WILL_DO_EXECUTE.set(true);
            }
            return tableName != null;
        }
        return false;
    }

    protected List<Long> fetchId(ResultSet resultSet) {
        if (resultSet == null) {
            return Collections.emptyList();
        }
        List<Long> idList = new ArrayList<>();
        try {
            while (resultSet.next()) {
                idList.add(Long.parseLong(resultSet.getString("id")));
            }
        } catch (Exception e) {
            log.error("fetchId error.", e);
        }
        return idList;
    }

    protected String withAnnotation(String sql) {
        try {
            Statement parse = CCJSqlParserUtil.parse(sql);
            String tableName = "";
            if (parse instanceof Insert) {
                tableName = ((Insert) parse).getTable().getName();
            } else if (parse instanceof Update) {
                tableName = ((Update) parse).getTable().getName();
            } else if (parse instanceof Delete) {
                tableName = ((Delete) parse).getTable().getName();
            }
            return LoggingContext.TABLE_NAME_ANNOTATION.get(tableName) != null ? tableName : null;
        } catch (Exception e) {
            log.error("withAnnotation determine error.sql:{}", sql, e);
        }
        return null;
    }
    //
    //    @Override
    //    public Object plugin(Object target) {
    //        return Plugin.wrap(target, this);
    //    }
}
