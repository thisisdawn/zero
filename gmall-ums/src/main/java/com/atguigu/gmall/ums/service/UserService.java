package com.atguigu.gmall.ums.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;
import com.atguigu.gmall.ums.entity.UserEntity;

import java.util.Map;

/**
 * 用户表
 *
 * @author fengge
 * @email fengge@atguigu.com
 * @date 2022-03-05 13:48:38
 */
public interface UserService extends IService<UserEntity> {

    PageResultVo queryPage(PageParamVo paramVo);

    Boolean checkUserData(String data, Integer type);

    void register(UserEntity userEntity, String code);

    UserEntity queryUser(String loginName, String password);
}

