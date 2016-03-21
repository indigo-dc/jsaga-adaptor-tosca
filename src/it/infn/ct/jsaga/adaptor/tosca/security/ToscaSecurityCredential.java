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
 * **************************************************************************
 */
package it.infn.ct.jsaga.adaptor.tosca.security;

import fr.in2p3.jsaga.adaptor.security.SecurityCredential;
import fr.in2p3.jsaga.adaptor.security.impl.SSHSecurityCredential;
import java.io.PrintStream;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.NotImplementedException;

public class ToscaSecurityCredential implements SecurityCredential {

    private String token;
    private SSHSecurityCredential sshCredential;

    public ToscaSecurityCredential(String token) {
        this.token = token;
    }

    public ToscaSecurityCredential() {
    }

    @Override
    public String getUserID() throws Exception {
        return "toscauser";
    }

    @Override
    public String getAttribute(String key)
            throws NotImplementedException, NoSuccessException {
        switch (key) {
            case "token":
                return token;
            default:
        }
        return null;
    }

    @Override
    public void close() throws Exception {
    }

    @Override
    public void dump(PrintStream out) throws Exception {
        out.flush();
    }

    public SSHSecurityCredential getSshCredential() {
        return this.sshCredential;
    }

    public void setSshCredential(SSHSecurityCredential sshCred) {
        this.sshCredential = sshCred;
    }
}
