import org.tensorflow.Tensor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;


public class Main {
    public static void main(String[] argv) throws IOException {
        TFUtils utils = new TFUtils();
        BufferedImage bImage = ImageIO.read(new File("src/inception5h/tensorPics/suncokret.jpg"));
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


}
