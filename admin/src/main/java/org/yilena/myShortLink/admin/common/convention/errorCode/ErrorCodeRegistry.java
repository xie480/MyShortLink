package org.yilena.myShortLink.admin.common.convention.errorCode;

import org.yilena.myShortLink.admin.common.convention.errorCode.codes.IErrorCode;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ErrorCodeRegistry {
    private static final ConcurrentMap<String, IErrorCode> CODE_MAP = new ConcurrentHashMap<>(512);
    private static final Set<String> REGISTERED_CLASSES = ConcurrentHashMap.newKeySet();
 
    static {
        // 注册核心错误码类
        registerClass("org.yilena.myShortLink.admin.common.convention.errorCode.codesUserErrorCodes");
        registerClass("org.yilena.myShortLink.admin.common.convention.errorCode.codesSystemErrorCodes");
        registerClass("org.yilena.myShortLink.admin.common.convention.errorCode.codesRemoteErrorCodes");
    }
 
    public static void register(IErrorCode errorCode) {
        if (CODE_MAP.putIfAbsent(errorCode.code(), errorCode) != null) {
            throw new IllegalStateException("错误码重复: " + errorCode.code());
        }
    }
 
    public static IErrorCode getByCode(String code) {
        return CODE_MAP.get(code);
    }
 
    /**
     * 注册指定类中的所有错误码
     */
    public static void registerClass(String className) {
        // 防止类重复注册
        if (!REGISTERED_CLASSES.add(className.trim())) {
            return;
        }
 
        try {
            // 获取类对象
            Class<?> clazz = Class.forName(className);
            registerClassFields(clazz);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("错误码类未找到: " + className, e);
        }
    }
 
    /**
     * 直接通过Class对象注册
     */
    public static void registerClass(Class<?> clazz) {
        String className = clazz.getName();
        // 二次防重
        if (!REGISTERED_CLASSES.add(className)) {
            return;
        }
        registerClassFields(clazz);
    }
 
    private static void registerClassFields(Class<?> clazz) {
        try {
            // 遍历每个错误码对象并进行注册
            for (Field field : clazz.getDeclaredFields()) {
                int modifiers = field.getModifiers();
                // 检查字段是否是常量且为错误码对象
                if (Modifier.isPublic(modifiers) &&
                    Modifier.isStatic(modifiers) &&
                    Modifier.isFinal(modifiers) &&
                    IErrorCode.class.isAssignableFrom(field.getType())) {
                    
                    // 设置字段可访问权限
                    field.setAccessible(true);
                    // 获取字段值
                    IErrorCode errorCode = (IErrorCode) field.get(null);
                    // 进行注册
                    register(errorCode);
                }
            }
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("访问字段失败: " + clazz.getName(), e);
        }
    }
}