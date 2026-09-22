package com.phi.domain;

public enum MedicationCourseType {
    /** Short course — malaria, dengue, viral fever; has an expected end. */
    ACUTE,
    /** Ongoing — diabetes, hypertension, thyroid; no automatic end date. */
    CHRONIC,
    /** Not classified yet — user or extraction should confirm. */
    UNKNOWN
}
