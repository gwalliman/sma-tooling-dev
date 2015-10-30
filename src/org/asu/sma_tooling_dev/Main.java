package org.asu.sma_tooling_dev;

import com.sforce.soap.partner.*;
import com.sforce.soap.partner.Connector;
import com.sforce.soap.partner.LoginResult;
import com.sforce.soap.tooling.*;
import com.sforce.soap.tooling.DeleteResult;
import com.sforce.soap.tooling.GetUserInfoResult;
import com.sforce.soap.tooling.QueryResult;
import com.sforce.soap.tooling.SaveResult;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;

import java.sql.Connection;
import java.util.ArrayList.*;
import java.util.Arrays;
import java.util.Arrays.*;

public class Main
{
    /**
     * @param args
     */
    public static void main(String[] args)
    {
        String username = "gwallima@asu.edu.garret";
        String password = "exO*RN9!898b*i6s3kn4";
        String security_token = "B2IQ5dwbsjaHTBJSKcQFpY31";

        SoapConnection toolingConnection = null;
        String classId = null;
        String triggerId = null;
        String containerId = null;

        try
        {
            ConnectorConfig partnerConfig = new ConnectorConfig();
            partnerConfig.setManualLogin(true);

            PartnerConnection partnerConnection = com.sforce.soap.partner.Connector.newConnection(partnerConfig);
            LoginResult lr = partnerConnection.login(username, password + security_token);

            ConnectorConfig toolingConfig = new ConnectorConfig();
            toolingConfig.setSessionId(lr.getSessionId());
            toolingConfig.setServiceEndpoint(lr.getServerUrl().replace('u', 'T'));

            toolingConnection = com.sforce.soap.tooling.Connector.newConnection(toolingConfig);

            GetUserInfoResult userInfo = toolingConnection.getUserInfo();
            System.out.println("UserID: " + userInfo.getUserId());
            System.out.println("User Full Name: " + userInfo.getUserFullName());
            System.out.println("User Email: " + userInfo.getUserEmail());
            System.out.println();
            System.out.println("SessionID: " + toolingConfig.getSessionId());
            System.out.println("Auth End Point: " + toolingConfig.getAuthEndpoint());
            System.out.println("Service End Point: " + toolingConfig.getServiceEndpoint());
            System.out.println();

            /**
             * Create a Messages class
             */
            String classBody = "public class Messages {\n"
                    + "public string SayHello() {\n"
                    + " return 'Hello';\n" + "}\n"
                    + "}";

            // create an ApexClass object and set the body
            ApexClass apexClass = new ApexClass();
            apexClass.setBody(classBody);
            ApexClass[] classes = {apexClass};
            classId = null;

            // call create() to add the class
            SaveResult[] saveResults = toolingConnection.create(classes);
            if (saveResults[0].isSuccess())
            {
                System.out.println("Successfully created Class: " + saveResults[0].getId());
                classId = saveResults[0].getId();
            }
            else
            {
                System.out.println("Error: could not create Class ");
                System.out.println("The error reported was: " + saveResults[0].getErrors()[0].getMessage() + "\n");
                throw new Exception("Error");
            }

            String triggerBody = "trigger MessageTrigger on Contact (before insert) { \n"
                    + "Integer x = 0;\n"
                    + "}";
            ApexTrigger trigger = new ApexTrigger();
            trigger.setBody(triggerBody);
            trigger.setFullName("MessageTrigger");
            trigger.setTableEnumOrId("Contact");
            ApexTrigger[] triggers = { trigger };

            SaveResult[] triggerCreateResults = toolingConnection.create(triggers);
            if (triggerCreateResults[0].isSuccess())
            {
                System.out.println("Successfully created Trigger: " + triggerCreateResults[0].getId());
                triggerId = triggerCreateResults[0].getId();
            }
            else
            {
                System.out.println("Error: could not create Class Trigger in metadata container");
                System.out.println("The error reported was: " + triggerCreateResults[0].getErrors()[0].getMessage() + "\n");
                throw new Exception("Error");
            }

            /**
             * Update the Messages class
             */

            //create the metadata container object
            //This object is used to aggregate metadata changes
            MetadataContainer Container = new MetadataContainer();
            Container.setName("SampleContainer");

            MetadataContainer[] Containers = { Container };
            SaveResult[] containerResults = toolingConnection.create(Containers);
            if (containerResults[0].isSuccess())
            {
                System.out.println("Successfully created MetadataContainer: " + containerResults[0].getId());
                containerId = containerResults[0].getId();
            }
            else
            {
                System.out.println("Error: could not create MetadataContainer ");
                System.out.println("The error reported was: " + containerResults[0].getErrors()[0].getMessage() + "\n");
                throw new Exception("Error");
            }

            String updatedClassBody = "public class Messages {\n"
                    + "public string SayHello(string fName, string lName) {\n"
                    + " return 'Hello ' + fName + ' ' + lName;\n" + "}\n"
                    + "}";

            //create the ApexClassMember object
            ApexClassMember classMember = new ApexClassMember();

            //pass in the class ID from when we created it
            classMember.setContentEntityId(classId);
            classMember.setBody(updatedClassBody);

            //pass the ID of the container created in the first step
            classMember.setMetadataContainerId(containerId);
            ApexClassMember[] classMembers = { classMember };

            SaveResult[] updateResults = toolingConnection.create(classMembers);
            if (updateResults[0].isSuccess())
            {
                System.out.println("Successfully updated class in metadata container: " + updateResults[0].getId());
            }
            else
            {
                System.out.println("Error: could not create Class Member in metadata container");
                System.out.println("The error reported was: " + updateResults[0].getErrors()[0].getMessage() + "\n");
                throw new Exception("Error");
            }

            String updatedTriggerBody = "trigger MessageTrigger on Contact (before insert) { \n"
                    + "Integer x = 1;\n"
                    + "}";

            //create the ApexClassMember object
            ApexTriggerMember triggerMember = new ApexTriggerMember();

            //pass in the class ID from when we created it
            triggerMember.setContentEntityId(triggerId);
            triggerMember.setBody(updatedTriggerBody);

            //pass the ID of the container created in the first step
            triggerMember.setMetadataContainerId(containerId);
            ApexTriggerMember[] triggerMembers = { triggerMember };

            updateResults = toolingConnection.create(triggerMembers);
            if (updateResults[0].isSuccess())
            {
                System.out.println("Successfully updated trigger in metadata container: " + updateResults[0].getId());
            }
            else
            {
                System.out.println("Error: could not create Trigger Member in metadata container");
                System.out.println("The error reported was: " + updateResults[0].getErrors()[0].getMessage() + "\n");
                throw new Exception("Error");
            }

            //create the ContainerAsyncRequest object
            ContainerAsyncRequest request = new ContainerAsyncRequest();

            //if the code compiled successfully, save the updated class to the server
            //change to IsCheckOnly = true to compile without saving
            request.setIsCheckOnly(false);
            request.setMetadataContainerId(containerId);
            ContainerAsyncRequest[] requests = { request };
            SaveResult[] RequestResults = toolingConnection.create(requests);
            if (!RequestResults[0].isSuccess())
            {
                System.out.println("Error: could not create ContainerAsyncRequest object");
                System.out.println("The error reported was: " + RequestResults[0].getErrors()[0].getMessage() + "\n");
                throw new Exception("Error");
            }

            String requestId = RequestResults[0].getId();

            //poll the server until the process completes
            QueryResult queryResult = null;
            String soql = "SELECT Id, State, ErrorMsg FROM ContainerAsyncRequest where id = '" + requestId + "'";
            queryResult = toolingConnection.query(soql);

            if (queryResult.getSize() <= 0)
            {
                //no rows returned
                System.out.println("Error: the ContainerAsyncRequest was apparently created, but can not be found on the server");
                throw new Exception("Error");
            }

            ContainerAsyncRequest _request = (ContainerAsyncRequest)queryResult.getRecords()[0];
            while (_request.getState().toLowerCase().equals("queued"))
            {
                //pause the process for 2 seconds
                Thread.sleep(2000);

                //poll the server again for completion
                queryResult = toolingConnection.query(soql);
                _request = (ContainerAsyncRequest)queryResult.getRecords()[0];
            }

            System.out.println("SOQL Query Result: " + _request.getState());

            //now process the result
            switch (_request.getState())
            {
                case "Completed":
                    break;

                default:
                    throw new Exception("Error");
            }

            //poll the server until the process completes
            queryResult = null;

            soql = "Select Id, Name, Body from ApexClass where Id = '" + classId + "' LIMIT 1";
            queryResult = toolingConnection.query(soql);

            if (queryResult.getSize() <= 0)
            {
                //no rows returned
                System.out.println("Error: the Messages class can not be found on the server");
                throw new Exception("Error");
            }

            ApexClass messagesClass = (ApexClass)queryResult.getRecords()[0];
            System.out.println("The messages class is below:");
            System.out.println(messagesClass);

            soql = "Select Id, Name, Body from ApexTrigger where Name = 'MessageTrigger' LIMIT 1";
            queryResult = toolingConnection.query(soql);

            if (queryResult.getSize() <= 0)
            {
                //no rows returned
                System.out.println("Error: the Messages Trigger can not be found on the server");
                throw new Exception("Error");
            }

            ApexTrigger messagesTrigger = (ApexTrigger)queryResult.getRecords()[0];
            System.out.println("Messages Trigger ID: " + messagesTrigger.getId());
            System.out.println("The trigger class is below:");
            System.out.println(messagesTrigger);

            DeleteResult[] deleteResult = toolingConnection.delete(new String[]{messagesClass.getId()});
            if (deleteResult[0].isSuccess())
            {
                System.out.println("Successfully deleted Messages class: " + deleteResult[0].getId());
            }
            else
            {
                System.out.println("Error: could not delete Messages class object");
                System.out.println("The error reported was: " + deleteResult[0].getErrors()[0].getMessage() + "\n");
                throw new Exception("Error");
            }

            deleteResult = toolingConnection.delete(new String[]{messagesTrigger.getId()});
            if (deleteResult[0].isSuccess())
            {
                System.out.println("Successfully deleted Messages trigger: " + deleteResult[0].getId());
            }
            else
            {
                System.out.println("Error: could not delete Messages trigger object");
                System.out.println("The error reported was: " + deleteResult[0].getErrors()[0].getMessage() + "\n");
                throw new Exception("Error");
            }

            deleteResult = toolingConnection.delete(new String[] { containerId });
            if (deleteResult[0].isSuccess())
            {
                System.out.println("Successfully deleted Metadata Container: " + deleteResult[0].getId());
            }
            else
            {
                System.out.println("Error: could not delete Metadata Container object");
                System.out.println("The error reported was: " + deleteResult[0].getErrors()[0].getMessage() + "\n");
                throw new Exception("Error");
            }
        }
        catch(Exception e)
        {
            try
            {
                e.printStackTrace();
                if(containerId != null)
                {
                    toolingConnection.delete(new String[] { containerId });
                }
                if(classId != null)
                {
                    toolingConnection.delete(new String[]{classId});
                }
                if(triggerId != null)
                {
                    toolingConnection.delete(new String[]{triggerId});
                }
            }
            catch(Exception e2)
            {
                e2.printStackTrace();
            }

        }
    }
}
