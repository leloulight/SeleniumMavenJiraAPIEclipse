//TODO Create config file to store static values such as URLs and username/password
//TODO Split out components such as JIRA inegration, test script processing, keyword drivers
//TODO Find out why the target forlder can not be checked in to SVN
//TODO Use a tool to hide username and passwords or use generic account
//TODO Pass in parameters to determine which project is being tested.
//TODO JIRA test cases should be used rather than the excel spreadsheet. Need to find a way to extract JIRA test steps
//TODO object repositories are to be set through the config file. Different for each project

package com.AutomationFramework;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import java.util.Scanner;

import javax.naming.AuthenticationException;

import org.apache.commons.io.FileUtils;

import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;

import org.json.JSONArray;
import org.json.JSONObject;

import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.htmlunit.HtmlUnitDriver;
import org.openqa.selenium.ie.InternetExplorerDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.core.util.Base64;



public class Main {
	// Declared global variables can be accessed from any method in the class.
	public static int xTCRows, xTCCols;
	public static String[][] xTCdata;
	public static int xTSRows, xTSCols;
	public static String[][] xTSdata;
	public static String vBrowser, vURL, vText, vGetText, vAttribute;
	public static String vDescription, vKeyword, vIP1, vIP2, vIP3;
	public static long vSleep, issueId;
	public static WebDriver myD = new HtmlUnitDriver();
	public static String cycleId, testId, testKey, executionId, auth, projectId, versionId;
	public static String BASE_URL;
	public static String defectKey, newFile, newfileName;
	public static Boolean testPass;
	public static ObjectMap objmap;
	//private static WebDriver driver = null;
	// The Chrome Driver locations under the resource folder
	private static String WINDOWS_CHROMEDRIVER = "/chromedriver/windows/chromedriver.exe";
	private static String WINDOWS_IEDRIVER = "/iedriver/IEDriverServer.exe";
	private static String OS = System.getProperty("os.name");	
	
	
	public static void main(String[] args) throws Throwable {
		
		
		//TODO Build number passed in from Jenkins. 
		//This is the build that was successful and now ready to test
		//String SVNBuildNum = args[0];		
		//System.out.println("Passed Param FRom Jenkins: " + SVNBuildNum);
		
		
		String currentDir = System.getProperty("user.dir");
		//String newline = System.getProperty("line.separator");
		//String eXPath;
		String xlPath;
		
		
		xlPath = currentDir + "\\src\\main\\datafiles\\Automation Plan 1.1.xls";
		
		//TODO Put this out to a config file
		BASE_URL = "http://rhe-jira-test01.test.lan:8280/jira";
		auth = new String(Base64.encode("test.project.officer:test.project.officer"));
		projectId = "12288";
		versionId = "18887";
		// Read The Object Repository
		objmap = new ObjectMap(currentDir + "\\src\\main\\datafiles\\object.properties");
		// Read Runsheet containing what to test
		File f = new File(currentDir + "\\src\\main\\datafiles\\Runsheet.txt");

		


		
		// Read All the Sheets that are listed in the runsheet document
		ArrayList<String> lines = get_arraylist_from_file(f);

		// Create JIRA TEST CYCLE
		System.out.println("Create JIRA Cycle");
		cycleId = JIRA_NewCycle();

		// Process each sheet one at a time
		for (int x = 0; x < lines.size(); x++) {
			//get test id from JIRA by passing Test Key
			testKey = lines.get(x);
			testId = JIRA_GetTestID(testKey);
			
			System.out.println("Currently Testing Test: " + testId);
			testPass = true;
			// For each sheet, create a new execution
			executionId = JIRA_NewExecution(cycleId,testId);

			// Read the Test Steps XL sheet.
			//TODO accept NULL or Blank values from excel. At the moment "NA" is required
			xlTSRead(xlPath, testKey);
			xlTCRead(xlPath, testKey);

			// Go to each row in the TC Sheet and see if the execute flag is Y

			for (int j = 1; j < xTCRows; j++) {
				if (testPass.equals(true))
					if (xTCdata[j][4].equals("Y")) { // If y then go to each row in TS Sheet
						for (int i = 1; i < xTSRows; i++) {

							if (xTCdata[j][0].equals(xTSdata[i][0])
									&& (xTCdata[i][4].equals("Y") && (testPass.equals(true)))) { // and see if TCID's match
								System.out.println("Testing " + xTCdata[i][1]);
								System.out.println("testPass is: " + testPass);
								vDescription = xTSdata[i][1];
								vKeyword = xTSdata[i][2];
								vIP1 = xTSdata[i][3];
								vIP2 = xTSdata[i][5];

								// Identify the keyword and execute the
								// corresponding function

								keyword_executor(vKeyword);

							}
						}
					}
			}
			// Execute the test
			 JIRA_ExecuteTest(executionId, testPass);
			// Create a defect if test failed. Attach Screenshot. Close browser
			if (!testPass.equals(true)) {
				System.out.println("Test Failed");
				FailedTest(testId, vDescription);
				
			} else {
				System.out.println("Test Passed");
			}
			// close_browser(myD);
		}
	}

	public static void navigate_to(WebDriver mD, String fURL) throws Exception {
		
		System.out.println("Navigating to: " + fURL);
		
		mD.navigate().to(fURL);
		String currentURL = mD.getCurrentUrl();
		Thread.sleep(5000L);
		System.out.println("Current URL: " + currentURL);
		
		testPass = false;
		
		
	}

	public static void send_keys(WebDriver mD, String fxPath, String fText)
			throws Exception {

		try {
			System.out.println("Enter value for: " + fxPath + " with value: "
					+ fText);
			WebElement element = (new WebDriverWait(mD, 30))
					.until(ExpectedConditions.presenceOfElementLocated(objmap
							.getLocator(fxPath)));
			element.sendKeys(fText);
			element.sendKeys(Keys.RETURN);
			

		} catch (NoSuchElementException e) {
			//log something
			System.out.println("Send Keys failed");
			System.out.println("Error is " + e);
			testPass = false;
			
		}
	}

	public static void click_element(WebDriver mD, String fxPath) throws Exception {
		try {
			System.out.println("Click element: " + fxPath);
			WebElement element = (new WebDriverWait(mD, 30))
					.until(ExpectedConditions.elementToBeClickable(objmap
							.getLocator(fxPath)));
			element.click();

		} catch (NoSuchElementException e) {
			// log something
			System.out.println("Click on element failed");
			System.out.println("Error is " + e);
			testPass = false;
			
		}
	}

	public static void click_link(WebDriver mD, String fxPath) throws Exception {
		try {
			System.out.println("Click link: " + fxPath);
			WebElement element = (new WebDriverWait(mD, 30))
					.until(ExpectedConditions.presenceOfElementLocated(objmap
							.getLocator(fxPath)));
			element.click();

			// log something
		} catch (NoSuchElementException e) {
			// log something
			System.out.println("Click link failed");
			System.out.println("Error is " + e);
			testPass = false;
			
		}
	}

	public static String get_text(WebDriver mD, String fxPath) throws Exception {
		try {
			WebElement element = (new WebDriverWait(mD, 30))
					.until(ExpectedConditions.presenceOfElementLocated(objmap
							.getLocator(fxPath)));

			return element.getText();
			// log something
		} catch (NoSuchElementException e) {
			// log something
			System.out.println("Text element not found");
			System.out.println("Error is " + e);
			testPass = false;
			return "Fail";
			
		}

	}

	public static void verify_text(WebDriver mD, String fxPath, String fText)
			throws Exception {
		try {
			System.out.println("Veryfy Text for : " + fxPath + " with text: "
					+ fText);
			WebElement element = (new WebDriverWait(mD, 30))
					.until(ExpectedConditions.presenceOfElementLocated(objmap
							.getLocator(fxPath)));

			String fTextOut = element.getText();
			if (fTextOut.equals(fText)) {
				System.out.println("Text element matching");
				System.out.println("Actual Text was: " + get_text(mD, fxPath));
			} else {
				testPass = false;
				System.out.println("Text element not matching");
				System.out.println("Actual Text was: " + get_text(mD, fxPath));
			}
		} catch (NoSuchElementException e) {
			System.out.println("Text element not found");
			System.out.println("Error is " + e);
			testPass = false;
		}
	}

	public static void element_present(WebDriver mD, String fxPath) {
		try {
			if (mD.findElement(By.xpath(fxPath)).isDisplayed()) {
				// log something
			} else {
				testPass = false;
			}
		} catch (NoSuchElementException e) {
			System.out.println("Element not found");
			System.out.println("Error is " + e);
			testPass = false;
		}
	}

	public static void link_present(WebDriver mD, String fText) {
		try {
			if (mD.findElement(By.linkText(fText)).isDisplayed()) {
				// log something
			} else {
				testPass = false;
				
			}
		} catch (NoSuchElementException e) {
			System.out.println("Element not found");
			System.out.println("Error is " + e);
			testPass = false;
			
		}
	}

	public static String get_attribute(WebDriver mD, String fxPath,
			String fAttribute) {
		return mD.findElement(By.xpath(fxPath)).getAttribute(fAttribute);
	}

	public static String verify_attribute(WebDriver mD, String fxPath,
			String fAttribute, String fText) {
		String fTextOut = mD.findElement(By.xpath(fxPath)).getAttribute(
				fAttribute);
		if (fTextOut.equals(fText)) {
			return "Pass";
		} else {
			return "Fail";
		}
	}

	public static void close_browser(WebDriver mD) {
		System.out.println("Closing Browser");	
		mD.close();
		mD.quit();
	}

	public static void wait_time(long i) throws Exception {
		System.out.println("Waiting" + i);
		Thread.sleep(i);
	}

	public static void xlTSRead(String sPath, String testKey) throws Exception {
		File myxl = new File(sPath);
		FileInputStream myStream = new FileInputStream(myxl);

		HSSFWorkbook myWB = new HSSFWorkbook(myStream);
		HSSFSheet mySheet = myWB.getSheet(testKey); // Referring to 1st sheet

		xTSRows = mySheet.getLastRowNum() + 1;
		xTSCols = mySheet.getRow(0).getLastCellNum();
		xTSdata = new String[xTSRows][xTSCols];
		for (int i = 0; i < xTSRows; i++) {
			HSSFRow row = mySheet.getRow(i);
			for (int j = 0; j < xTSCols; j++) {
				HSSFCell cell = row.getCell((short) j); // To read value from
														// each col in each row
				String value = cellToString(cell);
				xTSdata[i][j] = value;
			}
		}
	}

	public static void xlTCRead(String sPath, String testKey) throws Exception {
		File myxl = new File(sPath);
		FileInputStream myStream = new FileInputStream(myxl);

		HSSFWorkbook myWB = new HSSFWorkbook(myStream);
		HSSFSheet mySheet = myWB.getSheet(testKey); // Referring to 1st sheet
		xTCRows = mySheet.getLastRowNum() + 1;
		xTCCols = mySheet.getRow(0).getLastCellNum();
		xTCdata = new String[xTCRows][xTCCols];
		for (int i = 0; i < xTCRows; i++) {
			HSSFRow row = mySheet.getRow(i);
			for (int j = 0; j < xTCCols; j++) {
				HSSFCell cell = row.getCell((short) j); // To read value from
														// each col in each row
				String value = cellToString(cell);
				xTCdata[i][j] = value;
			}
		}
	}

	public static String cellToString(HSSFCell cell) {
		// This function will convert an object of type excel cell to a string
		// value

		int type = cell.getCellType();
		Object result;

		switch (type) {
		case Cell.CELL_TYPE_NUMERIC: // 0
			result = cell.getNumericCellValue();
			break;
		case Cell.CELL_TYPE_STRING: // 1
			result = cell.getStringCellValue();
			break;
		case Cell.CELL_TYPE_FORMULA: // 2
			throw new RuntimeException("We can't evaluate formulas in Java");
		case Cell.CELL_TYPE_BLANK: // 3
			result = "-";
			break;
		case Cell.CELL_TYPE_BOOLEAN: // 4
			result = cell.getBooleanCellValue();
			break;
		case Cell.CELL_TYPE_ERROR: // 5
			throw new RuntimeException("This cell has an error");
		default:
			throw new RuntimeException("We don't support this cell type: "
					+ type);
		}
		return result.toString();
	}
	
	public static String JIRA_GetTestID(String TestKey) throws Exception {
		
		//Object result = null;
		String GetTestIDData = "jql=key=" + TestKey + "&fields=id";
		String getResponse = invokeGetMethod(auth, BASE_URL	+ "/rest/api/latest/search?" + GetTestIDData);
		JSONObject jobj=new JSONObject(getResponse);
        String c = jobj.getString("issues");         
        JSONArray jArray = new JSONArray(c);
        String TestID=jArray.getJSONObject(0).getString("id");
        return TestID.toString();
	}
		

	public static String JIRA_NewCycle() throws Exception {
		Object result = null;
		String timestamp = new java.text.SimpleDateFormat("yyyyMMddhhmmss")
				.format(new Date());
		String todayDate = new java.text.SimpleDateFormat("d/MMM/yy")
				.format(new Date());
		String createIssueData = "{\"name\":\"Automated Cycle - "
				+ timestamp
				+ "\",\"description\":\"Cycle generated by automation\",\"startDate\": \""
				+ todayDate + "\",\"endDate\": \"" + todayDate
				+ "\",\"projectId\": \"" + projectId + "\",\"versionId\": \""
				+ versionId + "\"}";
		String issue = invokePostMethod(auth, BASE_URL
				+ "/rest/zapi/latest/cycle", createIssueData);
		System.out.println("New Cycle Created: " + issue);
		JSONObject issueObj = new JSONObject(issue);
		result = issueObj.getString("id");
		// String cycleId = issueObj.getString("id");
		// System.out.println("Generated cycle ID:"+cycleId);

		return result.toString();
	}

	public static String JIRA_NewExecution(String cycleId, String testId)
			throws Exception {
		//Object result = null;

		String createExecutionData = "{\"issueId\": \"" + testId
				+ "\",\"versionId\": \"" + versionId + "\",\"cycleId\": \""
				+ cycleId + "\",\"projectId\": \"" + projectId
				+ "\",\"executionStatus\": 1}";
		String execution = invokePostMethod(auth, BASE_URL
				+ "/rest/zapi/latest/execution", createExecutionData);
		 System.out.println("New Execution Created: " + execution);
		JSONObject executionObj = new JSONObject(execution);
		// Object is nested with an arbitrary key, return the first (and
		// hopefully only) key
		// value pair
		//This needs to be fixes in the future
		String nestedObjectKey = "";
		@SuppressWarnings("rawtypes")
		Iterator keyIterator = executionObj.keys();
		while (keyIterator.hasNext()) {
			nestedObjectKey = (String) keyIterator.next();
			System.out.println("Found nestedOject key: " + nestedObjectKey);
		}

		JSONObject nestedExecutionObj = executionObj
				.getJSONObject(nestedObjectKey);
		String executionId = nestedExecutionObj.getString("id");
		System.out.println("Generated execution ID:" + executionId);
		return executionId.toString();
	}

	public static void JIRA_ExecuteTest(String executionId, Boolean testPass)
			throws Exception {
		int executeStatus;
		if (testPass.equals(false)) {
			executeStatus = 2;
		} else {
			executeStatus = 1;
		}
		String ExecuteTestData = "{\"status\": \"" + executeStatus + "\"}";
		invokePostMethod(auth, BASE_URL
				+ "/rest/zapi/latest/execution/" + executionId
				+ "/quickExecute", ExecuteTestData);
		System.out.println("Test Executed Successfully:");
		//JSONObject executeObj = new JSONObject(execute);

	}

	public static String JIRA_CreateBug(String testId, String vDescription,
			String projectId) throws Exception {

		String createIssueData = "{\"fields\":{\"project\":{\"id\":"
				+ projectId
				+ "},\"summary\": \""
				+ testId
				+ "-"
				+ vDescription
				+ "\",\"issuetype\":{\"name\":\"Bug\"},\"timetracking\":{\"originalEstimate\":\"1d 0h\"}}}";
		String issue = invokePostMethod(auth, BASE_URL + "/rest/api/2/issue",
				createIssueData);
		System.out.println(issue);
		JSONObject issueObj = new JSONObject(issue);
		// issueId = issueObj.getLong("issueId");
		String newKey = issueObj.getString("key");
		return newKey;
	}
	
	
	public static void JIRA_LinkBug(String executionId, String defectKey)
			throws Exception {

		String createUpdateData = "{\"executions\": [\"" + executionId
				+ "\"],\"defects\":[\"" + defectKey
				+ "\"],\"detailedResponse\": false}";
		String update = invokePutMethod(auth, BASE_URL
				+ "/rest/zapi/latest/execution/updateWithBulkDefects",
				createUpdateData);
		System.out.println("Jira Linked to Bug: " + update);
		//JSONObject updateObj = new JSONObject(update);
		
		
	}
	
	private static String invokeGetMethod(String auth, String url)
			throws AuthenticationException, ClientHandlerException {
		
		Client client = Client.create();
		WebResource webResource = client.resource(url);
		System.out.println("Ready to GET!!!!!!");
		ClientResponse response = webResource
				.header("Authorization", "Basic " + auth)
				.type("application/json").accept("application/json")
				.get(ClientResponse.class);
		int statusCode = response.getStatus();
		if (statusCode == 401) {
			throw new AuthenticationException("Invalid Username or Password");
		}
		return response.getEntity(String.class);
	}
	
	private static String invokePostMethod(String auth, String url, String data)
			throws AuthenticationException, ClientHandlerException {
		
		Client client = Client.create();
		WebResource webResource = client.resource(url);
		ClientResponse response = webResource
				.header("Authorization", "Basic " + auth)
				.type("application/json").accept("application/json")
				.post(ClientResponse.class, data);
		int statusCode = response.getStatus();
		if (statusCode == 401) {
			throw new AuthenticationException("Invalid Username or Password");
		}
		return response.getEntity(String.class);
	}

	private static String invokePutMethod(String auth, String url, String data)
			throws AuthenticationException, ClientHandlerException {
		Client client = Client.create();
		WebResource webResource = client.resource(url);
		ClientResponse response = webResource
				.header("Authorization", "Basic " + auth)
				.type("application/json").accept("application/json")
				.put(ClientResponse.class, data);
		int statusCode = response.getStatus();
		if (statusCode == 401) {
			throw new AuthenticationException("Invalid Username or Password");
		}
		return response.getEntity(String.class);
	}

	
	//Read runsheet array
	public static ArrayList<String> get_arraylist_from_file(File f)
			throws FileNotFoundException {
		Scanner s;
		ArrayList<String> list = new ArrayList<String>();
		s = new Scanner(f);
		while (s.hasNext()) {
			list.add(s.next());
		}
		s.close();
		return list;
	}
	
	
	public static void Setup_Webdriver() throws Exception {
		//TODO Get CHROME, IE and Android working
		if (vBrowser.equals("IE")) {
	
			DesiredCapabilities capability = DesiredCapabilities.internetExplorer();
			capability.setCapability(InternetExplorerDriver.IGNORE_ZOOM_SETTING, true);
	        myD =  new RemoteWebDriver(new URL("http://localhost:1234/wd/hub"), capability);
			//myD = new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), capability);	
		} else if (vBrowser.equals("Chrome")) {
			
			DesiredCapabilities capability = DesiredCapabilities.chrome();
			myD = new RemoteWebDriver(new URL("http://localhost:1234/wd/hub"), capability);	
		} else
		if (vBrowser.equals("Firefox")) {	
			if(OS.contains("Windows")) {
				DesiredCapabilities capability = DesiredCapabilities.firefox();
				//myD = new RemoteWebDriver(new URL("http://localhost:1234/wd/hub"), capability);	
				myD = new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), capability);
			}/* else if(OS.equals("Windows XP")) {
			
				DesiredCapabilities capability = DesiredCapabilities.firefox();
				//myD = new RemoteWebDriver(new URL("http://localhost:1234/wd/hub"), capability);	
				myD = new RemoteWebDriver(new URL("http://localhost:4444/wd/hub"), capability);
			}*/ 
		}
	}
	public static void keyword_executor(String vKeyword) throws Exception {

		
		if ((vKeyword).equals("navigate_to")) {
			
			vBrowser = vIP2;
			System.out.println("Browser for testing is: " + vIP2);
			//setup webdriver
			Setup_Webdriver();
			
			navigate_to(myD, vIP1);
		}
		
		else if ((vKeyword).equals("send_keys")){
			send_keys(myD, vIP1, vIP2);
		}
		
		else if ((vKeyword).equals("click_element")){
			click_element(myD, vIP1);
		}
		
		else if ((vKeyword).equals("click_link")){
			click_link(myD, vIP1);
		}	
			
		else if ((vKeyword).equals("get_text")){
			get_text(myD, vIP1);
		}
			
		else if ((vKeyword).equals("verify_text")){
			verify_text(myD, vIP1, vIP2);
		}
		else if ((vKeyword).equals("wait_time")){
		wait_time(Long.parseLong(
				Integer.toString((int) Double.parseDouble(vIP2)), 10));
		}
		else if ((vKeyword).equals("close_browser")){
			close_browser(myD);
		}
		else {
			
			System.out.println("Keyword not found");
			testPass = false;
		}
		
		
		//TODO code other keywords when required	
		/*case "get_attribute":
			get_attribute(myD, vIP1, vIP2);
			break;
		case "verify_attribute":
			verify_attribute(myD, vIP1, vIP2, vIP3);
			break;
		
			break;
		case "element_present":
			element_present(myD, vIP1);
			break;
		case "link_present":
			link_present(myD, vIP1);
			break;
		*/	
			
		
	}

	public static void FailedTest(String testId, String vDescription)
			throws Throwable {

		// take the screenshot at the end of every test
		File scrFile = ((TakesScreenshot) myD).getScreenshotAs(OutputType.FILE);
		// now save the screenshot to a file some place
		try {
			String timestamp = new java.text.SimpleDateFormat("yyyyMMddHHmmss")
					.format(new Date());
			//TODO think about where the temp images are stored. They need to be removed after test completes
			newfileName = testId + "-" + vDescription + "-"	+ timestamp + ".png";
			newFile = "c:\\tmp\\" + newfileName;
			FileUtils.copyFile(scrFile, new File(newFile));

			// Create Defect. Link to execution
			defectKey = JIRA_CreateBug(testId, vDescription, projectId);

			// link defect to execution
			 JIRA_LinkBug(executionId, defectKey);

			// Attach screenshot to defect
			addAttachment(defectKey, newFile);// , newFile);
			close_browser(myD);
			myD.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	
	// Adds the screenshot to the defect
	//public static void addAttachment(String defectKey, String path)
	public static void addAttachment(String defectKey, String path)
			throws Throwable {
		URL url = new URL(BASE_URL); // Some instantiated URL object
		URI serverUri = url.toURI();

		//TODO move these usernames and passwords out and to the config file as a base64 value
		String username = "test.project.officer";
		String password = "test.project.officer";
		FileInputStream fileStreamPath = new FileInputStream(path);

		System.out.println("Server Url  :" + serverUri);
		final JiraRestClient restClient = new AsynchronousJiraRestClientFactory()
				.createWithBasicHttpAuthentication(serverUri, username,
						password);
		System.out.println("Attach file to defect:" + defectKey);
		restClient.getIssueClient().getIssue(defectKey);
		final java.net.URI AttachmentUri = new java.net.URI(serverUri
				+ "/rest/api/2/issue/" + defectKey + "/attachments");
		restClient.getIssueClient().addAttachment(AttachmentUri,
				fileStreamPath, newfileName);
		System.out.println("Attachment Successful");
		
		//remove the tmp file stored
		FileUtils.forceDelete(new File(newFile));
		System.out.println("Delete file: " + newFile);
	}

	
	public static WebDriver setupChromeDriver() {
		   
		      System.setProperty("webdriver.chrome.driver", Main.class.getResource(WINDOWS_CHROMEDRIVER).getFile());
		 
		   ChromeOptions options = new ChromeOptions();
		   options.addArguments("--start-maximized");
		   options.addArguments("--ignore-certificate-errors");
		   return new ChromeDriver(options);
		}
	
	
	public static WebDriver setupIEDriver() {
		   
	      
	   System.setProperty("webdriver.ie.driver",Main.class.getResource(WINDOWS_IEDRIVER).getFile());

	        DesiredCapabilities capab = DesiredCapabilities.internetExplorer();
	        capab.setCapability(InternetExplorerDriver.IGNORE_ZOOM_SETTING, true);

	        return new InternetExplorerDriver(capab);
 
	   
	}
	
}
