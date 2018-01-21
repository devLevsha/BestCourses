package by.potato.helper;

import by.potato.Pairs.Breaks;

import java.util.Comparator;

public class Comp {

    public static Comparator<Breaks> BREAKS = new Comparator<Breaks>() {
        @Override
        public int compare(Breaks o1, Breaks o2) {
            return o1.begin.compareTo(o2.begin);
        }
    };
}
