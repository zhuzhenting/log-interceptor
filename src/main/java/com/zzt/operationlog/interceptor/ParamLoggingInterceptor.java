package com.zzt.operationlog.interceptor;

import static com.zzt.operationlog.interceptor.LoggingContext.WILL_DO_EXECUTE;

import java.sql.ResultSet;
import java.util.Collections;
import java.util.Optional;

import org.apache.ibatis.executor.parameter.ParameterHandler;
import org.apache.ibatis.plugin.Invocation;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class ParamLoggingInterceptor extends AbstractUpdateOperation implements InnerLoggingInterceptor {
    @Override
    Object beforeUpdate(Invocation invocation) {
        try {
            ParameterHandlerWrapper wrapper = new ParameterHandlerWrapper(invocation);
            if (!StringUtils.hasText(wrapper.sql())) {
                return null;
            }
            log.info("[final sql]:{}", wrapper.sql());

            String parsedSelect = parseToSelect(wrapper.sql());
            if (parsedSelect == null) {
                return Collections.emptyList();
            }

            Optional<ResultSet> optional = executeSql(invocation, parsedSelect);
            if (optional.isPresent()) {
                LoggingContext.BEFORE_OPERATION_RESULT.set(optional.get());
                return optional.get();
            }
        } catch (Exception e) {
            log.error("[ParamLoggingInterceptor error.]", e);
        } finally {
            WILL_DO_EXECUTE.remove();
        }
        return null;
    }

    @Override
    void afterUpdate(Object proceedResult, Invocation invocation, Object beforeUpdate) {
    }

    @Override
    public boolean willDoIntercept(Invocation invocation) {
        return invocation.getTarget() instanceof ParameterHandler && Boolean.TRUE.equals(WILL_DO_EXECUTE.get());
    }

}