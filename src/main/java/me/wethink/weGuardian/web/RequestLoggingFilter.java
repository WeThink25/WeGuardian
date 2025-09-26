package me.wethink.weGuardian.web;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import me.wethink.weGuardian.WeGuardian;

import java.io.IOException;

public class RequestLoggingFilter implements Filter {

    private final WeGuardian plugin;

    public RequestLoggingFilter(WeGuardian plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init(FilterConfig filterConfig) {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        try {
            if (request instanceof HttpServletRequest req) {
                String ip = request.getRemoteAddr();
                String method = req.getMethod();
                String uri = req.getRequestURI();
                plugin.getLogger().info("[Web] " + ip + " -> " + method + " " + uri);
            }
        } catch (Exception ignored) {
        }
        chain.doFilter(request, response);
    }

    @Override
    public void destroy() {

    }
}
