package com.GigaChatMyBot.gigachat;

import com.GigaChatMyBot.model.GigaChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Класс реализует взаимодействие с API GigaChat.
 * Получает OAuth токен и отправляет запросы в чат GigaChat.
 * Использует HttpClient из Java 11+.
 * @author vladimir_shi
 * @since 23.09.2025
 */
public class GigaChatAPI {
    private final GigaChatModel config; // Конфигурация с URL, ключами и параметрами
    private final HttpClient client; // HttpClient для сетевых запросов
    private static final Logger logger = LoggerFactory.getLogger(GigaChatAPI.class); // Логгер для ошибок и инициализации

    /**
     * Конструктор.
     * Принимает объект конфигурации и инициализирует HttpClient.
     * @param config конфигурация с URL, ключами и параметрами
     */
    public GigaChatAPI(GigaChatModel config) {
        this.config = config;
        this.client = HttpClient.newHttpClient();
        logger.info("GigaChatAPI инициализирован с URL OAuth: {}, Chat: {}", config.getOauthUrl(), config.getChatUrl());
    }

    /**
     * Получает Access Token через POST-запрос на OAuth endpoint.
     * @param rqUid уникальный ID запроса
     * @return токен или null при ошибке
     * @throws IOException при сетевых ошибках
     * @throws InterruptedException при прерывании
     */
    public String getAccessToken(String rqUid) throws IOException, InterruptedException {
        // Тело запроса с указанием scope (области доступа)
        String requestBody = "scope=" + config.getScope();
        // Строим HTTP-запрос методом POST с нужными заголовками
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getOauthUrl()))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .header("RqUID", rqUid)  // уникальный ID запроса в заголовке
                .header("Authorization", config.getAuthorizationKeyBasic())  // Basic ключ
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        // Отправляем запрос и получаем ответ в виде строки
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Если статус 200 — пытаемся извлечь токен из ответа
        if (response.statusCode() == 200) {
            return extractAccessToken(response.body());
        } else {
            logger.error("Не удалось получить access token. Статус: {}, Ответ: {}", response.statusCode(), response.body());
            return null;
        }
    }

    /**
     * Парсит Access Token из JSON-ответа.
     * Ищет "access_token":"значение".
     * @param json JSON-строка
     * @return токен или null
     */
    private String extractAccessToken(String json) {
        String marker = "\"access_token\":\"";
        int start = json.indexOf(marker);
        if (start < 0) {
            logger.warn("Маркер access_token не найден в JSON");
            return null;
        }
        start += marker.length();
        int end = json.indexOf("\"", start);
        return (end < 0) ? null : json.substring(start, end);
    }

    /**
     * Отправляет запрос на chat completion с Bearer-токеном.
     * Формирует JSON с моделью и промптом.
     * @param accessToken Bearer-токен
     * @param prompt текст запроса
     * @return ответ GigaChat или ошибка
     * @throws IOException при сетевых ошибках
     * @throws InterruptedException при прерывании
     */
    public String sendChatRequest(String accessToken, String prompt) throws IOException, InterruptedException {
        // Формируем тело запроса, подставляя модель из конфигурации и экранируя JSON содержимое
        String jsonRequestBody = """
                {
                    "model": "%s",
                    "messages": [
                        {
                            "role": "user",
                            "content": "%s"
                        }
                    ],
                    "stream": false,
                    "repetition_penalty": 1
                }
                """.formatted(config.getModelName(), escapeJson(prompt));

        // Формируем POST-запрос с Bearer токеном в заголовке Authorization
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(config.getChatUrl()))
                .header("Authorization", "Bearer " + accessToken)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonRequestBody))
                .build();

        // Отправляем запрос и получаем ответ в виде строки
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Если успешно (200), парсим ответ
        if (response.statusCode() == 200) {
            return parseChatResponse(response.body());
        } else {
            logger.error("Ошибка в Chat API. Статус: {}, Ответ: {}", response.statusCode(), response.body());
            return "Ошибка при вызове GigaChat: " + response.statusCode() + "\n" + response.body();
        }
    }

    /**
     * Базовое экранирование символов в JSON строке.
     * Заменяет обратные слэши и кавычки, чтобы JSON был корректным.
     * @param text текст для экранирования
     * @return экранированный текст
     */
    private String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Простейший парсер для извлечения содержимого сообщения ассистента из JSON-ответа.
     * Ищет поле content в JSON и возвращает его значение с декодированием escape-последовательностей.
     * @param json строка JSON с ответом чата
     * @return текст ответа или сообщение об ошибке, если контент не найден
     */
    private String parseChatResponse(String json) {
        String marker = "\"content\":\"";
        int index = json.indexOf(marker);
        // Если поле контента не найдено — возвращаем ошибку
        if (index < 0) {
            return "Не удалось найти ответ в JSON.";
        }

        // Определяем начало и конец контента в JSON
        int start = index + marker.length();
        int end = json.indexOf("\"", start);

        // Обработка экранированных кавычек в содержимом
        while (end > start && json.charAt(end - 1) == '\\') {
            end = json.indexOf("\"", end + 1);
        }

        if (end < 0) end = json.length();

        // Извлекаем сырой контент и заменяем escape-последовательности на реальные символы
        String content = json.substring(start, end);
        return content
                .replace("\\n", "\n")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
