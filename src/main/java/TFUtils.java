import org.tensorflow.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class TFUtils {

    Tensor executeSavedModel(String modelFolderPath, Tensor input) {
        try {
            Path path = Paths.get(ClassLoader.getSystemClassLoader().getResource(modelFolderPath).toURI()).toAbsolutePath();
            //Parse model, and read all bytes or exit
            SavedModelBundle model = SavedModelBundle.load(path.toString(),"serve");
            List<Tensor<?>> results = model.session().runner().feed("input", input).fetch("output").run();
            Tensor result = results.get(0);
            return result;
        } catch (URISyntaxException e) {
            throw new Error("invalid path");
        }
    }

    Tensor executeModelFromByteArray(byte[] graphDef, Tensor input) {
        try (Graph g = new Graph()) {
            g.importGraphDef(graphDef);
            try (Session s = new Session(g)) {
                Tensor result = s.runner().feed("input", input).fetch("output").run().get(0);
                return result;
            }
        }
    }

    /**
     * create a Tensor from an image
     * <p>
     * Scale and normalize an image (to 224x224), and convert to a tensor
     *
     * @param imageBytes
     * @return
     */
    Tensor byteBufferToTensor(byte[] imageBytes) {
        try (Graph g = new Graph()) {
            GraphBuilder graphBuilder = new GraphBuilder(g);
            // Some constants specific to the pre-trained model at:
            // https://storage.googleapis.com/download.tensorflow.org/models/inception5h.zip
            //
            // - The model was trained with images scaled to 224x224 pixels.
            // - The colors, represented as R, G, B in 1-byte each were converted to
            //   float using (value - Mean)/Scale.
            final float mean = 117f;
            final float scale = 1f;
            final int H =224;
            final int W =224;

            // Since the graph is being constructed once per execution here, we can use a constant for the
            // input image. If the graph were to be re-used for multiple input images, a placeholder would
            // have been more appropriate.
            final Output input = graphBuilder.constant("input", imageBytes);
            final Output output =
                    graphBuilder.div(
                            graphBuilder.sub(
                                    graphBuilder.resizeBilinear(
                                            graphBuilder.expandDims(
                                                    graphBuilder.cast(graphBuilder.decodeJpeg(input, 3), DataType.FLOAT),
                                                    graphBuilder.constant("make_batch", 0)),
                                            graphBuilder.constant("size", new int[]{H, W})),
                                    graphBuilder.constant("mean", mean)),
                            graphBuilder.constant("scale", scale));
            try (Session s = new Session(g)) {
                return s.runner().fetch(output.op().name()).run().get(0);
            }
        }
    }

    private static class GraphBuilder {
        private Graph g;

        GraphBuilder(Graph g) {
            this.g = g;
        }

        Output div(Output x, Output y) {
            return binaryOp("Div", x, y);
        }

        Output sub(Output x, Output y) {
            return binaryOp("Sub", x, y);
        }

        Output resizeBilinear(Output images, Output size) {
            return binaryOp("ResizeBilinear", images, size);
        }

        Output expandDims(Output input, Output dim) {
            return binaryOp("ExpandDims", input, dim);
        }

        Output cast(Output value, DataType dtype) {
            return g.opBuilder("Cast", "Cast").addInput(value).setAttr("DstT", dtype).build().output(0);
        }

        Output decodeJpeg(Output contents, long channels) {
            return g.opBuilder("DecodeJpeg", "DecodeJpeg")
                    .addInput(contents)
                    .setAttr("channels", channels)
                    .build()
                    .output(0);
        }

        Output constant(String name, Object value) {
            try (Tensor t = Tensor.create(value)) {
                return g.opBuilder("Const", name)
                        .setAttr("dtype", t.dataType())
                        .setAttr("value", t)
                        .build()
                        .output(0);
            }
        }

        private Output binaryOp(String type, Output in1, Output in2) {
            return g.opBuilder(type, type).addInput(in1).addInput(in2).build().output(0);
        }
    }

}
