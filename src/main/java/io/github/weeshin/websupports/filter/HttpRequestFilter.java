package io.github.weeshin.websupports.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.weeshin.websupports.util.CommUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author sweehaw
 */
@Component
public class HttpRequestFilter extends GenericFilterBean {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private boolean printRequest;
    private boolean printResponse;

    public HttpRequestFilter(boolean printRequest, boolean printResponse) {
        this.printRequest = printRequest;
        this.printResponse = printResponse;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        LoggerRequestWrapper loggerRequestWrapper = new LoggerRequestWrapper(request);
        LoggerRequestMessage loggerRequestMessage = new LoggerRequestMessage();
        LoggerResponseWrapper loggerResponseWrapper = new LoggerResponseWrapper(response);

        String randomString = this.getRandomString(request);
        this.requestLogger(loggerRequestMessage, loggerRequestWrapper, randomString);

        loggerRequestWrapper.resetInputStream();
        if (!this.isRandomStringExist(request)) {
            loggerRequestWrapper.addRandomString(randomString);
        }

        chain.doFilter(loggerRequestWrapper, loggerResponseWrapper);
        this.responseLogger(response, loggerResponseWrapper, randomString);

    }

    private void requestLogger(LoggerRequestMessage loggerRequestMessage, LoggerRequestWrapper loggerRequestWrapper, String randomString) throws JsonProcessingException {

        if (this.printRequest) {
            ObjectMapper m = new ObjectMapper();

            String url = loggerRequestMessage.getUrl(loggerRequestWrapper);
            String ip = loggerRequestMessage.getIp(loggerRequestWrapper);
            String method = loggerRequestMessage.getMethod(loggerRequestWrapper);
            Object header = loggerRequestMessage.getHeader(loggerRequestWrapper);
            Object param = loggerRequestMessage.getParam(loggerRequestWrapper);
            Object body = loggerRequestMessage.getBody(loggerRequestWrapper);

            this.logger.info("");
            this.logger.info("{} ====================================== Incoming ======================================", randomString);
            this.logger.info("{} U: {}", randomString, url);
            this.logger.info("{} M: {}", randomString, method);
            this.logger.info("{} I: {}", randomString, ip);
            this.logger.info("{} H: {}", randomString, m.writeValueAsString(header));
            this.logger.info("{} P: {}", randomString, param);
            this.logger.info("{} B: {}", randomString, body instanceof String ? body : m.writeValueAsString(body));
        }
    }

    private void responseLogger(HttpServletResponse response, LoggerResponseWrapper loggerResponseWrapper, String randomString) throws IOException {

        if (this.printResponse) {
            loggerResponseWrapper.flushBuffer();
            byte[] copy = loggerResponseWrapper.getCopy();
            this.logger.info("{} R: {}", randomString, new String(copy, response.getCharacterEncoding()));
        }
    }

    private String getRandomString(HttpServletRequest request) {
        return this.isRandomStringExist(request)
                ? request.getHeader("randomString")
                : CommUtils.randomAlphanumeric();
    }

    private boolean isRandomStringExist(HttpServletRequest request) {
        return request.getHeader("randomString") != null;
    }
}
