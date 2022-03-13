package net.eltown.apiserver.components.handler.passes.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class Season {

    private String name;
    private String description;
    private List<String> quests;
    private HashMap<String, SeasonReward> rewards;
    private long expire;
    private boolean isActive;

    @AllArgsConstructor
    @Getter
    @Setter
    public static class SeasonReward {

        private final String id;
        private int points;
        private String type;
        private String image;
        private String description;
        private String data;

    }

}
