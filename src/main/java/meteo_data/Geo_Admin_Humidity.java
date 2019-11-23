package meteo_data;

import com.jayway.jsonpath.JsonPath;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Geo_Admin_Humidity {
    public static String GetData(){
        try {
            URL url = new URL("https://data.geo.admin.ch/ch.meteoschweiz.messwerte-luftfeuchtigkeit-10min/ch.meteoschweiz.messwerte-luftfeuchtigkeit-10min_de.json");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Content-Type", "application/json");

            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();

            JSONObject tempData = new JSONObject(response.toString());
            String tempvalue = JsonPath.read(response.toString(), "$.features[?(@.id=='CHZ')].properties.value").toString();
            con.disconnect();
            return tempvalue;
        } catch (IOException e) {
            e.printStackTrace();
            return "0";
        }
    }

}
