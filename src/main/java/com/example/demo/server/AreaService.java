package com.example.demo.server;

import com.example.demo.entity.AreaMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AreaService {

    @Autowired
    AreaMapper mapper;

    public void areaRecord(String area_name, String superior_name) {
        mapper.areaRecord(area_name, superior_name);
    }

    public void hh(){
        System.out.println("hh");
    }
}
