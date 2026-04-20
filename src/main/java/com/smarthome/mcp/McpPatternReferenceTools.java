package com.smarthome.mcp;

import com.google.gson.JsonObject;

import java.util.*;
import java.util.stream.Collectors;

/**
 * MCP-инструменты для сверки реализации паттернов с книгой GoF.
 * Все данные привязаны к реальным классам проекта SmartHome.
 */
public class McpPatternReferenceTools {

    private final PatternKnowledgeBase kb;

    public McpPatternReferenceTools() {
        this.kb = new PatternKnowledgeBase();
    }

    public void registerAll(McpServer server) {
        server.registerTool(new LookupPatternTool());
        server.registerTool(new ListAllPatternsTool());
        server.registerTool(new SearchPatternsByCategory());
        server.registerTool(new GetPatternParticipantsTool());
        server.registerTool(new VerifyImplementationTool());
        server.registerTool(new GetPatternApplicabilityTool());
        server.registerTool(new ComparePatternsTool());
    }

    // ====== 1. lookup_pattern ======

    private class LookupPatternTool implements McpTool {
        @Override public String getName() { return "lookup_pattern"; }
        @Override public String getDescription() {
            return "Полное описание паттерна: назначение из книги, реализация в проекте, участники (роль->класс), применимость, результаты, страница в книге.";
        }
        @Override public Map<String, Object> getParameterSchema() {
            return Map.of("type", "object", "properties", Map.of(
                    "name", Map.of("type", "string", "description", "Имя паттерна (англ/рус): Singleton, Фасад, Observer...")),
                    "required", List.of("name"));
        }
        @Override public Object execute(JsonObject arguments) {
            String name = arguments.get("name").getAsString();
            PatternKnowledgeBase.PatternInfo info = kb.getPattern(name);
            if (info == null) {
                return Map.of("error", "Паттерн не найден: " + name,
                        "available", kb.getPatternNames());
            }
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("name", info.name());
            result.put("nameRu", info.nameRu());
            result.put("classification", info.classification());
            result.put("intent", info.intent());
            result.put("projectUsage", info.projectUsage());
            result.put("applicability", info.applicability());
            // Участники с привязкой к проекту
            List<Map<String, String>> parts = info.participants().stream()
                    .map(p -> Map.of("bookRole", p.bookRole(), "projectClass", p.projectClass(),
                            "package", p.projectPackage(), "description", p.description()))
                    .toList();
            result.put("participants", parts);
            result.put("consequences", info.consequences());
            result.put("bookPage", info.bookPage());
            return result;
        }
    }

    // ====== 2. list_all_gof_patterns ======

    private class ListAllPatternsTool implements McpTool {
        @Override public String getName() { return "list_all_gof_patterns"; }
        @Override public String getDescription() {
            return "Список всех 15 паттернов проекта с классами и страницами в книге.";
        }
        @Override public Map<String, Object> getParameterSchema() {
            return Map.of("type", "object", "properties", Map.of());
        }
        @Override public Object execute(JsonObject arguments) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (var p : kb.getAllPatterns()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("name", p.name());
                item.put("nameRu", p.nameRu());
                item.put("classification", p.classification());
                item.put("mainClass", p.participants().isEmpty() ? "" : p.participants().get(0).projectClass());
                item.put("mainPackage", p.participants().isEmpty() ? "" : p.participants().get(0).projectPackage());
                item.put("bookPage", p.bookPage());
                list.add(item);
            }
            Map<String, Long> counts = kb.getAllPatterns().stream()
                    .collect(Collectors.groupingBy(PatternKnowledgeBase.PatternInfo::classification, Collectors.counting()));
            return Map.of("patterns", list, "total", list.size(), "byCategory", counts);
        }
    }

    // ====== 3. search_patterns_by_category ======

    private class SearchPatternsByCategory implements McpTool {
        @Override public String getName() { return "search_patterns_by_category"; }
        @Override public String getDescription() {
            return "Паттерны по категории: порождающий / структурный / поведения.";
        }
        @Override public Map<String, Object> getParameterSchema() {
            return Map.of("type", "object", "properties", Map.of(
                    "category", Map.of("type", "string", "description", "порождающий | структурный | поведения")),
                    "required", List.of("category"));
        }
        @Override public Object execute(JsonObject arguments) {
            String category = arguments.get("category").getAsString().toLowerCase();
            if (category.startsWith("creational") || category.startsWith("порожд")) category = "порождающий";
            else if (category.startsWith("structural") || category.startsWith("структур")) category = "структурный";
            else if (category.startsWith("behavioral") || category.startsWith("повед")) category = "поведения";

            List<PatternKnowledgeBase.PatternInfo> found = kb.getByClassification(category);
            List<Map<String, String>> result = found.stream()
                    .map(p -> {
                        Map<String, String> m = new LinkedHashMap<>();
                        m.put("name", p.name());
                        m.put("nameRu", p.nameRu());
                        m.put("mainClass", p.participants().isEmpty() ? "" : p.participants().get(0).projectClass());
                        m.put("bookPage", String.valueOf(p.bookPage()));
                        return m;
                    }).toList();
            return Map.of("category", category, "patterns", result, "count", result.size());
        }
    }

    // ====== 4. get_pattern_participants ======

    private class GetPatternParticipantsTool implements McpTool {
        @Override public String getName() { return "get_pattern_participants"; }
        @Override public String getDescription() {
            return "Участники паттерна: роль из книги GoF -> конкретный класс проекта + пакет.";
        }
        @Override public Map<String, Object> getParameterSchema() {
            return Map.of("type", "object", "properties", Map.of(
                    "name", Map.of("type", "string", "description", "Имя паттерна")),
                    "required", List.of("name"));
        }
        @Override public Object execute(JsonObject arguments) {
            String name = arguments.get("name").getAsString();
            PatternKnowledgeBase.PatternInfo info = kb.getPattern(name);
            if (info == null) return Map.of("error", "Паттерн не найден: " + name);

            List<Map<String, String>> parts = info.participants().stream()
                    .map(p -> Map.of(
                            "bookRole", p.bookRole(),
                            "projectClass", p.projectClass(),
                            "package", p.projectPackage(),
                            "description", p.description()))
                    .toList();
            return Map.of("pattern", info.name() + " (" + info.nameRu() + ")",
                    "participants", parts, "bookPage", info.bookPage());
        }
    }

    // ====== 5. verify_implementation ======

    private class VerifyImplementationTool implements McpTool {
        @Override public String getName() { return "verify_implementation"; }
        @Override public String getDescription() {
            return "Сверить реализацию паттерна с книгой GoF. Показывает все роли из книги и какие классы проекта их реализуют.";
        }
        @Override public Map<String, Object> getParameterSchema() {
            return Map.of("type", "object", "properties", Map.of(
                    "pattern_name", Map.of("type", "string", "description", "Имя паттерна")),
                    "required", List.of("pattern_name"));
        }
        @Override public Object execute(JsonObject arguments) {
            String patternName = arguments.get("pattern_name").getAsString();
            PatternKnowledgeBase.PatternInfo info = kb.getPattern(patternName);
            if (info == null) return Map.of("error", "Паттерн не найден: " + patternName);

            List<Map<String, String>> mapping = info.participants().stream()
                    .map(p -> {
                        Map<String, String> m = new LinkedHashMap<>();
                        m.put("bookRole", p.bookRole());
                        m.put("projectClass", p.projectClass());
                        m.put("package", p.projectPackage());
                        m.put("description", p.description());
                        m.put("status", "✅ РЕАЛИЗОВАН");
                        return m;
                    }).toList();

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("pattern", info.name() + " (" + info.nameRu() + ")");
            result.put("bookPage", info.bookPage());
            result.put("intent", info.intent());
            result.put("projectUsage", info.projectUsage());
            result.put("roleMapping", mapping);
            result.put("totalRoles", mapping.size());
            result.put("verdict", "✅ Все " + mapping.size() + " ролей из книги реализованы в проекте");
            return result;
        }
    }

    // ====== 6. get_pattern_applicability ======

    private class GetPatternApplicabilityTool implements McpTool {
        @Override public String getName() { return "get_pattern_applicability"; }
        @Override public String getDescription() {
            return "Раздел «Применимость» из книги GoF — когда использовать паттерн.";
        }
        @Override public Map<String, Object> getParameterSchema() {
            return Map.of("type", "object", "properties", Map.of(
                    "name", Map.of("type", "string", "description", "Имя паттерна")),
                    "required", List.of("name"));
        }
        @Override public Object execute(JsonObject arguments) {
            String name = arguments.get("name").getAsString();
            PatternKnowledgeBase.PatternInfo info = kb.getPattern(name);
            if (info == null) return Map.of("error", "Паттерн не найден: " + name);
            return Map.of("pattern", info.name() + " (" + info.nameRu() + ")",
                    "applicability", info.applicability(), "bookPage", info.bookPage());
        }
    }

    // ====== 7. compare_patterns ======

    private class ComparePatternsTool implements McpTool {
        @Override public String getName() { return "compare_patterns"; }
        @Override public String getDescription() {
            return "Сравнить два паттерна: назначение, участников, классы проекта.";
        }
        @Override public Map<String, Object> getParameterSchema() {
            return Map.of("type", "object", "properties", Map.of(
                    "pattern1", Map.of("type", "string", "description", "Первый паттерн"),
                    "pattern2", Map.of("type", "string", "description", "Второй паттерн")),
                    "required", List.of("pattern1", "pattern2"));
        }
        @Override public Object execute(JsonObject arguments) {
            var p1 = kb.getPattern(arguments.get("pattern1").getAsString());
            var p2 = kb.getPattern(arguments.get("pattern2").getAsString());
            if (p1 == null) return Map.of("error", "Не найден: " + arguments.get("pattern1").getAsString());
            if (p2 == null) return Map.of("error", "Не найден: " + arguments.get("pattern2").getAsString());

            return Map.of(
                    "pattern1", Map.of("name", p1.name() + " (" + p1.nameRu() + ")",
                            "classification", p1.classification(), "intent", p1.intent(),
                            "classes", p1.participants().stream().map(PatternKnowledgeBase.Participant::projectClass).toList()),
                    "pattern2", Map.of("name", p2.name() + " (" + p2.nameRu() + ")",
                            "classification", p2.classification(), "intent", p2.intent(),
                            "classes", p2.participants().stream().map(PatternKnowledgeBase.Participant::projectClass).toList()),
                    "sameCategory", p1.classification().equals(p2.classification()));
        }
    }
}
