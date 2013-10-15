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

package org.olat.core.commons.services.thumbnail.impl;

import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import org.olat.core.commons.services.thumbnail.FinalSize;
import org.olat.core.commons.services.thumbnail.ThumbnailSPI;
import org.olat.core.util.ImageHelper;
import org.olat.core.util.ImageHelper.Size;
import org.olat.core.util.vfs.VFSLeaf;

/**
 * Description:<br>
 * Generate a thumbnail from an image based on ImageIO
 * <P>
 * Initial Date: 30 mar. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com
 */
public class ImageToThumbnail implements ThumbnailSPI {

	private final List<String> extensions = new ArrayList<String>();

	public ImageToThumbnail() {
		for (String imageIOSuffix : ImageIO.getWriterFileSuffixes()) {
			extensions.add(imageIOSuffix);
		}
	}

	@Override
	public List<String> getExtensions() {
		return extensions;
	}

	@Override
	public FinalSize generateThumbnail(VFSLeaf file, VFSLeaf thumbnailFile, int maxWidth, int maxHeight) {
		Size finalSize = ImageHelper.scaleImage(file, thumbnailFile, maxWidth, maxHeight);
		if (finalSize != null) { return new FinalSize(finalSize.getWidth(), finalSize.getHeight()); }
		// a problem happens
		return null;
	}
}