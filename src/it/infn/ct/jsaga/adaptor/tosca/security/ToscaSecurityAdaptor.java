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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.ogf.saga.error.IncorrectStateException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.TimeoutException;

import fr.in2p3.jsaga.adaptor.base.defaults.Default;
import fr.in2p3.jsaga.adaptor.base.usage.U;
import fr.in2p3.jsaga.adaptor.base.usage.UAnd;
import fr.in2p3.jsaga.adaptor.base.usage.UDuration;
import fr.in2p3.jsaga.adaptor.base.usage.UFile;
import fr.in2p3.jsaga.adaptor.base.usage.UFilePath;
import fr.in2p3.jsaga.adaptor.base.usage.UHidden;
import fr.in2p3.jsaga.adaptor.base.usage.UOr;
import fr.in2p3.jsaga.adaptor.base.usage.Usage;
import fr.in2p3.jsaga.adaptor.security.ExpirableSecurityAdaptor;
import fr.in2p3.jsaga.adaptor.security.NoneSecurityAdaptor;
import fr.in2p3.jsaga.adaptor.security.PasswordDecrypterSingleton;
import fr.in2p3.jsaga.adaptor.security.PasswordEncrypterSingleton;
import fr.in2p3.jsaga.adaptor.ssh3.security.SSHSecurityAdaptor;
import fr.in2p3.jsaga.adaptor.security.SecurityCredential;
import fr.in2p3.jsaga.adaptor.security.UserPassExpirableSecurityCredential;
import fr.in2p3.jsaga.adaptor.security.impl.UserPassSecurityCredential;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
//import fr.in2p3.jsaga.adaptor.security.VOMSSecurityAdaptor;
//import fr.in2p3.jsaga.adaptor.security.VOMSSecurityCredential;
//import fr.in2p3.jsaga.adaptor.security.impl.SSHSecurityCredential;
import org.apache.log4j.Logger;
import org.ogf.saga.context.Context;

/* ***************************************************
* *** Centre de Calcul de l'IN2P3 - Lyon (France) ***
* ***             http://cc.in2p3.fr/             ***
* ***************************************************
* File:   ToscaSecurityAdaptor
* Author: Lionel Schwarz (lionel.schwarz@in2p3.fr)
* Date:   22 oct 2013
* ***************************************************/

public class ToscaSecurityAdaptor implements ExpirableSecurityAdaptor 
{
    private static final Logger log =  Logger.getLogger(ToscaSecurityAdaptor.class);
    private static final String LS = System.getProperty("line.separator"); 
    
      private static final String USERPASSFILE = "UserPassFile";
      private static final int USAGE_INIT = 1;
      private static final int USAGE_VOLATILE = 2;
      private static final int USAGE_LOAD = 3;
    
    //private SSHSecurityAdaptor m_sshAdaptor;

    public ToscaSecurityAdaptor() 
    {
        super();
        log.debug("Constructor()");
        //m_sshAdaptor = new SSHSecurityAdaptor();
    }
    
    @Override
    public String getType() { 
        log.debug("getType()->tosca");
        return "tosca"; 
    }

    @Override
    public Usage getUsage() {
        log.debug("getUsage()");
        return new UAnd.Builder()
                .and(new U(Context.USERID))
                .and(new UOr.Builder()
                             .or(new UAnd.Builder()
                                         .id(USAGE_INIT)
                                         .and(new UHidden(Context.USERPASS))
                                         .and(new U(Context.LIFETIME))
                                         .and(new UFilePath(USERPASSFILE))
                                         .build()
                             )
                             .or(new U(USAGE_VOLATILE, Context.USERPASS))
                             .or(new UFile(USAGE_LOAD, USERPASSFILE))
                             .build()
                )
                .build();
    }

    @Override
    public Default[] getDefaults(Map attributes) throws IncorrectStateException {
        return new Default[]{
                new Default(Context.USERID, System.getProperty("user.name"))
        };
    }

    @Override
    public Class getSecurityCredentialClass() 
    {
        log.debug("getSecurityCredentialClass()");
        return ToscaSecurityCredential.class;
    }

    
    @Override
    public SecurityCredential createSecurityCredential(int usage, Map attributes, String contextId) throws IncorrectStateException, NoSuccessException {
         try {
             switch(usage) {
                 case USAGE_INIT:
                 {
                     // get attributes
                     String name = (String) attributes.get(Context.USERID);
                     String password = (String) attributes.get(Context.USERPASS);
                     int lifetime = (attributes.containsKey(Context.LIFETIME)
                             ? UDuration.toInt(attributes.get(Context.LIFETIME))
                             : 12*3600);
                      File file = new File((String) attributes.get(USERPASSFILE));
  
                     // encrypt password
                     PasswordEncrypterSingleton crypter = new PasswordEncrypterSingleton(name, lifetime);
                     String cryptedPassword = crypter.encrypt(password);
                     int expiryDate = PasswordEncrypterSingleton.getExpiryDate(lifetime);
 
                     // write to file
                     DataOutputStream out = new DataOutputStream(new FileOutputStream(file));
                     out.writeBytes(cryptedPassword);
                     out.close();
 
                     // returns
                     return new UserPassExpirableSecurityCredential(name, password, expiryDate);
                 }
                 case USAGE_VOLATILE:
                 {
                     return new UserPassSecurityCredential(
                             (String) attributes.get(Context.USERID),
                             (String) attributes.get(Context.USERPASS));
                 }
                 case USAGE_LOAD:
                 {
                      // get attributes
                     String name = (String) attributes.get(Context.USERID);
                     File file = new File((String) attributes.get(USERPASSFILE));
 
                     // load from file
                     byte[] buffer = new byte[(int) file.length()];
                     DataInputStream in = new DataInputStream(new FileInputStream(file));
                     in.readFully(buffer);
                     in.close();
                     String cryptedPassword = new String(buffer);
 
                     // decrypt password
                     PasswordDecrypterSingleton decrypter = new PasswordDecrypterSingleton(name);
                     int expiryDate = decrypter.getExpiryDate();
                     int currentDate = (int) (System.currentTimeMillis()/1000);
                     String password;
                     if (currentDate > expiryDate) {
                         this.destroySecurityAdaptor(attributes, contextId);
                         password = null;
                     } else {
                         password = decrypter.decrypt(cryptedPassword);
                     }
                     return new UserPassExpirableSecurityCredential(name, password, expiryDate);
                 }
                 default:
                     throw new NoSuccessException("INTERNAL ERROR: unexpected exception");
             }
         } catch(IncorrectStateException e) {
             throw e;
         } catch(NoSuccessException e) {
             throw e;
         } catch(Exception e) {
             throw new NoSuccessException(e);
         }
     }
    
    public void destroySecurityAdaptor(Map attributes, String contextId) throws Exception {
        File file = new File((String) attributes.get(USERPASSFILE));
        if (file.exists() && !file.delete()) {
            throw new Exception("Failed to delete file: "+file);
        }
    }
}