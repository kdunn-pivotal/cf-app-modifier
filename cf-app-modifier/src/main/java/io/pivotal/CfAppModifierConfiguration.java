package io.pivotal;

import java.text.SimpleDateFormat;
import java.security.cert.X509Certificate;
import java.util.Date;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.annotation.PostConstruct;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.annotation.StreamListener;
import org.springframework.cloud.stream.messaging.Sink;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;

@SpringBootApplication
@EnableBinding(Sink.class)
@EnableConfigurationProperties(CfAppModifierProperties.class)
public class CfAppModifierConfiguration {
	
	@Autowired
	CfAppModifierProperties properties;
	
	private static final Logger log = LoggerFactory.getLogger(CfAppModifierConfiguration.class);
	private static final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

	private static TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			return new X509Certificate[0];
		}

		public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
		}

		public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
		}
	} };

	String getAppGUID(String name) throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> responseEntity;

		// URI takes the form https://somehost.somedomain/v###
		// so if we split on "/v" and take the first token, the host is left
		String apiHost = properties.getCfApiUri().split("/v")[0];
		HttpHeaders headers = Connection.getAuthorizationHeader(getOAuthToken(), apiHost);

		String getNameApiUri = properties.getCfApiUri() + "/apps?q=" + name;
		
		// get an application guid by name
		responseEntity = restTemplate.exchange(getNameApiUri, HttpMethod.GET,
				new HttpEntity<>(null, headers), String.class);

		JSONParser parser = new JSONParser();
		JSONObject json = (JSONObject) parser.parse(responseEntity.getBody());

		JSONObject resources = (JSONObject) json.get("resources"); 
		
		String guid = "";
		for (Object res : resources.values()){
			guid = (String)((JSONObject) res).get("guid");
			
			// TODO, we assume the first app will be the correct
			// GUID, since only one app should map to a name
			break;
		}
		
		return guid;
	}
	
	@StreamListener(Sink.INPUT)
	public void adjustAppScale(String commandMessage) throws Exception {

		log.info(dateFormat.format(new Date()));

		String apiUri = properties.getCfApiUri();

		log.debug("API URI : " + apiUri);

		// TODO do lookup of GUID by name
		String appAPI = apiUri + "/apps/" + getAppGUID(properties.getTargetApplication());
		log.debug("appAPI=" + appAPI);

		//boolean scaleApp = compareValues(ruleCompareValue, (Double) r.get(dataName), ruleOperator);
		log.info("scale app " + commandMessage);

		Integer appInstanceCount = getCurrentInstances(appAPI);
		
		log.info("app summary number of instances=" + appInstanceCount);
		Integer minInstances = properties.getMinInstances();
		Integer maxInstances = properties.getMaxInstances();

		if (commandMessage == "UP" && (appInstanceCount+1 <= maxInstances)) {
			appInstanceCount = appInstanceCount + 1;
		
			log.info("scale the app up to " + appInstanceCount + " instances.");
			scaleApp(appAPI, appInstanceCount);
		} else if (commandMessage == "DOWN" && (appInstanceCount-1 >= minInstances)) {
			appInstanceCount = appInstanceCount - 1;
		
			log.info("scale the app down to " + appInstanceCount + " instances.");
			scaleApp(appAPI, appInstanceCount);
		} else /* No change */ {
			// NOOP
		}
	}

	@PostConstruct
	private String getOAuthToken() throws Exception {
		
		// Install the all-trusting trust manager
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		RestTemplate restTemplate = new RestTemplate();

		HttpHeaders headers = Connection.getBasicAuthorizationHeader("cf", "");

		LinkedMultiValueMap<String, String> postBody = new LinkedMultiValueMap<>();
		postBody.add("grant_type", "password");
		postBody.add("username", properties.getCfAdminUser());
		postBody.add("password", properties.getCfAdminPassword());

		log.debug("Getting OAuth token from : " + properties.getCfLoginHost());
		
		ResponseEntity<String> r = restTemplate.exchange(properties.getCfLoginHost() + "/oauth/token", HttpMethod.POST,
				new HttpEntity<>(postBody, headers), String.class);

		org.json.simple.parser.JSONParser parser = new org.json.simple.parser.JSONParser();
		org.json.simple.JSONObject json = null;
		
		json = (org.json.simple.JSONObject) parser.parse(r.getBody());

		return (String) json.get("access_token");
	}

	
	public JSONObject scaleApp(String scaleAppURL, Integer currentInstances) throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> responseEntity;
		JSONParser parser = new JSONParser();
		JSONObject json = null;

		// URI takes the form https://somehost.somedomain/v###
		// so if we split on "/v" and take the first token, the host is left
		String apiHost = properties.getCfApiUri().split("/v")[0];
		HttpHeaders headers = Connection.getAuthorizationHeader(getOAuthToken(), apiHost);
		headers.add("Content-Type", "application/json");

		JSONObject postBody = new JSONObject();
		postBody.put("instances", currentInstances);

		// System.out.println("scaleApp headers=" + headers);
		log.debug("scaleApp scaleAppURL=" + scaleAppURL);
		log.debug("scaleApp postBody=" + postBody);

		// get current instances
		responseEntity = restTemplate.exchange(scaleAppURL, HttpMethod.PUT,
				new HttpEntity<>(postBody.toJSONString(), headers), String.class);
		log.debug("scaleApp return=" + responseEntity.getBody() + " status" + responseEntity.getStatusCodeValue());

		json = (org.json.simple.JSONObject) parser.parse(responseEntity.getBody());

		return json;
	}

	public Integer getCurrentInstances(String scaleAppURL) throws Exception {
		RestTemplate restTemplate = new RestTemplate();
		ResponseEntity<String> responseEntity;
		JSONParser parser = new JSONParser();
		JSONObject json = null;
		
		// URI takes the form https://somehost.somedomain/v###
		// so if we split on "/v" and take the first token, the host is left
		String apiHost = properties.getCfApiUri().split("/v")[0];
		HttpHeaders headers = Connection.getAuthorizationHeader(getOAuthToken(), apiHost);

		// get current instances
		responseEntity = restTemplate.exchange(scaleAppURL + "/summary", HttpMethod.GET,
				new HttpEntity<>(null, headers), String.class);

		json = (org.json.simple.JSONObject) parser.parse(responseEntity.getBody());

		Integer appInstanceCount = (Integer) json.get("instances");
		
		return appInstanceCount;
	}
	
	public static void main(String[] args) {
		SpringApplication.run(CfAppModifierConfiguration.class, args);
	}
}
