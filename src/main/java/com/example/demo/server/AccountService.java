package com.example.demo.server;

import com.example.demo.entity.Account;
import com.example.demo.entity.AccountMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

/**
 * Created by 10742 on 2018/2/8.
 */
@Service
public class AccountService {

    @Autowired
    private AccountMapper mapper;

    public String getPassword(String name) {
        return this.mapper.getPassword(name);
    }

    public Account findAuthor(String name) {
        return this.mapper.findAuthor(name);
    }

    public String findPrivateProfessor(Long user_id) {
        return mapper.findPrivateProfessor(user_id).toString();
    }

    public List<Long> getProfessorList(Long user_id) {
        return mapper.getProfessorList(user_id);
    }

    public String getName(Long user_id) {
        return mapper.getName(user_id);
    }

}
