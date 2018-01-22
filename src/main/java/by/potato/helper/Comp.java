package by.potato.helper;

import by.potato.Pairs.Breaks;

import java.util.Comparator;

public class Comp {

    public static Comparator<Breaks> BREAKS = (o1, o2) -> o1.begin.compareTo(o2.begin);
}
