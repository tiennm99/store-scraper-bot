package com.miti99.storescraperbot.bot.table;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import kotlin.collections.ArrayDeque;

public class Table {
  private final String[] headers;
  private final List<String[]> rows = new ArrayDeque<>();

  public Table(String... headers) {
    this.headers = headers;
  }

  public void addRow(Object... objs) {
    if (objs.length != headers.length) {
      throw new IllegalArgumentException(
          "objs.length (%d) != headers.length (%d)".formatted(objs.length, headers.length));
    }
    var row = new String[objs.length];
    for (int i = 0; i < objs.length; i++) {
      row[i] = String.valueOf(objs[i]);
    }
    rows.add(row);
  }

  @Override
  public String toString() {
    int[] maxWidths = new int[headers.length];
    for (int i = 0; i < headers.length; i++) {
      maxWidths[i] = headers[i].length();
    }
    for (var row : rows) {
      for (int i = 0; i < row.length; i++) {
        maxWidths[i] = Math.max(maxWidths[i], row[i].length());
      }
    }
    var formater =
        Arrays.stream(maxWidths)
            .mapToObj(width -> "%-" + width + "s")
            .collect(Collectors.joining(" │ ", "", "\n"));
    var sb = new StringBuilder();
    sb.append(formater.formatted((Object[]) headers));
    var rule =
        Arrays.stream(maxWidths).mapToObj("─"::repeat).collect(Collectors.joining("─┼─", "", "\n"));
    sb.append(rule);
    for (var row : rows) {
      sb.append(formater.formatted((Object[]) row));
    }
    return sb.toString();
  }
}
