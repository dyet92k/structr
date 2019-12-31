/**
 * Copyright (C) 2010-2019 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.test.web.basic;

import java.io.IOException;
import java.net.URI;
import java.text.SimpleDateFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.tika.io.IOUtils;
import org.testng.annotations.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.config.Settings;
import org.structr.api.util.Iterables;
import org.structr.common.AccessMode;
import org.structr.common.Permission;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.graph.Tx;
import org.structr.core.property.PropertyMap;
import org.structr.schema.export.StructrSchema;
import org.structr.schema.json.JsonSchema;
import org.structr.schema.json.JsonType;
import org.structr.test.web.StructrUiTest;
import org.structr.web.common.FileHelper;
import org.structr.web.common.ImageHelper;
import org.structr.web.common.ImageHelper.Thumbnail;
import org.structr.web.entity.AbstractFile;
import org.structr.web.entity.File;
import org.structr.web.entity.Folder;
import org.structr.web.entity.Image;
import org.structr.web.entity.User;
import org.structr.web.property.ThumbnailProperty;
import static org.testng.Assert.assertNotEquals;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;


public class UiTest extends StructrUiTest {

	private static String base64Image  = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAWAAAABUCAYAAAC8/e1DAAAAGXRFWHRTb2Z0d2FyZQBBZG9iZSBJbWFnZVJlYWR5ccllPAAAA2ZpVFh0WE1MOmNvbS5hZG9iZS54bXAAAAAAADw/eHBhY2tldCBiZWdpbj0i77u/IiBpZD0iVzVNME1wQ2VoaUh6cmVTek5UY3prYzlkIj8+IDx4OnhtcG1ldGEgeG1sbnM6eD0iYWRvYmU6bnM6bWV0YS8iIHg6eG1wdGs9IkFkb2JlIFhNUCBDb3JlIDUuMy1jMDExIDY2LjE0NTY2MSwgMjAxMi8wMi8wNi0xNDo1NjoyNyAgICAgICAgIj4gPHJkZjpSREYgeG1sbnM6cmRmPSJodHRwOi8vd3d3LnczLm9yZy8xOTk5LzAyLzIyLXJkZi1zeW50YXgtbnMjIj4gPHJkZjpEZXNjcmlwdGlvbiByZGY6YWJvdXQ9IiIgeG1sbnM6eG1wTU09Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC9tbS8iIHhtbG5zOnN0UmVmPSJodHRwOi8vbnMuYWRvYmUuY29tL3hhcC8xLjAvc1R5cGUvUmVzb3VyY2VSZWYjIiB4bWxuczp4bXA9Imh0dHA6Ly9ucy5hZG9iZS5jb20veGFwLzEuMC8iIHhtcE1NOk9yaWdpbmFsRG9jdW1lbnRJRD0ieG1wLmRpZDo2RjYyNjlFMUNFMTNFMjExQTQ2N0ZGMDI2MEZEQ0Q3NSIgeG1wTU06RG9jdW1lbnRJRD0ieG1wLmRpZDo2MDcwOEExQzEzRDMxMUUyQTMyQzlEQjBGNTBBQUUwMSIgeG1wTU06SW5zdGFuY2VJRD0ieG1wLmlpZDo2MDcwOEExQjEzRDMxMUUyQTMyQzlEQjBGNTBBQUUwMSIgeG1wOkNyZWF0b3JUb29sPSJBZG9iZSBQaG90b3Nob3AgQ1M2IChXaW5kb3dzKSI+IDx4bXBNTTpEZXJpdmVkRnJvbSBzdFJlZjppbnN0YW5jZUlEPSJ4bXAuaWlkOkYzODhBQzYwRDIxM0UyMTFBNDY3RkYwMjYwRkRDRDc1IiBzdFJlZjpkb2N1bWVudElEPSJ4bXAuZGlkOjZGNjI2OUUxQ0UxM0UyMTFBNDY3RkYwMjYwRkRDRDc1Ii8+IDwvcmRmOkRlc2NyaXB0aW9uPiA8L3JkZjpSREY+IDwveDp4bXBtZXRhPiA8P3hwYWNrZXQgZW5kPSJyIj8+xNxK9AAAAt9JREFUeNrs3TFOKlEUgGGuIWhGSwmVLoMlWFi8ddiZuA41sTMuw9LlYGWBEqOE4DC8c4mNhSUzhvt9yQ2813GCf46DOmk8Hu9dXl7+q6rqPqU06gGwNev1+mU+n1/c3d099uPfB+Eh/nMYx3QAtmuUmxuPTznAh+IL0Kphbm8O8GC1WhkHQLsGOcCpaRqjAGhXygHu2YAB2ifAAF0GuK5rkwCwAQPYgAEQYIAdDbBLEAA2YAABBqCFALsEASDAAGUF2CUIABswQFkB9tfQAGzAAGUF2DVggI4CvFgsTALABgxQUIDdkBOgfXtGACDAAAIMgAADCDAAAgwgwAAIMIAAAyDAAH/a5leRU0omAWADBrABA2ADBtjNAK/Dm1EAtCOaO8sPOcDL19fXm6Zp3o0FYLuitbNo7nVubxqPx1U8OY1zEucojgvC3egbAYV81516Py9/5jtC5DsD19/Pd3r5jfMR5znOJH/R5xvCTeJM4wwEuNM3JpQg/RKmpoDXnl/nMs5nbm//+Pi4d3V1dVZV1X1KaeS9AbDFAq/XL/P5/OL29vYxb8AH+/v7D03TDI0GYOtGubnx+JQDfBhFHroxJ0Br8sJ7mAM8iO3XOADaNcgBTgIM0Lq0+dGn1WplFAAt2wTYBgzQUYC/vr5MAqCLALsEASDAAGUF2CUIgI4C7EM4gI4CXNe1SQAIMEBBAfYhHIAAA5QVYJcgAGzAAGUF2N8CBugowC5BAAgwQFkBXiwWJgFgAwYoKMA+hANo354RAAgwgAADIMAAAgyAAAMIMAACDCDAAAgwwJ+2+VXklJJJANiAAWzAANiAAXYzwOvwZhQA7YjmzvJDDvByOp3eNE3zbiwA2xWtnUVzr3N70/n5eRVPTuOcxDmK44JwN/pGQCHfdafez8uf+Y4Qqzj19/OdXn7jfMR5jjPJX/T5hnCTONM4AwHu9I0JJUi/hKkp4LXn17mM85nb+1+AAQDuVAgNv/BqVwAAAABJRU5ErkJggg==";
	private static final Logger logger = LoggerFactory.getLogger(UiTest.class.getName());

	@Test
	public void test01CreateThumbnail() {

		final Class imageType = createTestImageType();

		try (final Tx tx = app.tx()) {

			Image img = (Image) ImageHelper.createFileBase64(securityContext, base64Image, imageType);

			img.setProperties(img.getSecurityContext(), new PropertyMap(AbstractNode.name, "test-image.png"));

			assertNotNull(img);
			assertTrue(img instanceof Image);

			Image tn = img.getProperty(StructrApp.key(imageType, "thumbnail"));

			assertNotNull(tn);
			assertEquals(new Integer(200), tn.getWidth());
			assertEquals(new Integer(48), tn.getHeight());  // cropToFit = false
			assertEquals("image/" + Thumbnail.Format.jpeg, tn.getContentType());

			tx.success();

		} catch (Exception ex) {
			ex.printStackTrace();
			fail("Unexpected exception");
		}
	}

	@Test
	public void test01AutoRenameThumbnail() {

		final String initialImageName = "initial_image_name.png";
		final String renamedImageName = "image_name_after_rename.png";
		Image testImage = null;

		try (final Tx tx = app.tx()) {

			testImage = (Image) ImageHelper.createFileBase64(securityContext, base64Image, Image.class);

			testImage.setProperties(testImage.getSecurityContext(), new PropertyMap(Image.name, initialImageName));

			assertNotNull(testImage);
			assertTrue(testImage instanceof Image);

			final Image tnSmall = testImage.getProperty(StructrApp.key(Image.class, "tnSmall"));
			final Image tnMid   = testImage.getProperty(StructrApp.key(Image.class, "tnMid"));

			assertEquals("Initial small thumbnail name not as expected", ImageHelper.getThumbnailName(initialImageName, tnSmall.getWidth(), tnSmall.getHeight()), tnSmall.getProperty(StructrApp.key(Image.class, "name")));
			assertEquals("Initial mid thumbnail name not as expected", ImageHelper.getThumbnailName(initialImageName, tnMid.getWidth(), tnMid.getHeight()), tnMid.getProperty(StructrApp.key(Image.class, "name")));

			tx.success();

		} catch (Exception ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			testImage.setProperties(testImage.getSecurityContext(), new PropertyMap(Image.name, renamedImageName));
			tx.success();

		} catch (Exception ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final Image tnSmall = testImage.getProperty(StructrApp.key(Image.class, "tnSmall"));
			final Image tnMid   = testImage.getProperty(StructrApp.key(Image.class, "tnMid"));

			assertEquals("Small Thumbnail name not auto-renamed as expected", ImageHelper.getThumbnailName(renamedImageName, tnSmall.getWidth(), tnSmall.getHeight()), tnSmall.getProperty(StructrApp.key(Image.class, "name")));
			assertEquals("Mid Thumbnail name not auto-renamed as expected", ImageHelper.getThumbnailName(renamedImageName, tnMid.getWidth(), tnMid.getHeight()), tnMid.getProperty(StructrApp.key(Image.class, "name")));

			tx.success();

		} catch (Exception ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

	}

	@Test
	public void test01AutoRenameThumbnailForImageSubclass() {

		final Class testImageType = createTestImageType();

		Image subclassTestImage = null;
		final String initialImageName = "initial_image_name.png";
		final String renamedImageName = "image_name_after_rename.png";

		try (final Tx tx = app.tx()) {

			subclassTestImage = (Image) ImageHelper.createFileBase64(securityContext, base64Image, testImageType);

			subclassTestImage.setProperties(subclassTestImage.getSecurityContext(), new PropertyMap(Image.name, initialImageName));

			assertNotNull(subclassTestImage);
			assertTrue(subclassTestImage instanceof Image);

			final Image tnSmall  = subclassTestImage.getProperty(StructrApp.key(testImageType, "tnSmall"));
			final Image tnMid    = subclassTestImage.getProperty(StructrApp.key(testImageType, "tnMid"));
			final Image tnCustom = subclassTestImage.getProperty(StructrApp.key(testImageType, "thumbnail"));

			assertEquals("Initial small thumbnail name not as expected", ImageHelper.getThumbnailName(initialImageName, tnSmall.getWidth(), tnSmall.getHeight()), tnSmall.getProperty(StructrApp.key(Image.class, "name")));
			assertEquals("Initial mid thumbnail name not as expected", ImageHelper.getThumbnailName(initialImageName, tnMid.getWidth(), tnMid.getHeight()), tnMid.getProperty(StructrApp.key(Image.class, "name")));
			assertEquals("Initial custom thumbnail name not as expected", ImageHelper.getThumbnailName(initialImageName, tnCustom.getWidth(), tnCustom.getHeight()), tnCustom.getProperty(StructrApp.key(Image.class, "name")));

			tx.success();

		} catch (Exception ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			subclassTestImage.setProperties(subclassTestImage.getSecurityContext(), new PropertyMap(Image.name, renamedImageName));
			tx.success();

		} catch (Exception ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

		try (final Tx tx = app.tx()) {

			final Image tnSmall  = subclassTestImage.getProperty(StructrApp.key(testImageType, "tnSmall"));
			final Image tnMid    = subclassTestImage.getProperty(StructrApp.key(testImageType, "tnMid"));
			final Image tnCustom = subclassTestImage.getProperty(StructrApp.key(testImageType, "thumbnail"));

			assertEquals("Small Thumbnail name not auto-renamed as expected for image subclass", ImageHelper.getThumbnailName(renamedImageName, tnSmall.getWidth(), tnSmall.getHeight()), tnSmall.getProperty(StructrApp.key(Image.class, "name")));
			assertEquals("Mid Thumbnail name not auto-renamed as expected for image subclass", ImageHelper.getThumbnailName(renamedImageName, tnMid.getWidth(), tnMid.getHeight()), tnMid.getProperty(StructrApp.key(Image.class, "name")));
			assertEquals("Custom Thumbnail name not auto-renamed as expected for image subclass", ImageHelper.getThumbnailName(renamedImageName, tnCustom.getWidth(), tnCustom.getHeight()), tnCustom.getProperty(StructrApp.key(Image.class, "name")));

			tx.success();

		} catch (Exception ex) {

			logger.error(ex.toString());
			fail("Unexpected exception");
		}

	}

	@Test
	public void testFolderPath() {

		try (final Tx tx = app.tx()) {

			FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), "/a/a");
			FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), "/c/b/a");
			FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), "/b/a");
			FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), "/a/b/c");

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			Folder a = (Folder) FileHelper.getFileByAbsolutePath(SecurityContext.getSuperUserInstance(), "/a");
			assertNotNull(a);
			assertEquals(a.getPath(), "/a");

			Folder b = (Folder) FileHelper.getFileByAbsolutePath(SecurityContext.getSuperUserInstance(), "/a/b");
			assertNotNull(b);
			assertEquals(b.getPath(), "/a/b");

			Folder c = (Folder) FileHelper.getFileByAbsolutePath(SecurityContext.getSuperUserInstance(), "/a/b/c");
			assertNotNull(c);
			assertEquals(c.getPath(), "/a/b/c");

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

	}

	@Test
	public void testAllowedCharacters() {

		try (final Tx tx = app.tx()) {

			app.create(Folder.class, "/a/b");

			tx.success();

			fail("Folder with non-allowed characters were created.");

		} catch (FrameworkException ex) {}

		try (final Tx tx = app.tx()) {

			app.create(Folder.class, "a/b");

			tx.success();

			fail("Folder with non-allowed characters were created.");

		} catch (FrameworkException ex) {}

		try (final Tx tx = app.tx()) {

			app.create(Folder.class, "/");

			tx.success();

			fail("Folder with non-allowed characters were created.");

		} catch (FrameworkException ex) {}

		try (final Tx tx = app.tx()) {

			app.create(Folder.class, "c/");

			tx.success();

			fail("Folder with non-allowed characters were created.");

		} catch (FrameworkException ex) {}

		try (final Tx tx = app.tx()) {

			app.create(Folder.class, "abc\0");

			tx.success();

			fail("Folder with non-allowed characters were created.");

		} catch (FrameworkException ex) {}

		try (final Tx tx = app.tx()) {

			app.create(Folder.class, "\0abc");

			tx.success();

			fail("Folder with non-allowed characters were created.");

		} catch (FrameworkException ex) {}

		try (final Tx tx = app.tx()) {

			app.create(Folder.class, "a\0bc");

			tx.success();

			fail("Folder with non-allowed characters were created.");

		} catch (FrameworkException ex) {}
	}

	@Test
	public void testCreateFolder() {

		Folder folder1 = null;

		try (final Tx tx = app.tx()) {

			folder1 = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), "/folder1");
			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			File file1 = (File) app.create(File.class, "file1");
			assertNotNull(file1);
			assertEquals(file1.getPath(), "/file1");

			file1.setProperty(StructrApp.key(File.class, "parent"), folder1);
			assertEquals(file1.getPath(), "/folder1/file1");

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			Image image1 = (Image) app.create(Image.class, "image1");
			assertNotNull(image1);
			assertEquals(image1.getPath(), "/image1");

			image1.setProperty(StructrApp.key(File.class, "parent"), folder1);
			assertEquals(image1.getPath(), "/folder1/image1");

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			assertEquals(2, Iterables.toList(folder1.getFiles()).size());
			assertEquals(1, Iterables.toList(folder1.getImages()).size());

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}
	}

	@Test
	public void testCreateBase64File() {

		final String base64Data = "data:text/plain;base64,RGllcyBpc3QgZWluIFRlc3Q=";
		final String plaintext  = "Dies ist ein Test";
		File file           = null;

		try (final Tx tx = app.tx()) {

			file = app.create(File.class,
				new NodeAttribute<>(StructrApp.key(AbstractNode.class, "name"),      "test.txt"),
				new NodeAttribute<>(StructrApp.key(File.class,         "base64Data"), base64Data)
			);

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			assertEquals("Invalid base64 encoded file content creation result", plaintext, IOUtils.toString(file.getInputStream()));

			tx.success();

		} catch (FrameworkException | IOException ex) {
			logger.error("", ex);
		}

	}

	@Test
	public void testAutoRenameFileWithIdenticalPathInRootFolder() {

		Settings.UniquePaths.setValue(Boolean.TRUE);

		File rootFile1 = null;
		File rootFile2 = null;

		try (final Tx tx = app.tx()) {

			rootFile1 = app.create(File.class, new NodeAttribute<>(AbstractNode.name, "test.txt"));
			assertNotNull(rootFile1);

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}


		try (final Tx tx = app.tx()) {

			rootFile2 = app.create(File.class, new NodeAttribute<>(AbstractNode.name, "test.txt"));
			assertNotNull(rootFile2);

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		assertNotEquals(rootFile1.getName(), rootFile2.getName());
	}

	@Test
	public void testAutoRenameFileWithIdenticalPathInSubFolder() {

		Settings.UniquePaths.setValue(Boolean.TRUE);

		Folder folder = null;
		File file1 = null;
		File file2 = null;

		try (final Tx tx = app.tx()) {

			folder = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), "/my/test/folder");

			assertNotNull(folder);
			assertEquals(folder.getPath(), "/my/test/folder");

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			file1 = app.create(File.class,
				new NodeAttribute<>(AbstractNode.name, "test.txt"),
				new NodeAttribute<>(StructrApp.key(AbstractFile.class, "parent"), folder)
			);

			assertNotNull(file1);
			assertEquals("Testfolder should have exactly one child", 1, Iterables.count(folder.getChildren()));

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			file2 = app.create(File.class,
				new NodeAttribute<>(AbstractNode.name, "test.txt"),
				new NodeAttribute<>(StructrApp.key(AbstractFile.class, "parent"), folder)
			);

			assertNotNull(file2);
			assertEquals("Testfolder should have exactly two children", 2, Iterables.count(folder.getChildren()));

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		assertNotEquals(file1.getName(), file2.getName());
	}

	@Test
	public void testAutoRenameFileWhenMovingToFolderWhereIdenticalFilenameExists() {

		Settings.UniquePaths.setValue(Boolean.TRUE);

		Folder folder1 = null;
		Folder folder2 = null;
		File file1 = null;
		File file2 = null;

		try (final Tx tx = app.tx()) {

			folder1 = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), "/my/test/folder");
			assertNotNull(folder1);
			assertEquals(folder1.getPath(), "/my/test/folder");

			folder2 = FileHelper.createFolderPath(SecurityContext.getSuperUserInstance(), "/another/directory");
			assertNotNull(folder2);
			assertEquals(folder2.getPath(), "/another/directory");

			tx.success();

			file1 = app.create(File.class,
				new NodeAttribute<>(AbstractNode.name, "test.txt"),
				new NodeAttribute<>(StructrApp.key(File.class, "parent"), folder1)
			);

			assertNotNull(file1);
			assertEquals("Testfolder 1 should have exactly one child", 1, Iterables.count(folder1.getChildren()));

			file2 = app.create(File.class,
				new NodeAttribute<>(AbstractNode.name, "test.txt"),
				new NodeAttribute<>(StructrApp.key(File.class, "parent"), folder2)
			);

			assertNotNull(file2);
			assertEquals("Testfolder 2 should have exactly one child", 1, Iterables.count(folder2.getChildren()));

			tx.success();

		} catch (FrameworkException ex) {
			logger.error("", ex);
		}

		try (final Tx tx = app.tx()) {

			folder2.treeRemoveChild(file2);
			folder1.treeAppendChild(file2);

			assertEquals("Testfolder 1 should have exactly two children", 2, Iterables.count(folder1.getChildren()));
			assertEquals("Testfolder 2 should have no children", 0, Iterables.count(folder2.getChildren()));

			tx.success();

		} catch (FrameworkException ex) {
			ex.printStackTrace();
		}

		assertNotEquals(file1.getName(), file2.getName());
	}


	@Test
	public void testExtensionBasedMimeTypeDetection() {

		final Map<String, Map<String, byte[]>> testMap = new LinkedHashMap<>();

		testMap.put("text/html",              toMap(new Pair("test.html", "<!DOCTYPE html><html><head><title>Test</title></head><body><h1>Test</h1></body></html>".getBytes()), new Pair("test.htm", "<!DOCTYPE html>".getBytes())));
		testMap.put("text/plain",             toMap(new Pair("test.txt",  "Hello world!".getBytes())));
		testMap.put("text/css",               toMap(new Pair("test.css",  "body { background-color: #ffffff; }".getBytes())));
		testMap.put("application/javascript", toMap(new Pair("test.js",   "function() { alert('Test'); }".getBytes())));
		testMap.put("application/zip",        toMap(new Pair("test.zip",  "".getBytes())));
		testMap.put("image/jpeg",             toMap(new Pair("test.jpg",  "".getBytes()), new Pair("test.jpeg",   "".getBytes())));
		testMap.put("image/png",              toMap(new Pair("test.png",  "".getBytes())));

		try (final Tx tx = app.tx()) {

			for (final Entry<String, Map<String, byte[]>> entry : testMap.entrySet()) {

				final String mimeType = entry.getKey();

				for (final Entry<String, byte[]> fileEntry : entry.getValue().entrySet()) {

					final String fileName = fileEntry.getKey();
					final byte[] content  = fileEntry.getValue();

					try {
						final File file = FileHelper.createFile(securityContext, content, null, File.class, fileName, true);
						assertEquals("MIME type detection failed", mimeType, file.getContentType());

					} catch (IOException ioex) {

						logger.warn("", ioex);
						fail("Unexpected exception");
					}

				}
			}

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
		}


	}

	@Test
	public void testContentBasedMimeTypeDetection() {

		final Map<String, Map<String, byte[]>> testMap = new LinkedHashMap<>();

		try {

			// text-based formats will of course resolved into "text/plain"
			testMap.put("text/plain",               toMap(new Pair("test01", "<!DOCTYPE html><html><head><title>Test</title></head><body><h1>Test</h1></body></html>".getBytes())));
			testMap.put("text/plain",               toMap(new Pair("test02", "Hello world!".getBytes())));
			testMap.put("text/plain",               toMap(new Pair("test03", "body { background-color: #ffffff; }".getBytes())));

			// disabled because jmimemagic detects matlab..
			// testMap.put("text/plain",               toMap(new Pair("test04", "function test() { alert('Test'); return 'Hello world!'; }".getBytes())));

			testMap.put("application/zip",          toMap(new Pair("test05", IOUtils.toByteArray(UiTest.class.getResourceAsStream("/test/test.zip")))));
			testMap.put("image/jpeg",               toMap(new Pair("test06", IOUtils.toByteArray(UiTest.class.getResourceAsStream("/test/test.jpg")))));
			testMap.put("image/png",                toMap(new Pair("test07", IOUtils.toByteArray(UiTest.class.getResourceAsStream("/test/test.png")))));
			testMap.put("image/gif",                toMap(new Pair("test08", IOUtils.toByteArray(UiTest.class.getResourceAsStream("/test/test.gif")))));

			// disabled because jmimemagic v0.1.2 does not properly detect image/tiff cross-OS
			// testMap.put("image/tiff",               toMap(new Pair("test09", IOUtils.toByteArray(FileHelperTest.class.getResourceAsStream("/test/test.tiff")))));

			// disabled because jmimemagic v0.1.2 does not properly detect image/bmp cross-OS
			// testMap.put("image/bmp",                toMap(new Pair("test10", IOUtils.toByteArray(FileHelperTest.class.getResourceAsStream("/test/test.bmp")))));

			// disabled because jmimemagic v0.1.2 does not properly detect image/vnd.microsoft.icon cross-OS
			// testMap.put("image/vnd.microsoft.icon", toMap(new Pair("test11", IOUtils.toByteArray(FileHelperTest.class.getResourceAsStream("/test/test.ico")))));

		} catch (IOException ioex) {
			fail("Unexpected exception.");
		}

		try (final Tx tx = app.tx()) {

			for (final Entry<String, Map<String, byte[]>> entry : testMap.entrySet()) {

				final String mimeType = entry.getKey();

				for (final Entry<String, byte[]> fileEntry : entry.getValue().entrySet()) {

					final String fileName = fileEntry.getKey();
					final byte[] content  = fileEntry.getValue();

					try {
						final File file = FileHelper.createFile(securityContext, content, null, File.class, fileName, true);
						assertEquals("MIME type detection failed for " + fileName, mimeType, file.getContentType());

					} catch (IOException ioex) {

						logger.warn("", ioex);
						fail("Unexpected exception");
					}

				}
			}

			tx.success();

		} catch (FrameworkException fex) {

			fail("Unexpected exception");
		}
	}

	@Test
	public void testImageAndThumbnailDelete() {

		User tester = null;
		String uuid = null;

		try (final Tx tx = app.tx()) {

			final Image image = ImageHelper.createFileBase64(securityContext, base64Image, Image.class);
			tester            = app.create(User.class, "tester");

			image.setProperty(Image.name, "test.png");

			// allow non-admin user to delete the image
			image.grant(Permission.delete, tester);
			image.grant(Permission.read,   tester);

			image.getProperty(StructrApp.key(Image.class, "tnSmall"));
			image.getProperty(StructrApp.key(Image.class, "tnMid"));

			assertEquals("Image should have two thumbnails", 2, Iterables.count(image.getThumbnails()));

			uuid = image.getUuid();

			tx.success();

		} catch (IOException | FrameworkException fex) {

			fex.printStackTrace();
			fail("Unexpected exception");
		}

		final SecurityContext ctx = SecurityContext.getInstance(tester, AccessMode.Backend);
		final App testerApp       = StructrApp.getInstance(ctx);

		try (final Tx tx = testerApp.tx()) {

			final Image deleteMe = testerApp.get(Image.class, uuid);

			assertNotNull("Image should be visible to test user", deleteMe);

			testerApp.delete(deleteMe);

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}

		try (final Tx tx = testerApp.tx()) {

			final List<Image> images = testerApp.nodeQuery(Image.class).getAsList();

			if (!images.isEmpty()) {

				final SimpleDateFormat df = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss.S");

				for (final Image image : images) {

					System.out.println("Found Image " + image.getName() + " (" + image.getUuid() + "), created " + df.format(image.getCreatedDate()) + " which should have been deleted.");
				}
			}

			assertEquals("No images should be visible to test user", 0, images.size());

			tx.success();

		} catch (FrameworkException fex) {

			fex.printStackTrace();
		}
	}

	// ----- private methods -----
	private Class createTestImageType() {

		try (final Tx tx = app.tx()) {

			final JsonSchema schema = StructrSchema.createFromDatabase(app);
			final JsonType type     = schema.addType("TestImage");

			type.setExtends(URI.create("#/definitions/Image"));

			type.addCustomProperty("thumbnail", ThumbnailProperty.class.getName()).setFormat("200, 100, false");

			StructrSchema.extendDatabaseSchema(app, schema);

			tx.success();

		} catch (Throwable t) {
			t.printStackTrace();
		}

		return StructrApp.getConfiguration().getNodeEntityClass("TestImage");
	}
}
