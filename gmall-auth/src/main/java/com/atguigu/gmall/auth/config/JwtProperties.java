package com.atguigu.gmall.auth.config;

import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import javax.annotation.PostConstruct;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

//批量读取
@Data
@Configuration
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String pubFilePath;
    private String priFilePath;
    private String secret;
    private Integer expire;
    private String cookieName;
    private String unick;

    private PublicKey publicKey;
    private PrivateKey privateKey;

    @PostConstruct
    public void init()  {
//        判空, 两文件如有一个不存在, 即要重新生成公钥密钥
        try {
            File pubFile = new File(pubFilePath);
            File priFile = new File(priFilePath);
            if (!pubFile.exists() || !priFile.exists()){
                RsaUtils.generateKey(pubFilePath,priFilePath,secret);
            }
            this.publicKey = RsaUtils.getPublicKey(pubFilePath);
            this.privateKey = RsaUtils.getPrivateKey(priFilePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}
