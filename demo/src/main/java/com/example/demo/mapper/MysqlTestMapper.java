package com.example.demo.mapper;

/**
 * MySQL数据库连接测试Mapper。
 */
public interface MysqlTestMapper {

    /**
     * 查询当前连接的数据库名称。
     */
    String selectCurrentDatabase();

    /**
     * 执行简单查询验证数据库连接。
     */
    Integer selectConnectionTestValue();
}
