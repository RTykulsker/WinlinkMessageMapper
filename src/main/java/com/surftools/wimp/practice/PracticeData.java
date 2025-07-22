/**

The MIT License (MIT)

Copyright (c) 2025, Robert Tykulsker

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.


*/

package com.surftools.wimp.practice;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.codec.binary.Base32;

import com.surftools.utils.WeightedRandomChooser;

public class PracticeData {
  public enum ListType {
    EMERGENCY_REPSONSE_ROLES, SHORT_EMERGENCY_ROLES, NAMES, DOUBLED_NAMES
  };

  public enum ExerciseIdMethod {
    UUID, SHORT_UUID, MID, PHONE
  }

  private final Random rng;
  public final WeightedRandomChooser deliveryChooser;
  public final WeightedRandomChooser priorityChooser;

  public PracticeData(Random rng) {
    this.rng = rng;

    deliveryChooser = new WeightedRandomChooser(deliveryArray, rng);
    priorityChooser = new WeightedRandomChooser(List.of("Low", "Routine", "URGENT"), rng);
  }

  private Map<ListType, List<String>> listTypeMap = Map
      .of(//
          ListType.EMERGENCY_REPSONSE_ROLES, emergencyResponseRoles, //
          ListType.SHORT_EMERGENCY_ROLES, shortEmergencyRoles, //
          ListType.NAMES, names, //
          ListType.DOUBLED_NAMES, doubleNames);

  /**
   * return exactly <count> unique elements from a list
   *
   * @param count
   * @param listType
   * @return
   */
  public List<String> getUniqueList(int count, List<String> list) {
    if (list == null) {
      return null;
    }

    var returnList = new ArrayList<String>();
    if (count <= 0) {
      return returnList;
    }

    var tmpList = new ArrayList<String>(list);
    Collections.shuffle(tmpList, rng);
    for (int i = 0; i < count; ++i) {
      returnList.add(tmpList.get(i % count));
    }

    return returnList;
  }

  /**
   * return exactly <count> unique elements from a list
   *
   * @param count
   * @param listType
   * @return
   */
  public List<String> getUniqueList(int count, ListType listType) {
    if (listType == null) {
      return null;
    }

    return getUniqueList(count, listTypeMap.get(listType));
  }

  public String getExerciseId(ExerciseIdMethod method) {
    var ret = "";
    switch (method) {
    case UUID:
      ret = java.util.UUID.randomUUID().toString();
    case SHORT_UUID:
      ret = java.util.UUID.randomUUID().toString().substring(9, 23);
    case MID:
      ret = generateMid(java.util.UUID.randomUUID().toString());
    case PHONE:
      var n = (long) (rng.nextDouble() * 9000000000L) + 1000000000L;
      var s = String.valueOf(n);
      ret = s.substring(0, 3) + "-" + s.substring(3, 6) + "-" + s.substring(6);
    }

    return ret;
  }

  private String generateMid(String string) {
    try {
      MessageDigest md = MessageDigest.getInstance("MD5");
      String stringToHash = string + System.nanoTime();
      md.update(stringToHash.getBytes());
      byte[] digest = md.digest();
      Base32 base32 = new Base32();
      String encodedString = base32.encodeToString(digest);
      String subString = encodedString.substring(0, 12);
      return subString;
    } catch (Exception e) {
      throw new RuntimeException("could not generate messageId: " + e.getMessage());
    }
  }

  private static final List<String> emergencyResponseRoles = Arrays
      .asList("Director of Emergency Management", "Deputy Director of Operations", "Chief of Staff",
          "Emergency Response Coordinator", "Strategic Planning Officer", "Crisis Management Director",
          "Regional Response Commander", "Assistant Director of Field Operations", "Chief Resilience Officer",
          "Director of Interagency Coordination", "Incident Commander", "Operations Section Chief",
          "Field Response Supervisor", "Emergency Services Officer", "Search and Rescue Team Leader",
          "Disaster Response Specialist", "Urban Search and Rescue Technician", "Hazardous Materials (HAZMAT) Officer",
          "Emergency Operations Center (EOC) Manager", "Rapid Response Team Member", "Planning Section Chief",
          "Situation Analyst", "Intelligence Officer", "Risk Assessment Specialist", "Continuity of Operations Planner",
          "Emergency Preparedness Analyst", "Geographic Information Systems (GIS) Analyst",
          "Threat Assessment Coordinator", "Scenario Planning Officer", "Data Modeling Specialist",
          "Logistics Section Chief", "Supply Chain Manager", "Resource Allocation Officer",
          "Transportation Coordinator", "Equipment Inventory Specialist", "Fleet Operations Manager",
          "Warehouse Supervisor", "Procurement Officer", "Fuel and Energy Logistics Officer",
          "Shelter Logistics Coordinator", "Chief Medical Officer", "Emergency Medical Services (EMS) Coordinator",
          "Public Health Liaison", "Epidemiologist", "Medical Triage Officer", "Mass Casualty Incident Coordinator",
          "Mental Health Response Specialist", "Medical Logistics Officer", "Field Medic",
          "Biohazard Response Technician", "Public Information Officer", "Communications Section Chief",
          "Media Relations Specialist", "Crisis Communications Strategist", "Social Media Response Coordinator",
          "Emergency Alert System Manager", "Community Outreach Officer", "Multilingual Communications Specialist",
          "Call Center Supervisor", "Internal Communications Manager", "IT Systems Administrator",
          "Emergency Communications Technician", "Cybersecurity Analyst", "Radio Communications Officer",
          "Infrastructure Resilience Engineer", "Power Systems Specialist", "Emergency Technology Integration Officer",
          "Drone Operations Coordinator", "Mobile Command Unit Technician", "Network Operations Center (NOC) Analyst",
          "Emergency Law Advisor", "Regulatory Compliance Officer", "Policy Development Analyst",
          "Intergovernmental Affairs Liaison", "Legal Risk Assessor", "Grants and Funding Specialist",
          "Mutual Aid Agreements Coordinator", "Ethics and Standards Officer", "Legislative Affairs Officer",
          "Public Safety Policy Analyst", "Training and Exercises Coordinator", "Emergency Response Instructor",
          "Simulation Designer", "Certification Program Manager", "Field Training Officer",
          "Volunteer Training Specialist", "Curriculum Development Specialist", "Community Preparedness Educator",
          "Safety Drill Coordinator", "Knowledge Management Officer", "Administrative Services Manager",
          "Budget and Finance Officer", "Human Resources Liaison", "Records and Documentation Specialist",
          "Grant Writer", "Scheduling Coordinator", "Facilities Manager", "Travel and Deployment Coordinator",
          "Timekeeping and Payroll Officer", "Volunteer Program Manager");

  private static final List<String> shortEmergencyRoles = Arrays
      .asList("EM Director", "Ops Chief", "Chief of Staff", "Rescue Leader", "Field Officer", "HAZMAT Officer",
          "EOC Manager", "Logistics Chief", "Medical Officer", "Public Info Lead", "Search Leader", "Rescue Tech",
          "Disaster Lead", "EMS Coordinator", "Crisis Planner", "Risk Analyst", "GIS Analyst", "Intel Officer",
          "Ops Planner", "Supply Officer", "Fleet Manager", "Warehouse Lead", "Procurement Lead", "Shelter Lead",
          "Triage Officer", "Epidemiologist", "Mental Health", "Biohazard Tech", "Comms Officer", "Media Liaison",
          "Alert Manager", "Outreach Lead", "Call Center Lead", "IT Admin", "Radio Tech", "Cyber Analyst",
          "Infra Engineer", "Drone Operator", "Mobile Tech", "NOC Analyst", "Legal Advisor", "Policy Analyst",
          "Compliance Lead", "Liaison Officer", "Grants Officer", "Ethics Officer", "Legislative Lead",
          "Safety Analyst", "Trainer", "Drill Planner", "Instructor", "Cert Manager", "Field Trainer", "Volunteer Lead",
          "Educator", "Drill Leader", "Knowledge Lead", "Admin Manager", "Finance Officer", "HR Liaison",
          "Records Clerk", "Grant Writer", "Scheduler", "Facilities Lead", "Travel Planner", "Payroll Officer",
          "Volunteer Coord", "Ops Director", "Deputy Director", "Resilience Lead", "Interagency Lead",
          "Rapid Responder", "SAR Tech", "HAZMAT Tech", "Continuity Lead", "Scenario Lead", "Data Modeler",
          "Transport Lead", "Inventory Lead", "Fuel Officer", "Medical Tech", "Mass Casualty", "Comms Strategist",
          "Social Media", "Internal Comms", "Tech Integrator", "Power Specialist", "Radio Officer", "Legal Risk",
          "Mutual Aid Lead", "Ethics Lead", "Affairs Officer", "Policy Officer", "Drill Coord", "Safety Officer",
          "Curriculum Lead", "Prep Educator", "Admin Officer", "Budget Officer", "HR Officer", "Doc Specialist",
          "Travel Officer", "Timekeeper");

  private static final List<String> names = Arrays
      .asList("Liam Carter", "Emma Brooks", "Noah Hayes", "Olivia Grant", "Elijah Stone", "Ava Morgan", "James Reed",
          "Sophia Lane", "Benjamin Fox", "Isabella Ross", "Lucas Ward", "Mia Blake", "Henry Wells", "Amelia Dean",
          "Alexander Cole", "Harper West", "William Nash", "Evelyn Shaw", "Daniel Boyd", "Abigail Ray", "Matthew King",
          "Ella Ford", "Jackson Hale", "Scarlett Moon", "Sebastian Lee", "Grace Hunt", "David Knox", "Chloe Page",
          "Joseph Tate", "Lily Snow", "Samuel Webb", "Zoey Hart", "Owen Cross", "Nora Quinn", "Wyatt Long", "Aria Bell",
          "John York", "Layla Park", "Julian Ross", "Riley Nash", "Levi Dean", "Ellie Wood", "Isaac Lane", "Luna Frost",
          "Gabriel West", "Hazel Knox", "Anthony Cole", "Violet Ray", "Andrew Hale", "Aurora King", "Lincoln Boyd",
          "Penelope Shaw", "Christopher Fox", "Camila Wells", "Joshua Stone", "Stella Moon", "Nathan Ward",
          "Paisley Hunt", "Caleb Nash", "Savannah Page", "Eli Ford", "Brooklyn Snow", "Thomas Webb", "Claire Hart",
          "Aaron Cross", "Skylar Quinn", "Charles Long", "Lucy Bell", "Hunter York", "Anna Park", "Adrian Ross",
          "Caroline Nash", "Jonathan Dean", "Madeline Wood", "Christian Lane", "Elena Frost", "Connor West",
          "Naomi Knox", "Jeremiah Cole", "Ruby Ray", "Robert Hale", "Ivy King", "Easton Boyd", "Kinsley Shaw",
          "Jordan Fox", "Aaliyah Wells", "Angel Stone", "Cora Moon", "Dominic Ward", "Sadie Hunt", "Austin Nash",
          "Julia Page", "Brayden Ford", "Piper Snow", "Jason Webb", "Eva Hart", "Miles Cross", "Alice Quinn",
          "Xavier Long", "Faith Bell", "Justin York", "Maya Park");

  private static final List<String> doubleNames = Arrays
      .asList("Alice Anderson", "Ben Baxter", "Catherine Carter", "Daniel Dawson", "Ella Edwards", "Franklin Foster",
          "Grace Griffin", "Henry Hughes", "Isla Ingram", "Jack Johnson", "Kara Keller", "Liam Lawson", "Mia Mitchell",
          "Noah Nelson", "Olivia Owens", "Peter Parker", "Quinn Quigley", "Rachel Rivers", "Samuel Scott",
          "Tara Thompson", "Ulysses Underwood", "Violet Vaughn", "William Walker", "Xander Xavier", "Yara Young",
          "Zane Zimmerman", "Amber Allen", "Brandon Brooks", "Chloe Chambers", "Dylan Drake", "Eva Ellison",
          "Felix Fisher", "Georgia Grant", "Hannah Hall", "Ian Irving", "Julia Jenkins", "Kyle Knight", "Leo Larson",
          "Mason Moore", "Natalie Neal", "Owen Ortega", "Paige Preston", "Quentin Quinn", "Rebecca Ross",
          "Sean Simmons", "Tina Taylor", "Uriel Upton", "Vanessa Vance", "Wesley West", "Ximena Xanders", "Yosef Yates",
          "Zara Ziegler", "Aiden Avery", "Bella Benson", "Caleb Cooper", "Daisy Dalton", "Ethan Ellis", "Fiona Flynn",
          "Gavin Gates", "Hailey Harmon", "Isaac Iverson", "Jasmine James", "Kevin Kirk", "Lily Lewis",
          "Madeline Marks", "Nathan Norris", "Olive O’Connor", "Peyton Pierce", "Quincy Quick", "Riley Randall",
          "Sophie Sanders", "Trevor Tate", "Uma Underhill", "Victor Vega", "Wendy Winters", "Xenia Xiong",
          "Yvonne York", "Zachary Zane", "Annie Abbott", "Blake Barnes", "Clara Clayton", "Derek Dorsey",
          "Eliza Emerson", "Finn Franklin", "Gemma Garrison", "Harper Hines", "Ivan Ives", "Jared Jennings",
          "Kylie King", "Logan Lane", "Megan Murphy", "Nolan Nash", "Oscar Olsen", "Phoebe Phelps", "Quora Quinlan",
          "Ronald Reid", "Samantha Steele", "Tristan Turner", "Ulric Urban", "Valerie Vaughn", "Willa Wade",
          "Xavier Xenos", "Yahir Yoder", "Zoey Zane");

  List<String> randomStrings = Arrays
      .asList("483-920-1745", "729-384-6102", "158-637-2940", "604-218-7391", "317-845-9206", "890-472-1635",
          "245-309-8761", "731-608-4927", "506-194-7382", "962-307-1584", "184-620-3957", "703-519-8460",
          "398-271-6043", "576-803-1294", "820-146-3709", "691-204-8753", "347-980-2165", "215-763-4890",
          "904-315-7286", "638-472-1905", "472-608-3519", "189-437-6208", "560-218-9347", "703-194-5826",
          "826-370-1492", "391-608-2475", "204-759-3186", "648-203-9175", "739-185-6204", "580-492-7316",
          "913-274-8065", "327-915-4802", "604-381-7290", "185-620-4937", "472-903-1684", "739-284-6105",
          "820-194-3756", "608-472-9315", "194-820-6375", "370-158-4926", "608-215-7394", "492-703-1860",
          "215-604-8397", "703-492-1685", "820-739-1046", "194-608-3725", "158-492-7036", "604-215-8391",
          "739-820-1643", "492-703-1586", "215-604-7398", "703-492-1864", "820-739-1056", "194-608-3726",
          "158-492-7037", "604-215-8392", "739-820-1644", "492-703-1587", "215-604-7399", "703-492-1865",
          "820-739-1057", "194-608-3727", "158-492-7038", "604-215-8393", "739-820-1645", "492-703-1588",
          "215-604-7400", "703-492-1866", "820-739-1058", "194-608-3728", "158-492-7039", "604-215-8394",
          "739-820-1646", "492-703-1589", "215-604-7401", "703-492-1867", "820-739-1059", "194-608-3729",
          "158-492-7040", "604-215-8395", "739-820-1647", "492-703-1590", "215-604-7402", "703-492-1868",
          "820-739-1060", "194-608-3730", "158-492-7041", "604-215-8396", "739-820-1648", "492-703-1591",
          "215-604-7403", "703-492-1869", "820-739-1061", "194-608-3731", "158-492-7042", "604-215-8397",
          "739-820-1649", "492-703-1592");

  String[] deliveryArray = { "Main Entrance", "Reception Desk", "Loading Dock", "Mailroom", "Security Office",
      "Parking Lot – North", "Parking Lot – South", "Visitor Entrance", "Side Entrance", "Rooftop Access",
      "Maintenance Room", "Basement Level", "Elevator Lobby – 1st Floor", "Elevator Lobby – 10th Floor",
      "Conference Room A", "Cafeteria Entrance", "Bike Rack Area", "Fire Exit – East Wing", "Drop Box – West Gate",
      "Garden Courtyard" };

}
