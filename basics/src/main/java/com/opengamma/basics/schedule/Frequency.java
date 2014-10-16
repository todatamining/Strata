/**
 * Copyright (C) 2014 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package com.opengamma.basics.schedule;

import java.io.Serializable;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeParseException;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.time.temporal.UnsupportedTemporalTypeException;
import java.util.List;

import org.joda.convert.FromString;
import org.joda.convert.ToString;

import com.opengamma.collect.ArgChecker;

/**
 * A periodic frequency used by financial products that have a specific event every so often.
 * <p>
 * Frequency is primarily intended to be used to subdivide events within a year.
 * <p>
 * A frequency is allowed to be any non-negative period of days, weeks, month or years.
 * This class provides constants for common frequencies which are best used by static import.
 * <p>
 * A special value, 'Term', is provided for when there are no subdivisions of the entire term.
 * This is also know as 'zero-coupon' or 'once'. It is represented using the period 10,000 years,
 * which allows addition/subtraction to work, producing a date after the end of the term.
 * <p>
 * Each frequency is based on a {@link Period}. The months and years of the period are not normalized,
 * thus it is possible to have a frequency of 12 months and a different one of 1 year.
 * When used, standard date addition rules apply, thus there is no difference between them.
 * Call {@link #normalized()} to apply normalization.
 * <p>
 * The periodic frequency is often expressed as a number of events per year.
 * The {@link #eventsPerYear()} method can be used to obtain this for common frequencies.
 * 
 * <h4>Usage</h4>
 * {@code Frequency} implements {@code TemporalAmount} allowing it to be directly added to a date:
 * <pre>
 *  LocalDate later = baseDate.plus(frequency);
 * </pre>
 */
public final class Frequency
    implements TemporalAmount, Serializable {

  /**
   * Serialization version.
   */
  private static final long serialVersionUID = 1;
  /**
   * The artificial maximum length of a normal tenor in years.
   */
  private static final int MAX_YEARS = 1_000;
  /**
   * The artificial maximum length of a normal tenor in months.
   */
  private static final int MAX_MONTHS = MAX_YEARS * 12;
  /**
   * The artificial length in years of the 'Term' frequency.
   */
  private static final int TERM_YEARS = 10_000;

  /**
   * A periodic frequency of one day.
   * Also known as daily.
   * There are considered to be 364 events per year with this frequency.
   */
  public static final Frequency P1D = ofDays(1);
  /**
   * A periodic frequency of 1 week (7 days).
   * Also known as weekly.
   * There are considered to be 52 events per year with this frequency.
   */
  public static final Frequency P1W = ofWeeks(1);
  /**
   * A periodic frequency of 2 weeks (14 days).
   * Also known as bi-weekly.
   * There are considered to be 26 events per year with this frequency.
   */
  public static final Frequency P2W = ofWeeks(2);
  /**
   * A periodic frequency of 4 weeks (28 days).
   * Also known as lunar.
   * There are considered to be 13 events per year with this frequency.
   */
  public static final Frequency P4W = ofWeeks(4);
  /**
   * A periodic frequency of 13 weeks (91 days).
   * There are considered to be 4 events per year with this frequency.
   */
  public static final Frequency P13W = ofWeeks(13);
  /**
   * A periodic frequency of 26 weeks (182 days).
   * There are considered to be 2 events per year with this frequency.
   */
  public static final Frequency P26W = ofWeeks(26);
  /**
   * A periodic frequency of 52 weeks (364 days).
   * There is considered to be 1 event per year with this frequency.
   */
  public static final Frequency P52W = ofWeeks(52);
  /**
   * A periodic frequency of 1 month.
   * Also known as monthly.
   * There are 12 events per year with this frequency.
   */
  public static final Frequency P1M = ofMonths(1);
  /**
   * A periodic frequency of 2 months.
   * Also known as bi-monthly.
   * There are 6 events per year with this frequency.
   */
  public static final Frequency P2M = ofMonths(2);
  /**
   * A periodic frequency of 3 months.
   * Also known as quarterly.
   * There are 4 events per year with this frequency.
   */
  public static final Frequency P3M = ofMonths(3);
  /**
   * A periodic frequency of 4 months.
   * There are 3 events per year with this frequency.
   */
  public static final Frequency P4M = ofMonths(4);
  /**
   * A periodic frequency of 6 months.
   * Also known as semi-annual.
   * There are 2 events per year with this frequency.
   */
  public static final Frequency P6M = ofMonths(6);
  /**
   * A periodic frequency of 12 months (1 year).
   * Also known as annual.
   * There is 1 event per year with this frequency.
   */
  public static final Frequency P12M = ofMonths(12);
  /**
   * A periodic frequency matching the term.
   * Also known as zero-coupon.
   * This is represented using the period 10,000 years.
   * There are no events per year with this frequency.
   */
  public static final Frequency TERM = new Frequency(Period.ofYears(TERM_YEARS), "Term");

  /**
   * The period of the frequency.
   */
  private final Period period;
  /**
   * The name of the frequency.
   */
  private final String name;

  //-------------------------------------------------------------------------
  /**
   * Obtains a periodic frequency from a {@code Period}.
   * <p>
   * The period normally consists of either days and weeks, or months and years.
   * It must also be positive and non-zero.
   * <p>
   * If the number of days is an exact multiple of 7 it will be converted to weeks.
   * Months are not normalized into years.
   * <p>
   * The maximum tenor length is 1,000 years.
   * A special tenor length of 10,000 years is accepted which represents the 'Term' frequency.
   *
   * @param period  the period to convert to a periodic frequency
   * @return the periodic frequency
   * @throws IllegalArgumentException if the period is negative, zero or too large
   */
  public static Frequency of(Period period) {
    ArgChecker.notNull(period, "period");
    int days = period.getDays();
    long months = period.toTotalMonths();
    if (months == 0 && days != 0) {
      return ofDays(days);
    }
    if (months > MAX_MONTHS) {
      throw new IllegalArgumentException("Period must not exceed 1000 years");
    }
    return new Frequency(period);
  }

  /**
   * Returns a periodic frequency backed by a period of days.
   * <p>
   * If the number of days is an exact multiple of 7 it will be converted to weeks.
   *
   * @param days  the number of days
   * @return the periodic frequency
   * @throws IllegalArgumentException if days is negative or zero
   */
  public static Frequency ofDays(int days) {
    if (days % 7 == 0) {
      return ofWeeks(days / 7);
    }
    return new Frequency(Period.ofDays(days));
  }

  /**
   * Returns a periodic frequency backed by a period of weeks.
   *
   * @param weeks  the number of weeks
   * @return the periodic frequency
   * @throws IllegalArgumentException if weeks is negative or zero
   */
  public static Frequency ofWeeks(int weeks) {
    return new Frequency(Period.ofWeeks(weeks), "P" + weeks + "W");
  }

  /**
   * Returns a periodic frequency backed by a period of months.
   * <p>
   * Months are not normalized into years.
   *
   * @param months  the number of months
   * @return the periodic frequency
   * @throws IllegalArgumentException if months is negative, zero or over 12,000
   */
  public static Frequency ofMonths(int months) {
    if (months > MAX_MONTHS) {
      throw new IllegalArgumentException("Months must not exceed 12,000");
    }
    return new Frequency(Period.ofMonths(months));
  }

  /**
   * Returns a periodic frequency backed by a period of years.
   *
   * @param years  the number of years
   * @return the periodic frequency
   * @throws IllegalArgumentException if years is negative, zero or over 1,000
   */
  public static Frequency ofYears(int years) {
    if (years > MAX_YEARS) {
      throw new IllegalArgumentException("Years must not exceed 1,000");
    }
    return new Frequency(Period.ofYears(years));
  }

  //-------------------------------------------------------------------------
  /**
   * Parses a formatted string representing the frequency.
   * <p>
   * The format can either be based on ISO-8601, such as 'P3M'
   * or without the 'P' prefix e.g. '2W'.
   * <p>
   * The period must be positive and non-zero.
   *
   * @param toParse  the string representing the frequency
   * @return the frequency
   * @throws IllegalArgumentException if the frequency cannot be parsed
   */
  @FromString
  public static Frequency parse(String toParse) {
    ArgChecker.notNull(toParse, "toParse");
    if (toParse.equalsIgnoreCase("Term")) {
      return TERM;
    }
    String prefixed = toParse.startsWith("P") ? toParse : "P" + toParse;
    try {
      return Frequency.of(Period.parse(prefixed));
    } catch (DateTimeParseException ex) {
      throw new IllegalArgumentException(ex);
    }
  }

  //-------------------------------------------------------------------------
  /**
   * Creates a periodic frequency.
   *
   * @param period  the period to represent
   */
  private Frequency(Period period) {
    this(period, period.toString());
 }

  /**
   * Creates a periodic frequency.
   *
   * @param period  the period to represent
   * @param name  the name
   */
  private Frequency(Period period, String name) {
    ArgChecker.notNull(period, "period");
    ArgChecker.isFalse(period.isZero(), "Period must not be zero");
    ArgChecker.isFalse(period.isNegative(), "Period must not be negative");
    this.period = period;
    this.name = name;
  }

  // safe deserialization
  private Object readResolve() {
    if (this.equals(TERM)) {
      return TERM;
    }
    return of(period);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the underlying period of the frequency.
   *
   * @return the period
   */
  public Period getPeriod() {
    return period;
  }

  /**
   * Checks if the periodic frequency is the 'Term' instance.
   * <p>
   * The term instance corresponds to there being no subdivisions of the entire term.
   *
   * @return true if this is the 'Term' instance
   */
  public boolean isTerm() {
    return this == TERM;
  }

  //-------------------------------------------------------------------------
  /**
   * Normalizes the months and years of this tenor.
   * <p>
   * This method returns a tenor of an equivalent length but with any number
   * of months greater than 12 normalized into a combination of months and years.
   *
   * @return the normalized tenor
   */
  public Frequency normalized() {
    Period norm = period.normalized();
    return (norm != period ? Frequency.of(norm) : this);
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if the periodic frequency is week-based.
   * <p>
   * A week-based frequency consists of an integral number of weeks.
   * There must be no day, month or year element.
   *
   * @return true if this is week-based
   */
  public boolean isWeekBased() {
    return period.toTotalMonths() == 0 && period.getDays() % 7 == 0;
  }

  /**
   * Checks if the periodic frequency is month-based.
   * <p>
   * A month-based frequency consists of an integral number of months.
   * Any year-based frequency is also counted as month-based.
   * There must be no day or week element.
   *
   * @return true if this is month-based
   */
  public boolean isMonthBased() {
    return period.toTotalMonths() > 0 && period.getDays() == 0 && isTerm() == false;
  }

  //-------------------------------------------------------------------------
  /**
   * Calculates the number of events that occur in a year.
   * <p>
   * The number of events per year is the number of times that the period occurs per year.
   * Not all periodic frequency instances can be converted to an integer events per year.
   * All constants declared on this class will return a result.
   * <p>
   * Month-based periodic frequencies are converted by dividing 12 by the number of months.
   * Only the following periodic frequencies return a value - P1M, P2M, P3M, P4M, P6M, P1Y.
   * <p>
   * Day-based and week-based periodic frequencies are converted by dividing 364 by the number of days.
   * Only the following periodic frequencies return a value - P1D, P2D, P4D, P1W, P2W, P4W, P13W, P26W, P52W.
   * <p>
   * The 'Term' periodic frequency returns zero.
   *
   * @return the number of events per year
   */
  public int eventsPerYear() {
    if (isTerm()) {
      return 0;
    }
    long months = period.toTotalMonths();
    int days = period.getDays();
    if (isMonthBased()) {
      if (12 % months == 0) {
        return (int) (12 / months);
      }
    } else if (months == 0 && 364 % days == 0) {
      return (int) (364 / days);
    }
    throw new IllegalArgumentException("Unable to calculate events per year: " + this);
  }

  //-------------------------------------------------------------------------
  /**
   * Gets the value of the specified unit.
   * <p>
   * This will return a value for the years, months and days units.
   * Note that weeks are not included.
   * All other units throw an exception.
   * <p>
   * The 'Term' period is returned as a period of 10,000 years.
   * <p>
   * This method implements {@link TemporalAmount}.
   * It is not intended to be called directly.
   *
   * @param unit  the unit to query
   * @return the value of the unit
   * @throws UnsupportedTemporalTypeException if the unit is not supported
   */
  @Override
  public long get(TemporalUnit unit) {
    return period.get(unit);
  }

  /**
   * Gets the unit of this periodic frequency.
   * <p>
   * This returns a list containing years, months and days.
   * Note that weeks are not included.
   * <p>
   * The 'Term' period is returned as a period of 10,000 years.
   * <p>
   * This method implements {@link TemporalAmount}.
   * It is not intended to be called directly.
   *
   * @return a list containing the years, months and days units
   */
  @Override
  public List<TemporalUnit> getUnits() {
    return period.getUnits();
  }

  /**
   * Adds the period of this frequency to the specified date.
   * <p>
   * This method implements {@link TemporalAmount}.
   * It is not intended to be called directly.
   * Use {@link LocalDate#plus(TemporalAmount)} instead.
   *
   * @param temporal  the temporal object to add to
   * @return the result with this frequency added
   * @throws DateTimeException if unable to add
   * @throws ArithmeticException if numeric overflow occurs
   */
  @Override
  public Temporal addTo(Temporal temporal) {
    return period.addTo(temporal);
  }

  /**
   * Subtracts the period of this frequency from the specified date.
   * <p>
   * This method implements {@link TemporalAmount}.
   * It is not intended to be called directly.
   * Use {@link LocalDate#minus(TemporalAmount)} instead.
   *
   * @param temporal  the temporal object to subtract from
   * @return the result with this frequency subtracted
   * @throws DateTimeException if unable to subtract
   * @throws ArithmeticException if numeric overflow occurs
   */
  @Override
  public Temporal subtractFrom(Temporal temporal) {
    return period.subtractFrom(temporal);
  }

  //-------------------------------------------------------------------------
  /**
   * Checks if this periodic frequency equals another periodic frequency.
   * <p>
   * The comparison checks the frequency period.
   * 
   * @param obj  the other frequency, null returns false
   * @return true if equal
   */
  @Override
  public boolean equals(Object obj) {
    if (obj == this) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    Frequency other = (Frequency) obj;
    return period.equals(other.period);
  }

  /**
   * Returns a suitable hash code for the periodic frequency.
   * 
   * @return the hash code
   */
  @Override
  public int hashCode() {
    return period.hashCode();
  }

  /**
   * Returns a formatted string representing the periodic frequency.
   * <p>
   * The format is a combination of the quantity and unit, such as P1D, P2W, P3M, P4Y.
   * The 'Term' amount is returned as 'Term'.
   *
   * @return the formatted frequency
   */
  @ToString
  @Override
  public String toString() {
    return name;
  }

}
