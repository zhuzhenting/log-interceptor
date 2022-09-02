/*
 * Copyright (C) 2022 SPEIYOU, Inc. All Rights Reserved.
 */
package com.zzt.operationlog.interceptor;

import org.apache.ibatis.plugin.Invocation;

/**
 * @author Zhu Zhenting
 * @date 2022/7/13 11:37
 **/
interface InnerLoggingInterceptor {
    Object intercept(Invocation invocation) throws Throwable;

    boolean willDoIntercept(Invocation invocation);
}
