package org.olat.core.configuration;

import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

public class MyPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer {
	
	@Override
	protected String convertProperty(String propertyName, String propertyValue) {
		
		System.out.println("propertyName: "+ propertyName);
		System.out.println("propertyValue: "+ propertyValue);
		
		return super.convertProperty(propertyName, propertyValue);
	}

}
