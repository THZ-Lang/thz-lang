package thz.lang.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = "thz.lang")
public class ThzApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(ThzApiApplication.class, args);
    }
}
