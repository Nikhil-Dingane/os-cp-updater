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
			String response = callAPI(cpApiUrl + "?query=" + getParamValue(columns[0]), null, "GET");
			List<HashMap<String, Object>> cps = objectMapper.readValue(response,
					new TypeReference<List<HashMap<String, Object>>>() {
			});
			Map<String, Object> cp = cps.get(0);
			
			// Adding email of principal Investigator
			Map<String, Object> principalInvestigator = new HashMap<String, Object>();
			principalInvestigator.put("emailAddress", columns[1]);
			cp.put("principalInvestigator", principalInvestigator);

			// Adding sites to the CP
			List<Map<String, Object>> sites = new ArrayList<Map<String, Object>>();

			// First column will be CP title and second will be PI's email. It will take all
			// column values onwards third column as site name.
			// Because a CP can have multiple sites.
			for (int i = 2; i < columns.length; i++) {
				// Checking if site is null
				if (columns[i].length() > 0) {
					Map<String, Object> site = new HashMap<String, Object>();
					site.put("siteName", columns[i]);
					sites.add(site);
				}
			}

			cp.put("cpSites", sites);
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
	
	public static String getParamValue(String param) {
		char[] tempParam = param.toCharArray();
		
		for(int i = 0; i < tempParam.length; i++) {
			if(tempParam[i] == ' ') {
				tempParam[i] = '+';
			} 
		}
	
		return new String(tempParam).replace("\"", "%22").replace("\'", "%27");
	}
}
