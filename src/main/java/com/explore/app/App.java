package com.explore.app;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class App {
    public static void main(String[] args) {
        // Obtain the access token
        String accessToken = getAccessToken();
        
        // Make API call to retrieve artist information
        String artistInfo = getArtistInfo(accessToken, "4Z8W4fKeB5YxbusRsdQVPb");
        System.out.println(artistInfo);
    }

    private static String getAccessToken() {
        try {
            // Set the request URL
            URL url = new URL("https://accounts.spotify.com/api/token");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Set the request method
            connection.setRequestMethod("POST");
            
            // Set the request headers
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            
            // Set the request body
            String requestBody = "grant_type=client_credentials&client_id=e2aeffcd41484cf2a18e8e30c3288019&client_secret=30adea1d5ff54ddc9c0e0133d707bb56";
            connection.setDoOutput(true);
            connection.getOutputStream().write(requestBody.getBytes());
            
            // Send the request
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the response
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                // Extract the access token from the response
                String json = response.toString();
                // Parse the JSON and extract the access token
                // Assuming the access token is in the "access_token" field
                // You might need to use a JSON library like Gson or Jackson for parsing JSON
                String accessToken = /* Parse the JSON and extract the access token */;
                
                return accessToken;
            } else {
                System.out.println("Failed to obtain access token. Response code: " + responseCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return null;
    }

    private static String getArtistInfo(String accessToken, String artistId) {
        try {
            // Set the request URL
            URL url = new URL("https://api.spotify.com/v1/artists/" + artistId);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            
            // Set the request method
            connection.setRequestMethod("GET");
            
            // Set the request headers
            connection.setRequestProperty("Authorization", "Bearer " + accessToken);
            
            // Send the request
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // Read the response
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                
                return response.toString();
            } else {
                System.out.println("Failed to retrieve artist information. Response code: " + responseCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return null;
    }
}



