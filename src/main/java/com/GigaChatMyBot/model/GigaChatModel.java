package com.GigaChatMyBot.model;

/**
 * Класс для хранения конфигурационных параметров, необходимых для работы с API GigaChat.
 * Основная задача — инкапсулировать настройки, чтобы упростить передачу параметров
 * (URLs, ключи, параметры запроса) в другие части приложения.
 *
 * @author vladimir_shi
 * @since 23.09.2025
 */
public class GigaChatModel {
    // URL для получения OAuth токена доступа
    private final String oauthUrl;
    // URL для обращения к сервису GigaChat
    private final String chatUrl;
    // Ключ авторизации в формате Basic (должен содержать префикс "Basic " + base64)
    private final String authorizationKeyBasic;
    // Параметр scope, передаваемый при запросе OAuth токена (например, "GIGACHAT_API_PERS")
    private final String scope;
    // Название модели, используемой при вызове GigaChat (например, "GigaChat")
    private final String modelName;

    /**
     * Конструктор класса.
     * Инициализирует все поля, которые необходимы для работы с API.
     * Все поля final, что обеспечивает неизменяемость конфигурации после создания объекта.
     *
     * @param oauthUrl              URL для OAuth токена
     * @param chatUrl               URL для вызова GigaChat API
     * @param authorizationKeyBasic базовый ключ авторизации с префиксом "Basic "
     * @param scope                 область доступа OAuth
     * @param modelName             имя модели для запросов к GigaChat
     */
    public GigaChatModel(String oauthUrl, String chatUrl, String authorizationKeyBasic, String scope, String modelName) {
        this.oauthUrl = oauthUrl;
        this.chatUrl = chatUrl;
        this.authorizationKeyBasic = authorizationKeyBasic;
        this.scope = scope;
        this.modelName = modelName;
    }

    // Геттеры для доступа к приватным полям

    public String getOauthUrl() {
        return oauthUrl;
    }

    public String getChatUrl() {
        return chatUrl;
    }

    public String getAuthorizationKeyBasic() {
        return authorizationKeyBasic;
    }

    public String getScope() {
        return scope;
    }

    public String getModelName() {
        return modelName;
    }
}
