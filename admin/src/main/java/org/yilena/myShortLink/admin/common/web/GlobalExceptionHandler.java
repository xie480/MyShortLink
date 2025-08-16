package org.yilena.myShortLink.admin.common.web;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.yilena.myShortLink.admin.common.convention.errorCode.codes.UserErrorCodes;
import org.yilena.myShortLink.admin.common.convention.exception.AbstractException;
import org.yilena.myShortLink.admin.common.convention.result.Result;
import org.yilena.myShortLink.admin.common.convention.result.Results;

import java.io.IOException;
import java.util.Optional;

@Slf4j
@Component
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(value = {IOException.class, ServletException.class})
    public Result handleServletException(HttpServletRequest request, Exception ex) {
        log.error("过滤器异常 [{}] {} ", request.getMethod(), getUrl(request), ex);
        return Results.failure();
    }
    
    // 拦截参数验证异常
    @SneakyThrows
    @ExceptionHandler(value = MethodArgumentNotValidException.class)
    public Result validExceptionHandler(HttpServletRequest request, MethodArgumentNotValidException ex) {
           // 获取绑定结果，用于后续处理验证错误信息
        BindingResult bindingResult = ex.getBindingResult();
        // 从绑定结果中获取第一个字段错误信息，以便后续处理
        FieldError firstFieldError = CollectionUtil.getFirst(bindingResult.getFieldErrors());
        // 使用Optional处理可能为null的错误信息，以避免空指针异常
        // 如果存在错误信息，则获取默认的错误消息；否则使用空字符串
        String exceptionStr = Optional.ofNullable(firstFieldError)
                .map(FieldError::getDefaultMessage)
                .orElse(StrUtil.EMPTY);
        // 记录错误日志，包括请求方法、请求URL和错误信息
        log.error("[{}] {} [ex] {}", request.getMethod(), getUrl(request), exceptionStr);
        // 返回包含错误代码和错误信息的响应结果
        return Results.failure(UserErrorCodes.USER_ERROR.code(), exceptionStr);
    }
 
    
    // 拦截应用内抛出的异常
    @ExceptionHandler(value = {AbstractException.class})
    public Result abstractException(HttpServletRequest request, AbstractException ex) {
        // 检查异常的原因是否为空
        if (ex.getCause() != null) {
            // 如果原因不为空，记录包含原因的错误日志，并返回失败结果
            log.error("[{}] {} [ex] {}", request.getMethod(), request.getRequestURL().toString(), ex, ex.getCause());
            return Results.failure(ex);
        }
        // 创建一个StringBuilder来构建异常的堆栈跟踪信息
        StringBuilder stackTraceBuilder = new StringBuilder();
        // 添加异常的类名和错误消息到StringBuilder中
        stackTraceBuilder.append(ex.getClass().getName()).append(": ").append(ex.getErrorMessage()).append("\n");
        // 获取异常的堆栈跟踪元素
        StackTraceElement[] stackTrace = ex.getStackTrace();
        // 遍历堆栈跟踪的前五个元素（或全部，如果少于五个），并添加到StringBuilder中
        for (int i = 0; i < Math.min(5, stackTrace.length); i++) {
            stackTraceBuilder.append("\tat ").append(stackTrace[i]).append("\n");
        }
        // 记录包含异常堆栈跟踪信息的详细错误日志，并返回失败结果
        log.error("[{}] {} [ex] {} \n\n{}", request.getMethod(), request.getRequestURL().toString(), ex, stackTraceBuilder);
        return Results.failure(ex);
    }
 
    
    // 拦截自定义以外的异常
    @ExceptionHandler(value = Throwable.class)
    public Result defaultErrorHandler(HttpServletRequest request, Throwable throwable) {
        log.error("[{}] {} ", request.getMethod(), getUrl(request), throwable);
        return Results.failure();
    }
 
    // 获取请求的url
    private String getUrl(HttpServletRequest request) {
        if (StrUtil.isEmpty(request.getQueryString())) {
            return request.getRequestURL().toString();
        }
        return request.getRequestURL().toString() + "?" + request.getQueryString();
    }
}