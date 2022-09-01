/*
 * Copyright (C) 2022 SPEIYOU, Inc. All Rights Reserved.
 */
package com.zzt.operationlog.interceptor;

import static com.zzt.operationlog.interceptor.LoggingContext.TABLE_NAME;

import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.select.AllColumns;
import net.sf.jsqlparser.statement.select.FromItem;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectExpressionItem;
import net.sf.jsqlparser.statement.update.Update;

/**
 * @author Zhu Zhenting
 * @date 2022/7/19 10:23
 **/
@Slf4j
abstract class AbstractUpdateOperation extends AbstractLoggingInterceptor {

    @Override
    void afterUpdate(Object proceedResult, Invocation invocation, Object beforeUpdate) {
        // do nothing
    }

    @Override
    Object beforeUpdate(Invocation invocation) {
        return null;
    }

    @SneakyThrows
    protected List<MetaObject> convert(ResultSet resultSet) {
        List<Map<String, String>> result = new ArrayList<>();
        Class tableBean = LoggingContext.TABLE_CLASS_MAPPING.get(TABLE_NAME.get());
        Field[] fields = tableBean.getDeclaredFields();

        while (resultSet.next()) {
            Map<String, String> map = new HashMap<>();
            for (Field field : fields) {
                String value = getColumnValue(resultSet, field.getName());
                if (value == null) {
                    continue;
                }
                map.put(field.getName(), value);
            }
            result.add(map);
        }
        List<MetaObject> metas = result.stream().map(SystemMetaObject::forObject).collect(Collectors.toList());
        return metas;
    }

    protected String parseToSelect(String sql) {
        try {
            Statement statement = CCJSqlParserUtil.parse(sql);

            Select select = new Select();
            select.accept(new UpdateToSelectVisitor(statement));
            return select.toString();
        } catch (JSQLParserException e) {
            log.error("UpdateLoggingInterceptor beforeUpdate error.", e);
        }
        return null;
    }

    protected static class UpdateToSelectVisitor extends StatementVisitorAdapter {
        private final Statement statement;
        private Expression whereExpression;
        private FromItem table;

        public UpdateToSelectVisitor(Statement statement) {
            this.statement = statement;
            if (statement instanceof Update) {
                whereExpression = ((Update) statement).getWhere();
                table = ((Update) statement).getTable();
            } else if (statement instanceof Delete) {
                whereExpression = ((Delete) statement).getWhere();
                table = ((Delete) statement).getTable();
            }
        }

        @Override
        public void visit(Select select) {

            PlainSelect plainSelect = new PlainSelect();

            plainSelect.setWhere(whereExpression);
            plainSelect.setFromItem(table);
            SelectExpressionItem selectExpressionItem = new SelectExpressionItem();
            selectExpressionItem.setExpression(new AllColumns());

            plainSelect.setSelectItems(Arrays.asList(selectExpressionItem));

            select.setSelectBody(plainSelect);
            log.info("UpdateLoggingInterceptor.parseToSelect. updateSql:[{}], selectSql:[{}]", statement, select);
        }
    }

    protected String getColumnValue(ResultSet resultSet, String fieldName) {
        try {
            String columnName = LoggingContext.camelToUnderScore(fieldName);
            String value = resultSet.getString(columnName);
            return value == null ? "" : value;
        } catch (Exception e) {
            log.warn("getColumnValue error.columnName:{}.", fieldName, e);
            return null;
        }
    }
}
