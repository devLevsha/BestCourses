package by.potato.holder;

import by.potato.Enum.Items;
import com.google.maps.model.LatLng;
import org.apache.commons.lang3.tuple.Pair;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;

public class StatusUser {

    public Deque<Items> actions;
    public Optional<LatLng> location;
    public Double distance;
    public String city;
    public List<Department> departments;
    public LocalDateTime localDateTime;
    public Pair<List<String>, List<LatLng>> messagesAndLocation;

    public StatusUser() {
        actions = new ArrayDeque<>();
    }
}
