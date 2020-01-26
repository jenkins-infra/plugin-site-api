package io.jenkins.plugins.utils;

import java.util.Iterator;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class StreamUtils {

  public static <T> Stream<T> asStream(Iterator<T> sourceIterator) {
    Iterable<T> iterable = () -> sourceIterator;
    return StreamSupport.stream(iterable.spliterator(), false);
  }

}
