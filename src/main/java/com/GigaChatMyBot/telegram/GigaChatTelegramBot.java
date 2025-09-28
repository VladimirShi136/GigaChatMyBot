package com.GigaChatMyBot.telegram;

import com.GigaChatMyBot.service.GigaChatService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

/**
 * Telegram-бот на базе GigaChat.
 * Обрабатывает входящие сообщения и отправляет их в GigaChat с помощью gigaChatService.
 * Получает ответы через askGigaChat и отправляет их обратно в Telegram через sendMessage.
 * @author vladimir_shi
 * @since 24.09.2025
 */
@Component // Регистрирует класс как компонент Spring, чтобы бот автоматически инжектировался и регистрировался
public class GigaChatTelegramBot extends TelegramLongPollingBot {
    private final GigaChatService gigaChatService; // Сервис для обработки запросов к GigaChat
    private static final Logger logger = LoggerFactory.getLogger(GigaChatTelegramBot.class); // Логгер для сообщений об ошибках

    @Value("${telegram.bot.username:default_bot_username}") // Инжектирует имя бота из application.properties
    private String botUsername;

    @Value("${telegram.bot.token:default_token}") // Инжектирует токен бота из application.properties
    private String botToken;

    /**
     * Конструктор Telegram-бота.
     * @param gigaChatService - сервис для обработки запросов к GigaChat
     */
    public GigaChatTelegramBot(GigaChatService gigaChatService) {
        this.gigaChatService = gigaChatService;
    }

    @PostConstruct // @PostConstruct Вызывается Spring после инъекции зависимостей и свойств
    public void init() {
        if (botUsername == null || botToken == null || botToken.isEmpty()) {
            logger.error("КРИТИЧЕСКАЯ ОШИБКА: свойства бота null! Проверьте application.properties и токен.");
        } else {
            logger.info("Telegram бот инициализирован успешно: username={}, token=доступен", botUsername);
        }
    }

    @Override // Переопределён от TelegramLongPollingBot для доступа к имени
    public String getBotUsername() {
        return botUsername;
    }

    @Override // Переопределён от TelegramLongPollingBot для доступа к токену
    public String getBotToken() {
        return botToken;
    }

    /**
     * Обрабатывает входящие сообщения из Telegram-бота и отправляет их в GigaChat с помощью gigaChatService.
     * @param update - входящее сообщение
     */
    @Override // Переопределён от TelegramLongPollingBot — основной метод для обработки updates
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String prompt = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();
            logger.info("Получено сообщение от пользователя chatId={}: {}", chatId, prompt);  // Логируем входящее сообщение
            String response;
            try {
                response = gigaChatService.askGigaChat(prompt);
                logger.info("Отправляем ответ в Telegram для chatId={}: {}", chatId, response);  // Отправка в Telegram
                sendMessage(chatId, response);
            } catch (Exception e) {
                logger.error("Ошибка при обработке сообщения от chatId={}: {}", chatId, e.getMessage(), e);  // Логируем ошибки
                sendMessage(chatId, "Произошла ошибка при обработке запроса. Попробуйте ещё раз.");
            }
        } else {
            logger.warn("Получено обновление без текста или сообщения: {}", update);  // Логируем непредвиденные обновления
        }
    }

    /**
     * Отправляет сообщение в Telegram.
     * @param chatId - идентификатор чата
     * @param text - текст сообщения
     */
    private void sendMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            logger.error("Не удалось отправить сообщение в чат chatId={}: {}", chatId, e.getMessage(), e);  // Ошибка отправки
        }
    }
}