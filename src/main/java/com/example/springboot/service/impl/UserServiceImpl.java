package com.example.springboot.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.springboot.entity.User;
import com.example.springboot.exception.ServiceException;
import com.example.springboot.mapper.UserMapper;
import com.example.springboot.service.IUserService;
import com.example.springboot.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private UserMapper userMapper;


    @Override
    public boolean save(User entity) {
        if (StrUtil.isBlank(entity.getName())) {
            entity.setName(entity.getUsername());
        }
        if (StrUtil.isBlank(entity.getPassword())) {
            entity.setPassword("123");
        }
        if (StrUtil.isBlank(entity.getRole())) {
            entity.setRole("USER");
        }
        return super.save(entity);
    }

    public User selectByUsername(String username,String role) {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);  //  eq => ==   where username = #{username}
        queryWrapper.eq("role", role);
        // 根据用户名查询数据库的用户信息，相当于select * from user where username = #{username}
        return getOne(queryWrapper);
    }

    // 验证用户账户是否合法
    public User login(User user) {
        User dbUser = selectByUsername(user.getUsername(),user.getRole());
        if (dbUser == null) {
            throw new ServiceException("用户名或密码错误");
        }
        if (!user.getPassword().equals(dbUser.getPassword())) {
            throw new ServiceException("用户名或密码错误");
        }
        // 生成token
        String token = TokenUtils.createToken(dbUser.getId().toString(), dbUser.getPassword());
        dbUser.setToken(token);
        return dbUser;
    }

    public User register(User user) {
        User dbUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername,user.getUsername()));
        if (dbUser != null) {
            throw new ServiceException("用户名已存在");
        }
        user.setName(user.getUsername());
        userMapper.insert(user);
        return user;
    }

    public void resetPassword(User user) {
        User dbUser = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getUsername,user.getUsername()));
        if (dbUser == null) {
            throw new ServiceException("用户不存在");
        }
        if (!user.getPhone().equals(dbUser.getPhone())) {
            throw new ServiceException("验证错误");
        }
        dbUser.setPassword("123");
        updateById(dbUser);
    }

    @Override
    public void updatePassword(User user) {
        int update = userMapper.updatePassword(user);
        if (update < 1) {
            throw new ServiceException("原始密码错误");
        }
    }
}
