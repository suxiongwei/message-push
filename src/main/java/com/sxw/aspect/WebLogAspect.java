package com.sxw.aspect;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;

/**
 * 日志记录
 * */
@Aspect
@Order(5)
@Component
public class WebLogAspect {
	
	private Logger logger = LoggerFactory.getLogger(getClass());

    ThreadLocal<Long> startTime = new ThreadLocal<>();

    private static final String TAB = "	";
    
    @Pointcut("execution(public * com.sxw.*.controller..*.*(..))")
    public void webLog(){}

    /**
     * 请求日志格式：URL + HTTP_METHOD + IP + CLASS_METHOD + ARGS
     * URL 请求的url
     * HTTP_METHOD 请求方式  get/post
     * IP 来源ip
     * CLASS_METHOD 请求的方法
     * ARGS 请求参数
     * */
    @Before("webLog()")
    public void doBefore(JoinPoint joinPoint) throws Throwable {
        startTime.set(System.currentTimeMillis());

        // 接收到请求，记录请求内容
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes.getRequest();
        // 记录下请求内容
		logger.info(request.getRequestURL().toString() + TAB
				+ request.getMethod() + TAB + request.getRemoteAddr() + TAB
				+ joinPoint.getSignature().getDeclaringTypeName() + "."
				+ joinPoint.getSignature().getName() + TAB
				+ Arrays.toString(joinPoint.getArgs()));

    }

    /**
     * 返回结果日志格式：RESPONSE + SPEND TIME
     * RESPONSE 返回结果
     * SPEND TIME 执行时间
     * */
    @AfterReturning(returning = "ret", pointcut = "webLog()")
    public void doAfterReturning(Object ret) throws Throwable {
        // 处理完请求，返回内容
        logger.info(ret+TAB+(System.currentTimeMillis() - startTime.get())+"ms");
    }
}
