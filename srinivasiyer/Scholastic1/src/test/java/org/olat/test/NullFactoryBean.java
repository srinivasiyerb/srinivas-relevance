package org.olat.test;

import org.springframework.beans.factory.FactoryBean;

/**
 * Description:<br>
 * use this bean if you want to reference an null value as a bean class value. If you like to set null as value try the <null/> bean.
 * <P>
 * Initial Date: 03.05.2010 <br>
 * 
 * @author guido
 */
public class NullFactoryBean implements FactoryBean<Void> {

	@Override
	public Void getObject() throws Exception {
		return null;
	}

	@Override
	public Class<? extends Void> getObjectType() {
		return null;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
