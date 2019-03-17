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
package org.structr.schema.action;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;

/**
 *
 *
 */
public class ActionEntry implements Comparable<ActionEntry> {

	private final Map<String, String> parameters = new LinkedHashMap<>();
	private final List<String> exceptions        = new LinkedList<>();
	private Actions.Type type                    = null;
	private boolean doExport                     = false;
	private boolean overrides                    = false;
	private boolean callSuper                    = false;
	private String sourceUuid                    = null;
	private String returnType                    = null;
	private String call                          = null;
	private String name                          = null;
	private int position                         = 0;

	@Override
	public String toString() {

		final StringBuilder buf = new StringBuilder();

		if (returnType != null) {
			buf.append(returnType);
			buf.append(" ");
		}

		buf.append(name);
		buf.append("(");
		buf.append(StringUtils.join(parameters.entrySet().stream().map(k -> k.getKey() + " " + k.getValue()).collect(Collectors.toList()), ", "));
		buf.append(")");

		if (!exceptions.isEmpty()) {

			buf.append(" throws ");
			buf.append(StringUtils.join(exceptions, ", "));
		}

		return buf.toString();
	}

	public ActionEntry(final String sourceName, final String value, final String codeType) {

		int positionOffset = 0;

		if (sourceName.startsWith("___onSave")) {

			this.name = "onModification";
			this.type = Actions.Type.Save;
			positionOffset = 9;

		} else if (sourceName.startsWith("___onCreate")) {

			this.name = "onCreation";
			this.type = Actions.Type.Create;
			positionOffset = 11;

		} else if (sourceName.startsWith("___afterCreate")) {

			this.name = "afterCreation";
			this.type = Actions.Type.AfterCreate;
			positionOffset = 14;

		} else if (sourceName.startsWith("___onDelete")) {

			this.name = "afterDeletion";
			this.type = Actions.Type.Delete;
			positionOffset = 11;

		} else {

			if (codeType != null && "java".equals(codeType)) {

				this.type = Actions.Type.Java;

			} else {

				this.type = Actions.Type.Custom;
			}

			positionOffset = 3;
		}

		switch (type) {

			case Custom:
			case Java:
				this.name = sourceName.substring(positionOffset);
				break;

			default:
				// try to identify a position
				final String positionString = sourceName.substring(positionOffset);
				if (!positionString.isEmpty()) {

					try { position = Integer.parseInt(positionString); } catch (Throwable t) { /* ignore */ }
				}
				break;
		}

		this.call = value;
	}

	public void setSourceUuid(final String uuid) {
		this.sourceUuid = uuid;
	}

	public void setReturnType(final String returnType) {
		this.returnType = returnType;
	}

	public String getReturnType() {
		return this.returnType;
	}

	public void addException(final String exception) {
		this.exceptions.add(exception);
	}

	public List<String>  getExceptions() {
		return exceptions;
	}

	public void addParameter(final String type, final String name) {
		this.parameters.put(name, type);
	}

	public Map<String, String> getParameters() {
		return this.parameters;
	}

	public void setCallSuper(final boolean callSuper) {
		this.callSuper = callSuper;
	}

	public boolean callSuper() {
		return callSuper;
	}

	public void setOverrides(final boolean overrides) {
		this.overrides = overrides;
	}

	public boolean overrides() {
		return overrides;
	}

	public void setDoExport(final boolean doExport) {
		this.doExport = doExport;
	}

	public boolean doExport() {
		return doExport;
	}

	public String getSource(final String objVariable, final String securityContextVariable, final boolean includeModifications) {
		return getSource(objVariable, securityContextVariable, false, includeModifications);
	}

	public String getSource(final String objVariable, final boolean includeParameters, final boolean includeModifications) {

		return getSource(objVariable, "securityContext", includeParameters, includeModifications);
	}

	public String getSource(final String objVariable, final String securityContextVariable, final boolean includeParameters, final boolean includeModifications) {

		final StringBuilder buf = new StringBuilder();

		if (Actions.Type.Java.equals(type)) {

			buf.append(this.call);

		} else {

			final String methodName = this.type.equals(Actions.Type.Custom) ? this.name : this.type.getLogName();

			buf.append(Actions.class.getSimpleName());
			buf.append(".execute(").append(securityContextVariable).append(", ").append(objVariable).append(", ");
			buf.append("SchemaMethod.getCachedSourceCode(\"");
			buf.append(sourceUuid);
			buf.append("\")");

			if (includeParameters) {
				buf.append(", parameters");
			}

			buf.append(", \"");
			buf.append(methodName);
			buf.append("\"");

			if (includeModifications) {
				buf.append(", arg2");
			}

			buf.append(")");
		}

		return buf.toString();
	}

	@Override
	public int compareTo(ActionEntry o) {
		return getPosition().compareTo(o.getPosition());
	}

	public Actions.Type getType() {
		return type;
	}

	public String getCall() {
		return call;
	}

	public Integer getPosition() {
		return position;
	}

	public String getName() {
		return name;
	}

	public void copy(final ActionEntry template) {

		this.name       = template.name;
		this.type       = template.type;
		this.returnType = template.returnType;

		// copy parameters
		for (final Entry<String, String> entry : template.getParameters().entrySet()) {
			this.parameters.put(entry.getKey(), entry.getValue());
		}

		// copy exceptions
		for (final String exception : template.getExceptions()) {
			this.exceptions.add(exception);
		}
	}
}
