/**************************************************************************
Copyright (c) 2011:
Istituto Nazionale di Fisica Nucleare (INFN), Italy
Consorzio COMETA (COMETA), Italy

See http://www.infn.it and and http://www.consorzio-cometa.it for details on
the copyright holders.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

@author <a href="mailto:riccardo.bruno@ct.infn.it">Riccardo Bruno</a>(INFN)
****************************************************************************/
package it.infn.ct.jsaga.adaptor.tosca.security;

import fr.in2p3.jsaga.adaptor.security.SecurityCredential;
import java.io.File;
import java.io.PrintStream;
import org.apache.log4j.Logger;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.NotImplementedException;

public class ToscaSecurityCredential implements SecurityCredential {
    
    private static final Logger log =  Logger.getLogger(ToscaSecurityCredential.class);
    private static final String LS = System.getProperty("line.separator"); 

    private byte[] token = null;
    private String userName = null;

    public ToscaSecurityCredential(byte[] token, String userName) throws NoSuccessException {
        log.debug("ToscaSecurityCredential" 
              +LS+"-----------------------"
              +LS+"Token: '" + token + "'"
              +LS+"userName: '" + userName + "'" 
                 );
       this.token=token;
       this.userName=userName;
    }

    public void dump(PrintStream out) throws Exception {
        log.debug("Dumping"                  + LS 
                 +"-------"                  + LS 
                 +"Token   : '"+token+"'"    + LS 
                 +"UserName: '"+userName+"'" + LS  
                 );
        // Dumping ...
        out.print("Token   : '"+token+"'");
        out.print("UserName: '"+userName+"'");
    }

    @Override
    public String getUserID() throws NoSuccessException {
        return userName;
    }

    @Override
    public String getAttribute(String key) throws NotImplementedException, NoSuccessException {
        log.debug("Key: '"+key+"', has value: '<not available>'");
        return "not available";
    }

    @Override
    public void close() throws Exception {
        log.debug("Close called");
    }

}

/*
public class ToscaSecurityCredential implements SecurityCredential 
{
    private SSHSecurityCredential m_sshCred;
    private VOMSSecurityCredential m_proxy;
	
    public ToscaSecurityCredential(VOMSSecurityCredential p, 
                                   SSHSecurityCredential s) 
    {
        this.m_sshCred = s;
	this.m_proxy = p;
    }
    
    public ToscaSecurityCredential() 
    {        
    }

	
    @Override
    public String getUserID() throws Exception 
    {
        //return m_proxy.getUserID();
        return "tosca_user"
    }

    @Override
    public String getAttribute(String key) 
            throws NotImplementedException, NoSuccessException 
    {
        //return m_proxy.getAttribute(key);
        return m_proxy.getAttribute(key);
    }

    @Override
    public void close() throws Exception 
    {
        m_proxy.close();
	m_sshCred.close();
    }

    @Override
    public void dump(PrintStream out) throws Exception 
    {
        out.println("VOMS Proxy to access Cloud");
	m_proxy.dump(out);
	out.flush();
	out.println("SSH Credential to access VM");
	m_sshCred.dump(out);
	out.flush();
    }

    public SSHSecurityCredential getSSHCredential() 
    {
        return this.m_sshCred;
    }
        
    public VOMSSecurityCredential getProxy() 
    {        
        return this.m_proxy;
    }
}
*/