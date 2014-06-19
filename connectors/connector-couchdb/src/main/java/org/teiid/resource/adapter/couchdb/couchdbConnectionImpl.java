/*
 * ${license}
 */
package org.teiid.resource.adapter.couchdb;


import javax.resource.ResourceException;

import org.teiid.resource.spi.BasicConnection;

/**
 * Connection to the resource. You must define couchdbConnection interface, that 
 * extends the "javax.resource.cci.Connection"
 */
public class couchdbConnectionImpl extends BasicConnection implements couchdbConnection {

    private couchdbManagedConnectionFactory config;

    public couchdbConnectionImpl(couchdbManagedConnectionFactory env) {
        this.config = env;
        // todo: connect to your source here
    }
    
    @Override
    public void close() {
    	
    }
}
