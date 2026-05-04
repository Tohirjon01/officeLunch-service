package uz.company.lunchbot.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public Clock appClock(LunchProperties lunchProperties) {
        return Clock.system(ZoneId.of(lunchProperties.timezone()));
    }
}
