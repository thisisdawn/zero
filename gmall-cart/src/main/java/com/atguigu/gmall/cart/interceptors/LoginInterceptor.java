package com.atguigu.gmall.cart.interceptors;

import com.atguigu.gmall.cart.config.JwtProperties;
import com.atguigu.gmall.cart.pojo.UserInfo;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
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

// 编写拦截器, 获取登录状态的拦截
@Component
@EnableConfigurationProperties(JwtProperties.class)
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtProperties properties;

    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        System.out.println("前置方法, 在controller方法执行之前执行");
        // 获取token和 userKey
        String token = CookieUtils.getCookieValue(request, properties.getCookieName());
        String userKey = CookieUtils.getCookieValue(request, properties.getUserKey());
//        判断userKey是否为空, 如果为空则重新生成一个, 放入cookie
        if (StringUtils.isBlank(userKey)) {
            userKey = UUID.randomUUID().toString();
            CookieUtils.setCookie(request,response,properties.getUserKey(),userKey,properties.getExpire());//cookie名称, cookie的值
        }
//        解析token 获取userId
        Long userId = null;
        if (StringUtils.isNotBlank(token)) {
            Map<String, Object> map = JwtUtils.getInfoFromToken(token, properties.getPublicKey());
            userId =  Long.valueOf(map.get("userId").toString());
        }

//      参数传递给后续业务代码
        THREAD_LOCAL.set(new UserInfo(userId,userKey));


        return true;
    }

    public static UserInfo getUserInfo(){
        return THREAD_LOCAL.get();
    }


    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        System.out.println("后置方法, 在controller方法执行之后执行");
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        System.out.println("完成方法, 在视图渲染完成之后执行");
//        由于使用的是线程池, 请求结束, 线程并没有结束, 需要手动释放threadlocal, 否则导致内存泄漏
        THREAD_LOCAL.remove();
    }
}
