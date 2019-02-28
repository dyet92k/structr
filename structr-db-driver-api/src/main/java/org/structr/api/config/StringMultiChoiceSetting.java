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
package org.structr.api.config;

import java.util.LinkedHashSet;
import java.util.Set;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.structr.api.util.html.Attr;
import org.structr.api.util.html.Tag;

/**
 * A configuration setting with a key and a type.
 */
public class StringMultiChoiceSetting extends Setting<String> {

	private Set<String> AvailableOptions = new LinkedHashSet<>();

	/**
	 * Constructor to create an empty StringSetting with NO default value.
	 *
	 * @param group
	 * @param key
	 */
	public StringMultiChoiceSetting(final SettingsGroup group, final String key) {
		this(group, key, null);
	}

	/**
	 * Constructor to create a StringSetting WITH default value.
	 *
	 * @param group
	 * @param key
	 * @param value
	 */
	public StringMultiChoiceSetting(final SettingsGroup group, final String key, final String value) {
		this(group, null, key, value);
	}


	/**
	 * Constructor to create a StringSetting with category name and default value.
	 * @param group
	 * @param categoryName
	 * @param key
	 * @param value
	 */
	public StringMultiChoiceSetting(final SettingsGroup group, final String categoryName, final String key, final String value) {
		super(group, categoryName, key, value);
	}

	/**
	 * Constructor to create a StringSetting with category name, default value and a comment
	 * @param group
	 * @param categoryName
	 * @param key
	 * @param value
	 * @param comment
	 */
	public StringMultiChoiceSetting(final SettingsGroup group, final String categoryName, final String key, final String value, final String comment) {
		super(group, categoryName, key, value, comment);
	}

	@Override
	public void render(final Tag parent) {

		final Tag group = parent.block("div").css("form-group");
		final Tag label = group.block("label").text(getKey());
		final String id = RandomStringUtils.randomAlphabetic(8);

		if (getComment() != null) {
			label.attr(new Attr("class", "has-comment"));
			label.attr(new Attr("data-comment", getComment()));
		}

		final Tag input = group.empty("input").attr(
			new Attr("type",         "text"),
			new Attr("name",         getKey()),
			new Attr("id",           id),
			new Attr("class",        "ordered-multi-select hidden"),
			new Attr("data-choices", StringUtils.join(AvailableOptions, ","))
		);

		final String value = getValue();

		// display value if non-empty
		if (value != null) {
			input.attr(new Attr("value", value));
		}

		final Tag options = group.block("div");

		for (final String option : AvailableOptions) {
			options.block("button").attr(
				new Attr("type", "button"),
				new Attr("class", "toggle-option" + (value.contains(option) ? " active" : "")),
				new Attr("title", "Toggle " + option),
				new Attr("data-value", option),
				new Attr("data-target", id)
			).text(option);
		}

		renderResetButton(options);
	}

	@Override
	public void fromString(final String source) {

		if (source == null) {
			return;
		}

		setValue(source);
	}

	@Override
	public String getValue(final String defaultValue) {

		if (StringUtils.isBlank(getValue())) {

			return defaultValue;
		}

		return getValue();
	}

	public void addAvailableOption(final String option) {
		this.AvailableOptions.add(option);
	}
}
