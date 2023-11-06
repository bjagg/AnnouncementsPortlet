/**
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.portlet.announcements.mvc.portlet.display;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletMode;
import javax.portlet.PortletPreferences;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jasig.portlet.announcements.model.Topic;
import org.jasig.portlet.announcements.model.TopicSubscription;
import org.jasig.portlet.announcements.mvc.IViewNameSelector;
import org.jasig.portlet.announcements.service.IAnnouncementsService;
import org.jasig.portlet.announcements.service.ITopicSubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * <p>AnnouncementsPreferencesController class.</p>
 *
 * @author eolsson
 * @version $Id: $Id
 */
@Controller
@RequestMapping("EDIT")
public class AnnouncementsPreferencesController {

  @Autowired private ITopicSubscriptionService tss = null;

  @Autowired private final IAnnouncementsService announcementsService = null;

  @Autowired(required = true)
  private final IViewNameSelector viewNameSelector = null;

  /** Constant <code>PREFERENCE_HIDE_ABSTRACT="AnnouncementsViewController.hideAbstrac"{trunked}</code> */
  public static final String PREFERENCE_HIDE_ABSTRACT = "AnnouncementsViewController.hideAbstract";

  private final Log logger = LogFactory.getLog(getClass());

  /**
   * <p>Setter for the field <code>tss</code>.</p>
   *
   * @param tss a {@link org.jasig.portlet.announcements.service.ITopicSubscriptionService} object.
   */
  public void setTss(ITopicSubscriptionService tss) {
    this.tss = tss;
  }

  /**
   * <p>editPreferences.</p>
   *
   * @param model a {@link org.springframework.ui.Model} object.
   * @param request a {@link javax.portlet.RenderRequest} object.
   * @return a {@link java.lang.String} object.
   * @throws javax.portlet.PortletException if any.
   */
  @RequestMapping()
  public String editPreferences(Model model, RenderRequest request) throws PortletException {

    PortletPreferences prefs = request.getPreferences();
    List<TopicSubscription> myTopics = tss.getTopicSubscriptionEdit(request);

    if (request.getRemoteUser() == null) {
      model.addAttribute("isGuest", Boolean.TRUE);
    } else {
      model.addAttribute("isGuest", Boolean.FALSE);
    }
    model.addAttribute("topicSubscriptions", myTopics);
    model.addAttribute("topicsToUpdate", myTopics.size());
    model.addAttribute(
        "prefHideAbstract", Boolean.valueOf(prefs.getValue(PREFERENCE_HIDE_ABSTRACT, "false")));
    return viewNameSelector.select(request, "editDisplayPreferences");
  }

  /**
   * <p>savePreferences.</p>
   *
   * @param request a {@link javax.portlet.ActionRequest} object.
   * @param response a {@link javax.portlet.ActionResponse} object.
   * @param topicsToUpdate a {@link java.lang.Integer} object.
   * @throws javax.portlet.PortletException if any.
   * @throws java.io.IOException if any.
   */
  @RequestMapping()
  public void savePreferences(
      ActionRequest request,
      ActionResponse response,
      @RequestParam("topicsToUpdate") Integer topicsToUpdate)
      throws PortletException, IOException {

    PortletPreferences prefs = request.getPreferences();
    List<TopicSubscription> newSubscription = new ArrayList<TopicSubscription>();

    for (int i = 0; i < topicsToUpdate; i++) {
      Long topicId = Long.valueOf(request.getParameter("topicId_" + i));

      // Will be numeric for existing, persisted TopicSubscription
      // instances;  blank (due to null id field) otherwise
      String topicSubId = request.getParameter("topicSubId_" + i).trim();

      Boolean subscribed = Boolean.valueOf(request.getParameter("subscribed_" + i));
      Topic topic = announcementsService.getTopic(topicId);

      // Make sure that any pushed_forced topics weren't sneakingly removed (by tweaking the URL, for example)
      if (topic.getSubscriptionMethod() == Topic.PUSHED_FORCED) {
        subscribed = Boolean.TRUE;
      }

      TopicSubscription ts = new TopicSubscription(request.getRemoteUser(), topic, subscribed);
      if (topicSubId.length() > 0) {
        // This TopicSubscription represents an existing, persisted entity
        try {
          ts.setId(Long.valueOf(topicSubId));
        } catch (NumberFormatException nfe) {
          logger.debug(nfe.getMessage(), nfe);
        }
      }

      newSubscription.add(ts);
    }

    if (newSubscription.size() > 0) {
      try {
        announcementsService.addOrSaveTopicSubscription(newSubscription);
      } catch (Exception e) {
        logger.error(
            "ERROR saving TopicSubscriptions for user "
                + request.getRemoteUser()
                + ". Message: "
                + e.getMessage());
      }
    }

    String hideAbstract = Boolean.valueOf(request.getParameter("hideAbstract")).toString();
    prefs.setValue(PREFERENCE_HIDE_ABSTRACT, hideAbstract);
    prefs.store();

    response.setPortletMode(PortletMode.VIEW);
    response.setRenderParameter("action", "displayAnnouncements");
  }

  /**
   * <p>isGuest.</p>
   *
   * @param req a {@link javax.portlet.PortletRequest} object.
   * @return a boolean.
   */
  @ModelAttribute("isGuest")
  public boolean isGuest(PortletRequest req) {
    boolean rslt = (req.getRemoteUser() == null);
    logger.debug("isGuest is: " + Boolean.toString(rslt));
    logger.debug("remoteUser is: " + req.getRemoteUser());
    return rslt;
  }
}
