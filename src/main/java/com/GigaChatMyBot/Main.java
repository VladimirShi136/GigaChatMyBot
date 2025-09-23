package com.GigaChatMyBot;

import java.io.IOException;
import java.util.Scanner;
import java.util.UUID;

/**
 * Главный класс приложения — точка входа.
 * <p>
 * Задачи:
 * - Загрузить конфигурацию из файла
 * - Инициализировать объекты для работы с API
 * - Обеспечить цикл ввода пользователя и обработки запросов к GigaChat
 * <p>
 * автор: vladimir_shi
 * дата: 23.09.2025
 */
public class Main {

    /**
     * Главный метод — запускает программу.
     *
     * @param args аргументы командной строки (не используются)
     * @throws IOException при ошибках ввода-вывода (например, при чтении конфига)
     * @throws InterruptedException если операции HTTP прерываются
     */
    public static void main(String[] args) throws IOException, InterruptedException {
        // Загружаем конфигурацию из файла config.properties по указанному пути
        ConfigLoader configLoader = new ConfigLoader("src/main/resources/config.properties");

        // Создаём объект конфигурации, передавая параметры, считанные из файла
        GigaChatConfig config = new GigaChatConfig(
                configLoader.get("oauth.url"),                    // URL для получения токена
                configLoader.get("chat.url"),                     // URL вызова GigaChat API
                configLoader.get("auth.authorizationKeyBasic"),  // Ключ авторизации в формате Basic
                configLoader.get("scope"),                        // Область доступа OAuth
                configLoader.get("modelName")                     // Название модели для запросов
        );

        // Создаём объект для работы с GigaChat API
        GigaChatAPI api = new GigaChatAPI(config);

        // Создаём сканер для чтения ввода пользователя из консоли
        Scanner scanner = new Scanner(System.in);

        // Главный цикл программы — повторяем, пока пользователь не введёт "exit"
        while (true) {
            // Генерируем уникальный идентификатор запроса (RqUID) для отслеживания
            String rqUid = UUID.randomUUID().toString();

            // Запрашиваем Access Token у API
            String accessToken = api.getAccessToken(rqUid);
            if (accessToken == null) {
                // Завершаем программу, если не смогли получить токен
                System.err.println("Не удалось получить Access Token");
                return;
            }

            // Просим пользователя ввести текст запроса (промпт) или "exit" для выхода
            System.out.print("Введите запрос для GigaChat (\"exit\" для выхода): ");
            String prompt = scanner.nextLine();

            // Если пользователь ввёл пустую строку — выводим предупреждение и запрашиваем заново
            if (prompt.trim().isEmpty()) {
                System.out.println("Запрос не может быть пустым.");
                continue;
            }

            // Если ввёл "exit" (любой регистр) — завершаем цикл и программу
            if ("exit".equalsIgnoreCase(prompt.trim())) {
                System.out.println("Программа завершена.");
                break;
            }

            // Отправляем промпт на обработку в GigaChat API и получаем ответ
            String response = api.sendChatRequest(accessToken, prompt);

            // Выводим полученный ответ в консоль
            System.out.println("Ответ GigaChat:");
            System.out.println(response);
        }
    }
}
