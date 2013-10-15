/**
 * This software is based on OLAT, www.olat.org
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Copyright (c) JLS goodsolutions GmbH, Zurich, Switzerland. http://www.goodsolutions.ch <br>
 * All rights reserved.
 * <p>
 */
package org.olat.core.util;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.olat.core.CoreSpringFactory;
import org.olat.core.configuration.Destroyable;
import org.olat.core.configuration.Initializable;
import org.olat.core.helpers.Settings;
import org.olat.core.logging.OLog;
import org.olat.core.logging.StartupException;
import org.olat.core.logging.Tracing;
import org.olat.core.util.i18n.I18nModule;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.web.context.ServletContextAware;

/**
 * Description:<br>
 * <P>
 * Initial Date: 04.01.2007 <br>
 * 
 * @author Felix Jost
 */
public class WebappHelper implements Initializable, Destroyable, ServletContextAware {

	private static final OLog log = Tracing.createLoggerFor(WebappHelper.class);
	private static int nodeId;
	private static String fullPathToSrc;
	private static ServletContext servletContext;
	private static String contextRoot;
	private static String instanceId;
	private static String userDataRoot;
	private static String defaultCharset;
	private static Map<String, String> mailConfig = new HashMap<String, String>(6);
	private static long timeOfServerStartup = System.currentTimeMillis();

	/**
	 * need to set this at least once before the actual request, since we cannot extract it from the servletContext, but many methods use it (renderers) which do not have
	 * access to userRequest and thus to to getPathInfo...
	 **/
	private static String servletContextPath;
	private String applicationName;
	private String version;

	/**
	 * @see Initializable
	 */
	@Override
	public void init() {
		// servletContext.getRealPath("/"); does not work with an unpacked war file we only use it as fallback for unit testing
		Resource res = new ClassPathResource(CoreSpringFactory.class.getCanonicalName().replaceAll("\\.", "\\/") + ".class");
		try {
			String fullPath = res.getFile().getAbsolutePath();
			if (fullPath.contains("/WEB-INF")) {
				fullPath = fullPath.substring(0, fullPath.indexOf("/WEB-INF"));
			} else {
				fullPath = servletContext.getRealPath("/");
			}
			log.info("Sucessfully extracted context root path as: " + fullPath);
			contextRoot = fullPath;
		} catch (Exception e) {
			throw new StartupException("Error getting canonical context root.", e);
		}
		servletContextPath = servletContext.getContextPath();

		testUtf8FileSystem();
		logStartup();
	}

	/**
	 * [used by spring]
	 * 
	 * @param nodeId
	 */
	public void setNodeId(int nodeId) {
		WebappHelper.nodeId = nodeId;
	}

	/**
	 * @return
	 */
	public static int getNodeId() {
		return nodeId;
	}

	/**
	 * implements service interface
	 */
	@Override
	public void destroy() {
		log.info("");
		log.info("*********************************************");
		log.info("*                SHUTDOWM                    ");
		log.info("*********************************************");
		log.info("* Application:   " + applicationName);
		log.info("* StopTimeStamp: " + new Date());
		log.info("*********************************************");
		log.info("");
	}

	/**
	 * [spring]
	 * 
	 * @param applicationName
	 */
	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	/**
	 * @return the context path like "olat" in localhost:8080/olat
	 */
	public static String getServletContextPath() {
		return servletContextPath;
	}

	/**
	 * [used by JUnitTest]
	 * 
	 * @param contextPath
	 */
	public static void setServletContextPath(String contextPath) {
		servletContextPath = contextPath;
	}

	/**
	 * @param fileName
	 * @return
	 */
	public static String getMimeType(String fileName) {
		return servletContext.getMimeType(fileName.toLowerCase());
	}

	/**
	 * This path is depends how you deploy OLAT. When deploying in eclipse you will get an different path 
	 * than deploying within tomcat
	 * @return the root folder of the webapplication, e.g. /opt/olat3/olat/target/classes (no trailing slash)
	 * 
	 */
	public static String getContextRoot() {
		return contextRoot;
	}

	/**
	 * needed only for development; with debug mode enabled. The returned path does never end with a slash
	 * 
	 * @return the absolute path to the application webapp source directory, e.g. /opt/olat3/webapp/WEB-INF/src" (no trailing slash)
	 */
	public static String getSourcePath() {
		//String srcPath = getContextRoot() + "/" + relPathToSrc;
		File fil = new File(fullPathToSrc);
		if (fil.exists()) {
			log.info("Path to source set to: " + fullPathToSrc);
		} else {
			if (Settings.isDebuging() || I18nModule.isTransToolEnabled()) {
				log.error("Path to source wrong, debugging may not work as expected: " + fullPathToSrc, new Exception("getSourcePath"));
			} else {
				log.info("Path to source not valid: " + fullPathToSrc);
			}
		}

		return fullPathToSrc;
	}
	
	public static String getBuildOutputFolderRoot() {
		Resource res = new ClassPathResource("maven.build.properties");
		try {
			String path = res.getFile().getAbsolutePath();
			path = path.substring(0, path.lastIndexOf("/"));
			return path;
		} catch (IOException e) {
			throw new StartupException("could not find classpath resource: 'serviceconfig/olat.properties'", e);
		}
	}
	
	

	/**
	 * @return the time when the server was started up used e.g. for stats or a Last-modified of resources which are in jars and do not have a last-modified available
	 */
	public static Long getTimeOfServerStartup() {
		return timeOfServerStartup;
	}

	
	public void setFullPathToSrc(String fullPathToSrc) {
		WebappHelper.fullPathToSrc = fullPathToSrc;
	}
	
	/**
	 * @return
	 */
	public static String getInstanceId() {
		return instanceId;
	}

	/**
	 * [spring]
	 * 
	 * @param instanceId
	 */
	public void setInstanceId(String instanceId) {
		if (instanceId == null) throw new StartupException("No instance id set for this installation (see olat.properties). Please fix!");
		if (instanceId.length() > 10) throw new StartupException("InstanceID is limited to 10 characters (see olat.properties). Please fix!", null);
		WebappHelper.instanceId = instanceId;
	}

	public static String getUserDataRoot() {
		return userDataRoot;
	}

	/**
	 * [spring]
	 * 
	 * @param userDataRoot
	 */
	public void setUserDataRoot(String userDataRoot) {
		if (!StringHelper.containsNonWhitespace(userDataRoot)) {
			userDataRoot = System.getProperty("java.io.tmpdir") + "/olatdata";
			log.info("using java.io.tmpdir as userdata. this is the default if userdata.dir is not set");
		}
		File fUserData = new File(userDataRoot);
		if (!fUserData.exists()) {
			if (!fUserData.mkdirs()) throw new StartupException("Unable to create userdata dir '" + userDataRoot + "'. Please fix!");
		}
		log.info("Setting userdata root to: " + userDataRoot);
		WebappHelper.userDataRoot = userDataRoot;
	}

	/**
	 * [spring]
	 * 
	 * @param mailConfig
	 */
	public void setMailConfig(Map<String, String> mailConfig) {
		WebappHelper.mailConfig = mailConfig;
		String mailHost = WebappHelper.mailConfig.get("mailhost");
		if (mailHost == null) log.warn("Attention! mailhost fqdn is not set. The system has restricted functionality, e.g. Self Registration will not work!");
	}

	public static String getDefaultCharset() {
		if (defaultCharset == null) return "ISO-8859-1";
		return defaultCharset;
	}

	/**
	 * [spring]
	 * 
	 * @param defaultCharset
	 */
	public void setDefaultCharset(String defaultCharset) {
		WebappHelper.defaultCharset = defaultCharset;
	}

	/**
	 * key="mailhost" key="mailTimeout" key="smtpUser" key="smtpPwd" key="mailSupport" key="mailFrom"
	 * 
	 * @param string
	 * @return
	 */
	public static String getMailConfig(String key) {
		return WebappHelper.mailConfig.get(key);
	}

	public static boolean isMailHostAuthenticationEnabled() {
		return StringHelper.containsNonWhitespace(getMailConfig("smtpUser"));
	}

	/**
	 * Test if filesystem is capable to store UTF-8 characters Try to read/write a file with UTF-8 chars in the filename in a temporary directory.
	 */
	private void testUtf8FileSystem() {
		File tmpDir = new File(new File(WebappHelper.getUserDataRoot()), "tmp");
		if (!tmpDir.exists()) tmpDir.mkdir();
		File writeFile = new File(tmpDir, "UTF-8 test läsÖiç-首页|新");
		if (writeFile.exists()) {
			// remove exising files first
			writeFile.delete();
		}
		try {
			writeFile.createNewFile();
		} catch (IOException e) {
			log.warn("No UTF-8 capable filesystem found! Error while writing testfile to filesystem", e);
		}
		// try to lookup file: get files from filesystem and search for file we created above
		File[] tmpFiles = tmpDir.listFiles();
		boolean foundUtf8File = false;
		if (tmpFiles != null) {
			for (int i = 0; i < tmpFiles.length; i++) {
				File tmpFile = tmpFiles[i];
				if (tmpFile.getName().equals("UTF-8 test läsÖiç-首页|新")) {
					foundUtf8File = true;
					break;
				}
			}
		}
		if (foundUtf8File) {
			// test ok
			log.info("UTF-8 capable filesystem detected");
		} else {
			// test failed
			log.warn("No UTF-8 capable filesystem found! Could not read / write UTF-8 characters from / to filesystem! "
					+ "You probably misconfigured your system, try setting your LC_HOME variable to a correct value.");
			log.warn("Your current file encoding configuration: java.nio.charset.Charset.defaultCharset().name()::" + java.nio.charset.Charset.defaultCharset().name()
					+ " (the one used) and your system property file.encoding::" + System.getProperty("file.encoding") + " (the one configured)");
		}
		// try to delete file anyway
		writeFile.delete();
	}

	/**
	 * @return The full OLAT licencse as a string
	 */
	public static String getOlatLicense() {
		Resource license = new ClassPathResource("NOTICE.TXT");
		String licenseS = "";
		if (license.exists()) {
			licenseS = FileUtils.load(license, "UTF-8");
		}
		Resource copyLicense = new ClassPathResource("COPYING");
		String copyLicenseS = "";
		if (copyLicense.exists()) {
			copyLicenseS = FileUtils.load(copyLicense, "UTF-8");
		}
		return licenseS + "<br /><br />" + copyLicenseS;
	}

	/**
	 * Either the version of the core or the webapps provided version gets printed
	 */
	private void logStartup() {
		log.info("");
		log.info("*********************************************");
		log.info("*                STARTUP                     ");
		log.info("*********************************************");
		log.info("* Application:    " + applicationName);
		log.info("* Version:        " + version);
		log.info("* StartTimeStamp: " + new Date());
		log.info("*********************************************");
		log.info("");
	}

	public void setVersion(String version) {
		this.version = version;
	}

	@Override
	public void setServletContext(ServletContext servletContext) {
		WebappHelper.servletContext = servletContext;
	}

	

}
