package io.github.weeshin.websupports.filter;


import org.apache.commons.io.IOUtils;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

/**
 * @author sweehaw
 */
public class LoggerRequestWrapper extends HttpServletRequestWrapper {

    private byte[] rawData;
    private final HttpServletRequest request;
    private final ResettableServletInputStream servletStream;
    private final Map<String, String> headerMap = new HashMap<>();

    LoggerRequestWrapper(HttpServletRequest request) {
        super(request);
        this.request = request;
        this.servletStream = new ResettableServletInputStream();
    }

    void resetInputStream() {
        this.servletStream.stream = new ByteArrayInputStream(this.rawData);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {

        if (this.rawData == null) {
            this.rawData = IOUtils.toByteArray(this.request.getReader(), Charset.defaultCharset());
            this.servletStream.stream = new ByteArrayInputStream(this.rawData);
        }
        return servletStream;
    }

    @Override
    public BufferedReader getReader() throws IOException {

        if (this.rawData == null) {
            this.rawData = IOUtils.toByteArray(this.request.getReader(), Charset.defaultCharset());
            this.servletStream.stream = new ByteArrayInputStream(this.rawData);
        }
        return new BufferedReader(new InputStreamReader(this.servletStream));
    }

    @Override
    public String getHeader(String name) {
        String headerValue = super.getHeader(name);
        if (this.headerMap.containsKey(name)) {
            headerValue = this.headerMap.get(name);
        }
        return headerValue;
    }

    void addRandomString(String value) {
        this.headerMap.put("randomString", value);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        List<String> names = Collections.list(super.getHeaderNames());
        names.addAll(this.headerMap.keySet());
        return Collections.enumeration(names);
    }

    @Override
    public Enumeration<String> getHeaders(String name) {
        List<String> values = Collections.list(super.getHeaders(name));
        if (this.headerMap.containsKey(name)) {
            values.add(this.headerMap.get(name));
        }
        return Collections.enumeration(values);
    }

    private class ResettableServletInputStream extends ServletInputStream {

        private InputStream stream;

        @Override
        public int read() throws IOException {
            return this.stream.read();
        }

        @Override
        public boolean isFinished() {
            return false;
        }

        @Override
        public boolean isReady() {
            return false;
        }

        @Override
        public void setReadListener(ReadListener readListener) {
        }
    }
}