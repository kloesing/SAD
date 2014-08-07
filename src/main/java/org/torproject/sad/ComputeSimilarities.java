/* Copyright 2014 The Tor Project
 * See LICENSE for licensing information */
package org.torproject.sad;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.SortedMap;
import java.util.TimeZone;

import com.google.gson.Gson;

class DetailsDocument {
  RelayDetails[] relays;
}

class RelayDetails {
  String nickname;
  String fingerprint;
  String[] or_addresses;
  String[] exit_addresses;
  String dir_address;
  String last_seen;
  String last_changed_address_or_port;
  String first_seen;
  Boolean running;
  Boolean hibernating;
  String[] flags;
  String country;
  String country_name;
  String region_name;
  String city_name;
  Float latitude;
  Float longitude;
  String as_number;
  String as_name;
  Integer consensus_weight;
  String host_name;
  String last_restarted;
  Integer bandwidth_rate;
  Integer bandwidth_burst;
  Integer observed_bandwidth;
  Integer advertised_bandwidth;
  String[] exit_policy;
  SortedMap<String, String[]> exit_policy_summary;
  SortedMap<String, String[]> exit_policy_v6_summary;
  String contact;
  String platform;
  Boolean recommended_version;
  String[] family;
  Float advertised_bandwidth_fraction;
  Float consensus_weight_fraction;
  Float guard_probability;
  Float middle_probability;
  Float exit_probability;
}

public class ComputeSimilarities {

  private static DateFormat dateFormat;

  public static void main(String[] args) {
    dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    dateFormat.setLenient(false);
    dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

    RelayDetails[] relayDetails = readRelayDetails();
    if (relayDetails == null) {
      System.exit(1);
    }
    writePairwiseSimilarity(relayDetails);
  }

  private static RelayDetails[] readRelayDetails() {
    RelayDetails[] rd = null;
    File detailsFile = new File("in/details.json");
    if (detailsFile.exists() && !detailsFile.isDirectory()) {
      try {
        FileReader fr = new FileReader(detailsFile);
        Gson gson = new Gson();
        DetailsDocument dd = gson.fromJson(fr, DetailsDocument.class);
        fr.close();
        rd = dd.relays;
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return rd;
  }

  private static void writePairwiseSimilarity(RelayDetails[] rd) {
    try {
      File outFile = new File("out/relay-similarity.csv");
      outFile.getParentFile().mkdirs();
      BufferedWriter bw = new BufferedWriter(new FileWriter(outFile));
      bw.write("same_family,common_address_prefix,"
          + "hours_between_first_seen,same_country,same_region,same_city,"
          + "same_autonomous_system,consensus_weight_difference\n");
      for (int i = 0; i < rd.length; i++) {
        for (int j = i + 1; j < rd.length; j++) {
          /* Only put the following in for testing. */
          /*if (rd[i].family == null || rd[j].family == null) {
            continue;
          }*/
          bw.write(sameFamily(rd[i], rd[j]) ? "1" : "0");
          bw.write("," + commonAddressPrefix(rd[i], rd[j]));
          bw.write("," + hoursBetweenFirstSeen(rd[i], rd[j]));
          bw.write("," + (sameCountry(rd[i], rd[j]) ? "1" : "0"));
          bw.write("," + (sameRegion(rd[i], rd[j]) ? "1" : "0"));
          bw.write("," + (sameCity(rd[i], rd[j]) ? "1" : "0"));
          bw.write(","
              + (sameAutonomousSystem(rd[i], rd[j]) ? "1" : "0"));
          bw.write("," + consensusWeightDifference(rd[i], rd[j]));
          bw.write("\n");
        }
      }
      bw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private static boolean sameFamily(RelayDetails a, RelayDetails b) {
    if (a.family == null || b.family == null) {
      return false;
    }
    String bFingerprint = "$" + b.fingerprint;
    for (String aFamilyMember : a.family) {
      if (aFamilyMember.equals(bFingerprint)) {
        String aFingerprint = "$" + a.fingerprint;
        for (String bFamilyMember : b.family) {
          if (bFamilyMember.equals(aFingerprint)) {
            return true;
          }
        }
      }
    }
    return false;
  }

  private static int commonAddressPrefix(RelayDetails a, RelayDetails b) {
    int prefixLength = 0;
    String[] aAddress = a.or_addresses[0].split(":")[0].split("\\.");
    String[] bAddress = b.or_addresses[0].split(":")[0].split("\\.");
    for (int i = 0; i < 4; i++) {
      if (aAddress[i].equals(bAddress[i])) {
        prefixLength += 8;
      } else {
        prefixLength += Integer.numberOfLeadingZeros(
            Integer.parseInt(aAddress[i]) ^
            Integer.parseInt(bAddress[i])) - 24;
        break;
      }
    }
    return prefixLength;
  }

  private static int hoursBetweenFirstSeen(RelayDetails a,
      RelayDetails b) {
    try {
      long aFirstSeenMillis = dateFormat.parse(a.first_seen).getTime();
      long bFirstSeenMillis = dateFormat.parse(b.first_seen).getTime();
      long millisBetweenFirstSeen = Math.abs(aFirstSeenMillis
          - bFirstSeenMillis);
      return (int) (millisBetweenFirstSeen / (60L * 60L * 1000L));
    } catch (ParseException e) {
    }
    System.err.println("hoursBetweenFirstSeen");
    System.exit(1);
    return -1;
  }

  private static boolean sameCountry(RelayDetails a,
      RelayDetails b) {
    return a.country != null && b.country != null &&
        a.country.equals(b.country);
  }

  private static boolean sameRegion(RelayDetails a, RelayDetails b) {
    return sameCountry(a, b) &&
        ((a.region_name == null && b.region_name == null) ||
        (a.region_name != null && b.region_name != null &&
        a.region_name.equals(b.region_name)));
  }

  private static boolean sameCity(RelayDetails a, RelayDetails b) {
    return sameRegion(a, b) &&
        ((a.city_name == null && b.city_name == null) ||
        (a.city_name != null && b.city_name != null &&
        a.city_name.equals(b.city_name)));
  }

  private static boolean sameAutonomousSystem(RelayDetails a,
      RelayDetails b) {
    return a.as_number != null && b.as_number != null &&
        a.as_number.equals(b.as_number);
  }

  private static int consensusWeightDifference(RelayDetails a,
      RelayDetails b) {
    return Math.abs(a.consensus_weight - b.consensus_weight);
  }
}


