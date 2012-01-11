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
package uk.ac.ed.ph.jqtiplus.xmlutils;


import java.io.InputStream;
import java.net.URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ResourceLocator} that looks for HTTP resources
 * in the ClassPath using a simple naming mechanism as follows:
 * <p>
 * A resource with URL <tt>http://server/path</tt> is mapped to a resource <tt>[basePath]/server/path</tt>, which is then looked up within the ClassPath.
 * <p>
 * This can be used to load "provided" or bundled resources, such as schemas, DTDs, standard resource processing templates etc.
 * 
 * @author David McKain
 */
public final class ClassPathHttpResourceLocator implements ResourceLocator {

    private static final long serialVersionUID = 6287482860619405237L;

    private static final Logger logger = LoggerFactory.getLogger(ClassPathHttpResourceLocator.class);

    /** basePath to search in. null is treated as blank */
    private String basePath;

    public ClassPathHttpResourceLocator() {
        this(null);
    }

    public ClassPathHttpResourceLocator(String basePath) {
        this.basePath = basePath;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    // -------------------------------------------

    @Override
    public InputStream findResource(final URI systemIdUri) {
        final String scheme = systemIdUri.getScheme();
        if ("http".equals(scheme)) {
            final String relativeSystemId = systemIdUri.toString().substring("http://".length());
            final String resultingPath = basePath != null ? basePath + "/" + relativeSystemId : relativeSystemId;
            return loadResource(systemIdUri, resultingPath);
        }
        return null;
    }

    private InputStream loadResource(final URI systemIdUri, final String resourcePath) {
        final InputStream resourceStream = getClass().getClassLoader().getResourceAsStream(resourcePath);
        if (resourceStream != null) {
            logger.debug("Successful locate of HTTP resource with URI {}  in ClassPath at {}", systemIdUri, resourcePath);
        }
        else {
            logger.warn("Failed to locate HTTP resource with URI {} in ClassPath at {}", systemIdUri, resourcePath);
        }
        return resourceStream;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "@" + hashCode()
                + "(basePath=" + basePath
                + ")";
    }
}