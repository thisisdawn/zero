package com.atguigu.gmall.auth.service;

import com.atguigu.gmall.auth.config.JwtProperties;
import com.atguigu.gmall.auth.exception.AuthException;
import com.atguigu.gmall.auth.feign.GmallUmsClient;
import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.common.utils.CookieUtils;
import com.atguigu.gmall.common.utils.IpUtils;
import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.ums.entity.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

@Service
@EnableConfigurationProperties(JwtProperties.class)
public class AuthService {

    @Autowired
    private GmallUmsClient gmallUmsClient;

    @Autowired
    private JwtProperties jwtProperties;

    public void login(String loginName, String password, HttpServletRequest request, HttpServletResponse response) {
//        1. 根据登录名 和 密码 查询用户
        ResponseVo<UserEntity> userEntityResponseVo = this.gmallUmsClient.queryUser(loginName, password);
        UserEntity userEntity = userEntityResponseVo.getData();

//        2. 判空
        if (userEntity == null) {
            throw new AuthException("用户或密码错误");
        }

//        3. 组装载荷
        Map<String, Object> map = new HashMap<>();
        map.put("userId",userEntity.getId());
        map.put("username",userEntity.getUsername());
        //防止jwt盗用, 加入本机ip地址
        map.put("ip", IpUtils.getIpAddressAtService(request));

//        4. 生成token--jwt
        try {
            String token = JwtUtils.generateToken(map, this.jwtProperties.getPrivateKey(), this.jwtProperties.getExpire());
//        5. 放入cookie
            CookieUtils.setCookie(request,response,this.jwtProperties.getCookieName(),token,this.jwtProperties.getExpire()*60);
//        6. 设置昵称
            CookieUtils.setCookie(request,response,this.jwtProperties.getUnick(),userEntity.getNickname(),this.jwtProperties.getExpire()*60);
        } catch (Exception e) {
            e.printStackTrace();
        }



    }
}
