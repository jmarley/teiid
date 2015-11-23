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

package org.teiid.query.function.aggregate;

import java.util.List;

import org.teiid.core.TeiidComponentException;
import org.teiid.core.TeiidProcessingException;
import org.teiid.core.types.ClobType;
import org.teiid.query.function.JSONFunctionMethods.JSONBuilder;
import org.teiid.query.util.CommandContext;

/**
 * Aggregates XML entries
 */
public class JSONArrayAgg extends SingleArgumentAggregateFunction {

	private ClobType result;
	private JSONBuilder concat;
    
    public JSONArrayAgg() {
	}

    public void reset() {
    	concat = null;
    	result = null;
    }

    /**
     * @throws TeiidProcessingException 
     * @throws TeiidComponentException 
     * @see org.teiid.query.function.aggregate.AggregateFunction#addInputDirect(List, CommandContext, CommandContext)
     */
    public void addInputDirect(Object input, List<?> tuple, CommandContext commandContext) throws TeiidComponentException, TeiidProcessingException {
    	if (concat == null) {
    		concat = new JSONBuilder(commandContext.getBufferManager());
    		concat.start(true);
    	}
    	concat.addValue(input);
    }

    /**
     * @throws TeiidProcessingException 
     * @throws TeiidComponentException 
     * @see org.teiid.query.function.aggregate.AggregateFunction#getResult(CommandContext)
     */
    public Object getResult(CommandContext commandContext) throws TeiidComponentException, TeiidProcessingException {
    	if (result == null) {
    		if (concat == null) {
        		return null;
    		}
    		concat.end(true);
    		result = concat.close(commandContext);
    		concat = null;
    	}
        return result;
    }

}
