package by.potato.holder;

import by.potato.Pairs.Breaks;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Day {
    private DayOfWeek dayOfWeek;

    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonSerialize(using = LocalTimeSerializer.class)
    private LocalTime begin;

    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonSerialize(using = LocalTimeSerializer.class)
    private LocalTime end;

    private List<Breaks> breaks;

    public Day() {
    }

    public Day(DayOfWeek dayOfWeek, LocalTime begin, LocalTime end, List<Breaks> breaks) {
        this.dayOfWeek = dayOfWeek;
        this.begin = begin;
        this.end = end;
        this.breaks = breaks;
    }

    public List<Breaks> getBreaks() {
        return breaks;
    }

    public DayOfWeek getDayOfWeek() {
        return dayOfWeek;
    }

    public LocalTime getBegin() {
        return begin;
    }

    public LocalTime getEnd() {
        return end;
    }


}
