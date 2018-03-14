package com.example.demo.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.example.demo.server.AreaService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@MapperScan("com.example.demo.entity")
@RestController
public class AreaController {
    @Autowired
    AreaService areaService;

    public static int count = 0;

//    @GetMapping("/area")
//    public String recordArea() {
//        json(getJSON());
//        return "success";
//    }

    public String getJSON() {
        StringBuilder sb = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new FileReader(ResourceUtils.getFile("classpath:area-json")));
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void json(String json) {
        int sum = 0;
        JSONObject object = JSON.parseObject(json);
        JSONArray jsonArray = JSON.parseArray(JSON.toJSONString(object.get("zone")));
        for (int i = 0; i < jsonArray.size(); i++) {
            JSONObject jsonObject = (JSONObject) jsonArray.get(i);
            recordAreaRelations(jsonObject);
        }
    }

    public void recordAreaRelations(JSONObject jsonObject) {
        JSONObject childObject = jsonObject;
        if (childObject.containsKey("zone")) {
            JSONArray jsonArray = JSON.parseArray(JSON.toJSONString(childObject.get("zone")));
            if (jsonArray.size() > 0) {
                for (int i = 0; i < jsonArray.size(); i++) {
                    JSONObject object = (JSONObject) jsonArray.get(i);
                    if (!childObject.getString("name").equals(object.getString("name"))) {
                        areaService.areaRecord(object.getString("name"), childObject.getString("name"));
                    }
                    recordAreaRelations(object);
                }
            }
        }
    }
}
