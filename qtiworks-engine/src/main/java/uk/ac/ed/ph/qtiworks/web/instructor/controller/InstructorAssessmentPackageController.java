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
package uk.ac.ed.ph.qtiworks.web.instructor.controller;

import uk.ac.ed.ph.qtiworks.domain.PrivilegeException;
import uk.ac.ed.ph.qtiworks.domain.entities.AssessmentPackage;
import uk.ac.ed.ph.qtiworks.services.AssessmentPackageServices;
import uk.ac.ed.ph.qtiworks.services.domain.AssessmentPackageFileImportException;
import uk.ac.ed.ph.qtiworks.services.domain.AssessmentPackageFileImportException.APFIFailureReason;
import uk.ac.ed.ph.qtiworks.services.domain.EnumerableClientFailure;

import java.io.IOException;
import java.io.InputStream;

import javax.annotation.Resource;

import org.springframework.stereotype.Controller;
import org.springframework.validation.DirectFieldBindingResult;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import com.google.common.io.Closeables;

/**
 * FIXME: Document this type
 *
 * @author David McKain
 */
@Controller
public class InstructorAssessmentPackageController {

    @Resource
    private AssessmentPackageServices assessmentPackageServices;

    @RequestMapping(value="/uploadAssessment", method=RequestMethod.GET)
    public String showUploadAssessmentForm() {
        return "uploadAssessmentForm";
    }

    @RequestMapping(value="/uploadAssessment", method=RequestMethod.POST)
    public String handleValidatorForm(final @RequestParam("upload") MultipartFile uploadFile)
            throws IOException, PrivilegeException {
        /* Make sure something was submitted */
        final DirectFieldBindingResult errors = new DirectFieldBindingResult(uploadFile, "uploadFile");
        if (uploadFile!=null && uploadFile.getSize()==0L) {
            errors.reject("uploadAssessment.noFile");
            return "uploadAssessmentForm";
        }

        /* Attempt to import the package */
        final InputStream uploadStream = uploadFile.getInputStream();
        final String uploadContentType = uploadFile.getContentType();
        try {
            final AssessmentPackage assessmentPackage = assessmentPackageServices.importAssessmentPackage(uploadStream, uploadContentType);
        }
        catch (final AssessmentPackageFileImportException e) {
            final EnumerableClientFailure<APFIFailureReason> failure = e.getFailure();
            failure.registerErrors(errors, "uploadAssessment");
            return "uploadAssessmentForm";
        }
        finally {
            Closeables.closeQuietly(uploadStream);
        }
        /* FIXME! */
        return "uploadAssessmentForm";
    }

}
