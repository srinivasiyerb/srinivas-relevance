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

package org.olat.core.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.StringTokenizer;

import net.sf.jazzlib.ZipEntry;
import net.sf.jazzlib.ZipInputStream;

import org.olat.core.commons.modules.bc.meta.MetaInfo;
import org.olat.core.commons.modules.bc.meta.MetaInfoHelper;
import org.olat.core.commons.modules.bc.meta.tagged.MetaTagged;
import org.olat.core.id.Identity;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.vfs.LocalFileImpl;
import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.core.util.vfs.LocalImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.version.Versionable;
import org.olat.core.util.vfs.version.VersionsFileManager;

/**
 * Initial Date: 04.12.2002
 * 
 * @author Mike Stock Comment:
 */
public class ZipUtil {

	private static final String DIR_NAME__MACOSX = "__MACOSX/";

	private static final OLog log = Tracing.createLoggerFor(VersionsFileManager.class);

	/**
	 * Constructor for ZipUtil.
	 */
	public ZipUtil() {
		super();
	}

	/**
	 * Unzip a file to a directory
	 * 
	 * @param zipFile The zip file to unzip
	 * @param targetDir The directory to unzip the file to
	 * @return True if successfull, false otherwise
	 */
	public static boolean unzip(File zipFile, File targetDir) {

		try {
			long s = System.currentTimeMillis();
			xxunzip(new FileInputStream(zipFile), targetDir.getAbsolutePath());
			log.info("unzip file=" + zipFile.getName() + " to=" + targetDir.getAbsolutePath() + " t=" + Long.toString(System.currentTimeMillis() - s));
			return true;
		} catch (IOException e) {
			log.error("I/O failure while unzipping " + zipFile.getAbsolutePath() + " to " + targetDir.getAbsolutePath());
			return false;
		}
	}

	/**
	 * Unzip a VFSLeaf (zip zip archive file) to a directory
	 * 
	 * @param zipLeaf zip archive file to unzip
	 * @param targetDir The directory to unzip the file to
	 * @return True if successfull, false otherwise
	 */
	public static boolean unzip(VFSLeaf zipLeaf, VFSContainer targetDir) {

		if (targetDir instanceof LocalFolderImpl) {
			String outdir = ((LocalFolderImpl) targetDir).getBasefile().getAbsolutePath();
			try {
				long s = System.currentTimeMillis();
				xxunzip(zipLeaf.getInputStream(), outdir);
				log.info("unzip file=" + zipLeaf.getName() + " to=" + outdir + " t=" + Long.toString(System.currentTimeMillis() - s));
				return true;
			} catch (IOException e) {
				log.error("I/O failure while unzipping " + zipLeaf.getName() + " to " + outdir);
				return false;
			}
		}

		return unzip(zipLeaf, targetDir, null, false);
	}

	/**
	 * Unzip a file to a directory using the versioning system of VFS
	 * 
	 * @param zipLeaf The file to unzip
	 * @param targetDir The directory to unzip the file to
	 * @param the identity of who unzip the file
	 * @param versioning enabled or not
	 * @return True if successfull, false otherwise
	 */
	public static boolean unzip(VFSLeaf zipLeaf, VFSContainer targetDir, Identity identity, boolean versioning) {
		InputStream in = zipLeaf.getInputStream();
		boolean unzipped = unzip(in, targetDir, identity, versioning);
		FileUtils.closeSafely(in);
		return unzipped;
	}

	/**
	 * Unzip an inputstream to a directory using the versioning system of VFS
	 * 
	 * @param zipLeaf The file to unzip
	 * @param targetDir The directory to unzip the file to
	 * @param the identity of who unzip the file
	 * @param versioning enabled or not
	 * @return True if successfull, false otherwise
	 */
	private static boolean unzip(InputStream in, VFSContainer targetDir, Identity identity, boolean versioning) {
		ZipInputStream oZip = new ZipInputStream(in);

		try {
			// unzip files
			ZipEntry oEntr = oZip.getNextEntry();
			while (oEntr != null) {
				if (oEntr.getName() != null && !oEntr.getName().startsWith(DIR_NAME__MACOSX)) {
					if (oEntr.isDirectory()) {
						// skip MacOSX specific metadata directory
						// create directories
						getAllSubdirs(targetDir, oEntr.getName(), identity, true);
					} else {
						// create file
						VFSContainer createIn = targetDir;
						String name = oEntr.getName();
						// check if entry has directories which did not show up as
						// directories above
						int dirSepIndex = name.lastIndexOf('/');
						if (dirSepIndex == -1) {
							// try it windows style, backslash is also valid format
							dirSepIndex = name.lastIndexOf('\\');
						}
						if (dirSepIndex > 0) {
							// create subdirs
							createIn = getAllSubdirs(targetDir, name.substring(0, dirSepIndex), identity, true);
							if (createIn == null) {
								if (log.isDebug()) log.debug("Error creating directory structure for zip entry: " + oEntr.getName());
								return false;
							}
							name = name.substring(dirSepIndex + 1);
						}

						if (versioning) {
							VFSLeaf newEntry = (VFSLeaf) createIn.resolve(name);
							if (newEntry == null) {
								newEntry = createIn.createChildLeaf(name);
								OutputStream out = newEntry.getOutputStream(false);
								if (!FileUtils.copy(oZip, out)) return false;
								FileUtils.closeSafely(out);
							} else if (newEntry instanceof Versionable) {
								Versionable versionable = (Versionable) newEntry;
								if (versionable.getVersions().isVersioned()) {
									versionable.getVersions().addVersion(identity, "", oZip);
								}
							}
							if (newEntry instanceof MetaTagged) {
								MetaInfo info = ((MetaTagged) newEntry).getMetaInfo();
								if (info != null) {
									info.setAuthor(identity.getName());
									info.write();
								}
							}

						} else {
							VFSLeaf newEntry = createIn.createChildLeaf(name);
							if (newEntry != null) {
								OutputStream out = newEntry.getOutputStream(false);
								if (!FileUtils.copy(oZip, out)) return false;
								FileUtils.closeSafely(out);
							}
							if (newEntry instanceof MetaTagged) {
								MetaInfo info = ((MetaTagged) newEntry).getMetaInfo();
								if (info != null && identity != null) {
									info.setAuthor(identity.getName());
									info.write();
								}
							}
						}
					}
				}
				oZip.closeEntry();
				oEntr = oZip.getNextEntry();
			}
		} catch (IOException e) {
			return false;
		} finally {
			FileUtils.closeSafely(oZip);
		}
		return true;
	} // unzip

	/**
	 * Check if a file in the zip is already in the path
	 * 
	 * @param zipLeaf
	 * @param targetDir
	 * @param identity
	 * @param isAdmin
	 * @return the list of files which already exist
	 */
	public static List<String> checkLockedFileBeforeUnzip(VFSLeaf zipLeaf, VFSContainer targetDir, Identity identity, boolean isAdmin) {
		List<String> lockedFiles = new ArrayList<String>();

		InputStream in = zipLeaf.getInputStream();
		ZipInputStream oZip = new ZipInputStream(in);

		try {
			// unzip files
			ZipEntry oEntr = oZip.getNextEntry();
			while (oEntr != null) {
				if (oEntr.getName() != null && !oEntr.getName().startsWith(DIR_NAME__MACOSX)) {
					if (oEntr.isDirectory()) {
						// skip MacOSX specific metadata directory
						// directories aren't locked
						oZip.closeEntry();
						oEntr = oZip.getNextEntry();
						continue;
					} else {
						// search file
						VFSContainer createIn = targetDir;
						String name = oEntr.getName();
						// check if entry has directories which did not show up as
						// directories above
						int dirSepIndex = name.lastIndexOf('/');
						if (dirSepIndex == -1) {
							// try it windows style, backslash is also valid format
							dirSepIndex = name.lastIndexOf('\\');
						}
						if (dirSepIndex > 0) {
							// get subdirs
							createIn = getAllSubdirs(targetDir, name.substring(0, dirSepIndex), identity, false);
							if (createIn == null) {
								// sub directories don't exist, and aren't locked
								oZip.closeEntry();
								oEntr = oZip.getNextEntry();
								continue;
							}
							name = name.substring(dirSepIndex + 1);
						}

						VFSLeaf newEntry = (VFSLeaf) createIn.resolve(name);
						if (MetaInfoHelper.isLocked(newEntry, identity, isAdmin)) {
							lockedFiles.add(name);
						}
					}
				}
				oZip.closeEntry();
				oEntr = oZip.getNextEntry();
			}
		} catch (IOException e) {
			return null;
		} finally {
			FileUtils.closeSafely(oZip);
			FileUtils.closeSafely(in);
		}

		return lockedFiles;
	}

	/**
	 * Get the whole subpath.
	 * 
	 * @param create the missing directories
	 * @param base
	 * @param subDirPath
	 * @return Returns the last container of this subpath.
	 */
	private static VFSContainer getAllSubdirs(VFSContainer base, String subDirPath, Identity identity, boolean create) {
		StringTokenizer st;
		if (subDirPath.indexOf("/") != -1) {
			st = new StringTokenizer(subDirPath, "/", false);
		} else {
			// try it windows style, backslash is also valid format
			st = new StringTokenizer(subDirPath, "\\", false);
		}
		VFSContainer currentPath = base;
		while (st.hasMoreTokens()) {
			String nextSubpath = st.nextToken();
			VFSItem vfsSubpath = currentPath.resolve(nextSubpath);
			if (vfsSubpath == null && !create) { return null; }
			if (vfsSubpath == null || (vfsSubpath instanceof VFSLeaf)) {
				vfsSubpath = currentPath.createChildContainer(nextSubpath);
				if (vfsSubpath == null) return null;
				if (identity != null && vfsSubpath instanceof MetaTagged) {
					MetaInfo info = ((MetaTagged) vfsSubpath).getMetaInfo();
					if (info != null) {
						info.setAuthor(identity.getName());
						info.write();
					}
				}
			}
			currentPath = (VFSContainer) vfsSubpath;
		}
		return currentPath;
	}

	/**
	 * Add the set of files residing in root to the ZIP file named target. Files in subfolders will be compressed too.
	 * 
	 * @param files Filenames to add to ZIP, relative to root
	 * @param root Base path.
	 * @param target Target ZIP file.
	 * @param compress to compress ot just store
	 * @return true if successfull, false otherwise.
	 */
	public static boolean zip(Set<String> files, File root, File target, boolean compress) {
		// Create a buffer for reading the files
		if (target.exists()) return false;
		List<VFSItem> vfsFiles = new ArrayList<VFSItem>();
		LocalFolderImpl vfsRoot = new LocalFolderImpl(root);
		for (Iterator<String> iter = files.iterator(); iter.hasNext();) {
			String fileName = iter.next();
			VFSItem item = vfsRoot.resolve(fileName);
			if (item == null) return false;
			vfsFiles.add(item);
		}
		return zip(vfsFiles, new LocalFileImpl(target), compress);
	} // zip

	/**
	 * Add the set of files residing in root to the ZIP file named target. Files in subfolders will be compressed too.
	 * 
	 * @param files Filenames to add to ZIP, relative to root
	 * @param root Base path.
	 * @param target Target ZIP file.
	 * @param compress to compress ot just store
	 * @return true if successfull, false otherwise.
	 */
	public static boolean zip(Set<String> files, File root, File target) {
		return zip(files, root, target, true);
	} // zip

	public static boolean zip(List<VFSItem> vfsFiles, VFSLeaf target, boolean compress) {
		boolean success = true;

		String zname = target.getName();
		if (target instanceof LocalImpl) {
			zname = ((LocalImpl) target).getBasefile().getAbsolutePath();
		}

		OutputStream out = target.getOutputStream(false);

		if (out == null) { throw new OLATRuntimeException(ZipUtil.class, "Error getting output stream for file: " + zname, null); }

		long s = System.currentTimeMillis();

		java.util.zip.ZipOutputStream zipOut = new java.util.zip.ZipOutputStream(new java.io.BufferedOutputStream(out, FileUtils.BSIZE));

		if (vfsFiles.size() == 0) {
			try {
				zipOut.close();
			} catch (IOException e) {
				//
			}
			return true;
		}

		zipOut.setLevel(compress ? 9 : 0);
		for (Iterator<VFSItem> iter = vfsFiles.iterator(); success && iter.hasNext();) {
			success = addToZip(iter.next(), "", zipOut);
		}

		try {
			zipOut.flush();
			zipOut.close();
			log.info("zipped (" + (compress ? "compress" : "store") + ") " + zname + " t=" + Long.toString(System.currentTimeMillis() - s));
		} catch (IOException e) {
			throw new OLATRuntimeException(ZipUtil.class, "I/O error closing file: " + zname, null);
		}

		return success;
	}

	private static boolean addToZip(VFSItem vfsItem, String currentPath, java.util.zip.ZipOutputStream out) {

		boolean success = true;
		InputStream in = null;

		byte[] buffer = new byte[FileUtils.BSIZE];

		try {
			String itemName = currentPath.length() == 0 ? vfsItem.getName() : currentPath + File.separator + vfsItem.getName();

			if (vfsItem instanceof VFSContainer) {

				out.putNextEntry(new java.util.zip.ZipEntry(itemName + File.separator));
				out.closeEntry();

				List<VFSItem> items = ((VFSContainer) vfsItem).getItems();
				for (Iterator<VFSItem> iter = items.iterator(); iter.hasNext();) {
					if (!addToZip(iter.next(), itemName, out)) {
						success = false;
						break;
					}
				}

			} else {

				out.putNextEntry(new java.util.zip.ZipEntry(itemName));
				in = ((VFSLeaf) vfsItem).getInputStream();

				int c;
				while ((c = in.read(buffer, 0, buffer.length)) != -1) {
					out.write(buffer, 0, c);
				}

				out.closeEntry();
			}
		} catch (IOException ioe) {
			String name = vfsItem.getName();
			if (vfsItem instanceof LocalImpl) {
				name = ((LocalImpl) vfsItem).getBasefile().getAbsolutePath();
			}
			log.error("I/O error while adding " + name + " to zip:" + ioe);
			return false;
		} finally {
			FileUtils.closeSafely(in);
		}
		return success;
	}

	/**
	 * Zip all files under a certain root directory. (choose to compress or not param compress)
	 * 
	 * @param rootFile
	 * @param targetZipFile
	 * @param compress to compress or just store (if already compressed)
	 * @return true = success, false = exception/error
	 */
	public static boolean zipAll(File rootFile, File targetZipFile, boolean compress) {
		Set<String> fileSet = new HashSet<String>();
		String[] files = rootFile.list();
		for (int i = 0; i < files.length; i++) {
			fileSet.add(files[i]);
		}
		return zip(fileSet, rootFile, targetZipFile, compress);
	}

	/**
	 * Zip all files under a certain root directory. (with compression)
	 * 
	 * @param rootFile
	 * @param targetZipFile
	 * @return true = success, false = exception/error
	 */
	public static boolean zipAll(File rootFile, File targetZipFile) {
		return zipAll(rootFile, targetZipFile, true);
	}

	/**
	 * Unzip files from VFSLeaf into VFSContainer and do NOTHING ELSE!!! See OLAT-6213
	 * 
	 * @param src, VFSLeaf input data
	 * @param target, outout VFSContainer
	 */
	public static boolean xxunzip(VFSLeaf src, VFSContainer dst) {
		if (dst instanceof LocalImpl) {
			try {
				xxunzip(src.getInputStream(), ((LocalImpl) dst).getBasefile().getAbsolutePath());
				return true;
			} catch (IOException e) {
				String s = ((LocalImpl) src).getBasefile().getAbsolutePath();
				String d = ((LocalImpl) dst).getBasefile().getAbsolutePath();
				log.error("I/O error unzipping " + s + " to " + d);
				return false;
			}
		}
		return false;
	}

	/**
	 * Unzip files from stream into target dir and do NOTHING ELSE!!! See OLAT-6213
	 * 
	 * @param is, stream from zip archive
	 * @param outdir, path to output directory, relative to cwd or absolute
	 */
	private static void xxunzip(InputStream is, String outdir) throws IOException {

		byte[] buffer = new byte[FileUtils.BSIZE];

		java.util.zip.ZipInputStream zis = new java.util.zip.ZipInputStream(new BufferedInputStream(is));

		java.util.zip.ZipEntry entry;

		try {
			while ((entry = zis.getNextEntry()) != null) {

				File of = new File(outdir + File.separator + entry.getName());

				if (entry.isDirectory()) {
					of.mkdirs();
					continue;
				} else {
					File xx = new File(of.getParent());
					if (!xx.exists()) {
						Stack<String> todo = new Stack<String>();
						do {
							todo.push(xx.getAbsolutePath());
							xx = new File(xx.getParent());
						} while (!xx.exists());
						while (todo.size() > 0) {
							xx = new File(todo.pop());
							if (!xx.exists()) {
								xx.mkdirs();
							}
						}
					}
				}

				BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(of), buffer.length);

				FileUtils.cpio(new BufferedInputStream(zis), bos, "unzip:" + entry.getName());

				bos.flush();
				bos.close();
			}
		} catch (IllegalArgumentException e) {
			// problem with chars in entry name likely
		}
		zis.close();

	}

}
