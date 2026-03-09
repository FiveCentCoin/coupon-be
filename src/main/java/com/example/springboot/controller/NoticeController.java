package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.entity.Notice;
import com.example.springboot.service.INoticeService;
import com.example.springboot.service.INoticeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@CrossOrigin
@RestController
@RequestMapping("/notice")
public class NoticeController {

    @Autowired
    private INoticeService noticeService;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result add(@RequestBody Notice notice){
        noticeService.save(notice);
        return Result.success();
   }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result update(@RequestBody Notice notice){
        noticeService.update(notice);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete")
    public Result delete(@RequestParam Integer id){
        noticeService.remove(id);
        return Result.success();
    }

    /**
     * 查询全部数据
     */
    @GetMapping("/selectAll")
    public Result selectAll(){
        return Result.success(noticeService.selectAll());
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/selectById")
    public Result selectById(@RequestParam Integer id){
        return Result.success(noticeService.selectById(id));
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(@RequestParam(defaultValue = "") String name,
                             @RequestParam Integer pageNum,
                             @RequestParam Integer pageSize){
        return Result.success(noticeService.selectPage(pageNum,pageSize,name));
    }
}
