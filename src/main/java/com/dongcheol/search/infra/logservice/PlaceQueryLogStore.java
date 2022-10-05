package com.dongcheol.search.infra.logservice;

import com.dongcheol.search.infra.logservice.dto.PlaceQueryLog;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PlaceQueryLogStore {

    private List<String> backupList = new ArrayList<>();
    private final static ArrayList<PlaceQueryLog> queryLogs = new ArrayList<>();

    public synchronized void put(PlaceQueryLog qLog) {
        qLog.getDatetime();
        this.queryLogs.add(qLog);
    }

    public synchronized List<PlaceQueryLog> getAllDel() {
        List<PlaceQueryLog> result = (List) this.queryLogs.clone();
        this.queryLogs.clear();
        backupList.add(result.toString());
        return result;
    }
}
