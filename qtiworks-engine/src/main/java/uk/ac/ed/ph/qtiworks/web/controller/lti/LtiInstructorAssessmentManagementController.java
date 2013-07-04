/* Copyright (c) 2012-2013, University of Edinburgh.
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
package uk.ac.ed.ph.qtiworks.web.controller.lti;

import uk.ac.ed.ph.qtiworks.QtiWorksLogicException;
import uk.ac.ed.ph.qtiworks.QtiWorksRuntimeException;
import uk.ac.ed.ph.qtiworks.domain.DomainEntityNotFoundException;
import uk.ac.ed.ph.qtiworks.domain.PrivilegeException;
import uk.ac.ed.ph.qtiworks.domain.entities.Assessment;
import uk.ac.ed.ph.qtiworks.domain.entities.CandidateSession;
import uk.ac.ed.ph.qtiworks.domain.entities.Delivery;
import uk.ac.ed.ph.qtiworks.domain.entities.DeliverySettings;
import uk.ac.ed.ph.qtiworks.domain.entities.ItemDeliverySettings;
import uk.ac.ed.ph.qtiworks.domain.entities.TestDeliverySettings;
import uk.ac.ed.ph.qtiworks.services.AssessmentDataService;
import uk.ac.ed.ph.qtiworks.services.AssessmentManagementService;
import uk.ac.ed.ph.qtiworks.services.CandidateSessionStarter;
import uk.ac.ed.ph.qtiworks.services.domain.AssessmentAndPackage;
import uk.ac.ed.ph.qtiworks.services.domain.AssessmentMetadataTemplate;
import uk.ac.ed.ph.qtiworks.services.domain.AssessmentPackageFileImportException;
import uk.ac.ed.ph.qtiworks.services.domain.AssessmentPackageFileImportException.APFIFailureReason;
import uk.ac.ed.ph.qtiworks.services.domain.AssessmentStateException;
import uk.ac.ed.ph.qtiworks.services.domain.AssessmentStateException.APSFailureReason;
import uk.ac.ed.ph.qtiworks.services.domain.EnumerableClientFailure;
import uk.ac.ed.ph.qtiworks.services.domain.ItemDeliverySettingsTemplate;
import uk.ac.ed.ph.qtiworks.services.domain.TestDeliverySettingsTemplate;
import uk.ac.ed.ph.qtiworks.web.GlobalRouter;
import uk.ac.ed.ph.qtiworks.web.domain.UploadAssessmentPackageCommand;

import uk.ac.ed.ph.jqtiplus.node.AssessmentObjectType;
import uk.ac.ed.ph.jqtiplus.validation.AssessmentObjectValidationResult;

import java.util.List;

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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/**
 * Controller for instructor assessment management when running over LTI (domain-level launch)
 *
 * @author David McKain
 */
@Controller
@RequestMapping("/resource/{lrid}")
public class LtiInstructorAssessmentManagementController {

    @Resource
    private LtiInstructorRouter ltiInstructorRouter;

    @Resource
    private LtiInstructorModelHelper ltiInstructorModelHelper;

    @Resource
    private AssessmentDataService assessmentDataService;

    @Resource
    private AssessmentManagementService assessmentManagementService;

    @Resource
    private CandidateSessionStarter candidateSessionStarter;

    //------------------------------------------------------

    @ModelAttribute
    public void setupModel(final Model model) {
        ltiInstructorModelHelper.setupModel(model);
    }

    //------------------------------------------------------

    @RequestMapping(value="", method=RequestMethod.GET)
    public String resourceTopPage(final Model model) {
        ltiInstructorModelHelper.setupModel(model);
        return "instructor/resource";
    }

    @RequestMapping(value="/debug", method=RequestMethod.GET)
    public String diagnosticsPage(final Model model) {
        ltiInstructorModelHelper.setupModel(model);
        return "instructor/debug";
    }

    //------------------------------------------------------
    // Assessment management

    /** Lists all Assignments in this LTI context */
    @RequestMapping(value="/assessments", method=RequestMethod.GET)
    public String listContextAssessments(final Model model) {
        final List<AssessmentAndPackage> assessments = assessmentDataService.getCallerLtiContextAssessments();
        model.addAttribute(assessments);
        model.addAttribute("assessmentListRouting", ltiInstructorRouter.buildAssessmentListRouting(assessments));
        return "instructor/listAssessments";
    }

    @RequestMapping(value="/assessments/upload-and-use", method=RequestMethod.GET)
    public String showUploadAndUseAssessmentForm(final Model model) {
        model.addAttribute(new UploadAssessmentPackageCommand());
        return "instructor/uploadAndUseAssessmentForm";
    }

    @RequestMapping(value="/assessments/upload-and-use", method=RequestMethod.POST)
    public String handleUploadAndUseAssessmentForm(final RedirectAttributes redirectAttributes,
            final @Valid @ModelAttribute UploadAssessmentPackageCommand command,
            final BindingResult result)
            throws PrivilegeException {
        Assessment assessment = null;
        if (!result.hasErrors()) {
            try {
                /* No binding errors, so attempt to import and validate the package */
                assessment = assessmentManagementService.importAssessment(command.getFile(), true);

                /* Use this assessment */
                assessmentManagementService.selectCurrentLtiResourceAssessment(assessment.getId());
            }
            catch (final AssessmentPackageFileImportException e) {
                final EnumerableClientFailure<APFIFailureReason> failure = e.getFailure();
                failure.registerErrors(result, "assessmentPackageUpload");
            }
            catch (final DomainEntityNotFoundException e) {
                throw new QtiWorksRuntimeException("New assessment disappeared immediately?");
            }
        }
        if (result.hasErrors()) {
            /* Return to form if any binding/service errors */
            return "instructor/uploadAndUseAssessmentForm";
        }
        GlobalRouter.addFlashMessage(redirectAttributes, "Assessment successfully created for immediate use");
        return ltiInstructorRouter.buildInstructorRedirect(""); /* Return immediately to dashboard */
    }

    @RequestMapping(value="/assessments/upload", method=RequestMethod.GET)
    public String showUploadAssessmentForm(final Model model) {
        model.addAttribute(new UploadAssessmentPackageCommand());
        return "instructor/uploadAssessmentForm";
    }

    @RequestMapping(value="/assessments/upload", method=RequestMethod.POST)
    public String handleUploadAssessmentForm(final RedirectAttributes redirectAttributes,
            final @Valid @ModelAttribute UploadAssessmentPackageCommand command,
            final BindingResult result)
            throws PrivilegeException {
        Assessment assessment = null;
        if (!result.hasErrors()) {
            /* No binding errors, so attempt to import and validate the package */
            try {
                assessment = assessmentManagementService.importAssessment(command.getFile(), true);
            }
            catch (final AssessmentPackageFileImportException e) {
                final EnumerableClientFailure<APFIFailureReason> failure = e.getFailure();
                failure.registerErrors(result, "assessmentPackageUpload");
                return "instructor/uploadAssessmentForm";
            }
        }
        if (result.hasErrors()) {
            /* Return to form if any binding/service errors */
            return "instructor/uploadAssessmentForm";
        }
        GlobalRouter.addFlashMessage(redirectAttributes, "Assessment successfully created");
        return ltiInstructorRouter.buildInstructorRedirect("/assessment/" + assessment.getId());
    }

    /** Shows the Assessment having the given ID (aid) */
    @RequestMapping(value="/assessment/{aid}", method=RequestMethod.GET)
    public String showAssessment(@PathVariable final long aid, final Model model)
            throws PrivilegeException, DomainEntityNotFoundException {
        final Assessment assessment = assessmentManagementService.lookupAssessment(aid);
        ltiInstructorModelHelper.setupModelForAssessment(assessment, model);
        return "instructor/showAssessment";
    }

    @RequestMapping(value="/assessment/{aid}/select", method=RequestMethod.POST)
    public String selectAssessment(@PathVariable final long aid)
            throws PrivilegeException, DomainEntityNotFoundException {
        assessmentManagementService.selectCurrentLtiResourceAssessment(aid);
        return ltiInstructorRouter.buildInstructorRedirect("/");
    }

    @RequestMapping(value="/assessment/{aid}/edit", method=RequestMethod.GET)
    public String showEditAssessmentForm(@PathVariable final long aid, final Model model)
            throws PrivilegeException, DomainEntityNotFoundException {
        final Assessment assessment = assessmentManagementService.lookupAssessment(aid);

        final AssessmentMetadataTemplate template = new AssessmentMetadataTemplate();
        template.setName(assessment.getName());
        template.setTitle(assessment.getTitle());
        model.addAttribute(template);

        ltiInstructorModelHelper.setupModelForAssessment(assessment, model);
        return "instructor/editAssessmentForm";
    }

    @RequestMapping(value="/assessment/{aid}/edit", method=RequestMethod.POST)
    public String handleEditAssessmentForm(@PathVariable final long aid, final Model model,
            final RedirectAttributes redirectAttributes,
            final @Valid @ModelAttribute AssessmentMetadataTemplate template, final BindingResult result)
            throws PrivilegeException, DomainEntityNotFoundException {
        /* Validate command Object */
        if (result.hasErrors()) {
            ltiInstructorModelHelper.setupModelForAssessment(aid, model);
            return "instructor/editAssessmentForm";
        }
        try {
            assessmentManagementService.updateAssessmentMetadata(aid, template);
        }
        catch (final BindException e) {
            throw new QtiWorksLogicException("Top layer validation is currently same as service layer in this case, so this Exception should not happen");
        }
        GlobalRouter.addFlashMessage(redirectAttributes, "Assessment successfully edited");
        return ltiInstructorRouter.buildInstructorRedirect("/assessment/" + aid);
    }

    @RequestMapping(value="/assessment/{aid}/replace", method=RequestMethod.GET)
    public String showReplaceAssessmentPackageForm(final @PathVariable long aid,
            final Model model)
            throws PrivilegeException, DomainEntityNotFoundException {
        model.addAttribute(new UploadAssessmentPackageCommand());
        ltiInstructorModelHelper.setupModelForAssessment(aid, model);
        return "instructor/replaceAssessmentPackageForm";
    }

    @RequestMapping(value="/assessment/{aid}/replace", method=RequestMethod.POST)
    public String handleReplaceAssessmentPackageForm(final @PathVariable long aid,
            final Model model, final RedirectAttributes redirectAttributes,
            final @Valid @ModelAttribute UploadAssessmentPackageCommand command, final BindingResult result)
            throws PrivilegeException, DomainEntityNotFoundException {
        if (!result.hasErrors()) {
            /* Attempt to import the package */
            final MultipartFile uploadFile = command.getFile();
            try {
                assessmentManagementService.replaceAssessmentPackage(aid, uploadFile, true);
            }
            catch (final AssessmentPackageFileImportException e) {
                final EnumerableClientFailure<APFIFailureReason> failure = e.getFailure();
                failure.registerErrors(result, "assessmentPackageUpload");
            }
            catch (final AssessmentStateException e) {
                final EnumerableClientFailure<APSFailureReason> failure = e.getFailure();
                failure.registerErrors(result, "assessmentPackageUpload");
            }
        }
        if (result.hasErrors()) {
            ltiInstructorModelHelper.setupModelForAssessment(aid, model);
            return "instructor/replaceAssessmentPackageForm";
        }
        GlobalRouter.addFlashMessage(redirectAttributes, "Assessment package content successfully replaced");
        return ltiInstructorRouter.buildInstructorRedirect("/assessment/{aid}");
    }

    @RequestMapping(value="/assessment/{aid}/validate", method=RequestMethod.GET)
    public String validateAssessment(final @PathVariable long aid, final Model model)
            throws PrivilegeException, DomainEntityNotFoundException {
        final AssessmentObjectValidationResult<?> validationResult = assessmentManagementService.validateAssessment(aid);
        model.addAttribute("validationResult", validationResult);
        ltiInstructorModelHelper.setupModelForAssessment(aid, model);
        return "instructor/validationResult";
    }

    @RequestMapping(value="/assessment/{aid}/delete", method=RequestMethod.POST)
    public String deleteAssessment(final @PathVariable long aid, final RedirectAttributes redirectAttributes)
            throws PrivilegeException, DomainEntityNotFoundException {
        assessmentManagementService.deleteAssessment(aid);
        GlobalRouter.addFlashMessage(redirectAttributes, "Assessment successfully deleted");
        return ltiInstructorRouter.buildInstructorRedirect("/assessments");
    }

    @RequestMapping(value="/assessment/{aid}/try", method=RequestMethod.POST)
    public String tryAssessment(final @PathVariable long aid)
            throws PrivilegeException, DomainEntityNotFoundException {
        final Assessment assessment = assessmentManagementService.lookupAssessment(aid);
        final Delivery demoDelivery = assessmentManagementService.createDemoDelivery(assessment, null);
        return runDelivery(aid, demoDelivery, true);
    }

    @RequestMapping(value="/assessment/{aid}/try/{dsid}", method=RequestMethod.POST)
    public String tryAssessment(final @PathVariable long aid, final @PathVariable long dsid)
            throws PrivilegeException, DomainEntityNotFoundException {
        final Assessment assessment = assessmentManagementService.lookupAssessment(aid);
        final DeliverySettings deliverySettings = assessmentManagementService.lookupAndMatchDeliverySettings(dsid, assessment);
        final Delivery demoDelivery = assessmentManagementService.createDemoDelivery(assessment, deliverySettings);
        return runDelivery(aid, demoDelivery, true);
    }

    private String runDelivery(final long aid, final Delivery delivery, final boolean authorMode)
            throws PrivilegeException {
        final String exitUrl = ltiInstructorRouter.buildWithinContextUrl("/assessment/" + aid);
        final CandidateSession candidateSession = candidateSessionStarter.createCandidateSession(delivery, authorMode, exitUrl, null, null);
        return GlobalRouter.buildSessionStartRedirect(candidateSession);
    }

    //------------------------------------------------------
    // Management of DeliverySettings (and subtypes)

    @RequestMapping(value="/deliverysettings", method=RequestMethod.GET)
    public String listOwnDeliverySettings(final Model model) {
        final List<DeliverySettings> deliverySettingsList = assessmentDataService.getCallerUserDeliverySettings();
        model.addAttribute("deliverySettingsList", deliverySettingsList);
        model.addAttribute("deliverySettingsListRouting", ltiInstructorRouter.buildDeliverySettingsListRouting(deliverySettingsList));
        return "instructor/listDeliverySettings";
    }

    @RequestMapping(value="/deliverysettings/create-for-item", method=RequestMethod.GET)
    public String showCreateItemDeliverySettingsForm(final Model model) {
        final long existingSettingsCount = assessmentDataService.countCallerUserDeliverySettings(AssessmentObjectType.ASSESSMENT_ITEM);
        final ItemDeliverySettingsTemplate template = assessmentDataService.createItemDeliverySettingsTemplate();
        template.setTitle("Item Delivery Settings #" + (existingSettingsCount+1));

        model.addAttribute(template);
        return "instructor/createItemDeliverySettingsForm";
    }

    @RequestMapping(value="/deliverysettings/create-for-test", method=RequestMethod.GET)
    public String showCreateTestDeliverySettingsForm(final Model model) {
        final long existingOptionCount = assessmentDataService.countCallerUserDeliverySettings(AssessmentObjectType.ASSESSMENT_TEST);
        final TestDeliverySettingsTemplate template = assessmentDataService.createTestDeliverySettingsTemplate();
        template.setTitle("Test Delivery Settings #" + (existingOptionCount+1));

        model.addAttribute(template);
        return "instructor/createTestDeliverySettingsForm";
    }

    @RequestMapping(value="/deliverysettings/create-for-item", method=RequestMethod.POST)
    public String handleCreateItemDeliverySettingsForm(final RedirectAttributes redirectAttributes,
            final @Valid @ModelAttribute ItemDeliverySettingsTemplate template,
            final BindingResult result)
            throws PrivilegeException {
        /* Validate command Object */
        if (result.hasErrors()) {
            return "instructor/createItemDeliverySettingsForm";
        }

        /* Try to create new entity */
        try {
            assessmentManagementService.createItemDeliverySettings(template);
        }
        catch (final BindException e) {
            throw new QtiWorksLogicException("Top layer validation is currently same as service layer in this case, so this Exception should not happen");
        }

        /* Go back to list */
        GlobalRouter.addFlashMessage(redirectAttributes, "Item Delivery Settings successfully created");
        return ltiInstructorRouter.buildInstructorRedirect("/deliverysettings");
    }

    @RequestMapping(value="/deliverysettings/create-for-test", method=RequestMethod.POST)
    public String handleCreateTestDeliverySettingsForm(final RedirectAttributes redirectAttributes,
            final @Valid @ModelAttribute TestDeliverySettingsTemplate template,
            final BindingResult result)
            throws PrivilegeException {
        /* Validate command Object */
        if (result.hasErrors()) {
            return "instructor/createTestDeliverySettingsForm";
        }

        /* Try to create new entity */
        try {
            assessmentManagementService.createTestDeliverySettings(template);
        }
        catch (final BindException e) {
            throw new QtiWorksLogicException("Top layer validation is currently same as service layer in this case, so this Exception should not happen");
        }

        /* Go back to list */
        GlobalRouter.addFlashMessage(redirectAttributes, "Test Delivery Settings successfully created");
        return ltiInstructorRouter.buildInstructorRedirect("/deliverysettings");
    }

    @RequestMapping(value="/deliverysettings/{dsid}/select", method=RequestMethod.POST)
    public String selectDeliverySettings(@PathVariable final long dsid)
            throws PrivilegeException, DomainEntityNotFoundException {
        assessmentManagementService.selectCurrentLtiResourceDeliverySettings(dsid);
        return ltiInstructorRouter.buildInstructorRedirect("/");
    }

    @RequestMapping(value="/deliverysettings/{dsid}/delete", method=RequestMethod.POST)
    public String deleteDeliverySettings(@PathVariable final long dsid, final RedirectAttributes redirectAttributes)
            throws PrivilegeException, DomainEntityNotFoundException {
        /* Delete settings and update any Deliveries using them */
        final int deliveriesAffected = assessmentManagementService.deleteDeliverySettings(dsid);

        /* Redirect back to list of settings */
        final StringBuilder flashMessageBuilder = new StringBuilder("Delivery Settings deleted.");
        if (deliveriesAffected>0) {
            if (deliveriesAffected==1) {
                flashMessageBuilder.append(" One Delivery was using these settings and has been updated to use default settings.");
            }
            else {
                flashMessageBuilder.append(" ")
                    .append(deliveriesAffected)
                    .append(" Deliveries were using these settings and have been updated to use default settings.");
            }
        }
        GlobalRouter.addFlashMessage(redirectAttributes, flashMessageBuilder.toString());
        return ltiInstructorRouter.buildInstructorRedirect("/deliverysettings");
    }

    @RequestMapping(value="/deliverysettings/{dsid}/for-item", method=RequestMethod.GET)
    public String showEditItemDeliverySettingsForm(@PathVariable final long dsid, final Model model)
            throws PrivilegeException, DomainEntityNotFoundException {
        final ItemDeliverySettings itemDeliverySettings = assessmentManagementService.lookupItemDeliverySettings(dsid);
        final ItemDeliverySettingsTemplate template = new ItemDeliverySettingsTemplate();
        assessmentDataService.mergeItemDeliverySettings(itemDeliverySettings, template);

        ltiInstructorModelHelper.setupModelForDeliverySettings(itemDeliverySettings, model);
        model.addAttribute(template);
        return "instructor/editItemDeliverySettingsForm";
    }

    @RequestMapping(value="/deliverysettings/{dsid}/for-test", method=RequestMethod.GET)
    public String showEditTestDeliverySettingsForm(@PathVariable final long dsid, final Model model)
            throws PrivilegeException, DomainEntityNotFoundException {
        final TestDeliverySettings testDeliverySettings = assessmentManagementService.lookupTestDeliverySettings(dsid);
        final TestDeliverySettingsTemplate template = new TestDeliverySettingsTemplate();
        assessmentDataService.mergeTestDeliverySettings(testDeliverySettings, template);

        ltiInstructorModelHelper.setupModelForDeliverySettings(testDeliverySettings, model);
        model.addAttribute(template);
        return "instructor/editTestDeliverySettingsForm";
    }

    @RequestMapping(value="/deliverysettings/{dsid}/for-item", method=RequestMethod.POST)
    public String handleEditItemDeliverySettingsForm(@PathVariable final long dsid, final Model model, final RedirectAttributes redirectAttributes,
            final @Valid @ModelAttribute ItemDeliverySettingsTemplate template, final BindingResult result)
            throws PrivilegeException, DomainEntityNotFoundException {
        /* Validate command Object */
        if (result.hasErrors()) {
            ltiInstructorModelHelper.setupModelForDeliverySettings(dsid, model);
            return "instructor/editItemDeliverySettingsForm";
        }

        /* Perform update */
        try {
            assessmentManagementService.updateItemDeliverySettings(dsid, template);
        }
        catch (final BindException e) {
            throw new QtiWorksLogicException("Top layer validation is currently same as service layer in this case, so this Exception should not happen");
        }

        /* Return to show/edit with a flash message */
        GlobalRouter.addFlashMessage(redirectAttributes, "Item Delivery Settings successfully changed");
        return ltiInstructorRouter.buildInstructorRedirect("/deliverysettings/" + dsid + "/for-item");
    }

    @RequestMapping(value="/deliverysettings/{dsid}/for-test", method=RequestMethod.POST)
    public String handleEditTestDeliverySettingsForm(@PathVariable final long dsid,
            final Model model, final RedirectAttributes redirectAttributes,
            final @Valid @ModelAttribute TestDeliverySettingsTemplate template, final BindingResult result)
            throws PrivilegeException, DomainEntityNotFoundException {
        /* Validate command Object */
        if (result.hasErrors()) {
            ltiInstructorModelHelper.setupModelForDeliverySettings(dsid, model);
            return "instructor/editTestDeliverySettingsForm";
        }

        /* Perform update */
        try {
            assessmentManagementService.updateTestDeliverySettings(dsid, template);
        }
        catch (final BindException e) {
            throw new QtiWorksLogicException("Top layer validation is currently same as service layer in this case, so this Exception should not happen");
        }

        /* Return to show/edit with a flash message */
        GlobalRouter.addFlashMessage(redirectAttributes, "Test Delivery Settings successfully changed");
        return ltiInstructorRouter.buildInstructorRedirect("/deliverysettings/" + dsid + "/for-test");
    }

}
