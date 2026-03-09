package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.entity.Collect;
import com.example.springboot.service.ICollectService;
import com.example.springboot.service.ICollectService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/collect")
public class CollectController {

    @Autowired
    private ICollectService collectService;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result add(@RequestBody Collect collect) {
        collectService.save(collect);
        return Result.success();
    }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result update(@RequestBody Collect collect) {
        collectService.update(collect);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete")
    public Result delete(@RequestParam Integer id) {
        collectService.remove(id);
        return Result.success();
    }

    /**
     * 查询全部数据
     */
    @GetMapping("/selectAll")
    public Result selectAll() {
        return Result.success(collectService.selectAll());
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/selectById")
    public Result selectById(@RequestParam Integer id) {
        return Result.success(collectService.selectById(id));
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(@RequestParam(defaultValue = "") String name,
                             @RequestParam Integer pageNum,
                             @RequestParam Integer pageSize) {
        return Result.success(collectService.selectPage(pageNum, pageSize, name));
    }

    /**
     * 我的收藏
     */
    @GetMapping("/myCollect")
    public Result myCollect(){
        return Result.success(collectService.myCollect());
    }
}