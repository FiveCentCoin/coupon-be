package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.entity.Goods;
import com.example.springboot.service.IGoodsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/goods")
public class GoodsController {

    @Autowired
    private IGoodsService goodsService;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result add(@RequestBody Goods goods) {
        goodsService.save(goods);
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result update(@RequestBody Goods goods) {
        goodsService.update(goods);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete")
    public Result delete(@RequestParam Integer id) {
        goodsService.remove(id);
        return Result.success();
    }

    /**
     * 查询全部数据
     */
    @GetMapping("/selectAll")
    public Result selectAll() {
        return Result.success(goodsService.selectAll());
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/selectById")
    public Result selectById(@RequestParam Integer id) {
        return Result.success(goodsService.selectById(id));
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(@RequestParam(defaultValue = "") String name,
                             @RequestParam Integer pageNum,
                             @RequestParam Integer pageSize) {
        return Result.success(goodsService.selectPage(pageNum, pageSize, name));
    }

    /**
     * 新品上架
     */
    @GetMapping("/times")
    public Result times() {
        return Result.success(goodsService.times());
    }

    /**
     * 热销商品
     */
    @GetMapping("/sales")
    public Result sales() {
        return Result.success(goodsService.sales());
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage/type")
    public Result selectPageType(@RequestParam(defaultValue = "") String name,
                                 @RequestParam Integer typeId,
                             @RequestParam Integer pageNum,
                             @RequestParam Integer pageSize) {
        return Result.success(goodsService.selectPageType(pageNum, pageSize, name,typeId));
    }

    /**
     * echarts商品销量统计分析
     */
    @GetMapping("/echarts")
    public Result echarts(){
        return Result.success(goodsService.echarts());
    }
}