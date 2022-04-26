package com.atguigu.gmall.ums.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;
import java.util.UUID;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.atguigu.gmall.common.bean.PageResultVo;
import com.atguigu.gmall.common.bean.PageParamVo;

import com.atguigu.gmall.ums.mapper.UserMapper;
import com.atguigu.gmall.ums.entity.UserEntity;
import com.atguigu.gmall.ums.service.UserService;


@Service("userService")
public class UserServiceImpl extends ServiceImpl<UserMapper, UserEntity> implements UserService {

    @Override
    public PageResultVo queryPage(PageParamVo paramVo) {
        IPage<UserEntity> page = this.page(
                paramVo.getPage(),
                new QueryWrapper<UserEntity>()
        );

        return new PageResultVo(page);
    }

    @Override
    public Boolean checkUserData(String data, Integer type) {

        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();

        switch(type){
            case 1: wrapper.eq("username",data);break;
            case 2: wrapper.eq("phone",data);break;
            case 3: wrapper.eq("email",data);break;
            default:
                return null;
        }

        return this.count(wrapper) == 0;

    }

    @Override
    public void register(UserEntity userEntity, String code) {
//        校验短信二维码 (省略
//        生成盐
        String salt = StringUtils.substring(UUID.randomUUID().toString(), 0, 6);
        userEntity.setSalt(salt);

//        加盐加密
        userEntity.setPassword(DigestUtils.md5Hex(userEntity.getPassword()+salt));

//        新增用户
        userEntity.setLevelId(1l);
        userEntity.setNickname(userEntity.getUsername());
        userEntity.setSourceType(1);
        userEntity.setIntegration(1000);
        userEntity.setGrowth(1000);
        userEntity.setStatus(1);
        userEntity.setCreateTime(new Date());
        this.save(userEntity);

//        删除redis中对应二维码
    }

    @Override
    public UserEntity queryUser(String loginName, String password) {
//        根据用户名查询用户
        QueryWrapper<UserEntity> wrapper = new QueryWrapper<>();
        wrapper.eq("username",loginName).or().eq("nickname",loginName)
                .or().eq("phone",loginName).or().eq("email",loginName);
        UserEntity userEntity = this.getOne(wrapper);
        //        判空, 说明用户名输错
        if (userEntity == null) {
            return userEntity;
        }
//        对用户输入的密码, 加盐加密
        password = DigestUtils.md5Hex((password + userEntity.getSalt()));
//        加盐加密后的密码 和 用户的密文密码 比较
        if (!StringUtils.equals(password, userEntity.getPassword())) {
            return null;
        }

        return userEntity;
    }

}