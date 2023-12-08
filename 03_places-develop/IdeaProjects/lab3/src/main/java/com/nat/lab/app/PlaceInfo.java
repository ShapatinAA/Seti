package com.nat.lab.app;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public class PlaceInfo {
    @Getter
    String name;
    @Getter
    String xid;

    @Override
    public String toString() {
        return String.format(" Place name: %s ", name);
    }
}
