package com.atguigu.gmall.gateway.config;

import com.atguigu.gmall.common.utils.RsaUtils;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.File;
import java.security.PrivateKey;
import java.security.PublicKey;

@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

    private String pubFilePath;
    private String cookieName;
    private String token;

    private PublicKey publicKey;

    @PostConstruct
    public void init(){
        try {
            this.publicKey = RsaUtils.getPublicKey(pubFilePath);
        } catch (Exception e) {
            e.printStackTrace();
    }
    }
}
