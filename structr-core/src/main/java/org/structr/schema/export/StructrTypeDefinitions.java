/**
 * Copyright (C) 2010-2018 Structr GmbH
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
package org.structr.schema.export;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaRelationshipNode;
import org.structr.schema.json.JsonObjectType;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonType;

/**
 *
 *
 */
public class StructrTypeDefinitions implements StructrDefinition {

	private final Set<StructrRelationshipTypeDefinition> relationships = new TreeSet<>();
	private final Set<StructrTypeDefinition> typeDefinitions           = new TreeSet<>();
	private StructrSchemaDefinition root                               = null;

	StructrTypeDefinitions(final StructrSchemaDefinition root) {
		this.root = root;
	}

	public Set<StructrTypeDefinition> getTypes() {
		return typeDefinitions;
	}

	public JsonType getType(final String name, final boolean create) {

		for (final JsonType type : typeDefinitions) {

			if (name.equals(type.getName())) {
				return type;
			}
		}

		if (create) {

			// create
			return addType(name);
		}

		return null;
	}

	public JsonObjectType addType(final String name) {

		final JsonType type = getType(name, false);
		if (type != null) {

			return (JsonObjectType)type;
		}

		final StructrNodeTypeDefinition def = new StructrNodeTypeDefinition(root, name);

		typeDefinitions.add(def);

		return def;

	}

	public void removeType(final String name) {

		final StructrNodeTypeDefinition def = new StructrNodeTypeDefinition(root, name);

		typeDefinitions.remove(def);

	}

	public void createDatabaseSchema(final App app, final JsonSchema.ImportMode importMode) throws FrameworkException {

		final Map<String, SchemaNode> schemaNodes = new LinkedHashMap<>();

		// collect list of schema nodes
		app.nodeQuery(SchemaNode.class).getAsList().stream().forEach(n -> { schemaNodes.put(n.getName(), n); });

		// iterate type definitions
		for (final StructrTypeDefinition type : typeDefinitions) {

			final AbstractSchemaNode schemaNode = type.createDatabaseSchema(schemaNodes, app);
			if (schemaNode != null) {

				type.setSchemaNode(schemaNode);
			}
		}

		for (final StructrRelationshipTypeDefinition rel : relationships) {
			rel.resolveEndpointTypesForDatabaseSchemaCreation(schemaNodes, app);
		}
	}

	@Override
	public StructrDefinition resolveJsonPointerKey(final String key) {

		for (final StructrTypeDefinition type : typeDefinitions) {

			if (key.equals(type.getName())) {
				return type;
			}
		}

		return null;
	}

	// ----- package methods -----
	Map<String, Object> serialize() {

		final Map<String, Object> map = new TreeMap<>();

		for (final StructrTypeDefinition type : typeDefinitions) {

			map.put(type.getName(), type.serialize());
		}

		return map;
	}

	void deserialize(final Map<String, Object> source) {
		deserialize(source, null);
	}

	void deserialize(final Map<String, Object> source, final List<String> nodeIds) {

		for (final Entry<String, Object> entry : source.entrySet()) {

			final String name  = entry.getKey();
			final Object value = entry.getValue();

			if (value instanceof Map) {

				final Map<String, Object> map    = (Map)value;
				final StructrTypeDefinition type = StructrTypeDefinition.deserialize(root, name, map);

				if (type != null) {

					typeDefinitions.add(type);

					if (type instanceof StructrRelationshipTypeDefinition) {
						relationships.add((StructrRelationshipTypeDefinition)type);
					}

				}

			} else {

				throw new IllegalStateException("Invalid JSON object for " + name + ", expected object, got " + value.getClass().getSimpleName());
			}
		}

		// initialize reference properties after all types are done
		for (final StructrTypeDefinition type : typeDefinitions) {
			type.initializeReferenceProperties();
		}
	}

	void deserialize(final App app) throws FrameworkException {

		final Map<String, SchemaNode> schemaNodes = new LinkedHashMap<>();

		// collect list of schema nodes
		app.nodeQuery(SchemaNode.class).getAsList().stream().forEach(n -> { schemaNodes.put(n.getName(), n); });

		// iterate
		for (final SchemaNode schemaNode : schemaNodes.values()) {

			final StructrTypeDefinition type = StructrTypeDefinition.deserialize(schemaNodes, root, schemaNode);
			if (type != null) {

				typeDefinitions.add(type);
			}
		}

		for (final SchemaRelationshipNode schemaRelationship : app.nodeQuery(SchemaRelationshipNode.class).getAsList()) {

			final StructrTypeDefinition type = StructrTypeDefinition.deserialize(schemaNodes, root, schemaRelationship);
			if (type != null) {

				typeDefinitions.add(type);

				if (type instanceof StructrRelationshipTypeDefinition) {
					relationships.add((StructrRelationshipTypeDefinition)type);
				}
			}
		}

	}

	void addType(final StructrTypeDefinition type) {
		typeDefinitions.add(type);
	}

	void removeType(final StructrTypeDefinition type) {
		typeDefinitions.remove(type);
	}

	Set<StructrRelationshipTypeDefinition> getRelationships() {
		return relationships;
	}
}
