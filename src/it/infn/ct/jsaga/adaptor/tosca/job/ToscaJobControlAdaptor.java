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

import fr.in2p3.jsaga.adaptor.job.control.description.JobDescriptionTranslator;
import fr.in2p3.jsaga.adaptor.job.control.description.JobDescriptionTranslatorXSLT;
import fr.in2p3.jsaga.adaptor.job.control.advanced.CleanableJobAdaptor;
import fr.in2p3.jsaga.adaptor.job.control.staging.StagingJobAdaptorTwoPhase;
import fr.in2p3.jsaga.adaptor.job.control.staging.StagingTransfer;
import fr.in2p3.jsaga.adaptor.job.control.JobControlAdaptor;
import fr.in2p3.jsaga.adaptor.job.monitor.JobMonitorAdaptor;
import fr.in2p3.jsaga.adaptor.job.BadResource;
import fr.in2p3.jsaga.adaptor.ssh3.job.SSHJobControlAdaptor;

import java.util.logging.Level;
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
import java.net.InetAddress;
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
 * Authors: Giuseppe LA ROCCA, Riccardo BRUNO
 * Email:   <giuseppe.larocca, riccardo.bruno>@ct.infn.it
 * Ver.:    1.0.0
 * Date:    24 February 2016
 * *********************************************/
public class ToscaJobControlAdaptor extends ToscaAdaptorCommon
        implements JobControlAdaptor,
        StagingJobAdaptorTwoPhase,
        CleanableJobAdaptor {

    protected String toscaId = "unset";

    //protected static final String ATTRIBUTES_TITLE = "attributes_title";
    //protected static final String MIXIN_OS_TPL = "mixin_os_tpl";
    //protected static final String MIXIN_RESOURCE_TPL = "mixin_resource_tpl";
    //protected static final String PREFIX = "prefix";  
    // MAX tentatives before to gave up to connect the VM server.
    //private final int MAX_CONNECTIONS = 10;
    private static final Logger log
            = Logger.getLogger(ToscaJobControlAdaptor.class);

    private ToscaJobMonitorAdaptor toscaJobMonitorAdaptor
            = new ToscaJobMonitorAdaptor();

    private SSHJobControlAdaptor sshControlAdaptor
            = new SSHJobControlAdaptor();

    private String action = "";
    private String tosca_template = "";

    //private String resource = "";  
    //private String auth = "";
    //private String attributes_title = "";
    //private String mixin_os_tpl = "";
    //private String mixin_resource_tpl = "";  
    private URL endpoint;

    // Adding FedCloud Contextualisation options here
    //private String context_user_data = "";  
    //enum ACTION_TYPE { list, delete, describe, create; }
    //String[] IP = new String[2];
    /*
  public boolean testIpAddress(byte[] testAddress)
  {
    Inet4Address inet4Address;
    boolean result=false;

    try
    {
        inet4Address = (Inet4Address) InetAddress.getByAddress(testAddress);
        result = inet4Address.isSiteLocalAddress();
    }
    catch (UnknownHostException ex) { log.error(ex); }
    
    return result;
  }
     */
 /*    
  private List<String> run_OCCI (String action_type, String action)            
  {
      String line;
      Integer cmdExitCode;
      //List<String> list_rOCCI = new ArrayList();
      List<String> list_rOCCI = new ArrayList<String>();      
            
      try
      {            
        Process p = Runtime.getRuntime().exec(action);
        cmdExitCode = p.waitFor();
        log.info("EXIT CODE = " + cmdExitCode);

        if (cmdExitCode==0)
        {
            BufferedReader in = new BufferedReader(
                            new InputStreamReader(p.getInputStream()));

            ACTION_TYPE type = ACTION_TYPE.valueOf(action_type);
             
            while ((line = in.readLine()) != null) 
            {         
                // Skip blank lines.
                if (line.trim().length() > 0) {
                    
                switch (type) {
                    case list:
                        list_rOCCI.add(line.trim());
                        break;

                    case create:
                        list_rOCCI.add(line.trim());
                        log.info("");
                        log.info("A new OCCI computeID has been created:");
                        break;

                    case describe:
                        list_rOCCI.add(line.trim());
                        break;
                
                    case delete:
                        break;
                } // end switch
                } // end if
            } // end while

            in.close();
             
            if (action_type.equals("describe") || 
                action_type.equals("list") ||
                action_type.equals("delete")) 
                log.info("\n");         
                             
            for (int i = 0; i < list_rOCCI.size(); i++)         
                log.info(list_rOCCI.get(i));
        }
                                             
        } catch (InterruptedException ex) {
            java.util.logging.Logger.getLogger(ToscaJobControlAdaptor.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) { log.error(ex); }
        
        return list_rOCCI;
    }
     */
    @Override
    public void connect(String userInfo, String host, int port, String basePath, Map attributes)
            throws NotImplementedException,
            AuthenticationFailedException,
            AuthorizationFailedException,
            IncorrectURLException,
            BadParameterException,
            TimeoutException,
            NoSuccessException {
        log.debug("[Connect]" + LS
                + "userInfo  : '" + userInfo + "'" + LS
                + "host      : '" + host + "'" + LS
                + "port      : '" + port + "'" + LS
                + "basePath  : '" + basePath + "'" + LS
                + "attributes: '" + attributes + "'"
        );

        action = (String) attributes.get(ACTION);
        tosca_template = (String) attributes.get(TOSCA_TEMPLATE);

        try {
            endpoint = new URL("http", host, port, basePath);
        } catch (MalformedURLException ex) {
            log.error("Error in the service end-point creation" + ex);
        }
        log.debug("action:" + action);
        log.debug("tosca_template: " + tosca_template);

        sshControlAdaptor.setSecurityCredential(credential.getSSHCredential());
    }

    @Override
    public void start(String nativeJobId) throws PermissionDeniedException,
            TimeoutException,
            NoSuccessException {
        /*
        String _publicIP = 
                nativeJobId.substring(nativeJobId.indexOf("@")+1, 
                                      nativeJobId.indexOf("#"));
        
        String _nativeJobId = nativeJobId.substring(0, nativeJobId.indexOf("@"));
        
        //try {                        
        //    sshControlAdaptor.connect(null, _publicIP, 22, null, new HashMap());            
        //    sshControlAdaptor.start(_nativeJobId);                         
        //    
        //} catch (NotImplementedException ex) { throw new NoSuccessException(ex); } 
        //  catch (AuthenticationFailedException ex) { throw new PermissionDeniedException(ex); } 
        //  catch (AuthorizationFailedException ex) { throw new PermissionDeniedException(ex); } 
        //  catch (BadParameterException ex) { throw new NoSuccessException(ex); }
         */
    }

    @Override
    public void cancel(String nativeJobId) throws PermissionDeniedException,
            TimeoutException,
            NoSuccessException {
        log.debug("NativeJobId: " + nativeJobId);
        String _publicIP = nativeJobId.substring(nativeJobId.indexOf("@") + 1,
                nativeJobId.indexOf("#"));

        String _nativeJobId = nativeJobId.substring(0, nativeJobId.indexOf("@"));

        //try {                        
        //    sshControlAdaptor.connect(null, _publicIP, 22, null, new HashMap());            
        //    sshControlAdaptor.cancel(_nativeJobId);
        //} catch (NotImplementedException ex) { throw new NoSuccessException(ex); } 
        //  catch (AuthenticationFailedException ex) { throw new PermissionDeniedException(ex); } 
        //  catch (AuthorizationFailedException ex) { throw new PermissionDeniedException(ex); } 
        //  catch (BadParameterException ex) { throw new NoSuccessException(ex); }
        log.info("Calling the cancel() method");
    }

    @Override
    public void clean(String nativeJobId) throws PermissionDeniedException,
            TimeoutException,
            NoSuccessException {
        //List<String> results = new ArrayList();
        /*
        List<String> results = new ArrayList<String>();
        
        String _publicIP = nativeJobId.substring(nativeJobId.indexOf("@")+1, 
                                                 nativeJobId.indexOf("#"));
        
        String _nativeJobId = nativeJobId.substring(0, nativeJobId.indexOf("@"));
        String _resourceId = nativeJobId.substring(nativeJobId.indexOf("#")+1);
        
        String Execute = prefix +
                         "occi --endpoint " + Endpoint +
                         " --action " + "delete" +
                         " --resource " + "compute" +
                         " --resource " + _resourceId +
                         " --auth " + auth +
                         " --user-cred " + user_cred +                         
                         " --voms --ca-path " + ca_path;
                         //" --context user_data=" + context_user_data;
                
        log.info("");
        log.info("Stopping the VM [ " + _publicIP + " ] in progress...");
                
        log.info(Execute);        
         */
        //try {            
        //    sshControlAdaptor.connect(null, _publicIP, 22, null, new HashMap());            
        //    sshControlAdaptor.clean(_nativeJobId);
        //    
        //    // Stopping the VM Server
        //    results = run_OCCI("delete", Execute);            
        //    
        //} catch (NotImplementedException ex) { throw new NoSuccessException(ex); } 
        //  catch (AuthenticationFailedException ex) { throw new PermissionDeniedException(ex); } 
        //  catch (AuthorizationFailedException ex) { throw new PermissionDeniedException(ex); } 
        //  catch (BadParameterException ex) { throw new NoSuccessException(ex); }
    }

    /*
    public String getIP (List<String> results)
    {
        String publicIP = null;
        String tmp  = "";
        int k=0;
        boolean check = false;
        
        // Extracting IPs                         
        for (int i = 0; i < results.size() && !check;  i++) 
        {            
            if ((results.get(i)).contains("\"address\"")) 
            {
                // Stripping quote and blank chars
                IP[k] = results.get(i).substring(12,results.get(i).length()-1);
                
                Pattern patternID = 
                    Pattern.compile("^(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})$");
                    //Pattern.compile("(\\d{1,3}.)(\\d{1,3}.)(\\d{1,3}.)(\\d{1,3}.)");
                                                                                                         
                tmp = IP[k];
                                   
                Matcher matcher = patternID.matcher(IP[k]);
                                   
                while (matcher.find()) 
                {
                    String _IP0 = 
                        matcher.group(1).replace(".","");
                                       
                    String _IP1 = 
                        matcher.group(2).replace(".","");
                                       
                    String _IP2 = 
                        matcher.group(3).replace(".","");
                                       
                    String _IP3 = 
                        matcher.group(4).replace(".","");
                                                                      
                     //CHECK if IP[k] is PRIVATE or PUBLIC
                     byte[] rawAddress = { 
                        (byte) Integer.parseInt(_IP0),
                        (byte) Integer.parseInt(_IP1),
                        (byte) Integer.parseInt(_IP2),
                        (byte) Integer.parseInt(_IP3)
                     };
                                   
                     if (!testIpAddress(rawAddress)) {
                        // Saving the public IP
                        publicIP = tmp;
                        check = true;
                     }
                                   
                     k++;
                } // while
            }  // if
        } // end for
        
        return publicIP;
    }
     */
 /*
    public boolean isNullOrEmpty(String myString)
    {
         return myString == null || "".equals(myString);
    }
     */
    
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
    public String submit(String jobDesc, boolean checkMatch, String uniqId)
            throws PermissionDeniedException,
            TimeoutException,
            NoSuccessException,
            BadResource {
        log.debug("action:" + action);
        log.debug("tosca_template:" + tosca_template);
        String resourceID = "";
        String publicIP = "";
        String result = "";

        //List<String> results = new ArrayList();
        List<String> results = new ArrayList<String>();

//        if (action.equals("create")) {

            log.info("Creating a new TOSCA Infra. Please wait! ");

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
            
/*

            log.info("");

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }

            // Getting info about the VM
            if (results.size() > 0) {
                resourceID = results.get(0);

                int k = 0;
                boolean check = false;

                try {
                    while (!check) {
                        log.info("");
                        log.info("See below the details of the VM ");
                        log.info("[ " + resourceID + " ]");
                        log.info("");

                        Execute = prefix
                                + "occi --endpoint " + Endpoint
                                + " --action " + "describe"
                                + " --resource " + resource
                                + " --resource " + resourceID
                                + " --auth " + auth
                                + " --user-cred " + user_cred
                                + " --voms --ca-path " + ca_path
                                + //" --context user_data=" + context_user_data +                                    
                                " --output-format json_extended_pretty";

                        log.info(Execute);

                        results = run_OCCI("describe", Execute);

                        publicIP = getIP(results);
                        if (!isNullOrEmpty(publicIP)) {
                            check = true;
                        }
                    } // end while
                } catch (Exception ex) {
                    log.error(ex);
                }

                //sshControlAdaptor.setSecurityCredential(credential.getSSHCredential());
                log.info("");
                log.info("Starting VM [ " + publicIP + " ] in progress...");

                Date date = new Date();
                SimpleDateFormat ft
                        = new SimpleDateFormat("E yyyy.MM.dd 'at' hh:mm:ss a zzz");

                log.info("");
                log.info("Waiting the remote VM finishes the boot! Sleeping for a while... ");
                log.info(ft.format(date));

                byte[] buff = new byte[1024];
                int ret_read = 0;
                boolean flag = true;
                int MAX = 0;

                TelnetClient tc = null;

                while ((flag) && (MAX < MAX_CONNECTIONS)) {
                    try {
                        tc = new TelnetClient();
                        tc.connect(publicIP, 22);
                        InputStream instr = tc.getInputStream();

                        ret_read = instr.read(buff);
                        if (ret_read > 0) {
                            log.info("[ SUCCESS ] ");
                            tc.disconnect();
                            flag = false;
                        }
                    } catch (IOException e) {

                        try {
                            Thread.sleep(60000);
                        } catch (InterruptedException ex) {
                        }

                        MAX++;
                    }
                }

                date = new Date();
                log.info(ft.format(date));

                toscaJobMonitorAdaptor.setSSHHost(publicIP);

                //try {            
                //    sshControlAdaptor.connect(null, publicIP, 22, null, new HashMap());            
                //} catch (NotImplementedException ex) { throw new NoSuccessException(ex); } 
                //  catch (AuthenticationFailedException ex) { throw new PermissionDeniedException(ex); } 
                //  catch (AuthorizationFailedException ex) { throw new PermissionDeniedException(ex); } 
                //  catch (BadParameterException ex) { throw new NoSuccessException(ex); }
                //result = sshControlAdaptor.submit(jobDesc, checkMatch, uniqId) 
                //    + "@" + publicIP + "#" + resourceID;
            }
        } // end creating

        //else return null;
        return result;
*/
 return "OK";
    }

    @Override
    public StagingTransfer[] getInputStagingTransfer(String nativeJobId)
            throws PermissionDeniedException,
            TimeoutException,
            NoSuccessException {
        StagingTransfer[] result = null;
        /*
        String _publicIP = nativeJobId.substring(nativeJobId.indexOf("@")+1, 
                                                 nativeJobId.indexOf("#"));
        
        String _nativeJobId = nativeJobId.substring(0, nativeJobId.indexOf("@"));
        
        //try {            	
	//    sshControlAdaptor.setSecurityCredential(credential.getSSHCredential());
        //    sshControlAdaptor.connect(null, _publicIP, 22, null, new HashMap());
        //    result = sshControlAdaptor.getInputStagingTransfer(_nativeJobId);
        //                
        //} catch (NotImplementedException ex) { throw new NoSuccessException(ex); } 
        //  catch (AuthenticationFailedException ex) { throw new PermissionDeniedException(ex); } 
        //  catch (AuthorizationFailedException ex) { throw new PermissionDeniedException(ex); } 
        //  catch (BadParameterException ex) { throw new NoSuccessException(ex); }
        //     
        // change URL sftp:// tp rocci://        
        return sftp2rocci(result);        
         */
        return result;
    }

    @Override
    public StagingTransfer[] getOutputStagingTransfer(String nativeJobId)
            throws PermissionDeniedException,
            TimeoutException,
            NoSuccessException {

        StagingTransfer[] result = null;
        /*
        String _publicIP = nativeJobId.substring(nativeJobId.indexOf("@")+1, 
                                                 nativeJobId.indexOf("#"));
        
        String _nativeJobId = nativeJobId.substring(0, nativeJobId.indexOf("@"));
        
        //try {            
        //    sshControlAdaptor.connect(null, _publicIP, 22, null, new HashMap());            
        //    result = sshControlAdaptor.getOutputStagingTransfer(_nativeJobId);
        //} catch (NotImplementedException ex) { throw new NoSuccessException(ex); } 
        //  catch (AuthenticationFailedException ex) { throw new PermissionDeniedException(ex); } 
        //  catch (AuthorizationFailedException ex) { throw new PermissionDeniedException(ex); } 
        //  catch (BadParameterException ex) { throw new NoSuccessException(ex); }
                        
        // change URL sftp:// tp rocci://
        return sftp2rocci(result);
         */
        return result;
    }

    /*
    private StagingTransfer[] sftp2rocci(StagingTransfer[] transfers) 
    {
        int index=0;
        StagingTransfer[] newTransfers = new StagingTransfer[transfers.length];
        
        for (StagingTransfer tr: transfers) 
        {
            StagingTransfer newTr = 
                    new StagingTransfer(
                        tr.getFrom().replace("sftp://", "rocci://"),
                        tr.getTo().replace("sftp://", "rocci://"),
                        tr.isAppend());
                
            newTransfers[index++] = newTr;
        }
        
        return newTransfers;
    }
     */
    @Override
    public String getStagingDirectory(String nativeJobId)
            throws PermissionDeniedException,
            TimeoutException,
            NoSuccessException {
        String result = null;
        /*
        String _publicIP = nativeJobId.substring(nativeJobId.indexOf("@")+1, 
                                                 nativeJobId.indexOf("#"));
        
        String _nativeJobId = nativeJobId.substring(0, nativeJobId.indexOf("@"));
        
        //try {            
        //    sshControlAdaptor.connect(null, _publicIP, 22, null, new HashMap());            
        //    result = sshControlAdaptor.getStagingDirectory(_nativeJobId);
        //} catch (NotImplementedException ex) { throw new NoSuccessException(ex); } 
        //  catch (AuthenticationFailedException ex) { throw new PermissionDeniedException(ex); } 
        //  catch (AuthorizationFailedException ex) { throw new PermissionDeniedException(ex); } 
        //  catch (BadParameterException ex) { throw new NoSuccessException(ex); }
         */
        return result;
    }

    @Override
    public JobMonitorAdaptor getDefaultJobMonitor() {
        return toscaJobMonitorAdaptor;
    }

    protected String getJobDescriptionTranslatorFilename() throws NoSuccessException {
        return "xsl/job/bes-jsdl.xsl";
    }

    @Override
    public JobDescriptionTranslator getJobDescriptionTranslator() throws NoSuccessException {
        return new JobDescriptionTranslatorXSLT(getJobDescriptionTranslatorFilename());
    }

    @Override
    public Usage getUsage() {
        return new UAnd.Builder()
                .and(super.getUsage())
                .and(new U(USER_NAME))
                .and(new U(TOKEN))
                //.and(new U(MIXIN_RESOURCE_TPL))
                //.and(new UOptional(PREFIX))
                .build();
    }
}
