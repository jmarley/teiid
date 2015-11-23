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
package org.teiid.translator.prestodb;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.teiid.metadata.BaseColumn.NullType;
import org.teiid.metadata.Column;
import org.teiid.metadata.MetadataFactory;
import org.teiid.metadata.Table;
import org.teiid.translator.MetadataProcessor;
import org.teiid.translator.TranslatorException;
import org.teiid.translator.TypeFacility;
import org.teiid.translator.jdbc.JDBCMetdataProcessor;

public class PrestoDBMetadataProcessor extends JDBCMetdataProcessor implements MetadataProcessor<Connection>{

	private boolean trimColumnNames;

    @Override
	public void process(MetadataFactory metadataFactory, Connection conn)	throws TranslatorException {
		try {
		    getConnectorMetadata(conn, metadataFactory);
        } catch (SQLException e) {
            throw new TranslatorException(e);
        }
	}
    
    @Override
    public void getConnectorMetadata(Connection conn, MetadataFactory metadataFactory)
            throws SQLException {
        
        List<String> catalogs = getCatalogs(conn);
        for (String catalog:catalogs) {
            if (getCatalog() != null && !getCatalog().equalsIgnoreCase(catalog)) {
                continue;
            }
            List<String> schemas = getSchema(conn, catalog);
            for (String schema:schemas) {
                if (getSchemaPattern() != null && !Pattern.matches(getSchemaPattern(), schema)) {
                    continue;
                }
            
                List<String> tables = getTables(conn, catalog, schema);
                for (String table:tables) {
                    if (shouldExclude(table)) {
                        continue;
                    }
                    addTable(table, conn, catalog, schema, metadataFactory);
                }
            }
        }
    }
    
    private List<String> getCatalogs(Connection conn) throws SQLException {
        ArrayList<String> catalogs = new ArrayList<String>();
        Statement stmt = conn.createStatement();
        ResultSet rs =  stmt.executeQuery("SHOW CATALOGS"); //$NON-NLS-1$
        while (rs.next()){
            catalogs.add(rs.getString(1));
        }
        rs.close();
        return catalogs;
    }   
    
    private List<String> getSchema(Connection conn, String catalog) throws SQLException {
        ArrayList<String> schemas = new ArrayList<String>();
        Statement stmt = conn.createStatement();
        ResultSet rs =  stmt.executeQuery("SHOW SCHEMAS FROM "+catalog); //$NON-NLS-1$
        while (rs.next()){
            schemas.add(rs.getString(1));
        }
        rs.close();
        return schemas;
    }     
    
	private List<String> getTables(Connection conn, String catalog, String schema) throws SQLException {
		ArrayList<String> tables = new ArrayList<String>();
		Statement stmt = conn.createStatement();
		ResultSet rs =  stmt.executeQuery("SHOW TABLES FROM "+catalog+"."+schema); //$NON-NLS-1$ //$NON-NLS-2$
		while (rs.next()){
			tables.add(rs.getString(1));
		}
		rs.close();
		return tables;
	}

	private String getRuntimeType(String type) {
	    
	    if (type.equalsIgnoreCase("boolean")) { //$NON-NLS-1$
            return TypeFacility.RUNTIME_NAMES.BOOLEAN;
        }	    
        else if (type.equalsIgnoreCase("bigint")) { //$NON-NLS-1$
            return TypeFacility.RUNTIME_NAMES.LONG;
        }
        else if (type.equalsIgnoreCase("double")) { //$NON-NLS-1$
            return TypeFacility.RUNTIME_NAMES.DOUBLE;
        }	    
        else if (type.equalsIgnoreCase("varchar")) { //$NON-NLS-1$
            return TypeFacility.RUNTIME_NAMES.STRING;
        }	    
        else if (type.equalsIgnoreCase("varbinary")) { //$NON-NLS-1$
            return TypeFacility.RUNTIME_NAMES.VARBINARY;
        }	    
        else if (type.equalsIgnoreCase("date")) { //$NON-NLS-1$
            return TypeFacility.RUNTIME_NAMES.DATE;
        }	    
        else if (type.equalsIgnoreCase("time") || type.equalsIgnoreCase("time with timezone")) { //$NON-NLS-1$ //$NON-NLS-2$
            return TypeFacility.RUNTIME_NAMES.TIME;
        }       
        else if (type.equalsIgnoreCase("timestamp") || type.equalsIgnoreCase("timestamp with timezone")) { //$NON-NLS-1$ //$NON-NLS-2$
            return TypeFacility.RUNTIME_NAMES.TIMESTAMP;
        }
        else if (type.equalsIgnoreCase("json")) { //$NON-NLS-1$
            return TypeFacility.RUNTIME_NAMES.BLOB;
        }
	    //TODO: Array, MAP, INETERVAL support.
		return TypeFacility.RUNTIME_NAMES.OBJECT;
	}

	private void addTable(String tableName, Connection conn, String catalog, String schema, MetadataFactory metadataFactory) throws SQLException {
		Table table = addTable(metadataFactory, null, null, tableName, null, tableName);		
		if (table == null) {
			return;
		}
		String nis = catalog+"."+schema+"."+tableName; //$NON-NLS-1$ //$NON-NLS-2$
		table.setNameInSource(nis); 
		
		Statement stmt = conn.createStatement();
		ResultSet rs =  stmt.executeQuery("SHOW COLUMNS FROM "+nis); //$NON-NLS-1$
		while (rs.next()){
			String name = rs.getString(1);
			if (this.trimColumnNames) {
				name = name.trim();
			}
			String type = rs.getString(2);
			if (type != null) {
				type = type.trim();
			}
			String runtimeType = getRuntimeType(type);
			
            NullType nt = Boolean.valueOf(rs.getString(3))?NullType.Nullable:NullType.No_Nulls;

			Column column = metadataFactory.addColumn(name, runtimeType, table);
			column.setNameInSource(name);
			column.setUpdatable(true);
		    column.setNullType(nt);
		}
		rs.close();
	}
}
