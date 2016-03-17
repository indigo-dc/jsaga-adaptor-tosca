/*
 * ====================================================================
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package it.infn.ct.jsaga.adaptor.tosca.job;

import it.infn.ct.jsaga.adaptor.tosca.ToscaAdaptorCommon;
import fr.in2p3.jsaga.adaptor.base.usage.U;
import fr.in2p3.jsaga.adaptor.base.usage.UAnd;
import fr.in2p3.jsaga.adaptor.base.usage.UOptional;
import fr.in2p3.jsaga.adaptor.base.usage.Usage;
import fr.in2p3.jsaga.adaptor.job.control.advanced.CleanableJobAdaptor;
import fr.in2p3.jsaga.adaptor.job.control.staging.StagingJobAdaptorTwoPhase;
import fr.in2p3.jsaga.adaptor.job.control.staging.StagingTransfer;
import fr.in2p3.jsaga.adaptor.job.control.JobControlAdaptor;
import fr.in2p3.jsaga.adaptor.job.monitor.JobMonitorAdaptor;
import fr.in2p3.jsaga.adaptor.job.BadResource;
import fr.in2p3.jsaga.adaptor.job.control.description.JobDescriptionTranslator;
import fr.in2p3.jsaga.adaptor.ssh3.job.SSHJobControlAdaptor;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.error.AuthenticationFailedException;
import org.ogf.saga.error.AuthorizationFailedException;
import org.ogf.saga.error.BadParameterException;
import org.ogf.saga.error.IncorrectURLException;
import org.ogf.saga.error.TimeoutException;
import org.ogf.saga.error.PermissionDeniedException;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.Inet4Address;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.logging.Level;
import org.apache.log4j.Logger;
import org.apache.commons.net.telnet.TelnetClient;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/* *********************************************
 * *** Istituto Nazionale di Fisica Nucleare ***
 * ***      Sezione di Catania (Italy)       ***
 * ***        http://www.ct.infn.it/         ***
 * *********************************************
 * File:    ToscaJobControlAdaptor.java
 * Authors: Giuseppe LA ROCCA, 
 *          Riccardo BRUNO,
 *          Marco FARGETTA
 * Email:   <giuseppe.larocca,
 *           riccardo.bruno,
 *           marco.fargetta>@ct.infn.it
 * Ver.:    1.0.0
 * Date:    17 March 2016
 * *********************************************/
public class ToscaJobControlAdaptor extends ToscaAdaptorCommon
        implements JobControlAdaptor,
        StagingJobAdaptorTwoPhase,
        CleanableJobAdaptor {

    protected String toscaId = "unset";

    private static final Logger log
            = Logger.getLogger(ToscaJobControlAdaptor.class);

    private ToscaJobMonitorAdaptor toscaJobMonitorAdaptor
            = new ToscaJobMonitorAdaptor();

    private SSHJobControlAdaptor sshControlAdaptor
            = new SSHJobControlAdaptor();

    private String action = "";
    private String tosca_template = "";
    private URL endpoint;

    @Override
    public void connect(String userInfo, String host, int port, String basePath, Map attributes)
            throws NotImplementedException,
            AuthenticationFailedException,
            AuthorizationFailedException,
            IncorrectURLException,
            BadParameterException,
            TimeoutException,
            NoSuccessException {

        log.debug("Connect (begin)");

        // Extract parameters from connection URL
        action = (String) attributes.get(ACTION);
        tosca_template = (String) attributes.get(TOSCA_TEMPLATE);

        // View parameters
        log.debug("userInfo      : '" + userInfo + "'" + LS
                + "host          : '" + host + "'" + LS
                + "port          : '" + port + "'" + LS
                + "basePath      : '" + basePath + "'" + LS
                + "attributes    : '" + attributes + "'" + LS
                + "action        : '" + action + "'" + LS
                + "tosca_template: '" + tosca_template + "'");

        action = (String) attributes.get(ACTION);
        tosca_template = (String) attributes.get(TOSCA_TEMPLATE);

        try {
            endpoint = new URL("http", host, port, basePath);
        } catch (MalformedURLException ex) {
            log.error("Error in the service end-point creation" + ex);
            throw new BadParameterException(ex);
        }
        log.debug("action:" + action);
        log.debug("tosca_template: " + tosca_template);

        // Get SSH security credentials 
        sshControlAdaptor.setSecurityCredential(credential.getSSHCredential());
    }

    @Override
    public void start(String nativeJobId) throws PermissionDeniedException,
            TimeoutException,
            NoSuccessException {

        log.debug("start (begin)");
        String[] jobIdInfo = getInfoFromNativeJobId(nativeJobId);
        String _publicIP = jobIdInfo[1];
        int _sshPort = 22;
        try {
            Integer.parseInt(jobIdInfo[2]);
        } catch (NumberFormatException e) {
            log.warn("Unable to get integer SSH port value from jobId '" + nativeJobId + "';");
        }
        String _nativeJobId = jobIdInfo[3];
        try {
            sshControlAdaptor.connect(null, _publicIP, _sshPort, null, new HashMap());
            sshControlAdaptor.start(_nativeJobId);
        } catch (NotImplementedException ex) {
            throw new NoSuccessException(ex);
        } catch (AuthenticationFailedException ex) {
            throw new PermissionDeniedException(ex);
        } catch (AuthorizationFailedException ex) {
            throw new PermissionDeniedException(ex);
        } catch (BadParameterException ex) {
            throw new NoSuccessException(ex);
        }
        log.debug("start (end)");
    }

    @Override
    public void cancel(String nativeJobId) throws PermissionDeniedException,
            TimeoutException,
            NoSuccessException {
        log.debug("cancel (begin)");
        String[] jobIdInfo = getInfoFromNativeJobId(nativeJobId);
        String _publicIP = jobIdInfo[1];
        int _sshPort = 22;
        try {
            Integer.parseInt(jobIdInfo[2]);
        } catch (NumberFormatException e) {
            log.warn("Unable to get integer SSH port value from jobId '" + nativeJobId + "';");
        }
        String _nativeJobId = jobIdInfo[3];
        try {
            sshControlAdaptor.connect(null, _publicIP, _sshPort, null, new HashMap());
            sshControlAdaptor.cancel(_nativeJobId);
        } catch (NotImplementedException ex) {
            throw new NoSuccessException(ex);
        } catch (AuthenticationFailedException ex) {
            throw new PermissionDeniedException(ex);
        } catch (AuthorizationFailedException ex) {
            throw new PermissionDeniedException(ex);
        } catch (BadParameterException ex) {
            throw new NoSuccessException(ex);
        }
        log.debug("cancel (end)");
    }
    
    private String getTOSCAStatus(String json) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(json);
        
        return (String) jsonObject.get("status");
    }
    
    private String getTOSCASUUID(String json) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(json);
        
        return (String) jsonObject.get("uuid");
    }

    @Override
    public void clean(String nativeJobId) throws PermissionDeniedException,
            TimeoutException,
            NoSuccessException {                
        log.debug("clean (begin)");
        String[] jobIdInfo = getInfoFromNativeJobId(nativeJobId);
        String _publicIP = jobIdInfo[1];
        int _sshPort = 22;
        try {
            Integer.parseInt(jobIdInfo[2]);
        } catch (NumberFormatException e) {
            log.warn("Unable to get integer SSH port value from jobId '" + nativeJobId + "';");
        }
        String _nativeJobId = jobIdInfo[3];
        try {
            sshControlAdaptor.connect(null, _publicIP, _sshPort, null, new HashMap());
            sshControlAdaptor.clean(_nativeJobId);

            // Stopping the VM Server
            //results = run_OCCI("delete", Execute);            
        } catch (NotImplementedException ex) {
            throw new NoSuccessException(ex);
        } catch (AuthenticationFailedException ex) {
            throw new PermissionDeniedException(ex);
        } catch (AuthorizationFailedException ex) {
            throw new PermissionDeniedException(ex);
        } catch (BadParameterException ex) {
            throw new NoSuccessException(ex);
        }
        log.debug("clean (end)");
    }
    
    /**
     * Retrieve information included in the Tosca JobId
     *
     * @param nativeJobId
     * @return
     */
    private String[] getInfoFromNativeJobId(String nativeJobId) {
        String _publicIP = nativeJobId.substring(nativeJobId.indexOf("@") + 1,
                nativeJobId.indexOf(":"));
        String _sshPort = nativeJobId.substring(nativeJobId.indexOf(":") + 1,
                nativeJobId.indexOf("#"));
        String _nativeJobId = nativeJobId.substring(0, nativeJobId.indexOf("@"));

        log.debug("nativeJobId: " + nativeJobId);
        log.debug("_publicIP: " + _publicIP);
        log.debug("_sshPort: " + _sshPort);
        log.debug("_nativeJobId: " + _nativeJobId);

        String[] info = {nativeJobId, _publicIP, _sshPort, _nativeJobId};
        return info;
    }
    
    private String[] submitTosca() {    
        String [] results = null;
        
        StringBuilder postData = new StringBuilder();
        postData.append("{ \"template\": \"");
        try {
            postData.append(new String(Files.readAllBytes(Paths.get(tosca_template))).replace("\n", "\\n"));
        } catch (IOException ex) {
            log.error("Template is not readable!");
        }
        postData.append("\"  }");

        log.debug("JSON Data sent to the orchestrator: \n" + postData);
        HttpURLConnection conn;
        try {
            conn = (HttpURLConnection) endpoint.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("charset", "utf-8");
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(postData.toString());
            wr.flush();
            wr.close();
            log.debug("Orchestrator status code: " + conn.getResponseCode());
            log.debug("Orchestrator status message: " + conn.getResponseMessage());
            if (conn.getResponseCode() == 201) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String ln;
                while ((ln = br.readLine()) != null) {
                    sb.append(ln);
                }
                log.debug("Orchestrator result: " + sb);
                String uuid = getTOSCASUUID(sb.toString());
                String status = getTOSCAStatus(sb.toString());
            }
        } catch (IOException ex) {
            log.error("Connection error with the service at " + endpoint.toString());
            log.error(ex);
        } catch (ParseException ex) {
            log.error("Orchestrator response not parsable");
        }
        
        return results;
    }
        
    @Override
    public String submit(String jobDesc, boolean checkMatch, String uniqId)
            throws PermissionDeniedException,
            TimeoutException,
            NoSuccessException,
            BadResource {
        log.debug("submit (begin)");
        String result = "";
               
        // SUbmit works in two stages; first create the Tosca resource
        // from the given toca_template, then submit the job to an 
        // SSH instance belonging to the Tosca resources
        if (action.equals("create")) {

            log.info("Creating a new tosca resource, please wait ...");
            log.debug("action:" + action);
            log.debug("tosca_template:" + tosca_template);

            // Create Tosca resource form tosca_template, then wait
            // for its creation and determine an access point with SSH:
            // IP/Port and credentials (username, PublicKey and PrivateKey)
            // String[] ToscaResults = submitTosca(tosca_template,...
            // Once tosca resource is ready, submit to SSH
            // String publicIP = ToscaResults[0];
            // String sshPort = toscaResults[1];            
            String publicIP = "127.0.0.1";
            int sshPort = 22;
            try {
                sshControlAdaptor.connect(null, publicIP, sshPort, null, new HashMap());
            } catch (NotImplementedException ex) {
                throw new NoSuccessException(ex);
            } catch (AuthenticationFailedException ex) {
                throw new PermissionDeniedException(ex);
            } catch (AuthorizationFailedException ex) {
                throw new PermissionDeniedException(ex);
            } catch (BadParameterException ex) {
                throw new NoSuccessException(ex);
            }
            result = sshControlAdaptor.submit(jobDesc, checkMatch, uniqId)
                    + "@" + publicIP + ":" + sshPort + "#" + "<tosca_res_id>";
        } else {
            log.warn("Unsupported action: '" + action + "' in submit");
        }
        log.debug("submit (end)");
        return result;
    }
   
    @Override
    public StagingTransfer[] getInputStagingTransfer(String nativeJobId)
            throws PermissionDeniedException,
            TimeoutException,
            NoSuccessException {
        StagingTransfer[] result = null;
        log.debug("getInputStagingTransfer (begin)");
        String[] jobIdInfo = getInfoFromNativeJobId(nativeJobId);
        String _publicIP = jobIdInfo[1];
        int _sshPort = 22;
        try {
            Integer.parseInt(jobIdInfo[2]);
        } catch (NumberFormatException e) {
            log.warn("Unable to get integer SSH port value from jobId '" + nativeJobId + "';");
        }
        String _nativeJobId = jobIdInfo[3];
        try {
            sshControlAdaptor.setSecurityCredential(credential.getSSHCredential());
            sshControlAdaptor.connect(null, _publicIP, _sshPort, null, new HashMap());
            result = sshControlAdaptor.getInputStagingTransfer(_nativeJobId);
            log.debug("result: " + result);
        } catch (NotImplementedException ex) {
            throw new NoSuccessException(ex);
        } catch (AuthenticationFailedException ex) {
            throw new PermissionDeniedException(ex);
        } catch (AuthorizationFailedException ex) {
            throw new PermissionDeniedException(ex);
        } catch (BadParameterException ex) {
            throw new NoSuccessException(ex);
        }
        // View result
        for (StagingTransfer tr : result) {
            log.debug("From: '" + tr.getFrom() + "' to '" + tr.getTo() + "'");
        }
        log.debug("getInputStagingTransfer (end)");
        return sftp2tosca(result);
    }

    @Override
    public StagingTransfer[] getOutputStagingTransfer(String nativeJobId)
            throws PermissionDeniedException,
            TimeoutException,
            NoSuccessException {
        StagingTransfer[] result = null;
        log.debug("getOutputStagingTransfer (begin)");
        String[] jobIdInfo = getInfoFromNativeJobId(nativeJobId);
        String _publicIP = jobIdInfo[1];
        int _sshPort = 22;
        try {
            Integer.parseInt(jobIdInfo[2]);
        } catch (NumberFormatException e) {
            log.warn("Unable to get integer SSH port value from jobId '" + nativeJobId + "';");
        }
        String _nativeJobId = jobIdInfo[3];
        try {
            sshControlAdaptor.connect(null, _publicIP, _sshPort, null, new HashMap());
            result = sshControlAdaptor.getOutputStagingTransfer(_nativeJobId);
            log.debug("result: " + result);
        } catch (NotImplementedException ex) {
            throw new NoSuccessException(ex);
        } catch (AuthenticationFailedException ex) {
            throw new PermissionDeniedException(ex);
        } catch (AuthorizationFailedException ex) {
            throw new PermissionDeniedException(ex);
        } catch (BadParameterException ex) {
            throw new NoSuccessException(ex);
        }
        // View result
        for (StagingTransfer tr : result) {
            log.debug("From: '" + tr.getFrom() + "' to '" + tr.getTo() + "'");
        }
        log.debug("getOutputStagingTransfer (end)");
        return sftp2tosca(result);
    }

    private StagingTransfer[] sftp2tosca(StagingTransfer[] transfers) {
        int index = 0;
        StagingTransfer[] newTransfers = new StagingTransfer[transfers.length];

        log.debug("sftp2tosca");
        for (StagingTransfer tr : transfers) {
            log.debug("From: " + tr.getFrom() + " to " + tr.getTo());
            StagingTransfer newTr
                    = new StagingTransfer(
                            tr.getFrom().replace("sftp://", "tosca://"),
                            tr.getTo().replace("sftp://", "tosca://"),
                            tr.isAppend());

            newTransfers[index++] = newTr;
            log.debug("From: " + newTr.getFrom() + " to " + newTr.getTo());
        }
        return newTransfers;
    }

    @Override
    public String getStagingDirectory(String nativeJobId)
            throws PermissionDeniedException,
            TimeoutException,
            NoSuccessException {
        log.debug("getStagingDirectory (begin)");
        String result;
        String[] jobIdInfo = getInfoFromNativeJobId(nativeJobId);
        String _publicIP = jobIdInfo[1];
        int _sshPort = 22;
        try {
            Integer.parseInt(jobIdInfo[2]);
        } catch (NumberFormatException e) {
            log.warn("Unable to get integer SSH port value from jobId '" + nativeJobId + "';");
        }
        String _nativeJobId = jobIdInfo[3];
        try {
            sshControlAdaptor.connect(null, _publicIP, _sshPort, null, new HashMap());
            result = sshControlAdaptor.getStagingDirectory(_nativeJobId);
            log.debug("result: " + result);
        } catch (NotImplementedException ex) {
            throw new NoSuccessException(ex);
        } catch (AuthenticationFailedException ex) {
            throw new PermissionDeniedException(ex);
        } catch (AuthorizationFailedException ex) {
            throw new PermissionDeniedException(ex);
        } catch (BadParameterException ex) {
            throw new NoSuccessException(ex);
        }
        log.debug("getStagingDirectory (end)");
        return result;
    }

    @Override
    public JobMonitorAdaptor getDefaultJobMonitor() {
        return toscaJobMonitorAdaptor;
    }


    @Override
    public JobDescriptionTranslator getJobDescriptionTranslator()
            throws NoSuccessException {
        return sshControlAdaptor.getJobDescriptionTranslator();
    }

    @Override
    public Usage getUsage() {
        return new UAnd.Builder()
                .and(super.getUsage())
                .and(new U(USER_NAME))
                .and(new U(TOKEN))
                .build();
    }
}
