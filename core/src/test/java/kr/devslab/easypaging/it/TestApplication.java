package kr.devslab.easypaging.it;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal Spring Boot application used by the integration tests in this package.
 * Not part of the published artifact.
 */
@SpringBootApplication
@MapperScan(basePackages = "kr.devslab.easypaging.it")
public class TestApplication {}
