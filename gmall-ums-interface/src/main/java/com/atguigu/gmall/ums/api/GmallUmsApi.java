package com.atguigu.gmall.ums.api;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.ums.entity.UserAddressEntity;
import com.atguigu.gmall.ums.entity.UserEntity;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface GmallUmsApi {

    //    查询用户
    @GetMapping("ums/user/query")
    public ResponseVo<UserEntity> queryUser(@RequestParam("loginName") String loginName, @RequestParam("password") String password);

    //    用户注册
    @PostMapping("ums/user/register")
    public ResponseVo register(UserEntity userEntity,@RequestParam("code") String code);

    //    用户系统数据校验
    @GetMapping("ums/user/check/{data}/{type}")
    public ResponseVo<Boolean> CheckUserData(@PathVariable("data") String data,@PathVariable("type") Integer type);

//    查询收货地址
    @GetMapping("ums/useraddress/userId/{userId}")
    public ResponseVo<List<UserAddressEntity>> queryAddressesByUserId(@PathVariable("userId") Long userId);


    @GetMapping("ums/user/{id}")
    @ApiOperation("详情查询")
    public ResponseVo<UserEntity> queryUserById(@PathVariable("id") Long id);

    }
