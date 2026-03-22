package com.example.springboot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.springboot.entity.Carousel;
import com.example.springboot.mapper.CarouselMapper;
import com.example.springboot.mapper.GoodsMapper;
import com.example.springboot.service.ICarouselService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CarouselServiceImpl implements ICarouselService {

    @Autowired
    private CarouselMapper carouselMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Override
    public void save(Carousel carousel) {
        carouselMapper.insert(carousel);
    }

    @Override
    public void update(Carousel carousel) {
        carouselMapper.updateById(carousel);
    }

    @Override
    public void remove(Integer id) {
        carouselMapper.deleteById(id);
    }

    @Override
    public List<Carousel> selectAll() {
        return carouselMapper.selectList(null);
    }

    @Override
    public Carousel selectById(Integer id) {
        return carouselMapper.selectById(id);
    }

    @Override
    public IPage<Carousel> selectPage(Integer pageNum, Integer pageSize, String name) {
        Page<Carousel> page = new Page<>(pageNum, pageSize);

        LambdaQueryWrapper<Carousel> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(Carousel::getName,name);

        Page<Carousel> carouselPage = carouselMapper.selectPage(page, queryWrapper);
        carouselPage.getRecords().stream().forEach(carousel -> {
            if (carousel.getGoodsId() != null) {
                com.example.springboot.entity.Goods goods = goodsMapper.selectById(carousel.getGoodsId());
                if (goods != null) {
                    carousel.setGoodsName(goods.getName());
                } else {
                    carousel.setGoodsName("商品已删除");
                }
            } else {
                carousel.setGoodsName("无关联商品");
            }
        });
        return carouselPage;
    }

}