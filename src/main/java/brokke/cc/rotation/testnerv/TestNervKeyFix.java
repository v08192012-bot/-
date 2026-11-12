package brokke.cc.rotation.testnerv;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Точный перенос поправщика клавиш Opus ({@code neurotest-keyfix-v1}).
 *
 * Сеть получает те же 42 признака тика плюс текущие клавиши и q, и переписывает ввод так,
 * чтобы движение совпадало с тем, что делал человек при такой же наводке. Снек сохраняется
 * всегда — его сеть не трогает.
 */
public final class TestNervKeyFix {
   public static final String FORMAT = "neurotest-keyfix-v1";
   public static final double REFERENCE_TOLERANCE = 0.001;

   public double[][][] weights;
   public double[][] biases;
   public double holdRatio;
   public double maxDistance;
   public int featureCount;
   public String source = "?";
   public String status = "";

   /** Аппроксимация erf из Opus (Абрамовиц—Стиган, 5 членов). */
   public static double erf(double value) {
      double sign = Math.signum(value);
      double absolute = Math.abs(value);
      double t = 1.0 / (1.0 + 0.3275911 * absolute);
      double polynomial = 1.0
         - ((((1.061405429 * t - 1.453152027) * t + 1.421413741) * t - 0.284496736) * t + 0.254829592)
            * t
            * Math.exp(-absolute * absolute);
      return sign * polynomial;
   }

   public static double normalCdf(double value) {
      return 0.5 * value * (1.0 + erf(value / Math.sqrt(2.0)));
   }

   /** Прямой проход: признаки + биты клавиш + q, затем слои с нормальным CDF на выходах. */
   public double[] forward(double[] features, int keys) {
      double[] input = new double[this.featureCount + 8];
      System.arraycopy(features, 0, input, 0, this.featureCount);

      for (int bit = 0; bit < 7; bit++) {
         input[this.featureCount + bit] = keys >> bit & 1;
      }

      input[this.featureCount + 7] = this.holdRatio;

      for (int layer = 0; layer < this.weights.length; layer++) {
         double[][] layerWeights = this.weights[layer];
         double[] output = new double[layerWeights.length];

         for (int row = 0; row < layerWeights.length; row++) {
            double sum = this.biases[layer][row];
            double[] weightsRow = layerWeights[row];

            for (int column = 0; column < weightsRow.length; column++) {
               sum += weightsRow[column] * input[column];
            }

            output[row] = layer < this.weights.length - 1 ? normalCdf(sum) : sum;
         }

         input = output;
      }

      return input;
   }

   /** Класс действия (9 направлений × прыжок × спринт) обратно в битовую маску клавиш. */
   public static int decodeKeys(int action, int currentKeys) {
      int sprint = action & 1;
      int jump = action >> 1 & 1;
      int direction = action >> 2;
      int strafe = direction % 3 - 1;
      int forward = direction / 3 - 1;
      return (forward > 0 ? 1 : 0)
         | (forward < 0 ? 2 : 0)
         | (strafe > 0 ? 4 : 0)
         | (strafe < 0 ? 8 : 0)
         | jump << 4
         | currentKeys & 32
         | sprint << 6;
   }

   /** Поправщик включается только на живой цели в пределах дистанции обучения. */
   public boolean applies(double[] features) {
      return features[21] > 0.5 && Math.exp(features[29] * 2.0) < this.maxDistance;
   }

   public int sampleKeys(double[] features, int currentKeys, Random random) {
      double[] logits = this.forward(features, currentKeys);
      double maximum = Double.NEGATIVE_INFINITY;

      for (double logit : logits) {
         maximum = Math.max(maximum, logit);
      }

      double total = 0.0;
      double[] weights = new double[logits.length];

      for (int index = 0; index < logits.length; index++) {
         weights[index] = Math.exp(logits[index] - maximum);
         total += weights[index];
      }

      double sample = random.nextDouble() * total;
      double accumulated = 0.0;

      for (int index = 0; index < weights.length; index++) {
         accumulated += weights[index];
         if (sample < accumulated) {
            return decodeKeys(index, currentKeys);
         }
      }

      return decodeKeys(weights.length - 1, currentKeys);
   }

   public static double[] readDoubles(JsonArray array) {
      double[] values = new double[array.size()];

      for (int index = 0; index < values.length; index++) {
         values[index] = array.get(index).getAsDouble();
      }

      return values;
   }

   /** Загрузка с проверкой эталона логитов, как в Opus. */
   public static TestNervKeyFix load(InputStream stream) throws IOException {
      InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
      JsonObject root;

      try {
         root = JsonParser.parseReader(reader).getAsJsonObject();
      } catch (Throwable error) {
         try {
            reader.close();
         } catch (Throwable closeError) {
            error.addSuppressed(closeError);
         }

         throw error;
      }

      reader.close();

      if (!FORMAT.equals(root.get("format").getAsString())) {
         throw new IOException("формат " + root.get("format").getAsString() + ", ждали " + FORMAT);
      }

      TestNervKeyFix net = new TestNervKeyFix();
      net.holdRatio = root.get("q").getAsDouble();
      net.maxDistance = root.get("max_dist").getAsDouble();
      net.featureCount = root.get("nf").getAsInt();
      net.source = root.get("source").getAsString();
      JsonArray layers = root.getAsJsonArray("layers");
      net.weights = new double[layers.size()][][];
      net.biases = new double[layers.size()][];

      for (int layer = 0; layer < layers.size(); layer++) {
         JsonObject layerJson = layers.get(layer).getAsJsonObject();
         JsonArray matrix = layerJson.getAsJsonArray("w");
         net.weights[layer] = new double[matrix.size()][];

         for (int row = 0; row < matrix.size(); row++) {
            net.weights[layer][row] = readDoubles(matrix.get(row).getAsJsonArray());
         }

         net.biases[layer] = readDoubles(layerJson.getAsJsonArray("b"));
      }

      JsonObject reference = root.getAsJsonObject("ref");
      JsonArray inputs = reference.getAsJsonArray("x");
      JsonArray keys = reference.getAsJsonArray("u");
      JsonArray expectedLogits = reference.getAsJsonArray("logits");
      double maximumError = 0.0;

      for (int index = 0; index < inputs.size(); index++) {
         double[] actual = net.forward(readDoubles(inputs.get(index).getAsJsonArray()), keys.get(index).getAsInt());
         double[] expected = readDoubles(expectedLogits.get(index).getAsJsonArray());

         for (int logit = 0; logit < actual.length; logit++) {
            maximumError = Math.max(maximumError, Math.abs(actual[logit] - expected[logit]));
         }
      }

      if (!(maximumError < REFERENCE_TOLERANCE)) {
         throw new IOException("эталон поправщика не сошёлся: max |Δлогит| " + maximumError);
      }

      net.status = String.format(
         "поправщик клавиш %s: эталон %d строк, max |Δлогит| %.1e, q %.2f, ближе %.1f блока",
         net.source,
         inputs.size(),
         maximumError,
         net.holdRatio,
         net.maxDistance
      );
      return net;
   }
}
