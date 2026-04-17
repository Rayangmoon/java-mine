# Stock Watchlist 实施计划

> **For agentic workers:** REQUIRED: Use superpowers:subagent-driven-development (if subagents available) or superpowers:executing-plans to implement this plan. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 构建一个自选股管理 REST API，支持增删改查 + 新浪行情接入，通过 Docker Compose 部署到阿里云服务器。

**Architecture:** 单模块 Spring Boot 应用，Controller → Service → Mapper 三层架构。MySQL 持久化，RestTemplate 调用新浪行情/搜索 API。Docker Compose 编排 app + mysql 两个容器。

**Tech Stack:** JDK 8, Spring Boot 2.7.18, MyBatis Plus 3.5.5, MySQL 8.0, Maven, Docker

**Spec:** `docs/superpowers/specs/2026-04-17-stock-watchlist-design.md`

---

## 文件结构

| 文件路径 | 职责 |
|---------|------|
| `pom.xml` | Maven 依赖管理（类似 package.json） |
| `sql/init.sql` | MySQL 建表脚本 |
| `src/main/java/com/watchlist/StockWatchlistApplication.java` | 启动入口（类似 main.js） |
| `src/main/java/com/watchlist/config/RestTemplateConfig.java` | HTTP 客户端配置 |
| `src/main/java/com/watchlist/common/Result.java` | 统一响应包装 |
| `src/main/java/com/watchlist/common/BusinessException.java` | 业务异常 |
| `src/main/java/com/watchlist/common/GlobalExceptionHandler.java` | 全局异常处理 |
| `src/main/java/com/watchlist/entity/Stock.java` | 数据库表映射实体 |
| `src/main/java/com/watchlist/dto/StockAddRequest.java` | 添加自选股入参 |
| `src/main/java/com/watchlist/dto/StockUpdateRequest.java` | 修改备注入参 |
| `src/main/java/com/watchlist/dto/StockSearchVO.java` | 搜索结果出参 |
| `src/main/java/com/watchlist/dto/StockQuoteVO.java` | 行情数据出参 |
| `src/main/java/com/watchlist/mapper/StockMapper.java` | 数据库操作接口 |
| `src/main/java/com/watchlist/service/StockService.java` | 业务逻辑接口 |
| `src/main/java/com/watchlist/service/SinaStockApi.java` | 新浪 API 客户端 |
| `src/main/java/com/watchlist/service/impl/StockServiceImpl.java` | 业务逻辑实现 |
| `src/main/java/com/watchlist/controller/StockController.java` | REST API 控制器 |
| `src/main/resources/application.yml` | 本地开发配置 |
| `src/main/resources/application-docker.yml` | Docker 部署配置 |
| `src/test/resources/application-test.yml` | 测试配置 |
| `src/test/resources/schema-h2.sql` | H2 测试数据库建表脚本 |
| `src/test/java/com/watchlist/mapper/StockMapperTest.java` | Mapper 层测试 |
| `src/test/java/com/watchlist/service/SinaStockApiTest.java` | 新浪 API 解析逻辑测试 |
| `src/test/java/com/watchlist/service/StockServiceTest.java` | Service 层测试 |
| `src/test/java/com/watchlist/controller/StockControllerTest.java` | Controller 层测试 |
| `Dockerfile` | 应用容器镜像定义 |
| `docker-compose.yml` | 生产部署编排 |

---

## Chunk 1: 项目基础搭建

### Task 1: 开发环境检查与安装

**说明：** 确保 macOS 上 JDK 8、Maven、Docker 就绪。

- [ ] **Step 1: 检查并安装 JDK 8**

```bash
# 检查是否已安装
java -version
# 如果未安装或版本不对，用 Homebrew 安装 Temurin JDK 8
brew install --cask temurin@8
# 验证
java -version
# 期望输出含 "1.8.0"
```

- [ ] **Step 2: 检查并安装 Maven**

```bash
mvn -version
# 如果未安装
brew install maven
# 验证
mvn -version
# 期望输出含 "Apache Maven 3.8" 或更高
```

- [ ] **Step 3: 检查 Docker Desktop**

```bash
docker --version
docker compose version
# 如果未安装，从 https://www.docker.com/products/docker-desktop/ 下载安装
# 启动 Docker Desktop 应用
```

- [ ] **Step 4: 验证环境完整性**

```bash
java -version && mvn -version && docker --version && echo "环境就绪"
```

---

### Task 2: 初始化 Maven 项目

**Files:**
- Create: `pom.xml`
- Create: `src/main/java/com/watchlist/StockWatchlistApplication.java`

- [ ] **Step 1: 创建项目目录结构**

```bash
cd /Users/lei_yang/Desktop/project/java-mine
mkdir -p src/main/java/com/watchlist/{config,common,entity,dto,mapper,service/impl,controller}
mkdir -p src/main/resources
mkdir -p src/test/java/com/watchlist/{mapper,service,controller}
mkdir -p src/test/resources
mkdir -p sql
```

- [ ] **Step 2: 创建 pom.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <!-- Spring Boot 父 POM，类似前端的 create-vue 脚手架，预设了大量默认配置 -->
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.7.18</version>
    </parent>

    <groupId>com.watchlist</groupId>
    <artifactId>stock-watchlist</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>
    <name>stock-watchlist</name>

    <properties>
        <java.version>1.8</java.version>
        <mybatis-plus.version>3.5.5</mybatis-plus.version>
    </properties>

    <dependencies>
        <!-- Web 框架，类似前端的 express/koa -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>

        <!-- 参数校验 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>

        <!-- MyBatis Plus：ORM 框架，操作数据库的工具 -->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>${mybatis-plus.version}</version>
        </dependency>

        <!-- MySQL 驱动 -->
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <scope>runtime</scope>
        </dependency>

        <!-- Lombok：自动生成 getter/setter，减少样板代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 测试框架 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>

        <!-- H2 内存数据库，测试用 -->
        <dependency>
            <groupId>com.h2database</groupId>
            <artifactId>h2</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 3: 创建启动入口**

```java
// src/main/java/com/watchlist/StockWatchlistApplication.java
package com.watchlist;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// @SpringBootApplication = 启动入口，类似前端的 main.js 里 createApp().mount()
// @MapperScan = 告诉框架去哪里扫描数据库操作接口，类似前端自动导入路由
@SpringBootApplication
@MapperScan("com.watchlist.mapper")
public class StockWatchlistApplication {
    public static void main(String[] args) {
        SpringApplication.run(StockWatchlistApplication.class, args);
    }
}
```

- [ ] **Step 4: 验证项目编译**

```bash
cd /Users/lei_yang/Desktop/project/java-mine
mvn compile
# 期望：BUILD SUCCESS（此时还没配数据源，compile 不会报错）
```

- [ ] **Step 5: 提交**

```bash
git add pom.xml src/main/java/com/watchlist/StockWatchlistApplication.java
git commit -m "feat: 初始化 Spring Boot Maven 项目"
```

---

### Task 3: 数据库配置

**Files:**
- Create: `sql/init.sql`
- Create: `src/main/resources/application.yml`
- Create: `src/main/resources/application-docker.yml`
- Create: `src/test/resources/application-test.yml`
- Create: `src/test/resources/schema-h2.sql`

- [ ] **Step 1: 创建 MySQL 建表脚本**

```sql
-- sql/init.sql
CREATE DATABASE IF NOT EXISTS stock_watchlist
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE stock_watchlist;

CREATE TABLE IF NOT EXISTS stock_watchlist (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_code  VARCHAR(6)   NOT NULL COMMENT '股票代码，如 600519',
    stock_name  VARCHAR(64)  NOT NULL COMMENT '股票名称',
    notes       VARCHAR(256) DEFAULT '' COMMENT '备注',
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_stock_code (stock_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 2: 创建本地开发配置**

```yaml
# src/main/resources/application.yml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/stock_watchlist?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4&allowPublicKeyRetrieval=true
    username: root
    password: root123
    driver-class-name: com.mysql.cj.jdbc.Driver

# MyBatis Plus 配置
mybatis-plus:
  configuration:
    # 下划线转驼峰：数据库 stock_code → Java stockCode（自动映射）
    map-underscore-to-camel-case: true
    # 开发时打印 SQL，方便调试
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

- [ ] **Step 3: 创建 Docker 部署配置**

```yaml
# src/main/resources/application-docker.yml
# 只覆盖与本地不同的配置，其余继承 application.yml
spring:
  datasource:
    # mysql 是 docker-compose 中的服务名，Docker 内部 DNS 自动解析
    url: jdbc:mysql://mysql:3306/stock_watchlist?useSSL=false&serverTimezone=Asia/Shanghai&characterEncoding=utf8mb4&allowPublicKeyRetrieval=true
    username: root
    password: root123

mybatis-plus:
  configuration:
    # 生产环境关闭 SQL 日志
    log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl
```

- [ ] **Step 4: 创建测试配置和 H2 建表脚本**

```yaml
# src/test/resources/application-test.yml
spring:
  datasource:
    url: jdbc:h2:mem:testdb;MODE=MySQL;DB_CLOSE_DELAY=-1
    driver-class-name: org.h2.Driver
    username: sa
    password:
  sql:
    init:
      mode: always
      schema-locations: classpath:schema-h2.sql

mybatis-plus:
  configuration:
    map-underscore-to-camel-case: true
```

```sql
-- src/test/resources/schema-h2.sql
-- H2 兼容版建表脚本（去掉 MySQL 特有的 COMMENT、ENGINE 等语法）
DROP TABLE IF EXISTS stock_watchlist;

CREATE TABLE stock_watchlist (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    stock_code  VARCHAR(6)   NOT NULL,
    stock_name  VARCHAR(64)  NOT NULL,
    notes       VARCHAR(256) DEFAULT '',
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_stock_code ON stock_watchlist(stock_code);
```

- [ ] **Step 5: 用 Docker 启动本地开发 MySQL**

```bash
docker run -d \
  --name watchlist-mysql \
  -e MYSQL_ROOT_PASSWORD=root123 \
  -p 3306:3306 \
  -v $(pwd)/sql/init.sql:/docker-entrypoint-initdb.d/init.sql \
  mysql:8.0
# 等待 MySQL 就绪（约 30 秒）
docker logs watchlist-mysql 2>&1 | tail -5
# 期望看到 "ready for connections"
```

- [ ] **Step 6: 提交**

```bash
git add sql/ src/main/resources/ src/test/resources/
git commit -m "feat: 添加数据库建表脚本和多环境配置"
```

---

### Task 4: Entity + Mapper 层

**Files:**
- Create: `src/main/java/com/watchlist/entity/Stock.java`
- Create: `src/main/java/com/watchlist/mapper/StockMapper.java`
- Create: `src/test/java/com/watchlist/mapper/StockMapperTest.java`

- [ ] **Step 1: 编写 Mapper 测试（先写测试）**

```java
// src/test/java/com/watchlist/mapper/StockMapperTest.java
package com.watchlist.mapper;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.watchlist.entity.Stock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test") // 使用 application-test.yml，连接 H2 而非 MySQL
@Transactional // 每个测试结束后自动回滚，保证测试之间数据隔离
class StockMapperTest {

    @Autowired
    private StockMapper stockMapper;

    @Test
    void testInsertAndSelectById() {
        Stock stock = new Stock();
        stock.setStockCode("600519");
        stock.setStockName("贵州茅台");
        stock.setNotes("长期持有");

        int rows = stockMapper.insert(stock);
        assertEquals(1, rows);
        assertNotNull(stock.getId()); // MyBatis Plus 自动回填主键

        Stock found = stockMapper.selectById(stock.getId());
        assertEquals("600519", found.getStockCode());
        assertEquals("贵州茅台", found.getStockName());
    }

    @Test
    void testSelectByStockCode() {
        Stock stock = new Stock();
        stock.setStockCode("000001");
        stock.setStockName("平安银行");
        stock.setNotes("");
        stockMapper.insert(stock);

        // QueryWrapper 类似前端的 WHERE 条件构建器
        QueryWrapper<Stock> wrapper = new QueryWrapper<>();
        wrapper.eq("stock_code", "000001");
        Stock found = stockMapper.selectOne(wrapper);

        assertNotNull(found);
        assertEquals("平安银行", found.getStockName());
    }

    @Test
    void testDeleteById() {
        Stock stock = new Stock();
        stock.setStockCode("300750");
        stock.setStockName("宁德时代");
        stockMapper.insert(stock);

        int rows = stockMapper.deleteById(stock.getId());
        assertEquals(1, rows);
        assertNull(stockMapper.selectById(stock.getId()));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
mvn test -Dtest=StockMapperTest -pl .
# 期望：编译失败，Stock 和 StockMapper 不存在
```

- [ ] **Step 3: 创建 Entity 实体类**

```java
// src/main/java/com/watchlist/entity/Stock.java
package com.watchlist.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

// @Data = Lombok 注解，自动生成 getter/setter/toString，类似 TypeScript 的 class 属性
// @TableName = 告诉 MyBatis Plus 这个类对应哪张数据库表
@Data
@TableName("stock_watchlist")
public class Stock {

    // @TableId = 主键标记，AUTO = 数据库自增
    @TableId(type = IdType.AUTO)
    private Long id;

    private String stockCode;   // MyBatis Plus 自动映射 stock_code → stockCode
    private String stockName;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
```

- [ ] **Step 4: 创建 Mapper 接口**

```java
// src/main/java/com/watchlist/mapper/StockMapper.java
package com.watchlist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.watchlist.entity.Stock;

// 继承 BaseMapper<Stock> 就自动拥有了 insert/selectById/updateById/deleteById 等方法
// 类似前端用了一个 ORM 库后，不需要手写 SQL 就能做基本 CRUD
// 如果需要自定义 SQL（如 JOIN 查询），可以在这个接口里加方法
public interface StockMapper extends BaseMapper<Stock> {
}
```

- [ ] **Step 5: 运行测试确认通过**

```bash
mvn test -Dtest=StockMapperTest -pl .
# 期望：3 个测试全部 PASS
```

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/watchlist/entity/ src/main/java/com/watchlist/mapper/ src/test/java/com/watchlist/mapper/
git commit -m "feat: 添加 Stock 实体和 StockMapper，通过 CRUD 测试"
```

---

## Chunk 2: 通用层与外部 API

### Task 5: 通用响应与异常处理

**Files:**
- Create: `src/main/java/com/watchlist/common/Result.java`
- Create: `src/main/java/com/watchlist/common/BusinessException.java`
- Create: `src/main/java/com/watchlist/common/GlobalExceptionHandler.java`

- [ ] **Step 1: 创建统一响应包装类**

```java
// src/main/java/com/watchlist/common/Result.java
package com.watchlist.common;

import lombok.Data;

// 统一响应格式，类似前端 axios 拦截器里包装的 { code, message, data }
@Data
public class Result<T> {
    private int code;
    private String message;
    private T data;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>(200, "success", data);
    }

    public static <T> Result<T> error(int code, String message) {
        return new Result<>(code, message, null);
    }
}
```

- [ ] **Step 2: 创建业务异常类**

```java
// src/main/java/com/watchlist/common/BusinessException.java
package com.watchlist.common;

import lombok.Getter;

// 业务异常，用于在 Service 层抛出带有错误码的异常
// 类似前端 throw new Error("xxx")，但多了一个 code 字段
@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }
}
```

- [ ] **Step 3: 创建全局异常处理器**

```java
// src/main/java/com/watchlist/common/GlobalExceptionHandler.java
package com.watchlist.common;

import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

// @RestControllerAdvice = 全局异常拦截器
// 类似前端的 app.config.errorHandler 或 axios 的响应拦截器
// 任何 Controller 抛出的异常都会被这里捕获，统一返回 Result 格式
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 业务异常（如重复添加、资源不存在）
    @ExceptionHandler(BusinessException.class)
    public Result<?> handleBusinessException(BusinessException e) {
        return Result.error(e.getCode(), e.getMessage());
    }

    // 参数校验失败（@Valid 注解触发）
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        return Result.error(400, message);
    }

    // 缺少必填的 query 参数
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<?> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.error(400, "缺少参数: " + e.getParameterName());
    }

    // 兜底：所有未处理的异常
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        return Result.error(500, "服务器内部错误");
    }
}
```

- [ ] **Step 4: 提交**

```bash
git add src/main/java/com/watchlist/common/
git commit -m "feat: 添加统一响应 Result、业务异常和全局异常处理"
```

---

### Task 6: DTO 类

**Files:**
- Create: `src/main/java/com/watchlist/dto/StockAddRequest.java`
- Create: `src/main/java/com/watchlist/dto/StockUpdateRequest.java`
- Create: `src/main/java/com/watchlist/dto/StockSearchVO.java`
- Create: `src/main/java/com/watchlist/dto/StockQuoteVO.java`

- [ ] **Step 1: 创建添加自选股入参 DTO**

```java
// src/main/java/com/watchlist/dto/StockAddRequest.java
package com.watchlist.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

// DTO = Data Transfer Object，定义接口入参的形状
// 类似前端组件的 Props 类型定义
@Data
public class StockAddRequest {

    @NotBlank(message = "股票代码不能为空")
    @Pattern(regexp = "^[03468]\\d{5}$", message = "股票代码格式不正确，需为6位数字且首位为0/3/4/6/8")
    private String stockCode;

    private String notes;
}
```

- [ ] **Step 2: 创建修改备注入参 DTO**

```java
// src/main/java/com/watchlist/dto/StockUpdateRequest.java
package com.watchlist.dto;

import lombok.Data;

import javax.validation.constraints.Size;

@Data
public class StockUpdateRequest {

    @Size(max = 256, message = "备注长度不能超过256个字符")
    private String notes;
}
```

- [ ] **Step 3: 创建搜索结果 VO**

```java
// src/main/java/com/watchlist/dto/StockSearchVO.java
package com.watchlist.dto;

import lombok.Data;

// VO = View Object，定义返回给前端的数据形状
@Data
public class StockSearchVO {
    private String stockCode;
    private String stockName;
}
```

- [ ] **Step 4: 创建行情数据 VO**

```java
// src/main/java/com/watchlist/dto/StockQuoteVO.java
package com.watchlist.dto;

import lombok.Data;

@Data
public class StockQuoteVO {
    private Long id;            // 来自数据库，纯行情查询时为 null
    private String stockCode;
    private String stockName;
    private Double currentPrice;
    private Double todayOpen;
    private Double yesterdayClose;
    private Double high;
    private Double low;
    private Long volume;        // Long 类型，成交量可能很大
    private String notes;       // 来自数据库，纯行情查询时为 null
}
```

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/watchlist/dto/
git commit -m "feat: 添加 DTO 类（请求入参和响应出参定义）"
```

---

### Task 7: 新浪行情 API 客户端

**Files:**
- Create: `src/main/java/com/watchlist/config/RestTemplateConfig.java`
- Create: `src/main/java/com/watchlist/service/SinaStockApi.java`

- [ ] **Step 1: 创建 RestTemplate 配置**

```java
// src/main/java/com/watchlist/config/RestTemplateConfig.java
package com.watchlist.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

// @Configuration = 配置类，类似前端的 vite.config.js
// @Bean = 注册一个可被注入的实例，类似前端的 app.provide()
@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 连接超时 5 秒
        factory.setReadTimeout(5000);     // 读取超时 5 秒
        return new RestTemplate(factory);
    }
}
```

- [ ] **Step 2: 创建新浪 API 客户端**

```java
// src/main/java/com/watchlist/service/SinaStockApi.java
package com.watchlist.service;

import com.watchlist.common.BusinessException;
import com.watchlist.dto.StockQuoteVO;
import com.watchlist.dto.StockSearchVO;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class SinaStockApi {

    private final RestTemplate restTemplate;

    public SinaStockApi(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 根据股票代码推导交易所前缀
     * 6 开头 → sh（上海），0/3 开头 → sz（深圳），4/8 开头 → bj（北交所）
     */
    public static String getExchangePrefix(String stockCode) {
        char first = stockCode.charAt(0);
        switch (first) {
            case '6': return "sh";
            case '0': case '3': return "sz";
            case '4': case '8': return "bj";
            default:
                throw new BusinessException(400, "无法识别的股票代码前缀: " + stockCode);
        }
    }

    /**
     * 批量获取实时行情
     * 调用 https://hq.sinajs.cn/list=sh600519,sz000001
     */
    public List<StockQuoteVO> getQuotes(List<String> stockCodes) {
        if (stockCodes == null || stockCodes.isEmpty()) {
            return Collections.emptyList();
        }

        String codeList = stockCodes.stream()
                .map(code -> getExchangePrefix(code) + code)
                .collect(Collectors.joining(","));

        String url = "https://hq.sinajs.cn/list=" + codeList;

        // 新浪 API 需要 Referer 头
        HttpHeaders headers = new HttpHeaders();
        headers.set("Referer", "https://finance.sina.com.cn");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, byte[].class);

        byte[] bytes = response.getBody();
        if (bytes == null) return Collections.emptyList();

        // 新浪返回 GBK 编码，手动转换
        String body = new String(bytes, Charset.forName("GBK"));
        return parseQuoteResponse(body);
    }

    /**
     * 搜索股票（模糊匹配）
     * 调用 https://suggest3.sinajs.cn/suggest/type=11&key=茅台
     */
    public List<StockSearchVO> search(String keyword) {
        String encodedKeyword;
        try {
            encodedKeyword = URLEncoder.encode(keyword, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return Collections.emptyList();
        }

        String url = "https://suggest3.sinajs.cn/suggest/type=11&key=" + encodedKeyword;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Referer", "https://finance.sina.com.cn");
        HttpEntity<String> entity = new HttpEntity<>(headers);

        ResponseEntity<byte[]> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, byte[].class);

        byte[] bytes = response.getBody();
        if (bytes == null) return Collections.emptyList();

        String body = new String(bytes, Charset.forName("GBK"));
        return parseSearchResponse(body);
    }

    /**
     * 解析行情响应
     * 格式: var hq_str_sh600519="贵州茅台,1688.00,1682.00,1690.00,1695.00,1680.00,...,12345678,...";
     */
    private List<StockQuoteVO> parseQuoteResponse(String body) {
        List<StockQuoteVO> result = new ArrayList<>();
        String[] lines = body.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty() || !line.contains("\"")) continue;

            // 提取代码: var hq_str_sh600519="..." → "sh600519"
            String fullCode = line.substring(line.indexOf("str_") + 4, line.indexOf("="));
            // 去掉前缀: "sh600519" → "600519"
            String stockCode = fullCode.substring(2);

            // 提取引号中的数据
            String data = line.substring(line.indexOf("\"") + 1, line.lastIndexOf("\""));
            if (data.isEmpty()) continue; // 无效代码返回空字符串

            String[] fields = data.split(",");
            if (fields.length < 9) continue;

            StockQuoteVO vo = new StockQuoteVO();
            vo.setStockCode(stockCode);
            vo.setStockName(fields[0]);
            vo.setTodayOpen(parseDouble(fields[1]));
            vo.setYesterdayClose(parseDouble(fields[2]));
            vo.setCurrentPrice(parseDouble(fields[3]));
            vo.setHigh(parseDouble(fields[4]));
            vo.setLow(parseDouble(fields[5]));
            vo.setVolume(parseLong(fields[8]));
            result.add(vo);
        }
        return result;
    }

    /**
     * 解析搜索响应
     * 格式: var suggestvalue="11,sh600519,贵州茅台,贵州茅台,gzmt;...";
     */
    private List<StockSearchVO> parseSearchResponse(String body) {
        List<StockSearchVO> result = new ArrayList<>();

        int start = body.indexOf("\"");
        int end = body.lastIndexOf("\"");
        if (start < 0 || end <= start) return result;

        String data = body.substring(start + 1, end);
        if (data.isEmpty()) return result;

        String[] entries = data.split(";");
        for (String entry : entries) {
            String[] fields = entry.split(",");
            if (fields.length < 4) continue;

            StockSearchVO vo = new StockSearchVO();
            // fields[1] = "sh600519" → 去掉 sh/sz/bj 前缀
            String fullCode = fields[1];
            vo.setStockCode(fullCode.replaceAll("^(sh|sz|bj)", ""));
            vo.setStockName(fields[2]);
            result.add(vo);
        }
        return result;
    }

    private Double parseDouble(String s) {
        try { return Double.parseDouble(s); }
        catch (NumberFormatException e) { return null; }
    }

    private Long parseLong(String s) {
        try { return Long.parseLong(s); }
        catch (NumberFormatException e) { return null; }
    }
}
```

- [ ] **Step 3: 编写 SinaStockApi 解析逻辑单元测试**

```java
// src/test/java/com/watchlist/service/SinaStockApiTest.java
package com.watchlist.service;

import com.watchlist.dto.StockQuoteVO;
import com.watchlist.dto.StockSearchVO;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SinaStockApiTest {

    /**
     * 通过反射调用 private 解析方法进行测试
     */
    private List<StockQuoteVO> invokeParseQuote(SinaStockApi api, String body) throws Exception {
        Method method = SinaStockApi.class.getDeclaredMethod("parseQuoteResponse", String.class);
        method.setAccessible(true);
        return (List<StockQuoteVO>) method.invoke(api, body);
    }

    private List<StockSearchVO> invokeParseSearch(SinaStockApi api, String body) throws Exception {
        Method method = SinaStockApi.class.getDeclaredMethod("parseSearchResponse", String.class);
        method.setAccessible(true);
        return (List<StockSearchVO>) method.invoke(api, body);
    }

    @Test
    void parseQuoteResponse_normal() throws Exception {
        SinaStockApi api = new SinaStockApi(null);
        String body = "var hq_str_sh600519=\"贵州茅台,1688.00,1682.00,1690.00,1695.00,1680.00,1689.00,1690.00,12345678,20000000000.00,\";\n";

        List<StockQuoteVO> result = invokeParseQuote(api, body);

        assertEquals(1, result.size());
        assertEquals("600519", result.get(0).getStockCode());
        assertEquals("贵州茅台", result.get(0).getStockName());
        assertEquals(1690.00, result.get(0).getCurrentPrice());
        assertEquals(12345678L, result.get(0).getVolume());
    }

    @Test
    void parseQuoteResponse_emptyData() throws Exception {
        SinaStockApi api = new SinaStockApi(null);
        String body = "var hq_str_sh999999=\"\";\n";

        List<StockQuoteVO> result = invokeParseQuote(api, body);
        assertTrue(result.isEmpty());
    }

    @Test
    void parseSearchResponse_normal() throws Exception {
        SinaStockApi api = new SinaStockApi(null);
        String body = "var suggestvalue=\"11,sh600519,贵州茅台,贵州茅台,gzmt;11,sz000858,五粮液,五粮液,wly\";";

        List<StockSearchVO> result = invokeParseSearch(api, body);

        assertEquals(2, result.size());
        assertEquals("600519", result.get(0).getStockCode());
        assertEquals("贵州茅台", result.get(0).getStockName());
        assertEquals("000858", result.get(1).getStockCode());
    }

    @Test
    void parseSearchResponse_empty() throws Exception {
        SinaStockApi api = new SinaStockApi(null);
        String body = "var suggestvalue=\"\";";

        List<StockSearchVO> result = invokeParseSearch(api, body);
        assertTrue(result.isEmpty());
    }

    @Test
    void getExchangePrefix_allCases() {
        assertEquals("sh", SinaStockApi.getExchangePrefix("600519"));
        assertEquals("sz", SinaStockApi.getExchangePrefix("000001"));
        assertEquals("sz", SinaStockApi.getExchangePrefix("300750"));
        assertEquals("bj", SinaStockApi.getExchangePrefix("430047"));
        assertEquals("bj", SinaStockApi.getExchangePrefix("830799"));
    }
}
```

- [ ] **Step 4: 运行 SinaStockApi 测试**

```bash
mvn test -Dtest=SinaStockApiTest -pl .
# 期望：5 个测试全部 PASS
```

- [ ] **Step 5: 手动验证新浪 API 可用性**

```bash
# 测试行情接口
curl -H "Referer: https://finance.sina.com.cn" "https://hq.sinajs.cn/list=sh600519"
# 期望返回含 "贵州茅台" 的数据

# 测试搜索接口
curl -H "Referer: https://finance.sina.com.cn" "https://suggest3.sinajs.cn/suggest/type=11&key=茅台"
# 期望返回含 "600519" 和 "贵州茅台" 的数据
```

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/watchlist/config/ src/main/java/com/watchlist/service/SinaStockApi.java src/test/java/com/watchlist/service/SinaStockApiTest.java
git commit -m "feat: 添加 RestTemplate 配置和新浪行情 API 客户端（含解析测试）"
```

---

## Chunk 3: 业务逻辑与 API 接口

### Task 8: Service 层

**Files:**
- Create: `src/main/java/com/watchlist/service/StockService.java`
- Create: `src/main/java/com/watchlist/service/impl/StockServiceImpl.java`
- Create: `src/test/java/com/watchlist/service/StockServiceTest.java`

- [ ] **Step 1: 编写 Service 测试（先写测试）**

```java
// src/test/java/com/watchlist/service/StockServiceTest.java
package com.watchlist.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.watchlist.common.BusinessException;
import com.watchlist.dto.StockAddRequest;
import com.watchlist.dto.StockQuoteVO;
import com.watchlist.dto.StockUpdateRequest;
import com.watchlist.entity.Stock;
import com.watchlist.mapper.StockMapper;
import com.watchlist.service.impl.StockServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

// @Mock + @InjectMocks = 模拟依赖注入
// 类似前端测试中的 jest.mock() 和 vi.mock()
@ExtendWith(MockitoExtension.class)
class StockServiceTest {

    @Mock
    private StockMapper stockMapper;

    @Mock
    private SinaStockApi sinaStockApi;

    @InjectMocks
    private StockServiceImpl stockService;

    @Test
    void addStock_success() {
        StockAddRequest request = new StockAddRequest();
        request.setStockCode("600519");

        // 模拟：数据库中不存在该股票
        when(stockMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);

        // 模拟：新浪 API 返回行情数据
        StockQuoteVO quoteVO = new StockQuoteVO();
        quoteVO.setStockCode("600519");
        quoteVO.setStockName("贵州茅台");
        when(sinaStockApi.getQuotes(anyList()))
                .thenReturn(Collections.singletonList(quoteVO));

        // 模拟：插入成功
        when(stockMapper.insert(any(Stock.class))).thenReturn(1);

        Stock result = stockService.addStock(request);

        assertEquals("600519", result.getStockCode());
        assertEquals("贵州茅台", result.getStockName());
        verify(stockMapper).insert(any(Stock.class));
    }

    @Test
    void addStock_duplicate_throwsException() {
        StockAddRequest request = new StockAddRequest();
        request.setStockCode("600519");

        // 模拟：数据库中已存在该股票
        Stock existing = new Stock();
        existing.setStockCode("600519");
        when(stockMapper.selectOne(any(QueryWrapper.class))).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> stockService.addStock(request));
        assertEquals(409, ex.getCode());
    }

    @Test
    void addStock_invalidCode_throwsException() {
        StockAddRequest request = new StockAddRequest();
        request.setStockCode("600519");

        when(stockMapper.selectOne(any(QueryWrapper.class))).thenReturn(null);
        // 模拟：新浪 API 返回空（无效代码）
        when(sinaStockApi.getQuotes(anyList())).thenReturn(Collections.emptyList());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> stockService.addStock(request));
        assertEquals(400, ex.getCode());
    }

    @Test
    void updateStock_notFound_throwsException() {
        when(stockMapper.selectById(999L)).thenReturn(null);

        StockUpdateRequest request = new StockUpdateRequest();
        request.setNotes("新备注");

        BusinessException ex = assertThrows(BusinessException.class,
                () -> stockService.updateStock(999L, request));
        assertEquals(404, ex.getCode());
    }

    @Test
    void deleteStock_notFound_throwsException() {
        when(stockMapper.selectById(999L)).thenReturn(null);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> stockService.deleteStock(999L));
        assertEquals(404, ex.getCode());
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
mvn test -Dtest=StockServiceTest -pl .
# 期望：编译失败，StockService 和 StockServiceImpl 不存在
```

- [ ] **Step 3: 创建 Service 接口**

```java
// src/main/java/com/watchlist/service/StockService.java
package com.watchlist.service;

import com.watchlist.dto.*;
import com.watchlist.entity.Stock;

import java.util.List;

// Service 接口定义业务操作，类似前端的 store 接口
// 为什么要接口 + 实现分离？方便测试时 mock，也支持未来切换实现
public interface StockService {

    List<StockQuoteVO> listStocksWithQuotes();

    List<StockSearchVO> searchStocks(String keyword);

    Stock addStock(StockAddRequest request);

    Stock updateStock(Long id, StockUpdateRequest request);

    void deleteStock(Long id);

    List<StockQuoteVO> getQuotes(List<String> codes);
}
```

- [ ] **Step 4: 创建 Service 实现**

```java
// src/main/java/com/watchlist/service/impl/StockServiceImpl.java
package com.watchlist.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.watchlist.common.BusinessException;
import com.watchlist.dto.*;
import com.watchlist.entity.Stock;
import com.watchlist.mapper.StockMapper;
import com.watchlist.service.SinaStockApi;
import com.watchlist.service.StockService;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

// @Service = 标记为业务逻辑组件，Spring 会自动创建实例并管理
// 类似前端的 Pinia defineStore()
@Service
public class StockServiceImpl implements StockService {

    private final StockMapper stockMapper;
    private final SinaStockApi sinaStockApi;

    // 构造器注入依赖，类似前端组件通过 props 接收依赖
    public StockServiceImpl(StockMapper stockMapper, SinaStockApi sinaStockApi) {
        this.stockMapper = stockMapper;
        this.sinaStockApi = sinaStockApi;
    }

    @Override
    public List<StockQuoteVO> listStocksWithQuotes() {
        List<Stock> stocks = stockMapper.selectList(null); // null = 无条件，查全部
        if (stocks.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> codes = stocks.stream()
                .map(Stock::getStockCode)
                .collect(Collectors.toList());

        // 尝试获取行情，失败则降级（返回列表但行情字段为 null）
        List<StockQuoteVO> quotes;
        try {
            quotes = sinaStockApi.getQuotes(codes);
        } catch (Exception e) {
            quotes = Collections.emptyList();
        }

        // 将行情数据按代码索引，方便合并
        Map<String, StockQuoteVO> quoteMap = quotes.stream()
                .collect(Collectors.toMap(StockQuoteVO::getStockCode, v -> v, (a, b) -> a));

        // 合并数据库数据 + 行情数据
        return stocks.stream().map(stock -> {
            StockQuoteVO vo = quoteMap.getOrDefault(
                    stock.getStockCode(), new StockQuoteVO());
            vo.setId(stock.getId());
            vo.setStockCode(stock.getStockCode());
            vo.setStockName(stock.getStockName());
            vo.setNotes(stock.getNotes());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<StockSearchVO> searchStocks(String keyword) {
        if (keyword.matches("^[03468]\\d{5}$")) {
            // 合法的 6 位代码：直接查行情验证是否存在
            try {
                List<StockQuoteVO> quotes = sinaStockApi.getQuotes(
                        Collections.singletonList(keyword));
                return quotes.stream().map(q -> {
                    StockSearchVO vo = new StockSearchVO();
                    vo.setStockCode(q.getStockCode());
                    vo.setStockName(q.getStockName());
                    return vo;
                }).collect(Collectors.toList());
            } catch (Exception e) {
                return Collections.emptyList();
            }
        }
        // 其他情况：走新浪搜索/建议接口（含部分数字、中文、字母）
        return sinaStockApi.search(keyword);
    }

    @Override
    public Stock addStock(StockAddRequest request) {
        String stockCode = request.getStockCode();

        // 检查重复（友好提示，数据库 UNIQUE 兜底）
        QueryWrapper<Stock> wrapper = new QueryWrapper<>();
        wrapper.eq("stock_code", stockCode);
        if (stockMapper.selectOne(wrapper) != null) {
            throw new BusinessException(409, "该股票已在自选列表中");
        }

        // 调新浪 API 获取名称，同时验证代码有效性
        List<StockQuoteVO> quotes = sinaStockApi.getQuotes(
                Collections.singletonList(stockCode));
        if (quotes.isEmpty()) {
            throw new BusinessException(400, "无效的股票代码");
        }

        Stock stock = new Stock();
        stock.setStockCode(stockCode);
        stock.setStockName(quotes.get(0).getStockName());
        stock.setNotes(request.getNotes() != null ? request.getNotes() : "");
        stockMapper.insert(stock);

        return stock;
    }

    @Override
    public Stock updateStock(Long id, StockUpdateRequest request) {
        Stock stock = stockMapper.selectById(id);
        if (stock == null) {
            throw new BusinessException(404, "自选股不存在");
        }
        stock.setNotes(request.getNotes());
        stockMapper.updateById(stock);
        return stock;
    }

    @Override
    public void deleteStock(Long id) {
        if (stockMapper.selectById(id) == null) {
            throw new BusinessException(404, "自选股不存在");
        }
        stockMapper.deleteById(id);
    }

    @Override
    public List<StockQuoteVO> getQuotes(List<String> codes) {
        return sinaStockApi.getQuotes(codes);
    }
}
```

- [ ] **Step 5: 运行测试确认通过**

```bash
mvn test -Dtest=StockServiceTest -pl .
# 期望：5 个测试全部 PASS
```

- [ ] **Step 6: 提交**

```bash
git add src/main/java/com/watchlist/service/ src/test/java/com/watchlist/service/
git commit -m "feat: 添加 StockService 业务逻辑层，通过单元测试"
```

---

### Task 9: Controller 层

**Files:**
- Create: `src/main/java/com/watchlist/controller/StockController.java`
- Create: `src/test/java/com/watchlist/controller/StockControllerTest.java`

- [ ] **Step 1: 编写 Controller 测试（先写测试）**

```java
// src/test/java/com/watchlist/controller/StockControllerTest.java
package com.watchlist.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.watchlist.dto.StockAddRequest;
import com.watchlist.dto.StockQuoteVO;
import com.watchlist.dto.StockSearchVO;
import com.watchlist.entity.Stock;
import com.watchlist.service.StockService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// @WebMvcTest = 只启动 Controller 层，不启动整个应用
// 类似前端的组件单元测试，只测试路由逻辑，mock 掉 service
@WebMvcTest(StockController.class)
class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockService stockService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listStocks_returnsQuotes() throws Exception {
        StockQuoteVO vo = new StockQuoteVO();
        vo.setId(1L);
        vo.setStockCode("600519");
        vo.setStockName("贵州茅台");
        vo.setCurrentPrice(1688.0);

        when(stockService.listStocksWithQuotes())
                .thenReturn(Collections.singletonList(vo));

        mockMvc.perform(get("/api/stocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].stockCode").value("600519"));
    }

    @Test
    void searchStocks_returnsResults() throws Exception {
        StockSearchVO vo = new StockSearchVO();
        vo.setStockCode("600519");
        vo.setStockName("贵州茅台");

        when(stockService.searchStocks("茅台"))
                .thenReturn(Collections.singletonList(vo));

        mockMvc.perform(get("/api/stocks/search").param("keyword", "茅台"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].stockName").value("贵州茅台"));
    }

    @Test
    void addStock_validRequest_returnsStock() throws Exception {
        Stock stock = new Stock();
        stock.setId(1L);
        stock.setStockCode("600519");
        stock.setStockName("贵州茅台");

        when(stockService.addStock(any(StockAddRequest.class))).thenReturn(stock);

        StockAddRequest request = new StockAddRequest();
        request.setStockCode("600519");

        mockMvc.perform(post("/api/stocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.stockCode").value("600519"));
    }

    @Test
    void addStock_invalidCode_returns400() throws Exception {
        StockAddRequest request = new StockAddRequest();
        request.setStockCode("12345"); // 5 位，格式不对

        mockMvc.perform(post("/api/stocks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400))
                .andExpect(jsonPath("$.message").isNotEmpty());
    }

    @Test
    void updateStock_returnsUpdatedStock() throws Exception {
        Stock stock = new Stock();
        stock.setId(1L);
        stock.setStockCode("600519");
        stock.setStockName("贵州茅台");
        stock.setNotes("核心持仓");

        when(stockService.updateStock(any(Long.class), any())).thenReturn(stock);

        mockMvc.perform(put("/api/stocks/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"notes\":\"核心持仓\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.notes").value("核心持仓"));
    }

    @Test
    void deleteStock_returns200() throws Exception {
        mockMvc.perform(delete("/api/stocks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    void getQuotes_returnsQuotes() throws Exception {
        StockQuoteVO vo = new StockQuoteVO();
        vo.setStockCode("600519");
        vo.setStockName("贵州茅台");

        when(stockService.getQuotes(Arrays.asList("600519", "000001")))
                .thenReturn(Collections.singletonList(vo));

        mockMvc.perform(get("/api/quotes").param("codes", "600519,000001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].stockCode").value("600519"));
    }
}
```

- [ ] **Step 2: 运行测试确认失败**

```bash
mvn test -Dtest=StockControllerTest -pl .
# 期望：编译失败，StockController 不存在
```

- [ ] **Step 3: 创建 Controller**

```java
// src/main/java/com/watchlist/controller/StockController.java
package com.watchlist.controller;

import com.watchlist.common.Result;
import com.watchlist.dto.*;
import com.watchlist.entity.Stock;
import com.watchlist.service.StockService;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Arrays;
import java.util.List;

// @RestController = 标记为 REST API 控制器
// 类似前端的路由文件（如 express 的 router 或 Next.js 的 API routes）
// Controller 的职责：接收请求参数 → 调用 Service → 返回响应，不写业务逻辑
@RestController
@RequestMapping("/api")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    // GET /api/stocks — 查询自选股列表（附带实时行情）
    @GetMapping("/stocks")
    public Result<List<StockQuoteVO>> listStocks() {
        return Result.success(stockService.listStocksWithQuotes());
    }

    // GET /api/stocks/search?keyword=xxx — 搜索股票
    @GetMapping("/stocks/search")
    public Result<List<StockSearchVO>> searchStocks(@RequestParam String keyword) {
        return Result.success(stockService.searchStocks(keyword));
    }

    // POST /api/stocks — 添加自选股
    // @Valid = 触发 DTO 上的校验注解（@NotBlank, @Pattern 等）
    // @RequestBody = 从 JSON 请求体解析参数，类似前端 req.body
    @PostMapping("/stocks")
    public Result<Stock> addStock(@Valid @RequestBody StockAddRequest request) {
        return Result.success(stockService.addStock(request));
    }

    // PUT /api/stocks/{id} — 修改备注
    // @PathVariable = 从 URL 路径提取参数，类似前端 /stocks/:id 中的 :id
    @PutMapping("/stocks/{id}")
    public Result<Stock> updateStock(@PathVariable Long id,
                                     @Valid @RequestBody StockUpdateRequest request) {
        return Result.success(stockService.updateStock(id, request));
    }

    // DELETE /api/stocks/{id} — 删除自选股
    @DeleteMapping("/stocks/{id}")
    public Result<Void> deleteStock(@PathVariable Long id) {
        stockService.deleteStock(id);
        return Result.success(null);
    }

    // GET /api/quotes?codes=600519,000001 — 批量获取实时行情
    @GetMapping("/quotes")
    public Result<List<StockQuoteVO>> getQuotes(@RequestParam String codes) {
        List<String> codeList = Arrays.asList(codes.split(","));
        return Result.success(stockService.getQuotes(codeList));
    }
}
```

- [ ] **Step 4: 运行测试确认通过**

```bash
mvn test -Dtest=StockControllerTest -pl .
# 期望：7 个测试全部 PASS
```

- [ ] **Step 5: 提交**

```bash
git add src/main/java/com/watchlist/controller/ src/test/java/com/watchlist/controller/
git commit -m "feat: 添加 StockController，6 个 REST API 端点全部通过测试（7 个测试用例）"
```

---

### Task 10: 端到端验证

**说明：** 启动完整应用，用 curl 验证所有接口。需要本地 MySQL 容器运行。

- [ ] **Step 1: 确认本地 MySQL 容器运行中**

```bash
docker ps | grep watchlist-mysql
# 如果没有运行，执行 Task 3 Step 5 中的 docker run 命令
```

- [ ] **Step 2: 启动应用**

```bash
cd /Users/lei_yang/Desktop/project/java-mine
mvn spring-boot:run
# 期望：控制台输出 "Started StockWatchlistApplication"
# 保持终端运行，另开一个终端执行后续 curl
```

- [ ] **Step 3: 测试搜索接口**

```bash
# 按名称模糊搜索
curl -s "http://localhost:8080/api/stocks/search?keyword=茅台" | python3 -m json.tool
# 期望：返回含 600519 贵州茅台的搜索结果

# 按代码精确搜索
curl -s "http://localhost:8080/api/stocks/search?keyword=600519" | python3 -m json.tool
```

- [ ] **Step 4: 测试添加接口**

```bash
curl -s -X POST "http://localhost:8080/api/stocks" \
  -H "Content-Type: application/json" \
  -d '{"stockCode":"600519","notes":"长期持有"}' | python3 -m json.tool
# 期望：code=200，data 中含 stockName="贵州茅台"

# 测试重复添加
curl -s -X POST "http://localhost:8080/api/stocks" \
  -H "Content-Type: application/json" \
  -d '{"stockCode":"600519"}' | python3 -m json.tool
# 期望：code=409
```

- [ ] **Step 5: 测试列表接口（含行情）**

```bash
curl -s "http://localhost:8080/api/stocks" | python3 -m json.tool
# 期望：返回自选股列表，每项附带实时行情数据
```

- [ ] **Step 6: 测试修改和删除**

```bash
# 修改备注（假设 id=1）
curl -s -X PUT "http://localhost:8080/api/stocks/1" \
  -H "Content-Type: application/json" \
  -d '{"notes":"核心持仓"}' | python3 -m json.tool
# 期望：code=200，notes 已更新

# 删除
curl -s -X DELETE "http://localhost:8080/api/stocks/1" | python3 -m json.tool
# 期望：code=200
```

- [ ] **Step 7: 测试行情接口**

```bash
curl -s "http://localhost:8080/api/quotes?codes=600519,000001" | python3 -m json.tool
# 期望：返回两只股票的行情数据
```

- [ ] **Step 8: 运行全部测试**

```bash
mvn test
# 期望：所有测试 PASS（Mapper 3 + SinaApi 5 + Service 5 + Controller 7 = 20 个测试）
```

- [ ] **Step 9: 提交（如有修复）**

```bash
# 如果端到端验证中发现问题并修复了代码
git add -A
git commit -m "fix: 修复端到端验证中发现的问题"
```

---

## Chunk 4: Docker 部署

### Task 11: Dockerfile + docker-compose.yml

**Files:**
- Create: `Dockerfile`
- Create: `docker-compose.yml`

- [ ] **Step 1: 打包 JAR**

```bash
cd /Users/lei_yang/Desktop/project/java-mine
mvn clean package -DskipTests
# 期望：target/stock-watchlist-1.0.0.jar 生成成功
ls -lh target/stock-watchlist-1.0.0.jar
```

- [ ] **Step 2: 创建 Dockerfile**

```dockerfile
# Dockerfile
FROM openjdk:8-jre-slim

WORKDIR /app

COPY target/stock-watchlist-1.0.0.jar app.jar

EXPOSE 8080

# profile 由 docker-compose 的环境变量控制，不在这里硬编码
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 3: 创建 docker-compose.yml**

```yaml
# docker-compose.yml
version: '3.8'

services:
  app:
    build: .
    ports:
      - "8080:8080"
    depends_on:
      mysql:
        condition: service_healthy
    environment:
      - SPRING_PROFILES_ACTIVE=docker
    restart: unless-stopped

  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: stock_watchlist
    volumes:
      - mysql_data:/var/lib/mysql
      - ./sql/init.sql:/docker-entrypoint-initdb.d/init.sql
    # 不映射端口到宿主机，仅内部网络可访问
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 5s
      timeout: 3s
      retries: 10
    restart: unless-stopped

volumes:
  mysql_data:
```

- [ ] **Step 4: 停止本地开发用的 MySQL 容器（避免端口冲突）**

```bash
docker stop watchlist-mysql && docker rm watchlist-mysql
```

- [ ] **Step 5: 本地验证 Docker Compose 启动**

```bash
cd /Users/lei_yang/Desktop/project/java-mine
docker compose up -d --build
# 等待启动（MySQL 初始化约 30 秒，app 可能会重启 1-2 次等待 MySQL）
sleep 30
docker compose logs app | tail -20
# 期望看到 "Started StockWatchlistApplication"
```

- [ ] **Step 6: 验证 Docker 环境接口可用**

```bash
curl -s "http://localhost:8080/api/stocks" | python3 -m json.tool
# 期望：code=200，data=[]（空列表）

curl -s -X POST "http://localhost:8080/api/stocks" \
  -H "Content-Type: application/json" \
  -d '{"stockCode":"600519"}' | python3 -m json.tool
# 期望：code=200，添加成功
```

- [ ] **Step 7: 清理并提交**

```bash
docker compose down
git add Dockerfile docker-compose.yml
git commit -m "feat: 添加 Dockerfile 和 docker-compose.yml 部署配置"
```

---

### Task 12: 部署到阿里云服务器

**前提：** 需要阿里云服务器的公网 IP、SSH 登录信息。服务器需要已安装 Docker 和 Docker Compose。

- [ ] **Step 1: 确认服务器信息（向用户获取）**

需要以下信息：
- 服务器公网 IP
- SSH 用户名（通常是 root）
- SSH 登录方式（密码或密钥）
- 服务器上是否已安装 Docker

- [ ] **Step 2: 在服务器上创建目录结构**

```bash
ssh root@YOUR_SERVER_IP "mkdir -p /opt/stock-watchlist/{target,sql,src/main/resources}"
```

- [ ] **Step 3: 上传项目到服务器**

```bash
# 先本地打包
cd /Users/lei_yang/Desktop/project/java-mine
mvn clean package -DskipTests

# 上传文件，保持与 Dockerfile 中路径一致（COPY target/xxx.jar）
scp Dockerfile docker-compose.yml root@YOUR_SERVER_IP:/opt/stock-watchlist/
scp sql/init.sql root@YOUR_SERVER_IP:/opt/stock-watchlist/sql/
scp target/stock-watchlist-1.0.0.jar root@YOUR_SERVER_IP:/opt/stock-watchlist/target/
scp src/main/resources/application.yml src/main/resources/application-docker.yml \
  root@YOUR_SERVER_IP:/opt/stock-watchlist/src/main/resources/
```

- [ ] **Step 4: 在服务器上启动**

```bash
ssh root@YOUR_SERVER_IP
cd /opt/stock-watchlist
docker compose up -d --build
docker compose logs -f app
# 期望看到 "Started StockWatchlistApplication"
# Ctrl+C 退出日志查看
```

- [ ] **Step 5: 配置阿里云安全组**

在阿里云控制台：
1. 进入 ECS 实例 → 安全组
2. 添加入方向规则：端口 8080，协议 TCP，授权对象 0.0.0.0/0
3. 保存

- [ ] **Step 6: 验证公网访问**

```bash
# 替换 YOUR_SERVER_IP
curl -s "http://YOUR_SERVER_IP:8080/api/stocks" | python3 -m json.tool
# 期望：code=200

curl -s "http://YOUR_SERVER_IP:8080/api/stocks/search?keyword=茅台" | python3 -m json.tool
# 期望：返回搜索结果
```

- [ ] **Step 7: 提交最终状态**

```bash
git add -A
git commit -m "feat: 完成部署配置，应用已上线"
```
