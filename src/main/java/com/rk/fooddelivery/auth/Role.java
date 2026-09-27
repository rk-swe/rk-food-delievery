package com.rk.fooddelivery.auth;

import java.util.Locale;

public enum Role {
  ADMIN,
  RESTAURANT_OWNER,
  CUSTOMER,
  DELIVERY_PARTNER;

  public static Role fromDatabase(String value) {
    return Role.valueOf(value.toUpperCase(Locale.ROOT));
  }

  public String databaseValue() {
    return name().toLowerCase(Locale.ROOT);
  }
}
