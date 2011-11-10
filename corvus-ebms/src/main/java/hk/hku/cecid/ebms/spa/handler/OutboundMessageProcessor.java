/* 
 * Copyright(c) 2005 Center for E-Commerce Infrastructure Development, The
 * University of Hong Kong (HKU). All Rights Reserved.
 *
 * This software is licensed under the GNU GENERAL PUBLIC LICENSE Version 2.0 [1]
 * 
 * [1] http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt
 */

package hk.hku.cecid.ebms.spa.handler;

import hk.hku.cecid.ebms.pkg.EbxmlMessage;
import hk.hku.cecid.ebms.pkg.MessageOrder;
import hk.hku.cecid.ebms.spa.EbmsProcessor;
import hk.hku.cecid.ebms.spa.dao.MessageDAO;
import hk.hku.cecid.ebms.spa.dao.MessageDVO;
import hk.hku.cecid.ebms.spa.dao.MessageServerDAO;
import hk.hku.cecid.ebms.spa.dao.OutboxDVO;
import hk.hku.cecid.ebms.spa.dao.PartnershipDAO;
import hk.hku.cecid.ebms.spa.dao.PartnershipDVO;
import hk.hku.cecid.ebms.spa.dao.RepositoryDVO;
import hk.hku.cecid.ebms.spa.listener.EbmsRequest;
import hk.hku.cecid.ebms.spa.listener.EbmsResponse;
import hk.hku.cecid.piazza.commons.dao.DAOException;
import hk.hku.cecid.piazza.commons.soap.SOAPRequest;
import hk.hku.cecid.piazza.commons.soap.WebServicesRequest;
import hk.hku.cecid.piazza.commons.util.Generator;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.xml.soap.SOAPException;

/**
 * @author Donahue Sze
 * 
 */
public class OutboundMessageProcessor {

    static OutboundMessageProcessor outboundMessageProcessor;

    static boolean outboundMessageProcessor_initFlag = false;

    public synchronized static OutboundMessageProcessor getInstance() {
        if (!outboundMessageProcessor_initFlag) {
            outboundMessageProcessor = new OutboundMessageProcessor();
            outboundMessageProcessor_initFlag = true;
        }
        return outboundMessageProcessor;
    }

    private OutboundMessageProcessor() {
    }

    public void processOutgoingMessage(EbmsRequest request,
            EbmsResponse response) throws MessageServiceHandlerException {

        EbxmlMessage ebxmlRequestMessage;
        String contentType = null;

        try {
            ebxmlRequestMessage = request.getMessage();

            // generate message id if it does not exist
            if (ebxmlRequestMessage.getMessageHeader().getMessageId() == null) {
                String messageId = Generator.generateMessageID();
                ebxmlRequestMessage.getMessageHeader().setMessageId(messageId);
                EbmsProcessor.core.log.info("Genereating message id: "
                        + messageId);
            }

            // classify where the message come from
            if (request.getSource() != null) {
                if (request.getSource() instanceof SOAPRequest) {
                    SOAPRequest soapRequest = (SOAPRequest) request.getSource();
                    if (soapRequest.getSource() instanceof HttpServletRequest) {
                        // message come from EbmsOutboundListener
                        // set the bytes already in EbmsAdaptor
                        HttpServletRequest httpServletRequest = (HttpServletRequest) soapRequest
                                .getSource();
                        if (httpServletRequest.getUserPrincipal() != null) {
                            contentType = httpServletRequest
                                    .getHeader("content-type");
                            MessageClassifier messageClassifier = new MessageClassifier(
                                    ebxmlRequestMessage);
                            if (messageClassifier.isMessageOrder()) {
                                storeOutgoingOrderedMessage(
                                        ebxmlRequestMessage, contentType);
                            } else {
                                storeOutgoingMessage(ebxmlRequestMessage, contentType);
                            }
                            return;
                        }
                    }
                } else if (request.getSource() instanceof WebServicesRequest) {
                    // message come from EbmsMessageSenderService
                    WebServicesRequest webServicesRequest = (WebServicesRequest) request
                            .getSource();
                    if (webServicesRequest.getSource() instanceof SOAPRequest) {
                        SOAPRequest soapRequest = (SOAPRequest) webServicesRequest
                                .getSource();
                        if (soapRequest.getSource() instanceof HttpServletRequest) {
                            HttpServletRequest httpServletRequest = (HttpServletRequest) soapRequest
                                    .getSource();
                            if (httpServletRequest.getUserPrincipal() != null) {
                                generateAndStoreEbxmlMessage(
                                        ebxmlRequestMessage, contentType);
                                return;
                            } else {
                                // no need for auth
                                generateAndStoreEbxmlMessage(
                                        ebxmlRequestMessage, contentType);
                                return;
                            }
                        }
                    }
                }
            } else {
                // msh generate reply msg (without source, no content type)
                // such as acknowledgement, error message
                storeOutgoingMessage(ebxmlRequestMessage, contentType);
                return;
            }
            throw new RuntimeException(
                    "Outbound Message Processor - invalid messsage: "
                            + ebxmlRequestMessage.getMessageId());

        } catch (Exception e) {
            throw new MessageServiceHandlerException(
                    "Error in processing outgoing message", e);
        }
    }

    /**
     * @param ebxmlRequestMessage
     * @param principalId
     * @param contentType
     * @throws DAOException
     * @throws SOAPException
     * @throws MessageServiceHandlerException
     */
    private synchronized void storeOutgoingOrderedMessage(
            EbxmlMessage ebxmlRequestMessage, String contentType) 
    	throws DAOException, SOAPException, MessageServiceHandlerException {

        MessageDAO messageDAO = (MessageDAO) EbmsProcessor.core.dao
                .createDAO(MessageDAO.class);

        // add the sequence no if necessary
        // for auto generating ebxmlmessage by server
        if (ebxmlRequestMessage.getMessageOrder() == null) {
            MessageDVO messageCPADVO = (MessageDVO) messageDAO.createDVO();
            messageCPADVO.setMessageBox(MessageClassifier.MESSAGE_BOX_OUTBOX);
            messageCPADVO.setCpaId(ebxmlRequestMessage.getCpaId());
            messageCPADVO.setService(ebxmlRequestMessage.getService());
            messageCPADVO.setAction(ebxmlRequestMessage.getAction());
            messageCPADVO.setConvId(ebxmlRequestMessage.getConversationId());

            int previousMaxSequenceNo = messageDAO
            		.findMaxSequenceNoByMessageBoxAndCpa(messageCPADVO);
            int status = (previousMaxSequenceNo == -1) ? MessageOrder.STATUS_RESET
                    : MessageOrder.STATUS_CONTINUE;
            int currentSequenceNo = previousMaxSequenceNo + 1;

            if (previousMaxSequenceNo >= 9000000) {
                EbmsProcessor.core.log.debug("Try to reset the sequence");
                if (isResetAllowed(messageCPADVO)) {
                    status = 0;
                    currentSequenceNo = 0;
                    EbmsProcessor.core.log.debug("Reset the sequence allowed");
                } else {
                    EbmsProcessor.core.log
                            .debug("Reset the sequence not allowed");
                }
            }

            EbmsProcessor.core.log.debug("Ordered message ("
                    + ebxmlRequestMessage.getMessageId()
                    + ") with sequence no: " + currentSequenceNo);

            ebxmlRequestMessage.addMessageOrder(status, currentSequenceNo);
        }

        // message type classification
        MessageClassifier messageClassifier = new MessageClassifier(
                ebxmlRequestMessage);
        String messageType = messageClassifier.getMessageType();
        EbxmlMessageDAOConvertor message = new EbxmlMessageDAOConvertor(
                ebxmlRequestMessage, MessageClassifier.MESSAGE_BOX_OUTBOX,
                messageType);

        MessageDVO messageDVO = message.getMessageDVO();
        messageDVO.setStatus(MessageClassifier.INTERNAL_STATUS_PENDING);
        // update the sequence group
        int currentMaxSequenceGroup = messageDAO
                .findMaxSequenceGroupByMessageBoxAndCpa(messageDVO);
        if (messageClassifier.isSeqeunceStatusReset()) {
            currentMaxSequenceGroup++;
            EbmsProcessor.core.log
                    .debug("Ordered RESET message with new sequence group "
                            + currentMaxSequenceGroup + " for message: "
                            + ebxmlRequestMessage.getMessageId());
        } else {
            EbmsProcessor.core.log.debug("Ordered message with sequence group "
                    + currentMaxSequenceGroup + " for message: "
                    + ebxmlRequestMessage.getMessageId());
        }
        messageDVO.setSequenceGroup(currentMaxSequenceGroup);

        RepositoryDVO repositoryDVO = message.getRepositoryDVO();
        if (contentType != null) {
            repositoryDVO.setContentType(contentType);
        }
        OutboxDVO outboxDVO = message.getOutboxDVO();

        MessageServerDAO messageServerDAO = (MessageServerDAO) EbmsProcessor.core.dao
                .createDAO(MessageServerDAO.class);
        messageServerDAO.storeOutboxMessage(messageDVO, repositoryDVO,
                outboxDVO);
        EbmsProcessor.core.log.info("Store outgoing ordered message: "
                + ebxmlRequestMessage.getMessageId());

    }

    /**
     * @param messageCPADVO
     * @return
     * @throws DAOException
     */
    private boolean isResetAllowed(MessageDVO messageCPADVO)
            throws DAOException {
        MessageDAO messageDAO = (MessageDAO) EbmsProcessor.core.dao
                .createDAO(MessageDAO.class);
        messageCPADVO
                .setStatus(MessageClassifier.INTERNAL_STATUS_DELIVERY_FAILURE);
        // find all the failed outbox sequence messageDVO using cpa
        List failedList = messageDAO
                .findOrderedMessagesByMessageBoxAndCpaAndStatus(messageCPADVO);
        if (failedList.size() != 0) {
            return false;
        }
        messageCPADVO.setStatus(MessageClassifier.INTERNAL_STATUS_PROCESSING);
        // find all the processing outbox sequence messageDVO using cpa
        List processingList = messageDAO
                .findOrderedMessagesByMessageBoxAndCpaAndStatus(messageCPADVO);
        // the number of processing sequence msg more than 1
        if (processingList.size() > 1) {
            return false;
        }
        messageCPADVO
                .setStatus(MessageClassifier.INTERNAL_STATUS_PROCESSED_ERROR);
        // find all the processing outbox sequence messageDVO using cpa
        List processedErrorList = messageDAO
                .findOrderedMessagesByMessageBoxAndCpaAndStatus(messageCPADVO);
        // the number of processing sequence msg more than 1
        if (processedErrorList.size() > 1) {
            return false;
        }
        return true;
    }

    /**
     * @throws DAOException
     *  
     */
    private void storeOutgoingMessage(
    		EbxmlMessage ebxmlRequestMessage, String contentType) throws DAOException {
        // message type classification
        MessageClassifier messageClassifier = new MessageClassifier(
                ebxmlRequestMessage);
        String messageType = messageClassifier.getMessageType();

        MessageServerDAO dao = (MessageServerDAO) EbmsProcessor.core.dao
                .createDAO(MessageServerDAO.class);
        EbxmlMessageDAOConvertor message = new EbxmlMessageDAOConvertor(
                ebxmlRequestMessage, MessageClassifier.MESSAGE_BOX_OUTBOX,
                messageType);
        
        MessageDVO messageDVO = message.getMessageDVO();
        messageDVO.setStatus(MessageClassifier.INTERNAL_STATUS_PENDING);

        RepositoryDVO repositoryDVO = message.getRepositoryDVO();
        if (contentType != null) {
            repositoryDVO.setContentType(contentType);
        }

        dao.storeOutboxMessage(messageDVO, repositoryDVO, message
                .getOutboxDVO());

        EbmsProcessor.core.log.info("Store outgoing message: "
                + ebxmlRequestMessage.getMessageId());
    }

    /**
     * @throws DAOException
     * @throws SOAPException
     * @throws MessageServiceHandlerException
     *  
     */
    private void generateAndStoreEbxmlMessage(
    		EbxmlMessage ebxmlRequestMessage, String contentType) throws DAOException,
            SOAPException, MessageServiceHandlerException {

        // find the cpa and set the related element in the ebxmlMessage
        PartnershipDAO partnershipDAO = (PartnershipDAO) EbmsProcessor.core.dao
                .createDAO(PartnershipDAO.class);
        PartnershipDVO partnershipDVO = (PartnershipDVO) partnershipDAO
                .createDVO();
        partnershipDVO.setCpaId(ebxmlRequestMessage.getCpaId());
        partnershipDVO.setService(ebxmlRequestMessage.getService());
        partnershipDVO.setAction(ebxmlRequestMessage.getAction());

        if (partnershipDAO.findPartnershipByCPA(partnershipDVO)) {

            // set the sync reply element
            if (partnershipDVO.getSyncReplyMode() != null) {
                if (partnershipDVO.getSyncReplyMode().equalsIgnoreCase(
                        "mshSignalsOnly")) {
                    ebxmlRequestMessage.addSyncReply();
                }
            }

            // set the ack requested element
            if (partnershipDVO.getAckRequested() != null) {
                if (partnershipDVO.getAckRequested().equalsIgnoreCase("always")) {
                    if (partnershipDVO.getAckSignRequested() != null) {
                        if (partnershipDVO.getAckSignRequested()
                                .equalsIgnoreCase("always")) {
                            ebxmlRequestMessage.addAckRequested(true);
                        } else {
                            ebxmlRequestMessage.addAckRequested(false);
                        }
                    } else {
                        ebxmlRequestMessage.addAckRequested(false);
                    }
                }
            }

            // set message order
            if (partnershipDVO.getMessageOrder() != null) {
                if (partnershipDVO.getMessageOrder().equalsIgnoreCase(
                        "Guaranteed")) {
                    // if msg order is on, find the next sequence no
                    storeOutgoingOrderedMessage(ebxmlRequestMessage, contentType);
                } else {
                    // set duplicate elimination
                    if (partnershipDVO.getDupElimination().equalsIgnoreCase(
                            "always")) {
                        ebxmlRequestMessage.getMessageHeader()
                                .setDuplicateElimination();
                    }
                    storeOutgoingMessage(ebxmlRequestMessage, contentType);
                }
            } else {
                // set duplicate elimination
                if (partnershipDVO.getDupElimination().equalsIgnoreCase(
                        "always")) {
                    ebxmlRequestMessage.getMessageHeader()
                            .setDuplicateElimination();
                }
                storeOutgoingMessage(ebxmlRequestMessage, contentType);
            }

        } else {
            EbmsProcessor.core.log.error("Partnership not found");
            throw new MessageServiceHandlerException("Partnership not found");
        }

    }
}