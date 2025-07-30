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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.surftools.wimp.message.Hics259Message;
import com.surftools.wimp.message.Hics259Message.CasualtyEntry;

public class PracticeHicsData {
  private Random rng;

  public PracticeHicsData(Random rng) {
    this.rng = rng;
  }

  public enum Types {
    HOSPITAL_NAMES, SEEN, WAITING, ADMITTED, BED, DISCHARGED, TRANSFERRED
  }

  private static Map<Types, List<String>> buckets = new HashMap<>();

  public String get(Types type) {
    var bucket = buckets.getOrDefault(type, new ArrayList<String>());
    if (bucket.size() == 0) {
      switch (type) {
      case ADMITTED:
        bucket.addAll(patientsAdmitted);
        break;
      case BED:
        bucket.addAll(bedStatus);
        break;
      case DISCHARGED:
        bucket.addAll(patientsDischarged);
        break;
      case HOSPITAL_NAMES:
        bucket.addAll(hospitalNames);
        break;
      case SEEN:
        bucket.addAll(patientsSeen);
        break;
      case TRANSFERRED:
        bucket.addAll(patientsTransferred);
        break;
      case WAITING:
        bucket.addAll(patientsWaiting);
        break;
      }
      Collections.shuffle(bucket, rng);
      buckets.put(type, bucket);
    }

    var value = bucket.remove(0);
    return value;
  }

  private List<String> hospitalNames = Arrays
      .asList("Mercy Ridge Medical Center", "Summit Peak General Hospital", "Evergreen Regional Health",
          "Crescent Valley Trauma Center", "Starlight Children's Hospital", "Horizon Behavioral Health",
          "Red Rock Emergency Hospital", "Blue River Community Medical", "Golden Gate Cardiac Institute",
          "Willow Grove Rehabilitation Center", "Northbridge University Hospital", "Cascade Point Surgical Hospital",
          "Silver Lake Psychiatric Facility", "Twin Pines Memorial Hospital", "Ironwood Infectious Disease Center",
          "Liberty Field Mobile Hospital", "Maplecrest Veterans Hospital", "Oceanview Regional Hospital",
          "Prairie Hill Long-Term Care", "Lakeshore Medical and Imaging");

  private List<String> patientsSeen = Arrays
      .asList("Seen and discharged", "Seen with minor injuries", "Seen and admitted to ICU",
          "Seen and referred to specialist", "Seen with psychological distress", "Seen with smoke inhalation",
          "Seen for dehydration", "Seen for chemical exposure", "Seen for crush injury", "Seen for burns - 1st degree",
          "Seen for burns - 2nd degree", "Seen for fracture assessment", "Seen and placed under quarantine",
          "Seen after delayed arrival", "Seen as walk-in from incident zone", "Seen with unknown history",
          "Seen, identity confirmed", "Seen and refused further care", "Seen during secondary triage",
          "Seen under pediatric protocol");

  private List<String> patientsWaiting = Arrays
      .asList("Waiting - red tag priority", "Waiting - yellow tag, delayed care", "Waiting - green tag, minor injuries",
          "Waiting - black tag confirmation pending", "Waiting - pediatric assessment",
          "Waiting - geriatric evaluation", "Waiting - language interpreter required",
          "Waiting - behavioral health screening", "Waiting - wheelchair assistance",
          "Waiting - non-verbal communication", "Waiting - injury review in progress", "Waiting - vital signs unstable",
          "Waiting - reassessment post-movement", "Waiting - isolated for infection control",
          "Waiting - exposure to hazardous material", "Waiting - identity not confirmed",
          "Waiting - parental consent required", "Waiting - evacuee priority group",
          "Waiting - re-triage due to condition change", "Waiting - follow-up diagnostics required");

  private List<String> patientsAdmitted = Arrays
      .asList("Admitted - intensive care unit (ICU)", "Admitted - general medical ward", "Admitted - surgical recovery",
          "Admitted - respiratory support", "Admitted - under cardiac monitoring",
          "Admitted - neurological observation", "Admitted - pending imaging results",
          "Admitted - infectious disease isolation", "Admitted - psychiatric evaluation", "Admitted - pediatric care",
          "Admitted - burn unit treatment", "Admitted - trauma stabilization", "Admitted - dehydration protocol",
          "Admitted - chemical exposure decontamination", "Admitted - under quarantine protocol",
          "Admitted - orthopedic management", "Admitted - high-risk pregnancy monitoring", "Admitted - renal support",
          "Admitted - post-operative follow-up", "Admitted - awaiting surgical consult");

  private List<String> bedStatus = Arrays
      .asList("Beds occupied – trauma patient", "Beds available – post-quake inspection cleared",
          "Beds under quarantine – biohazard exposure", "Beds reserved – incoming critical care",
          "Beds out of service – structural damage", "Beds occupied – awaiting transfer to field unit",
          "Beds ready – emergency sanitation complete", "Beds status unknown – communication outage",
          "Beds available – staffed and functional", "Beds occupied – disaster responder admitted",
          "Beds repurposed – converted to intensive care", "Beds inspection pending – infrastructure review",
          "Beds assigned – overflow emergency triage", "Beds offline – power supply limited",
          "Beds prepared – mobile oxygen unit installed", "Beds sealed – infection control protocol",
          "Beds occupied – casualty stabilization ongoing", "Beds requested – regional coordination in progress",
          "Beds released – patient transfer completed", "Beds activated – surge capacity initiated");

  private List<String> patientsDischarged = Arrays
      .asList("Discharged - recovered fully", "Discharged - follow-up required",
          "Discharged - referred to outpatient care", "Discharged - home care instructions provided",
          "Discharged - non-emergency condition", "Discharged - mental health referral",
          "Discharged - post-surgical recovery", "Discharged - relocated to long-term care",
          "Discharged - stable vitals", "Discharged - after observation period", "Discharged - self-transport arranged",
          "Discharged - caregiver notified", "Discharged - consent obtained",
          "Discharged - telehealth follow-up scheduled", "Discharged - cleared by attending physician",
          "Discharged - wound dressing instructions provided", "Discharged - no treatment required",
          "Discharged - shelter referral given", "Discharged - documentation completed",
          "Discharged - released per triage protocol");

  private List<String> patientsTransferred = Arrays
      .asList("Transferred - trauma center", "Transferred - burn unit", "Transferred - cardiac care facility",
          "Transferred - pediatric specialty hospital", "Transferred - psychiatric treatment center",
          "Transferred - rehabilitation clinic", "Transferred - long-term care facility",
          "Transferred - infectious disease center", "Transferred - orthopedic specialty hospital",
          "Transferred - surgical suite", "Transferred - neurology department", "Transferred - dialysis center",
          "Transferred - hospice care", "Transferred - emergency overflow site", "Transferred - mobile field hospital",
          "Transferred - radiology for diagnostics", "Transferred - isolation unit",
          "Transferred - maternity care center", "Transferred - respiratory support facility",
          "Transferred - discharge shelter coordination");

  private String rng(int min, int max) {
    return String.valueOf(rng.nextInt(min, max));
  }

  public Map<String, CasualtyEntry> makeCasualtyMap() {
    var map = new HashMap<String, CasualtyEntry>();
    var keys = Hics259Message.CASUALTY_KEYS;
    /*
     * public static final List<String> CASUALTY_KEYS = List .of("Patients seen", "Waiting to be seen", "Admitted",
     * "Critical care bed", "Medical/surgical bed", "Pediatric Bed", "Discharged", "Transferred", "Expired");
     */
    map.put(keys.get(0), new CasualtyEntry(rng(50, 100), rng(5, 10), get(Types.SEEN)));
    map.put(keys.get(1), new CasualtyEntry(rng(25, 50), rng(5, 10), get(Types.WAITING)));
    map.put(keys.get(2), new CasualtyEntry(rng(20, 60), rng(5, 10), get(Types.ADMITTED)));
    map.put(keys.get(3), new CasualtyEntry(rng(10, 20), rng(5, 10), get(Types.BED))); // critical care
    map.put(keys.get(4), new CasualtyEntry(rng(10, 20), rng(5, 10), get(Types.BED))); // medical/surgical
    map.put(keys.get(5), new CasualtyEntry("0", rng(5, 10), get(Types.BED))); // pediatric
    map.put(keys.get(6), new CasualtyEntry(rng(10, 20), rng(5, 10), get(Types.DISCHARGED)));
    map.put(keys.get(7), new CasualtyEntry(rng(10, 20), rng(5, 10), get(Types.TRANSFERRED)));
    map.put(keys.get(8), new CasualtyEntry("0", "0", "")); // Expired

    return map;
  }

}
