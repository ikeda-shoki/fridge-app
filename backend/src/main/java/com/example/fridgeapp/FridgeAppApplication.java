package com.example.fridgeapp;

import com.example.fridgeapp.common.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/** アプリケーションのエントリポイント。 */
@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class FridgeAppApplication {

  public static void main(String[] args) {
    SpringApplication.run(FridgeAppApplication.class, args);
  }
}
