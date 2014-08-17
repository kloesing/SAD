# Sybil Attack Detector

The code in this repository is a starting point for detecting possible Sybil attacks on the Tor network.  The idea is to use various similarity metrics to detect relays that are similar but not defined to be in the same relay family.  Those relays might be part of a Sybil attack.

## Similarity metrics

Possible similarity metrics are:

 - Common address prefix: how many bits does the common IP address prefix have, from 0 to 32?

 - Hours between first seen: how many hours have passed between first seeing the two relays in a consensus?

 - Same country/region/city: are the two relays located in the same country, region, or city?

 - Geographic distance: what's the geographic distance between relays as determined by resolving their IP addresses to latitude and longitude?

 - Same autonomous system: are the two relays located in the same autonomous system?

 - Consensus weight: what's the absolute difference between the consensus weight values?

 - Advertised/consumed bandwidth: what's the absolute difference between advertised/consumed bandwidth?

 - Reverse domain name lookups: how similar is the reverse domain name, starting from top-level domain down to subdomains?

 - Exit policies: how similar are the configured exit policies?

 - Nicknames: how similar are the chosen relay nicknames?
 
 - Guardiness: Do relays have or not have the Guard flag?
 
 - Hours between last restart (uptime): how many hours have passed between last restarting relays?

 - Contact information: how similar are the contact lines, if provided?

 - Operating system: are relays running on the same OS?

 - Software version: are relays running the same tor software version?

 - Configured OR port: Are relays using the same OR port?

 - Common fingerprint prefix: how many of the fingerprint bits match starting from most significant bit?  This is relevant to detect attacks on the hidden service hash ring.

Obviously, some of these are easier to game than others, and there's likely not a single metric that indicates a Sybil attack.

## Getting started

The following steps are necessary to compute pairwise similarity metrics for all currently running relays.

 - Fetch relay details of all running relays from the Onionoo service: `bin/fetch-details.sh`.  This step is separate from subsequent steps to facilitate testing without having to download new data in every run.

 - Install Java 6 or higher, the build tool Ant, and Google's JSON library Gson.  On Debian, this can be done with the following command: `apt-get install openjdk-6-jdk ant libgoogle-gson-java`

 - Compile and run the Java sources: `ant`.  This takes a few minutes and produces an output file with pairwise similarities: `out/similarities.csv`.

## Next steps

The following next steps are yet to be done:

 - Analyze pairwise similarity metrics to find out which of them indicate a possible relation between relays.  The R code in `bin/analyze.R` may serve as a starting point but doesn't produce usable results yet.

 - Consider how results could be utilized to identify geographic locations, network locations, operating systems, etc. for new relays to increase network diversity.

 - Run the Sybil attack detection script to find relays that are obviously part of a relay family but forgot to define that.  Ask operators to fix that.

 - Run the Sybil attack detection script on directory archives to detect possible attacks that happened in the past.  Find out when the directory authorities forcibly removed relays from the network in the past (by looking at votes) and make sure the detection script would have detected these.

 - Quantify impact of possible Sybil attacks.  The easiest is counting the number of relays, but depending on the adversary's goal, that may not be as meaningful.  Other ideas may be total advertised bandwidth fraction, total consensus weight fraction, or total guard/middle/exit probability.

 - Turn these scripts into a monitoring system that raises an alarm if a potential Sybil attack is detected.  The attack could be louder the larger the share of the network is and the clearer the indications for an attack are.  There should probably be a way to silence an alarm if something turned out as false alarm.  Silencing could happen for a limited time, for a given threshold of number of relays or network fraction, etc.  Alarms should be sent to a public email list, silenced alarms should be made publicly available, and everyone should be able to run their own instance of this detector.

 - Create good visualizations of potential Sybil attacks.  See David Fifield's work in ticket #12813 for a fine starting point.
