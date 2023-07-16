package com.explore.app;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.util.Scanner;
import java.net.HttpURLConnection;
import java.net.URL;


/* token URL = ("https://accounts.spotify.com/api/token"); 
method = ("POST");
            
headers = "Content-Type", "application/x-www-form-urlencoded";

String requestBody = "grant_type=client_credentials&client_id=e2aeffcd41484cf2a18e8e30c3288019&client_secret=30adea1d5ff54ddc9c0e0133d707bb56";
*/ 


/* Need to first log in
 * need a playlist to dump songs into
 * methods to clear, delete, add to playlist
 * search songs by given inputs
 * print inputs needed/required
 */
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class App {
    public static void main(String[] args) {
        // get token
        String clientId = "e2aeffcd41484cf2a18e8e30c3288019";
        String clientSecret = "e823196c5aa14db6a3cd6eb57efa8bad";
        String accessToken = getAccessToken(clientId, clientSecret);

        // TO DO: check to see if there's a playlist called "Explore." If there is, save the ID#. If there isn't, make one and save the ID#
        System.out.println(accessToken);
        String explorePlaylist = get(accessToken, "me", "playlists");
        System.out.println(explorePlaylist);
        

        // Go to interface
        mainLoop();
    }

    public static String getAccessToken(String clientId, String clientSecret) {
        try {
            URL url = new URL("https://accounts.spotify.com/api/token");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setDoOutput(true);

            String requestBody = "grant_type=client_credentials&client_id=" + clientId + "&client_secret=" + clientSecret;
            byte[] postData = requestBody.getBytes(StandardCharsets.UTF_8);

            // Send the request body
            try (DataOutputStream outputStream = new DataOutputStream(conn.getOutputStream())) {
                outputStream.write(postData);
            }

            // Check if the connection is successful
            int responseCode = conn.getResponseCode();
            if (responseCode != 200) {
                throw new RuntimeException("HttpResponseCode: " + responseCode);
            } else {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parse the JSON response
                JSONParser parser = new JSONParser();
                JSONObject jsonResponse = (JSONObject) parser.parse(response.toString());
                String accessToken = (String) jsonResponse.get("access_token");

                return accessToken;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String[] getSongs(String accessToken, String args) {
        return null;
    }

    public static String getArtistName(String accessToken, String artistId) {
        String response = get(accessToken, "artists", artistId);
        JSONParser parser = new JSONParser();
        try {
            JSONObject jsonResponse = (JSONObject) parser.parse(response.toString());
            String artistName = (String) jsonResponse.get("name");
            return artistName;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String get(String accessToken, String subject, String propertyId) {
        System.out.println(accessToken);
        try {
            String apiUrl = "https://api.spotify.com/v1/" + subject + "/" + propertyId;
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Process the information in the response
                return response.toString();
            } else {
                throw new RuntimeException("HttpResponseCode: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void mainLoop() {
        while (true) {
            Scanner scanner = new Scanner(System.in);

            System.out.println("What would you like to do?");
            System.out.println("1: Edit Explore playlist");
            System.out.println("2: Find new music");
            System.out.println("3: Exit");
            int option = scanner.nextInt();

            switch (option) {
                case 1:
                    // do something
                    scanner.close();
                    return;
                case 2:
                    // something
                    scanner.close();
                    return;
                case 3:
                    scanner.close();
                    return;
                default:
                    System.out.println("Please enter a number between 1 and 2.");
                    continue;
            }
        }
    }

}



