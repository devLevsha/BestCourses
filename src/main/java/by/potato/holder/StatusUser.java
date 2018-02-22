package by.potato.holder;

import by.potato.Enum.Items;
import com.google.maps.model.LatLng;

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
    public List<String> messagesDepartments;

    public StatusUser() {
        actions = new ArrayDeque<>();
    }
}
