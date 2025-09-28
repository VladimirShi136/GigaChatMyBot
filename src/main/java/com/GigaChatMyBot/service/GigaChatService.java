package com.GigaChatMyBot.service;

import com.GigaChatMyBot.gigachat.GigaChatAPI;
import com.GigaChatMyBot.model.GigaChatModel;
import com.GigaChatMyBot.config.GigaChatSpringConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.UUID;

/**
 * Сервис для взаимодействия с API GigaChat.
 * Обрабатывает запросы пользователей, получая ответы от GigaChat через GigaChatAPI.
 * @author vladimir_shi
 * @since 24.09.2025
 */
@Service  // Аннотирует класс как сервис Spring для авто-инъекции в другие компоненты
public class GigaChatService {
    private final GigaChatAPI gigaChatAPI;  // API-клиент для общения с GigaChat

    private static final Logger logger = LoggerFactory.getLogger(GigaChatService.class);  // Логгер для сообщений об инициализации и ошибках

    /**
     * Конструктор сервиса GigaChatService.
     * Инициализирует API-клиент GigaChatAPI с конфигурацией из GigaChatSpringConfig.
     * @param springConfig - конфигурация GigaChatSpringConfig
     */
    public GigaChatService(GigaChatSpringConfig springConfig) {
        GigaChatModel config = new GigaChatModel(
                springConfig.getOauthUrl(),
                springConfig.getChatUrl(),
                springConfig.getAuthorizationKeyBasic(),
                springConfig.getScope(),
                springConfig.getModelName()
        );
        this.gigaChatAPI = new GigaChatAPI(config);
        logger.info("GigaChatService инициализирован с моделью: {}", config.getModelName());
    }

    /**
     * Отправляет prompt в GigaChat и возвращает ответ.
     * Генерирует уникальный rqUid для запроса, получает token и вызывает API.
     * @param prompt текст запроса
     * @return ответ от GigaChat
     */
    public String askGigaChat(String prompt) {
        logger.info("Метод askGigaChat вызван с prompt: {}", prompt);
        try {
            String rqUid = UUID.randomUUID().toString();
            String accessToken = gigaChatAPI.getAccessToken(rqUid);
            if (accessToken == null) {
                logger.error("Не удалось получить access token");
                return "Ошибка: не удалось получить access token";
            }
            String response = gigaChatAPI.sendChatRequest(accessToken, prompt);
            logger.info("Ответ от GigaChat API: {}", response);
            return response;
        } catch (Exception e) {
            logger.error("Ошибка в методе askGigaChat: {}", e.getMessage(), e);
            return "Ошибка при вызове GigaChat: " + e.getMessage();
        }
    }
}