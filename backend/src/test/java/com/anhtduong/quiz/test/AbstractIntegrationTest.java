package com.anhtduong.quiz.test;

import com.anhtduong.quiz.auth.AuthDtos_AuthResponse;
import com.anhtduong.quiz.auth.AuthDtos_LoginRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Test tích hợp dùng PostgreSQL đang chạy ở localhost (khởi động qua Docker Compose).
 * Cổng lấy từ biến môi trường POSTGRES_PORT, mặc định 5432 — phải khớp với
 * application-test.yml (cũng đọc POSTGRES_PORT).
 * Reset schema một lần trước class đầu tiên; Flyway sẽ tự tạo lại dữ liệu.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public abstract class AbstractIntegrationTest {

    private static final AtomicBoolean RESET_DONE = new AtomicBoolean(false);

    /** Cùng nguồn với application-test.yml để reset đúng database mà app test đang dùng. */
    private static final String JDBC_URL = "jdbc:postgresql://localhost:"
            + System.getenv().getOrDefault("POSTGRES_PORT", "5432") + "/quizdb";

    @LocalServerPort protected int port;
    @Autowired protected ObjectMapper json;
    @Autowired protected JdbcTemplate jdbc;
    protected final RestTemplate http = new RestTemplate();

    protected String baseUrl() { return "http://localhost:" + port; }

    @BeforeAll
    static void resetSchemaOnce() {
        if (RESET_DONE.compareAndSet(false, true)) {
            try (Connection c = DriverManager.getConnection(JDBC_URL, "quiz", "quiz");
                 Statement s = c.createStatement()) {
                s.execute("DROP SCHEMA IF EXISTS public CASCADE");
                s.execute("CREATE SCHEMA public");
                s.execute("GRANT ALL ON SCHEMA public TO quiz");
            } catch (Exception e) {
                throw new RuntimeException("Không reset được schema: " + e.getMessage(), e);
            }
        }
    }

    protected String login(String email, String password) {
        var req = new AuthDtos_LoginRequest(email, password);
        var res = http.postForObject(baseUrl() + "/api/auth/login", req, AuthDtos_AuthResponse.class);
        return res.token();
    }

    protected HttpHeaders authHeaders(String token) {
        var h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(token);
        return h;
    }

    @SuppressWarnings("unchecked")
    protected <T> T exchange(String method, String path, Object body, Class<T> respType, String token) {
        var headers = authHeaders(token);
        var entity = new HttpEntity<>(body, headers);
        return (T) http.exchange(baseUrl() + path, HttpMethod.valueOf(method), entity, respType).getBody();
    }
}
