package b2bApiTest;


import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONException;
import org.json.JSONObject;


public class mailboxUtils {

	//Hostname and port
	//TODO: edit this to specify the location of your API server
	private static final String HTTP_BASE = "http://";
	private static final String HTTPS_BASE = "https://";
	private static final String HOSTNAME = "9.155.214.000";
	private static final int PORT = 8154;
	private static final String MBX_EXT = "/B2BAPIs/svc/";
	private static final String API_URI_BASE = HTTP_BASE + HOSTNAME + ":" + PORT + MBX_EXT;

	//API URI's
	private static final String DOCUMENT_URI = "documents/";
	private static final String MAILBOX_URI = "mailboxes/";
	private static final String MESSAGE_URI = "mailboxmessages/";
	private static final String MAILBOX_CONTENTS_URI = "mailboxcontents/?parentMailboxId=";

	//Headers
	private static final String CONTENT_TYPE = "Content-Type";
	private static final String ACCEPT = "Accept";
	private static final String APPLICATION_JSON = "application/json";

	//Auth
	//TODO: edit these to specify a user created in B2Bi that has the "API User" permission
	private static final String USERNAME = "admin";
	//Eventually you'll want to do something smarter with the password
	private static final String PASSWORD = "password";

	//TODO: set this to a mailbox you want to operate on
	//Hardcoded mailbox ID and path for demo purposes
	//These 2 must match
	private static final String MBX_LIST_URI = "67";
	private static final String MBX_PATH = "/mymailbox";


	public static void main(String[] args) throws IOException, JSONException {

		//
		// List mailbox contents
		//
		CloseableHttpResponse response = null;
		try {
			//Create and make request
			HttpRequestBase request = createRequest("GET", MAILBOX_CONTENTS_URI+MBX_LIST_URI, null);
			response = executeRequest(request);

			//Print body of response to system.out
			printResponseFromEntity(response);

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} finally { //Must close response
			response.close();
		}


		//
		// Create Document
		//
		//

		//This will store the id of the document we're going to create
		String documentId = null;

		//Create JSON request body
		JSONObject docJson = new JSONObject();
		docJson.put("payload", "SGVsbG8gV29ybGQh"); //Hello World! Base 64 encoded
		String docJsonString = docJson.toString();

		try {
			//Create and make request
			HttpRequestBase request = createRequest("POST", DOCUMENT_URI, docJsonString);
			response = executeRequest(request);
			
			//Parse created document id out of response JSON
			HttpEntity entity = response.getEntity();
			JSONObject json = new JSONObject(getResponseString(entity));
			
			//Get created Location element from JSON
			String jsonLocation = (String) json.get("Location");
			
			//Split the location and grab the last entry, the document id
			String[] jsonLocationSplit = jsonLocation.split("/");
			documentId = jsonLocationSplit[jsonLocationSplit.length-1];

			System.out.println("Created document id: " + documentId);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} finally { //Must close response
			response.close();
		}

		//
		// Create Message
		//
		//Create JSON request body
		JSONObject msgJson = new JSONObject();
		//On Windows the documentId needs to be changed to replace the ":" encoded
		//documentId="WIN-33OJ71JEER3:node1:156fff99d23:26283";
		msgJson.put("documentId", documentId);
		msgJson.put("extractableAlways", true);
		msgJson.put("mailboxPath", mailboxUtils.MBX_PATH);
		msgJson.put("name", "demoFile.txt");
		String msgJsonString = msgJson.toString();

		try {
			//Create and make request
			HttpRequestBase request = createRequest("POST", MESSAGE_URI, msgJsonString);
			response = executeRequest(request);

			printResponseFromEntity(response);	
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} finally { //Must close response
			response.close();
		}


		//
		// List mailbox contents, again
		//
		try {
			//Create and make request
			HttpRequestBase request = createRequest("GET", MAILBOX_CONTENTS_URI+MBX_LIST_URI, null);
			response = executeRequest(request);

			//Print body of response to system.out
			printResponseFromEntity(response);

		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		} finally { //Must close response
			response.close();
		}
	}


	/**
	 * Creates an HTTP request of the provided type, using the provided URI with the body containing the provided JSON
	 * 
	 * @param requestType - GET, POST, PUT, or DELETE
	 * @param URI - Full URI to make request to, eg; http://test.com/api/someid123
	 * @param jsonString - JSON string to be sent in the request body
	 * @return - The request object that has not yet been executed
	 * @throws UnsupportedEncodingException - if the JSON body uses characters not in the default HTTP charset
	 */
	private static HttpRequestBase createRequest(String requestType, String URI, String jsonString) throws UnsupportedEncodingException {
		HttpRequestBase httpRequest = null;
		switch(requestType.toUpperCase()) {
		case "GET": 
			httpRequest = new HttpGet(API_URI_BASE + URI);
			break;
		case "POST": 
			httpRequest = new HttpPost(API_URI_BASE + URI);
			((HttpPost) httpRequest).setEntity(new StringEntity(jsonString));
			break;
		case "PUT": 
			httpRequest = new HttpPut(API_URI_BASE + URI);
			((HttpPut) httpRequest).setEntity(new StringEntity(jsonString));
			break;
		case "DELETE": 
			httpRequest = new HttpDelete(API_URI_BASE + URI);
			break;	
		}
		httpRequest.setHeader(CONTENT_TYPE, APPLICATION_JSON);
		httpRequest.setHeader(ACCEPT, APPLICATION_JSON);
		return httpRequest;
	}


	/**
	 * Executes the provided request and returns a response object
	 * 
	 * @param request - HTTP request to be executed
	 * @return - Response to the executed request
	 * @throws ClientProtocolException - if there is an HTTP protocol error during the request execution
	 * @throws IOException - if there is an error at the connection level during request execution
	 */
	private static CloseableHttpResponse executeRequest(HttpRequestBase request) throws ClientProtocolException, IOException {
		//Setup HTTP auth
		CredentialsProvider credsProvider = new BasicCredentialsProvider();
		credsProvider.setCredentials(new AuthScope(HOSTNAME, PORT),new UsernamePasswordCredentials(USERNAME, PASSWORD));
		CloseableHttpClient httpclient = HttpClients.custom().setDefaultCredentialsProvider(credsProvider).build();

		return httpclient.execute(request);
	}


	/**
	 * Prints the body of the response object to System.out
	 * 
	 * @param response - Response to be printed from
	 * @throws UnsupportedOperationException - if the response body cannot be streamed correctly
	 * @throws IOException - if there is an error streaming the response body
	 */
	private static void printResponseFromEntity(CloseableHttpResponse response) throws UnsupportedOperationException, IOException {
		System.out.println(response.getStatusLine());
		HttpEntity entity = response.getEntity();
		System.out.println(getResponseString(entity));
	}


	/**
	 * Streams the content of the provided entity into a String and returns that String
	 * 
	 * @param entity - Entity to take content from
	 * @return - String containing the content of the entity
	 * @throws UnsupportedOperationException - if the entity content cannot be streamed to a string
	 * @throws IOException - if there is an error streaming the entity content
	 */
	private static String getResponseString(HttpEntity entity) throws UnsupportedOperationException, IOException {
		InputStream responseIn = entity.getContent();
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		IOUtils.copy(responseIn, out);
		String result = out.toString();

		//Close streams
		out.flush();
		out.close();
		responseIn.close();

		//Return
		return result;
	}
}
