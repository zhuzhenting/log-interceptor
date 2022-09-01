/*
 * Copyright (C) 2022 SPEIYOU, Inc. All Rights Reserved.
 */
package com.zzt.operationlog.interceptor;

import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;

import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.util.ClassUtils;

import com.baomidou.mybatisplus.annotation.TableName;
import com.zzt.operationlog.bo.ParamSupplier;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Zhu Zhenting
 * @date 2022/7/12 17:57
 **/
@Intercepts({@Signature(type = ParameterHandler.class, method = "setParameters", args = {PreparedStatement.class}),
        @Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class})})
@Slf4j
public class OperationLogInterceptor implements Interceptor, InitializingBean {

    private final String BEAN_PACKAGE_DIR;
    private final List<InnerLoggingInterceptor> interceptors = new ArrayList<>();

    public OperationLogInterceptor(ParamSupplier supplier) {
        this(supplier, "com.zzt");
    }

    public OperationLogInterceptor(ParamSupplier supplier, String packageDir) {
        LoggingContext.PARAM_SUPPLIER = supplier;
        BEAN_PACKAGE_DIR = packageDir;
        register(new ParamLoggingInterceptor());
        register(new InsertLoggingInterceptor());
        register(new UpdateLoggingInterceptor());
        register(new DeleteLoggingInterceptor());
    }

    /**
     * 后期需要支持用户自行注册
     *
     * @param interceptor
     */
    void register(InnerLoggingInterceptor interceptor) {
        interceptors.add(interceptor);
    }

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        try {
            for (InnerLoggingInterceptor interceptor : interceptors) {
                if (interceptor.willDoIntercept(invocation)) {
                    return interceptor.intercept(invocation);
                }
            }
        } catch (Exception e) {
            log.error("OperationLogInterceptor.intercept error", e);
        }
        return invocation.proceed();
    }

    private static final String RESOURCE_PATTERN = "/**/*.class";

    @Override
    public void afterPropertiesSet() throws Exception {
        ResourcePatternResolver resourcePatternResolver = new PathMatchingResourcePatternResolver();
        String pattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +
                ClassUtils.convertClassNameToResourcePath(BEAN_PACKAGE_DIR) + RESOURCE_PATTERN;
        Resource[] resources = resourcePatternResolver.getResources(pattern);
        MetadataReaderFactory factory = new CachingMetadataReaderFactory(resourcePatternResolver);
        for (Resource resource : resources) {
            try {
                MetadataReader reader = factory.getMetadataReader(resource);
                String clzName = reader.getClassMetadata().getClassName();
                Class<?> clazz = Class.forName(clzName);
                OperationLogging operationLogging = clazz.getDeclaredAnnotation(OperationLogging.class);
                if (operationLogging == null) {
                    continue;
                }
                TableName tb = clazz.getDeclaredAnnotation(TableName.class);
                String tableName;
                if (tb != null) {
                    tableName = tb.value();
                } else {
                    tableName = LoggingContext.camelToUnderScore(clazz.getSimpleName());
                }
                LoggingContext.TABLE_NAME_ANNOTATION.put(tableName, operationLogging);
                LoggingContext.TABLE_CLASS_MAPPING.put(tableName, clazz);
            } catch (Throwable e) {
                // ignore
            }
        }
        log.info("the whole tableName and annotation mapping:{}", LoggingContext.TABLE_NAME_ANNOTATION);
    }
}
