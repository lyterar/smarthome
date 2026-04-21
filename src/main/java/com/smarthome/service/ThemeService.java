package com.smarthome.service;

import javafx.scene.Scene;
import javafx.scene.paint.Color;

/**
 * Сервис смены тем приложения.
 *
 * Причина: одна тема не позволяет пользователю настроить интерфейс под себя.
 * Следствие: три темы переключаются без перезапуска через метод applyTheme().
 *
 * GoF паттерн: Strategy
 * Каждая тема — стратегия (ThemeStrategy), выбираемая по enum Theme.
 * ThemeService — контекст, делегирующий применение нужной стратегии.
 *
 * Этот класс также хранит текущую тему, чтобы Room3DView мог получить
 * цвет фона через getSubSceneColor().
 */
public class ThemeService {

    // =========================================================
    //  Enum тем (выбор стратегии)
    // =========================================================

    public enum Theme {
        DARK,
        LIGHT,
        BLUE
    }

    // =========================================================
    //  Интерфейс стратегии
    // =========================================================

    /** Стратегия применения темы. */
    public interface ThemeStrategy {
        /** Путь к CSS-файлу темы (classpath, начинается с '/') */
        String getCssPath();
        /** Цвет фона SubScene в 3D-режиме */
        Color getSubSceneColor();
        /** Человекочитаемое название */
        String getDisplayName();
    }

    // =========================================================
    //  Конкретные стратегии
    // =========================================================

    /** Тёмная тема (по умолчанию). */
    private static class DarkTheme implements ThemeStrategy {
        @Override public String getCssPath()        { return "/css/theme-dark.css"; }
        @Override public Color  getSubSceneColor()  { return Color.web("#1a1a2e"); }
        @Override public String getDisplayName()    { return "Тёмная"; }
    }

    /** Светлая тема. */
    private static class LightTheme implements ThemeStrategy {
        @Override public String getCssPath()        { return "/css/theme-light.css"; }
        @Override public Color  getSubSceneColor()  { return Color.web("#e8e8f0"); }
        @Override public String getDisplayName()    { return "Светлая"; }
    }

    /** Синяя тема. */
    private static class BlueTheme implements ThemeStrategy {
        @Override public String getCssPath()        { return "/css/theme-blue.css"; }
        @Override public Color  getSubSceneColor()  { return Color.web("#0d1b2a"); }
        @Override public String getDisplayName()    { return "Синяя"; }
    }

    // =========================================================
    //  Состояние контекста
    // =========================================================

    private Theme          currentTheme    = Theme.DARK;
    private ThemeStrategy  currentStrategy = new DarkTheme();

    // Singleton — один ThemeService на приложение
    private static final ThemeService INSTANCE = new ThemeService();
    private ThemeService() {}

    public static ThemeService getInstance() {
        return INSTANCE;
    }

    // =========================================================
    //  Выбор стратегии
    // =========================================================

    private ThemeStrategy strategyFor(Theme theme) {
        return switch (theme) {
            case DARK  -> new DarkTheme();
            case LIGHT -> new LightTheme();
            case BLUE  -> new BlueTheme();
        };
    }

    // =========================================================
    //  Публичный API
    // =========================================================

    /**
     * Применяет тему к сцене.
     *
     * Причина: пользователь хочет сменить тему без перезапуска.
     * Следствие: список стилей сцены заменяется на новый CSS-файл.
     *
     * @param scene сцена JavaFX
     * @param theme выбранная тема
     */
    public void applyTheme(Scene scene, Theme theme) {
        currentTheme    = theme;
        currentStrategy = strategyFor(theme);

        // Заменяем все стили — оставляем только тему
        scene.getStylesheets().clear();
        String cssUrl = getClass().getResource(currentStrategy.getCssPath()).toExternalForm();
        scene.getStylesheets().add(cssUrl);
    }

    /** Возвращает цвет фона SubScene для текущей темы. */
    public Color getSubSceneColor() {
        return currentStrategy.getSubSceneColor();
    }

    /** Возвращает текущую тему. */
    public Theme getCurrentTheme() {
        return currentTheme;
    }

    /** Возвращает название текущей темы. */
    public String getCurrentThemeName() {
        return currentStrategy.getDisplayName();
    }
}
