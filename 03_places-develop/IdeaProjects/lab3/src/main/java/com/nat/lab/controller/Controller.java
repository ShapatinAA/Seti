package com.nat.lab.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.nat.lab.app.AppModel;
import com.nat.lab.app.ModelException;
import com.nat.lab.app.State;

import javafx.fxml.FXML;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;

import java.io.UnsupportedEncodingException;
import java.util.concurrent.ExecutionException;

public class Controller {
    @FXML
    public ListView<String> listPlaces;
    @FXML
    public TextArea weather;
    @FXML
    public ListView<String> listPlacesNear;
    @FXML
    public TextArea info;
    @FXML
    public TextField searchBar;

    private final AppModel model;

    public Controller() {
        model = new AppModel();
    }

    public void updateSearchList() {
        listPlaces.getItems().clear();
        listPlacesNear.getItems().clear();
        weather.clear();
        info.clear();

        for (com.nat.lab.app.Place curPos : model.getListPlaces()) {
            listPlaces.getItems().add(curPos.toString());
        }

        listPlaces.refresh();
    }

    public void updateError() {
        if (model.getState() == State.SEARCH) {
            listPlaces.getItems().clear();
            listPlacesNear.getItems().clear();
            weather.clear();
            info.clear();
            listPlaces.getItems().add("No such place. Please, search something else!");
        } else {
            listPlacesNear.getItems().clear();
            weather.clear();
            info.clear();
            listPlacesNear.getItems().add("API don't have info about this place!");
        }
    }

    public void updateNearPlaceInfo() throws ExecutionException, InterruptedException {
        weather.clear();
        info.clear();
        listPlacesNear.getItems().clear();

        var weather = model.getWeather().get();
        this.weather.appendText(weather.toString());

        var nearbyPlaces = model.getListNearbyPlaces().get();

        if (nearbyPlaces.isEmpty()) {
            listPlacesNear.getItems().add("API don't have info about this place!");
        } else {
            for (com.nat.lab.app.PlaceInfo curPos : nearbyPlaces) {
                listPlacesNear.getItems().add(curPos.toString());
            }
        }
        listPlacesNear.refresh();
    }

    public void updatePlaceInfo() throws ExecutionException, InterruptedException {
        info.clear();

        var information = model.getInformation().get();

        if (information.getTitle() == null && information.getText() == null) {
            info.appendText("API don't have description about this place");
        } else {
            info.appendText(information.toString());
        }
    }

    private void handleSearch() throws UnsupportedEncodingException, ExecutionException, JsonProcessingException, InterruptedException, ModelException {
        model.setPlaceName(searchBar.getText());
        model.searchPossiblePlaces();
        updateSearchList();

        listPlaces.setOnMouseClicked(event -> {
            try {
                model.searchAdditionInfo(listPlaces.getSelectionModel().getSelectedIndex());
                updateNearPlaceInfo();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        listPlacesNear.setOnMouseClicked(event -> {
            try {
                model.setInfoAboutPlace(listPlacesNear.getSelectionModel().getSelectedIndex());
                updatePlaceInfo();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    @FXML
    protected void keyListener(KeyEvent event)  {
        if (event.getCode() == KeyCode.ENTER) {
            try {
                handleSearch();
            } catch (Exception e) {
                e.printStackTrace();
                updateError();
            }
        }
    }

    @FXML
    protected void mouseListener()  {
        try {
            handleSearch();
        } catch (Exception e) {
            e.printStackTrace();
            updateError();
        }
    }

}
