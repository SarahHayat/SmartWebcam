import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.tensorflow.Tensor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javafx.application.Application;

public class Main extends Application {
    private Property props = new Property();

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

        GridPane root = new GridPane();

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

        TextField definition = new TextField();

        Label path = new Label();

        Label label = new Label();

        Label labelProba = new Label();

        TextField tfProba = new TextField();

        buttonFolder.setOnAction((action -> {
            FileChooser fileChooser = new FileChooser();
            FileChooser.ExtensionFilter extFilter = new FileChooser.ExtensionFilter("JPG files (*.jpg)", "*.jpg");
            fileChooser.getExtensionFilters().add(extFilter);
            File file = fileChooser.showSaveDialog(primaryStage);
            props.setFile(file);
            if (props.getFile() != null) {
                path.setText(props.getFile().getPath());
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
            FileChooser fileChooser = new FileChooser();
            File file = fileChooser.showOpenDialog(null);
            try {
                BufferedImage bufferedImage = ImageIO.read(file);
                WritableImage image1 = SwingFXUtils.toFXImage(bufferedImage, null);
                ImageView imageView = new ImageView();
                imageView.setImage(image1);
                imageView.setFitHeight(300);
                imageView.setFitWidth(300);
                GridPane.setConstraints(imageView, 1, 0);
                root.getChildren().add(imageView);
                props.setPath(file.toPath());
                props.setBufferedImage(bufferedImage);
                getProperty(props);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));

        EventHandler<ActionEvent> event = (ActionEvent e) -> {
            if (definition.getText().contains(props.getDescription()) && props.getProba() >= Float.parseFloat(tfProba.getText())) {
                label.setText(props.getDescription());
                labelProba.setText(String.valueOf(props.getProba()));
                buttonSave.setDisable(false);
                buttonFolder.setDisable(false);
            } else {
                buttonSave.setDisable(true);
                buttonFolder.setDisable(true);
                label.setText("");
                labelProba.setText("");
            }
        };

        buttonProcess.setOnAction(event);

        root.getChildren().add(label);
        GridPane.setConstraints(label, 1, 1);

        root.getChildren().add(labelProba);
        GridPane.setConstraints(labelProba, 1, 3);

        root.getChildren().add(tfProba);
        GridPane.setConstraints(tfProba, 1, 2);

        root.getChildren().add(definition);
        GridPane.setConstraints(definition, 0, 2);

        root.getChildren().add(path);
        GridPane.setConstraints(path, 0, 3);

        root.getChildren().add(buttonSave);
        GridPane.setConstraints(buttonSave, 2, 1);

        root.getChildren().add(buttonUpload);
        GridPane.setConstraints(buttonUpload, 0, 0);

        root.getChildren().add(buttonFolder);
        GridPane.setConstraints(buttonFolder, 0, 1);

        root.getChildren().add(buttonProcess);
        GridPane.setConstraints(buttonProcess, 0, 4);

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
