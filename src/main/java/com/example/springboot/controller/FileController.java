package com.example.springboot.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Dict;
import com.example.springboot.common.AuthAccess;
import com.example.springboot.common.Result;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;

@RestController
@RequestMapping("/file")
public class FileController {

    @Value("${ip:localhost}")
    String ip;

    @Value("${server.port}")
    String port;

    private static final String ROOT_PATH = new File(System.getProperty("user.dir") + File.separator + ".." + File.separator + "files").getAbsolutePath();

    /**
     * 文件上传
     */
    @PostMapping("/upload")
    public Result upload(MultipartFile file) throws IOException {
        // 文件的原始名称 ps：aaa.png
        String originalFilename = file.getOriginalFilename();
        // aaa
        String mainName = FileUtil.mainName(originalFilename);
        // png
        String extName = FileUtil.extName(originalFilename);
        if (!FileUtil.exist(ROOT_PATH)) {
            // 如果当前文件的父级目录不存在，就创建
            FileUtil.mkdir(ROOT_PATH);
        }
        // 如果当前上传的文件已经存在了，那么这个时候我就要重名一个文件名称
        if (FileUtil.exist(ROOT_PATH + File.separator + originalFilename)) {
            originalFilename = System.currentTimeMillis() + "_" + mainName + "." + extName;
        }
        File saveFile = new File(ROOT_PATH + File.separator + originalFilename);
        // 存储文件到本地的磁盘里面去
        file.transferTo(saveFile);
        String url = "http://" + ip + ":" + port + "/file/download/" + originalFilename;
        return Result.success(url);
    }

    /**
     * 下载文件
     */
    @AuthAccess
    @GetMapping("/download/{fileName}")
    public void download(@PathVariable String fileName, HttpServletResponse response) throws IOException {
        response.addHeader("Content-Disposition", "inline;filename=" + URLEncoder.encode(fileName, "UTF-8"));
        String filePath = ROOT_PATH  + File.separator + fileName;
        System.out.println("ROOT_PATH: " + ROOT_PATH);
        System.out.println("fileName: " + fileName);
        System.out.println("filePath: " + filePath);
        System.out.println("File exists: " + FileUtil.exist(filePath));
        if (!FileUtil.exist(filePath)) {
            return;
        }
        byte[] bytes = FileUtil.readBytes(filePath);
        ServletOutputStream outputStream = response.getOutputStream();
        try {
            outputStream.write(bytes);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e){
            e.printStackTrace();
        }
    }

    /**
     * 富文本上传图片
     */
    @PostMapping("/editor/upload")
    public Dict editorUpload(MultipartFile file) throws IOException {
        // 文件的原始名称 aaa.png
        String originalFilename = file.getOriginalFilename();
        // aaa
        String mainName = FileUtil.mainName(originalFilename);
        // png
        String extName = FileUtil.extName(originalFilename);
        if (!FileUtil.exist(ROOT_PATH)) {
            // 如果当前文件的父级目录不存在，就创建
            FileUtil.mkdir(ROOT_PATH);
        }
        // 如果当前上传的文件已经存在了，那么这个时候我就要重名一个文件名称
        if (FileUtil.exist(ROOT_PATH + File.separator + originalFilename)) {
            originalFilename = System.currentTimeMillis() + "_" + mainName + "." + extName;
        }
        File saveFile = new File(ROOT_PATH + File.separator + originalFilename);
        // 存储文件到本地的磁盘里面去
        file.transferTo(saveFile);
        String url = "http://" + ip + ":" + port + "/file/download/" + originalFilename;

        Dict dict = Dict.create().set("errno", 0).set("data", CollUtil.newArrayList(Dict.create().set("url", url)));
        //返回文件的链接，这个链接就是文件的下载地址，这个下载地址就是我的后台提供出来的
        return dict;
    }

    /**
     * 富文本上传视频
     */
    @PostMapping("/editor/uploadVideo")
    public Dict editorUploadVideo(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        // aaa.png
        String mainName = FileUtil.mainName(originalFilename);
        String extName = FileUtil.extName(originalFilename);
        if (!FileUtil.exist(ROOT_PATH)) {
            FileUtil.mkdir(ROOT_PATH);
        }
        if (FileUtil.exist(ROOT_PATH + File.separator + originalFilename)) {
            originalFilename = System.currentTimeMillis() + "_" + mainName + "." + extName;
        }
        File saveFile = new File(ROOT_PATH + File.separator + originalFilename);
        file.transferTo(saveFile);
        String url = "http://" + ip + ":" + port + "/file/download/" + originalFilename;

        Dict dict = Dict.create().set("errno", 0).set("data", Dict.create().set("url", url));
        return dict;
    }
}
