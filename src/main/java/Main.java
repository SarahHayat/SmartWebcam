import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.bytedeco.javacv.*;
import org.bytedeco.opencv.opencv_core.IplImage;
import org.tensorflow.Tensor;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.bytedeco.javacv.*;
import org.bytedeco.javacv.Frame;
import org.bytedeco.opencv.opencv_core.IplImage;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import static org.bytedeco.opencv.global.opencv_core.cvFlip;
import static org.bytedeco.opencv.helper.opencv_imgcodecs.cvSaveImage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.Executors;

import javafx.application.Application;

public class Main extends Application {
    private Property props = new Property();
    private final Java2DFrameConverter java2DFrameConverter = new Java2DFrameConverter();
    final int INTERVALCAM = 1;///you may use interval


    public static void main(String[] argv) throws IOException {
        launch(argv);
    }

    public static Property getProperty(Property props) throws IOException {
        TFUtils utils = new TFUtils();
        byte[] data;
        if (props.getPath() != null) {
            System.out.println("LE PAAAAAATHHHH "+ props.getPath());
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

        VBox boxCamera = new VBox();

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

        Label descriptionLabel = new Label();

        Label percentLabel = new Label();

        TextField tfProba = new TextField();

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
                        System.out.println("_____DESCRIPTION______ = "+ props.getDescription());
                        while (true) {
                            frame = opengrabber.grabFrame();
                            img = converter.convert(frame);
                            //save
//                            cvSaveImage("images" + File.separator + (i++) + "-aa.jpg", img);
                            BufferedImage bufferedImage = java2DFrameConverter.getBufferedImage(frame);
                            imageView.setImage(frameToImage(frame));

                            Thread.sleep(INTERVALCAM);
                            cvSaveImage("images" + File.separator + "picture.jpg", img);

                            props.setPath(Paths.get("images/picture.jpg"));
                            props.setBufferedImage(bufferedImage);
                            if(props.getPath() != null) {
                                getProperty(props);
                            }
                        }

                    } catch (Exception e) {
                        e.printStackTrace();
                    }
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
            if(props.getDescription() != null) {
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

        boxPicture.getChildren().addAll(buttonUpload, buttonFolder, buttonSave, pathLabel);
        boxPicture.setPadding(new Insets(5, 5, 5, 50));


        boxDescLabels.getChildren().addAll(definitionLabel, tfProba, buttonProcess);
        boxDescLabels.setPadding(new Insets(5, 5, 5, 50));

        boxProperties.getChildren().addAll(descriptionLabel, percentLabel);
        boxProperties.setPadding(new Insets(5, 5, 5, 50));

        root.getChildren().add(open);
        GridPane.setConstraints(open, 4, 4);

        root.getChildren().add(boxPicture);
        GridPane.setConstraints(boxPicture, 0, 0);

        root.getChildren().add(boxDescLabels);
        GridPane.setConstraints(boxDescLabels, 0, 1);

        root.getChildren().add(boxProperties);
        GridPane.setConstraints(boxProperties, 1, 1);

        root.getChildren().add(boxCamera);
        GridPane.setConstraints(boxCamera, 0, 2);


        primaryStage.setScene(new Scene(root, 1000, 600));
        primaryStage.show();
    }

    private WritableImage frameToImage(Frame frame) {
        BufferedImage bufferedImage = java2DFrameConverter.getBufferedImage(frame);
        return SwingFXUtils.toFXImage(bufferedImage, null);
    }

    private static void save(Property property) throws IOException {
        try {
            ImageIO.write(property.getBufferedImage(), "jpg", property.getFile());
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

}
