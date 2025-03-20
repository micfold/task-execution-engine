package cz.rb.task.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.jdbc.DataSourceBuilder;

import javax.sql.DataSource;

/**
 * This is the default java doc for the class
 *
 * @Project: edi-task-execution-engine
 * @Author: micfold on 20.03.2025
 */
@Configuration
public class DataSourceConfig {

    @Bean
    public DataSource dataSource() {
        return DataSourceBuilder.create().build();
    }
}
