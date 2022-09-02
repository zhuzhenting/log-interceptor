/*
 * Copyright (C) 2022 SPEIYOU, Inc. All Rights Reserved.
 */
package com.zzt.operationlog.interceptor;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.reflection.SystemMetaObject;

import com.zzt.operationlog.bo.ParamSupplier;
import com.zzt.operationlog.enums.OperationType;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.expression.operators.relational.MultiExpressionList;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.insert.Insert;

/**
 * @author Zhu Zhenting
 * @date 2022/7/13 14:42
 **/
@Slf4j
class SqlAppender {

    public static final String DEFAULT_TABLE_NAME = "operation_log";
    public static final String DEFAULT_PRIMARY_ID_ALIAS = "target_primary_id";

    public static String insertSql(OperationLogging logging, Collection<?> paramList) {
        List<MetaObject> metas = paramList.stream().map(SystemMetaObject::forObject).collect(Collectors.toList());

        return build(logging, metas, OperationType.INSERT);
    }

    public static String deleteSql(OperationLogging logging, List<MetaObject> metaObject) {
        return build(logging, metaObject, OperationType.DELETE);
    }

    public static String updateSql(OperationLogging logging, List<MetaObject> metaObject) {
        return build(logging, metaObject, OperationType.UPDATE);
    }

    private static String build(OperationLogging logging, List<MetaObject> metas, OperationType operationType) {

        ParamSupplier supplier = LoggingContext.PARAM_SUPPLIER;

        Date now = new Date();
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        Insert insert = new Insert();
        insert.setTable(new Table(logging.logTable()));
        insert.setColumns(buildDefaultColumns(logging));

        insert.setItemsList(new MultiExpressionList().addExpressionLists(convert(metas, formatter.format(now), supplier.operatorId(), supplier.operatorName(), operationType.name(), logging)));
        String sql = insert.toString();
        log.info("[SqlAppender.{}], SQL = {}", operationType.name(), sql);
        return sql;
    }

    private static List<ExpressionList> convert(List<MetaObject> metas, String operationTime,
                                                Long operatorId, String operatorName,
                                                String operationType, OperationLogging logging) {
        return metas.stream().map(metaObject -> {
            Long primaryId = Long.parseLong(metaObject.getValue(LoggingContext.underScoreToCamel(logging.recordKey())).toString());
            String snapshot = JSONUtil.toJsonStr(metaObject.getOriginalObject());
            if (logging.logTable().equals(DEFAULT_TABLE_NAME)) {
                return new ExpressionList(new StringValue(snapshot), new LongValue(primaryId), new StringValue(LoggingContext.TABLE_NAME.get()),
                        new LongValue(operatorId), new StringValue(operatorName), new StringValue(operationType), new StringValue(operationTime));
            }
            return new ExpressionList(new StringValue(snapshot), new LongValue(primaryId), new LongValue(operatorId), new StringValue(operatorName),
                    new StringValue(operationType), new StringValue(operationTime));

        }).collect(Collectors.toList());
    }

    private static List<Column> buildDefaultColumns(OperationLogging logging) {
        if (logging.logTable().equals(DEFAULT_TABLE_NAME)) {
            return Arrays.asList(new Column("snapshot"), new Column(LoggingContext.camelToUnderScore(logging.foreignKey())), new Column("target_table_name"),
                    new Column("operator_id"), new Column("operator_name"), new Column("operation_type"), new Column("operation_time"));
        }
        return Arrays.asList(new Column("snapshot"), new Column(LoggingContext.camelToUnderScore(logging.foreignKey())),
                new Column("operator_id"), new Column("operator_name"), new Column("operation_type"), new Column("operation_time"));
    }

}
