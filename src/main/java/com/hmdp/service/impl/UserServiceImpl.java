package com.hmdp.service.impl;

import cn.hutool.core.util.PhoneUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private UserMapper userMapper;
    @Override
    public Result sendCode(String phone) {
        //        判断电话号码是否正确
        if (phone.isEmpty() || !PhoneUtil.isPhone(phone)) {
            return Result.fail("手机号格式错误");
        }
//        获取6位验证法
        String code= RandomUtil.randomNumbers(6);
//        将验证码保存到redis中
        redisTemplate.opsForValue().set("code:"+phone,code,60, TimeUnit.SECONDS);
        log.debug("发送验证码成功，验证码为：{}",code);
        return Result.ok("发送验证码成功");
    }

    @Override
    public Result login(LoginFormDTO loginForm) {
        String phone = loginForm.getPhone();
        if (StrUtil.isBlank(phone)||!PhoneUtil.isPhone(phone)){
            return Result.fail("手机号格式错误");
        }
        String code = loginForm.getCode();
        if (StrUtil.isBlank(code)){
            //todo 账号密码登录
        }
        //验证码登录
        if (redisTemplate.opsForValue().get("code:"+phone).equals(code)){
//            保存用户信息到数据库
            UserDTO userDTO=new UserDTO();
            QueryWrapper<User> queryWrapper = new QueryWrapper<>();
            queryWrapper.lambda().eq(User::getPhone,phone);
            User user = userMapper.selectOne(queryWrapper);
            if (user==null){
                user.setPhone(phone);
                user.setCreateTime(LocalDateTime.now());
                user.setNickName(UUID.randomUUID().toString());
                save(user);
            }
            BeanUtils.copyProperties(user,userDTO);
            return Result.ok();
        }else {
            return Result.fail("验证码错误");
        }
    }
}
