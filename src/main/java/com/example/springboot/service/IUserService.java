package com.example.springboot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.springboot.entity.User;

public interface IUserService extends IService<User> {

    User login(User user);

    User register(User user);

    void resetPassword(User user);

    void updatePassword(User user);

}
