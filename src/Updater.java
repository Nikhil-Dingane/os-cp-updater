import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvException;

public class Updater {
	static String basicUrlAuthentication = "";

	public static void main(String[] args) throws IOException, CsvException {
		
		String cpApiUrl = args[0];
		String usernamePassword = args[1] + ":" + args[2];
		String fileName = args[3];
		
		basicUrlAuthentication = "Basic " + Base64.getEncoder().encodeToString(usernamePassword.getBytes());		
		
		CSVReader reader = new CSVReader(new FileReader(fileName));
		reader.readNext();
		String[] columns = null;
		ObjectMapper objectMapper = new ObjectMapper();

		while ((columns = reader.readNext()) != null) {
			
			Integer cpId = Integer.valueOf(columns[0]);
			String response = callAPI(cpApiUrl + "/" + cpId, null, "GET");
			
			Map<String, Object> cp = objectMapper.readValue(response, new TypeReference<HashMap<String, Object>>() {
			});
			
			cp.put("irbId", columns[2]);
			
			String id = cp.get("id").toString();
			String payload = objectMapper.writeValueAsString(cp);

			response = callAPI(cpApiUrl + "/" + id, payload, "PUT");
		}

	}

	public static String callAPI(String url, String payload, String method){
		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
			connection.setRequestProperty("Authorization", basicUrlAuthentication);
			connection.setRequestMethod(method);

			if (method.equals("PUT")) {
				connection.setRequestProperty("Content-Type", "application/json");
				connection.setDoOutput(true);
				OutputStream outputStream = connection.getOutputStream();
				outputStream.write(payload.getBytes());
			}

			int responseCode = connection.getResponseCode();
			BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String readLine = null;
			String response = "";

			while ((readLine = br.readLine()) != null) {
				response = response + readLine;
			}

			return response;
		} catch (Exception e) {
			e.printStackTrace();
			return "[]";
		}
	}
	
}
