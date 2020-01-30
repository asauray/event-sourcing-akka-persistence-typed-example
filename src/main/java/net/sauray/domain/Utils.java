package net.sauray.domain;

/*
 * Utils.java
 * Copyright (C) 2020 antoinesauray <sauray.antoine@gmail.com>
 *
 * Distributed under terms of the MIT license.
 */

public class Utils
{

  public static void validateIsPositive(Long value) {
    if(value <= 0) {
      throw new IllegalArgumentException("value should be positive");
    }
  }
  
}

