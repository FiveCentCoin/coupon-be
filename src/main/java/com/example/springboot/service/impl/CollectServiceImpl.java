package com.example.springboot.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.entity.Collect;
import com.example.springboot.exception.ServiceException;
import com.example.springboot.mapper.CollectMapper;
import com.example.springboot.mapper.CollectMapper;
import com.example.springboot.mapper.GoodsMapper;
import com.example.springboot.mapper.UserMapper;
import com.example.springboot.service.ICollectService;
import com.example.springboot.service.ICollectService;
import com.example.springboot.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class CollectServiceImpl implements ICollectService {

    @Autowired
    private CollectMapper collectMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private UserMapper userMapper;

    @Override
    public void save(Collect collect) {
        Integer userId = TokenUtils.getCurrentUser().getId();
        // 1、判断该用户是否之前收藏过该商品
        // select * from collect where user_id = xx and goods_id = xx
        LambdaQueryWrapper<Collect> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Collect::getUserId, userId);
        queryWrapper.eq(Collect::getGoodsId,collect.getGoodsId());
        Collect one = collectMapper.selectOne(queryWrapper);
        // 2、如果收藏过就给之前的记录删掉，提示已收藏
        if (Objects.nonNull(one)){
            collectMapper.delete(queryWrapper);
            // 200 除200之外的状态码
            throw new ServiceException("201","已取消收藏");
        }
        // 3、如果没收藏，我们就把这条记录插入到数据库
        collect.setUserId(userId);
        collect.setTime(DateUtil.now());
        collectMapper.insert(collect);
    }

    @Override
    public void update(Collect collect) {
        collectMapper.updateById(collect);
    }

    @Override
    public void remove(Integer id) {
        collectMapper.deleteById(id);
    }

    @Override
    public List<Collect> selectAll() {
        return collectMapper.selectList(null);
    }

    @Override
    public Collect selectById(Integer id) {
        return collectMapper.selectById(id);
    }

    @Override
    public IPage<Collect> selectPage(Integer pageNum, Integer pageSize, String name) {
        Page<Collect> page = new Page<>(pageNum, pageSize);

        Page<Collect> collectPage = collectMapper.selectPage(page, null);
        collectPage.getRecords().stream().forEach(collect -> {
            collect.setUserName(userMapper.selectById(collect.getUserId()).getName());
            collect.setGoodsName(goodsMapper.selectById(collect.getGoodsId()).getName());
        });
        return collectPage;
    }

    @Override
    public List<Collect> myCollect() {
        LambdaQueryWrapper<Collect> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(Collect::getUserId, TokenUtils.getCurrentUser().getId());
        List<Collect> collects = collectMapper.selectList(queryWrapper);
        collects.stream().forEach(collect -> {
            collect.setGoods(goodsMapper.selectById(collect.getGoodsId()));
        });
        return collects;
    }

}