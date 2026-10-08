package com.couserver;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.utility.DockerImageName;

// 테스트마다 Docker로 MySQL을 띄워 접속 정보를 자동으로 연결한다. 로컬 개발 DB는 건드리지 않는다.
// docker-compose.yaml과 같은 이미지를 쓴다.
@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

    @Bean
    @ServiceConnection
    MySQLContainer mysqlContainer() {
        return new MySQLContainer(DockerImageName.parse("mysql:8.4"));
    }
}
