package com.example.demo.entity;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * Created by 10742 on 2018/2/8.
 */
@Mapper
@Component(value = "accountMapper")
public interface AccountMapper {

    @Select("select password from account where name = #{name}")
    String getPassword(@Param("name") String name);

    @Select("select user_id,name,role,area from account where name = #{name}")
    Account findAuthor(@Param("name") String name);

    @Select("select user_id from account where role = 'professor' and area in(select area from account where user_id = #{user_id})")
    Long findPrivateProfessor(@Param("user_id") Long user_id);

    @Select("select user_id from account where role = 'professor' and user_id <> #{user_id}")
    List<Long> getProfessorList(@Param("user_id") Long user_id);

    @Select("select name from account where user_id = #{user_id}")
    String getName(@Param("user_id") Long user_id);

//    @Select("select role from account where user_id = #{user_id}")
//    String findRoleByID(@Param("user_id") Long user_id);
}
