package org.asu.sma_tooling_dev;

import com.sforce.soap.partner.*;
import com.sforce.soap.tooling.GetUserInfoResult;
import com.sforce.soap.tooling.ApexClass;
import com.sforce.soap.tooling.ApexClassMember;
import com.sforce.soap.tooling.ContainerAsyncRequest;
import com.sforce.soap.tooling.MetadataContainer;
import com.sforce.soap.tooling.QueryResult;
import com.sforce.soap.tooling.SaveResult;
import com.sforce.soap.tooling.SoapConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

import java.sql.Connection;

public class Main
{
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        String username = "gwallima@asu.edu.dev";
        String password = "1Rn1oQWyyRrHLVfG4JsnfVQ01CQMEpF4uRlSNDwIxmhKc";

        try
        {
            ConnectorConfig partnerConfig = new ConnectorConfig();
            partnerConfig.setManualLogin(true);

            PartnerConnection partnerConnection = com.sforce.soap.partner.Connector.newConnection(partnerConfig);
            LoginResult lr = partnerConnection.login(username, password);

            ConnectorConfig toolingConfig = new ConnectorConfig();
            toolingConfig.setSessionId(lr.getSessionId());
            toolingConfig.setServiceEndpoint(lr.getServerUrl().replace('u', 'T'));

            SoapConnection toolingConnection = com.sforce.soap.tooling.Connector.newConnection(toolingConfig);

            GetUserInfoResult userInfo = toolingConnection.getUserInfo();
            System.out.println("UserID: " + userInfo.getUserId());
            System.out.println("User Full Name: " + userInfo.getUserFullName());
            System.out.println("User Email: " + userInfo.getUserEmail());
            System.out.println();
            System.out.println("SessionID: " + toolingConfig.getSessionId());
            System.out.println("Auth End Point: " + toolingConfig.getAuthEndpoint());
            System.out.println("Service End Point: " + toolingConfig.getServiceEndpoint());
            System.out.println();

            String classBody = "public class Messages {\n"
                    + "public string SayHello() {\n"
                    + " return 'Hello';\n" + "}\n"
                    + "}";

            // create an ApexClass object and set the body
            ApexClass apexClass = new ApexClass();
            apexClass.setBody(classBody);
            ApexClass[] classes = {apexClass};

            // call create() to add the class
            SaveResult[] saveResults = toolingConnection.create(classes);
            for (int i = 0; i < saveResults.length; i++)
            {
                if (saveResults[i].isSuccess())
                {
                    System.out.println("Successfully created Class: " +
                            saveResults[i].getId());
                }
                else
                {
                    System.out.println("Error: could not create Class ");
                    System.out.println("   The error reported was: " + saveResults[i].getErrors()[0].getMessage() + "\n");
                }
            }

            String updatedClassBody = "public class Messages {\n"
                    + "public string SayHello(string fName, string lName) {\n"
                    + " return 'Hello ' + fName + ' ' + lName;\n" + "}\n"
                    + "}";

            //create the metadata container object
            MetadataContainer Container = new MetadataContainer();
            Container.setName("SampleContainer");

            MetadataContainer[] Containers = { Container };
            SaveResult[] containerResults = sforce.create(Containers);
            if (containerResults[0].success)
            {
                String containerId = containerResults[0].id;

                //create the ApexClassMember object
                ApexClassMember classMember = new ApexClassMember();
                //pass in the class ID from the first example
                classMember.ContentEntityId = classId;
                classMember.Body = updatedClassBody;
                //pass the ID of the container created in the first step
                classMember.MetadataContainerId = containerId;
                ApexClassMember[] classMembers = { classMember };

                SaveResult[] MembersResults = sforce.create(classMembers);
                if (MembersResults[0].success)
                {
                    //create the ContainerAsyncRequest object
                    ContainerAsyncRequest request = new ContainerAsyncRequest();
                    //if the code compiled successfully, save the updated class to the server
                    //change to IsCheckOnly = true to compile without saving
                    request.IsCheckOnly = false;
                    request.MetadataContainerId = containerId;
                    ContainerAsyncRequest[] requests = { request };
                    SaveResult[] RequestResults = sforce.create(requests);
                    if (RequestResults[0].success)
                    {
                        string requestId = RequestResults[0].id;

                        //poll the server until the process completes
                        QueryResult queryResult = null;
                        String soql = "SELECT Id, State, ErrorMsg FROM ContainerAsyncRequest where id = '" + requestId + "'";
                        queryResult = sforce.query(soql);
                        if (queryResult.size > 0)
                        {
                            ContainerAsyncRequest _request = (ContainerAsyncRequest)queryResult.records[0];
                            while (_request.State.ToLower() == "queued")
                            {
                                //pause the process for 2 seconds
                                Thread.Sleep(2000);

                                //poll the server again for completion
                                queryResult = sforce.query(soql);
                                _request = (ContainerAsyncRequest)queryResult.records[0];
                            }

                            //now process the result
                            switch (_request.State)
                            {
                                case "Invalidated":
                                    break;

                                case "Completed":
                                    //class compiled successfully
                                    //see the next example on how to process the SymbolTable
                                    break;

                                case "Failed":
                                    . .   break;

                                case "Error":
                                    break;

                                case "Aborted":
                                    break;

                            }
                        }
                        else
                        {
                            //no rows returned
                        }
                    }
                    else
                    {
                        Console.WriteLine("Error: could not create ContainerAsyncRequest object");
                        Console.WriteLine("   The error reported was: " +
                                RequestResults[0].errors[0].message + "\n");
                    }
                }
                else
                {
                    Console.WriteLine("Error: could not create Class Member ");
                    Console.WriteLine("   The error reported was: " +
                            MembersResults[0].errors[0].message + "\n");
                }
            }
            else
            {
                .. Console.WriteLine("Error: could not create MetadataContainer ");
                Console.WriteLine("   The error reported was: " +
                        containerResults[0].errors[0].message + "\n");
            }
        }
        catch(ConnectionException e)
        {
            e.printStackTrace();
        }
    }
}
