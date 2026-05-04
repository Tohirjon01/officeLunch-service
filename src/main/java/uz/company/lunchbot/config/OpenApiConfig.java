package uz.company.lunchbot.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI lunchBotOpenApi() {
        return new OpenAPI().info(new Info()
                .title("Office Lunch Order Management System API")
                .version("v1")
                .description("Internal management API for office lunch ordering automation")
                .contact(new Contact().name("Office Lunch Bot")));
    }
}
