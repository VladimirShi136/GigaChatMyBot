package com.GigaChatMyBot.config;


import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Spring-конфигурация для свойств GigaChat.
 * Автоматически загружает значения из application.properties под префиксом "gigachat".
 * @author vladimir_shi
 * @since 24.09.2025
 */
@Component // Регистрирует класс как компонент Spring
@ConfigurationProperties(prefix = "gigachat") // Загружает свойства с префиксом "gigachat" в поля
public class GigaChatSpringConfig {
    // URL для OAuth-токена
    private String oauthUrl;
    // URL для запросов в чат
    private String chatUrl;
    // Ключ авторизации
    private String authorizationKeyBasic;
    // Область видимости
    private String scope;
    // Имя модели
    private String modelName;

    // Геттеры и сеттеры

    public String getOauthUrl() {
        return oauthUrl;
    }
    public void setOauthUrl(String oauthUrl) {
        this.oauthUrl = oauthUrl;
    }
    public String getChatUrl() {
        return chatUrl;
    }
    public void setChatUrl(String chatUrl) {
        this.chatUrl = chatUrl;
    }
    public String getAuthorizationKeyBasic() {
        return authorizationKeyBasic;
    }
    public void setAuthorizationKeyBasic(String authorizationKeyBasic) {
        this.authorizationKeyBasic = authorizationKeyBasic;
    }
    public String getScope() {
        return scope;
    }
    public void setScope(String scope) {
        this.scope = scope;
    }
    public String getModelName() {
        return modelName;
    }
    public void setModelName(String modelName) {
        this.modelName = modelName;
    }
}