package com.GigaChatMyBot;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Класс для загрузки конфигурационных параметров из файла конфигурации.
 *
 * @author vladimir_shi
 * @since 23.09.2025
 */
public class ConfigLoader {
    private final Properties props = new Properties();

    /**
     * Конструктор загрузчика конфигурации.
     * Загружает файл конфигурации по абсолютному или относительному пути в файловой системе.
     *
     * @param path путь к файлу конфигурации (например, "src/main/resources/config.properties")
     * @throws IOException если файл не найден или ошибка чтения
     */
    public ConfigLoader(String path) throws IOException {
        try (FileInputStream fis = new FileInputStream(path)) {
            props.load(fis);
        }
    }

    /**
     * Метод для получения значения свойства по ключу.
     *
     * @param key ключ свойства в файле конфигурации
     * @return значение свойства или null, если ключ отсутствует
     */
    public String get(String key) {
        return props.getProperty(key);
    }
}
