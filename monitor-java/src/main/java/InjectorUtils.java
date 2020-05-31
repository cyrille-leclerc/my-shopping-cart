import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class InjectorUtils {

    public static String toString(InputStream in, String encoding) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(in, encoding))) {
            StringBuilder response = new StringBuilder();
            String responseLine = null;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
           return response.toString();
        }
    }
}
