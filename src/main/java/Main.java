import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.tensorflow.Tensor;

import javafx.application.Application;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import org.bytedeco.javacv.Frame;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Executors;

import static org.bytedeco.opencv.helper.opencv_imgcodecs.cvSaveImage;

import javax.imageio.ImageIO;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class Main extends Application {

    private Property props = new Property();
    private final Java2DFrameConverter java2DFrameConverter = new Java2DFrameConverter();
    final int INTERVALCAM = 1000;///you may use interval

    private BufferedImage originalImage;
    private BufferedImage redImage;
    private BufferedImage greenImage;
    private BufferedImage blueImage;

    private Object value;

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

        GridPane root = new GridPane();

        VBox boxPicture = new VBox(5);

        VBox boxDescLabels = new VBox(5);

        VBox boxProperties = new VBox(5);

        VBox boxCamera = new VBox(5);

        ObservableList<String> liste = FXCollections.observableArrayList();
        liste.add("beagle/red");
        liste.add("mouse/blue");
        liste.add("daisy/green");

        ComboBox comboBox = new ComboBox();
        comboBox.setValue("Filtres");
        comboBox.setItems(liste);


        Button buttonSave = new Button();
        buttonSave.setText("Sauvegarder");
        buttonSave.setDisable(true);

        Button buttonUpload = new Button();
        buttonUpload.setText("Importer");

        Button buttonFolder = new Button();
        buttonFolder.setText("Choix du dossier");
        buttonFolder.setDisable(true);

        Button buttonProcess = new Button();
        buttonProcess.setText("Process");

        Button open = new Button();
        open.setText("Cam");

        TextField definitionLabel = new TextField();

        Label pathLabel = new Label();

        TextField descriptionLabel = new TextField();
        descriptionLabel.setEditable(false);
        descriptionLabel.setDisable(true);

        TextField percentLabel = new TextField();
        percentLabel.setEditable(false);
        percentLabel.setDisable(true);

        TextField tfProba = new TextField();

        Label label1 = new Label("Definition");
        Label label2 = new Label("Pourcentage");

        comboBox.setOnAction((action -> {
            value = comboBox.getSelectionModel().getSelectedItem();
            System.out.println("VALUEEE = " + value);

        }));

        open.setOnAction((action -> {
            ImageView imageView = new ImageView();
            OpenCVFrameGrabber opengrabber = new OpenCVFrameGrabber(0);
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
                            img = converter.convert(frame);
                            //save
//                            cvSaveImage("images" + File.separator + (i++) + "-aa.jpg", img);
                            BufferedImage bufferedImage = java2DFrameConverter.getBufferedImage(frame);
                            imageView.setImage(frameToImage(frame, value));

                            Thread.sleep(INTERVALCAM);
                            bufferedImage = setFilter(value, bufferedImage);

//                            cvSaveImage("images" + File.separator + "picture"+i +".jpg", img);

                            props.setBufferedImage(bufferedImage);
                            props.setPath(Paths.get("images" + File.separator + "picture"+i +".jpg"));
                            save(props);
                            getProperty(props);

                            if (props.getDescription() != null) {
                                System.out.println(i + " : " + props.getDescription());
                                buttonSave.setDisable(false);
                                buttonFolder.setDisable(false);
                                descriptionLabel.setText(props.getDescription());
                                percentLabel.setText(String.valueOf(props.getProba()));

                            } else {
                                buttonSave.setDisable(true);
                                buttonFolder.setDisable(true);
                                descriptionLabel.setText("");
                                percentLabel.setText("");

                            }
                            i++;
                        }


                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    descriptionLabel.setText(props.getDescription());
                    percentLabel.setText(String.valueOf(props.getProba()));
                }
            });
            imageView.setFitWidth(200);
            imageView.setFitHeight(200);
            boxCamera.setMaxWidth(200);
            boxCamera.setMaxHeight(200);
            boxCamera.getChildren().add(imageView);
            GridPane.setConstraints(boxCamera, 0, 4);
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
            upload(props, boxPicture);
        }));

        EventHandler<ActionEvent> event = (ActionEvent e) -> {
            System.out.println("LA DESCRIPTIOOOOOOOON " + props.getDescription());
            if (props.getDescription() != null) {
                if (definitionLabel.getText().contains(props.getDescription()) && props.getProba() >= Float.parseFloat(tfProba.getText())) {
                    descriptionLabel.setText(props.getDescription());
                    percentLabel.setText(String.valueOf(props.getProba()));
                    buttonSave.setDisable(false);
                    buttonFolder.setDisable(false);
                } else {
                    buttonSave.setDisable(true);
                    buttonFolder.setDisable(true);
                    descriptionLabel.setText("");
                    percentLabel.setText("");
                }
            }
        };


        buttonProcess.setOnAction(event);

        boxPicture.getChildren().addAll(buttonFolder, buttonSave, pathLabel);
        boxPicture.setPadding(new Insets(5, 5, 5, 50));

        boxDescLabels.getChildren().addAll( label1, definitionLabel, label2, tfProba, buttonProcess);
        boxDescLabels.setPadding(new Insets(5, 5, 5, 50));

        boxProperties.getChildren().addAll(label1, descriptionLabel, label2, percentLabel);
        boxProperties.setPadding(new Insets(5, 5, 5, 50));

        root.getChildren().add(buttonUpload);
        GridPane.setConstraints(buttonUpload, 4, 5);

        root.getChildren().add(open);
        GridPane.setConstraints(open, 4, 4);

        root.getChildren().add(boxPicture);
        GridPane.setConstraints(boxPicture, 0, 0);

        root.getChildren().add(boxDescLabels);
        GridPane.setConstraints(boxDescLabels, 0, 1);

        root.getChildren().add(boxProperties);
        GridPane.setConstraints(boxProperties, 0, 3);

        root.getChildren().add(boxCamera);

        root.getChildren().add(comboBox);
        GridPane.setConstraints(comboBox, 1, 0);


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

    public BufferedImage getOriginalImage() {
        return originalImage;
    }

    public void setOriginalImage(BufferedImage originalImage) {
        this.originalImage = originalImage;
        this.redImage = createColorImage(originalImage, 0xFFFF0000);
        this.greenImage = createColorImage(originalImage, 0xFF00FF00);
        this.blueImage = createColorImage(originalImage, 0xFF0000FF);
    }

    public BufferedImage getRedImage() {
        return redImage;
    }

    public BufferedImage getGreenImage() {
        return greenImage;
    }

    public BufferedImage getBlueImage() {
        return blueImage;
    }

    private static void save(Property property) throws IOException {
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

    public static void upload(Property props, VBox picture) {
        FileChooser fileChooser = new FileChooser();
        File file = fileChooser.showOpenDialog(null);
        try {
            BufferedImage bufferedImage = ImageIO.read(file);
            WritableImage image1 = SwingFXUtils.toFXImage(bufferedImage, null);
            ImageView imageView = new ImageView();
            imageView.setImage(image1);
            imageView.setFitHeight(300);
            imageView.setFitWidth(300);
            imageView.setStyle("-fx-background-color: RED");
            picture.getChildren().add(imageView);
            props.setPath(file.toPath());
            props.setBufferedImage(bufferedImage);
            getProperty(props);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void chooseFolder(Property props, Stage primaryStage) {
        FileChooser fileChooser = new FileChooser();
        FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("JPG files (*.jpg)", "*.jpg");
        fileChooser.getExtensionFilters().add(extFilter);
        File file = fileChooser.showSaveDialog(primaryStage);
        props.setFile(file);

    }

    public BufferedImage setFilter(Object value, BufferedImage bufferedImage){
        setOriginalImage(bufferedImage);
        switch (value.toString()){
            case "beagle/red":
                System.out.println("APPLY RED FILTER");
                bufferedImage = getRedImage();
                break;
            case "mouse/blue":
                System.out.println("APPLY BLUE FILTER");
                bufferedImage = getBlueImage();
                break;
            case "daisy/green":
                System.out.println("APPLY GREEN FILTER");
                bufferedImage = getGreenImage();
                break;
            default:
                System.out.println("APPLY NO FILTER");
                bufferedImage = bufferedImage;
                break;
        }
        return bufferedImage;
    }

}
