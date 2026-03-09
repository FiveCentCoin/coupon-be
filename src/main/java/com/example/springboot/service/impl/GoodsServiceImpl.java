package com.example.springboot.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.entity.Collect;
import com.example.springboot.entity.Goods;
import com.example.springboot.entity.User;
import com.example.springboot.mapper.CollectMapper;
import com.example.springboot.mapper.GoodsMapper;
import com.example.springboot.mapper.TypeMapper;
import com.example.springboot.mapper.UserMapper;
import com.example.springboot.service.IGoodsService;
import com.example.springboot.utils.TokenUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class GoodsServiceImpl implements IGoodsService {

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private TypeMapper typeMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private CollectMapper collectMapper;

    @Override
    public void save(Goods goods) {
        goods.setUserId(TokenUtils.getCurrentUser().getId());
        goodsMapper.insert(goods);
    }

    @Override
    public void update(Goods goods) {
        goodsMapper.updateById(goods);
    }

    @Override
    public void remove(Integer id) {
        goodsMapper.deleteById(id);
    }

    @Override
    public List<Goods> selectAll() {
        return goodsMapper.selectList(null);
    }

    @Override
    public Goods selectById(Integer id) {
        Goods goods = goodsMapper.selectById(id);

        User user = TokenUtils.getCurrentUser();
        if (Objects.nonNull(user)){
            LambdaQueryWrapper<Collect> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(Collect::getUserId, user.getId());
            queryWrapper.eq(Collect::getGoodsId,id);
            Collect one = collectMapper.selectOne(queryWrapper);
            if (Objects.isNull(one)){
                goods.setIsCollect(false);
            } else {
                goods.setIsCollect(true);
            }
        }
        return goods;
    }

    @Override
    public IPage<Goods> selectPage(Integer pageNum, Integer pageSize, String name) {
        Page<Goods> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Goods> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Goods::getName, name);

        Page<Goods> goodsPage = goodsMapper.selectPage(page, queryWrapper);
        goodsPage.getRecords().stream().forEach(goods -> {
            goods.setTypeName(typeMapper.selectById(goods.getTypeId()).getName());
            goods.setUserName(userMapper.selectById(goods.getUserId()).getName());
        });
        return goodsPage;
    }

    @Override
    public List<Goods> times() {
        LambdaQueryWrapper<Goods> queryWrapper = new LambdaQueryWrapper<Goods>();
        queryWrapper.orderByDesc(Goods::getDate);
        return goodsMapper.selectList(queryWrapper).stream().limit(4).collect(Collectors.toList());
    }

    @Override
    public List<Goods> sales() {
        LambdaQueryWrapper<Goods> queryWrapper = new LambdaQueryWrapper<Goods>();
        queryWrapper.orderByDesc(Goods::getSales);
        return goodsMapper.selectList(queryWrapper).stream().limit(4).collect(Collectors.toList());
    }

    @Override
    public IPage<Goods> selectPageType(Integer pageNum, Integer pageSize, String name, Integer typeId) {
        Page<Goods> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Goods> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(StrUtil.isNotBlank(name), Goods::getName, name);
        queryWrapper.eq(typeId != 0, Goods::getTypeId, typeId);

        Page<Goods> goodsPage = goodsMapper.selectPage(page, queryWrapper);
        goodsPage.getRecords().stream().forEach(goods -> {
            goods.setTypeName(typeMapper.selectById(goods.getTypeId()).getName());
            goods.setUserName(userMapper.selectById(goods.getUserId()).getName());
        });
        return goodsPage;
    }

    @Override
    public List<JSONObject> echarts() {
        // 1、查询出所有商品
        List<Goods> goods = goodsMapper.selectList(null);
        // 2、遍历商品
        List<JSONObject> list = new ArrayList<>();
        goods.stream().forEach(good -> {
            JSONObject jsonObject = new JSONObject();
            jsonObject.set("name",good.getName());
            jsonObject.set("value",good.getSales());

            list.add(jsonObject);
        });
        // 3、封装数据
        return list;
    }
}