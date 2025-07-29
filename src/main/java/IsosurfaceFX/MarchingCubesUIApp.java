package IsosurfaceFX;

import javafx.application.Application;
import javafx.geometry.Point3D;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import javafx.scene.Camera;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
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

    private double isovalue = 0.5;
    private int pointCount = 500;
    private String shapeType = "sphere";

    @Override
    public void start(Stage stage) {
        SubScene subScene = create3DSubScene();
        BorderPane root = new BorderPane(subScene);
        root.setBottom(createControls());

        Scene scene = new Scene(root, 800, 1000, true);
        //Make everything pretty
        String CSS = StyleResourceProvider.getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(CSS);
        
        stage.setScene(scene);
        stage.setTitle("Marching Cubes Visualizer");
        stage.show();

        regenerate();
    }

    private SubScene create3DSubScene() {
        PerspectiveCamera camera = new PerspectiveCamera(true);
        camera.setTranslateZ(-300);
        camera.setNearClip(0.1);
        camera.setFarClip(10000.0);

        SubScene subScene = new SubScene(root3D, 800, 800, true, SceneAntialiasing.BALANCED);
        subScene.setCamera(camera);
        subScene.setFill(Color.web("#202020"));

        addMouseControls(subScene, camera);
        return subScene;
    }

    private void regenerate() {
        pointCloudGroup.getChildren().clear();
        meshGroup.getChildren().clear();

        List<Point3D> cloud = generatePointCloud(pointCount, shapeType);
        renderPointCloud(cloud);

        VoxelGrid grid = PointCloudToField.builder()
                .withPoints(cloud)
                .withVoxelSize(4.0)
                .withGaussianRadius(6.0)
                .build();

        MarchingCubes mc = new MarchingCubes(grid, isovalue, true);
        List<Triangle3D> triangles = mc.generateMesh();

        TriangleMesh mesh = TriangleMeshConverter.convertToMesh(triangles);
        MeshView meshView = new MeshView(mesh);
        meshView.setMaterial(new PhongMaterial(Color.DODGERBLUE));
        meshGroup.getChildren().add(meshView);
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

    private HBox createControls() {
         Slider isoSlider = new Slider(0, 1, isovalue);
        isoSlider.setShowTickLabels(true);
        isoSlider.setShowTickMarks(true);
        isoSlider.valueProperty().addListener((obs, old, val) -> {
            isovalue = val.doubleValue();
            regenerate();
        });
        Slider countSlider = new Slider(100, 3000, pointCount);
        countSlider.setShowTickLabels(true);
        countSlider.setShowTickMarks(true);
        countSlider.valueProperty().addListener((obs, old, val) -> {
            pointCount = val.intValue();
            regenerate();
        });

        ComboBox<String> shapeSelector = new ComboBox<>();
        shapeSelector.getItems().addAll("sphere", "cube", "torus");
        shapeSelector.setValue(shapeType);
        shapeSelector.valueProperty().addListener((obs, old, val) -> {
            shapeType = val;
            regenerate();
        });

        HBox box = new HBox(15, new Label("Isovalue:"), isoSlider,
                new Label("Points:"), countSlider,
                new Label("Shape:"), shapeSelector);
        box.setStyle("-fx-padding: 10; -fx-background-color: #303030; -fx-text-fill: white;");
        return box;
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

    private List<Point3D> generatePointCloud(int count, String shape) {
        List<Point3D> points = new ArrayList<>();
        switch (shape) {
            case "cube":
                for (int i = 0; i < count; i++) {
                    points.add(new Point3D(
                            (Math.random() - 0.5) * 100,
                            (Math.random() - 0.5) * 100,
                            (Math.random() - 0.5) * 100
                    ));
                }
                break;
            case "torus":
                for (int i = 0; i < count; i++) {
                    double u = Math.random() * 2 * Math.PI;
                    double v = Math.random() * 2 * Math.PI;
                    double R = 30;
                    double r = 10;
                    double x = (R + r * Math.cos(v)) * Math.cos(u);
                    double y = (R + r * Math.cos(v)) * Math.sin(u);
                    double z = r * Math.sin(v);
                    points.add(new Point3D(x, y, z));
                }
                break;
            case "sphere":
            default:
                for (int i = 0; i < count; i++) {
                    double theta = Math.random() * 2 * Math.PI;
                    double phi = Math.acos(2 * Math.random() - 1);
                    double r = 40;
                    double x = r * Math.sin(phi) * Math.cos(theta);
                    double y = r * Math.sin(phi) * Math.sin(theta);
                    double z = r * Math.cos(phi);
                    points.add(new Point3D(x, y, z));
                }
                break;
        }
        return points;
    }

    public static void main(String[] args) {
        launch();
    }
}
