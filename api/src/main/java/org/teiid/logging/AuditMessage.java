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

package org.teiid.logging;

import java.util.Arrays;

import org.teiid.CommandContext;
import org.teiid.adminapi.Session;

/**
 * Log format for auditing.
 */
public class AuditMessage {
	
	/**
	 * Contains information related to a logon attempt
	 */
	public static class LogonInfo {

		private final String vdbName;
		private final String vdbVersion;
		private final String authType;
		private final String userName;
		private final String applicationName;

		public LogonInfo(String vdbName, String vdbVersion,
				String authType, String userName,
				String applicationName) {
			this.vdbName = vdbName;
			this.vdbVersion = vdbVersion;
			this.authType = authType;
			this.userName = userName;
			this.applicationName = applicationName;
		}
		
		public String getVdbName() {
			return vdbName;
		}
		
		public String getVdbVersion() {
			return vdbVersion;
		}
		
		public String getAuthType() {
			return authType;
		}
		
		public String getUserName() {
			return userName;
		}
		
		public String getApplicationName() {
			return applicationName;
		}
		
		@Override
		public String toString() {
			StringBuffer msg = new StringBuffer();
	        msg.append( vdbName );
	        msg.append(", "); //$NON-NLS-1$
	        msg.append( vdbVersion );
	        msg.append(' ');
	        msg.append( userName );
	        msg.append(' ');
	        msg.append(authType);
	        return msg.toString();
		}
		
	}
	
	private String context;
	private String activity;
	
	private LogonInfo logonInfo;
	private Exception exception;
	
	private Session session;
	
	private String[] resources;
	private CommandContext commandContext;

	public AuditMessage(String context, String activity, String[] resources, CommandContext commandContext) {
	    this.context = context;
	    this.activity = activity;
	    this.resources = resources;
	    this.commandContext = commandContext;
	}
	
	public AuditMessage(String context, String activity, LogonInfo info, Exception e) {
		this.context = context;
		this.activity = activity;
		this.logonInfo = info;
		this.exception = e;
	}
	
	public AuditMessage(String context, String activity, Session session) {
		this.context = context;
		this.activity = activity;
		this.session = session;
	}
	
	/**
	 * The related {@link LogonInfo} only if this is a logon related event
	 * @return
	 */
	public LogonInfo getLogonInfo() {
		return logonInfo;
	}
	
	/**
	 * The {@link Session} for the event or null if one has not been established. 
	 * @return
	 */
	public Session getSession() {
		if (this.commandContext != null) {
			return this.commandContext.getSession();
		}
		return session;
	}

    public String getContext() {
        return this.context;
    }

    public String getActivity() {
        return this.activity;
    }

    /**
     * The user name or null if the session has not yet been established.
     * @return
     */
    public String getPrincipal() {
    	Session s = getSession();
    	if (s != null) {
    		return s.getUserName();
    	}
    	return null; 
    }

    /**
     * The list of relevant resources for the audit event.
     * Will be null for logon/logoff events.
     * @return
     */
	public String[] getResources() {
		return this.resources;
	}
	
	public CommandContext getCommandContext() {
		return commandContext;
	}
	
	/**
	 * The exception associated with a failed logon attempt.
	 * @return
	 */
	public Exception getException() {
		return exception;
	}
	
	public String toString() {
        StringBuffer msg = new StringBuffer();
        if (this.commandContext != null) {
        	msg.append( this.commandContext.getRequestId());
        } 
        msg.append(" ["); //$NON-NLS-1$
        if (this.logonInfo != null) {
        	msg.append(this.logonInfo);
        } else {
        	msg.append( getPrincipal() );
        }
        msg.append("] <"); //$NON-NLS-1$
        msg.append( getContext() );
        msg.append('.');
        msg.append( getActivity() );
        msg.append("> "); //$NON-NLS-1$
        if (resources != null) {
        	msg.append( Arrays.toString(resources) );
        }
        return msg.toString();
	}

}
