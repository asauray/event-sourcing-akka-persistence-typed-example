package net.sauray.application.serializers;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import akka.serialization.JSerializer;
import net.sauray.domain.events.*;

/*
 * JsonSerializer.java
 * Copyright (C) 2020 antoinesauray <sauray.antoine@gmail.com>
 *
 * Distributed under terms of the MIT license.
 */

public class BankEventSerializer extends JSerializer {

  private static String EVENT_JSON_FIELD = "event";

  private static String MONEY_ADDED_TO_ACCOUNT = "MoneyAddedToAccount";
  private static String MONEY_REMOVED_FROM_ACCOUNT = "MoneyRemovedFromAccount";

  // If you need logging here, introduce a constructor that takes an
  // ExtendedActorSystem.
  // public MyOwnSerializer(ExtendedActorSystem actorSystem)
  // Get a logger using:
  // private final LoggingAdapter logger = Logging.getLogger(actorSystem, this);

  private final Gson gson;

  public BankEventSerializer() {
    gson = new Gson();
  }

  // This is whether "fromBinary" requires a "clazz" or not
  @Override
  public boolean includeManifest() {
    return false;
  }

  // Pick a unique identifier for your Serializer,
  // you've got a couple of billions to choose from,
  // 0 - 40 is reserved by Akka itself
  @Override
  public int identifier() {
    return 100;
  }

  // "toBinary" serializes the given object to an Array of Bytes
  @Override
  public byte[] toBinary(Object obj) {
    JsonObject eventJson = new JsonObject();
    if (obj instanceof MoneyAddedToAccount) {
      MoneyAddedToAccount moneyAddedToAccountEvent = (MoneyAddedToAccount) obj;
      eventJson.addProperty(EVENT_JSON_FIELD, MONEY_ADDED_TO_ACCOUNT);
      eventJson.addProperty("amount", moneyAddedToAccountEvent.getAmountCents());
      eventJson.addProperty("eventId", moneyAddedToAccountEvent.id().toString());
    } else if (obj instanceof MoneyRemovedFromAccount) {
      MoneyRemovedFromAccount moneyRemovedFromAccountEvent = (MoneyRemovedFromAccount) obj;
      eventJson.addProperty(EVENT_JSON_FIELD, MONEY_REMOVED_FROM_ACCOUNT);
      eventJson.addProperty("amount", moneyRemovedFromAccountEvent.getAmountCents());
      eventJson.addProperty("eventId", moneyRemovedFromAccountEvent.id().toString());
    }
    return gson.toJson(eventJson).getBytes();
  }

  // "fromBinary" deserializes the given array,
  // using the type hint (if any, see "includeManifest" above)
  @Override
  public Object fromBinaryJava(byte[] bytes, Class<?> clazz) {
    try {
      JsonObject jsonObj = JsonParser.parseString(new String(bytes, "UTF-8")).getAsJsonObject();

      switch (jsonObj.get(EVENT_JSON_FIELD).getAsString()) {
        case "MoneyAddedToAccount": return deserializeMoneyAddedToAccount(jsonObj);
        case "MoneyRemovedFromAccount": return deserializeMoneyRemovedFromAccount(jsonObj);
        default: {
          System.out.println(String.format("error: %s", (jsonObj.get(EVENT_JSON_FIELD).getAsString())));
          return null;
        }
      }

    } catch (JsonSyntaxException | UnsupportedEncodingException e) {
      e.printStackTrace();
      return null;
    }
  }

  MoneyAddedToAccount deserializeMoneyAddedToAccount(JsonObject jsonObj) {
    Long amount = jsonObj.get("amount").getAsLong();
    UUID eventId = UUID.fromString(jsonObj.get("eventId").getAsString());
    return new MoneyAddedToAccount(eventId, amount);
  }

  MoneyRemovedFromAccount deserializeMoneyRemovedFromAccount(JsonObject jsonObj) {
    Long amount = jsonObj.get("amount").getAsLong();
    UUID eventId = UUID.fromString(jsonObj.get("eventId").getAsString());
    return new MoneyRemovedFromAccount(eventId, amount);
  }
}
