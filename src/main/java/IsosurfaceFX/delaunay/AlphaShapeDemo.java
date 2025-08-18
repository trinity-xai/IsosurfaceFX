package IsosurfaceFX.delaunay;

/**
 *
 * @author phillsm1
 */
import IsosurfaceFX.StyleResourceProvider;
import javafx.application.Application;
import javafx.beans.property.*;
import javafx.geometry.Point3D;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.DrawMode;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.TriangleMesh;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javafx.scene.paint.PhongMaterial;

public class AlphaShapeDemo extends Application {

    private final IntegerProperty pointCount = new SimpleIntegerProperty(3000);
    private final DoubleProperty alpha = new SimpleDoubleProperty(12.0);
    private final DoubleProperty thickness = new SimpleDoubleProperty(12.0);
    private final DoubleProperty klocal = new SimpleDoubleProperty(12.0);
    private final StringProperty shape = new SimpleStringProperty("torus");
    private final LongProperty seed = new SimpleLongProperty(42L);
    private final BooleanProperty smooth = new SimpleBooleanProperty(false);
    private final BooleanProperty peel = new SimpleBooleanProperty(true);
    private final BooleanProperty visibilityCull = new SimpleBooleanProperty(true);
    
    private final Group root3D = new Group();
    private MeshView meshView;
    private MeshView meshViewLines;
    private Group pointsGroup = new Group();

    @Override public void start(Stage stage) {

PerspectiveCamera cam = new PerspectiveCamera(true);
cam.setNearClip(0.1);
cam.setFarClip(10000);
cam.setTranslateZ(-400);

SubScene sub = new SubScene(root3D, 900, 640, true, SceneAntialiasing.BALANCED);
sub.setFill(Color.gray(0.08));
sub.setCamera(cam);
sub.setFocusTraversable(true);

//// NEW: attach camera controls
//FlyCameraController controls3D = new FlyCameraController(cam, sub);
//controls3D.setSpeed(150);        // units/sec (optional)
//controls3D.setBoostMultiplier(3);// Shift speed multiplier (optional)
//controls3D.setSensitivity(0.2);  // mouse deg/pixel (optional)
ArcballCameraController arc = new ArcballCameraController(root3D, cam, sub);
arc.setYawPitch(30, 20);
arc.setRadius(400);           // starting distance
arc.setSensitivity(0.2);      // mouse deg/pixel
arc.setZoomSpeed(1.0);        // wheel factor
arc.setPanSpeed(1.0);         // pan pixels → world units

        // Controls
        ComboBox<String> shapeBox = new ComboBox<>();
        shapeBox.getItems().addAll("sphere","torus","plus","hollowBox");
        shapeBox.valueProperty().bindBidirectional(shape);

        Slider ptsSlider = new Slider(500, 20000, pointCount.get());
        ptsSlider.valueProperty().addListener((obs, o, v) -> pointCount.set(v.intValue()));
        ptsSlider.setShowTickMarks(true);
        ptsSlider.setShowTickLabels(true);

        Slider alphaSlider = new Slider(1, 50, alpha.get());
        alpha.bind(alphaSlider.valueProperty());
        alphaSlider.setShowTickMarks(true); 
        alphaSlider.setShowTickLabels(true);

        TextField seedField = new TextField(Long.toString(seed.get()));
        seedField.textProperty().addListener((o,ov,nv)->{
            try { seed.set(Long.parseLong(nv)); } catch (NumberFormatException ignore){}
        });

        CheckBox wire = new CheckBox("Wireframe");
        CheckBox showPoints = new CheckBox("Show points");
        Label status = new Label("Ready.");

CheckBox smoothBox = new CheckBox("Smooth (Taubin)");
smoothBox.setSelected(false);        
smooth.bind(smoothBox.selectedProperty());
        
CheckBox peelBox = new CheckBox("Peel");
peelBox.setSelected(true);
peel.bind(peelBox.selectedProperty());

CheckBox visBox  = new CheckBox("Visibility Cull");
visBox.setSelected(true);
visibilityCull.bind(visBox.selectedProperty());

Slider kLocalSlider = new Slider(0.25, 3.0, 1.0);
kLocalSlider.setShowTickMarks(true); 
kLocalSlider.setShowTickLabels(true);
kLocalSlider.setMajorTickUnit(0.5); 
kLocalSlider.setBlockIncrement(0.1);
klocal.bind(kLocalSlider.valueProperty());

// optional: thicknessK (peel strength)
Slider thicknessSlider = new Slider(0.2, 1.2, 0.5);
thicknessSlider.setShowTickMarks(true); 
thicknessSlider.setShowTickLabels(true);
thickness.bind(thicknessSlider.valueProperty());
        
        
        Button buildBtn = new Button("Build α-Shape");
        buildBtn.setOnAction(e -> buildAlphaShape(status, wire.isSelected(), showPoints.isSelected()));

        VBox controls = new VBox(8,
            new HBox(8, new Label("Shape:"), shapeBox),
            new HBox(8, new Label("Points:"), ptsSlider),
            new HBox(8, new Label("α:"), alphaSlider),
            new HBox(8, new Label("Seed:"), seedField),
            new HBox(10, wire, showPoints),
            new HBox(10, smoothBox),
            new HBox(10, new Label("Peel:"), peelBox, new Label("thickK"), thicknessSlider),
            new HBox(10, visBox, new Label("kLocal"), kLocalSlider),                
            buildBtn, status);
        controls.setStyle("-fx-padding:12; -fx-background-color:#1f1f1f; -fx-text-fill: white; "
                        + " -fx-font-size: 12px;");
        for (var n : controls.lookupAll(".label")) ((Label)n).setTextFill(Color.LIGHTGRAY);

        BorderPane root = new BorderPane();
        root.setCenter(sub);    // put the 3D SubScene in the center
        root.setLeft(controls); // controls on the left
        Scene scene = new Scene(root, 1100, 640);
        stage.setScene(scene);
        stage.setTitle("Alpha Shape Demo");
        stage.show();
                //Make everything pretty
        String CSS = StyleResourceProvider.getResource("styles.css").toExternalForm();
        scene.getStylesheets().add(CSS);
        root3D.getChildren().add(new AmbientLight(Color.color(1,1,1)));
    }

    private void buildAlphaShape(Label status, boolean wireframe, boolean showPts) {
        status.setText("Generating points...");
        List<Point3D> pts = generatePoints(shape.get(), pointCount.get(), seed.get());

        // Show points (optional)
        root3D.getChildren().remove(pointsGroup);
        if (showPts) {
            pointsGroup = renderPoints(pts);
            root3D.getChildren().add(pointsGroup);
        }

        // Run α-shape (Delaunay) in background
        status.setText("Building Delaunay + α-shape...");
        long t0 = System.nanoTime();

        // Delaunay + alpha
Delaunay3D dt = new BowyerWatson3D(Predicates3D.tolerant());
var rawTets = dt.tetrahedralize(pts);
// Good, general-purpose defaults for volume point clouds
double DEFAULT_CLUSTER_TOL_SCALE = 0.015;   // ~1.5% of scene diagonal
double DEFAULT_AREA_TOL_SCALE    = 0.0015;  // ~0.15% of diag on (2*area)

TriangleMesh mesh = AlphaShape3D.buildFromTets(
    pts, rawTets, alpha.get(),
    peel.get(), thickness.get(), /*binsAz*/96, /*binsEl*/48,
    visibilityCull.get(), klocal.get(),
    Predicates3D.tolerant(),
        DEFAULT_CLUSTER_TOL_SCALE,  
    DEFAULT_AREA_TOL_SCALE    
);

        long ms = (System.nanoTime() - t0) / 1_000_000;
int tris = mesh.getFaces().size() / 6;
status.setText("Done: P=" + pts.size() + ", Tris=" + tris + ", " + ms + " ms");
System.out.println(status.getText());

        // Display
        if (meshView != null) root3D.getChildren().remove(meshView);
        if (meshViewLines != null) root3D.getChildren().remove(meshViewLines);
                
        meshView = new MeshView(mesh);
        meshView.setCullFace(CullFace.BACK);
        meshView.setDrawMode(wireframe ? DrawMode.LINE : DrawMode.FILL);
        meshView.setMaterial(new javafx.scene.paint.PhongMaterial(Color.DODGERBLUE));

        meshViewLines = new MeshView(mesh);
        meshViewLines.setCullFace(CullFace.BACK);
        meshViewLines.setDrawMode(DrawMode.LINE);
        meshViewLines.setMaterial(new PhongMaterial(Color.WHITE));

// Optional post-pass smoothing
if (smooth.get()) {
    long s0 = System.nanoTime();
    // start simple: uniform weights (faster). Try COTAN if you want crisper results.
    TaubinSmoother.smooth(mesh, 20, 0.33, -0.34, TaubinSmoother.Weights.UNIFORM, true);
    long sms = (System.nanoTime() - s0) / 1_000_000;
    System.out.println("Taubin smoothing: " + sms + " ms");
}        
        
        root3D.getChildren().add(meshView);
        root3D.getChildren().add(meshViewLines);

        status.setText("Done: P=" + pts.size() + ", Tris=" + (mesh.getFaces().size()/6) + ", " + ms + " ms");
    }

    // Simple point-clouds (reuse your PointCloud if you prefer)
    private List<Point3D> generatePoints(String type, int count, long seed) {
        Random rng = new Random(seed);
        switch (type) {
            case "torus": return torus(count, 30, 10, rng);
            case "plus": return plusVolume(count, 60, 6, rng);
            case "hollowBox": return hollowBox(24, 50, 5);
            case "sphere":
            default: return sphereVolume(count, 40, rng);
        }
    }
private static List<Point3D> sphereVolume(int count, double R, Random rng) {
    var pts = new ArrayList<Point3D>(count);
    for (int i=0;i<count;i++){
        double u=rng.nextDouble();
        double r=R*Math.cbrt(u);
        double th=2*Math.PI*rng.nextDouble();
        double ph=Math.acos(2*rng.nextDouble()-1);
        pts.add(new Point3D(
            r*Math.sin(ph)*Math.cos(th),
            r*Math.sin(ph)*Math.sin(th),
            r*Math.cos(ph)
        ));
    }
    return pts;
}
    private static List<Point3D> sphereSurface(int count, double R, Random rng) {
        var pts = new ArrayList<Point3D>(count);
        for (int i=0;i<count;i++){
            double u=rng.nextDouble(), v=rng.nextDouble();
            double th=2*Math.PI*u, ph=Math.acos(2*v-1);
            pts.add(new Point3D(R*Math.sin(ph)*Math.cos(th),
                                R*Math.sin(ph)*Math.sin(th),
                                R*Math.cos(ph)));
        }
        return pts;
    }

    private static List<Point3D> torus(int count, double R, double r, Random rng) {
        var pts = new ArrayList<Point3D>(count);
        for (int i=0;i<count;i++){
            double u=2*Math.PI*rng.nextDouble(), v=2*Math.PI*rng.nextDouble();
            double x=(R + r*Math.cos(v))*Math.cos(u);
            double y=(R + r*Math.cos(v))*Math.sin(u);
            double z= r*Math.sin(v);
            pts.add(new Point3D(x,y,z));
        }
        return pts;
    }

    private static List<Point3D> plusVolume(int count, double len, double rad, Random rng) {
        var pts = new ArrayList<Point3D>(count);
        int per = Math.max(1, count/3);
        sampleCylinder(pts, per, len, rad, 'x', rng);
        sampleCylinder(pts, per, len, rad, 'y', rng);
        sampleCylinder(pts, count - 2*per, len, rad, 'z', rng);
        return pts;
    }
    private static void sampleCylinder(List<Point3D> out, int count, double len, double r, char axis, Random rng){
        for (int i=0;i<count;i++){
            double s = -len/2 + rng.nextDouble()*len;
            double rr = r*Math.sqrt(rng.nextDouble());
            double a  = 2*Math.PI*rng.nextDouble();
            double rx = rr*Math.cos(a), ry = rr*Math.sin(a);
            double x=0,y=0,z=0;
            switch (axis){
                case 'x' -> { x=s; y=rx; z=ry; }
                case 'y' -> { y=s; x=rx; z=ry; }
                default  -> { z=s; x=rx; y=ry; }
            }
            out.add(new Point3D(x,y,z));
        }
    }

    private static List<Point3D> hollowBox(int res, double half, double t) {
        var pts = new ArrayList<Point3D>();
        double h2 = Math.max(0, half-t);
        for (double d : new double[]{+half,-half,+h2,-h2}) {
            // X-constant faces
            for (int i=0;i<=res;i++) for (int j=0;j<=res;j++){
                double u=(double)i/res, v=(double)j/res, H=Math.abs(d);
                pts.add(new Point3D(d, -H+2*H*u, -H+2*H*v));
            }
            // Y-constant faces
            for (int i=0;i<=res;i++) for (int j=0;j<=res;j++){
                double u=(double)i/res, v=(double)j/res, H=Math.abs(d);
                pts.add(new Point3D(-H+2*H*u, d, -H+2*H*v));
            }
            // Z-constant faces
            for (int i=0;i<=res;i++) for (int j=0;j<=res;j++){
                double u=(double)i/res, v=(double)j/res, H=Math.abs(d);
                pts.add(new Point3D(-H+2*H*u, -H+2*H*v, d));
            }
        }
        return pts;
    }

    private Group renderPoints(List<Point3D> pts) {
        // super lightweight: one Sphere per point is too heavy; use tiny TriangleMesh of billboards or skip
        // For now, skip heavy point rendering and rely on the mesh.
        return new Group(); // placeholder
    }

    public static void main(String[] args) { launch(args); }
}
