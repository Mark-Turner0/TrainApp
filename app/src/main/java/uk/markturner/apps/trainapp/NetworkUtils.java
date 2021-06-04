package uk.markturner.apps.trainapp;

import android.app.Application;
import android.util.Log;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.URL;
import javax.net.ssl.HttpsURLConnection;

public class NetworkUtils extends Application {

    private static String response;
    private static DataInputStream input;
    private static final String httpsURL = "https://lite.realtime.nationalrail.co.uk/OpenLDBWS/ldb11.asmx"; //SAME URL for all requests
    private static final String token = "[INSERT YOUR TOKEN HERE WHICH YOU CAN GET FROM https://realtime.nationalrail.co.uk/OpenLDBWSRegistration/]";
    private static String request1 ="<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:typ=\"http://thalesgroup.com/RTTI/2013-11-28/Token/types\" xmlns:ldb=\"http://thalesgroup.com/RTTI/2017-10-01/ldb/\">\n" +
            "   <soapenv:Header>\n" +
            "      <typ:AccessToken>\n" +
            "         <typ:TokenValue>"+token+"</typ:TokenValue>\n" +
            "      </typ:AccessToken>\n" +
            "   </soapenv:Header>\n" +
            "   <soapenv:Body>\n" +
            "      <ldb:GetDepBoardWithDetailsRequest>\n" +
            "         <ldb:numRows>10</ldb:numRows>\n" +
            "         <ldb:crs>XXX</ldb:crs>\n" +
            "      </ldb:GetDepBoardWithDetailsRequest>\n" +
            "   </soapenv:Body>\n" +
            "</soapenv:Envelope>";

    private static String request2 = "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\" xmlns:typ=\"http://thalesgroup.com/RTTI/2013-11-28/Token/types\" xmlns:ldb=\"http://thalesgroup.com/RTTI/2017-10-01/ldb/\">\n" +
            "   <soapenv:Header>\n" +
            "      <typ:AccessToken>\n" +
            "         <typ:TokenValue>"+token+"</typ:TokenValue>\n" +
            "      </typ:AccessToken>\n" +
            "   </soapenv:Header>\n" +
            "   <soapenv:Body>\n" +
            "      <ldb:GetServiceDetailsRequest>\n" +
            "         <ldb:serviceID>XXX</ldb:serviceID>\n" +
            "      </ldb:GetServiceDetailsRequest>\n" +
            "   </soapenv:Body>\n" +
            "</soapenv:Envelope>";

    static String getTrainInfo(String queryString, int type) {
        try {
            URL url = new URL(httpsURL);
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection(); //Opens a standard HTTPS connection
            connection.setRequestMethod("POST"); //Sends POST request in the header
            connection.setRequestProperty("Content-Type", "text/xml"); //Sends content type in the header
            connection.setDoOutput(true); //Allows you to see output in more detail

            DataOutputStream output = new DataOutputStream(connection.getOutputStream());
            if (type == 0) { //if mode/type = 0 (departure)
                request1 = request1.replace("XXX", queryString.toUpperCase()); //Replace XXX in request1 with station code
                output.writeBytes(request1); //convert this to bytes
                request1 = request1.replace(queryString.toUpperCase(), "XXX"); //Replace the station code back with XXX
            } else if (type == 1) { //if mode/type = 1 (arrival)
                request1 = request1.replace("XXX",queryString.toUpperCase()); //Replace XXX in request1 with station code again
                request1 = request1.replaceAll("GetDepBoardWithDetailsRequest","GetArrBoardWithDetailsRequest");
                output.writeBytes(request1); //replace DepBoard with ArrBoard to get different results, and write this to bytes
                request1 = request1.replace(queryString.toUpperCase(), "XXX"); //Replace the station code back with XXX
                request1= request1.replaceAll("GetArrBoardWithDetailsRequest","GetDepBoardWithDetailsRequest");
            } else if (type == 2) { //Reset ArrBoard back to DepBoard. If type = 2 (ID lookup)...
                request2 = request2.replace("XXX", queryString); //Whole different request is needed, same methodology though
                output.writeBytes(request2); //Write this to bytes
                request2 = request2.replace(queryString, "XXX"); //Replace the station code back with XXX
            }
            output.close(); //Close the connection
            input = new DataInputStream(connection.getInputStream()); //Get the input stream from the connection
            StringBuilder toBR = new StringBuilder();

            for (int c = input.read(); c != -1; c = input.read()) { //For each line, save it to c, and read it. (-1 is EOF)
                toBR.append((char) c);//Append it to a StringBuilder
            }
            input.close(); //Close the connection
            response = toBR.toString(); //Convert StringBuilder to String
        }
        catch (IOException e){
            return null; //If an error occurs, return null, and it will be handled by callback
        }
        return response;} //Return it
}