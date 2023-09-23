package com.explore.app;

import java.util.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.StringBuilder;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.io.InputStreamReader;
import com.google.gson.Gson; 
import com.google.gson.JsonObject; 
import com.google.gson.JsonElement;
import com.google.gson.JsonArray;  
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;


/* TODO
 * Tidy up getting recommendations
 *     - Manually finding IDs for tracks, artists, albums, etc. is clunky. Is there a way to easily search them? Maybe use Spotify's /search endpoint?
 *     - Might be nice to be able to have an option to print out a description of every parameter
 * 
 * Presets for recommendations (happy/sad, upbeat/mellow, workout, etc.)
 * CRUD methods for "My Explore" playlist songs
 * handling of refresh tokens
 * 
 * Maybe throw this in a Docker container when it's done
 */


public class Explore {

    private Scanner scanner;
    private Properties config;
    private String userId;
    private String clientId;
    private String redirectURI;
    private String clientSecret;
    private String accessToken;
    private String myExplorePlaylistId;
    private String upbeatPreset;
    private String mellowPreset;

    public Explore() {
        this.scanner     = new Scanner(System.in);
        this.config      = loadConfig("config.properties");
        this.userId      = config.getProperty("user.id");
        this.clientId    = config.getProperty("client.id");
        this.clientSecret= config.getProperty("client.secret");
        this.redirectURI = config.getProperty("redirect.uri");
        this.accessToken = config.getProperty("access.token");
    }
    public static void main(String[] args) {
        Explore explore = new Explore();
        try {
            explore.checkPlaylists();
        } catch (Exception e) {
            explore.getAccessToken();
            explore.checkPlaylists();
        }
        explore.mainLoop();
        explore.scanner.close();

    }

    // Interface for navigation through the program
    private void mainLoop() {
        while (true) {
            System.out.println("What would you like to do?");
            System.out.println("1: Edit Explore playlist");
            System.out.println("2: Find new music");
            System.out.println("3: Exit");
    
            if (scanner.hasNextInt()) {
                int option = scanner.nextInt();
                scanner.nextLine(); // Consume the newline character after reading the integer
    
                switch (option) {
                    case 1:
                        // TODO: Handle option 1 - Edit Explore playlist
                        break;
                    case 2:
                        exploreMusic();
                        break;
                    case 3:
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

    // Checks if "My Explore" exists in your playlists. Creates it if it doesn't
    private void checkPlaylists() {
        String myPlaylistData = getJsonString("me/playlists");
        ArrayList<String> myPlaylists = parseJSON(myPlaylistData, "name");
        System.out.println("________________________________________________________________________________________________________");
        System.out.println("\nYour playlists:\n");
        for (String item : myPlaylists) {
            System.out.println(item);
        }
        System.out.println();
        if (!myPlaylists.contains("My Explore")) {
            System.out.println("Creating \"My Explore\" playlist.");
            createPlaylist();
        } 
    
        // get the id
        try {
            Gson gson = new Gson();
            JsonObject jsonPlaylistData = gson.fromJson(myPlaylistData, JsonObject.class);
            JsonArray jsonPlaylistArray = jsonPlaylistData.getAsJsonArray("items");
    
            for (int i = 0; i < jsonPlaylistArray.size(); i++) {
                JsonObject item = jsonPlaylistArray.get(i).getAsJsonObject();
                String name = item.get("name").getAsString();
                if (name.equals("My Explore")) {
                    myExplorePlaylistId = item.get("id").getAsString();
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    //TODO - add an exit from anywhere
    private void getRecommendations() {
        // initialize reccomendations query with the market Id
        StringBuilder recQuery = new StringBuilder();
        recQuery.append("recommendations?market=US");

        System.out.println("Enter values for each of the following, or press Enter to skip:");

        // limit input (required, default is 10)
        int limit = 10;
        while (true) {
            System.out.print("Limit number of recommendations (Enter a number between 1-100): ");
            String limitInput = scanner.nextLine();
            if (!limitInput.isEmpty()) {
                try {
                    limit = Integer.parseInt(limitInput);
                    if (limit < 1 || limit > 100) {
                        System.out.println("Invalid input for limit. It should be between 1 and 100");
                        limit = 10;
                    } else {
                        recQuery.append("&limit=").append(limitInput);
                        break;
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input for limit. Please enter a valid integer.");
                }

            } else {
                recQuery.append("&limit=10");
                break;
            }
        }

        // seed_artists input
        handleMultiParameter("seed_artists", "seed_artists (Enter one at a time, up to 5. To continue, press enter.): ", recQuery);

        // seed_genres input
        handleMultiParameter("seed_genres", "seed_genres (Enter one at a time, up to 5. To continue, press enter.): ", recQuery);

        // seed_tracks input
        handleMultiParameter("seed_tracks", "seed_tracks (Enter one at a time, up to 5. To continue, press enter.): ", recQuery);

        // target_acousticness input
        handleDoubleParameter("target_acousticness", 0.0, 1.0, "target_acousticness (Enter a number between 0.0-1.0): ", recQuery);

        // target_danceability input
        handleDoubleParameter("target_danceability", 0.0, 1.0, "target_danceability (Enter a number between 0.0-1.0): ", recQuery);

        // target_duration_ms input
        handleIntParameter("target_duration_ms", 1, 1200000, "target_duration_ms (Enter a positive number): ", recQuery);

        // target_energy input
        handleDoubleParameter("target_energy", 0.0, 1.0, "target_energy (Enter a number between 0.0-1.0): ", recQuery);

        // target_instrumentalness input
        handleDoubleParameter("target_instrumentalness", 0.0, 1.0, "target_instrumentalness (Enter a number between 0.0-1.0): ", recQuery);

        // target_key input
        handleIntParameter("target_key", 0, 11, "target_key (Enter a number between 0-11): ", recQuery);

        // target_liveness input
        handleDoubleParameter("target_liveness", 0.0, 1.0, "target_liveness (Enter a number between 0.0-1.0): ", recQuery);

        // target_loudness input
        handleIntParameter("target_loudness", -60, 0, "target_loudness (Enter a number between -60-0): ", recQuery);

        // target_mode input
        handleIntParameter("target_mode", 0, 1, "target_mode (Enter 0 for minor, 1 for major): ", recQuery);

        // target_popularity input
        handleIntParameter("target_popularity", 0, 100, "target_popularity (Enter a number between 0-100): ", recQuery);

        // target_speechiness input
        handleDoubleParameter("target_speechiness", 0.0, 1.0, "target_speechiness (Enter a number between 0.0-1.0): ", recQuery);

        // target_tempo input
        handleIntParameter("target_tempo", 0, 1000, "target_tempo (Enter a positive integer): ", recQuery);

        // target_time_signature input
        handleIntParameter("target_time_signature", 0, 10000, "target_time_signature (Enter a positive integer): ", recQuery);

        // target_valence input
        handleDoubleParameter("target_valence", 0.0, 1.0, "target_valence (Enter a number between 0.0-1.0): ", recQuery);

        // make the recommendations query and then store in a Json array
        String recsDataString = getJsonString(recQuery.toString());
        if (recsDataString == null) {
            return;
        }
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(recsDataString, JsonObject.class);
        JsonArray tracksArray = jsonObject.getAsJsonArray("tracks");
        int n = tracksArray.size();

        // map numbers to the track ids and print out track info
        System.out.println();
        HashMap<String,String> recsMap = new HashMap<>();
        for (int i=0;i<n;i++) {
            JsonObject trackObject = tracksArray.get(i).getAsJsonObject();
            String trackName = trackObject.get("name").getAsString();
            String trackId = trackObject.get("id").getAsString();
            recsMap.put(Integer.toString(i+1), trackId);
            String artistName = trackObject.getAsJsonArray("artists").get(0).getAsJsonObject().get("name").getAsString();
            String previewUrl = "Preview not available.";
            if (!trackObject.get("preview_url").isJsonNull()) {
                previewUrl = trackObject.get("preview_url").getAsString();
            } 

            System.out.println((i+1) + " " + trackName + " by " + artistName + ".");
            System.out.println("Preview: " + previewUrl);
            System.out.println("Track Id: " + trackId +"\n\n");
        }

        // interface for deciding which tracks to add to My Explore
        Set<String> recsSet = new HashSet<>();
        System.out.println("Which track would you like to add? (Enter a number between 1 and " + limit + ", 0 to add all,");
        System.out.println("or press Enter to continue.");
        while (true) {
            String option = scanner.nextLine();
            if (!option.isEmpty()) {
                if (option.equals("0")) {
                    for (String id : recsMap.values()) {
                        recsSet.add(id);
                    }
                    System.out.println("Selected all tracks");
                    break;
                } else if (recsMap.keySet().contains(option)) {
                    recsSet.add(recsMap.get(option));
                    System.out.println("Added track " + option + " to selections");
                } else {
                    System.out.println("Invalid input. Please enter a number between 0 and " + n);
                }
            } else {
                break;
            }
        }

        // add tracks to My Explore playlist
        if (!recsSet.isEmpty()) {
            System.out.println("\nWhat would you like to do with your selections?");
            System.out.println("1. Add your recommendations to My Explore");
            System.out.println("2. Clear My Explore and then add your recommendations");
            System.out.println("3. Exit with no actions");
            while (true) {
                String option = scanner.nextLine();
                if (option.equals("1")) {
                    addRecommendations(recsSet, "POST");
                    break;
                } else if (option.equals("2")) {
                    addRecommendations(recsSet, "PUT");
                    break;
                } else if (option.equals("3")) {
                    return;
                } else {
                    System.out.println("Invalid input. Please enter 1, 2 or 3.");
                }
            }
        }
    }

    // prints Song - Artist : id
    private void seePlaylistSongIds(String playlistId) {
        String playlistSongs = getJsonString("playlists/" + playlistId + "?fields=tracks.items%28track%28name%2Cartists%28name%29%2Cid%29%29");

        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(playlistSongs, JsonObject.class);
        JsonObject tracksObject = jsonObject.getAsJsonObject("tracks");
        JsonArray tracksArray = tracksObject.getAsJsonArray("items");
        int n = tracksArray.size();

        System.out.println();
        for (int i=0;i<n;i++) {
            JsonObject trackObject = tracksArray.get(i).getAsJsonObject();
            JsonObject trackInfo = trackObject.getAsJsonObject("track");
            String trackName = trackInfo.get("name").getAsString();
            String trackArtist = trackInfo.getAsJsonArray("artists").get(0).getAsJsonObject().get("name").getAsString();
            String trackId = trackInfo.get("id").getAsString();

            String formattedLine = String.format("%-30s by: %-30s id: %s", trackName, trackArtist, trackId);
            System.out.println(formattedLine);
        }
    }

    //TODO : validate inputs
    //handles parameters that take up to 5 strings
    private void handleMultiParameter(String parameter, String message, StringBuilder result){
        ArrayList<String> paramList = new ArrayList<>();
        while (true) {
            System.out.print(message);
            String paramInput = scanner.nextLine();
            if (!paramInput.isEmpty()) {
                paramList.add(paramInput);
                if (paramList.size() == 5) {
                    break;
                }
            } else {
                break;
            }
        }

        // format paramList 
        if (!paramList.isEmpty()) {
            result.append("&").append(parameter).append("=");
            for (int i = 0; i < paramList.size() - 1; i++) {
                result.append(paramList.get(i)).append("%2C");
            }
            result.append(paramList.get(paramList.size() - 1)); // Append the last element without the separator
        }
    }

    private void handleIntParameter(String parameter, int minParam, int maxParam, String message, StringBuilder result) {
        System.out.print(message);
        while (true) {
            String paramInput = scanner.nextLine();
            if (!paramInput.isEmpty()) {
                try {
                    int param = Integer.parseInt(paramInput);
                    if (param < minParam || param > maxParam) {
                        System.out.println("Invalid input for " + parameter + ". It should be between " + minParam + " and " + maxParam + ".");
                    } else {
                        result.append("&").append(parameter).append("=").append(param); // Add string to recQuery
                        return;
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input for " + parameter + ". Please enter a valid integer.");
                }
            } else {
                return; // No input, leave the result as it was
            }
        }
    }

    private void handleDoubleParameter(String parameter, Double minParam, Double maxParam, String message, StringBuilder result){
        System.out.print(message);
        while (true) {
            String paramInput = scanner.nextLine();
            if (!paramInput.isEmpty()) {
                try {
                    Double param = Double.parseDouble(paramInput);
                    if (param < minParam || param > maxParam) {
                        System.out.println("Invalid input for " + parameter + ". It should be between " + minParam + " and " + maxParam + ".");
                    } else {
                        result.append("&").append(parameter).append("=").append(param); // Add string to recQuery
                        return;
                    }
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input for " + parameter + ". Please enter a valid Double.");
                }
            } else {
                return; // No input, leave the result as it was
            }
        }
    }

    // interface for finding music and getting music info
    private void exploreMusic() {
        while (true) {

            System.out.println("What would you like to do?");
            System.out.println("1: Get recommendations");
            System.out.println("2: See Genre Seeds");
            System.out.println("3: Get audio features of a trackID");
            System.out.println("4: Get track ids from a playlist");
            System.out.println("5: Main Menu");
            int option = scanner.nextInt();
            scanner.nextLine();
            
            switch (option) {
                case 1:
                    getRecommendations();
                    break;
                case 2:
                    seeGenreSeeds();
                    break;
                case 3:
                    System.out.println("Enter the track ID you want to get features from");
                    String track = scanner.nextLine();
                    getAudioFeatures(track);
                    break;
                case 4:
                    System.out.println("Enter the playlist ID you want to get track Ids from");
                    String seePlaylistId = scanner.nextLine();
                    seePlaylistSongIds(seePlaylistId);
                    break;
                case 5:
                    return;
                default:
                    System.out.println("Invalid input. Please enter a number between x and y.");
                    break;
            }
        }
    }

    // adds trackIdSet items to My Explore playlist
    private void addRecommendations(Set<String> trackIdSet, String method) {
        try {
            String url = "https://api.spotify.com/v1/playlists/" + myExplorePlaylistId + "/tracks";
            URL obj = new URL(url);
            HttpURLConnection con = (HttpURLConnection) obj.openConnection();
            con.setRequestMethod(method);
            con.setRequestProperty("Authorization", "Bearer " + accessToken);
            con.setRequestProperty("Content-Type", "application/json");

            String requestBody = "{\"uris\": [";
            for (String trackIdString : trackIdSet) {
                requestBody += "\"spotify:track:" + trackIdString + "\",";
            }
            requestBody = requestBody.substring(0, requestBody.length() - 1);
            requestBody += "]}";

            con.setDoOutput(true);
            OutputStream wr = con.getOutputStream();
            byte [] bodyBytes = requestBody.getBytes();
            wr.write(bodyBytes);
            wr.flush();
            wr.close();

            if (con.getResponseCode() == 200) {
                System.out.println("Added all tracks to My Explore");
            }
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Your recommendations have been added to My Explore.\n");
    }

    // list options of genre seeds to use in recommendations
    private void seeGenreSeeds() {
        String genreData = getJsonString("recommendations/available-genre-seeds");
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(genreData, JsonObject.class);
        JsonArray itemsArray = jsonObject.getAsJsonArray("genres");
        for (JsonElement genre : itemsArray) {
            System.out.println(genre.getAsString());
        }
    }

    // see audio features of a given track id
    private void getAudioFeatures(String trackId) {
        String features = getJsonString("audio-features" + "?ids=" + trackId);
        ArrayList<String> featureList = parseJSON(features, "audio_features");
        for (String item : featureList) {
            System.out.println(item);
        }
    }

    // simple getter for JSON response
    private String getJsonString(String endpoint) {
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

                return response.toString();
            } else if (responseCode == 204) {
                System.out.println("No Content - The request has succeeded but returns no message body.");
            } else {
                System.out.println("\nRequest failed with HttpResponseCode " + responseCode + ": " + conn.getResponseMessage());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

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
            System.out.println("\n\nEnter the entire redirected URL: \n\n");
            String code = scanner.nextLine().substring(37);
            
            try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
                String authorizationHeader = Base64.getEncoder().encodeToString((clientId + ":" + clientSecret).getBytes());
                HttpPost httpPost = new HttpPost("https://accounts.spotify.com/api/token");
                httpPost.setHeader("Authorization", "Basic " + authorizationHeader);
    
                List<NameValuePair> params = new ArrayList<>();
                params.add(new BasicNameValuePair("grant_type", "authorization_code"));
                params.add(new BasicNameValuePair("code", code));
                params.add(new BasicNameValuePair("redirect_uri", redirectURI));
                httpPost.setEntity(new UrlEncodedFormEntity(params));
    
                HttpResponse response = httpclient.execute(httpPost);

                if (response.getStatusLine().getStatusCode() == 200) {
                    String jsonResponse = EntityUtils.toString(response.getEntity());
                    JSONObject jsonObject = new JSONObject(jsonResponse);
                    accessToken = jsonObject.getString("access_token");
                    System.out.println("Your access token: " + accessToken);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

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
    
}

