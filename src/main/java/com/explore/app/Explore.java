package com.explore.app;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import java.util.ArrayList;
import java.util.Scanner;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStreamReader;
import com.google.gson.Gson; 
import com.google.gson.JsonObject; 
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;  


/* TODO
 * method to search recommendations
 * CRUD methods for "My Explore" playlist songs
 * figure out how to do Auth without copy/pasting into terminal manually
 * handling of refresh tokens
 */


public class Explore {

    private Scanner scanner;
    private Properties config;
    private String userId;
    private String clientId;
    private String redirectURI;
    private String encoded;
    private String accessToken;

    public Explore() {
        this.scanner     = new Scanner(System.in);
        this.config      = loadConfig("config.properties");
        this.userId      = config.getProperty("user.id");
        this.clientId    = config.getProperty("client.id");
        this.redirectURI = config.getProperty("redirect.uri");
        this.encoded     = config.getProperty("encoded");
    }
    public static void main(String[] args) {
        Explore explore = new Explore();
        explore.getAccessToken();
        explore.checkPlaylists();
        explore.mainLoop();
        explore.scanner.close();
    }

    // Checks if "My Explore" exists in your playlists. Creates it if it doesn't
    // WARNING - Spotify API takes some time to recognize this new playlist. May recreate if ran again too soon
    private void checkPlaylists() {
        String explorePlaylist = get("me", "playlists");
        ArrayList<String> myPlaylists = parseJSON(explorePlaylist);
        for (String item : myPlaylists) {
            System.out.println(item);
        }
        if (!myPlaylists.contains("My Explore")) {
            System.out.println("Creating \"My Explore\" playlist.");
            createPlaylist();
        } 
    }
    
    // unused
    private String[] getRecommendations(String args) {
        return null;
    }

    //unused (mostly for testing)
    private String getArtistName(String artistId) {
        String response = get("artists", artistId);
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

    // getter for JSON response
    private String get(String subject, String propertyId) {
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

    //TODO
    private void createPlaylist() {
        try {
            URL url = new URL("https://api.spotify.com/v1/users/" + userId + "/playlists");
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + accessToken);
            conn.setDoOutput(true);
            String jsonInputString = "{\"name\": \"My Explore\", \"public\": \"false\"}";
            try(OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);			
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Interface for navigation through the program
    private void mainLoop() {
        while (true) {
            System.out.println("What would you like to do?");
            System.out.println("1: Edit Explore playlist");
            System.out.println("2: Find new music");
            System.out.println("3: Exit");
            int option = scanner.nextInt();
    
            switch (option) {
                case 1:
                    //TODO Handle option 1 - Edit Explore playlist
                    break;
                case 2:
                    //TODO Handle option 2 - Find new music
                    break;
                case 3:
                    scanner.close(); // Exit the entire program
                    return;
                default:
                    System.out.println("Invalid input. Please enter a number between 1 and 3.");
                    break;
            }
        }
    }

    // Use standard output to help user run bash script to retrieve access token
    private void getAccessToken() {
        try {    
            // Get authorization code from user
            System.out.println("--------------------------------------------------------------------------------------------------- \n");
            System.out.println("Please visit the following URL and authorize the application: \n\n" +
                    "https://accounts.spotify.com/authorize?"
                    + "response_type=code"
                    + "&client_id=" + clientId
                    + "&scope=playlist-modify-private"
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
    
    private Properties loadConfig(String filePath) {
        // loads variables declared in filePath

        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(filePath)) {
            properties.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return properties;
    }

    private ArrayList<String> parseJSON(String json) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
        ArrayList<String> jsonItems = new ArrayList<>();

        if (jsonObject.has("items") && jsonObject.get("items").isJsonArray()) {
            JsonArray itemsArray = jsonObject.getAsJsonArray("items");
            for (JsonElement itemElement : itemsArray) {
                if (itemElement.isJsonObject()) {
                    JsonObject playlistObject = itemElement.getAsJsonObject();
                    String playlistName = playlistObject.get("name").getAsString();
                    jsonItems.add(playlistName);
                }
            }
        } else {
            System.out.println("No playlists found in the JSON response.");
        }
        return jsonItems;
    }

}

