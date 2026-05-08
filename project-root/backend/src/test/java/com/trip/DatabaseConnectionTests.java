package com.trip;

import com.trip.mapper.UserMapper;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DatabaseConnectionTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private UserMapper userMapper;

    @Test
    void shouldConnectToDatabaseWhenCredentialEnvironmentExists() throws SQLException {
        Assumptions.assumeTrue(hasDatabasePassword(), "DB_PASSWORD is not set in current process");

        try (Connection connection = dataSource.getConnection()) {
            assertThat(connection.isValid(3)).isTrue();
            assertThat(connection.getCatalog()).isEqualTo("tour_system");
        }

        Long userCount = userMapper.selectCount(null);
        assertThat(userCount).isNotNull();
    }

    private boolean hasDatabasePassword() {
        String propertyPassword = System.getProperty("spring.datasource.password");
        if (propertyPassword != null) {
            return true;
        }

        String password = System.getenv("DB_PASSWORD");
        return password != null;
    }
}
