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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
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
    final int INTERVALCAM = 1000;///you may use interval
    private final String RED = "red";
    private final String BLUE = "blue";
    private final String GREEN = "green";
    private final String NONE = "none";

    private BufferedImage originalImage;
    private BufferedImage redImage;
    private BufferedImage greenImage;
    private BufferedImage blueImage;
    private OpenCVFrameGrabber opengrabber = new OpenCVFrameGrabber(0);
    private Object value;
    private BorderPane root = new BorderPane();
    private VBox boxPicture = new VBox(5);
    private VBox boxDescLabels = new VBox(5);
    private VBox boxProperties = new VBox(5);
    private VBox boxCamera = new VBox(5);
    private Button buttonSave = new Button();
    private Button buttonUpload = new Button();
    private Button buttonFolder = new Button();
    private Button buttonProcess = new Button();
    private Button open = new Button();
    private TextField definitionTf = new TextField();
    private Label pathLabel = new Label();
    private TextField descriptionTf = new TextField();
    private TextField percentTf = new TextField();
    private Label definitionLabel = new Label("Definition");
    private Label PercentLabel = new Label("Pourcentage");
    private TextField ProbaTf = new TextField();
    private ComboBox comboBox = new ComboBox();
    private ObservableList<String> liste = FXCollections.observableArrayList();

    public static void main(String[] argv) throws IOException {
        launch(argv);
    }

    public static Property getProperty(Property props) throws IOException {
        TFUtils utils = new TFUtils();
        byte[] data;
        if (props.getPath() != null) {
            System.out.println("LE PAAAAAATHHHH " + props.getPath());
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
        liste.addAll(RED, BLUE, GREEN, NONE);

        comboBox.setValue("Filtres");
        comboBox.setItems(liste);

        buttonSave.setText("Sauvegarder");
        buttonSave.setDisable(true);

        buttonUpload.setText("Importer");

        buttonFolder.setText("Choix du dossier");
        buttonFolder.setDisable(true);

        buttonProcess.setText("Process");

        open.setText("Caméra");

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

        primaryStage.setScene(new Scene(root, 1000, 600));
        primaryStage.show();
    }

    private WritableImage frameToImage(Frame frame, Object value) throws IOException {
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

    private void openCam(){
        ImageView imageView = new ImageView();
        try {
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
                        imageView.setImage(frameToImage(frame, value));
                        Thread.sleep(INTERVALCAM);
                        bufferedImage = setFilter(value, bufferedImage);
                        props.setBufferedImage(bufferedImage);
                        props.setPath(Paths.get("images" + File.separator + "picture"+i +".jpg"));
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
//                    descriptionLabel.setText(props.getDescription());
//                    percentLabel.setText(String.valueOf(props.getProba()));
            }
        });
        imageView.setFitWidth(400);
        imageView.setFitHeight(400);
//            boxCamera.setMaxWidth(400);
//            boxCamera.setMaxHeight(400);
        boxCamera.getChildren().add(imageView);
        root.setCenter(boxCamera);
    }

    private void addToRoot(){
        boxPicture.getChildren().addAll(buttonFolder,buttonUpload,open ,buttonSave, pathLabel);
        boxPicture.setPadding(new Insets(40, 5, 5, 50));

        boxDescLabels.getChildren().addAll(definitionLabel, definitionTf, PercentLabel, ProbaTf, buttonProcess);
        boxDescLabels.setPadding(new Insets(5, 5, 5, 50));

        boxProperties.getChildren().addAll(definitionLabel, descriptionTf, PercentLabel, percentTf);
        boxProperties.setPadding(new Insets(5, 5, 5, 50));

        root.setBottom(boxPicture);
        root.setLeft(boxDescLabels);
        root.setRight(boxProperties);
        root.setCenter(boxCamera);
        root.setTop(comboBox);
    }

    private void save(Property property) throws IOException {
        try {
            if(property.getFile() == null){
                ImageIO.write(property.getBufferedImage() , "jpg", property.getPath().toFile());
            }
            else {
                ImageIO.write(property.getBufferedImage(), "jpg", property.getFile());
                System.out.println("GET FILE  " + property.getFile());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void process(){
        if (props.getDescription() != null) {
            if (definitionTf.getText().contains(props.getDescription()) && props.getProba() >= Float.parseFloat(ProbaTf.getText())) {
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

    private void upload(Property props, BorderPane root) {
        boxCamera.getChildren().clear();
        descriptionTf.setText("");
        percentTf.setText("");
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(null);
        try {
            BufferedImage bufferedImage = ImageIO.read(file);
            WritableImage image1 = SwingFXUtils.toFXImage(bufferedImage, null);
            ImageView imageView = new ImageView();
            imageView.setImage(image1);
            imageView.setFitHeight(300);
            imageView.setFitWidth(300);
            root.setCenter(imageView);
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

    private BufferedImage setFilter(Object value, BufferedImage bufferedImage){
        setOriginalImage(bufferedImage);
        if(value == null){
            bufferedImage = bufferedImage;
        }
        else{
            switch (value.toString()){
                case RED:
                    System.out.println("APPLY RED FILTER");
                    bufferedImage = getRedImage();
                    break;
                case BLUE:
                    System.out.println("APPLY BLUE FILTER");
                    bufferedImage = getBlueImage();
                    break;
                case GREEN:
                    System.out.println("APPLY GREEN FILTER");
                    bufferedImage = getGreenImage();
                    break;
                default:
                    System.out.println("APPLY NO FILTER");
                    bufferedImage = bufferedImage;
                    break;
            }
        }
        return bufferedImage;
    }

}
