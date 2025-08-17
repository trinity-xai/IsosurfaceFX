package IsosurfaceFX;

import static IsosurfaceFX.MeshUtils.concaveHullFaces;
import static IsosurfaceFX.MeshUtils.medianHullEdgeLength;
import IsosurfaceFX.PointCloudToField.FieldMode;
import com.github.quickhull3d.Point3d;
import com.github.quickhull3d.QuickHull3D;
import javafx.application.Application;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javafx.geometry.Point3D;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Sphere;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;

/**
 *
 * @author Sean Phillips
 */
public class MarchingCubesUIApp extends Application {

    private final Group meshGroup = new Group();
    private final Group pointCloudGroup = new Group();
    private final Group root3D = new Group(meshGroup, pointCloudGroup);

    private double isovalue = 0.0;
    private int pointCount = 150;
    private String shapeType = "sphere";
    private SurfaceMode surfaceMode = SurfaceMode.CARVED_CONCAVE_HULL;

    @Override
    public void start(Stage stage) {
        SubScene subScene = create3DSubScene();
        BorderPane root = new BorderPane(subScene);
        root.setBottom(createControls());

        Scene scene = new Scene(root, 1600, 1000, true);
        //Make everything pretty
        String CSS = StyleResourceProvider.getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(CSS);

        stage.setScene(scene);
        stage.setTitle("Marching Cubes Visualizer");
        stage.show();

    }

    private SubScene create3DSubScene() {
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-300);
        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);

        SubScene subScene = new SubScene(root3D, 1600, 800, true, SceneAntialiasing.BALANCED);
        subScene.setCamera(camera);
        subScene.setFill(Color.web("#202020"));

        addMouseControls(subScene, camera);
        return subScene;
    }

    public TriangleMesh computeCarvedConcaveHull(List<Point3D> pointCloud, double alpha) {
        // 1. Convert to QuickHull3D Point3d[]
        Point3d[] qhPoints = pointCloud.stream()
                .map(p -> new Point3d(p.getX(), p.getY(), p.getZ()))
                .toArray(Point3d[]::new);

        // 2. Compute convex hull
        QuickHull3D hull = new QuickHull3D(qhPoints);

        // 3. Get faces with circumradius ≤ alpha
        Point3d[] H = hull.getVertices();
        int[][] F = hull.getFaces();
        double med = medianHullEdgeLength(H, F);
        double alphaDog = alpha + med;  

        List<int[]> concaveFaces  = concaveHullFaces(hull, alphaDog);

        // 4. Build TriangleMesh for JavaFX
        return MeshUtils.toTriangleMesh(hull.getVertices(), concaveFaces);
    }

    public TriangleMesh computeMarchingCubes(List<Point3D> points, int resolution, 
        double influenceRadius, FieldMode mode) {
        Point3D min = PointCloudUtils.computeBoundingBoxMin(points);
        Point3D max = PointCloudUtils.computeBoundingBoxMax(points);
        double margin = influenceRadius * 2.0;
        Point3D origin = min.subtract(margin, margin, margin);
        double sizeX = max.getX() - min.getX() + 2 * margin;
        double sizeY = max.getY() - min.getY() + 2 * margin;
        double sizeZ = max.getZ() - min.getZ() + 2 * margin;

        VoxelGrid grid = new VoxelGrid.Builder()
                .pointCloud(points)
                .margin(margin)
                .origin(origin)
                .size(sizeX, sizeY, sizeZ)
                .resolution(resolution)
                .build();

        int normalsK = 6;

        Map<Point3D, Point3D> normalMap = PointCloudUtils.estimateNormals(points, normalsK);
        Map<Point3D, Point3D> smoothedNormalMap = PointCloudUtils.smoothNormals(normalMap, points, normalsK);

        PointCloudToField fieldGenerator = new PointCloudToField(
                points, mode, influenceRadius, smoothedNormalMap);
        fieldGenerator.applyTo(grid);

        boolean outwardIncreases =
                (mode == FieldMode.UDF_TSDF) ||
                (mode == FieldMode.VOXEL_SDF) ||
                (mode == FieldMode.SDF) || 
                (mode == FieldMode.MLS_TSDF);

        double iso = (mode == FieldMode.UDF_TSDF) ? 0.0 : isovalue;
        MarchingCubes mc = new MarchingCubes(grid, iso, true, outwardIncreases);
        MarchingCubes.MeshData meshData = mc.generateMeshData();
        TriangleMesh mesh = toTriangleMesh(meshData);
        return mesh;
    }
    
public TriangleMesh computeMarchingTetrahedra(List<Point3D> points, int resolution,
        double influenceRadius, FieldMode mode) {

    // ---- 0) Decide whether to extract a surface subset ----
    boolean volumetric = isLikelyVolumetric(points); // heuristic below

    List<Point3D> usedPts;
    if (volumetric) {
        // keep top 30% “outer shell” points by kNN spacing
        List<Point3D> surfacePts = PointCloudUtils.extractSurfacePoints(points, 12, 0.30);
        // fallback if we got too few points
        if (surfacePts.size() < Math.max(200, points.size() / 10)) {
            usedPts = points; // too sparse → revert
        } else {
            usedPts = surfacePts;
        }
    } else {
        // already a surface cloud (sphere/torus in your UI)
        usedPts = points;
    }

    // ---- 1) Grid bounds from the points we actually use ----
    Point3D min = PointCloudUtils.computeBoundingBoxMin(usedPts);
    Point3D max = PointCloudUtils.computeBoundingBoxMax(usedPts);
    double margin = influenceRadius * 2.0; // wide enough so outside-air is reachable for TSDF
    Point3D origin = min.subtract(margin, margin, margin);
    double sizeX = max.getX() - min.getX() + 2 * margin;
    double sizeY = max.getY() - min.getY() + 2 * margin;
    double sizeZ = max.getZ() - min.getZ() + 2 * margin;

    VoxelGrid grid = new VoxelGrid.Builder()
            .pointCloud(usedPts)   // IMPORTANT: keep builder + field in sync
            .margin(margin)
            .origin(origin)
            .size(sizeX, sizeY, sizeZ)
            .resolution(resolution)
            .build();

    // ---- 2) Normals on the same set of points we used for the field ----
    int normalsK = volumetric ? 16 : 12; // a bit more neighbors if we extracted
    Map<Point3D, Point3D> normalMap = PointCloudUtils.estimateNormals(usedPts, normalsK);
    Map<Point3D, Point3D> smoothedNormalMap = PointCloudUtils.smoothNormals(normalMap, usedPts, normalsK);

    // ---- 3) Build the field from the same set ----
    PointCloudToField fieldGenerator = new PointCloudToField(
            usedPts, mode, influenceRadius, smoothedNormalMap);
    fieldGenerator.applyTo(grid);

    // ---- 4) Iso + orientation policy and extract ----
    boolean outwardIncreases =
            (mode == FieldMode.UDF_TSDF) || (mode == FieldMode.VOXEL_SDF) ||
            (mode == FieldMode.SDF)  || (mode == FieldMode.VOXEL_SDF);
    double iso = (mode == FieldMode.UDF_TSDF) ? 0.0 : isovalue;

    MarchingTetrahedra mt = new MarchingTetrahedra(grid, iso, true, outwardIncreases);
    return mt.generateMesh();
}

/**
 * Heuristic: a volumetric cloud has many points away from the outer shell.
 * We shrink the bounding box by 20% on each side and see how many points
 * lie inside that inner box. If the fraction is big, call it volumetric.
 */
private static boolean isLikelyVolumetric(List<Point3D> pts) {
    if (pts.isEmpty()) return false;
    Point3D min = PointCloudUtils.computeBoundingBoxMin(pts);
    Point3D max = PointCloudUtils.computeBoundingBoxMax(pts);
    double sx = max.getX() - min.getX();
    double sy = max.getY() - min.getY();
    double sz = max.getZ() - min.getZ();

    // shrink 20% per side → inner box is 60% of each dimension
    double shrink = 0.2;
    double ix0 = min.getX() + shrink * sx, ix1 = max.getX() - shrink * sx;
    double iy0 = min.getY() + shrink * sy, iy1 = max.getY() - shrink * sy;
    double iz0 = min.getZ() + shrink * sz, iz1 = max.getZ() - shrink * sz;

    int inside = 0;
    for (Point3D p : pts) {
        if (p.getX() >= ix0 && p.getX() <= ix1 &&
            p.getY() >= iy0 && p.getY() <= iy1 &&
            p.getZ() >= iz0 && p.getZ() <= iz1) {
            inside++;
        }
    }
    double fracInside = inside / (double) pts.size();

    // Tunable threshold: if >30% of points are deep inside, treat as volumetric
    return fracInside > 0.30;
}


private void regenerate(int resolution, double influenceRadius, FieldMode mode) {
    pointCloudGroup.getChildren().clear();
    meshGroup.getChildren().clear();

    List<Point3D> points = PointCloud.generate(pointCount, shapeType);

    TriangleMesh mesh = null;
    switch (surfaceMode) {
        case CARVED_CONCAVE_HULL:
            mesh = computeCarvedConcaveHull(points, influenceRadius);
            break;
        case CONTRACTION_CONCAVE_HULL: 
            mesh = ContractionConcaveHullMini.build(points, influenceRadius);
            break;            
        case MARCHING_TETRAHEDRA:
            mesh = computeMarchingTetrahedra(points, resolution, influenceRadius, mode);
            break;
        case MARCHING_CUBES:
        default:
            mesh = computeMarchingCubes(points, resolution, influenceRadius, mode);
            break;
        // Add DELAUNAY_ALPHA_SHAPE here when ready
    }

    MeshView meshView = new MeshView(mesh);
    meshView.setCullFace(CullFace.BACK);
    meshView.setMaterial(new PhongMaterial(Color.DODGERBLUE.deriveColor(0, 1, 1, 0.1)));
    meshGroup.getChildren().add(meshView);
    
    MeshView linesMeshView = new MeshView(mesh);
    linesMeshView.setCullFace(CullFace.NONE);
    linesMeshView.setDrawMode(DrawMode.LINE);
    linesMeshView.setMaterial(new PhongMaterial(Color.ALICEBLUE));
    meshGroup.getChildren().add(linesMeshView);

    renderPointCloud(points);   
}

    public static TriangleMesh toTriangleMesh(MarchingCubes.MeshData meshData) {
        TriangleMesh mesh = new TriangleMesh();
        float[] points = new float[meshData.vertices.size() * 3];
        for (int i = 0; i < meshData.vertices.size(); i++) {
            Point3D p = meshData.vertices.get(i);
            points[3 * i] = (float) p.getX();
            points[3 * i + 1] = (float) p.getY();
            points[3 * i + 2] = (float) p.getZ();
        }
        mesh.getPoints().setAll(points);
        mesh.getTexCoords().setAll(0, 0); // dummy tex coord
        int[] faces = meshData.faces.stream().mapToInt(Integer::intValue).toArray();
        mesh.getFaces().setAll(faces);
        return mesh;
    }

    private void renderPointCloud(List<Point3D> cloud) {
        for (Point3D pt : cloud) {
            Sphere sphere = new Sphere(0.7);
            sphere.setTranslateX(pt.getX());
            sphere.setTranslateY(pt.getY());
            sphere.setTranslateZ(pt.getZ());
            sphere.setMaterial(new PhongMaterial(Color.LIGHTGREEN));
            pointCloudGroup.getChildren().add(sphere);
        }
    }

    private VBox createControls() {
        double SLIDER_PREF_WIDTH = 550;
        Slider resolutionSlider = new Slider(16, 256, 16);
        Slider influenceRadiusSlider = new Slider(1, 100, 10);
        Slider isoSlider = new Slider(-1, 1, isovalue);
        ComboBox<FieldMode> modeCombo = new ComboBox<>();
        modeCombo.getItems().addAll(FieldMode.values());
        modeCombo.getSelectionModel().selectFirst();

        isoSlider.setShowTickLabels(true);
        isoSlider.setShowTickMarks(true);
        isoSlider.setMajorTickUnit(0.1);
        isoSlider.setPrefWidth(SLIDER_PREF_WIDTH);
        isoSlider.setSnapToTicks(true);
        isoSlider.valueProperty().addListener((obs, old, val) -> {
            isovalue = val.doubleValue();
            regenerate(
                    Double.valueOf(resolutionSlider.getValue()).intValue(),
                    influenceRadiusSlider.getValue(), modeCombo.getValue()
            );
        });
        Slider countSlider = new Slider(100, 3000, pointCount);
        countSlider.setPrefWidth(SLIDER_PREF_WIDTH);
        countSlider.setShowTickLabels(true);
        countSlider.setShowTickMarks(true);
        countSlider.valueProperty().addListener((obs, old, val) -> {
            pointCount = val.intValue();
            regenerate(
                    Double.valueOf(resolutionSlider.getValue()).intValue(),
                    influenceRadiusSlider.getValue(), modeCombo.getValue()
            );
        });

        ComboBox<String> shapeSelector = new ComboBox<>();
        shapeSelector.getItems().addAll("sphere", "cube", "torus", "plus", "hollowbox", "swisssphere");
        shapeSelector.setValue(shapeType);

        shapeSelector.valueProperty().addListener((obs, old, val) -> {
            shapeType = val;
            regenerate(
                    Double.valueOf(resolutionSlider.getValue()).intValue(),
                    influenceRadiusSlider.getValue(), modeCombo.getValue()
            );
        });
        
        ComboBox<SurfaceMode> surfaceModeCombo = new ComboBox<>();
        surfaceModeCombo.getItems().addAll(SurfaceMode.values());
        surfaceModeCombo.setValue(SurfaceMode.MARCHING_CUBES);        

        resolutionSlider.setShowTickLabels(true);
        resolutionSlider.setShowTickMarks(true);
        resolutionSlider.setMajorTickUnit(32);
        resolutionSlider.setBlockIncrement(1);
        resolutionSlider.setSnapToTicks(true);
        resolutionSlider.setPrefWidth(SLIDER_PREF_WIDTH);

        influenceRadiusSlider.setShowTickLabels(true);
        influenceRadiusSlider.setShowTickMarks(true);
        influenceRadiusSlider.setMajorTickUnit(1);
        influenceRadiusSlider.setSnapToTicks(true);
        influenceRadiusSlider.setPrefWidth(SLIDER_PREF_WIDTH);

        modeCombo.setValue(FieldMode.VOXEL_SDF);

        resolutionSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            resolutionSlider.setValue(newVal.intValue());
            regenerate(
                    Double.valueOf(resolutionSlider.getValue()).intValue(),
                    influenceRadiusSlider.getValue(), modeCombo.getValue()
            );
        });

        influenceRadiusSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            regenerate(
                    Double.valueOf(resolutionSlider.getValue()).intValue(),
                    influenceRadiusSlider.getValue(), modeCombo.getValue()
            );
        });

        modeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            regenerate(
                    Double.valueOf(resolutionSlider.getValue()).intValue(),
                    influenceRadiusSlider.getValue(), modeCombo.getValue()
            );
        });
        
        surfaceModeCombo.valueProperty().addListener((obs, oldVal, newVal) -> {
            surfaceMode = newVal;
            regenerate(
                    Double.valueOf(resolutionSlider.getValue()).intValue(),
                    influenceRadiusSlider.getValue(), modeCombo.getValue()
            );
        });
        HBox box = new HBox(15,
                new VBox(5, new Label("Isovalue:"), isoSlider),
                new VBox(5, new Label("Points:"), countSlider),
                new VBox(5, new Label("Shape:"), shapeSelector)
        );
        
        HBox box2 = new HBox(15,
                new VBox(5, new Label("Resolution:"), resolutionSlider),
                new VBox(5, new Label("Influence Radius:"), influenceRadiusSlider),
                new VBox(5, new Label("Surface Mode:"), surfaceModeCombo),
                new VBox(5, new Label("Point Field Mode:"), modeCombo)
        );
        box.setStyle("-fx-padding: 10; -fx-background-color: #303030; -fx-text-fill: white;");
        box2.setStyle("-fx-padding: 10; -fx-background-color: #303030; -fx-text-fill: white;");
        return new VBox(box, box2);
    }

    private void addMouseControls(SubScene scene, Camera camera) {
        Rotate rotateX = new Rotate(0, Rotate.X_AXIS);
        Rotate rotateY = new Rotate(0, Rotate.Y_AXIS);
        root3D.getTransforms().addAll(rotateX, rotateY);

        scene.setOnMouseDragged(e -> {
            rotateY.setAngle(rotateY.getAngle() + e.getSceneX() * 0.005);
            rotateX.setAngle(rotateX.getAngle() - e.getSceneY() * 0.005);
        });

        scene.addEventHandler(ScrollEvent.SCROLL, e -> {
            camera.setTranslateZ(camera.getTranslateZ() + e.getDeltaY());
        });
    }

    public static void main(String[] args) {
        launch();
    }
}
