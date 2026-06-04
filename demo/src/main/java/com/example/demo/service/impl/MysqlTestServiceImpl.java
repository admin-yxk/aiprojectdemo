package com.example.demo.service.impl;

import com.example.demo.mapper.MysqlTestMapper;
import com.example.demo.service.MysqlTestService;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * MySQL数据库连接测试服务实现。
 */
@Service
public class MysqlTestServiceImpl implements MysqlTestService {

    private final MysqlTestMapper mysqlTestMapper;

    /**
     * 通过构造器注入MyBatis数据库访问Mapper。
     */
    public MysqlTestServiceImpl(MysqlTestMapper mysqlTestMapper) {
        this.mysqlTestMapper = mysqlTestMapper;
    }

    /**
     * 测试MySQL数据库连接是否可用。
     */
    @Override
    public Map<String, Object> testMysqlConnection() {
        String databaseName = mysqlTestMapper.selectCurrentDatabase();
        Integer result = mysqlTestMapper.selectConnectionTestValue();

        return Map.of(
                "success", true,
                "database", databaseName,
                "result", result,
                "message", "MySQL数据库连接成功"
        );
    }
}
