import com.sun.javafx.logging.PlatformLogger;
import javafx.embed.swing.SwingFXUtils;
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
        primaryStage.setTitle("Hello World!");
//        Image image = new Image(String.valueOf(getClass().getResource(property.getImage())));
//        ImageView imageView = new ImageView();
//        imageView.setImage(image);
//        imageView.setFitHeight(100);
//        imageView.setFitWidth(100);

        GridPane root = new GridPane();
        Button buttonSave = new Button();
        Button buttonUpload = new Button();
        buttonSave.setText("Process");
        buttonUpload.setText("Importer");
        buttonSave.setOnAction((action -> {
            try {
                save(props);
                getProperty(props);
                Label label = new Label();
                Label labelProba = new Label();
                label.setText(props.getDescription());
                labelProba.setText(String.valueOf(props.getProba()));
                root.getChildren().add(label);
                root.getChildren().add(labelProba);
                GridPane.setConstraints(labelProba, 1, 2);
                GridPane.setConstraints(label, 1, 1);


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
                System.out.println("CECI EST UN PATH " + props.getPath());
                props.setBufferedImage(bufferedImage);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }));
        GridPane.setConstraints(buttonSave, 2, 1);
        GridPane.setConstraints(buttonUpload, 0, 0);

        root.getChildren().add(buttonSave);
        root.getChildren().add(buttonUpload);

        primaryStage.setScene(new Scene(root, 600, 600));
        primaryStage.show();
    }

    private static void save(Property property) {

        try {
            Path path = Paths.get("src/main/resources/output");
            Files.createDirectories(path);
            System.out.println("Directory is created!");
            File initialImage = new File(path + "/" + property.getPath().getFileName());
            ImageIO.write(property.getBufferedImage(), "jpg", initialImage);
            System.out.println("DANS SAVE"+property.getPath());
        } catch (IOException e) {

            System.err.println("Failed to create directory!" + e.getMessage());

        }
    }


}
