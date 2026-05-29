package com.sc.shortlinkcommon;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)          // 只能用在方法上
@Retention(RetentionPolicy.RUNTIME)  // 运行时有效（反射能读到）
public @interface RateLimit {

    double permitsPerSecond() default 1.0; // 每秒允许的请求数量
    String message() default "请求过于频繁，请稍后重试";

    enum LimitType { GLOBAL, IP }
    LimitType limitType() default LimitType.GLOBAL;

    int windowSeconds() default 1;

}
