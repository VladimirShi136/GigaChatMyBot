    *Документация по Spring Boot Telegram-боту с интеграцией GigaChat*
Telegram-бот, который обрабатывает сообщения через GigaChat API на Java/Spring Boot.
Параметры-секреты в application.properties — важно: это не безопасно для продакшена, перенести в env.
Документация включает жизненный цикл, архитектуру, схемы/структуры и всё нужное.

    1. Общий взгляд
Приложение реализует Telegram-бота для автоматического диалога с пользователями через ИИ GigaChat (от Сбера). Пользователь пишет боту в Telegram, бот отправляет запрос в GigaChat API, получает ответ и отсылает обратно. Использует Spring Boot для деплоя и telegrambots-spring-boot-starter для поллинга обновлений.
    - Функции: Обработка текстовых сообщений, получение OAuth-токенов от Sber, чат с GigaChat.
    - Технологии: Java 21, Spring Boot 3.1.4, Hibernate Validator, Telegram Bots API, HttpClient (Java 11+).
    - Зависимости: В pom.xml — Spring Boot Core, Telegram Starter, логгер SLF4J.
    - Порт по умолчанию: 8080 (Tomcat embedded).

    2. Архитектура приложения
Архитектура основана на MVC-подобном паттерне в Spring: разделение на слои (контроллер для бота, сервис для логики, API для внешних вызовов).

2.1. Структура каталогов (tree view)
src/main/java/com/GigaChatMyBot/
├── GigaChatTelegramBotApplication.java  # Главный класс запуска
├── telegram/
│   └── GigaChatTelegramBot.java         # Бот для Telegram (обработка updates)
├── service/
│   └── GigaChatService.java             # Сервис логики в GigaChat
├── gigachat/
│   └── GigaChatAPI.java                 # Клиент для API GigaChat (OAuth + чат)
├── model/
│   └── GigaChatModel.java               # POJO с конфигурацией GigaChat
├── config/
│   ├── GigaChatSpringConfig.java        # Spring-инъекция свойств для GigaChat
│   └── TelegramBotConfig.java           # Регистрация бота (WebHook/Polling)
src/main/resources/
├── application.properties               # Конфигурация (лог, свойства бота)
pom.xml                                  # Зависимости Maven

2.2. Диаграмма классов и зависимостей (текстовое представление)
[Spring Boot Context] -----> GigaChatBotApplication (главный класс)
          |
          |-- GigaChatTelegramBot (Component: наследует TelegramLongPollingBot)
          |     |-- onUpdateReceived: обрабатывает сообщения от Telegram
          |     |-- getBotToken/getBotUsername: геттеры для аутентификации
          |     └--> GigaChatService (поле, инжектировано)
          |
          |-- GigaChatService (Service: бизнес-логика)
          |     |-- askGigaChat: отправляет prompt в GigaChat
          |     └--> GigaChatAPI (поле)
          |
          |-- GigaChatAPI (клиент: HttpClient для API)
          |     |-- getAccessToken: OAuth вызов
          |     └--> sendChatRequest: Chat вызов
          |
          |-- GigaChatSpringConfig (Component: @ConfigurationProperties)
          |     └--> GigaChatModel (POJO с URL/ключами)
          |
          └-- TelegramBotConfig (Component: регистрирует бот явно)

Внешние зависимости:
    - Telegram API (telegrambots) ----> OAuth/token
    - GigaChat API (HttpClient) ----> Chat completions

2.3. Диаграмма потоков данных (текстовое представление)
Пользователь (Telegram) ----> Telegram сервера ----> Бот получает update
                               ↓
GigaChatTelegramBot.onUpdateReceived() ----> GigaChatService.askGigaChat()
                                               ↓
                                     GigaChatAPI.getAccessToken() ----> OAuth: token
                                               ↓
                                     GigaChatAPI.sendChatRequest() ----> Chat: ответ ИИ
                                               ↓
                            GigaChatTelegramBot.sendMessage() ----> Пользователь получает ответ

    3. Жизненный цикл приложения
По пунктам от запуска до остановки (как Spring управляет бинами и потоками).
Это стандартный цикл Spring Boot.
------------------------------------------------------
3.1. Этап 1: Инициализация Spring Context
Приложение запускается в main() через SpringApplication.run().
Spring сканирует @Component, @Service, @Configuration и создаёт бины.
Импортятся свойства из application.properties (токен, URL, логи).
Валидируются зависимости (например, GigaChatService инжектируется в бота).
------------------------------------------------------
3.2. Этап 2: Создание бинов (Beans)
GigaChatSpringConfig: Загружает свойства (например, gigachat.oauth-url).
GigaChatModel: Создаётся как POJO с данными из config.
GigaChatAPI: Инициализируется с HttpClient и config.
GigaChatService: Инжектируется GigaChatSpringConfig, создаёт API-клиент.
GigaChatTelegramBot: Инжектируется сервис и свойства (токен/имя), вызывает @PostConstruct для проверки null.
TelegramBotConfig: В @PostConstruct регистрирует бота в TelegramBotsApi для long polling — бот начинает слушать updates от Telegram.
------------------------------------------------------
3.3. Этап 3: Рабочая фаза (Runtime)
Polling поток: Telegram Bots (стадий) запускает фоновый поток для поллинга (getUpdates() → onUpdateReceived().
Обработка сообщения: При получении update от пользователя → боту → сервису → API → ответ обратно.
Логгирование: INFO для ключевых событий (инит, запрос, ошибка), ERROR для фейлов.
Фон: Spring Tomcat слушает порт 8080, но бот работает asynchronously.
------------------------------------------------------
3.4. Этап 4: Остановка
При CTRL+C или системном shutdown: Spring закрывает context.
TelegramBotConfig: Останавливает polling (если webhook — отключает).
HttpClient: Закрывается автоматически.
Логи: "Shutting down application".
------------------------------------------------------
3.5. Временная шкала (примерная)
0-1 сек: Spring старт и бин-инициализация.
1-5 сек: Регистрация бота и подключение к Telegram.
5+ сек: Приложение готово, ждёт сообщений.
При сообщение: 2-10 сек (зависит от GigaChat скорости).

    4. Workflow пользователя (как использовать бота)
Пользователь находит бота в Telegram (по username из telegram.bot.username).
Пишет /start или любое сообщение.
Бот получает update, отправляет prompt в GigaChat.
GigaChat генерирует ответ (например, диалог о погоде).
Бот отсылает ответ в чат пользователя.
Если ошибка: Бот шлёт "Ошибка при обработке".
------------------------------------------------------
4.1. Схема workflow (текстовое представление)
[Пользователь] --> "/start" --> [Telegram API] --> [Бот] --> "Привет! Как дела?"
                                                                   ↓
    prompt --> [GigaChat] --> Генерирует ответ "Отлично, расскажи о себе"
                                                                   ↓
                            [Бот] --> Отсылает в Telegram чат
------------------------------------------------------
    5. Установка и запуск
5.1. Требования
JDK 21+, Maven 3.8+.
Аккаунт в Telegram (создай бота через @BotFather, получи токен).
Аккаунт в Sber GigaChat (получи Basic-ключ и URL).
5.2. Шаги установки
Клонируй/kлонискуюном репозиторий.
Отредактируй application.properties: вставь твой telegram.bot.token, gigachat.authorization-key-basic и URL'ы.
Сохрани pom.xml (не меняй зависимости).
В IDEA: Импорт как Maven-проект.
Запий в IDEA или terminal: mvn clean compile && mvn spring-boot:run.
Проверь консоль: Логи "Бот успешно зарегистрирован" — приложение запущено.
Протестируй: напиши боту в Telegram.
5.3. Деплой (продакшил)
Сычный JAR: mvn clean package, затем java -jar target/GigaChatMyBot-1.0-SNAPSHOT.jar.
Docker: Добавь Dockerfile (используй base image openjdk:21-jdk-slim), копируй JAR и запускай.
Сервер: Используй systemd для автозапуска, с env vars (несмотря на твой выбор без env, рекомендуется перенести).

    6. Конфигурация
6.1. Основные настройки (application.properties)
Логи: logging.level.* — уровень вывода (WARN для JVM, INFO для твоего пакета).
Бот: telegram.bot.username — публичное имя (@BotFather), telegram.bot.token — секрет.
GigaChat: gigachat.* — URL'ы, ключ, scope, модель.
Spring: Ничего особого, дефолты (порт 8080, aiiике нет веб-API).
6.2. Кастомизация
Скорость GigaChat: Измени repetition_penalty в GigaChatAPI.sendChatRequest().
Лимиты: Добавь rate limiting (например, запомнить пользовательские запросы).
Webhook: Если polling медленный, перешли на webhook (измені TelegramBotConfig на WebhookBot).

    7. Troubleshooting (проблемы и решения)
При запуске: Ошибка "КРИТИЧЕСКАЯ ОШИБКА: свойства бота null" → проверь application.properties (токен должен быть строкой без пробелов).
Бот не получает сообщения: Проверь токен в getMe, сеть (ping api.telegram.org), лог onUpdateReceived.
GigaChat ошибка: 429 (rate limit) → подожди час, или проверь квоту. 401 — ключ истёк, обнови в Sber.
HttpClient фан: Асинхронные вызовы (CompletableFuture), если синхронные блокируют.
Логи: Если не видит updates, добавь logger.debug в onUpdateReceived, включи DEBUG для org.telegram.
JVM crashes: Проверь heap (-Xmx1g), сетевые таймауты в HttpClient.

    8. Безопасность
Сейчас: Токены и ключи в application.properties — НЕ коммит в Git! Добавь в .gitignore.
В продакшене: Так, как указал раньше — env vars, секреты облака. Логируй секреты — повод для врага.
OAuth: Sber требует Basic-ключ, не фла, храни безопасно.
Атаки: Rate limit снижает DDoS, validaция inputs (несмотря на текст, проверь длин).

    9. Будущие улучшения
Секреты: Перенести в env/секреты.
Асинхронность: Добавить @Async для GigaChat (не блокировать бот).
Кэш: Сохранить токены (Redis), язык аудио GigaChat поддерживает.
Модулы: Разделить на подпроекты Milo (bot, api).
Тесты: Добавь unit-тесты (JUnit/Mockito) для GigaChatService.
UI: Веб-интерфейс (Thymeleaf) для управления ботом.

!!!->>>>>>>>
Добавить в будущем: схемы в ASCII, полный README файл, диаграммы в PlantUML