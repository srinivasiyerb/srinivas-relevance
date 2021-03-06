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
 * Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */
package de.bps.ims.qti.fileresource;

import java.io.File;

import org.olat.ims.qti.fileresource.TestFileResourceValidator;

import de.bps.onyx.plugin.OnyxModule;

/**
 * Description:<br>
 * TODO: patrickb Class Description for TestFileResourceValidator
 * <P>
 * Initial Date: 25.06.2010 <br>
 * 
 * @author patrickb
 */
public class TestFileResourceValidatorOnyx extends TestFileResourceValidator {

	/**
	 * @see org.olat.ims.qti.fileresource.QTIFileResourceValidator#validate(java.io.File)
	 */
	@Override
	public boolean validate(final File unzippedDir) {
		if (OnyxModule.isOnyxTest(unzippedDir)) {
			return true;
		} else {
			return super.validate(unzippedDir);
		}
	}
}
