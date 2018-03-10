package com.example.demo.controller;

import com.example.demo.entity.Account;
import com.example.demo.entity.Talk;
import com.example.demo.server.AccountService;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.List;

/**
 * Created by 10742 on 2018/2/8.
 */
@MapperScan("com.example.demo.entity")
@Controller
public class AccountController {
    @Autowired
    AccountService accountService;

    @GetMapping("/")
    public String login() {
        return "login";
    }

    @GetMapping("/test")
    public String test() {
        return "test";
    }

    @PostMapping("/")
    public String checkAccount(String name, String pwd, Model model, HttpSession httpSession){
        Account account = accountService.findAuthor(name);
        if (account != null){
            if (pwd.equals(accountService.getPassword(name)))
                model.addAttribute("account", account);
                httpSession.setAttribute("account", account);
                return "text";
        }
        return "login";
    }

    @PostMapping("/getPrivateChatObj")
    @ResponseBody
    public String getPrivateChatObj(String user_id){
        return accountService.findPrivateProfessor(Long.valueOf(user_id));
    }

    @PostMapping("/getProfessorList")
    @ResponseBody
    public List<Long> getProfessorList(String user_id){
        return accountService.getProfessorList(Long.valueOf(user_id));
    }

}
