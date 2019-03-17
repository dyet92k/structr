/**
 * Copyright (C) 2010-2019 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.converter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.DatabaseService;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.cypher.CypherQueryHandler;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.GraphDatabaseCommand;

/**
 * Executes the given {@link CypherQueryConverter} on the current node and
 * returns the results.
 *
 *
 */
public class CypherQueryConverter extends PropertyConverter {

	private static final Logger logger = LoggerFactory.getLogger(CypherQueryConverter.class.getName());

	private DatabaseService graphDb    = null;
	private CypherQueryHandler handler = null;

	public CypherQueryConverter(SecurityContext securityContext, GraphObject entity, CypherQueryHandler handler) {

		super(securityContext, entity);

		this.handler = handler;

		try {
			graphDb = (DatabaseService)StructrApp.getInstance().command(GraphDatabaseCommand.class).execute();

		} catch(Throwable t) {

			logger.warn("Unable to create cypher execution engine.");
		}
	}

	@Override
	public Object convert(Object source) throws FrameworkException {
		return source;
	}

	@Override
	public Object revert(Object source) {

		if (currentObject != null) {

			Map<String, Object> parameters = new LinkedHashMap<>();
			String query                   = handler.getQuery();
			String name                    = currentObject.getProperty(AbstractNode.name);
			String uuid                    = currentObject.getProperty(GraphObject.id);

			// initialize parameters
			parameters.put("id",   uuid);
			parameters.put("uuid", uuid);
			parameters.put("name", name);

			// initialize query handler with security context
			handler.setSecurityContext(securityContext);

			try {

				List<AbstractNode> nodes = (List<AbstractNode>)handler.handleQueryResults(graphDb.execute(query, parameters));

				return nodes;


			} catch(FrameworkException fex) {

				logger.warn("Exception while executing cypher query {}: {}", new Object[] { query, fex.getMessage() } );
			}
		}

		return null;
	}

}
