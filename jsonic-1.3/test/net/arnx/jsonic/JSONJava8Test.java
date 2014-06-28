package net.arnx.jsonic;

import static org.junit.Assert.*;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Month;
import java.time.MonthDay;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Period;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import org.junit.Test;

public class JSONJava8Test {

	@Test
	public void testEncode() {
		Java8Bean bean = new Java8Bean();
		bean.duration = Duration.ofDays(3L);
		bean.instant = Instant.from(ZonedDateTime.of(2010, 3, 3, 2, 2, 2, 2, ZoneOffset.of("Z")));
		bean.localDate = LocalDate.of(2010, 3, 3);
		bean.localDateTime = LocalDateTime.of(2010, 3, 3, 2, 2, 2, 2);
		bean.localTime = LocalTime.of(2, 2, 2);
		bean.monthDay = MonthDay.of(2, 2);
		bean.offsetDateTimey = OffsetDateTime.of(2010, 3, 3, 2, 2, 2, 2, ZoneOffset.of("Z"));
		bean.offsetTime = OffsetTime.of(2, 2, 2, 2, ZoneOffset.of("Z"));
		bean.period = Period.of(1, 1, 3);
		bean.year = Year.of(2010);
		bean.month = Month.of(3);
		bean.yearMonth = YearMonth.of(2010, 3);
		bean.zonedDateTime = ZonedDateTime.of(2010, 3, 3, 2, 2, 2, 2, ZoneOffset.of("Z"));
		bean.zoneId = ZoneId.of("UTC");
		bean.zonedOffset = ZoneOffset.of("Z");

		assertEquals("{"
				+ "\"duration\":\"PT72H\","
				+ "\"instant\":\"2010-03-03T02:02:02.000000002Z\","
				+ "\"localDate\":\"2010-03-03\","
				+ "\"localDateTime\":\"2010-03-03T02:02:02.000000002\","
				+ "\"localTime\":\"02:02:02\","
				+ "\"month\":\"MARCH\","
				+ "\"monthDay\":\"--02-02\","
				+ "\"offsetDateTimey\":\"2010-03-03T02:02:02.000000002Z\","
				+ "\"offsetTime\":\"02:02:02.000000002Z\","
				+ "\"period\":\"P1Y1M3D\","
				+ "\"year\":\"2010\","
				+ "\"yearMonth\":\"2010-03\","
				+ "\"zoneId\":\"UTC\","
				+ "\"zonedDateTime\":\"2010-03-03T02:02:02.000000002Z\","
				+ "\"zonedOffset\":\"Z\""
				+ "}", JSON.encode(bean));
	}

	@Test
	public void testDecode() {
		Java8Bean bean = new Java8Bean();
		bean.duration = Duration.ofDays(3L);
		bean.instant = Instant.from(ZonedDateTime.of(2010, 3, 3, 2, 2, 2, 2, ZoneOffset.of("Z")));
		bean.localDate = LocalDate.of(2010, 3, 3);
		bean.localDateTime = LocalDateTime.of(2010, 3, 3, 2, 2, 2, 2);
		bean.localTime = LocalTime.of(2, 2, 2);
		bean.monthDay = MonthDay.of(2, 2);
		bean.offsetDateTimey = OffsetDateTime.of(2010, 3, 3, 2, 2, 2, 2, ZoneOffset.of("Z"));
		bean.offsetTime = OffsetTime.of(2, 2, 2, 2, ZoneOffset.of("Z"));
		bean.period = Period.of(1, 1, 3);
		bean.year = Year.of(2010);
		bean.month = Month.of(3);
		bean.yearMonth = YearMonth.of(2010, 3);
		bean.zonedDateTime = ZonedDateTime.of(2010, 3, 3, 2, 2, 2, 2, ZoneOffset.of("Z"));
		bean.zoneId = ZoneId.of("UTC");
		bean.zonedOffset = ZoneOffset.of("Z");

		assertEquals(bean, JSON.decode("{"
				+ "\"duration\":\"PT72H\","
				+ "\"instant\":\"2010-03-03T02:02:02.000000002Z\","
				+ "\"localDate\":\"2010-03-03\","
				+ "\"localDateTime\":\"2010-03-03T02:02:02.000000002\","
				+ "\"localTime\":\"02:02:02\","
				+ "\"month\":\"MARCH\","
				+ "\"monthDay\":\"--02-02\","
				+ "\"offsetDateTimey\":\"2010-03-03T02:02:02.000000002Z\","
				+ "\"offsetTime\":\"02:02:02.000000002Z\","
				+ "\"period\":\"P1Y1M3D\","
				+ "\"year\":\"2010\","
				+ "\"yearMonth\":\"2010-03\","
				+ "\"zoneId\":\"UTC\","
				+ "\"zonedDateTime\":\"2010-03-03T02:02:02.000000002Z\","
				+ "\"zonedOffset\":\"Z\""
				+ "}", Java8Bean.class));
	}

	public static class Java8Bean {
		public Duration duration;
		public Instant instant;
		public LocalDate localDate;
		public LocalDateTime localDateTime;
		public LocalTime localTime;
		public MonthDay monthDay;
		public OffsetDateTime offsetDateTimey;
		public OffsetTime offsetTime;
		public Period period;
		public Year year;
		public Month month;
		public YearMonth yearMonth;
		public ZonedDateTime zonedDateTime;
		public ZoneId zoneId;
		public ZoneOffset zonedOffset;
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((duration == null) ? 0 : duration.hashCode());
			result = prime * result
					+ ((instant == null) ? 0 : instant.hashCode());
			result = prime * result
					+ ((localDate == null) ? 0 : localDate.hashCode());
			result = prime * result
					+ ((localDateTime == null) ? 0 : localDateTime.hashCode());
			result = prime * result
					+ ((localTime == null) ? 0 : localTime.hashCode());
			result = prime * result + ((month == null) ? 0 : month.hashCode());
			result = prime * result
					+ ((monthDay == null) ? 0 : monthDay.hashCode());
			result = prime
					* result
					+ ((offsetDateTimey == null) ? 0 : offsetDateTimey
							.hashCode());
			result = prime * result
					+ ((offsetTime == null) ? 0 : offsetTime.hashCode());
			result = prime * result
					+ ((period == null) ? 0 : period.hashCode());
			result = prime * result + ((year == null) ? 0 : year.hashCode());
			result = prime * result
					+ ((yearMonth == null) ? 0 : yearMonth.hashCode());
			result = prime * result
					+ ((zonedDateTime == null) ? 0 : zonedDateTime.hashCode());
			result = prime * result
					+ ((zoneId == null) ? 0 : zoneId.hashCode());
			result = prime * result
					+ ((zonedOffset == null) ? 0 : zonedOffset.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Java8Bean other = (Java8Bean) obj;
			if (duration == null) {
				if (other.duration != null)
					return false;
			} else if (!duration.equals(other.duration))
				return false;
			if (instant == null) {
				if (other.instant != null)
					return false;
			} else if (!instant.equals(other.instant))
				return false;
			if (localDate == null) {
				if (other.localDate != null)
					return false;
			} else if (!localDate.equals(other.localDate))
				return false;
			if (localDateTime == null) {
				if (other.localDateTime != null)
					return false;
			} else if (!localDateTime.equals(other.localDateTime))
				return false;
			if (localTime == null) {
				if (other.localTime != null)
					return false;
			} else if (!localTime.equals(other.localTime))
				return false;
			if (month != other.month)
				return false;
			if (monthDay == null) {
				if (other.monthDay != null)
					return false;
			} else if (!monthDay.equals(other.monthDay))
				return false;
			if (offsetDateTimey == null) {
				if (other.offsetDateTimey != null)
					return false;
			} else if (!offsetDateTimey.equals(other.offsetDateTimey))
				return false;
			if (offsetTime == null) {
				if (other.offsetTime != null)
					return false;
			} else if (!offsetTime.equals(other.offsetTime))
				return false;
			if (period == null) {
				if (other.period != null)
					return false;
			} else if (!period.equals(other.period))
				return false;
			if (year == null) {
				if (other.year != null)
					return false;
			} else if (!year.equals(other.year))
				return false;
			if (yearMonth == null) {
				if (other.yearMonth != null)
					return false;
			} else if (!yearMonth.equals(other.yearMonth))
				return false;
			if (zonedDateTime == null) {
				if (other.zonedDateTime != null)
					return false;
			} else if (!zonedDateTime.equals(other.zonedDateTime))
				return false;
			if (zoneId == null) {
				if (other.zoneId != null)
					return false;
			} else if (!zoneId.equals(other.zoneId))
				return false;
			if (zonedOffset == null) {
				if (other.zonedOffset != null)
					return false;
			} else if (!zonedOffset.equals(other.zonedOffset))
				return false;
			return true;
		}
		@Override
		public String toString() {
			return "Java8Bean [duration=" + duration + ", instant=" + instant
					+ ", localDate=" + localDate + ", localDateTime="
					+ localDateTime + ", localTime=" + localTime
					+ ", monthDay=" + monthDay + ", offsetDateTimey="
					+ offsetDateTimey + ", offsetTime=" + offsetTime
					+ ", period=" + period + ", year=" + year + ", month="
					+ month + ", yearMonth=" + yearMonth + ", zonedDateTime="
					+ zonedDateTime + ", zoneId=" + zoneId
					+ ", zonedOffset=" + zonedOffset + "]";
		}


	}
}
