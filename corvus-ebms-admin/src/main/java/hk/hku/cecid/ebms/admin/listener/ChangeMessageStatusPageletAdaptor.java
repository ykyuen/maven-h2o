/* 
 * Copyright(c) 2005 Center for E-Commerce Infrastructure Development, The
 * University of Hong Kong (HKU). All Rights Reserved.
 *
 * This software is licensed under the Apache License Version 2.0 [1]
 * 
 * [1] http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package hk.hku.cecid.ebms.admin.listener;

import hk.hku.cecid.ebms.spa.EbmsProcessor;
import hk.hku.cecid.ebms.spa.dao.MessageDVO;
import hk.hku.cecid.ebms.spa.dao.MessageServerDAO;
import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.util.PropertyTree;
import hk.hku.cecid.piazza.corvus.admin.listener.AdminPageletAdaptor;

import javax.servlet.http.HttpServletRequest;
import javax.xml.transform.Source;

/**
 * @author Donahue Sze
 *  
 */
public class ChangeMessageStatusPageletAdaptor extends AdminPageletAdaptor {

    protected Source getCenterSource(HttpServletRequest request) {

        // construct the dom tree
        PropertyTree dom = new PropertyTree();
        dom.setProperty("/message_history", "");

        try {
            String messageId = request.getParameter("message_id");
            String messageBox = request.getParameter("message_box");
            String status = request.getParameter("status");

            MessageServerDAO msgServerDAO = 
				(MessageServerDAO)EbmsProcessor.core.dao.createDAO(MessageServerDAO.class);
            MessageDVO result = null;
            try{
            	result = msgServerDAO.resetMessage(messageId, messageBox, status);
            }catch(DAOException daoException){
            	result = null;
            	 EbmsProcessor.core.log.debug(
                         "Error occur when reseting message, record rollbacked",
                         daoException);
            }
            
            if(result != null){
                dom.setProperty("message[0]/message_id",
                        checkNullAndReturnEmpty(result.getMessageId()));
                dom.setProperty("message[0]/message_box",
                        checkNullAndReturnEmpty(result.getMessageBox()));
                dom.setProperty("message[0]/ref_to_message_id",
                                checkNullAndReturnEmpty(result
                                        .getRefToMessageId()));
                dom.setProperty("message[0]/message_type",
                        checkNullAndReturnEmpty(result.getMessageType()));
                dom.setProperty("message[0]/cpa_id",
                        checkNullAndReturnEmpty(result.getCpaId()));
                dom.setProperty("message[0]/service",
                        checkNullAndReturnEmpty(result.getService()));
                dom.setProperty("message[0]/action",
                        checkNullAndReturnEmpty(result.getAction()));
                dom.setProperty("message[0]/conv_id",
                        checkNullAndReturnEmpty(result.getConvId()));
                dom.setProperty("message[0]/time_stamp", result
                        .getTimeStamp().toString());
                dom.setProperty("message[0]/status",
                        checkNullAndReturnEmpty(result.getStatus()));
                dom.setProperty("message[0]/status_description", String
                        .valueOf(checkNullAndReturnEmpty(result
                                .getStatusDescription())));
                dom.setProperty("message[0]/from_party_id",
                        checkNullAndReturnEmpty(result.getFromPartyId()));
                dom.setProperty("message[0]/to_party_id",
                        checkNullAndReturnEmpty(result.getToPartyId()));

            }
            
            // set the search criteria
            dom.setProperty("search_criteria/message_id", "");
            dom.setProperty("search_criteria/message_box", "");
            dom.setProperty("search_criteria/cpa_id", "");
            dom.setProperty("search_criteria/service", "");
            dom.setProperty("search_criteria/action", "");
            dom.setProperty("search_criteria/conv_id", "");
            dom.setProperty("search_criteria/principal_id", "");
            dom.setProperty("search_criteria/status", "");
            dom.setProperty("search_criteria/num_of_messages", "");
            dom.setProperty("search_criteria/offset", "0");
            dom.setProperty("search_criteria/is_detail", "");
            dom.setProperty("search_criteria/message_time","");

        } catch (Exception e) {
            EbmsProcessor.core.log.debug(
                    "Unable to process the pagelet request", e);
        }
        return dom.getSource();
    }

    /**
     * @param messageId
     * @return
     */
    private String checkNullAndReturnEmpty(String value) {
        if (value == null) {
            return new String("");
        }
        return value;
    }

}