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
package uk.ac.ed.ph.jqtiplus.node.block;

import uk.ac.ed.ph.jqtiplus.attribute.value.StringAttribute;
import uk.ac.ed.ph.jqtiplus.node.LoadingContext;
import uk.ac.ed.ph.jqtiplus.node.XmlNode;

import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * This block can contain any xml node.
 * It should contain only xhtml node, but there is no such check.
 * 
 * @author Jonathon Hare
 */
public class UnsupportedBlock extends ContainerBlock {

    private static final long serialVersionUID = 474940437634236118L;

    /** Class tag of this block. */
    private final String localName;
    
    private final String namespaceUri;

    public UnsupportedBlock(XmlNode parent, String localName, String namespaceUri) {
        super(parent);
        this.localName = localName;
        this.namespaceUri = namespaceUri;
    }

    @Override
    public String getClassTag() {
        return localName;
    }
    
    public String getNamespaceUri() {
        return namespaceUri;
    }

    @Override
    protected void loadAttributes(Element element, LoadingContext context) {
        getAttributes().clear();

        for (int i = 0; i < element.getAttributes().getLength(); i++) {
            final Node attribute = element.getAttributes().item(i);
            getAttributes().add(new StringAttribute(this, attribute.getLocalName(), attribute.getNamespaceURI(),
                    attribute.getNodeValue(), null, false, true));
        }
    }
}
