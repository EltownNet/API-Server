package net.eltown.apiserver.components.handler.quests.data;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@AllArgsConstructor
@Getter
@Setter
public class QuestPlayer {

    private final String player;
    private List<QuestData> questPlayerData;

    @AllArgsConstructor
    @Getter
    @Setter
    public static class QuestData {

        private String questNameId;
        private String questSubId;
        private String data;
        private int current;
        private int required;
        private long expire;

    }

}
