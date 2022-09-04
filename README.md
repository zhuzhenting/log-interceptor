

## 介绍

log-interceptor 是一个基于mybatis插件开发的 数据表写操作行为记录组件，通过简单配置即可记录`当前操作人`对目标数据的`插入`、`更新`、`删除`操作。



## 优点

和以往的AOP记录日志方式不同，你不需要在历史或当前正在开发的方法上加各种注解，各种变量，甚至之后新开发的数据写方法你都不需要担心因为遗漏注解操作而导致日志记录丢失。



## 接入流程

### 1. 创建数据表

```sql
CREATE TABLE `operation_log` (
  `id` bigint(16) NOT NULL AUTO_INCREMENT,
  `snapshot` text,
  `target_table_name` varchar(255) DEFAULT NULL,
  `target_primary_id` bigint(20) DEFAULT NULL,
  `operator_id` bigint(20) DEFAULT NULL,
  `operation_type` varchar(255) DEFAULT NULL,
  `operation_time` datetime DEFAULT NULL,
  `operator_name` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 2. 注入bean

```java
@Bean
public OperationLogInterceptor operationLogInterceptor() {
    return new OperationLogInterceptor(new ParamSupplier() {
        @Override
        public Long operatorId() {
           // 具体取决于业务自身实现逻辑，推荐从线程上下文获取
           // eg: RequestHolder.operatorName();
        }

        @Override
        public String operatorName() {
            //具体取决于业务自身实现逻辑，推荐从线程上下文获取
            // eg: RequestHolder.operatorName();
        }
    }, "com.zzt"); // 这里的包路径就是和表 bean 对应的包路径，例如下面的 com.zzt.User类
}
```



### 3.在数据表对应的Bean上添加注解

```java
package com.zzt.bean; // 这里的包路径在 第二点 中用到
@OperationLogging
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;
}
```


### 4.最终效果

<img width="1056" alt="image" src="https://user-images.githubusercontent.com/73268312/188319353-fcda2e48-09fb-44c6-b4a3-cf2d6b6c389d.png">
