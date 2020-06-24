/*
 * Copyright (C) 2010-2020 Structr GmbH
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
package org.structr.core.script;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.structr.common.error.FrameworkException;
import org.structr.schema.action.ActionContext;

/**
 */
public class ScriptTestHelper {

	public static Object testExternalScript(final ActionContext actionContext, final InputStream stream) throws FrameworkException {

		try (final InputStream is = stream) {

			final String script = IOUtils.toString(is, "utf-8");
			if (script != null) {

				return Scripting.evaluateJavascript(actionContext, null, new TestSnippet("test", script));
			}
			
		} catch (IOException ioex) {
			ioex.printStackTrace();
		}

		return null;
	}

	// ----- nested classes -----
	private static class TestSnippet extends Snippet {

		public TestSnippet(final String name, final String source) {
			super(name, source);
		}

		@Override
		public boolean embed() {
			return false;
		}
	}
}
