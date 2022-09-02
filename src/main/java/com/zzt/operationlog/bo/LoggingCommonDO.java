/*
 * Copyright (C) 2022 SPEIYOU, Inc. All Rights Reserved.
 */
package com.zzt.operationlog.bo;

import lombok.Data;

/**
 * @author Zhu Zhenting
 * @date 2022/7/19 11:11
 **/
@Data
public class LoggingCommonDO {
    protected String snapshot;
    protected String operatorId;
    protected String operationType;
    protected String operationTime;
}
