import com.sun.javafx.logging.PlatformLogger;
import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.TilePane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.tensorflow.Tensor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javafx.application.Application;

public class Main extends Application {
    private Property props = new Property();
    public static void main(String[] argv) throws IOException {
        launch(argv);
    }
    public static Property getProperty(Property props) throws IOException {
        TFUtils utils = new TFUtils();
        byte [] data;
        System.out.println("AVANT LE IF"+props.getPath());
        if(props.getPath() != null) {
            data = Files.readAllBytes(props.getPath());
            System.out.println("PATHHHHHHHHHHHHH"+props.getPath());
        }else{
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
        System.out.println("label = " + labelText);
        return props;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Smart Webcam");

        GridPane root = new GridPane();
        Button buttonSave = new Button();
        Button buttonUpload = new Button();
        Button buttonFolder = new Button();
        buttonSave.setText("Sauvegarder");
        buttonUpload.setText("Importer");
        buttonFolder.setText("Choix du dossier");
        buttonSave.setDisable(true);
        buttonFolder.setDisable(true);
        TextField definition = new TextField();

        Label label = new Label();
        Label labelProba = new Label();
        buttonFolder.setOnAction((action->{
            FileChooser fileChooser = new FileChooser();

            //Set extension filter for text files
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("JPG files (*.jpg)", "*.jpg");
            fileChooser.getExtensionFilters().add(extFilter);
            System.out.println(fileChooser);
            //Show save file dialog
            File file = fileChooser.showSaveDialog(primaryStage);
            System.out.println("ITS FILE" + file);
//            File initialImage = new File(extFilter + "/" + props.getPath().getFileName());
            props.setFile(file);

        }));
        buttonSave.setOnAction((action -> {
            try {
                save(props);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        buttonUpload.setOnAction((action -> {
            FileChooser fileChooser = new FileChooser();
            File file = fileChooser.showOpenDialog(null);
            try {
                BufferedImage bufferedImage = ImageIO.read(file);
                WritableImage image1 = SwingFXUtils.toFXImage(bufferedImage, null);
                ImageView imageView = new ImageView();
                imageView.setImage(image1);
                imageView.setFitHeight(100);
                imageView.setFitWidth(100);
                GridPane.setConstraints(imageView, 1, 0);
                root.getChildren().add(imageView);
                props.setPath(file.toPath());
                props.setBufferedImage(bufferedImage);
                getProperty(props);
                label.setText(props.getDescription());
                labelProba.setText(String.valueOf(props.getProba()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        EventHandler<ActionEvent> event = (ActionEvent e) -> {
            if(definition.getText().contains(props.getDescription())){
                buttonSave.setDisable(false);
                buttonFolder.setDisable(false);
            }else{
                buttonSave.setDisable(true);
                buttonFolder.setDisable(true);
            }
        };

        // when enter is pressed
        definition.setOnAction(event);


        root.getChildren().add(label);
        root.getChildren().add(labelProba);
        root.getChildren().add(definition);
        GridPane.setConstraints(labelProba, 1, 2);
        GridPane.setConstraints(label, 1, 1);
        GridPane.setConstraints(buttonSave, 2, 1);
        GridPane.setConstraints(buttonUpload, 0, 0);
        GridPane.setConstraints(buttonFolder, 0, 1);
        GridPane.setConstraints(definition, 0, 2);

        root.getChildren().add(buttonSave);
        root.getChildren().add(buttonUpload);
        root.getChildren().add(buttonFolder);

        primaryStage.setScene(new Scene(root, 600, 600));
        primaryStage.show();
    }

    private static void save(Property property) throws IOException {
        try {
            ImageIO.write(property.getBufferedImage(), "jpg", property.getFile());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
