package b2bApiTest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;

public class AddSFTPuser {
	public static void main(String[] args) throws Exception {
		
		// REST API connection details CHANGE HERE --------------
		String host = "80.14.22.000";
		String port = "14674";
		// REST API credentials
		String apiuser = "admin";
		String password = "password";
		// REST API URLs
		String svc_userkey = "B2BAPIs/svc/sshauthorizeduserkeys/";
		String svc_user = "B2BAPIs/svc/useraccounts/";
		
		// Create SSH AuthorizedUserKey JSON Object
		JSONObject sshUserKey = new JSONObject();
		// Define key name and set key enabled
		sshUserKey.put("keyName", "sftpuser14");
		sshUserKey.put("keyStatusEnabled", "TRUE");
		// Base64 encode SSH Public key 
		String ssh_pub_key = "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCpCnvNTUPtdxYen8hYKMeJYDJU+GEz7pGYPVN3GOSqy3TOXX/zQ7Kl1dt26T9WDHwWgwWIZo8LlaxXjnHtrNpdcGpe+NZxtWQvmLXbSQMSHKIwJei9BZHZpCHHkX5WAy4nWTuFus3qFJ5Pr+UASDSYJPbYt42zvyibDXZvs8UXjFVuSkdu+p6zDI9pxk3MroBLt3LkwFWzbxxgY8reHpEJmbwOueON8ysHhgQN94ZydmzTRG9mNtWSFX+IfiH8g85ODg6h2BwhSLArME00aEj9cxM7DO0TcawjFTxRU/E6m5B9+8TAfhXL4ZlKtX/V9O1KuQ39RB1t9+H3n2sLzMM1 martinwarnes@Martins-MacBook-Pro.local";
		Base64.Encoder encoder = Base64.getEncoder();
		sshUserKey.put("keyData", encoder.encodeToString(ssh_pub_key.getBytes(StandardCharsets.UTF_8) ));
		
		// Add SSH AuthorizedUserKey to the SB2BI Store
		String sshUserKeyResponse = post(host,port,apiuser,password,svc_userkey,sshUserKey);
		System.out.println(sshUserKeyResponse);

		// Create Array of permissions required to access the SFTP Inbox
		JSONArray permissions = new JSONArray();
		// User requires access to Mailbox login ...
		JSONObject permission = new JSONObject();
		permission.put("name", "Mailbox Login Without Virtual Root Permission");
		permissions.put(permission);
		// .. and also access to the specific mailbox
		permission = new JSONObject();
		//SFTP mailbox needs to exist
		permission.put("name","/CONS4 Mailbox");		
		permissions.put(permission);

		// Create Array of available AuthorizedUserKeys
		JSONArray userKeys = new JSONArray();
		// Add the name of each key the user is permitted
		// to authenticate with
		JSONObject userKey = new JSONObject();
		userKey.put("name","sftpuser14");	
		userKeys.put(userKey);
		
		// Create User
		JSONObject user = new JSONObject();
		// Required parameters
		user.put("authenticationType", "local");
		user.put("givenName", "Partner");
		user.put("surname", "surname");
		user.put("userId", "sftpuser13");
		user.put("password", "sterling");	
		// Optional parameters
		user.put("permissions", permissions);
		user.put("authorizedUserKeys", userKeys); 
	    
		// Add user to SB2BI
		String userResponse = post(host,port,apiuser,password,svc_user,user);
		System.out.println(userResponse);    

	}

	public static String post(String host, String port, String apiuser, String password, String svcURL, JSONObject obj) throws Exception {
		String jsonResponse=null;
		
		HttpPost httppost=null;
		
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(host, new Integer(port).intValue()),
				new UsernamePasswordCredentials(apiuser,password));
		CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();
		
		try {
			
			httppost = new HttpPost("http://" + host + ":" + port + "/" + svcURL );
		    StringEntity params =new StringEntity(obj.toString());
		    httppost.addHeader("content-type", "application/json");
		    httppost.addHeader("Accept","application/json");
		    httppost.setEntity(params);
		    System.out.println(httppost.toString());
			CloseableHttpResponse response = httpclient.execute(httppost);
			try {
				HttpEntity entity = response.getEntity();
				int retCode = response.getStatusLine().getStatusCode();
				String retPhrase = 	response.getStatusLine().getReasonPhrase();	
				jsonResponse = IOUtils.toString(entity.getContent(), StandardCharsets.UTF_8.name());
				if (retCode!=201) {
					throw new Exception("HTTP POST Request failed: " + retCode + " " + retPhrase + " " + jsonResponse);
				} else {
					System.out.println("HTTP Response: " + retCode);
				}

			} finally {
				response.close();
			}
			
			
		} finally {
			httpclient.close();
		}
		
		return jsonResponse;
	}
	
}
