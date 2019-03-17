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
package org.structr.schema.compiler;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javax.tools.Diagnostic;
import javax.tools.Diagnostic.Kind;
import javax.tools.DiagnosticListener;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.ToolProvider;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.Predicate;
import org.structr.api.config.Settings;
import org.structr.common.error.DiagnosticErrorToken;
import org.structr.common.error.ErrorBuffer;
import org.structr.core.Services;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.TransactionCommand;
import org.structr.module.JarConfigurationProvider;

/**
 *
 *
 */
public class NodeExtender {

	private static final Logger logger   = LoggerFactory.getLogger(NodeExtender.class.getName());

	private static final JavaCompiler compiler       = ToolProvider.getSystemJavaCompiler();
	private static final JavaFileManager fileManager = new ClassFileManager(compiler.getStandardFileManager(null, null, null));
	private static final ClassLoader classLoader     = fileManager.getClassLoader(null);
	private static final Map<String, Class> classes  = new TreeMap<>();

	private List<JavaFileObject> jfiles  = null;
	private Set<String> fqcns            = null;
	private String initiatedBySessionId  = null;

	public NodeExtender(final String initiatedBySessionId) {

		this.initiatedBySessionId = initiatedBySessionId;
		this.jfiles               = new ArrayList<>();
		this.fqcns                = new LinkedHashSet<>();
	}

	public static ClassLoader getClassLoader() {
		return classLoader;
	}

	public static Class getClass(final String fqcn) {
		return classes.get(fqcn);
	}

	public static Map<String, Class> getClasses() {
		return classes;
	}

	public void addClass(final String className, final String content) throws ClassNotFoundException {

		if (className != null && content != null) {

			final String packageName = JarConfigurationProvider.DYNAMIC_TYPES_PACKAGE;

			jfiles.add(new CharSequenceJavaFileObject(className, content));
			fqcns.add(packageName.concat(".".concat(className)));

			if (Settings.LogSchemaOutput.getValue()) {

				System.out.println("########################################################################################################################################################");

				int count = 0;

				for (final String line : content.split("[\\n\\r]{1}")) {
					System.out.println(StringUtils.rightPad(++count + ": ", 6) + line);
				}
			}
		}
	}

	public synchronized Map<String, Class> compile(final ErrorBuffer errorBuffer) throws ClassNotFoundException {

		final Writer errorWriter     = new StringWriter();
		final List<Class> newClasses = new LinkedList<>();

		if (!jfiles.isEmpty()) {

			logger.info("Compiling {} dynamic entities...", jfiles.size());

			final long t0 = System.currentTimeMillis();

			Boolean success = compiler.getTask(errorWriter, fileManager, new Listener(errorBuffer), Arrays.asList("-g"), null, jfiles).call();

			logger.info("Compiling done in {} ms", System.currentTimeMillis() - t0);

			if (success) {

				final ClassLoader loader = fileManager.getClassLoader(null);

				for (final String fqcn : fqcns) {

					try {

						newClasses.add(loader.loadClass(fqcn));

					} catch (Throwable t) {

						logger.warn("Unable to load dynamic entity {}: {}", new Object[] { fqcn, t.toString() });
						logger.warn("", t);

						success = false;
					}
				}

				for (final Class oldType : classes.values()) {
					StructrApp.getConfiguration().unregisterEntityType(oldType);
				}

				// clear classes map
				classes.clear();

				// add new classes to map
				for (final Class newType : newClasses) {
					classes.put(newType.getName(), newType);
				}

				logger.info("Successfully compiled {} dynamic entities: {}", new Object[] { jfiles.size(), jfiles.stream().map(f -> f.getName().replaceFirst("/", "")).collect(Collectors.joining(", ")) });

				final Map<String, Object> data = new LinkedHashMap();
				data.put("success", true);
				TransactionCommand.simpleBroadcast("SCHEMA_COMPILED", data, Predicate.allExcept(getInitiatedBySessionId()));

				Services.getInstance().setOverridingSchemaTypesAllowed(false);
			}
		}

		return classes;
	}

	public String getInitiatedBySessionId () {
		return initiatedBySessionId;
	}

	public void setInitiatedBySessionId (final String initiatedBySessionId) {
		this.initiatedBySessionId = initiatedBySessionId;
	}

	private static class Listener implements DiagnosticListener<JavaFileObject> {

		private ErrorBuffer errorBuffer = null;

		public Listener(final ErrorBuffer errorBuffer) {
			this.errorBuffer = errorBuffer;
		}

		@Override
		public void report(Diagnostic<? extends JavaFileObject> diagnostic) {

			if (diagnostic.getKind().equals(Kind.ERROR)) {

				final JavaFileObject obj = diagnostic.getSource();
				String name              = "unknown";

				if (obj != null && obj instanceof CharSequenceJavaFileObject) {
					name = ((CharSequenceJavaFileObject)obj).getClassName();
				}

				errorBuffer.add(new DiagnosticErrorToken(name, diagnostic));

				if (Settings.LogSchemaErrors.getValue()) {

					String src = ((CharSequenceJavaFileObject) diagnostic.getSource()).getCharContent(true).toString();

					// Add line numbers
					final AtomicInteger index = new AtomicInteger();
					src = Arrays.asList(src.split("\\R")).stream()
						.map(line -> (index.getAndIncrement()+1) + ": " + line)
						.collect(Collectors.joining("\n"));

					// log also to log file
					logger.error("Unable to compile dynamic entity {}:{}: {}\n{}", name, diagnostic.getLineNumber(), diagnostic.getMessage(Locale.ENGLISH), src);
				}
			}
		}
	}
}
