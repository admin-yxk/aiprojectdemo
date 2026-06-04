package com.example.demo.service;

import java.util.Map;

/**
 * MySQL数据库连接测试服务。
 */
public interface MysqlTestService {

    /**
     * 测试MySQL数据库连接是否可用。
     */
    Map<String, Object> testMysqlConnection();
}
