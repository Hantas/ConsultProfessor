package com.example.demo.entity;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import java.util.Date;

@Mapper
@Component(value = "areaMapper")
public interface AreaMapper {
    @Insert("insert into area(area_name, superior_name) values(#{area_name},#{superior_name})")
    int areaRecord(@Param("area_name") String area_name, @Param("superior_name") String superior_name);
}
