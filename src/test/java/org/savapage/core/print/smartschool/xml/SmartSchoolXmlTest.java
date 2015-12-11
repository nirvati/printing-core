/*
 * This file is part of the SavaPage project <http://savapage.org>.
 * Copyright (c) 2011-2015 Datraverse B.V.
 * Author: Rijk Ravestein.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * For more information, please contact Datraverse B.V. at this
 * address: info@datraverse.com
 */
package org.savapage.core.print.smartschool.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.bind.JAXBException;

import org.junit.Test;
import org.savapage.core.print.smartschool.SmartSchoolPrintStatusEnum;
import org.savapage.core.services.impl.SmartSchoolProxyServiceImpl;

/**
 *
 * @author Rijk Ravestein
 *
 */
public final class SmartSchoolXmlTest {

    final static String REQ_INFO_DATE = "2014-08-12 17:34:36";

    final static String DOCUMENT_ID = "2";
    final static String DOCUMENT_NAME = "Document.pdf";
    final static String DOCUMENT_COMMENT = "some comment";

    final static String REQUESTER_ID = "";
    final static String REQUESTER_REGISTRATIONNUMBER = "NULL";
    final static String REQUESTER_NAME = "Admin";
    final static String REQUESTER_SURNAME = "Admin";
    final static String REQUESTER_USERNAME = "admin";
    final static String REQUESTER_ROLE = "teacher";

    final static String JOBTICKET_LOCATION = "SavaPage Test Location";
    final static String JOBTICKET_TYPE = "print";

    final static String BOOLEAN_ON = "on";

    final static String PROCESSINFO_PAPERSIZE = "a4";
    final static String PROCESSINFO_RENDERMODE = "grayscale";
    final static String PROCESSINFO_DUPLEX = BOOLEAN_ON;
    final static String PROCESSINFO_STAPLE = BOOLEAN_ON;
    final static String PROCESSINFO_PUNCH = BOOLEAN_ON;
    final static String PROCESSINFO_FRONTCOVER = BOOLEAN_ON;
    final static String PROCESSINFO_BACKCOVER = BOOLEAN_ON;
    final static String PROCESSINFO_CONFIDENTIAL = BOOLEAN_ON;

    final static String JOB_TICKET_XML_TEST = "<?xml version=\"1.0\"?>"
            + "<jobticket location=\""
            + JOBTICKET_LOCATION
            + "\" type=\""
            + JOBTICKET_TYPE
            + "\">"
            + "<requestinfo><date>"
            + REQ_INFO_DATE
            + "</date></requestinfo>"
            + "<documents>"
            + "<document id=\""
            + DOCUMENT_ID
            + "\">"
            + "<name>"
            + DOCUMENT_NAME
            + "</name>"
            + "<comment>"
            + DOCUMENT_COMMENT
            + "</comment>"
            + "<requester>"
            + "<id>"
            + REQUESTER_ID
            + "</id>"
            + "<registrationnumber>"
            + REQUESTER_REGISTRATIONNUMBER
            + "</registrationnumber>"
            + "<name>"
            + REQUESTER_NAME
            + "</name>"
            + "<surname>"
            + REQUESTER_SURNAME
            + "</surname>"
            + "<username>"
            + REQUESTER_USERNAME
            + "</username>"
            + "<role>"
            + REQUESTER_ROLE
            + "</role>"
            + "</requester>"
            //
            + "<billinginfo>"
            + "<accounts>"
            + "<account>"
            + "<id></id>"
            + "<registrationnumber>NULL</registrationnumber>"
            + "<name>Admin</name>"
            + "<surname>Admin</surname>"
            + "<username>admin</username>"
            + "<class></class>"
            + "<copies>1</copies>"
            + "<extra>0</extra>"
            + "<role>teacher</role>"
            + "</account>"
            + "</accounts>"
            + "</billinginfo>"
            //
            + "<processinfo>"
            + "<papersize>"
            + PROCESSINFO_PAPERSIZE
            + "</papersize>"
            + "<rendermode>"
            + PROCESSINFO_RENDERMODE
            + "</rendermode>"
            + "<duplex>"
            + PROCESSINFO_DUPLEX
            + "</duplex>"
            + "<staple>"
            + PROCESSINFO_STAPLE
            + "</staple>"
            + "<punch>"
            + PROCESSINFO_PUNCH
            + "</punch>"
            + "<frontcover>"
            + PROCESSINFO_FRONTCOVER
            + "</frontcover>"
            + "<backcover>"
            + PROCESSINFO_BACKCOVER
            + "</backcover>"
            + "<confidential>"
            + PROCESSINFO_CONFIDENTIAL
            + "</confidential>"
            + "</processinfo>"
            // ----
            + "<deliverinfo>"
            + "<delivery>"
            + "<date>"
            + "2014-06-19"
            + "</date>" + "</delivery>" + "</deliverinfo>"
            //
            + "</document>" + "</documents>" + "</jobticket>";

    /**
     * @deprecated
     * @return
     */
    @Deprecated
    private static Jobticket createTestJobticket() {

        final Jobticket obj = new Jobticket();
        obj.setLocation("Datraverse");
        obj.setType("print");

        final Requestinfo reqInfo = new Requestinfo();
        reqInfo.setDate(SmartSchoolProxyServiceImpl.formatDate(new Date()));
        obj.setRequestinfo(reqInfo);

        final Documents docs = new Documents();
        obj.setDocuments(docs);

        final List<Document> docList = new ArrayList<>();
        docs.setDocument(docList);

        final Document doc = new Document();
        docList.add(doc);

        doc.setId(String.valueOf(System.currentTimeMillis()));
        doc.setName("test.pdf");

        Processinfo processinfo = new Processinfo();
        doc.setProcessinfo(processinfo);

        processinfo.setPapersize("A4");
        processinfo.setDuplex("no");
        processinfo.setRendermode("grayscale");

        final Requester requester = new Requester();

        requester.setId("rijk");
        requester.setRole("teacher");
        requester.setUsername("rijk");
        requester.setName("rijk");

        doc.setRequester(requester);

        final Billinginfo billinginfo = new Billinginfo();
        doc.setBillinginfo(billinginfo);

        final Accounts accounts = new Accounts();
        billinginfo.setAccounts(accounts);

        final List<Account> accountList = new ArrayList<>();
        accounts.setAccount(accountList);

        final Account account = new Account();
        accountList.add(account);
        account.setClazz("1A1");
        account.setCopies(Integer.valueOf(1));
        account.setRole("teacher");
        account.setId("rijk");
        account.setName("rijk");
        account.setUsername("rijk");

        return obj;
    }

    @Test
    public void test() throws JAXBException {

        final Jobticket jobTicket =
                SmartschoolXmlObject.create(Jobticket.class,
                        JOB_TICKET_XML_TEST);

        assertEquals(JOBTICKET_LOCATION, jobTicket.getLocation());
        assertEquals(JOBTICKET_TYPE, jobTicket.getType());

        assertEquals(REQ_INFO_DATE, jobTicket.getRequestinfo().getDate());

        assertTrue(jobTicket.getDocuments().getDocument().size() == 1);

        final Document doc = jobTicket.getDocuments().getDocument().get(0);

        assertEquals(DOCUMENT_COMMENT, doc.getComment());
        assertEquals(DOCUMENT_ID, doc.getId());
        assertEquals(DOCUMENT_NAME, doc.getName());

        //
        final Requester requester = doc.getRequester();

        assertEquals(REQUESTER_ID, requester.getId());
        assertEquals(REQUESTER_NAME, requester.getName());

        assertEquals(REQUESTER_SURNAME, requester.getSurname());
        assertEquals(REQUESTER_USERNAME, requester.getUsername());
        assertEquals(REQUESTER_REGISTRATIONNUMBER,
                requester.getRegistrationnumber());
        assertEquals(REQUESTER_ROLE, requester.getRole());

        //
        final Processinfo processInfo = doc.getProcessinfo();
        assertEquals(PROCESSINFO_BACKCOVER, processInfo.getBackcover());
        assertEquals(PROCESSINFO_CONFIDENTIAL, processInfo.getConfidential());
        assertEquals(PROCESSINFO_DUPLEX, processInfo.getDuplex());
        assertEquals(PROCESSINFO_FRONTCOVER, processInfo.getFrontcover());
        assertEquals(PROCESSINFO_PAPERSIZE, processInfo.getPapersize());
        assertEquals(PROCESSINFO_PUNCH, processInfo.getPunch());
        assertEquals(PROCESSINFO_RENDERMODE, processInfo.getRendermode());
        assertEquals(PROCESSINFO_STAPLE, processInfo.getStaple());
    }

    @Test
    public void test2() throws JAXBException {

        final DocumentStatusIn docStat = new DocumentStatusIn();

        docStat.setDocumentId("5");
        docStat.setComment("my comment");
        docStat.setCode(SmartSchoolPrintStatusEnum.COMPLETED.getXmlText());

        final String xml = docStat.asXmlString();

        final DocumentStatusIn docStat2 =
                SmartschoolXmlObject.create(DocumentStatusIn.class, xml);

        final String xml2 = docStat2.asXmlString();

        assertEquals(xml, xml2);
    }

}
