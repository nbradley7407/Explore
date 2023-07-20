package com.explore.app;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.util.Scanner;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import com.google.gson.Gson; 
import com.google.gson.JsonObject; 
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;  





/* Need to first log in
 * need a playlist to dump songs into
 * methods to clear, delete, add to playlist
 * search songs by given inputs
 * print inputs needed/required
 */


public class App {
    static Scanner scanner;
    static Properties config;
    static String clientId;
    static String clientSecret;
    static String redirectURI;
    static String encoded;
    static String accessToken;


    static {
        scanner = new Scanner(System.in);
        config = loadConfig("config.properties");
        clientId = config.getProperty("client.id");
        clientSecret = config.getProperty("client.secret");
        redirectURI = config.getProperty("redirect.uri");
        encoded = config.getProperty("encoded");
    }
    public static void main(String[] args) {
        getAccessToken();

        // TO DO: check to see if there's a playlist called "Explore." If there is, save the ID#. If there isn't, make one and save the ID#
        String explorePlaylist = get(accessToken, "me", "playlists");
        parseJSON(explorePlaylist);
        

        // Go to interface
        mainLoop();

        scanner.close();
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
            System.out.println("What would you like to do?");
            System.out.println("1: Edit Explore playlist");
            System.out.println("2: Find new music");
            System.out.println("3: Exit");
            int option = scanner.nextInt();
    
            switch (option) {
                case 1:
                    // Handle option 1 - Edit Explore playlist
                    break;
                case 2:
                    // Handle option 2 - Find new music
                    break;
                case 3:
                    scanner.close(); // Close the Scanner only when exiting the loop
                    return;
                default:
                    System.out.println("Invalid input. Please enter a number between 1 and 3.");
                    break;
            }
        }
    }
    
    public static void getAccessToken() {
        // Uses standard output to help user run bash script to retrieve access token

        try {    
            // Get authorization code from user
            System.out.println("--------------------------------------------------------------------------------------------------- \n");
            System.out.println("Please visit the following URL and authorize the application: \n\n" +
                    "https://accounts.spotify.com/authorize?"
                    + "response_type=code"
                    + "&client_id=" + clientId
                    + "&scope=playlist-read-private"
                    + "&redirect_uri=" + redirectURI);
            System.out.println("\n\nEnter the code from the redirected URL: \n\n");
            String code = scanner.nextLine();

            // get access token
            System.out.println("\n\nRun the following bash script:\n\n");
            System.out.println("curl -H \"Authorization: Basic " + encoded + "\" "
            + "-d grant_type=authorization_code "
            + "-d code=" + code + " "
            + "-d redirect_uri=" + redirectURI + " https://accounts.spotify.com/api/token");
            System.out.println("\n\nEnter the given access token: ");
            accessToken = scanner.nextLine();

        } catch (Exception e) {
            e.printStackTrace();
    }
    }
    public static Properties loadConfig(String filePath) {
        // loads variables declared in filePath

        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(filePath)) {
            properties.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

    public static void parseJSON(String json) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);

        if (jsonObject.has("items") && jsonObject.get("items").isJsonArray()) {
            JsonArray itemsArray = jsonObject.getAsJsonArray("items");
            for (JsonElement itemElement : itemsArray) {
                if (itemElement.isJsonObject()) {
                    JsonObject playlistObject = itemElement.getAsJsonObject();
                    String playlistName = playlistObject.get("name").getAsString();
                    System.out.println(playlistName);
                }
            }
        } else {
            System.out.println("No playlists found in the JSON response.");
        }
    }

}

