package com.example.springboot.controller;

import com.example.springboot.common.Result;
import com.example.springboot.entity.Orders;
import com.example.springboot.service.IOrdersService;
import com.example.springboot.service.IOrdersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@CrossOrigin
@RestController
@RequestMapping("/orders")
public class OrdersController {

    @Autowired
    private IOrdersService ordersService;

    /**
     * 新增
     */
    @PostMapping("/add")
    public Result add(@RequestBody Orders orders){
        ordersService.save(orders);
        return Result.success();
   }

    /**
     * 修改
     */
    @PutMapping("/update")
    public Result update(@RequestBody Orders orders){
        ordersService.update(orders);
        return Result.success();
    }

    /**
     * 删除
     */
    @DeleteMapping("/delete")
    public Result delete(@RequestParam Integer id){
        ordersService.remove(id);
        return Result.success();
    }

    /**
     * 查询全部数据
     */
    @GetMapping("/selectAll")
    public Result selectAll(){
        return Result.success(ordersService.selectAll());
    }

    /**
     * 根据ID查询
     */
    @GetMapping("/selectById")
    public Result selectById(@RequestParam Integer id){
        return Result.success(ordersService.selectById(id));
    }

    /**
     * 分页查询
     */
    @GetMapping("/selectPage")
    public Result selectPage(@RequestParam(defaultValue = "") String name,
                             @RequestParam(defaultValue = "") String orderNo,
                             @RequestParam Integer pageNum,
                             @RequestParam Integer pageSize){
        return Result.success(ordersService.selectPage(pageNum,pageSize,name,orderNo));
    }

    /**
     * 支付接口
     */
    @PostMapping("/pay")
    public Result pay(@RequestBody Orders orders){
        ordersService.pay(orders);
        return Result.success();
    }

    /**
     * 批量支付接口
     */
    @PostMapping("/batchPay")
    public Result batchPay(@RequestBody Map<String, Object> params){
        List<Integer> orderIds = (List<Integer>) params.get("orderIds");
        Long couponId = params.get("couponId") != null ? Long.valueOf(params.get("couponId").toString()) : null;
        ordersService.batchPay(orderIds, couponId);
        return Result.success();
    }
}
