/*
 * ${license}
 */
package org.teiid.resource.adapter.couchdb;

import javax.resource.ResourceException;
import javax.resource.spi.InvalidPropertyException;

import org.teiid.resource.spi.BasicConnectionFactory;
import org.teiid.resource.spi.BasicManagedConnectionFactory;

public class couchdbManagedConnectionFactory extends BasicManagedConnectionFactory {
	
	@Override
	public BasicConnectionFactory<couchdbConnectionImpl> createConnectionFactory() throws ResourceException {
		return new BasicConnectionFactory<couchdbConnectionImpl>() {
			@Override
			public couchdbConnectionImpl getConnection() throws ResourceException {
				return new couchdbConnectionImpl(couchdbManagedConnectionFactory.this);
			}
		};
	}	
	
	// ra.xml files getters and setters go here.

}
