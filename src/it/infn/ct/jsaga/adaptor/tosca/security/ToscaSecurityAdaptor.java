/**
 * ************************************************************************
 * Copyright (c) 2011: Istituto Nazionale di Fisica Nucleare (INFN), Italy
 * Consorzio COMETA (COMETA), Italy
 *
 * See http://www.infn.it and and http://www.consorzio-cometa.it for details on
 * the copyright holders.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 *
 * @author <a href="mailto:riccardo.bruno@ct.infn.it">Riccardo Bruno</a>(INFN)
***************************************************************************
 */
package it.infn.ct.jsaga.adaptor.tosca.security;

import java.util.Map;

import org.ogf.saga.error.IncorrectStateException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.TimeoutException;

import fr.in2p3.jsaga.adaptor.base.defaults.Default;
import fr.in2p3.jsaga.adaptor.base.usage.UAnd;
import fr.in2p3.jsaga.adaptor.base.usage.Usage;
import fr.in2p3.jsaga.adaptor.security.ExpirableSecurityAdaptor;
import fr.in2p3.jsaga.adaptor.ssh3.security.SSHSecurityAdaptor;
import fr.in2p3.jsaga.adaptor.security.SecurityCredential;
import fr.in2p3.jsaga.adaptor.security.impl.SSHSecurityCredential;

/* ***************************************************
 * *** Centre de Calcul de l'IN2P3 - Lyon (France) ***
 * ***             http://cc.in2p3.fr/             ***
 * ***************************************************
 * File:   ToscaSecurityAdaptor
 * Author: Lionel Schwarz (lionel.schwarz@in2p3.fr)
 * Date:   22 oct 2013
 * ***************************************************/
public class ToscaSecurityAdaptor implements ExpirableSecurityAdaptor {

    private SSHSecurityAdaptor m_sshAdaptor;

    public ToscaSecurityAdaptor() {
        super();
        m_sshAdaptor = new SSHSecurityAdaptor();
    }

    @Override
    public String getType() {
        return "tosca";
    }

    @Override
    public Usage getUsage() {
        return new UAnd.Builder()
                .and(new SSHSecurityAdaptor().getUsage())
                .build();
    }

    @Override
    public Default[] getDefaults(Map attributes)
            throws IncorrectStateException {
        return m_sshAdaptor.getDefaults(attributes);
    }

    @Override
    public Class getSecurityCredentialClass() {
        return ToscaSecurityCredential.class;
    }

    @Override
    public SecurityCredential createSecurityCredential(int usage,
            Map attributes, String contextId)
            throws IncorrectStateException,
            TimeoutException, NoSuccessException {
        return new ToscaSecurityCredential((String)attributes.get("token"));
    }

    @Override
    public void destroySecurityAdaptor(Map map, String string) throws Exception {
        // ???
    }
}
