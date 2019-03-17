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
package org.structr.util;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.security.CodeSigner;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Base64.Decoder;
import java.util.Base64.Encoder;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang3.RandomUtils;
import org.codehaus.plexus.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.structr.api.service.Feature;
import org.structr.api.service.LicenseManager;
import org.structr.common.error.FrameworkException;
import org.structr.core.graph.MaintenanceCommand;
import org.structr.core.graph.NodeServiceCommand;

/**
 */
public class StructrLicenseManager implements LicenseManager {

	private static final Logger logger      = LoggerFactory.getLogger(LicenseManager.class);
	private static final String Certificate =
		"MIIDpzCCAo+gAwIBAgIEbgcR/TANBgkqhkiG9w0BAQ0FADCBgzELMAkGA1UEBhMC" +
		"REUxDzANBgNVBAgTBkhlc3NlbjEaMBgGA1UEBxMRRnJhbmtmdXJ0IGFtIE1haW4x" +
		"FTATBgNVBAoTDFN0cnVjdHIgR21iSDEUMBIGA1UECxMLRGV2ZWxvcG1lbnQxGjAY" +
		"BgNVBAMTEUNocmlzdGlhbiBNb3JnbmVyMB4XDTE3MDUxNzExMjExMloXDTI4MDQy" +
		"OTExMjExMlowgYMxCzAJBgNVBAYTAkRFMQ8wDQYDVQQIEwZIZXNzZW4xGjAYBgNV" +
		"BAcTEUZyYW5rZnVydCBhbSBNYWluMRUwEwYDVQQKEwxTdHJ1Y3RyIEdtYkgxFDAS" +
		"BgNVBAsTC0RldmVsb3BtZW50MRowGAYDVQQDExFDaHJpc3RpYW4gTW9yZ25lcjCC" +
		"ASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEBAI2i0WhHV9OauRZ6viefhuSK" +
		"OZaKlTvVYi0HLmk99/JlfIvAkkmS3Dlz+xwp9SUUPdfFMvPvMpgwXyfj9HlZGMg6" +
		"fdSGocZyJpozVSmzlM0D1xJB6MbU1k8lcvI52IYx6gUCa7jQIoPxkwb+8/NP2SXA" +
		"56RVIsNQFGpb31AwKNFViE5CqCHcF4hQ3uB/rcbxPUQ9t94R51dNLMWw37lKiqpq" +
		"iHGKYmriBOV9iivdgUFFFA0hNbclnRImXhYuUgCBaPmtjOGDywBfkRs5kbRBixR5" +
		"V41DOqaFnG5jOKTW+ycLMMmtvHnPt1dHckvYTnnM0YUS9FINbXLoxZBw2hP1mfkC" +
		"AwEAAaMhMB8wHQYDVR0OBBYEFBM6fjBbxeespRRG87mEHbqzfXCGMA0GCSqGSIb3" +
		"DQEBDQUAA4IBAQANnACQFo5r8gNK8ULZ5yvndx8Bv3YNbQEtXgHSnaQLWAgIgflG" +
		"E4GF8/0OwaxVuGxvyCd/ib74vjfCgOJvNe+XOHQQ+6o30JaILar2353QvVS5cqSU" +
		"HACOyFaBa7ndjgHSYdzabZHdhmpIIv/Tx2whClYdCCLTQi1sYjtXGMJ4FJyyJqjm" +
		"IjREcPU1KT8etnYQXaTvj2njM26lZVWc7DizsN83b+vL5h2m+z/4I5dAwVGEBMFZ" +
		"/u7r3lfqJ4h5bVTb6AMJ9gl/lOyob46gH1jNbx5ld53/ADgDFRcMW2vwLiZhay0p" +
		"Nhx1o8vT2VnQUzZxU1G4gnkdtiXN6knFTorl";

	public static final String DataEncryptionAlgorithm = "AES/CBC/PKCS5Padding";
	public static final String KeyEncryptionAlgorithm  = "RSA/ECB/OAEPWithSHA-256AndMGF1Padding";
	public static final String SignatureAlgorithm      = "SHA512WithRSA";
	public static final String DatePattern             = "yyyy-MM-dd";
	public static final String KeystoreAlias           = "structr";
	public static final String KeyAlgorithm            = "RSA";
	public static final String CharSet                 = "UTF-8";
	public static final String NameKey                 = "name";
	public static final String DateKey                 = "date";
	public static final String StartKey                = "start";
	public static final String EndKey                  = "end";
	public static final String EditionKey              = "edition";
	public static final String ModulesKey              = "modules";
	public static final String MachineKey              = "machine";
	public static final String SignatureKey            = "key";
	public static final String ServersKey              = "servers";
	public static final String HostIdKey               = "hostId";
	public static final String LimitKey                = "limit";
	public static final String HostIdMappingKey        = "hostIdValidationAttempts";
	public static final int ServerPort                 = 5725;

	private static final int CommunityMask              = 0x01; // 0001
	private static final int BasicMask                  = 0x03; // 0011
	private static final int SmallBusinessMask          = 0x07; // 0111
	private static final int EnterpriseMask             = 0x0f; // 1111

	private final Set<String> modules                   = new LinkedHashSet<>(Arrays.asList("core", "rest", "ui"));
	private final Set<String> classes                   = new LinkedHashSet<>();
	private final SimpleDateFormat format               = new SimpleDateFormat(DatePattern);
	private Certificate certificate                     = null;
	private PublicKey publicKey                         = null;
	private boolean allModulesLicensed                  = false;
	private boolean licensePresent                      = false;
	private String edition                              = "Community";
	private String licensee                             = null;
	private String startDateString                      = null;
	private String endDateString                        = null;
	private int editionMask                             = CommunityMask;

	public StructrLicenseManager(final String licenseFileName) {

		logger.info("Host ID is {}", createHash());
		logger.info("Checking Structr license..");

		// initialize certificate from static data above
		this.certificate = certFromBase64(Certificate);
		this.publicKey   = certificate.getPublicKey();

		// check license
		final Map<String, String> properties = read(licenseFileName);
		if (licensePresent) {

			// read license file (if present)
			if (isValid(properties)) {

				// init edition
				edition = properties.get(EditionKey);
				if (edition != null) {

					switch (edition) {

						case "Enterprise":
							editionMask = EnterpriseMask;
							break;

						case "Small Business":
							editionMask = SmallBusinessMask;
							break;

						case "Basic":
							editionMask = BasicMask;
							break;
					}
				}

				licensee        = properties.get(NameKey);
				startDateString = properties.get(StartKey);
				endDateString   = properties.get(EndKey);

				// init modules
				final String licensedModules = properties.get(ModulesKey);
				if (licensedModules != null) {

					// remove default modules from Community Edition
					modules.clear();

					if ("*".equals(licensedModules)) {

						allModulesLicensed = true;

					} else if (!"none".equals(licensedModules)) {

						for (final String module : licensedModules.split(",")) {

							modules.add(module.trim());
						}
					}
				}
			}
		}

		logger.info("Running {} Edition with {}.", edition, allModulesLicensed ? "all modules" : "modules " + modules.toString());

		if (licensee != null) {

			logger.info("Licensed for {}", licensee);

		} else {

			logger.info("Evaluation License");
		}
	}

	@Override
	public boolean isModuleLicensed(final String module) {
		return allModulesLicensed || modules.contains(module);
	}

	@Override
	public boolean isClassLicensed(final String fqcn) {
		return allModulesLicensed || classes.contains(fqcn);
	}

	@Override
	public boolean isEdition(final int bitmask) {
		return (editionMask & bitmask) > 0;
	}

	@Override
	public String getEdition() {
		return edition;
	}

	@Override
	public String getLicensee() {
		return licensee;
	}

	@Override
	public String getHardwareFingerprint() {
		return createHash();
	}

	@Override
	public String getStartDate() {
		return startDateString;
	}

	@Override
	public String getEndDate() {
		return endDateString;
	}

	@Override
	public boolean isValid(final Feature feature) {

		if (feature != null) {

			final String moduleName = feature.getModuleName();
			return moduleName != null && isModuleLicensed(moduleName);
		}

		return false;
	}

	/**
	 *
	 * @param codeSigners
	 * @return
	 */
	@Override
	public boolean isValid(final CodeSigner[] codeSigners) {

		if (codeSigners != null && codeSigners.length > 0) {

			for (final CodeSigner codeSigner : codeSigners) {

				for (final Certificate cert : codeSigner.getSignerCertPath().getCertificates()) {

					try {

						cert.verify(publicKey);
						return true;

					} catch (Throwable ignore) {}
				}
			}
		}

		// none of the code signer certificates could be verified using our key => not valid
		return false;
	}

	@Override
	public void addLicensedClass(final String fqcn) {
		classes.add(fqcn);
	}

	// ----- private methods -----
	private boolean isValid(final Map<String, String> properties) {

		if (!licensePresent) {
			return false;
		}

		final String src             = collectLicenseFieldsForSignature(properties);
		final String name            = properties.get(NameKey);
		final String key             = properties.get(SignatureKey);
		final String hostId          = properties.get(MachineKey);
		final String thisHostId      = createHash();
		final String edition         = properties.get(EditionKey);
		final String modules         = properties.get(ModulesKey);
		final String dateString      = properties.get(DateKey);
		final String startDateString = properties.get(StartKey);
		final String endDateString   = properties.get(EndKey);
		final String serversString   = properties.get(ServersKey);
		final Date now               = new Date();

		if (StringUtils.isEmpty(key)) {

			logger.error("Unable to read key from license file.");
			return false;
		}

		try {

			final byte[] data      = src.getBytes(CharSet);
			final Signature signer = Signature.getInstance(SignatureAlgorithm);
			final byte[] signature = Hex.decodeHex(key.toCharArray());

			signer.initVerify(certificate);
			signer.update(data);

			if (!signer.verify(signature)) {

				logger.error("License signature verification failed, license is not valid.");
				return false;
			}

		} catch (Throwable t) {

			logger.error("Unable to verify license.", t);
			return false;
		}

		if (StringUtils.isEmpty(name)) {

			logger.error("License file doesn't contain licensee name.");
			return false;
		}

		if (StringUtils.isEmpty(edition)) {

			logger.error("License file doesn't contain edition.");
			return false;
		}

		if (StringUtils.isEmpty(modules)) {

			logger.error("License file doesn't contain modules.");
			return false;
		}

		if (StringUtils.isEmpty(hostId)) {

			logger.error("License file doesn't contain host ID.");
			return false;
		}

		if (StringUtils.isEmpty(dateString)) {

			logger.error("License file doesn't contain license date.");
			return false;
		}

		if (StringUtils.isEmpty(startDateString)) {

			logger.error("License file doesn't contain start date.");
			return false;
		}

		if (StringUtils.isEmpty(endDateString)) {

			logger.error("License file doesn't contain end date.");
			return false;
		}

		// verify host ID
		if (!thisHostId.equals(hostId) && !"*".equals(hostId)) {

			logger.error("Host ID found in license ({}) file does not match current host ID.", hostId);
			return false;
		}

		if ("*".equals(hostId)) {

			// check volume license against server addresses
			if (StringUtils.isNotBlank(serversString)) {

				// send HostID to server
				properties.put(HostIdKey, thisHostId);

				return checkVolumeLicense(properties, serversString);
			}

			final Calendar issuedAtPlusOneMonth = GregorianCalendar.getInstance();
			final Calendar cal                  = GregorianCalendar.getInstance();

			// set issuedAt to license date plus one month
			issuedAtPlusOneMonth.setTime(parseDate(dateString));
			issuedAtPlusOneMonth.add(Calendar.MONTH, 1);

			// check that the license file was issued not more than one month ago
			if (cal.after(issuedAtPlusOneMonth)) {

				logger.error("Development license found in license file is not valid any more, license period ended {}.", format.format(issuedAtPlusOneMonth.getTime()));
				return false;
			}
		}

		// verify that the license is valid for the current date
		final Date startDate = parseDate(startDateString);
		if (startDate != null && now.before(startDate) && !now.equals(startDate)) {

			logger.error("License found in license file is not yet valid, license period starts {}.", format.format(startDate.getTime()));
			return false;
		}

		// verify that the license is valid for the current date
		final Date endDate = parseDate(endDateString);
		if (endDate != null && now.after(endDate) && !now.equals(endDate)) {

			logger.error("License found in license file is not valid any more, license period ended {}.", format.format(endDate.getTime()));
			return false;
		}

		return true;
	}

	private Map<String, String> read(final String fileName) {

		final Decoder base64Decoder          = java.util.Base64.getMimeDecoder();
		final Map<String, String> properties = new LinkedHashMap<>();

		try (final BufferedReader reader = new BufferedReader(new InputStreamReader(base64Decoder.wrap(new FileInputStream(fileName))))) {

			licensePresent = true;

			String line = reader.readLine();
			while (line != null) {

				final int pos = line.indexOf("=");
				if (pos >= 0) {

					final String key   = line.substring(0, pos).trim();
					final String value = line.substring(pos+1).trim();

					properties.put(key, value);
				}

				line = reader.readLine();
			}

		} catch (FileNotFoundException fnex) {
			licensePresent = false;
		} catch (IOException ioex) {
			logger.warn("Error reading license file: {}", ioex.getMessage());
		}

		return properties;
	}

	private Certificate certFromBase64(final String src) {

		try {

			final byte[] byteKey = Base64.decode(src.getBytes());

			return CertificateFactory.getInstance("X.509").generateCertificate(new ByteArrayInputStream(byteKey));

		} catch (Throwable t) {
			logger.warn("Unable to decode public key.", t);
		}

		return null;
	}

	private String createHash() {

		try {

			final MessageDigest digest = MessageDigest.getInstance("MD5");

			// use network interface hardware addresses for host identification
			for (final NetworkInterface iface : getNetworkInterfaces()) {

				try {
					final byte[] hardwareAddress = iface.getHardwareAddress();
					if (hardwareAddress != null) {

						digest.update(hardwareAddress);
					}

				} catch (SocketException ex) {}
			}

			return Hex.encodeHexString(digest.digest());

		} catch (NoSuchAlgorithmException ex) {
			logger.warn("Unable to create hardware hash.", ex);
		}

		return null;
	}

	private Iterable<NetworkInterface> getNetworkInterfaces() {

		final List<NetworkInterface> interfaces = new LinkedList<>();

		try {
			for (final Enumeration<NetworkInterface> enumeration = NetworkInterface.getNetworkInterfaces(); enumeration.hasMoreElements();) {

				interfaces.add(enumeration.nextElement());
			}

		} catch (SocketException ex) {}

		return interfaces;
	}

	private Date parseDate(final String dateOrNull) {

		if (dateOrNull != null) {

			try {
				return alignToDay(format.parse(dateOrNull));

			} catch (Throwable ignore) {}
		}

		return new Date(0L);
	}

	private Date alignToDay(final Date date) {

		try {
			return format.parse(format.format(date));

		} catch (Throwable ignore) {}

		return null;
	}

	// ----- private static methods -----
	public static String collectLicenseFieldsForSignature(final Map<String, String> properties) {

		final StringBuilder buf = new StringBuilder();

		buf.append(properties.get(NameKey));
		buf.append(properties.get(DateKey));
		buf.append(properties.get(StartKey));
		buf.append(properties.get(EndKey));
		buf.append(properties.get(EditionKey));
		buf.append(properties.get(ModulesKey));
		buf.append(properties.get(MachineKey));

		// optional value
		final String servers = properties.get(ServersKey);
		if (StringUtils.isNotBlank(servers)) {

			buf.append(servers);
		}

		return buf.toString();
	}

	private static void create(final String name, final String start, final String end, final String edition, final String modules, final String hostId, final String servers, final String keystoreFileName, final String password, final String outputFileName) {

		final Map<String, String> properties = new LinkedHashMap<>();
		final SimpleDateFormat format        = new SimpleDateFormat(DatePattern);

		properties.put(NameKey,    name);
		properties.put(DateKey,    format.format(System.currentTimeMillis()));
		properties.put(StartKey,   start);
		properties.put(EndKey,     end);
		properties.put(EditionKey, edition);
		properties.put(ModulesKey, modules);
		properties.put(MachineKey, hostId);

		if (StringUtils.isNotBlank(servers)) {
			properties.put(ServersKey, servers);
		}

		sign(properties, keystoreFileName, password);
		write(properties, outputFileName);
	}

	private static void sign(final Map<String, String> properties, final String keystoreFileName, final String password) {

		final String src = collectLicenseFieldsForSignature(properties);

		try {

			final byte[] data       = src.getBytes(CharSet);
			final Signature signer  = Signature.getInstance(SignatureAlgorithm);
			final KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());

			try (final InputStream is = new FileInputStream(keystoreFileName)) {

				keyStore.load(is, password.toCharArray());

				final Key key = keyStore.getKey(KeystoreAlias, password.toCharArray());

				signer.initSign((PrivateKey)key);
				signer.update(data);

				properties.put(SignatureKey, Hex.encodeHexString(signer.sign()));
			}

		} catch (Throwable t) {
			logger.warn("Unable to sign license.", t);
		}
	}

	private static void write(final Map<String, String> properties, final String fileName) {

		final Encoder base64Encoder = java.util.Base64.getEncoder();

		try (final Writer writer = new OutputStreamWriter(base64Encoder.wrap(new FileOutputStream(fileName)))) {

			writer.write(write(properties));

		} catch (IOException ioex) {
			logger.warn("Unable to write file.", ioex);
		}
	}

	private static String write(final Map<String, String> properties) {

		final StringBuilder buf = new StringBuilder();

		properties.forEach((key, value) -> {

			buf.append(key);
			buf.append(" = ");
			buf.append(value);
			buf.append("\n");
		});

		return buf.toString();

	}

	private boolean checkVolumeLicense(final Map<String, String> properties, final String serversString) {

		try {

			final KeyGenerator kgen = KeyGenerator.getInstance("AES");
			final byte[] data       = write(properties).getBytes("utf-8");
			final String name       = properties.get(NameKey);
			final byte[] expected   = name.getBytes("utf-8");

			kgen.init(128);

			for (final String part : serversString.split("[, ]+")) {

				final String address = part.trim();

				if (StringUtils.isNotBlank(address)) {

					try {

						logger.info("Trying to verify volume license with server {}", address);

						final long t0              = System.currentTimeMillis();
						final SecretKey aesKey     = kgen.generateKey(); // symmetric stream key
						final byte[] ivspec        = RandomUtils.nextBytes(16); // initialization vector for stream cipher
						final byte[] key           = encryptSessionKey(aesKey.getEncoded());
						final byte[] encryptedIV   = encryptSessionKey(ivspec);
						final byte[] encryptedData = encryptData(data, aesKey, ivspec);
						final byte[] response      = sendAndReceive(address, key, encryptedIV, encryptedData);
						final boolean result       = verify(expected, response);

						if (result == true) {
							logger.info("License verified in {} ms", System.currentTimeMillis() - t0);
						}

						return result;

					} catch (Throwable t) {
						logger.warn("Unable to verify volume license: {}", t.getMessage());
					}
				}
			}

		} catch (Throwable t) {
			t.printStackTrace();
		}

		return false;
	}

	private byte[] encryptData(final byte[] data, final SecretKey sessionKey, final byte[] ivSpec) throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, UnsupportedEncodingException, InvalidAlgorithmParameterException {

		// setup
		final Cipher cipher = Cipher.getInstance(DataEncryptionAlgorithm);

		cipher.init(Cipher.ENCRYPT_MODE, sessionKey, new IvParameterSpec(ivSpec));

		return cipher.doFinal(data);
	}

	private byte[] encryptSessionKey(final byte[] sessionKey) throws NoSuchAlgorithmException, NoSuchPaddingException, IllegalBlockSizeException, InvalidKeyException, BadPaddingException, UnsupportedEncodingException {

		// setup
		final Cipher cipher = Cipher.getInstance(KeyEncryptionAlgorithm);

		cipher.init(Cipher.ENCRYPT_MODE, certificate);

		return cipher.doFinal(sessionKey);
	}

	private byte[] sendAndReceive(final String address, final byte[] key, final byte[] ivspec, final byte[] data) {

		final byte[] result = new byte[256];

		try(final Socket socket = new java.net.Socket(address, ServerPort)) {

			socket.getOutputStream().write(key);
			socket.getOutputStream().flush();

			socket.getOutputStream().write(ivspec);
			socket.getOutputStream().flush();

			socket.getOutputStream().write(data);
			socket.getOutputStream().flush();

			// don't waste much time to wait for an answer
			socket.setSoTimeout(2000);

			// read exactly 256 bytes (size of expected signature response)
			socket.getInputStream().read(result, 0, 256);

		} catch (Throwable  t) {
			logger.warn("Unable to verify volume license: {}", t.getMessage());
		}

		return result;
	}

	private boolean verify(final byte[] data, final byte[] signatureData) {

		try {

			final Signature verifier = Signature.getInstance(SignatureAlgorithm);

			verifier.initVerify(certificate);
			verifier.update(data);

			if (verifier.verify(signatureData)) {

				return true;
			}

		} catch (Throwable t) {
			logger.warn("Unable to verify volume license: {}", t.getMessage());
		}

		logger.error("License verification failed, license is not valid.");

		return false;
	}

	// ----- nested classes ------
	public static class CreateLicenseCommand extends NodeServiceCommand implements MaintenanceCommand {

		@Override
		public void execute(final Map<String, Object> attributes) throws FrameworkException {

			final String name     = (String)attributes.get(NameKey);
			final String start    = (String)attributes.get(StartKey);
			final String end      = (String)attributes.get(EndKey);
			final String edition  = (String)attributes.get(EditionKey);
			final String modules  = (String)attributes.get(ModulesKey);
			final String hostId   = (String)attributes.get(MachineKey);
			final String servers  = (String)attributes.get(ServersKey);
			final String keystore = (String)attributes.get("keystore");
			final String password = (String)attributes.get("password");

			// outfile, fallback to license.key
			String outFile = (String)attributes.get("outFile");
			if (outFile == null) {
				outFile = "license.key";
			}

			if (name == null || start == null || end == null || edition == null || modules == null || hostId == null || keystore == null || password == null) {

				logger.warn("Cannot create license file, missing parameter. Parameters are: name, start, end, edition, modules, machine, keystore, password, outFile (optional).");

			} else {

				StructrLicenseManager.create(name, start, end, edition, modules, hostId, servers, keystore, password, outFile);
				logger.info("Successfully created license file {}.", outFile);
			}
		}

		@Override
		public boolean requiresEnclosingTransaction() {
			return false;
		}

		@Override
		public boolean requiresFlushingOfCaches() {
			return false;
		}
	}
}
