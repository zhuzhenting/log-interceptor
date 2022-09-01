/*
 * Copyright (C) 2022 SPEIYOU, Inc. All Rights Reserved.
 */
package com.zzt.operationlog.interceptor;

import java.sql.ResultSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Invocation;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;

/**
 * @author Zhu Zhenting
 * @date 2022/7/13 11:38
 **/
@Slf4j
class UpdateLoggingInterceptor extends AbstractUpdateOperation implements InnerLoggingInterceptor {

    @Override
    public boolean willDoIntercept(Invocation invocation) {
        return willDoIntercept(invocation, SqlCommandType.UPDATE);
    }

    private String inExpressionSelect(Collection<Long> ids) {
        String tableName = LoggingContext.TABLE_NAME.get();
        if (StringUtils.isEmpty(tableName)) {
            log.warn("get tableName failed.");
            return "";
        }
        PlainSelect plainSelect = new PlainSelect();
        plainSelect.setSelectItems(Arrays.asList(new AllColumns()));
        plainSelect.setFromItem(new Table(tableName));
        ExpressionList expressionList = new ExpressionList();

        expressionList.setExpressions(ids.stream().map(LongValue::new).collect(Collectors.toList()));

        plainSelect.setWhere(new InExpression().withLeftExpression(new Column("id")).withRightItemsList(expressionList));

        Select selectAll = new Select();
        selectAll.setSelectBody(plainSelect);
        log.info("inExpressionSelect,sql:{},tableName:{}", selectAll, tableName);
        return selectAll.toString();
    }

    @Override
    void afterUpdate(Object proceedResult, Invocation invocation, Object beforeUpdate) {
        boolean rowsAffected = proceedResult instanceof Number && Long.parseLong(proceedResult.toString()) > 0;
        if (!rowsAffected) {
            log.debug("no rows affected.");
            return;
        }
        ResultSet rs = LoggingContext.BEFORE_OPERATION_RESULT.get();
        if (rs == null) {
            return;
        }
        List<Long> ids = fetchId(rs);

        String select = inExpressionSelect(ids);
        if (StringUtils.isEmpty(select)) {
            log.info("UpdateLoggingInterceptor suspend. tableName: {}", LoggingContext.TABLE_NAME.get());
            return;
        }
        Optional<ResultSet> optional = executeSql(invocation, select);
        optional.ifPresent(resultSet -> executeSql(invocation, SqlAppender.updateSql(LoggingContext.TABLE_NAME_ANNOTATION.get(LoggingContext.TABLE_NAME.get()), convert(resultSet))));
    }
}
