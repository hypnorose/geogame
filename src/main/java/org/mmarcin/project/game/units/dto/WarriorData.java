package org.mmarcin.project.game.units.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;

public class WarriorData {
    public String name;
    public String lat;
    public String lon;

    public WarriorData() {

    }

    public WarriorData(String name, String lat, String lon) {
        this.name = name;
        this.lat = lat;
        this.lon = lon;
    }
}
