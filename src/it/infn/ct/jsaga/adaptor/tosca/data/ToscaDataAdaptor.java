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
package it.infn.ct.jsaga.adaptor.tosca.data;

import fr.in2p3.jsaga.adaptor.ssh3.data.SFTPDataAdaptor;
import it.infn.ct.jsaga.adaptor.tosca.security.ToscaSecurityCredential;
import org.ogf.saga.error.AuthenticationFailedException;
import org.ogf.saga.error.AuthorizationFailedException;
import org.ogf.saga.error.BadParameterException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.error.TimeoutException;
import ch.ethz.ssh2.Connection;
import fr.in2p3.jsaga.adaptor.security.impl.UserPassSecurityCredential;
import it.infn.ct.jsaga.adaptor.tosca.job.ToscaJobControlAdaptor;
import java.util.Map;
import org.apache.log4j.Logger;
import org.ogf.saga.context.Context;

/* ***************************************************
 * *** Centre de Calcul de l'IN2P3 - Lyon (France) ***
 * ***             http://cc.in2p3.fr/             ***
 * ***************************************************
 * File:   ToscaDataAdaptor
 * Author: Lionel Schwarz (lionel.schwarz@in2p3.fr)
 * Date:   22 oct 2013
 * ***************************************************/
public class ToscaDataAdaptor extends SFTPDataAdaptor {
    
    private static final Logger log
            = Logger.getLogger(ToscaDataAdaptor.class);

    @Override
    public String getType() {
        return "tosca";
    }

    @Override
    public Class[] getSupportedSecurityCredentialClasses() {
        return new Class[]{ToscaSecurityCredential.class};
    }

    @Override
    public void connect(String userInfo, String host,
            int port, String basePath, Map attributes)
            throws NotImplementedException, AuthenticationFailedException,
            AuthorizationFailedException, BadParameterException,
            TimeoutException, NoSuccessException { 
        log.debug("ToscaDataAdaptor connect()");
        log.debug("userInfo: "+userInfo);
        log.debug("host: "+host);
        log.debug("port: "+port);
        log.debug("basePath: "+basePath);
        log.debug("attributes: "+attributes);
                
        String ssh_username = ((ToscaSecurityCredential) credential).getUsername();                       
        log.debug("sshUserId: "+ssh_username);
                
        String ssh_password = ((ToscaSecurityCredential) credential).getPassword();                        
        log.debug("sshPassword: "+ssh_password);
        
        setSecurityCredential(new UserPassSecurityCredential(ssh_username, ssh_password));
        //setSecurityCredential(new UserPassSecurityCredential("jobtest", "Xvf56jZ751f"));
        super.connect(userInfo,host,port,basePath,attributes);                        
    }
}
