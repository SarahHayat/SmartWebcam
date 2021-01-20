import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.TilePane;
import javafx.stage.Stage;
import org.tensorflow.Tensor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import javafx.application.Application;
public class Main extends Application {
    public static void main(String[] argv) throws IOException {
        launch(argv);
    }

    public static Property getProperty() throws IOException {
        TFUtils utils = new TFUtils();
        Property props = new Property();
        byte [] data = Files.readAllBytes(Paths.get("src/main/resources/tensorPics/suncokret.jpg"));

        String filePath = "src/inception5h/tensorflow_inception_graph.pb";

        List<String> labels = Files.readAllLines(Paths.get("src/inception5h/labels.txt"));
        byte[] pb = Files.readAllBytes(Paths.get(filePath));
        Tensor<Float> tensor = utils.byteBufferToTensor(data);
        Tensor<Float> result = utils.executeModelFromByteArray(pb, tensor);

        Description description = new Description();
        int index = description.getIndex(result);
        String labelText = labels.get(index);
        props.setDescription(labelText);
        System.out.println("label = " + labelText);
        return props;
    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        Property property = getProperty();
        primaryStage.setTitle("Hello World!");
        Image image = new Image("tensorPics/jack.jpg");
        ImageView imageView = new ImageView();
        imageView.setImage(image);
        imageView.setFitHeight(100);
        imageView.setFitWidth(100);
        Label label = new Label();
        label.setText(property.getDescription());
        GridPane root = new GridPane();
        GridPane.setConstraints(label, 1, 1);
        GridPane.setConstraints(imageView, 1, 0);
        root.getChildren().add(imageView);
        root.getChildren().add(label);
        primaryStage.setScene(new Scene(root, 600, 600));
        primaryStage.show();
    }

}
