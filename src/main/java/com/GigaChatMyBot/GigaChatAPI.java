package com.GigaChatMyBot;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Класс GigaChatAPI реализует взаимодействие с API GigaChat.
 * <p>
 * Основные задачи класса:
 * - Получение OAuth Access Token через API аутентификации
 * - Отправка запросов на чат у GigaChat и получение ответов
 * <p>
 * Использует HttpClient из Java 11+ для сетевых запросов.
 * <p>
 * Автор: vladimir_shi
 * Дата: 23.09.2025
 */
public class GigaChatAPI {
    // Объект с конфигурацией (URL, ключи, параметры)
    private final GigaChatConfig config;
    // HttpClient — отвечает за сетевые запросы, создаётся один раз на весь класс
    private final HttpClient client;

    /**
     * Конструктор.
     * Принимает объект конфигурации и инициализирует HttpClient.
     *
     * @param config конфигурация с URL, ключами и параметрами
     */
    public GigaChatAPI(GigaChatConfig config) {
        this.config = config;
        this.client = HttpClient.newHttpClient();
    }

    /**
     * Получение Access Token.
     * Формирует и отправляет POST-запрос на endpoint OAuth для получения токена.
     *
     * @param rqUid уникальный идентификатор запроса (например, UUID)
     * @return строка Access Token или null при ошибке
     * @throws IOException          при ошибках ввода-вывода
     * @throws InterruptedException если был прерван запрос
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
            // Иначе выводим ошибку и возвращаем null
            System.err.println("Ошибка получения токена: " + response.statusCode());
            System.err.println(response.body());
            return null;
        }
    }

    /**
     * Вспомогательный метод для парсинга Access Token из JSON-ответа.
     * <p>
     * Работает упрощённо: ищет строку "access_token":"значение" и возвращает значение.
     *
     * @param json строка JSON с ответом сервера
     * @return Access Token или null, если не найден
     */
    private String extractAccessToken(String json) {
        String marker = "\"access_token\":\"";
        int start = json.indexOf(marker);
        if (start < 0) return null;
        start += marker.length();
        int end = json.indexOf("\"", start);
        if (end < 0) return null;
        return json.substring(start, end);
    }

    /**
     * Отправка запроса на chat completion GigaChat.
     * Формирует JSON с промптом, моделью и дополнительными параметрами.
     * Отправляет POST-запрос с токеном авторизации.
     *
     * @param accessToken valid Bearer токен для авторизации
     * @param prompt      запрос пользователя, который отправляем в чат
     * @return ответ от GigaChat или текст ошибки
     * @throws IOException          исключения сетевого ввода-вывода
     * @throws InterruptedException если запрос был прерван
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
            // Иначе возвращаем описание ошибки
            return "Ошибка при вызове GigaChat: " + response.statusCode() + "\n" + response.body();
        }
    }

    /**
     * Базовое экранирование символов в JSON строке.
     * Заменяет обратные слэши и кавычки, чтобы JSON был корректным.
     *
     * @param text текст для экранирования
     * @return экранированный текст
     */
    private String escapeJson(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    /**
     * Простейший парсер для извлечения содержимого сообщения ассистента из JSON-ответа.
     * <p>
     * Ищет поле content в JSON и возвращает его значение с декодированием escape-последовательностей.
     *
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
