import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
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

        TFUtils utils = new TFUtils();
        BufferedImage bImage = ImageIO.read(new File("tensorPics/suncokret.jpg"));
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(bImage, "jpg", bos);
        byte[] data = bos.toByteArray();

        String filePath = "src/inception5h/tensorflow_inception_graph.pb";

        List<String> labels = Files.readAllLines(Paths.get("src/inception5h/labels.txt"));
        byte[] pb = Files.readAllBytes(Paths.get(filePath));
        Tensor<Float> tensor = utils.byteBufferToTensor(data);
        Tensor<Float> result = utils.executeModelFromByteArray(pb, tensor);

        Description description = new Description();
        int index = description.getIndex(result);
        String labelText = labels.get(index);
        System.out.println("label = " + labelText);

    }

    @Override
    public void start(Stage primaryStage) throws Exception {
        primaryStage.setTitle("Hello World!");
        Button btn = new Button();
        btn.setText("Say 'Hello World'");
        btn.setOnAction((action) -> {
            System.out.println("Hello World!");
        });

        ImageView image = new ImageView();
        image.setImage(new Image(getClass().getResource("inception5h/tensorPics/jack.jpg").toExternalForm()));
        image.setFitHeight(100);
        image.setFitWidth(100);
        TilePane root = new TilePane();
        root.getChildren().add(image);
        root.getChildren().add(btn);
        primaryStage.setScene(new Scene(root, 600, 600));
        primaryStage.show();
    }

}
