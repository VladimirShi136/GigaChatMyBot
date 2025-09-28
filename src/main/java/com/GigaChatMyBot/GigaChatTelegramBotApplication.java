package com.GigaChatMyBot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Основной класс приложения.
 * @author vladimir_shi
 * @since 24.09.2025
 */
@SpringBootApplication
public class GigaChatTelegramBotApplication {

    /**
     * Точка входа в приложение.
     * @param args аргументы командной строки
     */
    public static void main(String[] args) {
        SpringApplication.run(GigaChatTelegramBotApplication.class, args);
    }
}
