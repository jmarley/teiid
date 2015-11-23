/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */
package org.teiid.translator.mongodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.teiid.CommandContext;
import org.teiid.cdk.api.TranslationUtility;
import org.teiid.core.util.ObjectConverterUtil;
import org.teiid.core.util.UnitTestUtil;
import org.teiid.language.Command;
import org.teiid.mongodb.MongoDBConnection;
import org.teiid.query.metadata.TransformationMetadata;
import org.teiid.query.unittest.RealMetadataFactory;
import org.teiid.translator.Execution;
import org.teiid.translator.ExecutionContext;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;

@SuppressWarnings("nls")
@Ignore
public class TestEmbeddedMongoExecution {
    private static MongoDBExecutionFactory translator;
    private static TranslationUtility utility;
    private static EmbeddedMongoDB mongodb;
    private MongoClient client;
    private MongoDBConnection connection;
    
    @BeforeClass
    public static void setUp() throws Exception {
    	translator = new MongoDBExecutionFactory();
    	translator.start();

    	TransformationMetadata metadata = RealMetadataFactory.fromDDL(ObjectConverterUtil.convertFileToString(UnitTestUtil.getTestDataFile("test.ddl")), "test", "dummy");
    	utility = new TranslationUtility(metadata);
    	
    	mongodb = new EmbeddedMongoDB();
    }
    
    @Before
    public void beforeTest() throws Exception {
        this.client = new MongoClient("localhost", 12345);
        this.connection = getConnection(client);
    }
    
    @After
    public void afterTest() throws Exception {
        this.connection.close();
        this.client.close();        
    }

	private static MongoDBConnection getConnection(MongoClient client) {
		MongoDBConnection connection = Mockito.mock(MongoDBConnection.class);
    	Mockito.stub(connection.getDatabase()).toReturn(client.getDB("test"));
    	return connection;
	}
    
    public static void stop() {
    	mongodb.stop();
    }
    
	private Execution executeCmd(String sql) throws Exception {
		Command cmd = utility.parseCommand(sql);
		CommandContext cc = Mockito.mock(CommandContext.class);
		Mockito.stub(cc.isReturnAutoGeneratedKeys()).toReturn(false);
		ExecutionContext ec = Mockito.mock(ExecutionContext.class);
		Mockito.stub(ec.getCommandContext()).toReturn(cc);
    	Execution exec =  translator.createExecution(cmd, ec, utility.createRuntimeMetadata(), this.connection);
    	exec.execute();
    	return exec;
	}     
	
	
    @Test
    public void testSingleTableExecution() throws Exception {
    	executeCmd("delete from G1");
    	executeCmd("insert into G1 (e1, e2, e3) values (1, 1, 1)");
    	executeCmd("insert into G1 (e1, e2, e3) values (2, 2, 2)");
    	executeCmd("insert into G1 (e1, e2, e3) values (3, 3, 3)");
    	
    	MongoDBQueryExecution exec = (MongoDBQueryExecution)executeCmd("select * from G1");
    	assertEquals(Arrays.asList(1, 1, 1), exec.next());
    	assertEquals(Arrays.asList(2, 2, 2), exec.next());
    	assertEquals(Arrays.asList(3, 3, 3), exec.next());
    	assertNull(exec.next());

    	executeCmd("update G1 set e2=4 where e3 >= 2");
    	exec = (MongoDBQueryExecution)executeCmd("select * from G1");
    	assertEquals(Arrays.asList(1, 1, 1), exec.next());
    	assertEquals(Arrays.asList(2, 4, 2), exec.next());
    	assertEquals(Arrays.asList(3, 4, 3), exec.next());
    	assertNull(exec.next());

    	executeCmd("delete from G1 where e2=4");
    	exec = (MongoDBQueryExecution)executeCmd("select * from G1");
    	assertEquals(Arrays.asList(1, 1, 1), exec.next());
    	assertNull(exec.next());    	
    }	

    @Test
    public void testOne2OneMerge() throws Exception {
    	executeCmd("delete from G1");
    	executeCmd("insert into G1 (e1, e2, e3) values (1, 1, 1)");
    	executeCmd("insert into G2 (e1, e2, e3) values (1, 2, 3)");
    	MongoDBQueryExecution exec = (MongoDBQueryExecution)executeCmd("select * from G2");
    	assertEquals(Arrays.asList(1, 2, 3), exec.next());
    	assertNull(exec.next());
    	
    	MongoClient client = new MongoClient("localhost", 12345);
    	assertNull(client.getDB("test").getCollection("G2").findOne());
    	
    	BasicDBObject row = new BasicDBObject("_id", 1).append("e2", 1).append("e3", 1);
    	row.append("G2", new BasicDBObject("e2", 2).append("e3", 3));    
    	assertEquals(row, client.getDB("test").getCollection("G1").findOne());

    	exec = (MongoDBQueryExecution)executeCmd("select g1.e1, g1.e2, g2.e3 from G1 JOIN G2 ON G1.e1=G2.e1");
    	assertEquals(Arrays.asList(1, 1, 3), exec.next());
    	assertNull(exec.next());
    	
    	client.close();
    }
    
    @Test
    public void testOne2ManyMerge() throws Exception {
    	executeCmd("delete from G1");
    	executeCmd("insert into G1 (e1, e2, e3) values (1, 1, 1)");
    	executeCmd("insert into G3 (e1, e2, e3) values (2, 1, 3)");
    	MongoDBQueryExecution exec = (MongoDBQueryExecution)executeCmd("select * from G3");
    	assertEquals(Arrays.asList(2, 1, 3), exec.next());
    	assertNull(exec.next());
    	
    	MongoClient client = new MongoClient("localhost", 12345);
    	assertNull(client.getDB("test").getCollection("G3").findOne());
    	
    	BasicDBObject row = new BasicDBObject("_id", 1).append("e2", 1).append("e3", 1);
    	BasicDBList list = new BasicDBList();
    	list.add(new BasicDBObject("_id", 2).append("e3", 3));
    	row.append("G3", list);    
    	assertEquals(row, client.getDB("test").getCollection("G1").findOne());

    	exec = (MongoDBQueryExecution)executeCmd("select G1.e1, G1.e2, G3.e2, G3.e3 from G1 JOIN G3 ON G1.e1=G3.e2");
    	assertEquals(Arrays.asList(1, 1, 1, 3), exec.next());
    	assertNull(exec.next());
    	
    	executeCmd("update G3 set e3=4 where e2=1");    	
    	exec = (MongoDBQueryExecution)executeCmd("select G1.e1, G1.e2, G3.e2, G3.e3 from G1 JOIN G3 ON G1.e1=G3.e2");
    	assertEquals(Arrays.asList(1, 1, 1, 4), exec.next());
    	assertNull(exec.next());
    	
    	executeCmd("delete from G3 where G3.e2=1");

    	exec = (MongoDBQueryExecution)executeCmd("select G1.e1, G1.e2, G3.e2, G3.e3 from G1 JOIN G3 ON G1.e1=G3.e2");
    	assertNull(exec.next());
    	
    	client.close();
    }    
    
    @Test
    public void testEmbedded() throws Exception {
    	executeCmd("delete from G1");    	
    	executeCmd("insert into G4 (e1, e2, e3) values (2, 2, 3)");
    	executeCmd("insert into G1E (e1, e2, e3, e4) values (2, 2, 2, 2)");    	
    	MongoDBQueryExecution exec = (MongoDBQueryExecution)executeCmd("select * from G4");
    	assertEquals(Arrays.asList(2, 2, 3), exec.next());
    	assertNull(exec.next());
    	
    	BasicDBObject g4_row = new BasicDBObject("_id", 2).append("e2", 2).append("e3", 3);
    	MongoClient client = new MongoClient("localhost", 12345);
    	assertEquals(g4_row, client.getDB("test").getCollection("G4").findOne());
    	
    	BasicDBObject row = new BasicDBObject("_id", 2).append("e2", 2).append("e3", 2).append("e4", 2);
    	row.append("G4",new BasicDBObject("e2", 2).append("e3", 3));    
    	assertEquals(row, client.getDB("test").getCollection("G1E").findOne());

    	exec = (MongoDBQueryExecution)executeCmd("select G1E.e1, G1E.e2, G4.e3 from G1E JOIN G4 ON G1E.e1=G4.e1");
    	assertEquals(Arrays.asList(2, 2, 3), exec.next());
    	assertNull(exec.next());
    	
    	client.close();
    }    
}
