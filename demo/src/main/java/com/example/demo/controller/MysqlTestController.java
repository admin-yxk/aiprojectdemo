package com.example.demo.controller;

import com.example.demo.service.MysqlTestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * MySQL数据库连接测试接口。
 */
@RestController
public class MysqlTestController {

    private final MysqlTestService mysqlTestService;

    /**
     * 通过构造器注入MySQL数据库连接测试服务。
     */
    public MysqlTestController(MysqlTestService mysqlTestService) {
        this.mysqlTestService = mysqlTestService;
    }

    /**
     * 测试MySQL数据库连接是否可用。
     */
    @GetMapping("/mysql/test")
    public Map<String, Object> testMysqlConnection() {
        return mysqlTestService.testMysqlConnection();
    }
}
