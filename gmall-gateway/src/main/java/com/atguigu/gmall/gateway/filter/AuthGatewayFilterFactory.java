package com.atguigu.gmall.gateway.filter;


import com.atguigu.gmall.common.utils.IpUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.gateway.config.JwtProperties;
import lombok.Data;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpCookie;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
/*
 * 局部过滤器
 * */

@Component
@EnableConfigurationProperties(JwtProperties.class)
//  1. 编写实现类, 继承抽象类   4. 指定泛型是定义的实体类
public class AuthGatewayFilterFactory extends AbstractGatewayFilterFactory<AuthGatewayFilterFactory.PathConfig> {

    @Autowired
    private JwtProperties jwtProperties;

    //  5. 重写父类的无参构造方法, 调用super(实体类.class)
    public AuthGatewayFilterFactory() {
        super(PathConfig.class);
    }

    //  6. 重写父类的shortcutFieldOrder方法, 指定接受参数的字段顺序
    @Override
    public List<String> shortcutFieldOrder() {
        return Arrays.asList("paths");
    }

    //  8. 重写ShortcutType方法
    @Override
    public ShortcutType shortcutType() {
        return ShortcutType.GATHER_LIST;
    }

    //    拦截的业务逻辑
    @Override
    public GatewayFilter apply(PathConfig config) {
        return new GatewayFilter() {
            @Override
            public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
//            类似于servlet中的HttpServletRequest
                ServerHttpRequest request = exchange.getRequest();
                ServerHttpResponse response = exchange.getResponse();
//            1. 判断当前请求在不在拦截名单里, 不在则直接放行
                List<String> paths = config.paths; //拦截名单
                String curPath = request.getURI().getPath(); //当前请求路径
//                如果拦截名单不为空, 并且当前路径不以拦截名单中的任意路径开头, 放行
                if (paths != null && !paths.stream().anyMatch(path -> curPath.startsWith(path))) {
                    return chain.filter(exchange);
                }

                // 2.获取token。同步-cookie 异步-头信息中
                String token = request.getHeaders().getFirst(jwtProperties.getToken());
                if (StringUtils.isBlank(token)){
                    MultiValueMap<String, HttpCookie> cookies = request.getCookies();
                    if (!CollectionUtils.isEmpty(cookies) && cookies.containsKey(jwtProperties.getCookieName())){
                        HttpCookie cookie = cookies.getFirst(jwtProperties.getCookieName());
                        token = cookie.getValue();
                    }
                }

//            3. 判空. 如果为空, 重定向到登录页面
                if (StringUtils.isBlank(token)) {
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    response.getHeaders().add(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                    return response.setComplete(); //响应结束
                }

                try {
//            4. 解析token, 如果出现异常, 重定向到登录页面
                    Map<String, Object> map = JwtUtils.getInfoFromToken(token, jwtProperties.getPublicKey());
//            5. 判断token载荷中的IP地址, 是否和当前请求的IP地址一致, 不一致重定向到登录页面
                    String ip = map.get("ip").toString();
                    String curIp = IpUtils.getIpAddressAtGateway(request);
                    if (!StringUtils.equals(ip, curIp)) {
                        response.setStatusCode(HttpStatus.SEE_OTHER);
                        response.getHeaders().add(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                        return response.setComplete();
                    }
//            6. 把解析出的载荷信息传递给后续服务 (头信息)
                    request.mutate().header("userId", map.get("userId").toString())
                            .header("username", map.get("username").toString()).build();
                    exchange.mutate().request(request).build();
//            7. 放行
                    return chain.filter(exchange);
                } catch (Exception e) {
                    e.printStackTrace();
                    response.setStatusCode(HttpStatus.SEE_OTHER);
                    response.getHeaders().add(HttpHeaders.LOCATION, "http://sso.gmall.com/toLogin.html?returnUrl=" + request.getURI());
                    return response.setComplete();
                }

            }
        };
    }

    //    3. 定义内部静态实体类, 定义接受参数的字段
    @Data
    public static class PathConfig {
        //    7.  定义集合字段( 拦截名单
        private List<String> paths;
    }
}
