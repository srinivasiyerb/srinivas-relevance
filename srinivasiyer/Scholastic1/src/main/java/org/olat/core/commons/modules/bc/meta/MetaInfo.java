package org.olat.core.commons.modules.bc.meta;

import java.util.Date;

import org.olat.core.id.Identity;
import org.olat.core.util.vfs.OlatRelPathImpl;
import org.olat.core.util.vfs.VFSLeaf;

public interface MetaInfo {

	/**
	 * Factory method to create a MetaInfo Bean.
	 * 
	 * @param olatRelPathImpl
	 * @return
	 */
	public MetaInfo createMetaInfoFor(OlatRelPathImpl olatRelPathImpl);

	/**
	 * Rename the given meta info file
	 * 
	 * @param meta
	 * @param newName
	 */
	public void rename(String newName);

	/**
	 * Move/Copy the given meta info to the target directory.
	 * 
	 * @param targetDir
	 * @param move
	 */
	public void moveCopyToDir(OlatRelPathImpl target, boolean move);

	/**
	 * Delete all associated meta info including sub files/directories
	 * 
	 * @param meta
	 */
	public void deleteAll();

	/**
	 * Copy values from froMeta into this object except name.
	 * 
	 * @param fromMeta
	 */
	public void copyValues(MetaInfo fromMeta);

	/**
	 * Delete this meta info
	 * 
	 * @return True upon success.
	 */
	public boolean delete();

	/**
	 * @return name of the initial author (OLAT user name)
	 */
	public String getAuthor();

	/**
	 * @return The display name of the user formatted as HTML
	 */
	public String getHTMLFormattedAuthor();

	/**
	 * Returns the identity of the initial author (the same identity is used in {@link MetaInfo#getAuthor()}.
	 * 
	 * @return The identity of the initial author.
	 */
	public Identity getAuthorIdentity();

	/**
	 * Corresponds to DublinCore:description
	 * 
	 * @return comment
	 */
	public String getComment();

	public String getName();

	/**
	 * DublinCore compatible
	 */
	public String getTitle();

	/**
	 * DublinCore compatible
	 */
	public String getPublisher();

	/**
	 * In this context, the creator is the person or organization that is primarily responsible for making the content. The author, by contrast, is an OLAT user who
	 * uploaded the file.
	 * 
	 * @return The writer of the resource
	 */
	public String getCreator();

	/**
	 * DublinCore compatible
	 */
	public String getSource();

	/**
	 * @return The city or location of publication
	 */
	public String getCity();

	public String getPages();

	/**
	 * DublinCore compatible
	 */
	public String getLanguage();

	public String getUrl();

	/**
	 * Corresponds to DublinCore:date + refinement
	 * 
	 * @return The date in form of a {year, month} array.
	 */
	public String[] getPublicationDate();

	/**
	 * @return True if this is a directory
	 */
	public boolean isDirectory();

	/**
	 * @return Last modified timestamp
	 */
	public long getLastModified();

	/**
	 * @return size of file
	 */
	public long getSize();

	/**
	 * @return formatted representation of size of file
	 */
	public String getFormattedSize();

	/**
	 * @return if the file is locked
	 */
	public boolean isLocked();

	public void setLocked(boolean locked);

	/**
	 * @return Identity of the user who locked the file
	 */
	public Identity getLockedByIdentity();

	/**
	 * @return key of the user's identity who locked the file
	 */
	public Long getLockedBy();

	public void setLockedBy(Long lockedById);

	/**
	 * @return date of the lock
	 */
	public Date getLockedDate();

	public void setLockedDate(Date lockedDate);

	/**
	 * @param string
	 */
	public void setAuthor(String username);

	/**
	 * @param string
	 */
	public void setComment(String string);

	public void setTitle(String title);

	public void setPublisher(String publisher);

	public void setCreator(String creator);

	public void setSource(String source);

	public void setCity(String city);

	public void setPages(String pages);

	public void setLanguage(String language);

	public void setUrl(String url);

	public void setPublicationDate(String month, String year);

	public boolean isThumbnailAvailable();

	public VFSLeaf getThumbnail(int maxWidth, int maxHeight);

	public void clearThumbnails();

	/**
	 * Writes the meta data to file. If no changes have been made, does not write anything.
	 * 
	 * @return True upon success.
	 */
	public boolean write();

	/**
	 * Increases the download count by one.
	 */
	public void increaseDownloadCount();

	/**
	 * @return The download count
	 */
	public int getDownloadCount();

	/**
	 * @return An icon css class that represents this type of file
	 */
	public String getIconCssClass();
}