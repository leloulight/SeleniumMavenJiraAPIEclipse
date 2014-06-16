package com.AutomationFramework;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.openqa.selenium.By;

public class ObjectMap {

	Properties prop = new Properties();

	public ObjectMap(String mapFile) {
		try {
			System.out.println("Loading Object Map");
			FileInputStream Master = new FileInputStream(mapFile);
			prop.load(Master);
			System.out.println("Object Map Loaded Successfully");
		} catch (IOException e) {
			System.out.println("Object Map Loading Error");
			e.printStackTrace();
		}
	}

	public By getLocator(String ElementName) throws Exception {
		// Read value using the logical name as Key

		String locator = prop.getProperty(ElementName);
		// Split the value which contains locator type and locator value

		String locatorType = locator.split("<->")[0];
		System.out.println("locatorType: " + locatorType);
		String locatorValue = locator.split("<->")[1];
		System.out.println("locatorValue: " + locatorValue);
		// Return a instance of By class based on type of locator
		if (locatorType.toLowerCase().equals("id"))
			return By.id(locatorValue);
		else if (locatorType.toLowerCase().equals("name"))
			return By.name(locatorValue);
		else if ((locatorType.toLowerCase().equals("classname"))
				|| (locatorType.toLowerCase().equals("class")))
			return By.className(locatorValue);
		else if ((locatorType.toLowerCase().equals("tagname"))
				|| (locatorType.toLowerCase().equals("tag")))
			return By.className(locatorValue);
		else if ((locatorType.toLowerCase().equals("linktext"))
				|| (locatorType.toLowerCase().equals("link")))
			return By.linkText(locatorValue);
		else if (locatorType.toLowerCase().equals("partiallinktext"))
			return By.partialLinkText(locatorValue);
		else if ((locatorType.toLowerCase().equals("cssselector"))
				|| (locatorType.toLowerCase().equals("css")))
			return By.cssSelector(locatorValue);
		else if (locatorType.toLowerCase().equals("xpath"))
			return By.xpath(locatorValue);
		else
			throw new Exception("Locator type '" + locatorType
					+ "' not defined!!");
	}
}
