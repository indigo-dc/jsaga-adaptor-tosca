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
package it.infn.ct.jsaga.adaptor.tosca;

import fr.in2p3.jsaga.adaptor.ClientAdaptor;
import fr.in2p3.jsaga.adaptor.base.defaults.Default;
import fr.in2p3.jsaga.adaptor.base.usage.UAnd;
import fr.in2p3.jsaga.adaptor.base.usage.Usage;
import fr.in2p3.jsaga.adaptor.security.SecurityCredential;

import org.ogf.saga.error.*;

import it.infn.ct.jsaga.adaptor.tosca.security.ToscaSecurityCredential;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import java.util.Map;
import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/* *********************************************
 * *** Istituto Nazionale di Fisica Nucleare ***
 * ***      Sezione di Catania (Italy)       ***
 * ***        http://www.ct.infn.it/         ***
 * *********************************************
 * File:    rOCCIJobControlAdaptor.java
 * Authors: Giuseppe LA ROCCA, Riccardo BRUNO
 * Email:   <giuseppe.larocca, riccardo.bruno>@ct.infn.it
 * Ver.:    1.0.0
 * Date:    24 February 2016
 * *********************************************/
public class ToscaAdaptorCommon extends Object implements ClientAdaptor {

    protected ToscaSecurityCredential credential = null;

    protected String sshHost = null;
    protected static final String AUTH = "auth";
    protected static final String RESOURCE = "compute";
    protected static final String USER_NAME = "user.name";
    protected static final String TOKEN = "token";
    protected static final String TOSCA_TEMPLATE = "tosca_template";
    protected static final String TOSCA_WAITMS = "wait_ms";
    protected static final String TOSCA_MAXWAITS = "max_waits";

    public static final String LS = System.getProperty("line.separator");
    private static final Logger log
            = Logger.getLogger(ToscaAdaptorCommon.class);

    // TOSCA data
    protected String toscaHost = "unset";
    protected int toscaPort = 80; // Default port is HTTP
    protected String notfyEndpointHost = "unset";
    protected int notfyEndpointPort = 8888; // Default port is 8888 ApiServerDaemon dev. port
    protected String tosca_UUID = null;
    protected String ssh_username = "";
    protected String ssh_password = "";
    protected URL endpoint = null;

    @Override
    public Class[] getSupportedSecurityCredentialClasses() {
        return new Class[]{
            ToscaSecurityCredential.class
        };
    }

    @Override
    public void setSecurityCredential(SecurityCredential sc) {
        credential = (ToscaSecurityCredential) sc;
        credential.setUsername(ssh_username);
        credential.setPassword(ssh_password);

        try {
            log.debug("No security is necessary yet" + LS
                    + "User: '" + credential.getUserID() + "'" + LS
                    + "ssh_username: '" + ssh_username + "'" + LS
                    + "ssh_password: '" + ssh_password + "'" + LS
            );
            log.debug("TOKEN:" + sc.getAttribute("token"));
        } catch (NotImplementedException e) {
            log.debug("NotImplementedException: " + LS + e.toString());
        } catch (NoSuccessException e) {
            log.debug("NoSuccessException: " + LS + e.toString());
        } catch (Exception e) {
            log.debug("Exception: " + LS + e.toString());
        }
    }

    @Override
    public String getType() {
        return "tosca";
    }

    @Override
    public int getDefaultPort() {
        return toscaPort;
    }

    @Override
    public void connect(String userInfo, String host, int port, String basePath, Map attributes)
            throws NotImplementedException,
            AuthenticationFailedException,
            AuthorizationFailedException,
            IncorrectURLException,
            BadParameterException,
            TimeoutException,
            NoSuccessException {
        log.debug("ToscaAdaptorCommon: connect()");
        log.debug("userInfo: " + userInfo);
        log.debug("host: " + host);
        log.debug("port: " + port);
        log.debug("basePath: " + basePath);
        log.debug("attributes: " + attributes);

        try {
            endpoint = new URL("http", host, port, basePath);
        } catch (MalformedURLException ex) {
            log.error("Error in the service end-point creation" + ex);
            throw new BadParameterException(ex);
        }
    }

    @Override
    public void disconnect() throws NoSuccessException {
    }

    @Override
    public Usage getUsage() {
        return new UAnd.Builder()
                //.and(new U(AUTH))
                .build();
    }

    @Override
    public Default[] getDefaults(Map map) throws IncorrectStateException {
        return new Default[]{ //new Default (AUTH, "x509"),        
        };
    }
    
    /**
     * Retrieve information included in the Tosca JobId
     *
     * @param nativeJobId <ssh_jobId>#<toscaUUID>
     * @return [0] Native JobId, [1] ssh_publicIP [2] ssh_port [3] SSH jobId
     */
    protected String[] getInfoFromNativeJobId(String nativeJobId) {
        
        log.debug("nativeJobId: " + nativeJobId);
        String sshJobId = nativeJobId.substring(0, nativeJobId.indexOf("#"));
        String tosca_UUID = nativeJobId.substring(nativeJobId.indexOf("#") + 1);        
        log.debug("ssh_JobId: '"+sshJobId+"'");
        log.debug("tosca_UUID: '"+tosca_UUID+"'");
        
        // Extract info from tosca_UUID
        String doc = getToscaDeployment(tosca_UUID);        
        String[] sshCredentials={"","","",""};
        try {
            sshCredentials = getToscaResourceCredentials(doc);
            for(int i=0; i<sshCredentials.length; i++)
                log.debug(sshCredentials[i]);
        } catch (ParseException ex) {
            log.error("Unable to get credentials from '"+doc+"'");
        }
        String ssh_publicIP = sshCredentials[0];
        String ssh_port     = sshCredentials[1];
        ssh_username        = sshCredentials[2];
        ssh_password        = sshCredentials[3];
        
        log.debug("_publicIP: " + ssh_publicIP);
        log.debug("_sshPort: " + ssh_port);
        log.debug("ssh_username: " + ssh_username);
        log.debug("ssh_password: " + ssh_password);

        String[] info = { nativeJobId
                        , sshJobId
                        , tosca_UUID
                        , ssh_publicIP
                        , ssh_port
                        , ssh_username
                        , ssh_password };
        return info;
    }

    protected String getToscaDeployment(String toscaUUID) {
        StringBuilder deployment = new StringBuilder();
        HttpURLConnection conn;
        try {
            log.debug("endpoint: '" + endpoint + "'");
            URL deploymentEndpoint = new URL(endpoint.toString() + "/" + toscaUUID);
            log.debug("deploymentEndpoint: '" + deploymentEndpoint + "'");
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

    protected void deleteToscaDeployment(String toscaUUID) {
        StringBuilder deployment = new StringBuilder();
        HttpURLConnection conn;
        try {
            URL deploymentEndpoint = new URL(endpoint.toString() + "/" + toscaUUID);
            conn = (HttpURLConnection) deploymentEndpoint.openConnection();
            conn.setRequestMethod("DELETE");
            log.debug("Orchestrator status code: " + conn.getResponseCode());
            log.debug("Orchestrator status message: " + conn.getResponseMessage());
            if (conn.getResponseCode() == 204) {
                log.debug("Successfully removed resource: '" + toscaUUID + "'");
            } else {
                log.error("Unable to remove resource: '" + toscaUUID + "'");
            }
        } catch (IOException ex) {
            log.error("Connection error with the service at " + endpoint.toString());
            log.error(ex);
        }
    }

    /**
     * Read values from the json.
     *
     * @param json The json from where to
     * @param key The element to return. It can retrieve nested elements
     * providing the full chain as
     * &lt;element&gt;.&lt;element&gt;.&lt;element&gt;
     * @return The element value
     * @throws ParseException If the json cannot be parsed
     */
    protected String getDocumentValue(String json, String key) throws ParseException {
        JSONParser parser = new JSONParser();
        JSONObject jsonObject = (JSONObject) parser.parse(json);
        String keyelement[] = key.split("\\.");
        for (int i = 0; i < (keyelement.length - 1); i++) {
            jsonObject = (JSONObject) jsonObject.get(keyelement[i]);
        }
        return (String) jsonObject.get(keyelement[keyelement.length - 1]);
    }

    protected String[] getToscaResourceCredentials(String doc) throws ParseException {

        log.debug("Extracting credentials from doc: '" + doc + "'");
        String ssh_publicIP = getDocumentValue(doc, "outputs.node_ip");
        log.debug("IP: '" + ssh_publicIP + "'");
        int ssh_port = 22; // Not yet available, maybe in next versions
        String creds = getDocumentValue(doc, "outputs.node_creds");
        log.debug("creds: '" + creds + "'");
        creds = creds.substring(1, creds.length() - 1);        
        String sCreds[] = creds.split(",");
        if (sCreds[0].startsWith("password")) {
            ssh_password = sCreds[0].split("=")[1].trim();
            ssh_username = sCreds[1].split("=")[1].trim();
        } else {
            ssh_password = sCreds[1].split("=")[1].trim();
            ssh_username = sCreds[0].split("=")[1].trim();
        }
        log.debug("ssh_username: '"+ssh_username+"'");
        log.debug("ssh_password: '"+ssh_password+"'");
        
        String[] credentials = {ssh_publicIP, "" + ssh_port, ssh_username, ssh_password};        
        return credentials;
    }
}
