/* Copyright (c) 2012, University of Edinburgh.
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice, this
 *   list of conditions and the following disclaimer in the documentation and/or
 *   other materials provided with the distribution.
 *
 * * Neither the name of the University of Edinburgh nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *
 * This software is derived from (and contains code from) QTItools and MathAssessEngine.
 * QTItools is (c) 2008, University of Southampton.
 * MathAssessEngine is (c) 2010, University of Edinburgh.
 */
package uk.ac.ed.ph.jqtiplus.attribute.value;


import uk.ac.ed.ph.jqtiplus.attribute.SingleAttribute;
import uk.ac.ed.ph.jqtiplus.node.XmlNode;
import uk.ac.ed.ph.jqtiplus.validation.AttributeValidationError;
import uk.ac.ed.ph.jqtiplus.validation.ValidationContext;
import uk.ac.ed.ph.jqtiplus.value.BaseType;
import uk.ac.ed.ph.jqtiplus.value.SingleValue;

/**
 * Attribute with single value.
 * 
 * @author Jiri Kajaba
 */
public class SingleValueAttribute extends SingleAttribute<SingleValue> {

    private static final long serialVersionUID = 3061500904873811836L;

    /** BaseType of attribute. */
    private BaseType baseType;

    /**
     * Constructs attribute.
     * 
     * @param parent attribute's parent
     * @param localName attribute's localName
     * @param baseType attribute's baseType
     */
    public SingleValueAttribute(XmlNode parent, String localName, BaseType baseType) {
        this(parent, localName, baseType, null, null, true);
    }

    /**
     * Constructs attribute.
     * 
     * @param parent attribute's parent
     * @param localName attribute's localName
     * @param baseType attribute's baseType
     * @param defaultValue attribute's default value
     */
    public SingleValueAttribute(XmlNode parent, String localName, BaseType baseType, SingleValue defaultValue) {
        this(parent, localName, baseType, null, defaultValue, false);
    }

    /**
     * Constructs attribute.
     * 
     * @param parent attribute's parent
     * @param localName attribute's localName
     * @param baseType attribute's baseType
     * @param value attribute's value
     * @param defaultValue attribute's default value
     * @param required is this attribute required
     */
    public SingleValueAttribute(
            XmlNode parent, String localName, BaseType baseType, SingleValue value, SingleValue defaultValue, boolean required) {
        super(parent, localName, value, defaultValue, required);

        this.baseType = baseType;
    }

    /**
     * Gets baseType of attribute.
     * 
     * @return baseType of attribute
     * @see #setBaseType
     */
    public BaseType getBaseType() {
        return baseType;
    }

    /**
     * Sets new baseType of attribute.
     * 
     * @param baseType new baseType of attribute
     * @see #getBaseType
     */
    public void setBaseType(BaseType baseType) {
        this.baseType = baseType;
    }

    @Override
    protected SingleValue parseValue(String value) {
        return baseType.parseSingleValue(value);
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);

        if (getValue() != null && getValue().getBaseType() != baseType) {
            context.add(new AttributeValidationError(this, "BaseType of " + getLocalName()
                    + " attribute does not match. Expected:  "
                    + baseType
                    + ", but found: "
                    + getValue().getBaseType()));
        }
    }
}
