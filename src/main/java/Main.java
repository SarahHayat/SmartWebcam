import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.tensorflow.Tensor;
import javafx.application.Application;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import org.bytedeco.javacv.Frame;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main extends Application {

    private Property props = new Property();
    private final Java2DFrameConverter java2DFrameConverter = new Java2DFrameConverter();
    final int INTERVALCAM = 1;///you may use interval
    private final String BEAGLE_RED = "Beagle/Red";
    private final String TIGERCAT_BLUE = "Tiger Cat/Blue";
    private final String MOUSE_GREEN = "Mouse/Green";
    private final String NONE = "none";

    private BufferedImage originalImage;
    private BufferedImage redImage;
    private BufferedImage greenImage;
    private BufferedImage blueImage;
    private OpenCVFrameGrabber opengrabber = new OpenCVFrameGrabber(0);
    private Object value;
    private GridPane root = new GridPane();
    private RowConstraints rc = new RowConstraints();
    private ColumnConstraints cc = new ColumnConstraints();
    private VBox boxCamera = new VBox(5);
    private Button buttonSave = new Button("Sauvegarder");
    private Button buttonUpload = new Button("Importer");
    private Button buttonFolder = new Button("Choix du dossier");
    private Button buttonProcess = new Button("Process");
    private Button open = new Button("Caméra");
    private Label pathLabel = new Label();
    private TextField definitionTf = new TextField();
    private TextField descriptionTf = new TextField();
    private TextField percentTf = new TextField();
    private TextField probaTf = new TextField();
    private Label definitionLabel = new Label("Definition");
    private Label percentLabel = new Label("Pourcentage");
    private ImageView imageViewCam = new ImageView();
    private ImageView imageViewPicture = new ImageView();
    private ComboBox comboBox = new ComboBox();
    private ObservableList<String> liste = FXCollections.observableArrayList();

    public static void main(String[] argv) throws IOException {
        launch(argv);
    }

    public static Property getProperty(Property props) throws IOException {
        TFUtils utils = new TFUtils();
        byte[] data;
        if (props.getPath() != null) {
            data = Files.readAllBytes(props.getPath());
        } else {
            data = Files.readAllBytes(Paths.get("src/main/resources/tensorPics/jack.jpg"));
        }
        String filePath = "src/inception5h/tensorflow_inception_graph.pb";
        List<String> labels = Files.readAllLines(Paths.get("src/inception5h/labels.txt"));
        byte[] pb = Files.readAllBytes(Paths.get(filePath));
        Tensor<Float> tensor = utils.byteBufferToTensor(data);
        Tensor<Float> result = utils.executeModelFromByteArray(pb, tensor);
        Description description = new Description();
        int index = description.getIndex(result, props);
        String labelText = labels.get(index);
        props.setDescription(labelText);
        return props;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Smart Webcam");
        liste.addAll(BEAGLE_RED, TIGERCAT_BLUE, MOUSE_GREEN, NONE);

        comboBox.setValue("Filtres");
        comboBox.setItems(liste);
        buttonSave.setDisable(true);
        buttonFolder.setDisable(true);
        descriptionTf.setEditable(false);
        descriptionTf.setDisable(true);
        percentTf.setEditable(false);
        percentTf.setDisable(true);

        comboBox.setOnAction((action -> {
            value = comboBox.getSelectionModel().getSelectedItem();
        }));

        open.setOnAction((action -> {
            openCam();
        }));

        buttonFolder.setOnAction((action -> {
            chooseFolder(props, primaryStage);
            if (props.getFile() != null) {
                pathLabel.setText(props.getFile().getPath());
            }

        }));

        buttonSave.setOnAction((action -> {
            try {
                save(props);
                buttonSave.setDisable(true);
                Thread thread = new Thread(()->{
                    try {
                        Thread.sleep(2000);
                        Platform.runLater(()->{
                            buttonSave.setDisable(false);
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                thread.start();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }));

        buttonUpload.setOnAction((action -> {
            try {
                opengrabber.stop();
                upload(props, root);
            } catch (FrameGrabber.Exception e) {
                e.printStackTrace();
            }
        }));

        EventHandler<ActionEvent> event = (ActionEvent e) -> {
            process();
        };
        buttonProcess.setOnAction(event);
        addToRoot();

        primaryStage.setScene(new Scene(root, 1500, 1000));
        primaryStage.show();
    }

    private WritableImage frameToImage(Frame frame, String value) throws IOException {
        BufferedImage bufferedImage = java2DFrameConverter.getBufferedImage(frame);
        bufferedImage = setFilter(value, bufferedImage);
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }

    private BufferedImage createColorImage(BufferedImage originalImage, int mask) {
        BufferedImage colorImage = new BufferedImage(originalImage.getWidth(),
                originalImage.getHeight(), originalImage.getType());

        for (int x = 0; x < originalImage.getWidth(); x++) {
            for (int y = 0; y < originalImage.getHeight(); y++) {
                int pixel = originalImage.getRGB(x, y) & mask;
                colorImage.setRGB(x, y, pixel);
            }
        }

        return colorImage;
    }

    private BufferedImage getOriginalImage() {
        return originalImage;
    }

    private void setOriginalImage(BufferedImage originalImage) {
        this.originalImage = originalImage;
        this.redImage = createColorImage(originalImage, 0xFFFF0000);
        this.greenImage = createColorImage(originalImage, 0xFF00FF00);
        this.blueImage = createColorImage(originalImage, 0xFF0000FF);
    }

    private BufferedImage getRedImage() {
        return redImage;
    }

    private BufferedImage getGreenImage() {
        return greenImage;
    }

    private BufferedImage getBlueImage() {
        return blueImage;
    }

    private void openCam() {

        try {
            definitionTf.setText(null);
            probaTf.setText(null);
            props.setPath(null);
            props.setFile(null);
            imageViewPicture.setImage(null);
            opengrabber.start();
        } catch (FrameGrabber.Exception e) {
            e.printStackTrace();
        }
        Executors.newSingleThreadExecutor().execute(new Runnable() {
            @Override
            public void run() {
                Frame frame = null;
                new File("images").mkdir();
                OpenCVFrameConverter.ToIplImage converter = new OpenCVFrameConverter.ToIplImage();
                IplImage img;
                int i = 0;
                try {
                    while (true) {
                        frame = opengrabber.grabFrame();
                        BufferedImage bufferedImage = java2DFrameConverter.getBufferedImage(frame);
                        imageViewCam.setImage(frameToImage(frame, value.toString()));
                        Thread.sleep(INTERVALCAM);
                        bufferedImage = setFilter(value.toString(), bufferedImage);
                        props.setBufferedImage(bufferedImage);
                        props.setPath(Paths.get("images" + File.separator + "picture" + i + ".jpg"));
                        save(props);
                        getProperty(props);
                        if (props.getDescription() != null) {
                            System.out.println(i + " : " + props.getDescription());
                            buttonSave.setDisable(false);
                            buttonFolder.setDisable(false);
                            descriptionTf.setText(props.getDescription());
                            percentTf.setText(String.valueOf(props.getProba()));
                        } else {
                            buttonSave.setDisable(true);
                            buttonFolder.setDisable(true);
                            descriptionTf.setText("");
                            percentTf.setText("");

                        }
                        i++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void addToRoot() {
        root.setPadding(new Insets(20));
//        root.setGridLinesVisible( true );
        root.getColumnConstraints().addAll( new ColumnConstraints( 200 ), new ColumnConstraints( 200 ), new ColumnConstraints( 200 ), new ColumnConstraints( 200 ) );
        root.setVgap(15);
        root.setHgap(15);

        int ROW_0 = 0;
        int ROW_1 = 1;
        int ROW_2 = 2;
        int ROW_3 = 3;
        int ROW_4 = 4;

        int COL_0 = 0;
        int COL_1 = 1;
        int COL_2 = 2;
        int COL_3 = 3;

        // ROW 0
        root.getChildren().add(buttonUpload);
        GridPane.setConstraints(buttonUpload, COL_0, ROW_0, 1, 1);
        buttonUpload.setPadding(new Insets(10));

        root.getChildren().add(buttonFolder);
        GridPane.setConstraints(buttonFolder, COL_1, ROW_0, 1, 1);
        buttonFolder.setPadding(new Insets(10));

        root.getChildren().add(buttonSave);
        GridPane.setConstraints(buttonSave, COL_2, ROW_0, 1, 1);
        buttonSave.setPadding(new Insets(10));

        root.getChildren().add(pathLabel);
        GridPane.setConstraints(pathLabel, COL_3, ROW_0, 1, 1);
        pathLabel.setPadding(new Insets(10));

        // ROW 1
        root.getChildren().add(open);
        GridPane.setConstraints(open, COL_0, ROW_1, 1, 1);
        open.setPadding(new Insets(10));

        root.getChildren().add(comboBox);
        GridPane.setConstraints(comboBox, COL_1, ROW_1, 1, 1);
        comboBox.setPadding(new Insets(10));

        // ROW 2
        root.getChildren().add(definitionLabel);
        GridPane.setConstraints(definitionLabel, COL_0, ROW_2, 1, 1);
        definitionLabel.setPadding(new Insets(10));

        root.getChildren().add(definitionTf);
        GridPane.setConstraints(definitionTf, COL_1, ROW_2, 1, 1);
        definitionTf.setPadding(new Insets(10));


        root.getChildren().add(descriptionTf);
        GridPane.setConstraints(descriptionTf, COL_2, ROW_2, 1, 1);
        descriptionTf.setPadding(new Insets(10));

        // ROW 3
        root.getChildren().add(percentLabel);
        GridPane.setConstraints(percentLabel, COL_0, ROW_3, 1, 1);
        percentLabel.setPadding(new Insets(10));

        root.getChildren().add(probaTf);
        GridPane.setConstraints(probaTf, COL_1, ROW_3, 1, 1);
        probaTf.setPadding(new Insets(10));

        root.getChildren().add(percentTf);
        GridPane.setConstraints(percentTf, COL_2, ROW_3, 1, 1);
        percentTf.setPadding(new Insets(10));

        // ROW 4
        root.getChildren().add(buttonProcess);
        GridPane.setConstraints(buttonProcess, COL_0, ROW_4, 1, 1);
        buttonProcess.setPadding(new Insets(10));

        // ROW 5
        imageViewCam.setFitWidth(400);
        imageViewCam.setFitHeight(400);
        root.getChildren().add(imageViewCam);
        GridPane.setConstraints(imageViewCam, 4, 5, 1, 1);

        imageViewPicture.setFitHeight(400);
        imageViewPicture.setFitWidth(400);
        root.getChildren().add(imageViewPicture);
        GridPane.setConstraints(imageViewPicture, 4, 5, 1, 1);

    }

    private void save(Property property) throws IOException {
        try {
            if (property.getFile() == null) {
                ImageIO.write(property.getBufferedImage(), "jpg", property.getPath().toFile());
            } else {
                ImageIO.write(property.getBufferedImage(), "jpg", property.getFile());
                System.out.println("GET FILE  " + property.getFile());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void process() {
        if (props.getDescription() != null) {
            if (definitionTf.getText().contains(props.getDescription()) && props.getProba() >= Float.parseFloat(probaTf.getText())) {
                descriptionTf.setText(props.getDescription());
                percentTf.setText(String.valueOf(props.getProba()));
                buttonSave.setDisable(false);
                buttonFolder.setDisable(false);
            } else {
                buttonSave.setDisable(true);
                buttonFolder.setDisable(true);
                descriptionTf.setText("");
                percentTf.setText("");
            }
        }
    }

    private void upload(Property props, GridPane root) {
//        boxCamera.getChildren().clear();
        imageViewCam.setImage(null);
        descriptionTf.setText("");
        percentTf.setText("");
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(null);
        try {
            BufferedImage bufferedImage = ImageIO.read(file);
            WritableImage image1 = SwingFXUtils.toFXImage(bufferedImage, null);
            imageViewPicture.setImage(image1);
            props.setPath(file.toPath());
            props.setBufferedImage(bufferedImage);
            getProperty(props);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void chooseFolder(Property props, Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("JPG files (*.jpg)", "*.jpg");
        fileChooser.getExtensionFilters().add(extFilter);
        File file = fileChooser.showSaveDialog(primaryStage);
        props.setFile(file);

    }

    private BufferedImage setFilter(String value, BufferedImage bufferedImage) throws IOException {
        setOriginalImage(bufferedImage);
        if (value == null) {
            bufferedImage = bufferedImage;
        } else if (props.getDescription() != null){
            if(props.getDescription().equals("beagle") && value.equals(BEAGLE_RED)){
                System.out.println("APPLY RED FILTER");
                bufferedImage = getRedImage();
            }
            else if(props.getDescription().equals("tiger cat") && value.equals(TIGERCAT_BLUE)){
                System.out.println("APPLY BLUE FILTER");
                bufferedImage = getBlueImage();
            }
            else if(props.getDescription().equals("mouse") && value.equals(MOUSE_GREEN)) {
                System.out.println("APPLY GREEN FILTER");
                bufferedImage = getGreenImage();
            }
            else {
                System.out.println("APPLY NO FILTER");
                bufferedImage = bufferedImage;
            }
        }
        return bufferedImage;
    }

}
