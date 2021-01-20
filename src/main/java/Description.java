import org.tensorflow.Tensor;

import java.io.IOException;
import java.util.Arrays;

public class Description {
    public int getIndex(Tensor<Float> result , Property props) throws IOException {
        final long[] rshape = result.shape();
        if (result.numDimensions() != 2 || rshape[0] != 1) {
            throw new RuntimeException(
                    String.format(
                            "Expected model to produce a [1 N] shaped tensor where N is the number of labels, instead it produced one with shape %s",
                            Arrays.toString(rshape)));
        }
        int nlabels = (int) rshape[1];
        float[] proba = result.copyTo(new float[1][nlabels])[0];

        int index = maxIndex(proba);

        float probabilite = proba[index]*100f;
        props.setProba(probabilite);
        System.out.println();
        return index;
    }

    private static int maxIndex(float[] probabilities) {
        int best = 0;
        for (int i = 1; i < probabilities.length; ++i) {
            if (probabilities[i] > probabilities[best]) {
                best = i;
            }
        }
        return best;
    }
}

