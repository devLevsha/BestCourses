package by.potato.Pairs;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalTimeSerializer;

import java.time.LocalTime;

public class Breaks {

    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonSerialize(using = LocalTimeSerializer.class)
    public LocalTime begin;

    @JsonDeserialize(using = LocalTimeDeserializer.class)
    @JsonSerialize(using = LocalTimeSerializer.class)
    public LocalTime end;

    public Breaks() {
    }


    public Breaks(LocalTime begin, LocalTime end) {
        this.begin = begin;
        this.end = end;
    }
}
