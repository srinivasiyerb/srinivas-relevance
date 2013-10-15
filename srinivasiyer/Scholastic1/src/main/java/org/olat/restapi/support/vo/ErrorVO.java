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

package org.olat.restapi.support.vo;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Description:<br>
 * <P>
 * Initial Date: 19 apr. 2010 <br>
 * 
 * @author srosse, stephane.rosse@frentix.com
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "errorVO")
public class ErrorVO {

	private String code;
	private String translation;

	public ErrorVO() {
		// make jaxb happy
	}

	public ErrorVO(final String pack, final String code, final String translation) {
		this.code = pack + ":" + code;
		this.translation = translation;
	}

	public String getCode() {
		return code;
	}

	public void setCode(final String code) {
		this.code = code;
	}

	public String getTranslation() {
		return translation;
	}

	public void setTranslation(final String translation) {
		this.translation = translation;
	}

	@Override
	public String toString() {
		return "errorVo[code=" + code + "]";
	}
}