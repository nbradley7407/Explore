package com.explore.app;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
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
 * MAIN GOAL: finish get recommendations
 *     - Need to find good way of building the query off of given inputs
 *          - note: need '&' between params and '%2' between lists items (like artists)
 *     - Manually finding IDs for tracks, artists, albums, etc. is clunky. Is there a way to easily search them?
 *     - Maybe use Spotify's /search endpoint?
 * 
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
                        findNewMusic();
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

    //TODO
    // Should add an exit from anywhere
    private ArrayList<String> getRecommendations() {
        ArrayList<String> recs = new ArrayList<>();
        String market = "US";
        String recQuery = "recommendations?market=US";


        System.out.println("Enter values for each of the following, or press Enter to skip:");

        // limit input
        System.out.print("Limit number of recommendations (Enter a number between 1-100): ");
        String limitInput = scanner.nextLine();
        int limit = -1; // Default value or an invalid value
        if (!limitInput.isEmpty()) {
            limit = Integer.parseInt(limitInput);
            if (limit < 1 || limit > 100) {
                System.out.println("Invalid input for limit. It should be between 1 and 100");
                limit = -1; // Reset to default value
            } else {
                recQuery += "&limit=" + limitInput;
            }
        }

        // seed_artists input
        System.out.print("seed_artists (Enter an artist id): ");
        String seedArtistInput = scanner.nextLine();
        if (!seedArtistInput.isEmpty()) {
            recQuery += "&seed_artists=" + seedArtistInput;
        }

        // seed_genres input
        ArrayList<String> genreList = new ArrayList<>();
        while (true) {
            System.out.print("seed_genres (Enter a genre. To continue, press enter.): ");
            String seedGenresInput = scanner.nextLine();
            if (!seedGenresInput.isEmpty()) {
                    genreList.add(seedGenresInput);
            } else {
                break;
            }
        }
        // format genre list 
        if (!genreList.isEmpty()) {
            recQuery += "&seed_genres=";
            for (String genre : genreList) {
                recQuery += genre + "%2";
            }
            recQuery = recQuery.substring(0, (recQuery.length() - 2));
        }

        // seed_genres input
        ArrayList<String> trackList = new ArrayList<>();
        while (true) {
            System.out.print("seed_tracks (Enter a track id. To continue, press enter.): ");
            String seedTracksInput = scanner.nextLine();
            if (!seedTracksInput.isEmpty()) {
                    trackList.add(seedTracksInput);
            } else {
                break;
            }
        }
        // format genre list 
        if (!trackList.isEmpty()) {
            recQuery += "&seed_genres=";
            for (String track : trackList) {
                recQuery += track + "%2";
            }
            recQuery = recQuery.substring(0, (recQuery.length() - 2));
        }

        // target_acousticness input
        System.out.print("target_acousticness (Enter a number between 0.0-1.0): ");
        String acousticnessInput = scanner.nextLine();
        double targetAcousticness = -1; // Default value or an invalid value
        if (!acousticnessInput.isEmpty()) {
            targetAcousticness = Double.parseDouble(acousticnessInput);
            if (targetAcousticness < 0.0 || targetAcousticness > 1.0) {
                System.out.println("Invalid input for target_acousticness. It should be between 0.0 and 1.0");
                targetAcousticness = -1; // Reset to default value
            } else {
                recQuery += "target_acousticness=" + acousticnessInput + "&";
            }
        }

        // target_danceability input
        System.out.print("target_danceability (Enter a number between 0.0-1.0): ");
        String danceabilityInput = scanner.nextLine();
        double targetDanceability = -1;
        if (!danceabilityInput.isEmpty()) {
            targetDanceability = Double.parseDouble(danceabilityInput);
            if (targetDanceability < 0.0 || targetDanceability > 1.0) {
                System.out.println("Invalid input for target_danceability. It should be between 0.0 and 1.0");
                targetDanceability = -1;
            }
        }

        // target_duration_ms input
        System.out.print("target_duration_ms (Enter a positive number): ");
        String durationInput = scanner.nextLine();
        long targetDurationMs = -1;
        if (!durationInput.isEmpty()) {
            targetDurationMs = Long.parseLong(durationInput);
            if (targetDurationMs <= 0) {
                System.out.println("Invalid input for target_duration_ms. It should be a positive number");
                targetDurationMs = -1;
            }
        }

        // target_energy input
        System.out.print("target_energy (Enter a number between 0.0-1.0): ");
        String energyInput = scanner.nextLine();
        double targetEnergy = -1;
        if (!energyInput.isEmpty()) {
            targetEnergy = Double.parseDouble(energyInput);
            if (targetEnergy < 0.0 || targetEnergy > 1.0) {
                System.out.println("Invalid input for target_energy. It should be between 0.0 and 1.0");
                targetEnergy = -1;
            }
        }

        // target_instrumentalness input
        System.out.print("target_instrumentalness (Enter a number between 0.0-1.0): ");
        String instrumentalnessInput = scanner.nextLine();
        double targetInstrumentalness = -1;
        if (!instrumentalnessInput.isEmpty()) {
            targetInstrumentalness = Double.parseDouble(instrumentalnessInput);
            if (targetInstrumentalness < 0.0 || targetInstrumentalness > 1.0) {
                System.out.println("Invalid input for target_instrumentalness. It should be between 0.0 and 1.0");
                targetInstrumentalness = -1;
            }
        }

        // target_key input
        System.out.print("target_key (Enter an integer between 0-11): ");
        String keyInput = scanner.nextLine();
        int targetKey = -1;
        if (!keyInput.isEmpty()) {
            targetKey = Integer.parseInt(keyInput);
            if (targetKey < 0 || targetKey > 11) {
                System.out.println("Invalid input for target_key. It should be between 0 and 11");
                targetKey = -1;
            }
        }

        // target_liveness input
        System.out.print("target_liveness (Enter a number between 0.0-1.0): ");
        String livenessInput = scanner.nextLine();
        double targetLiveness = -1;
        if (!livenessInput.isEmpty()) {
            targetLiveness = Double.parseDouble(livenessInput);
            if (targetLiveness < 0.0 || targetLiveness > 1.0) {
                System.out.println("Invalid input for target_liveness. It should be between 0.0 and 1.0");
                targetLiveness = -1;
            }
        }

        // target_loudness input
        System.out.print("target_loudness (Enter a number between -60 to 0): ");
        String loudnessInput = scanner.nextLine();
        double targetLoudness = -1;
        if (!loudnessInput.isEmpty()) {
            targetLoudness = Double.parseDouble(loudnessInput);
            if (targetLoudness < -60 || targetLoudness > 0) {
                System.out.println("Invalid input for target_loudness. It should be between -60 and 0");
                targetLoudness = -1;
            }
        }

        // target_mode input
        System.out.print("target_mode (Enter 0 for minor, 1 for major): ");
        String modeInput = scanner.nextLine();
        int targetMode = -1;
        if (!modeInput.isEmpty()) {
            targetMode = Integer.parseInt(modeInput);
            if (targetMode != 0 && targetMode != 1) {
                System.out.println("Invalid input for target_mode. It should be 0 for minor or 1 for major");
                targetMode = -1;
            }
        }

        // target_popularity input
        System.out.print("target_popularity (Enter a number between 0-100): ");
        String popularityInput = scanner.nextLine();
        int targetPopularity = -1;
        if (!popularityInput.isEmpty()) {
            targetPopularity = Integer.parseInt(popularityInput);
            if (targetPopularity < 0 || targetPopularity > 100) {
                System.out.println("Invalid input for target_popularity. It should be between 0 and 100");
                targetPopularity = -1;
            }
        }

        // target_speechiness input
        System.out.print("target_speechiness (Enter a number between 0.0-1.0): ");
        String speechinessInput = scanner.nextLine();
        double targetSpeechiness = -1; 
        if (!speechinessInput.isEmpty()) {
            targetSpeechiness = Double.parseDouble(speechinessInput);
            if (targetSpeechiness < 0.0 || targetSpeechiness > 1.0) {
                System.out.println("Invalid input for target_speechiness. It should be between 0.0 and 1.0");
                targetSpeechiness = -1; 
            }
        }

        // target_tempo input
        System.out.print("target_tempo (Enter a positive number): ");
        String tempoInput = scanner.nextLine();
        double targetTempo = -1; 
        if (!tempoInput.isEmpty()) {
            targetTempo = Double.parseDouble(tempoInput);
            if (targetTempo <= 0) {
                System.out.println("Invalid input for target_tempo. It should be a positive number");
                targetTempo = -1; 
            }
        }

        // target_time_signature input
        System.out.print("target_time_signature (Enter a positive integer): ");
        String timeSignatureInput = scanner.nextLine();
        int targetTimeSignature = -1;
        if (!timeSignatureInput.isEmpty()) {
            targetTimeSignature = Integer.parseInt(timeSignatureInput);
            if (targetTimeSignature <= 0) {
                System.out.println("Invalid input for target_time_signature. It should be a positive integer");
                targetTimeSignature = -1;
            }
        }

        // target_valence input
        System.out.print("target_valence (Enter a number between 0.0-1.0): ");
        String valenceInput = scanner.nextLine();
        double targetValence = -1;
        if (!valenceInput.isEmpty()) {
            targetValence = Double.parseDouble(valenceInput);
            if (targetValence < 0.0 || targetValence > 1.0) {
                System.out.println("Invalid input for target_valence. It should be between 0.0 and 1.0");
                targetValence = -1; 
            }
        }

        String recsJson = get(recQuery);


        return recs;
    }

    // interface for finding music and getting music info
    private void findNewMusic(){
        while (true) {
            System.out.println("What would you like to do?");
            System.out.println("1: Get recommendations");
            System.out.println("2: See Genre Seeds");
            System.out.println("3: Get audio features of a trackID");
            System.out.println("4: Exit");
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
                    System.out.println("Enter the trackID you want to get features from");
                    String track = scanner.nextLine();
                    getAudioFeatures(track);
                    break;
                case 4:
                    return;
                default:
                    System.out.println("Invalid input. Please enter a number between x and y.");
                    break;
            }
        }
    }

    // list options of genre seeds to use in recommendations
    private void seeGenreSeeds() {
        String genreData = get("recommendations/available-genre-seeds");
        Gson gson = new Gson();
        JsonObject jsonObject = gson.fromJson(genreData, JsonObject.class);
        JsonArray itemsArray = jsonObject.getAsJsonArray("genres");
        for (JsonElement genre : itemsArray) {
            System.out.println(genre.getAsString());
        }
    }

    // see audio features of a given track id
    private void getAudioFeatures(String trackId) {
        String features = get("audio-features" + "?ids=" + trackId);
        ArrayList<String> featureList = parseJSON(features, "audio_features");
        for (String item : featureList) {
            System.out.println(item);
        }
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
    
}

