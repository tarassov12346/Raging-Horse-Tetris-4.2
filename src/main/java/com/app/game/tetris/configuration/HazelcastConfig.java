package com.app.game.tetris.configuration;

import com.hazelcast.config.Config;
import com.hazelcast.config.MapConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastConfig {
    @Bean
    public Config tetrisHazelcastConfig() {
        Config config = new Config();
        config.setInstanceName("hazelcast-instance");

        // Настройка мапы для состояний игры
        MapConfig userStatesConfig = new MapConfig();
        userStatesConfig.setName("user-states")
                .setTimeToLiveSeconds(3600); // Состояние живет час после последней активности

        config.addMapConfig(userStatesConfig);

        // Настройка сети (чтобы инстансы находили друг друга)
        config.getNetworkConfig().getJoin().getMulticastConfig().setEnabled(true);

        return config;
    }
}
