package com.nat.lab.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.Getter;
import lombok.Setter;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;

public class AppModel {
    private final static String keyOpenTrip = "5ae2e3f221c38a28845f05b6997afb2a1337c477897ee679ca8ac4c3";
    private final static String keyGraphHopper = "85bede4c-a486-490d-baf8-1468ce2ae8cf";
    private final static String keyOpenWeather = "71325e7beb3e7c702ad68697d6b7489d";
    private final static String urlOpenTrip = "https://api.opentripmap.com/0.1/en/places/";
    private final static String urlGraphHopper = "https://graphhopper.com/api/1/geocode";
    private final static String urlOpenWeather = "https://api.openweathermap.org/data/2.5/weather";

    @Setter
    private String placeName;
    @Getter
    private CopyOnWriteArrayList<Place> listPlaces;
    @Getter
    private CompletableFuture<CopyOnWriteArrayList<PlaceInfo>> listNearbyPlaces;
    @Getter
    private CompletableFuture<WeatherInfo> weather;
    @Getter
    private CompletableFuture<Info> information;
    @Getter
    State state;
    private boolean ifSearchError;

    private HttpResponse<String> body(HttpResponse<String> response) {
        if (response.statusCode() != 200) {
            ifSearchError = true;
        }
        return response;
    }

    private String getSearchResponse() throws ExecutionException, InterruptedException, UnsupportedEncodingException {
        var client = HttpClient.newHttpClient();
        var placeNameUTF = URLEncoder.encode(placeName, StandardCharsets.UTF_8.toString());
        var requestURIString = String.format("%s?q=%s&key=%s", urlGraphHopper, placeNameUTF, keyGraphHopper);
        var request = HttpRequest.newBuilder(
                        URI.create(requestURIString))
                .header("accept", "application/json")
                .build();
        System.out.println(requestURIString);
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::body).thenApply(HttpResponse::body).get();
    }


    private CompletableFuture<CopyOnWriteArrayList<PlaceInfo>> getNearPlaceResponse(Place place)  {
        var client = HttpClient.newHttpClient();
        var requestURIString = String.format("%sradius?radius=1000&lon=%s&lat=%s&units=metric&apikey=%s", urlOpenTrip, place.getLng(), place.getLat(), keyOpenTrip);
        var request = HttpRequest.newBuilder(
                        URI.create(requestURIString))
                .header("accept", "application/json")
                .build();
        System.out.println(requestURIString);
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::body).thenApply(HttpResponse::body).thenApply(CustomJsonParser::parseNearbyPlaceJson);
    }

    private CompletableFuture<Info> getInfoResponse(PlaceInfo place) {
        var client = HttpClient.newHttpClient();
        var requestURIString = String.format("%sxid/%s?apikey=%s", urlOpenTrip, place.getXid(), keyOpenTrip);
        var request = HttpRequest.newBuilder(
                        URI.create(requestURIString))
                .header("accept", "application/json")
                .build();
        System.out.println(requestURIString);
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::body).thenApply(HttpResponse::body).thenApply(CustomJsonParser::parseInfo);
    }


    private CompletableFuture<WeatherInfo> getWeatherResponse(Place place) {
        var client = HttpClient.newHttpClient();
        var requestURIString = String.format("%s?lat=%s&lon=%s&units=metric&appid=%s", urlOpenWeather, place.getLat(), place.getLng(), keyOpenWeather);
        var request = HttpRequest.newBuilder(
                        URI.create(requestURIString))
                .header("accept", "application/json")
                .build();
        System.out.println(requestURIString);
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                .thenApply(this::body).thenApply(HttpResponse::body).thenApply(CustomJsonParser::parseWeatherJson);
    }


    public void searchAdditionInfo(int index) throws ModelException {
        ifSearchError = false;
        state = State.INFO;

        Place place = listPlaces.get(index);
        weather = getWeatherResponse(place);
        listNearbyPlaces = getNearPlaceResponse(place);
        if (ifSearchError) {
            throw new ModelException("Bad response");
        }
    }

    public void setInfoAboutPlace(int index) throws ExecutionException, InterruptedException {
        information = getInfoResponse(listNearbyPlaces.get().get(index));
    }

    private void setPlaces() throws JsonProcessingException, ExecutionException, InterruptedException, UnsupportedEncodingException, ModelException {
        listPlaces = new CopyOnWriteArrayList<>();
        ifSearchError = false;

        var response = getSearchResponse();

        if (ifSearchError) {
            throw new ModelException("Bad response");
        }
        this.listPlaces = CustomJsonParser.parseGraphHopperJson(response);
    }

    public void searchPossiblePlaces() throws UnsupportedEncodingException, ExecutionException, JsonProcessingException, InterruptedException, ModelException {
        state = State.SEARCH;
        setPlaces();
    }
}
