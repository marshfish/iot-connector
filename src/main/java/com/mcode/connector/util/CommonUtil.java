package com.mcode.connector.util;

import com.mcode.connector.exception.NullParamException;
import com.mcode.connector.mvc.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

@Slf4j
public class CommonUtil {
    private static final String GET = "get";

    public static String buildParam(String... strings) {
        StringBuilder stringBuilder = new StringBuilder();
        for (String str : strings) {
            stringBuilder.append(str);
        }
        return stringBuilder.toString();
    }

    public static String MD5(String message) {
        try {
            return DigestUtils.md5DigestAsHex(message.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void validEmpty(String paramName, Object value) {
        doValid(paramName, value);
    }

    private static void doValid(String fieldName, Object invoke) {
        if (invoke == null) {
            throw new NullParamException(fieldName);
        }
        if (invoke instanceof String) {
            if (StringUtils.EMPTY.equals(((String) invoke).trim())) {
                throw new NullParamException(fieldName);
            }
        }
        if (invoke instanceof Collection) {
            if (((Collection) invoke).size() == 0) {
                throw new NullParamException(fieldName);
            }
        }
    }

    public static void validDTOEmpty(Object dto) {
        if (null == dto) {
            throw new NullParamException("DTO");
        }
        List<String> fieldNames = new LinkedList<>();
        Class<?> aClass = dto.getClass();
        Field[] fields = aClass.getDeclaredFields();
        for (Field field : fields) {
            int modifiers = field.getModifiers();
            if ((modifiers & Modifier.STATIC) == 0 && (modifiers & Modifier.FINAL) == 0) {
                if (field.isAnnotationPresent(NotNull.class)) {
                    fieldNames.add(field.getName());
                }
            }
        }
        for (String fieldName : fieldNames) {
            Method method;
            Object invoke;
            try {
                String methodName = GET + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                method = aClass.getMethod(methodName);
                method.setAccessible(Boolean.TRUE);
                invoke = method.invoke(dto);
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                log.warn("valid param exception:" + e.getMessage(), e);
                return;
            }
            doValid(fieldName, invoke);
        }
    }


}
