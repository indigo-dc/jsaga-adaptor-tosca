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
import fr.in2p3.jsaga.adaptor.base.usage.Usage;
import fr.in2p3.jsaga.adaptor.job.control.advanced.CleanableJobAdaptor;
import fr.in2p3.jsaga.adaptor.job.control.staging.StagingJobAdaptorTwoPhase;
import fr.in2p3.jsaga.adaptor.job.control.staging.StagingTransfer;
import fr.in2p3.jsaga.adaptor.job.control.JobControlAdaptor;
import fr.in2p3.jsaga.adaptor.job.monitor.JobMonitorAdaptor;
import fr.in2p3.jsaga.adaptor.job.BadResource;
import fr.in2p3.jsaga.adaptor.job.control.description.JobDescriptionTranslator;
import fr.in2p3.jsaga.adaptor.security.impl.SSHSecurityCredential;
import fr.in2p3.jsaga.adaptor.security.impl.UserPassSecurityCredential;
import fr.in2p3.jsaga.adaptor.ssh3.job.SSHJobControlAdaptor;
import fr.in2p3.jsaga.adaptor.ssh3.security.SSHSecurityAdaptor;
import it.infn.ct.jsaga.adaptor.tosca.security.ToscaSecurityCredential;
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
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.ogf.saga.context.Context;
import org.ogf.saga.context.ContextFactory;

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
    
    private static final Logger log
            = Logger.getLogger(ToscaJobControlAdaptor.class);

    private ToscaJobMonitorAdaptor toscaJobMonitorAdaptor
            = new ToscaJobMonitorAdaptor();

    private SSHJobControlAdaptor sshControlAdaptor
            = new SSHJobControlAdaptor();

    protected String tosca_id = null;
    private String action = "";
    private String tosca_template = "";
    private URL endpoint = null;
    private String tosca_UUID = null;
    

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

        tosca_template = (String) attributes.get(TOSCA_TEMPLATE);

        // View parameters
        log.debug("userInfo      : '" + userInfo + "'" + LS
                + "host          : '" + host + "'" + LS
                + "port          : '" + port + "'" + LS
                + "basePath      : '" + basePath + "'" + LS
                + "attributes    : '" + attributes + "'" + LS
                + "action        : '" + action + "'" + LS
                + "tosca_template: '" + tosca_template + "'");

        try {
            endpoint = new URL("http", host, port, basePath);
        } catch (MalformedURLException ex) {
            log.error("Error in the service end-point creation" + ex);
            throw new BadParameterException(ex);
        }
        log.debug("action:" + action);
        log.debug("tosca_template: " + tosca_template);
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
            sshControlAdaptor.setSecurityCredential(
                    new UserPassSecurityCredential(ssh_username, ssh_password)
            );
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
            sshControlAdaptor.setSecurityCredential(
                    new UserPassSecurityCredential(ssh_username, ssh_password)
            );
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
            sshControlAdaptor.setSecurityCredential(
                    new UserPassSecurityCredential(ssh_username, ssh_password)
            );
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
     * @return [0] Native JobId, [1] ssh_publicIP [2] ssh_port [3] SSH jobId
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
    
    /**
     * Read values from the json.
     * 
     * @param json The json from where to 
     * @param key The element to return. It can retrieve nested elements providing the full chain as &lt;element&gt;.&lt;element&gt;.&lt;element&gt;
     * @return The element value
     * @throws ParseException If the json cannot be parsed
     */
    private String getDocumentValue(String json, String key) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(json);
        String keyelement[]=key.split("\\.");
        for(int i=0; i<(keyelement.length-1); i++){
            jsonObject = (JSONObject)jsonObject.get(keyelement[i]);
        }
        return (String) jsonObject.get(keyelement[keyelement.length-1]);
    } 
    
    private String getToscaDeployment(String toscaUUID) {    
        StringBuilder deployment = new StringBuilder();
        HttpURLConnection conn;
        try {
            URL deploymentEndpoint = new URL(endpoint.toString() + "/" + toscaUUID);
            conn = (HttpURLConnection) deploymentEndpoint.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("charset", "utf-8");
            log.debug("Orchestrator status code: " + conn.getResponseCode());
            log.debug("Orchestrator status message: " + conn.getResponseMessage());
            if (conn.getResponseCode() == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));                
                String ln;
                while ((ln = br.readLine()) != null) {
                    deployment.append(ln);
                }
                log.debug("Orchestrator result: " + deployment);
            }
        } catch (IOException ex) {
            log.error("Connection error with the service at " + endpoint.toString());
            log.error(ex);        
        }
        return deployment.toString();
    }
    
    private String submitTosca() 
        throws IOException,
               ParseException,
               BadResource,
               NoSuccessException {   
        StringBuilder orchestrator_result=new StringBuilder("");
        StringBuilder postData = new StringBuilder();
        postData.append("{ \"template\": \"");
        String tosca_template_content="";
        try {
            tosca_template_content = new String(Files.readAllBytes(Paths.get(tosca_template))).replace("\n", "\\n"); 
            postData.append(tosca_template_content);
        } catch (IOException ex) {
            log.error("Template '"+tosca_template+"'is not readable");
            throw new BadResource("Template '"+tosca_template+"'is not readable; template:" +LS
                                 +"'"+tosca_template_content+"'"
            );
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
                orchestrator_result = new StringBuilder();
                String ln;
                while ((ln = br.readLine()) != null) {
                    orchestrator_result.append(ln);
                }
                
                log.debug("Orchestrator result: " + orchestrator_result);
                String orchestratorDoc = orchestrator_result.toString();
                tosca_UUID = getDocumentValue(orchestratorDoc,"uuid");
                log.debug("Created resource has UUID: '"+tosca_UUID+"'");
                return orchestratorDoc;
                
            }
        } catch (IOException ex) {
            log.error("Connection error with the service at " + endpoint.toString());
            log.error(ex);
            throw new NoSuccessException("Connection error with the service at " + endpoint.toString());
        } catch (ParseException ex) {
            log.error("Orchestrator response not parsable");
            throw new NoSuccessException("Orchestrator response not parsable:"+LS
                                        +"'"+orchestrator_result.toString()+"'");
        }
        return tosca_UUID;
    }
    
    private void waitToscaResource() 
            throws NoSuccessException,
                   BadResource,
                   TimeoutException {
         int attempts = 0;
         int max_attempts = 12;
         int wait_step= 10000;
         String toscaStatus = "CREATE_IN_PROGRESS";
         for(attempts=0;
             tosca_UUID != null 
          && attempts < max_attempts-1 
          && toscaStatus.equals("CREATE_IN_PROGRESS");
             attempts++) {
             try {
                 log.debug("Waiting ("+wait_step+"ms) for resource creation; attempt: "+(attempts+1)+"/"+max_attempts+" ...");
                 Thread.sleep(wait_step);
             } catch (InterruptedException e1) {
                 // TODO Auto-generated catch block
                 e1.printStackTrace();
             }
             String toscaDeployment = getToscaDeployment(tosca_UUID);
             try {
                 log.debug("Deployment: " + toscaDeployment);
                 toscaStatus = getDocumentValue(toscaDeployment, "status");
                 log.debug("Deplyment " + tosca_UUID + " has status " + toscaStatus);
             } catch (ParseException ex) {
                 log.warn("Impossible to parse the tosca deployment json: '"+toscaDeployment+"'");
             }
         }
         if(!toscaStatus.equals("CREATE_COMPLETE ")) {
             log.debug("Deployments error for "+ tosca_UUID+" with status "+toscaStatus+". Attempts "+attempts+"/" + max_attempts);
             if(attempts >= max_attempts)
                 throw new TimeoutException("Reached timeout while waiting for resource");
             else
                throw new NoSuccessException("Deployment error.");
         }
    }
        
    /**
     * Free all allocated resources
     */
    private void releaseResources() {
        if(tosca_UUID != null) {
            log.debug("Releasing Tosca resource '"+tosca_UUID+"'");
        }
        if(tosca_id != null) {
            log.debug("Releasing SSH resource '"+tosca_id+"'");
        }
    }
        
    @Override
    public String submit(String jobDesc, boolean checkMatch, String uniqId)
            throws PermissionDeniedException,
            TimeoutException,
            NoSuccessException,
            BadResource {
        
        log.debug("submit (begin)");
        log.debug("action:" + action);
        log.debug("jobDesc:" + jobDesc);
        log.debug("checkMatch:" + checkMatch);
        log.debug("uniqId:" + uniqId);
        String result = "";
        String ssh_publicIP = "127.0.0.1";
        int ssh_port = 22;
      //String ssh_username;
      //String ssh_password;
        
        // SUbmit works in two stages; first create the Tosca resource
        // from the given toca_template, then submit the job to an 
        // SSH instance belonging to the Tosca resources
        try {
            log.info("Creating a new tosca resource, please wait ...");            
            log.debug("tosca_template:" + tosca_template);

            // Create Tosca resource form tosca_template, then wait
            // for its creation and determine an access point with SSH:
            // IP/Port and credentials (username, PublicKey and PrivateKey)
            String doc = submitTosca();

            // Now waits until the resource is available
            // A maximum number of attempts will be done
            // until the resource will be made available
            waitToscaResource();

            // Once tosca resource is ready, submit to SSH
            // String ssh_publicIP = ToscaResults[0];
            // String ssh_port = toscaResults[1];
            ssh_publicIP = getDocumentValue(doc, "outputs.node_ip");
            ssh_port = 22;
	    String creds = getDocumentValue(doc, "outputs.node_creds");
	    creds = creds.substring(1, creds.length()-1);
	    String sCreds[] = creds.split(",");
	    if(sCreds[0].startsWith("password")){
              ssh_password=sCreds[0].split("=")[1].trim();
              ssh_username=sCreds[1].split("=")[1].trim();
            } else {
		ssh_password=sCreds[1].split("=")[1].trim();
              ssh_username=sCreds[0].split("=")[1].trim();
	    }
            credential.setUsername(ssh_username);
            credential.setPassword(ssh_password);            
            sshControlAdaptor.setSecurityCredential(
                    new UserPassSecurityCredential(ssh_username, ssh_password)
            );
            sshControlAdaptor.connect(null, ssh_publicIP, ssh_port, null, new HashMap());
        } catch (NotImplementedException ex) {
            releaseResources();
            throw new NoSuccessException(ex);
        } catch (AuthenticationFailedException ex) {
            releaseResources();
            throw new PermissionDeniedException(ex);
        } catch (AuthorizationFailedException ex) {
            releaseResources();
            throw new PermissionDeniedException(ex);
        } catch (BadParameterException ex) {
            releaseResources();
            throw new NoSuccessException(ex);
        } catch (Exception ex) {
            releaseResources();
            throw new NoSuccessException(ex);
        }
        result = sshControlAdaptor.submit(jobDesc, checkMatch, uniqId)
                + "@" + ssh_publicIP + ":" + ssh_port + "#" + tosca_UUID;
        log.debug("submit (end)");
        log.debug("JobId: '"+result+"'");
        this.tosca_id = result;
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
            sshControlAdaptor.setSecurityCredential(
                    new UserPassSecurityCredential(ssh_username, ssh_password)
            );
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
            sshControlAdaptor.setSecurityCredential(
                    new UserPassSecurityCredential(ssh_username, ssh_password)
            );
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
            sshControlAdaptor.setSecurityCredential(
                    new UserPassSecurityCredential(ssh_username, ssh_password)
            );
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
