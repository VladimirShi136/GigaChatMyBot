package com.GigaChatMyBot.config;

import org.springframework.stereotype.Component;
import com.GigaChatMyBot.telegram.GigaChatTelegramBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import jakarta.annotation.PostConstruct;

/**
 * Конфигурация для регистрации Telegram-бота.
 * Выполняет инициализацию бота через TelegramBotsApi при старте приложения.
 * @author vladimir_shi
 * @since 28.09.2025
 */
@Component // Регистрирует класс как компонент Spring для авто-вызова @PostConstruct
public class TelegramBotConfig {
    private static final Logger logger = LoggerFactory.getLogger(TelegramBotConfig.class); // Логгер для регистрации и ошибок
    private final GigaChatTelegramBot bot; // Инжектируемый экземпляр бота

    /**
     * Конструктор класса TelegramBotConfig.
     * @param bot - экземпляр GigaChatTelegramBot
     */
    public TelegramBotConfig(GigaChatTelegramBot bot) {
        this.bot = bot;
    }

    @PostConstruct // Вызывается Spring после создания бина и инъекции — регистрирует бота
    public void registerBot() {
        try {
            logger.info("Инициализируем Telegram API и регистрируем бота...");
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(bot);  // Регистрирует бота для long polling
            logger.info("Бот успешно зарегистрирован!");
        } catch (TelegramApiException e) {
            logger.error("Ошибка при регистрации бота: {}", e.getMessage(), e);
        }
    }
}
