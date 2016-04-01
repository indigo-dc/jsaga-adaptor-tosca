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

package test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.ogf.saga.context.Context;
import org.ogf.saga.context.ContextFactory;
import org.ogf.saga.error.NotImplementedException;
import org.ogf.saga.error.IncorrectStateException;
import org.ogf.saga.error.NoSuccessException;
import org.ogf.saga.error.PermissionDeniedException;
import org.ogf.saga.error.SagaException;
import org.ogf.saga.session.Session;
import org.ogf.saga.session.SessionFactory;
import org.ogf.saga.job.JobDescription;
import org.ogf.saga.job.JobService;
import org.ogf.saga.job.JobFactory;
import org.ogf.saga.job.Job;
import org.ogf.saga.task.State;
import org.ogf.saga.url.URL;
import org.ogf.saga.url.URLFactory;
import fr.in2p3.jsaga.impl.job.instance.JobImpl;
import fr.in2p3.jsaga.impl.job.service.JobServiceImpl;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/* *********************************************
 * *** Istituto Nazionale di Fisica Nucleare ***
 * ***      Sezione di Catania (Italy)       ***
 * ***        http://www.ct.infn.it/         ***
 * *********************************************
 * File:    ToscaJobControlAdaptor.java
  * Authors: Giuseppe LA ROCCA, Riccardo BRUNO
 * Email:   <giuseppe.larocca
 *          ,riccardo.bruno
 *          ,marco.fargetta>@ct.infn.it
 * Ver.:    1.0.0
 * Date:    24 February 2016
 * *********************************************/

public class RunTest 
{     
    private static Logger log = Logger.getLogger(RunTest.class);
    private static final String LS = System.getProperty("line.separator"); 
    
    // Setup here endpoint and TOSCA template file
    private String m_EndPoint="tosca://90.147.170.168:31491/orchestrator/deployments";
    private String m_Template="/tmp/tosca_template.yaml";
    
    public static void main(String[] args) throws NotImplementedException 
    {                
        System.setProperty("saga.factory", "fr.in2p3.jsaga.impl.SagaFactoryImpl");
        Logger.getRootLogger().setLevel(Level.DEBUG);
        
        RunTest rt = new RunTest();
        String jobId = null;
        
        //
        // Submit 1st
        //
        jobId = rt.submitJob();
        //jobId="[tosca://90.147.170.168:31491/orchestrator/deployments?tosca_template=/tmp/tosca_template.yaml]-[6d9a4630-d436-4d03-991d-99233f8762cc#d3338dcf-b7a9-4517-8876-59af9dd140a2]";

        //
        // Now check status
        //
        if (jobId != null && jobId.length() > 0)
            rt.checkStatus(jobId);
                
        log.debug(("Done"));
    }  

    public String submitJob() {
        Session session = null;
        Context context = null;        
        JobService service = null; 
        Job job = null;
        
        String ServiceURL = "";
        String jobId = "";        
        
        try {
            session = SessionFactory.createSession(false);            
            context = ContextFactory.createContext("tosca");
            context.setAttribute("token","AABBCCDDEEFF00112233445566778899");                        
            session.addContext(context);
            
            try {    
                log.info("Initialize the JobService context... ");
                
                ServiceURL = m_EndPoint + "?" + "tosca_template=" + m_Template;
                URL serviceURL = URLFactory.createURL(ServiceURL);
                log.info("serviceURL = '" + serviceURL +"'");
                
                service = JobFactory.createJobService(session, serviceURL);  
                JobDescription desc = JobFactory.createJobDescription();
                log.info("Attributes ...");
                desc.setAttribute(JobDescription.EXECUTABLE, "tosca_test.sh");                	           
                desc.setAttribute(JobDescription.OUTPUT, "output.txt");
                desc.setAttribute(JobDescription.ERROR, "error.txt");                  
                log.info("VectorAttributes ...");
                desc.setVectorAttribute(JobDescription.ARGUMENTS,new String[]{"arg1" , "arg2"}); 
                desc.setVectorAttribute(desc.FILETRANSFER,
                    new String[]{
                        "/tmp/tosca_template.yaml>tosca_template.yaml"
                       ,"/tmp/tosca_test.sh>tosca_test.sh"    
                       ,"/tmp/output.txt<output.txt"
                       ,"/tmp/error<error.txt"
                    }                   
                );
                log.info("Create job ...");
                job = service.createJob(desc);
                log.info("Run job ...");
                job.run();                                

                // Getting the jobId
                jobId = job.getAttribute(Job.JOBID);
                log.info("Job instance created with jobId: '"+jobId+"'");                

                try {
                        ((JobServiceImpl)service).disconnect();                        
                } catch (NoSuccessException ex) {
                        log.error("See below the stack trace... ");
                        ex.printStackTrace(System.out);					
                }
                log.info("");
                log.info("Closing session...");
                session.close();   
            } catch (Exception ex) {
                    log.error("");
                    log.error("Initialize the JobService context [ FAILED ] ");
                    log.error("See below the stack trace... ");                
                    ex.printStackTrace(System.out);
            }                                           
        } catch (Exception ex) {
            log.error("Failed to initialize the security context"+LS
                     +"See below the stack trace... "
                     );
            ex.printStackTrace(System.out);
            System.exit(1);
        }        
        return jobId;
    }
            
    public void checkStatus(String jobId) {
        Session session = null;
        Context context = null;
        
        String ServiceURL = "";
        
        //Create an empty SAGA session
        log.info("");
        log.info("Initialize the security context for the tosca JSAGA adaptor");
        try {
            session = SessionFactory.createSession(false);
            context = ContextFactory.createContext("tosca");           
            context.setAttribute("token","AABBCCDDEEFF00112233445566778899");            
            session.addContext(context);

            ServiceURL = m_EndPoint;            
            URL serviceURL = URLFactory.createURL(ServiceURL);                
            log.info("serviceURL = '" + serviceURL +"'");
            JobService service = JobFactory.createJobService(session, serviceURL);  

            String nativeJobId = getNativeJobId(jobId);
            Job job = service.getJob(nativeJobId);                                

            log.info("Fetching the status of the job: '" +jobId+ "'");
            log.info("nativeJobId: '" + nativeJobId + "'");

            //String nativeJobId = "";
            log.debug(("Entering job status loop ..."));
            while(true) 
            {
                // display final state
                State state = null;

                try { 
                    state = job.getState();
                    log.info("Current Status = " + state.name());

                    //String executionHosts[];
                    //executionHosts = job.getVectorAttribute(Job.EXECUTIONHOSTS);
                    //log.info("Execution Host = " + executionHosts[0]);

                } catch (Exception ex) {
                    log.error("");
                    log.error("Error in getting job status... [ FAILED ] ");
                    log.error(ex.toString());
                    log.error("Cause :" + ex.getCause());
                }
                
                if (State.CANCELED.compareTo(state) == 0) {
                    log.info("");
                    log.info("Job Status = CANCELED ");
                    break;
                } else if (State.FAILED.compareTo(state) == 0) {
                    try {
                            String exitCode = job.getAttribute(Job.EXITCODE);
                            log.info("");
                            log.info("Job Status = FAILED");
                            log.info("Exit Code (" + exitCode + ")");
                    } catch (SagaException e) { 
                        log.error("Job failed."); 
                    }
                    break;
                } else if (State.DONE.compareTo(state) == 0) {                
                    String exitCode = job.getAttribute(Job.EXITCODE);                    
                    log.info("Final Job Status = DONE");
                    log.info("Exit Code ("+exitCode+")");                    
                    log.info("Retrieving job results; this operation may take a few minutes to complete ...");

                    // ========================================== //
                    // === EXECUTING post-staging and cleanup === //
                    // ========================================== //
                    try { 
                        ((JobImpl)job).postStagingAndCleanup();
                        log.info("Job outputs retrieved");
                    } catch (NotImplementedException ex) { ex.printStackTrace(System.out); 
                    } catch (PermissionDeniedException ex) { ex.printStackTrace(System.out); 
                    } catch (IncorrectStateException ex) { ex.printStackTrace(System.out); 
                    } catch (NoSuccessException ex) { ex.printStackTrace(System.out); }
                    
                    try { 
                        ((JobServiceImpl)service).disconnect();
                        log.info("Service disconnected successfully");
                    } catch (NoSuccessException ex) { 
                        log.info("Service disconnected unsuccessfully");
                        log.error("See below the stack trace... ");
                        ex.printStackTrace(System.out); 
                    }
                    session.close();
                    log.info("Session closed");
                    break;
                } else {
                    log.info("");
                    log.info("Unexpected job status: " + state);                    
                }

                try { 
                    int waitms = 10000;
                    log.info("Waiting "+waitms+"ms for next job status check ...");
                    Thread.sleep(waitms);            
                } catch (InterruptedException ex) {
                    ex.printStackTrace(System.out);
                } 
            } 
        } catch (Exception ex) {
            log.error("Failed to initialize the security context"+LS
                     +"See below the stack trace... "
                     );
            ex.printStackTrace(System.out);
            System.exit(1);
        }        
    }

    public static String getNativeJobId(String jobId) 
    {
        String nativeJobId = "";
        Pattern pattern = Pattern.compile("\\[(.*)\\]-\\[(.*)\\]");
        Matcher matcher = pattern.matcher(jobId);
    
        try {
            if (matcher.find()) nativeJobId = matcher.group(2);                
            else return null;               
        } catch (Exception ex) { 
            System.out.println(ex.toString());
            return null;
        }

        return nativeJobId;
    }
}
 