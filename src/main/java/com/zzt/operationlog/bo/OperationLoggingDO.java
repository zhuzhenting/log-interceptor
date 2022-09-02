/*
 * Copyright (C) 2022 SPEIYOU, Inc. All Rights Reserved.
 */
package com.zzt.operationlog.bo;

import java.util.Date;

import lombok.Data;

/**
 * @author Zhu Zhenting
 * @date 2022/7/13 11:07
 **/
@Data
public class OperationLoggingDO {
    private Long id;
    private String snapshot;
    /**
     * 数据表主键id
     */
    private Long targetTableId;
    private Long operatorId;
    private String operationType;

    private Date operationTime;
}
