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
package org.structr.schema.parser;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.structr.common.error.ErrorBuffer;
import org.structr.common.error.ErrorToken;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.entity.AbstractSchemaNode;
import org.structr.core.entity.SchemaNode;
import org.structr.core.entity.SchemaProperty;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.property.StringProperty;
import org.structr.schema.Schema;
import org.structr.schema.SchemaHelper;
import org.structr.schema.SchemaHelper.Type;

/**
 *
 *
 */
public abstract class PropertySourceGenerator {

	private final Set<String> compoundIndexKeys   = new LinkedHashSet<>();
	private final Set<Validator> globalValidators = new LinkedHashSet<>();
	private final Set<String> enumDefinitions     = new LinkedHashSet<>();
	protected PropertyDefinition source           = null;
	protected ErrorBuffer errorBuffer             = null;
	protected String className                    = "";


	public abstract Type getKey();
	public abstract String getPropertyType();
	public abstract String getValueType();
	public abstract String getUnqualifiedValueType();
	public abstract String getPropertyParameters();
	public abstract void parseFormatString(final Map<String, SchemaNode> schemaNodes, final Schema entity, final String expression) throws FrameworkException;

	public PropertySourceGenerator(final ErrorBuffer errorBuffer, final String className, final PropertyDefinition propertyDefinition) {
		this.errorBuffer  = errorBuffer;
		this.className    = className;
		this.source       = propertyDefinition;
	}

	public void getPropertySource(final Map<String, SchemaNode> schemaNodes, final StringBuilder buf, final Schema entity) throws FrameworkException {

		if (source.isNotNull()) {

			globalValidators.add(new Validator("isValidPropertyNotNull", className, source.getPropertyName()));
		}

		if (source.isUnique()) {

			globalValidators.add(new Validator("isValidUniqueProperty", className, source.getPropertyName()));
		}

		if (source.isCompound()) {

			compoundIndexKeys.add(SchemaHelper.cleanPropertyName(source.getPropertyName()) + "Property");
		}

		parseFormatString(schemaNodes, entity, source.getFormat());
		getPropertySource(buf);
	}

	public String getClassName() {
		return className;
	}

	public Set<String> getCompoundIndexKeys() {
		return compoundIndexKeys;
	}

	public Set<Validator> getGlobalValidators() {
		return globalValidators;
	}

	public void addGlobalValidator(final Validator validator) {
		globalValidators.add(validator);
	}

	public Set<String> getEnumDefinitions() {
		return enumDefinitions;
	}

	public String getSourcePropertyName() {
		return source.getPropertyName();
	}

	public void reportError(final ErrorToken error) {
		errorBuffer.add(error);
	}

	public ErrorBuffer getErrorBuffer() {
		return errorBuffer;
	}

	public void addEnumDefinition(final String item) {
		enumDefinitions.add(item);
	}

	public String getSourceDefaultValue() {
		return source.getDefaultValue();
	}

	public String getDefaultValue() {
		return getSourceDefaultValue();
	}

	public void createSchemaPropertyNode(final AbstractSchemaNode schemaNode, final String underscorePropertyName) throws FrameworkException {

		final App app             = StructrApp.getInstance();
		final String propertyName = getSourcePropertyName();

		if (app.nodeQuery(SchemaProperty.class).and(SchemaProperty.schemaNode, schemaNode).and(AbstractNode.name, propertyName).getFirst() == null) {

			app.create(SchemaProperty.class,
				new NodeAttribute<>(AbstractNode.name,                    propertyName),
				new NodeAttribute<>(SchemaProperty.schemaNode,            schemaNode),
				new NodeAttribute<>(SchemaProperty.propertyType,          getKey().name()),
				new NodeAttribute<>(SchemaProperty.contentType,           source.getContentType()),
				new NodeAttribute<>(SchemaProperty.dbName,                source.getDbName()),
				new NodeAttribute<>(SchemaProperty.defaultValue,          source.getDefaultValue()),
				new NodeAttribute<>(SchemaProperty.format,                source.getFormat()),
				new NodeAttribute<>(SchemaProperty.typeHint,              source.getTypeHint()),
				new NodeAttribute<>(SchemaProperty.hint,                  source.getHint()),
				new NodeAttribute<>(SchemaProperty.category,              source.getCategory()),
				new NodeAttribute<>(SchemaProperty.fqcn,                  source.getFqcn()),
				new NodeAttribute<>(SchemaProperty.compound,              source.isCompound()),
				new NodeAttribute<>(SchemaProperty.unique,                source.isUnique()),
				new NodeAttribute<>(SchemaProperty.indexed,               source.isIndexed()),
				new NodeAttribute<>(SchemaProperty.notNull,               source.isNotNull()),
				new NodeAttribute<>(SchemaProperty.isPartOfBuiltInSchema, source.isPartOfBuiltInSchema()),
				new NodeAttribute<>(SchemaProperty.isCachingEnabled,	  source.isCachingEnabled()),
				new NodeAttribute<>(SchemaProperty.readFunction,          source.getReadFunction()),
				new NodeAttribute<>(SchemaProperty.writeFunction,         source.getWriteFunction()),
				new NodeAttribute<>(SchemaProperty.transformers,          source.getTransformators()),
				new NodeAttribute<>(SchemaProperty.validators,            source.getValidators())
			);

			schemaNode.removeProperty(new StringProperty(underscorePropertyName));
		}
	}

	// ----- protected methods -----
	protected void getPropertySource(final StringBuilder buf) {

		final String sourceUuid = source.getUuid();

		buf.append("\tpublic static final Property<").append(getValueType()).append("> ").append(SchemaHelper.cleanPropertyName(source.getPropertyName())).append("Property");
		buf.append(" = new ").append(getPropertyType()).append("(\"").append(source.getPropertyName()).append("\"");

		if (StringUtils.isNotBlank(source.getDbName())) {
			buf.append(", \"").append(source.getDbName()).append("\"");
		}

		if (getPropertyParameters() != null) {
			buf.append(getPropertyParameters());
		}

		buf.append(")");

		if (StringUtils.isNotBlank(source.getContentType())) {
			buf.append(".contentType(\"").append(source.getContentType()).append("\")");
		}

		if (StringUtils.isNotBlank(source.getDefaultValue())) {
			buf.append(".defaultValue(").append(getDefaultValue()).append(")");
		}

		if (StringUtils.isNotBlank(source.getFormat())) {
			buf.append(".format(\"").append(StringEscapeUtils.escapeJava(source.getFormat())).append("\")");
		}

		if (StringUtils.isNotBlank(sourceUuid)) {

			buf.append(".setSourceUuid(\"").append(sourceUuid).append("\")");

		} else {

			if (StringUtils.isNotBlank(source.getReadFunction())) {
				buf.append(".readFunction(\"").append(StringEscapeUtils.escapeJava(source.getReadFunction())).append("\")");
			}

			if (StringUtils.isNotBlank(source.getWriteFunction())) {
				buf.append(".writeFunction(\"").append(StringEscapeUtils.escapeJava(source.getWriteFunction())).append("\")");
			}
		}

		if (StringUtils.isNotBlank(source.getTypeHint())) {
			buf.append(".typeHint(\"").append(StringEscapeUtils.escapeJava(source.getTypeHint())).append("\")");
		}

		if (source.isUnique()) {
			buf.append(".unique()");
		}

		if (source.isCompound()) {
			buf.append(".compound()");
		}

		if (source.isNotNull()) {
			buf.append(".notNull()");
		}

		if (source.isCachingEnabled()) {
			buf.append(".cachingEnabled(true)");
		}

		if (source.isIndexed()) {

			if (StringUtils.isNotBlank(source.getDefaultValue())) {

				buf.append(".indexedWhenEmpty()");

			} else {

				buf.append(".indexed()");
			}
		}

		if (source.isReadOnly()) {

			buf.append(".readOnly()");
		}

		final String[] transformators = source.getTransformators();
		if (transformators != null && transformators.length > 0) {

			buf.append(".transformators(\"");
			buf.append(StringUtils.join(transformators, "\", \""));
			buf.append("\")");
		}

		if (source.isPartOfBuiltInSchema()) {
			buf.append(".partOfBuiltInSchema()");
		}

		buf.append(".dynamic()");

		if (StringUtils.isNotBlank(source.getHint())) {
			buf.append(".hint(\"").append(StringEscapeUtils.escapeJava(source.getHint())).append("\")");
		}

		if (StringUtils.isNotBlank(source.getCategory())) {
			buf.append(".category(\"").append(StringEscapeUtils.escapeJava(source.getCategory())).append("\")");
		}

		buf.append(";\n");
	}
}
