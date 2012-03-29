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
package uk.ac.ed.ph.jqtiplus.node.item.response.processing;

import uk.ac.ed.ph.jqtiplus.exception.QtiEvaluationException;
import uk.ac.ed.ph.jqtiplus.exception2.RuntimeValidationException;
import uk.ac.ed.ph.jqtiplus.node.XmlNode;
import uk.ac.ed.ph.jqtiplus.node.outcome.declaration.LookupTable;
import uk.ac.ed.ph.jqtiplus.node.outcome.declaration.OutcomeDeclaration;
import uk.ac.ed.ph.jqtiplus.running.ItemProcessingContext;
import uk.ac.ed.ph.jqtiplus.validation.ValidationContext;
import uk.ac.ed.ph.jqtiplus.validation.ValidationError;
import uk.ac.ed.ph.jqtiplus.validation.ValidationWarning;
import uk.ac.ed.ph.jqtiplus.value.BaseType;
import uk.ac.ed.ph.jqtiplus.value.Cardinality;
import uk.ac.ed.ph.jqtiplus.value.Value;

/**
 * The setOutcomeValue rule sets the value of an outcome variable to the value obtained from the associated expression.
 * An outcome variable can be updated with reference to A previously assigned value, in other words, the outcome variable
 * being set may appear in the expression where it takes the value previously assigned to it.
 * <p>
 * Special care is required when using the numeric base-types because floating point values can not be assigned to integer variables and vice-versa. The
 * truncate, round, or integerToFloat operators must be used to achieve numeric type conversion.
 * 
 * @author Jonathon Hare
 */
public class SetOutcomeValue extends ProcessResponseValue {

    private static final long serialVersionUID = 2510329183906024639L;

    /** Name of this class in xml schema. */
    public static final String QTI_CLASS_NAME = "setOutcomeValue";

    public SetOutcomeValue(XmlNode parent) {
        super(parent, QTI_CLASS_NAME);
    }

    @Override
    public Cardinality[] getRequiredCardinalities(ValidationContext context, int index) {
        if (getIdentifier() != null) {
            final OutcomeDeclaration declaration = context.getSubject().getOutcomeDeclaration(getIdentifier());
            if (declaration != null && declaration.getCardinality() != null) {
                return new Cardinality[] { declaration.getCardinality() };
            }
        }

        return Cardinality.values();
    }

    @Override
    public BaseType[] getRequiredBaseTypes(ValidationContext context, int index) {
        if (getIdentifier() != null) {
            final OutcomeDeclaration declaration = context.getSubject().getOutcomeDeclaration(getIdentifier());
            if (declaration != null && declaration.getBaseType() != null) {
                return new BaseType[] { declaration.getBaseType() };
            }
        }

        return BaseType.values();
    }

    @Override
    public void validate(ValidationContext context) {
        super.validate(context);

        if (getIdentifier() != null) {
            if (context.getSubject().getOutcomeDeclaration(getIdentifier()) == null) {
                context.add(new ValidationError(this, "Cannot find " + OutcomeDeclaration.QTI_CLASS_NAME + ": " + getIdentifier()));
            }

            final OutcomeDeclaration declaration = context.getSubject().getOutcomeDeclaration(getIdentifier());
            if (declaration != null && declaration.getLookupTable() != null) {
                context.add(new ValidationWarning(this, "Never used " + LookupTable.DISPLAY_NAME
                        + " in "
                        + OutcomeDeclaration.QTI_CLASS_NAME
                        + ": "
                        + getIdentifier()));
            }
        }
    }

    @Override
    public void evaluate(ItemProcessingContext context) throws RuntimeValidationException {
        final Value value = getExpression().evaluate(context);

        final OutcomeDeclaration declaration = context.getSubject().getOutcomeDeclaration(getIdentifier());
        if (declaration == null) {
            throw new QtiEvaluationException("Cannot find " + OutcomeDeclaration.QTI_CLASS_NAME + ": " + getIdentifier());
        }
        context.getItemSessionState().setOutcomeValue(declaration, value);
    }
}
