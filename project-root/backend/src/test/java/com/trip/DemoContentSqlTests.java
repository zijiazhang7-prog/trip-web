package com.trip;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DemoContentSqlTests {

    private static final Pattern DEMO_PREFERENCE_ROW = Pattern.compile(
            "\\(@user_0[123]_id,\\s*(\\d+),\\s*'[^']*',\\s*'[^']*',\\s*(\\d+),");

    @Test
    void demoPreferenceLevelsShouldStayWithinApiRange() throws IOException {
        String sql;
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("db/init-demo-content.sql")) {
            assertThat(input).isNotNull();
            sql = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        Matcher matcher = DEMO_PREFERENCE_ROW.matcher(sql);
        int rowCount = 0;
        while (matcher.find()) {
            assertThat(Integer.parseInt(matcher.group(1))).isBetween(1, 5);
            assertThat(Integer.parseInt(matcher.group(2))).isBetween(1, 5);
            rowCount++;
        }
        assertThat(rowCount).isEqualTo(3);
    }
}
