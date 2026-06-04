package com.example.demo;

import com.example.demo.service.MysqlTestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * MySQL数据库连接测试。
 */
@SpringBootTest
class MysqlConnectionTests {

    @Autowired
    private MysqlTestService mysqlTestService;

    /**
     * 验证应用可以连接到znkf数据库。
     */
    @Test
    void shouldConnectToMysqlDatabase() {
        Map<String, Object> result = mysqlTestService.testMysqlConnection();

        assertThat(result.get("success")).isEqualTo(true);
        assertThat(result.get("database")).isEqualTo("znkf");
        assertThat(result.get("result")).isEqualTo(1);
    }
}
