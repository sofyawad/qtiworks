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

import uk.ac.ed.ph.qtiworks.QtiWorksLogicException;
import uk.ac.ed.ph.qtiworks.QtiWorksRuntimeException;
import uk.ac.ed.ph.qtiworks.domain.DomainEntityNotFoundException;
import uk.ac.ed.ph.qtiworks.domain.PrivilegeException;
import uk.ac.ed.ph.qtiworks.domain.entities.Assessment;
import uk.ac.ed.ph.qtiworks.domain.entities.AssessmentPackage;
import uk.ac.ed.ph.qtiworks.domain.entities.CandidateItemSession;
import uk.ac.ed.ph.qtiworks.domain.entities.ItemDelivery;
import uk.ac.ed.ph.qtiworks.domain.entities.ItemDeliverySettings;
import uk.ac.ed.ph.qtiworks.services.AssessmentCandidateService;
import uk.ac.ed.ph.qtiworks.services.AssessmentManagementService;
import uk.ac.ed.ph.qtiworks.services.EntityGraphService;
import uk.ac.ed.ph.qtiworks.services.domain.AssessmentPackageFileImportException;
import uk.ac.ed.ph.qtiworks.services.domain.AssessmentPackageFileImportException.APFIFailureReason;
import uk.ac.ed.ph.qtiworks.services.domain.AssessmentStateException;
import uk.ac.ed.ph.qtiworks.services.domain.AssessmentStateException.APSFailureReason;
import uk.ac.ed.ph.qtiworks.services.domain.EnumerableClientFailure;
import uk.ac.ed.ph.qtiworks.services.domain.UpdateAssessmentCommand;
import uk.ac.ed.ph.qtiworks.web.domain.UploadAssessmentPackageCommand;

import uk.ac.ed.ph.jqtiplus.exception2.RuntimeValidationException;
import uk.ac.ed.ph.jqtiplus.validation.AssessmentObjectValidationResult;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Resource;
import javax.validation.Valid;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller providing management functions for {@link Assessment}s
 *
 * @author David McKain
 */
@Controller
public final class InstructorAssessmentManagementController {

    private static final String controllerPath = "/web/instructor";

    @Resource
    private String contextPath;

    @Resource
    private EntityGraphService entityGraphService;

    @Resource
    private AssessmentManagementService assessmentManagementService;

    @Resource
    private AssessmentCandidateService assessmentCandidateService;

    //------------------------------------------------------

    /** Lists all Assignments owned by the caller */
    @RequestMapping(value="/assessments", method=RequestMethod.GET)
    public String listCallerAssessments(final Model model) {
        final List<Assessment> assessments = entityGraphService.getCallerAssessments();
        model.addAttribute(assessments);
        model.addAttribute("assessmentRouting", buildAssessmentListRouting(assessments));
        return "listAssessments";
    }

    public Map<Long, Map<String, String>> buildAssessmentListRouting(final List<Assessment> assessments) {
        final Map<Long, Map<String, String>> result = new HashMap<Long, Map<String, String>>();
        for (final Assessment assessment : assessments) {
            result.put(assessment.getId(), buildAssessmentRouting(assessment));
        }
        return result;
    }

    public Map<String, String> buildAssessmentRouting(final Assessment assessment) {
        return buildAssessmentRouting(assessment.getId().longValue());
    }

    public Map<String, String> buildAssessmentRouting(final long aid) {
        final Map<String, String> result = new HashMap<String, String>();
        result.put("show", buildActionPath("/assessment/" + aid));
        result.put("edit", buildActionPath("/assessment/" + aid + "/edit"));
        result.put("upload", buildActionPath("/assessment/" + aid + "/upload"));
        result.put("validate", buildActionPath("/assessment/" + aid + "/validate"));
        result.put("try", buildActionPath("/assessment/" + aid + "/try"));
        return result;
    }

    private String buildActionPath(final String requestPath) {
        return contextPath + controllerPath + requestPath;
    }

    private String buildActionRedirect(final String requestPath) {
        return "redirect:" + controllerPath + requestPath;
    }

    @ModelAttribute
    public void setupPrimaryRouting(final Model model) {
        final Map<String, String> primaryRouting = new HashMap<String, String>();
        primaryRouting.put("uploadAssessment", buildActionPath("/assessments/upload"));
        primaryRouting.put("listAssessments", buildActionPath("/assessments"));
        primaryRouting.put("listItemDeliverySettings", buildActionPath("/deliverysettings"));
        primaryRouting.put("createItemDeliverySettings", buildActionPath("/deliverysettings/create"));
        model.addAttribute("instructorAssessmentRouting", primaryRouting);
    }

    private void setupModelForAssessment(final long aid, final Model model)
            throws PrivilegeException, DomainEntityNotFoundException {
        setupModelForAssessment(assessmentManagementService.lookupAssessment(aid), model);
    }

    private void setupModelForAssessment(final Assessment assessment, final Model model) {
        model.addAttribute("assessment", assessment);
        model.addAttribute("assessmentRouting", buildAssessmentRouting(assessment));
    }

    //------------------------------------------------------

    @RequestMapping(value="/assessments/upload", method=RequestMethod.GET)
    public String showUploadAssessmentForm(final Model model) {
        model.addAttribute(new UploadAssessmentPackageCommand());
        return "uploadAssessmentForm";
    }

    /**
     * TODO: I'm doing upload + validation together. It would make sense later to split
     * these into 2 steps and find some way of showing progress.
     */
    @RequestMapping(value="/assessments/upload", method=RequestMethod.POST)
    public String handleUploadAssessmentForm(final @Valid @ModelAttribute UploadAssessmentPackageCommand command, final BindingResult result)
            throws PrivilegeException {
        /* Validate command Object */
        if (result.hasErrors()) {
            return "uploadAssessmentForm";
        }

        /* Attempt to import the package */
        Assessment assessment;
        try {
            assessment = assessmentManagementService.importAssessment(command.getFile());
        }
        catch (final AssessmentPackageFileImportException e) {
            final EnumerableClientFailure<APFIFailureReason> failure = e.getFailure();
            failure.registerErrors(result, "uploadAssessmentPackageCommand");
            return "uploadAssessmentForm";
        }
        try {
            assessmentManagementService.validateAssessment(assessment.getId().longValue());
        }
        catch (final DomainEntityNotFoundException e) {
            /* This could only happen if there's some kind of race condition */
            throw QtiWorksRuntimeException.unexpectedException(e);
        }
        return buildActionRedirect("/assessment/" + assessment.getId());
    }

    /**
     * Shows the Assessment having the given ID (aid)
     */
    @RequestMapping(value="/assessment/{aid}", method=RequestMethod.GET)
    public String showAssessment(final Model model, @PathVariable final long aid)
            throws PrivilegeException, DomainEntityNotFoundException {
        final Assessment assessment = assessmentManagementService.lookupAssessment(aid);
        final AssessmentPackage assessmentPackage = entityGraphService.getCurrentAssessmentPackage(assessment);
        final List<ItemDeliverySettings> itemDeliverySettingsList = entityGraphService.getCallerItemDeliverySettings();

        model.addAttribute(assessment);
        model.addAttribute(assessmentPackage);
        model.addAttribute(itemDeliverySettingsList);
        model.addAttribute("assessmentRouting", buildAssessmentRouting(aid));
        return "showAssessment";
    }

    //------------------------------------------------------

    @RequestMapping(value="/assessment/{aid}/edit", method=RequestMethod.GET)
    public String showEditAssessmentForm(final Model model, @PathVariable final long aid)
            throws PrivilegeException, DomainEntityNotFoundException {
        final Assessment assessment = assessmentManagementService.lookupAssessment(aid);

        final UpdateAssessmentCommand command = new UpdateAssessmentCommand();
        command.setName(assessment.getName());
        command.setTitle(assessment.getTitle());
        command.setPublic(assessment.isPublic());
        model.addAttribute(command);

        setupModelForAssessment(assessment, model);
        return "editAssessmentForm";
    }

    @RequestMapping(value="/assessment/{aid}/edit", method=RequestMethod.POST)
    public String handleEditAssessmentForm(@PathVariable final long aid, final Model model,
            final @Valid @ModelAttribute UpdateAssessmentCommand command, final BindingResult result)
            throws PrivilegeException, DomainEntityNotFoundException {
        /* Validate command Object */
        if (result.hasErrors()) {
            setupModelForAssessment(aid, model);
            return "editAssessmentForm";
        }
        try {
            assessmentManagementService.updateAssessment(aid, command);
        }
        catch (final BindException e) {
            throw new QtiWorksLogicException("Top layer validation is currently same as service layer in this case, so this Exception should not happen");
        }
        return buildActionRedirect("/assessment/" + aid);
    }

    //------------------------------------------------------

    @RequestMapping(value="/assessment/{aid}/upload", method=RequestMethod.GET)
    public String showUpdateAssessmentPackageForm(final @PathVariable long aid,
            final Model model)
            throws PrivilegeException, DomainEntityNotFoundException {
        model.addAttribute(new UploadAssessmentPackageCommand());
        setupModelForAssessment(aid, model);
        return "updateAssessmentPackageForm";
    }

    /**
     * TODO: I'm doing upload + validation together. It would make sense later to split
     * these into 2 steps and find some way of showing progress.
     */
    @RequestMapping(value="/assessment/{aid}/upload", method=RequestMethod.POST)
    public String handleUploadAssessmentPackageForm(final @PathVariable long aid, final Model model,
            final @Valid @ModelAttribute UploadAssessmentPackageCommand command, final BindingResult result)
            throws PrivilegeException, DomainEntityNotFoundException {
        /* Make sure something was submitted */
        /* Validate command Object */
        if (result.hasErrors()) {
            setupModelForAssessment(aid, model);
            return "updateAssessmentPackageForm";
        }

        /* Attempt to import the package */
        final MultipartFile uploadFile = command.getFile();
        try {
            assessmentManagementService.updateAssessmentPackageFiles(aid, uploadFile);
        }
        catch (final AssessmentPackageFileImportException e) {
            final EnumerableClientFailure<APFIFailureReason> failure = e.getFailure();
            failure.registerErrors(result, "assessmentPackageUpload");
            setupModelForAssessment(aid, model);
            return "updateAssessmentPackageForm";
        }
        catch (final AssessmentStateException e) {
            final EnumerableClientFailure<APSFailureReason> failure = e.getFailure();
            failure.registerErrors(result, "assessmentPackageUpload");
            setupModelForAssessment(aid, model);
            return "updateAssessmentPackageForm";
        }
        try {
            assessmentManagementService.validateAssessment(aid);
        }
        catch (final DomainEntityNotFoundException e) {
            /* This could only happen if there's some kind of race condition */
            throw QtiWorksRuntimeException.unexpectedException(e);
        }
        return buildActionRedirect("/assessment/{aid}");
    }

    //------------------------------------------------------

    /** TODO: For performance, we should cache the validation result */
    @RequestMapping(value="/assessment/{aid}/validate", method=RequestMethod.GET)
    public String validateAssessment(final @PathVariable long aid, final Model model)
            throws PrivilegeException, DomainEntityNotFoundException {
        final AssessmentObjectValidationResult<?> validationResult = assessmentManagementService.validateAssessment(aid);
        model.addAttribute("validationResult", validationResult);
        setupModelForAssessment(aid, model);
        return "validationResult";
    }

    //------------------------------------------------------

    @RequestMapping(value="/assessment/{aid}/try", method=RequestMethod.POST)
    public String tryLatestAssessmentPackage(final @PathVariable long aid)
            throws PrivilegeException, DomainEntityNotFoundException, RuntimeValidationException {
        final Assessment assessment = assessmentManagementService.lookupAssessment(aid);
        final ItemDelivery demoDelivery = assessmentManagementService.createDemoDelivery(assessment);
        final CandidateItemSession candidateSession = assessmentCandidateService.createCandidateSession(demoDelivery.getId().longValue());
        return "redirect:/web/instructor/session/" + candidateSession.getId().longValue();
    }

    @RequestMapping(value="/assessment/{aid}/try/{dsid}", method=RequestMethod.POST)
    public String tryLatestAssessmentPackage(final @PathVariable long aid, final @PathVariable long dsid)
            throws PrivilegeException, DomainEntityNotFoundException, RuntimeValidationException {
        final Assessment assessment = assessmentManagementService.lookupAssessment(aid);
        final ItemDeliverySettings itemDeliverySettings = assessmentManagementService.lookupItemDeliverySettings(dsid);
        final ItemDelivery demoDelivery = assessmentManagementService.createDemoDelivery(assessment, itemDeliverySettings);
        final CandidateItemSession candidateSession = assessmentCandidateService.createCandidateSession(demoDelivery.getId().longValue());
        return "redirect:/web/instructor/session/" + candidateSession.getId().longValue();
    }

    //------------------------------------------------------
    // Management of ItemDeliverySettings

    @RequestMapping(value="/deliverysettings", method=RequestMethod.GET)
    public String listItemDeliverySettings(final Model model) {
        final List<ItemDeliverySettings> itemDeliverySettingsList = entityGraphService.getCallerItemDeliverySettings();
        model.addAttribute(itemDeliverySettingsList);
        model.addAttribute("itemDeliverySettingsRouting", buildDeliverySettingsListRouting(itemDeliverySettingsList));
        return "listDeliverySettings";
    }

    @RequestMapping(value="/deliverysettings/create", method=RequestMethod.GET)
    public String showCreateItemDeliverySettingsForm(final Model model) {
        final long existingOptionCount = entityGraphService.countCallerItemDeliverySettings();
        final ItemDeliverySettings template = assessmentManagementService.createItemDeliverySettingsTemplate();
        template.setTitle("Item Delivery Settings #" + (existingOptionCount+1));

        model.addAttribute(template);
        return "createItemDeliverySettingsForm";
    }

    @RequestMapping(value="/deliverysettings/create", method=RequestMethod.POST)
    public String handleCreateItemDeliverySettingsForm(final @Valid @ModelAttribute ItemDeliverySettings command, final BindingResult result) {
        /* Validate command Object */
        if (result.hasErrors()) {
            System.out.println(result);
            return "createItemDeliverySettingsForm";
        }

        /* Try to create new entity */
        assessmentManagementService.createItemDeliverySettings(command);

        /* Go back to list */
        return buildActionRedirect("/deliverysettings");
    }

    @RequestMapping(value="/deliverysettings/{dsid}", method=RequestMethod.GET)
    public String showEditItemDeliverySettingsForm(final Model model, @PathVariable final long dsid)
            throws PrivilegeException, DomainEntityNotFoundException {
        final ItemDeliverySettings itemDeliverySettings = assessmentManagementService.lookupItemDeliverySettings(dsid);

        model.addAttribute(itemDeliverySettings);
        return "editItemDeliverySettingsForm";
    }

    @RequestMapping(value="/deliverysettings/{dsid}", method=RequestMethod.POST)
    public String handleEditItemDeliverySettingsForm(@PathVariable final long dsid,
            final @Valid @ModelAttribute ItemDeliverySettings command, final BindingResult result)
            throws PrivilegeException, DomainEntityNotFoundException {
        /* Validate command Object */
        if (result.hasErrors()) {
            return "editItemDeliverySettingsForm";
        }

        /* Perform update */
        assessmentManagementService.updateItemDeliverySettings(dsid, command);

        /* Return to show/edit
         * FIXME: Add some flash message here so that it's not confusing.
         */
        return buildActionRedirect("/deliverysettings/" + dsid);
    }

    public Map<Long, Map<String, String>> buildDeliverySettingsListRouting(final List<ItemDeliverySettings> itemDeliverySettingsList) {
        final Map<Long, Map<String, String>> result = new HashMap<Long, Map<String, String>>();
        for (final ItemDeliverySettings itemDeliverySettings : itemDeliverySettingsList) {
            result.put(itemDeliverySettings.getId(), buildDeliverySettingsRouting(itemDeliverySettings));
        }
        return result;
    }

    public Map<String, String> buildDeliverySettingsRouting(final ItemDeliverySettings itemDeliverySettings) {
        return buildDeliverySettingsRouting(itemDeliverySettings.getId().longValue());
    }

    public Map<String, String> buildDeliverySettingsRouting(final long dsid) {
        final Map<String, String> result = new HashMap<String, String>();
        result.put("show", buildActionPath("/deliverysettings/" + dsid));
        result.put("update", buildActionPath("/deliverysettings/" + dsid + "/update"));
        return result;
    }
}
