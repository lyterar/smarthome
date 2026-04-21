package com.smarthome.view.component;

import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Загрузчик 3D моделей из OBJ-файлов.
 *
 * ПРИЧИНА: Внешняя библиотека FXyz3D 0.6.0 недоступна на Maven Central
 *          (Bintray/JCenter закрыт в 2021, репозиторий более не работает).
 * СЛЕДСТВИЕ: Реализован собственный парсер OBJ на базе JavaFX TriangleMesh
 *            — без внешних зависимостей, компилируется и работает везде.
 *
 * GoF паттерн: Factory Method.
 *   ObjModelLoader.load() — фабричный метод, скрывающий создание Group
 *   из OBJ-файла. Вызывающий код (Device3DModel) не знает деталей парсинга.
 *
 * Поддерживаемые форматы граней OBJ:
 *   v x y z          — вершины
 *   vt u v           — текстурные координаты
 *   vn x y z         — нормали (парсируются, не передаются в TriangleMesh)
 *   f v/vt/vn ...    — грани (треугольники и квады через fan triangulation)
 *   f v//vn ...      — грани без текстурных координат
 *   f v ...          — грани только с вершинами
 */
public class ObjModelLoader {

    /**
     * Загружает OBJ-модель из ресурсного пути /models/{modelName}.obj
     *
     * ПРИЧИНА: модели хранятся в ресурсах проекта для переносимости.
     * СЛЕДСТВИЕ: при отсутствии файла возвращается null, и Device3DModel
     *            автоматически использует примитив — без краша приложения.
     *
     * @param modelName  имя файла без расширения (например, "light")
     * @param scale      масштаб модели (1.0 = оригинальный размер)
     * @return           Group с MeshView, или null если файл не найден
     */
    public static Group load(String modelName, double scale) {
        String resourcePath = "/models/" + modelName + ".obj";

        try (InputStream is = ObjModelLoader.class.getResourceAsStream(resourcePath)) {
            if (is == null) {
                // Штатная ситуация: OBJ ещё не добавлен — fallback на примитив
                return null;
            }

            TriangleMesh mesh = parseObj(is, scale);
            if (mesh == null || mesh.getPoints().isEmpty()) {
                System.err.println("[ObjModelLoader] Пустой меш: " + modelName);
                return null;
            }

            MeshView meshView = new MeshView(mesh);
            meshView.setCullFace(CullFace.BACK);

            PhongMaterial material = new PhongMaterial(Color.LIGHTGRAY);
            material.setSpecularColor(Color.WHITE);
            material.setSpecularPower(32);
            meshView.setMaterial(material);

            return new Group(meshView);

        } catch (Exception e) {
            System.err.println("[ObjModelLoader] Ошибка загрузки '" + modelName + "': " + e.getMessage());
            return null;
        }
    }

    /**
     * Загружает OBJ с заданным цветом материала.
     *
     * ПРИЧИНА: цвет устройства зависит от его состояния (вкл/выкл).
     * СЛЕДСТВИЕ: OBJ-модель визуально отражает состояние так же, как примитив.
     *
     * @param modelName  имя файла без расширения
     * @param scale      масштаб
     * @param color      цвет материала
     * @return           Group с MeshView, или null
     */
    public static Group load(String modelName, double scale, Color color) {
        Group group = load(modelName, scale);
        if (group == null) return null;

        // Применяем цвет ко всем MeshView в группе
        group.getChildren().stream()
                .filter(n -> n instanceof MeshView)
                .map(n -> (MeshView) n)
                .forEach(mv -> {
                    PhongMaterial mat = new PhongMaterial(color);
                    mat.setSpecularColor(Color.WHITE);
                    mat.setSpecularPower(32);
                    mv.setMaterial(mat);
                });

        return group;
    }

    // =========================================================
    //  Парсер OBJ
    // =========================================================

    /**
     * Парсит OBJ-поток и строит TriangleMesh.
     *
     * ПРИЧИНА: JavaFX TriangleMesh требует плоские массивы с форматом
     *          (vertexIdx, texCoordIdx) на каждую вершину грани.
     * СЛЕДСТВИЕ: парсер нормализует все варианты записи граней OBJ
     *            в единый формат и выполняет fan-триангуляцию квадов.
     */
    private static TriangleMesh parseObj(InputStream is, double scale) throws Exception {
        List<float[]> verts    = new ArrayList<>(); // v x y z
        List<float[]> texs     = new ArrayList<>(); // vt u v
        // Каждая грань — массив вершин: int[n][2] где [vertIdx, texIdx]
        List<int[][]> faceList = new ArrayList<>();

        // Заглушка с индексом 0 для вершин без текстурных координат
        // (OBJ индексы начинаются с 1, так что 0 = "нет текстуры")
        texs.add(new float[]{0f, 0f});

        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        String line;

        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;

            String[] parts = line.split("\\s+");
            if (parts.length == 0) continue;

            switch (parts[0]) {

                case "v" -> {
                    // Вершина: v x y z [w]
                    if (parts.length >= 4) {
                        verts.add(new float[]{
                            (float)(parseFloat(parts[1]) * scale),
                            (float)(parseFloat(parts[2]) * scale),
                            (float)(parseFloat(parts[3]) * scale)
                        });
                    }
                }

                case "vt" -> {
                    // Текстурные координаты: vt u [v]
                    // JavaFX инвертирует ось V относительно OpenGL
                    float u = parts.length >= 2 ? parseFloat(parts[1]) : 0f;
                    float v = parts.length >= 3 ? 1f - parseFloat(parts[2]) : 0f;
                    texs.add(new float[]{u, v});
                }

                case "vn" -> {
                    // Нормали: парсируем, но TriangleMesh вычисляет их сам
                }

                case "f" -> {
                    int n = parts.length - 1;
                    if (n < 3) break; // некорректная грань

                    int[][] faceVerts = new int[n][2]; // [vertIdx, texIdx]
                    for (int i = 0; i < n; i++) {
                        String[] idx = parts[i + 1].split("/", -1);
                        faceVerts[i][0] = Integer.parseInt(idx[0]) - 1; // vertex (0-based)
                        // texIdx: если не указан — заглушка 0
                        faceVerts[i][1] = (idx.length > 1 && !idx[1].isEmpty())
                                ? Integer.parseInt(idx[1])  // OBJ с 1; заглушка под 0
                                : 0;
                    }

                    // Fan triangulation: (0,1,2), (0,2,3), (0,3,4), ...
                    for (int i = 1; i < n - 1; i++) {
                        faceList.add(new int[][]{faceVerts[0], faceVerts[i], faceVerts[i + 1]});
                    }
                }
            }
        }

        if (verts.isEmpty() || faceList.isEmpty()) return null;

        // --- Сборка TriangleMesh ---

        // Points: [x, y, z, x, y, z, ...]
        float[] pointsArr = new float[verts.size() * 3];
        for (int i = 0; i < verts.size(); i++) {
            pointsArr[i * 3]     = verts.get(i)[0];
            pointsArr[i * 3 + 1] = verts.get(i)[1];
            pointsArr[i * 3 + 2] = verts.get(i)[2];
        }

        // TexCoords: [u, v, u, v, ...]
        float[] texArr = new float[texs.size() * 2];
        for (int i = 0; i < texs.size(); i++) {
            texArr[i * 2]     = texs.get(i)[0];
            texArr[i * 2 + 1] = texs.get(i)[1];
        }

        // Faces: [p0, t0, p1, t1, p2, t2, ...]
        int[] facesArr = new int[faceList.size() * 6];
        for (int i = 0; i < faceList.size(); i++) {
            int[][] f = faceList.get(i);
            facesArr[i * 6]     = f[0][0];
            facesArr[i * 6 + 1] = f[0][1];
            facesArr[i * 6 + 2] = f[1][0];
            facesArr[i * 6 + 3] = f[1][1];
            facesArr[i * 6 + 4] = f[2][0];
            facesArr[i * 6 + 5] = f[2][1];
        }

        TriangleMesh mesh = new TriangleMesh();
        mesh.getPoints().addAll(pointsArr);
        mesh.getTexCoords().addAll(texArr);
        mesh.getFaces().addAll(facesArr);

        return mesh;
    }

    /** Парсит float, поддерживая как точку, так и запятую (локали) */
    private static float parseFloat(String s) {
        return Float.parseFloat(s.replace(',', '.'));
    }
}
