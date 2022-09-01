/*
 * Copyright (C) 2022 SPEIYOU, Inc. All Rights Reserved.
 */
package com.zzt.operationlog.bo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * @author Zhu Zhenting
 * @date 2022/5/25 17:20
 **/
@Data
@Accessors(chain = true)
public class OperatorInfo {
    private Long accountId;
}
