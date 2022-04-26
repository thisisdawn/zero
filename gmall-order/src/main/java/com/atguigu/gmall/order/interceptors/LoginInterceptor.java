package com.atguigu.gmall.order.interceptors;

import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.order.pojo.UserInfo;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.UUID;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    //public static UserInfo userInfo;
    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        Long userId = Long.valueOf(request.getHeader("userId"));
        String username = request.getHeader("username");
        // 传递给后续业务代码
        THREAD_LOCAL.set(new UserInfo(userId, null, username));

        return true;
    }

    public static UserInfo getUserInfo(){
        return THREAD_LOCAL.get();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 由于使用的tomcat线程池，请求结束线程并没有结束，所以必须手动释放threadLocal，否则就会导致内存泄漏
        THREAD_LOCAL.remove();
    }
}
