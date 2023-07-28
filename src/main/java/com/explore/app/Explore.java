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
import com.google.gson.JsonParser;


/* TODO
 * MAIN GOAL: finish get recommendations
 *     - Manually finding IDs for tracks, artists, albums, etc. is clunky. Is there a way to easily search them?
 *     - GET /search ?
 * Would be good to utilize GET /audio-features to understand Spotify's ranking system
 * CRUD methods for "My Explore" playlist songs
 * figure out how to do Auth without copy/pasting into terminal manually
 * handling of refresh tokens
 * 
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
    private void checkPlaylists() {
        String myPlaylistData = get("me/playlists");
        ArrayList<String> myPlaylists = parseJSON(myPlaylistData, "name");
        for (String item : myPlaylists) {
            System.out.println(item);
        }
        if (!myPlaylists.contains("My Explore")) {
            System.out.println("Creating \"My Explore\" playlist.");
            createPlaylist();
        } 
    }
    
    //TODO
    private ArrayList<String> getRecommendations(int limit) {      
        String market = "US";  
        try {
            String apiUrl = "https://api.spotify.com/v1/recommendations";
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
                String raw = response.toString();
                ArrayList<String> res = parseJSON(raw, "name");
                return res;

            } else {
                throw new RuntimeException("HttpResponseCode: " + responseCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;

        /* 
         * for (int i=0;i<recommendationNames.size();i++) {
            System.out.println(recommendationData);
        }
        */
    }

    private void findNewMusic(){
        while (true) {
            System.out.println("What would you like to do?");
            System.out.println("1: Get recommendations");
            System.out.println("2: See Genre Seeds");
            System.out.println("3: Exit");
            int option = scanner.nextInt();
            scanner.nextLine();
    
            switch (option) {
                case 1:
                    getRecommendations(10);
                    break;
                case 2:
                    seeGenreSeeds();
                    break;
                case 3:
                    return;
                default:
                    System.out.println("Invalid input. Please enter a number between x and y.");
                    break;
            }
        }
    }

    //unused (mostly for testing)
    private String getArtistName(String artistId) {
        String response = get("artists" + "/" + artistId);
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

    // getter for basic JSON response
    private String get(String endpoint) {
        try {
            String apiUrl = "https://api.spotify.com/v1/" + endpoint;
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

    // create "My Explore" playlist
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
            System.out.println("3: Get audio features of a trackID");
            System.out.println("4: Exit");
    
            if (scanner.hasNextInt()) {
                int option = scanner.nextInt();
                scanner.nextLine(); // Consume the newline character after reading the integer
    
                switch (option) {
                    case 1:
                        // TODO: Handle option 1 - Edit Explore playlist
                        break;
                    case 2:
                        findNewMusic();
                        break;
                    case 3:
                        System.out.println("Enter the trackID you want to get features from");
                        String track = scanner.nextLine();
                        getAudioFeatures(track);
                        break;
                    case 4:
                        scanner.close(); // Exit the entire program
                        return;
                    default:
                        System.out.println("Invalid input. Please enter a number between 1 and 3.");
                        break;
                }
            } else {
                scanner.nextLine();
                System.out.println("Invalid input. Please enter a number between 1 and 4.");
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
                    + "&scope=playlist-modify-private+playlist-read-private"
                    + "&redirect_uri=" + redirectURI);
            System.out.println("\n\nEnter the code from the redirected URL: \n\n");
            String code = scanner.nextLine();

            // get access token
            System.out.println("\n\nRun the following bash script:\n\n");
            System.out.println("echo\n\ncurl -s -H \"Authorization: Basic " + encoded + "\" "
            + "-d grant_type=authorization_code "
            + "-d code=" + code + " "
            + "-d redirect_uri=" + redirectURI + " https://accounts.spotify.com/api/token " 
            + "| jq -r '.access_token'");
            System.out.println("\n\nEnter the given access token: ");
            accessToken = scanner.nextLine();

        } catch (Exception e) {
            e.printStackTrace();
    }
    }
    
    // load sensitive data
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

    // returns a list of specified key values from Json data
    private ArrayList<String> parseJSON(String json, String key) {
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(json, JsonObject.class);
        ArrayList<String> jsonItems = new ArrayList<>();
    
        if (jsonObject.has("items") && jsonObject.get("items").isJsonArray()) {
            JsonArray itemsArray = jsonObject.getAsJsonArray("items");
            for (JsonElement itemElement : itemsArray) {
                if (itemElement.isJsonObject()) {
                    JsonObject itemData = itemElement.getAsJsonObject();
                    jsonItems.add(itemData.get(key).getAsString());
                }
            }
        } else if (jsonObject.has(key)) {
            JsonElement element = jsonObject.get(key);
            if (element.isJsonArray()) {
                JsonArray jsonArray = element.getAsJsonArray();
                for (JsonElement jsonElement : jsonArray) {
                    if (jsonElement.isJsonObject()) {
                        JsonObject itemData = jsonElement.getAsJsonObject();
                        jsonItems.add(itemData.toString());
                    }
                }
            } else {
                jsonItems.add(element.getAsString());
            }
        } else {
            System.out.println("None found in the JSON response.");
        }
        return jsonItems;
    }
    
    

    private void seeGenreSeeds() {
        String genreData = get("recommendations/available-genre-seeds");
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(genreData, JsonObject.class);
        JsonArray itemsArray = jsonObject.getAsJsonArray("genres");
        for (JsonElement genre : itemsArray) {
            System.out.println(genre.getAsString());
        }
    }

    private void getAudioFeatures(String trackId) {
        String features = get("audio-features" + "?ids=" + trackId);
    ArrayList<String> featureList = parseJSON(features, "audio_features");
    for (String item : featureList) {
        // Parse the JSON string and pretty-print it
        JsonParser parser = new JsonParser();
        JsonElement jsonElement = parser.parse(item);
        Gson gson = new Gson();
        String prettyJson = gson.toJson(jsonElement);
        System.out.println(prettyJson);
    }
    }
}

