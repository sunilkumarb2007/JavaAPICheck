import java.io.InputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.HttpURLConnection;

public class Download {
    public static void main(String[] args) throws Exception {
        URL url = new URL("https://start.spring.io/starter.zip?type=maven-project&javaVersion=17");
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        
        InputStream in = con.getInputStream();
        FileOutputStream out = new FileOutputStream("spring.zip");
        byte[] buffer = new byte[4096];
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
        in.close();
        out.close();
        System.out.println("Download complete.");
    }
}
