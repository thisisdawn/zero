package com.atguigu.gmall.auth;

import com.atguigu.gmall.common.utils.JwtUtils;
import com.atguigu.gmall.common.utils.RsaUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;

public class JwtTest {

    // 别忘了创建D:\\project\rsa目录
//    指定文件
	private static final String pubKeyPath = "D:\\李先锋\\project-dawn\\rsa\\rsa.pub";
    private static final String priKeyPath = "D:\\李先锋\\project-dawn\\rsa\\rsa.pri";

    private PublicKey publicKey;

    private PrivateKey privateKey;

    @Test
    public void testRsa() throws Exception {
//        根据密文，生存rsa公钥和私钥,并写入指定文件
        RsaUtils.generateKey(pubKeyPath, priKeyPath, "234");
    }

    @BeforeEach
    public void testGetRsa() throws Exception {
        this.publicKey = RsaUtils.getPublicKey(pubKeyPath); //获取公钥
        this.privateKey = RsaUtils.getPrivateKey(priKeyPath); //获取私钥
    }

    @Test
    public void testGenerateToken() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("id", "11");
        map.put("username", "liuyan");
        // 生成token
        String token = JwtUtils.generateToken(map, privateKey, 2);
        System.out.println("token = " + token);
    }

    @Test
    public void testParseToken() throws Exception {
        String token = "eyJhbGciOiJSUzI1NiJ9.eyJpZCI6IjExIiwidXNlcm5hbWUiOiJsaXV5YW4iLCJleHAiOjE2NDY2NDI0MDN9.VARNy0UmPCKxdvTLT1zSMz5tY_nPwb4ys2HBcjQDAOB1dmK4lY9Qb8tTUS807QSizQMZM3yUmUBUViUwH0gI9idLMgUYKkkPGqzk4LHHEbd2CNyUYKcY7ZJwB92DlBGfJlis_AxiHYP86VqsxCHx0MnRQRvRh6Q0EBrJ6wvaT4z1sFTb-FGVUTXmgJYx7DMBgo_LRwxPHoh7jsi77OKuZIM4mYaOxkn0ey2FSQ_qIn2o4eoSwVB37Xx5NzCF8yqll8ROjBX1rHUhrW_oc8svHr05wEsWhwrkqI9uZMtkNZ8XnXRJB0JTcsskW8qaDDFvERrQH6x1o6QFkc2kaFQi-A";

        // 解析token
        Map<String, Object> map = JwtUtils.getInfoFromToken(token, publicKey);
        System.out.println("id: " + map.get("id"));
        System.out.println("userName: " + map.get("username"));
    }
}