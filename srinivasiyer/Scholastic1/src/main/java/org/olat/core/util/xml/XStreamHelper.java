/**
 * OLAT - Online Learning and Training<br>
 * http://www.olat.org
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
 * Copyright (c) 1999-2006 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */
package org.olat.core.util.xml;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.util.FileUtils;
import org.olat.core.util.vfs.VFSLeaf;

import com.thoughtworks.xstream.XStream;

/**
 * Description:<br>
 * The XStreamHelper provides you with some methods to quickly save and restore objects on disk using XML. This helper does the try/catch and writing and reading from/to
 * the disk.
 * <p>
 * Using xStream without configuration is not recommended. This persists the class using the class name which makes it very difficult to refactor the class at a later
 * stage.
 * <p>
 * It is recommended to have a private XStream instance in the manager of your package. You can create such an instance using the createXStreamInstance() method. Note
 * that the XStream instance is threadsave, it is normally ok to have one stream per manager. Now you can use field and attribute mappers to define how the XML should
 * look like.
 * <p>
 * <code>
 * private static final XStream mystream;
 * static {
 * 	mystream = XStreamHelper.createXStreamInstance();
 *  mystream.alias("user", UserImpl.class);
 *  mystream.alias("identity", IdentityImpl.class);
 *  mystream.alias("addr", AddressImpl.class);
 * }
 * ... 
 * XStreamHelper.writeObject(mystream, myfile, myIdentity);
 * ...
 * Identity ident = (Identity) XStreamHelper.readObject(mystream, myfile);
 * <code>
 * <p>
 * 
 * @see http://xstream.codehaus.org/alias-tutorial.html <p> Initial Date: 01.12.2004 <br>
 * @author Felix Jost, Florian Gnaegi
 */
public class XStreamHelper {
	private static final String ENCODING = "UTF-8";

	private static XStream unconfiguredXStream = new XStream();

	/**
	 * Write a an object to an XML file. UTF-8 is used as encoding
	 * <p>
	 * This method uses an unconfigured XStream, thus a default mapping which includes the object class name will be used. This should only be used for quick prototyping.
	 * For long term persisting of data in real applications, hide classnames and attributes by mapping attributes and fields on the xStream instance.
	 * <p>
	 * Use writeObject(XStream stream, VFSLeaf vfsLeaf, Object obj) and configure the mapping there!
	 * 
	 * @param vfsLeaf
	 * @param obj
	 */
	@Deprecated
	public static void writeObject(VFSLeaf vfsLeaf, Object obj) {
		writeObject(unconfiguredXStream, vfsLeaf, obj);
	}

	/**
	 * Write a an object to an XML file. UTF-8 is used as encoding
	 * <p>
	 * This method uses an unconfigured XStream, thus a default mapping which includes the object class name will be used. This should only be used for quick prototyping.
	 * For long term persisting of data in real applications, hide classnames and attributes by mapping attributes and fields on the xStream instance.
	 * <p>
	 * Use writeObject(XStream stream, File file, Object obj) and configure the mapping there!
	 * 
	 * @param file
	 * @param obj
	 */
	@Deprecated
	public static void writeObject(File file, Object obj) {
		writeObject(unconfiguredXStream, file, obj);
	}

	/**
	 * Write a an object to an XML file. UTF-8 is used as encoding
	 * <p>
	 * This method uses an unconfigured XStream, thus a default mapping which includes the object class name will be used. This should only be used for quick prototyping.
	 * For long term persisting of data in real applications, hide classnames and attributes by mapping attributes and fields on the xStream instance.
	 * <p>
	 * Use writeObject(XStream stream, OutputStream os, Object obj) and configure the mapping there!
	 * 
	 * @param file
	 * @param obj
	 */
	@Deprecated
	public static void writeObject(OutputStream os, Object obj) {
		writeObject(unconfiguredXStream, os, obj);
	}

	/**
	 * Create an XML string from the given object using an unconfigured XStream
	 * 
	 * @param obj
	 * @return the Object in XStream form as an xml-String
	 */
	@Deprecated
	public static String toXML(Object obj) {
		return unconfiguredXStream.toXML(obj);
	}

	/**
	 * Create an object from the given XML using an unconfigured XStream
	 * 
	 * @param xml
	 * @return the Object reconstructed from the xml structure
	 */
	@Deprecated
	public static Object fromXML(String xml) {
		return unconfiguredXStream.fromXML(xml);
	}

	/**
	 * clones an object with the library XStream. The object to be cloned does not need to be serializable, but must have a default constructor.
	 * 
	 * @param in
	 * @return the clone Object
	 */
	public static Object xstreamClone(Object in) {
		String data = unconfiguredXStream.toXML(in);
		Object out = unconfiguredXStream.fromXML(data);
		return out;
	}

	/**
	 * Read a structure from XML from the given input stream
	 * <p>
	 * This method uses an unconfigured XStream, thus a default mapping which includes the object class name will be used. This should only be used for quick prototyping.
	 * For long term persisting of data in real applications, hide classnames and attributes by mapping attributes and fields on the xStream instance.
	 * <p>
	 * Use readObject(XStream stream, InputStream is) and configure the mapping there!
	 * 
	 * @param is
	 * @return the object
	 */
	@Deprecated
	public static Object readObject(InputStream is) {
		return readObject(unconfiguredXStream, is);
	}

	/**
	 * Read a structure from XML file within the provided folder.
	 * <p>
	 * This method uses an unconfigured XStream, thus a default mapping which includes the object class name will be used. This should only be used for quick prototyping.
	 * For long term persisting of data in real applications, hide classnames and attributes by mapping attributes and fields on the xStream instance.
	 * <p>
	 * Use readObject(XStream stream, File file) and configure the mapping there!
	 * 
	 * @param file
	 * @return de-serialized object
	 * @throws OLATRuntimeException if de-serialization fails.
	 */
	@Deprecated
	public static Object readObject(File file) {
		return readObject(unconfiguredXStream, file);
	}

	/**
	 * Factory to create a fresh XStream instance. Use this when reading and writing to a configured XML mapping
	 */
	public static XStream createXStreamInstance() {
		return new XStream();
	}

	/**
	 * Read an object from the given file using the xStream object. It is usefull to add field and attribute mappers to the stream.
	 * 
	 * @param xStream The (configured) xStream.
	 * @param file
	 * @return
	 */
	public static Object readObject(XStream xStream, File file) {
		FileInputStream fis = null;
		BufferedInputStream bis = null;
		try {
			fis = new FileInputStream(file);
			bis = new BufferedInputStream(fis);
			return readObject(xStream, bis);
		} catch (IOException e) {
			throw new OLATRuntimeException(XStreamHelper.class, "could not read Object from file: " + file.getAbsolutePath(), e);
		} finally {
			try {
				if (fis != null) fis.close();
				if (bis != null) bis.close();
			} catch (Exception e) {
				// we did our best to close the inputStream
			}
		}
	}

	/**
	 * Read an object from the given leaf using the xStream object. It is usefull to add field and attribute mappers to the stream.
	 * 
	 * @param xStream The (configured) xStream.
	 * @param file
	 * @return
	 */
	public static Object readObject(XStream xStream, VFSLeaf file) {
		InputStream fis = null;
		BufferedInputStream bis = null;
		try {
			fis = file.getInputStream();
			bis = new BufferedInputStream(fis);
			return readObject(xStream, bis);
		} catch (Exception e) {
			throw new OLATRuntimeException(XStreamHelper.class, "could not read Object from file: " + file.getName(), e);
		} finally {
			try {
				if (fis != null) fis.close();
				if (bis != null) bis.close();
			} catch (Exception e) {
				// we did our best to close the inputStream
			}
		}
	}

	/**
	 * Read an object from the given input stream using the xStream object. It is usefull to add field and attribute mappers to the stream.
	 * 
	 * @param xStream The (configured) xStream.
	 * @param is
	 * @return
	 */
	public static Object readObject(XStream xStream, InputStream is) {
		try {
			InputStreamReader isr = new InputStreamReader(is, ENCODING);
			Object obj = xStream.fromXML(isr);
			isr.close();
			is.close();
			return obj;
		} catch (Exception e) {
			throw new OLATRuntimeException(XStreamHelper.class, "could not read Object from inputstream: " + is, e);
		} finally {
			FileUtils.closeSafely(is);
		}
	}

	/**
	 * Write a an object to an XML file. UTF-8 is used as encoding. It is useful to set attribute and field mappers to allow later refactoring of the class!
	 * 
	 * @param xStream The (configured) xStream.
	 * @param vfsLeaf
	 * @param obj the object to be serialized
	 */
	public static void writeObject(XStream xStream, VFSLeaf vfsLeaf, Object obj) {
		writeObject(xStream, vfsLeaf.getOutputStream(false), obj);
	}

	/**
	 * Write a an object to an XML file. UTF-8 is used as encoding. It is useful to set attribute and field mappers to allow later refactoring of the class!
	 * 
	 * @param xStream The (configured) xStream.
	 * @param file
	 * @param obj the object to be serialized
	 */
	public static void writeObject(XStream xStream, File file, Object obj) {
		try {
			writeObject(xStream, new FileOutputStream(file), obj);
		} catch (Exception e) {
			throw new OLATRuntimeException(XStreamHelper.class, "Could not write object to file: " + file.getAbsolutePath(), e);
		}
	}

	/**
	 * Write a an object to an output stream. UTF-8 is used as encoding in the XML declaration. It is useful to set attribute and field mappers to allow later refactoring
	 * of the class!
	 * 
	 * @param xStream The (configured) xStream.
	 * @param os
	 * @param obj the object to be serialized
	 */
	public static void writeObject(XStream xStream, OutputStream os, Object obj) {
		try {
			OutputStreamWriter osw = new OutputStreamWriter(os, ENCODING);
			String data = xStream.toXML(obj);
			data = "<?xml version=\"1.0\" encoding=\"" + ENCODING + "\"?>\n" + data; // give a decent header with the encoding used
			osw.write(data);
			osw.close();
		} catch (Exception e) {
			throw new OLATRuntimeException(XStreamHelper.class, "Could not write object to stream.", e);
		} finally {
			FileUtils.closeSafely(os);
		}
	}

}