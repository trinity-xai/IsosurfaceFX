package IsosurfaceFX;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Point3D;
import javafx.scene.AmbientLight;
import javafx.scene.Group;
import javafx.scene.PerspectiveCamera;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.SubScene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.Sphere;
import javafx.stage.Stage;
import java.util.ArrayList;
import java.util.Random;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.shape.VertexFormat;
import org.fxyz3d.shapes.composites.PolyLine3D;

public class App extends Application {

    PerspectiveCamera camera = new PerspectiveCamera(true);
    public Group sceneRoot = new Group();
    public SubScene subScene;
    public CameraTransformer cameraTransform = new CameraTransformer();
    private double cameraDistance = -500;
    private final double sceneWidth = 4000;
    private final double sceneHeight = 4000;

    private double mousePosX;
    private double mousePosY;
    private double mouseOldX;
    private double mouseOldY;
    private double mouseDeltaX;
    private double mouseDeltaY;

    ArrayList<Point3D> positions;
    Random rando = new Random();
    double scale = 100;
    int totalPoints = 10;
    double radius = 1;
    int divisions = 8;

    @Override
    public void start(Stage primaryStage) throws Exception {

        subScene = new SubScene(sceneRoot, sceneWidth, sceneHeight, true, SceneAntialiasing.BALANCED);
        //Start Tracking mouse movements only when a button is pressed
        subScene.setOnMousePressed((MouseEvent me) -> {
            mousePosX = me.getSceneX();
            mousePosY = me.getSceneY();
            mouseOldX = me.getSceneX();
            mouseOldY = me.getSceneY();
        });
        subScene.setOnMouseDragged((MouseEvent me) -> mouseDragCamera(me));
        subScene.setOnScroll((ScrollEvent event) -> {
            double modifier = 2.0;
            double modifierFactor = 0.1;

            if (event.isControlDown()) {
                modifier = 1;
            }
            if (event.isShiftDown()) {
                modifier = 50.0;
            }
            double z = camera.getTranslateZ();
            double newZ = z + event.getDeltaY() * modifierFactor * modifier;
            camera.setTranslateZ(newZ);
        });
        StackPane stackPane = new StackPane(subScene);
        subScene.widthProperty().bind(stackPane.widthProperty());
        subScene.heightProperty().bind(stackPane.heightProperty());
        subScene.setFill(Color.BLACK);

        camera = new PerspectiveCamera(true);
        //setup camera transform for rotational support
        cameraTransform.setTranslate(0, 0, 0);
        cameraTransform.getChildren().add(camera);
        camera.setNearClip(0.1);
        camera.setFarClip(100000.0);
        camera.setTranslateZ(cameraDistance);
        cameraTransform.ry.setAngle(-45.0);
        cameraTransform.rx.setAngle(-10.0);
        subScene.setCamera(camera);

        AmbientLight ambientLight = new AmbientLight(Color.WHITE);
        int pointCount = 100;
        positions = new ArrayList<>(pointCount);

        //create 3D Matrix to hold data
        int width = Double.valueOf(scale).intValue()+1;
        double[][][] dataArr = new double[width][width][width];

        //generate some random positions
        for (int i = 0; i < pointCount; i++) {
            Point3D p3D = new Point3D(
                    rando.nextDouble() * scale,
                    rando.nextDouble() * scale,
                    rando.nextDouble() * scale);
            positions.add(p3D);
            Sphere sphere = new Sphere(radius, divisions);
            sphere.setTranslateX(p3D.getX());
            sphere.setTranslateY(p3D.getY());
            sphere.setTranslateZ(p3D.getZ());
            sceneRoot.getChildren().add(sphere);
    
            //map points into 3D matrix
            dataArr[Double.valueOf(p3D.getX()).intValue()]
                [Double.valueOf(p3D.getY()).intValue()]
                [Double.valueOf(p3D.getZ()).intValue()] = 1.0;    
        }
        

        
        byte leastValue = 0;
        IsoSurfaceDoubleDataMatrix dataMatrix = new IsoSurfaceDoubleDataMatrix(
            IsoSurfaceDataMatrix.XY_PLANE, 
            width, width, width,  //dimension widths
            1.0f, 1.0f, 1.0f, //scaling
            IsoSurfaceDataMatrix.CENTER, 
            leastValue);
        
         // For each of the layers along the y-axis, fill in the XZ plane of
         // data. The first argument is the y-value, the second is a 2-D
         // array
         // of values that are to go at that particular y-value, and the last
         // two are the midpoint values (X and Z respectively) for the plane
         // of data. This allows centering of the data.
         for (int i = 0; i < dataArr.length; i++) {
//             System.out.println("i == " + i);
            dataMatrix.setPlaneData(i, dataArr[i], width / 2, width / 2);
         } // end for        
                
        // This class take in the data and, using the the specified threshold
        // parameters, will figure out which points of data are inside or
        // outside
        // the isosurface.
        final IsoSurfaceGenerator isoGen = new IsoSurfaceGenerator(
        dataMatrix, IsoSurfaceGenerator.GREATER_THAN, 0.0);
        
        System.out.println("Vertices Used: " + isoGen.getVertexArray().length);
        MeshView meshView = new MeshView();
        meshView.setCullFace(CullFace.NONE);
        meshView.setDrawMode(DrawMode.FILL);
        
        TriangleMesh mesh = new TriangleMesh();
        for(Vector3d vector3d : isoGen.getVertexArray()) {
            mesh.getPoints().setAll(
            Double.valueOf(vector3d.x).floatValue(),
            Double.valueOf(vector3d.y).floatValue(),
            Double.valueOf(vector3d.z).floatValue()
            );
        }
        
        mesh.getTexCoords().setAll(0,0);
//        for(Vector3f vector3f : isoGen.getNormalArray()){
//            mesh.getNormals().addAll(vector3f.get());
//        }

        meshView.setMesh(mesh);
//        for(int i=0;i<isoGen.getVertexArray().length-3;i+=3) {  //add each triangle tube segment 
        for(int i=0;i<3;i+=3) {  //add each triangle tube segment 
            //Face Triangle 
            mesh.getFaces().addAll(
            i,0,i+1, 0,i+3,0
            ); 
            mesh.getFaces().addAll(
                i+1,0, i+2, 0, i+3,0
            ); 
        }        

        PhongMaterial mat = new PhongMaterial(Color.CYAN);
        meshView.setMaterial(mat);
        
        Sphere sphereX = new Sphere(5);
        sphereX.setTranslateX(scale);
        sphereX.setMaterial(new PhongMaterial(Color.RED));

        Sphere sphereY = new Sphere(5);
        sphereY.setTranslateY(-scale);
        sphereY.setMaterial(new PhongMaterial(Color.GREEN));

        Sphere sphereZ = new Sphere(5);
        sphereZ.setTranslateZ(scale);
        sphereZ.setMaterial(new PhongMaterial(Color.BLUE));

        sceneRoot.getChildren().addAll(cameraTransform, ambientLight, 
            meshView,sphereX, sphereY, sphereZ);

        subScene.setOnKeyPressed(event -> {
            //What key did the user press?
            KeyCode keycode = event.getCode();

            double change = 10.0;
            //Add shift modifier to simulate "Running Speed"
            if (event.isShiftDown()) {
                change = 100.0;
            }

            //Zoom controls
            if (keycode == KeyCode.W) {
                camera.setTranslateZ(camera.getTranslateZ() + change);
            }
            if (keycode == KeyCode.S) {
                camera.setTranslateZ(camera.getTranslateZ() - change);
            }
            //Strafe controls
            if (keycode == KeyCode.A) {
                camera.setTranslateX(camera.getTranslateX() - change);
            }
            if (keycode == KeyCode.D) {
                camera.setTranslateX(camera.getTranslateX() + change);
            }

            if (keycode == KeyCode.SPACE) {
                camera.setTranslateY(camera.getTranslateY() - change);
            }
            if (keycode == KeyCode.C) {
                camera.setTranslateY(camera.getTranslateY() + change);
            }

            change = event.isShiftDown() ? 10.0 : 1.0;

        });

        BorderPane bpOilSpill = new BorderPane(subScene);
        stackPane.getChildren().clear();
        stackPane.getChildren().addAll(bpOilSpill);
        stackPane.setPadding(new Insets(10));
        stackPane.setBackground(new Background(new BackgroundFill(Color.rgb(255, 255, 255), CornerRadii.EMPTY, Insets.EMPTY)));
        Scene scene = new Scene(stackPane, 1000, 1000);
        scene.setOnMouseEntered(event -> subScene.requestFocus());

        primaryStage.setTitle("Concave Hull Test");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void mouseDragCamera(MouseEvent me) {
        mouseOldX = mousePosX;
        mouseOldY = mousePosY;
        mousePosX = me.getSceneX();
        mousePosY = me.getSceneY();
        mouseDeltaX = (mousePosX - mouseOldX);
        mouseDeltaY = (mousePosY - mouseOldY);
        double modifier = 1.0;
        double modifierFactor = 0.1;

        if (me.isControlDown()) {
            modifier = 0.1;

        }
        if (me.isShiftDown()) {
            modifier = 10.0;
        }
        if (me.isPrimaryButtonDown()) {
            if (me.isAltDown()) { //roll
                cameraTransform.rz.setAngle(
                        ((cameraTransform.rz.getAngle() + mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180); // +
            } else {
                cameraTransform.ry.setAngle(
                        ((cameraTransform.ry.getAngle() + mouseDeltaX * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180); // +
                cameraTransform.rx.setAngle(
                        ((cameraTransform.rx.getAngle() - mouseDeltaY * modifierFactor * modifier * 2.0) % 360 + 540) % 360 - 180);
            }
        } else if (me.isMiddleButtonDown()) {
            cameraTransform.t.setX(cameraTransform.t.getX() + mouseDeltaX * modifierFactor * modifier * 0.3); // -
            cameraTransform.t.setY(cameraTransform.t.getY() + mouseDeltaY * modifierFactor * modifier * 0.3); // -
        }
    }

    public static void main(String[] args) {
        Application.launch(App.class, args);
    }
}
