package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.entity.Carousel;
import com.example.springboot.service.ICarouselService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/carousel")
public class CarouselController {

    @Autowired
    private ICarouselService carouselService;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result add(@RequestBody Carousel carousel) {
        carouselService.save(carousel);
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result update(@RequestBody Carousel carousel) {
        carouselService.update(carousel);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete")
    public Result delete(@RequestParam Integer id) {
        carouselService.remove(id);
        return Result.success();
    }

    /**
     * 查询全部数据
     */
    @GetMapping("/selectAll")
    public Result selectAll() {
        return Result.success(carouselService.selectAll());
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/selectById")
    public Result selectById(@RequestParam Integer id) {
        return Result.success(carouselService.selectById(id));
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(@RequestParam(defaultValue = "") String name,
                             @RequestParam Integer pageNum,
                             @RequestParam Integer pageSize) {
        return Result.success(carouselService.selectPage(pageNum, pageSize, name));
    }
}