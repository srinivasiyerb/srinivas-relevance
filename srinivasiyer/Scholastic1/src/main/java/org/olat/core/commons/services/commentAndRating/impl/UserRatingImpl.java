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
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */
package org.olat.core.commons.services.commentAndRating.impl;

import org.olat.core.commons.persistence.PersistentObject;
import org.olat.core.commons.services.commentAndRating.model.UserRating;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;

/**
 * Description:<br>
 * Implemenation of the user rating class
 * <P>
 * Initial Date: 23.11.2009 <br>
 * 
 * @author gnaegi
 */
public class UserRatingImpl extends PersistentObject implements UserRating {
	private String resName;
	private Long resId;
	private String resSubPath;

	private Identity creator;
	private Integer rating;

	/**
	 * Default constructor for hibernate, don't use this!
	 */
	private UserRatingImpl() {
		//
	}

	/**
	 * Package constructor to create a new user rating with the given arguments
	 * 
	 * @param ores
	 * @param subpath
	 * @param creator
	 * @param ratingValue
	 */
	UserRatingImpl(OLATResourceable ores, String subpath, Identity creator, Integer ratingValue) {
		this.creator = creator;
		this.resName = ores.getResourceableTypeName();
		this.resId = ores.getResourceableId();
		this.resSubPath = subpath;
		this.rating = ratingValue;
	}

	/**
	 * @see org.olat.core.commons.services.commentAndRating.model.UserComment#getCreator()
	 */
	@Override
	public Identity getCreator() {
		return creator;
	}

	/**
	 * @see org.olat.core.commons.services.commentAndRating.model.UserComment#getResId()
	 */
	@Override
	public Long getResId() {
		return this.resId;
	}

	/**
	 * @see org.olat.core.commons.services.commentAndRating.model.UserComment#getResName()
	 */
	@Override
	public String getResName() {
		return this.resName;
	}

	/**
	 * @see org.olat.core.commons.services.commentAndRating.model.UserComment#getResSubPath()
	 */
	@Override
	public String getResSubPath() {
		return this.resSubPath;
	}

	/**
	 * @see org.olat.core.commons.services.commentAndRating.model.UserRating#getRating()
	 */
	@Override
	public Integer getRating() {
		return this.rating;
	}

	/**
	 * Set the resource subpath
	 * 
	 * @param resSubPath
	 */
	public void setResSubPath(String resSubPath) {
		this.resSubPath = resSubPath;
	}

	/**
	 * Set the resoruce name
	 * 
	 * @param resName
	 */
	public void setResName(String resName) {
		this.resName = resName;
	}

	/**
	 * Set the resource ID
	 * 
	 * @param resId
	 */
	public void setResId(Long resId) {
		this.resId = resId;
	}

	/**
	 * @see org.olat.core.commons.services.commentAndRating.model.UserComment#setCreator(org.olat.core.id.Identity)
	 */
	@Override
	public void setCreator(Identity creator) {
		this.creator = creator;
	}

	/**
	 * @see org.olat.core.commons.services.commentAndRating.model.UserRating#setRating(int)
	 */
	@Override
	public void setRating(Integer ratingValue) {
		this.rating = ratingValue;
	}

}
