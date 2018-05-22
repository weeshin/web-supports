package io.github.weeshin.websupports.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;

/**
 * @author sweehaw
 */
class LoggerRequestMessage {

    String getUrl(LoggerRequestWrapper request) {

        String url = request.getRequestURL().toString();
        url = url.replace("%7B", "{");
        url = url.replace("%7D", "}");

        return url;
    }

    String getIp(LoggerRequestWrapper request) {
        return request.getRemoteAddr();
    }

    String getMethod(LoggerRequestWrapper request) {
        return request.getMethod();
    }

    Object getHeader(LoggerRequestWrapper request) {

        String s = "authorization;content-length;content-type;accept;";
        HashMap<String, String> m = new HashMap<>(0);

        Enumeration<String> headerNames = request.getHeaderNames();
        if (headerNames != null) {
            while (headerNames.hasMoreElements()) {
                String k = headerNames.nextElement();
                String v = request.getHeader(k);
                if (s.contains(k)) {
                    m.put(k, v);
                }
            }
        }

        return m;
    }

    Object getBody(LoggerRequestWrapper request) {

        try {
            String line = IOUtils.toString(request.getReader());
            return new ObjectMapper().readValue(line, HashMap.class);
        } catch (IOException e) {
            return "";
        }
    }

    String getParam(LoggerRequestWrapper request) {
        return request.getQueryString();
    }
}
