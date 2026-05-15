package com.nmleytem.cursorpagination;

import com.nmleytem.repository.EventRepository;
import com.nmleytem.runner.SeedDataRunner;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import javax.sql.DataSource;

import static org.mockito.Mockito.mock;

@SpringBootTest
class CursorpaginationApplicationTests {

    @TestConfiguration
    static class Config {
        @Bean
        public DataSource dataSource() {
            return mock(DataSource.class);
        }

        @Bean
        public JdbcTemplate jdbcTemplate(DataSource dataSource) {
            return mock(JdbcTemplate.class);
        }
    }

    @MockitoBean
    private EventRepository eventRepository;

    @MockitoBean
    private SeedDataRunner seedDataRunner;

    @Test
    void contextLoads() {
    }

}
