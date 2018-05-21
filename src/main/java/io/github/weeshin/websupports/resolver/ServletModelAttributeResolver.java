package io.github.weeshin.websupports.resolver;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.weeshin.websupports.annotation.JsonArg;
import io.github.weeshin.websupports.util.CommUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.HandlerMapping;

import javax.servlet.http.HttpServletRequest;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;


/**
 * @author sweehaw
 */
public class ServletModelAttributeResolver implements HandlerMethodArgumentResolver {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    private static final String LAST_MODIFIED_USER = "last_modified_user";
    private static final String HEADER_ACCESS_USER = "accessUser";
    private static final String JSONBODY_ATTRIBUTE = "JSON_REQUEST_BODY";

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(JsonArg.class);
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer viewContainer, NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {

        try {

            String className = parameter.getParameterType().getName();

            Class<?> c = Class.forName(className);
            Constructor<?> constructor = c.getConstructor();
            Object object = constructor.newInstance();
            HashMap<String, String> pathVariable = this.getPathVariable(webRequest);
            object = this.getRequestBody(object, parameter, webRequest);

            for (Field f : object.getClass().getDeclaredFields()) {

                this.setPathVariable(f, object, pathVariable);
                this.setParameterValue(f, object, webRequest);
                this.setServletRequest(f, object, webRequest);
                this.setAccessUser(f, object, webRequest);
            }

            return object;

        } catch (Exception ex) {
            this.logger.error(ex.toString(), ex);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private HashMap<String, String> getPathVariable(NativeWebRequest webRequest) {
        HttpServletRequest httpServletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
        return (HashMap<String, String>) httpServletRequest.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
    }

    private Object getRequestBody(Object o, MethodParameter parameter, NativeWebRequest webRequest) {

        try {

            HttpServletRequest servletRequest = webRequest.getNativeRequest(HttpServletRequest.class);
            String jsonBody = (String) webRequest.getAttribute(JSONBODY_ATTRIBUTE, NativeWebRequest.SCOPE_REQUEST);

            if (CommUtils.parseString(jsonBody) == null) {
                jsonBody = CommUtils.getStringFromInputStream(servletRequest.getInputStream());
                webRequest.setAttribute(JSONBODY_ATTRIBUTE, jsonBody, NativeWebRequest.SCOPE_REQUEST);
            }
            return new ObjectMapper().readValue(jsonBody, parameter.getParameterType());
        } catch (Exception ex) {
            return o;
        }
    }

    private void setPathVariable(Field f, Object o, HashMap<String, String> map) throws Exception {

        JsonProperty jsonProperty = f.getAnnotation(JsonProperty.class);

        if (jsonProperty != null) {

            String jsonKey = jsonProperty.value();
            Object value = map.get(jsonKey);

            if (CommUtils.parseString(value) != null) {

                f.setAccessible(true);

                if (f.getType().isAssignableFrom(String.class)) {
                    f.set(o, CommUtils.parseString(value));

                } else if (f.getType().isAssignableFrom(Integer.class)) {
                    f.set(o, CommUtils.parseInt(value));

                }
            }
        }
    }

    private void setServletRequest(Field f, Object o, NativeWebRequest nativeWebRequest) throws Exception {

        if (f.getType().isAssignableFrom(HttpServletRequest.class)) {
            HttpServletRequest request = (HttpServletRequest) nativeWebRequest.getNativeRequest();
            f.setAccessible(true);
            f.set(o, request);
        }
    }

    private void setAccessUser(Field f, Object o, NativeWebRequest nativeWebRequest) throws Exception {

        JsonProperty jsonProperty = f.getAnnotation(JsonProperty.class);

        if (jsonProperty != null) {

            String jsonKey = jsonProperty.value();

            if (jsonKey.contains(LAST_MODIFIED_USER)) {
                String accessUser = nativeWebRequest.getHeader(HEADER_ACCESS_USER);
                f.setAccessible(true);
                f.set(o, accessUser);
            }
        }
    }

    private void setParameterValue(Field f, Object o, NativeWebRequest nativeWebRequest) throws Exception {

        JsonProperty jsonProperty = f.getAnnotation(JsonProperty.class);
        JsonFormat jsonFormat = f.getAnnotation(JsonFormat.class);

        if (jsonProperty != null) {

            String jsonKey = jsonProperty.value();
            Object value = nativeWebRequest.getParameter(jsonKey);

            if (CommUtils.parseString(value) != null) {

                f.setAccessible(true);

                if (f.getType().isAssignableFrom(String.class)) {
                    f.set(o, CommUtils.parseString(value));

                } else if (f.getType().isAssignableFrom(Integer.class)) {
                    f.set(o, CommUtils.parseInt(value));

                } else if (f.getType().isAssignableFrom(BigDecimal.class)) {
                    f.set(o, CommUtils.parseBigDecimal(value));

                } else if (f.getType().isAssignableFrom(Date.class) && jsonFormat != null) {
                    f.set(o, CommUtils.parseDate(jsonFormat.pattern(), value.toString()));

                } else if (f.getType().isAssignableFrom(Long.class)) {
                    f.set(o, CommUtils.parseLong(value));

                }
            }
        }
    }
}