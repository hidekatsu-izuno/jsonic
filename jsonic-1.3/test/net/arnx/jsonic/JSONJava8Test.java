package net.arnx.jsonic;

import static org.junit.Assert.*;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.DayOfWeek;
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
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.junit.Test;

public class JSONJava8Test {

	@Test
	public void testEncode() {
		Java8DataTimeAPIBean bean = new Java8DataTimeAPIBean();
		bean.dayOfWeek = DayOfWeek.of(3);
		bean.duration = Duration.ofDays(3L);
		bean.instant = Instant.from(ZonedDateTime.of(2010, 3, 3, 2, 2, 2, 2, ZoneOffset.of("Z")));
		bean.localDate = LocalDate.of(2010, 3, 3);
		bean.localDateTime = LocalDateTime.of(2010, 3, 3, 2, 2, 2, 2);
		bean.localTime = LocalTime.of(2, 2, 2, 2);
		bean.monthDay = MonthDay.of(2, 2);
		bean.offsetDateTime = OffsetDateTime.of(2010, 3, 3, 2, 2, 2, 2, ZoneOffset.of("Z"));
		bean.offsetTime = OffsetTime.of(2, 2, 2, 2, ZoneOffset.of("Z"));
		bean.period = Period.of(1, 1, 3);
		bean.year = Year.of(2010);
		bean.month = Month.of(3);
		bean.yearMonth = YearMonth.of(2010, 3);
		bean.zonedDateTime = ZonedDateTime.of(2010, 3, 3, 2, 2, 2, 2, ZoneOffset.of("Z"));
		bean.zoneId = ZoneId.of("UTC");
		bean.zonedOffset = ZoneOffset.of("Z");
		assertEquals("{"
				+ "\"dayOfWeek\":\"WEDNESDAY\","
				+ "\"duration\":\"PT72H\","
				+ "\"instant\":\"2010-03-03T02:02:02.000000002Z\","
				+ "\"localDate\":\"2010-03-03\","
				+ "\"localDateTime\":\"2010-03-03T02:02:02.000000002\","
				+ "\"localTime\":\"02:02:02.000000002\","
				+ "\"month\":\"MARCH\","
				+ "\"monthDay\":\"--02-02\","
				+ "\"offsetDateTime\":\"2010-03-03T02:02:02.000000002Z\","
				+ "\"offsetTime\":\"02:02:02.000000002Z\","
				+ "\"period\":\"P1Y1M3D\","
				+ "\"year\":\"2010\","
				+ "\"yearMonth\":\"2010-03\","
				+ "\"zoneId\":\"UTC\","
				+ "\"zonedDateTime\":\"2010-03-03T02:02:02.000000002Z\","
				+ "\"zonedOffset\":\"Z\""
				+ "}", JSON.encode(bean));

		Java8DataTimeAPIFormatterBean fbean = new Java8DataTimeAPIFormatterBean();
		fbean.instant = Instant.from(ZonedDateTime.of(2010, 3, 3, 2, 2, 2, 2, ZoneOffset.of("Z")));
		fbean.localDate = LocalDate.of(2010, 3, 3);
		fbean.localDateTime = LocalDateTime.of(2010, 3, 3, 2, 2, 2, 2);
		fbean.localTime = LocalTime.of(2, 2, 2, 2);
		fbean.monthDay = MonthDay.of(2, 2);
		fbean.offsetDateTime = OffsetDateTime.of(2010, 3, 3, 2, 2, 2, 2, ZoneOffset.of("Z"));
		fbean.offsetTime = OffsetTime.of(2, 2, 2, 2, ZoneOffset.of("Z"));
		fbean.year = Year.of(2010);
		fbean.month = Month.of(3);
		fbean.dayOfWeek = DayOfWeek.of(3);
		fbean.yearMonth = YearMonth.of(2010, 3);
		fbean.zonedDateTime = ZonedDateTime.of(2010, 3, 3, 2, 2, 2, 2, ZoneOffset.of("Z"));
		assertEquals("{"
				+ "\"dayOfWeek\":\"水\","
				+ "\"instant\":\"2010/3/3 11:2:2.000000002\","
				+ "\"localDate\":\"2010/3/3\","
				+ "\"localDateTime\":\"2010/3/3 2:2:2.000000002\","
				+ "\"localTime\":\"2:2:2.000000002\","
				+ "\"month\":\"3月\","
				+ "\"monthDay\":\"2/2\","
				+ "\"offsetDateTime\":\"2010/3/3 2:2:2.000000002+0000\","
				+ "\"offsetTime\":\"2:2:2.000000002+0000\","
				+ "\"year\":\"2010\","
				+ "\"yearMonth\":\"2010/3\","
				+ "\"zonedDateTime\":\"2010/3/3 2:2:2.000000002Z\""
				+ "}", JSON.encode(fbean));

		Java8OptionalBean obean = new Java8OptionalBean();
		obean.optionalInt = OptionalInt.of(2);
		obean.optionalLong = OptionalLong.of(10000L);
		obean.optionalDouble = OptionalDouble.of(100.123D);
		obean.optionalBigDecimal = Optional.of(new BigDecimal("3.14"));
		obean.optionalBean = Optional.of(new OptionalBeanBean());
		assertEquals("{"
				+ "\"optionalBean\":{\"text\":\"text\"},"
				+ "\"optionalBigDecimal\":3.14,"
				+ "\"optionalDouble\":100.123,"
				+ "\"optionalInt\":2,"
				+ "\"optionalLong\":10000"
				+ "}", JSON.encode(obean));

		obean = new Java8OptionalBean();
		obean.optionalInt = OptionalInt.empty();
		obean.optionalLong = OptionalLong.empty();
		obean.optionalDouble = OptionalDouble.empty();
		obean.optionalBigDecimal = Optional.empty();
		obean.optionalBean = Optional.empty();
		assertEquals("{"
				+ "\"optionalBean\":null,"
				+ "\"optionalBigDecimal\":null,"
				+ "\"optionalDouble\":null,"
				+ "\"optionalInt\":null,"
				+ "\"optionalLong\":null"
				+ "}", JSON.encode(obean));

		PathBean pbean = new PathBean();
		pbean.path = Paths.get("./test.txt");
		assertEquals("{"
				+ "\"path\":\".\\\\test.txt\""
				+ "}", JSON.encode(pbean));
	}

	@Test
	public void testDecode() {
		Java8DataTimeAPIBean bean = new Java8DataTimeAPIBean();
		bean.dayOfWeek = DayOfWeek.of(3);
		bean.duration = Duration.ofDays(3L);
		bean.instant = Instant.from(ZonedDateTime.of(2010, 3, 3, 2, 2, 2, 2, ZoneOffset.of("Z")));
		bean.localDate = LocalDate.of(2010, 3, 3);
		bean.localDateTime = LocalDateTime.of(2010, 3, 3, 2, 2, 2, 2);
		bean.localTime = LocalTime.of(2, 2, 2, 2);
		bean.monthDay = MonthDay.of(2, 2);
		bean.offsetDateTime = OffsetDateTime.of(2010, 3, 3, 2, 2, 2, 2, ZoneOffset.of("Z"));
		bean.offsetTime = OffsetTime.of(2, 2, 2, 2, ZoneOffset.of("Z"));
		bean.period = Period.of(1, 1, 3);
		bean.year = Year.of(2010);
		bean.month = Month.of(3);
		bean.yearMonth = YearMonth.of(2010, 3);
		bean.dayOfWeek = DayOfWeek.WEDNESDAY;
		bean.zonedDateTime = ZonedDateTime.of(2010, 3, 3, 2, 2, 2, 2, ZoneOffset.of("Z"));
		bean.zoneId = ZoneId.of("UTC");
		bean.zonedOffset = ZoneOffset.of("Z");
		assertEquals(bean, JSON.decode("{"
				+ "\"dayOfWeek\":\"WEDNESDAY\","
				+ "\"duration\":\"PT72H\","
				+ "\"instant\":\"2010-03-03T02:02:02.000000002Z\","
				+ "\"localDate\":\"2010-03-03\","
				+ "\"localDateTime\":\"2010-03-03T02:02:02.000000002\","
				+ "\"localTime\":\"02:02:02.000000002\","
				+ "\"month\":\"MARCH\","
				+ "\"monthDay\":\"--02-02\","
				+ "\"offsetDateTime\":\"2010-03-03T02:02:02.000000002Z\","
				+ "\"offsetTime\":\"02:02:02.000000002Z\","
				+ "\"period\":\"P1Y1M3D\","
				+ "\"year\":\"2010\","
				+ "\"yearMonth\":\"2010-03\","
				+ "\"zoneId\":\"UTC\","
				+ "\"zonedDateTime\":\"2010-03-03T02:02:02.000000002Z\","
				+ "\"zonedOffset\":\"Z\""
				+ "}", Java8DataTimeAPIBean.class));

		Java8DataTimeAPIFormatterBean fbean = new Java8DataTimeAPIFormatterBean();
		fbean.instant = Instant.from(ZonedDateTime.of(2010, 3, 3, 2, 2, 2, 2, ZoneOffset.of("Z")));
		fbean.localDate = LocalDate.of(2010, 3, 3);
		fbean.localDateTime = LocalDateTime.of(2010, 3, 3, 2, 2, 2, 2);
		fbean.localTime = LocalTime.of(2, 2, 2, 2);
		fbean.monthDay = MonthDay.of(2, 2);
		fbean.offsetDateTime = OffsetDateTime.of(2010, 3, 3, 2, 2, 2, 2, ZoneOffset.of("Z"));
		fbean.offsetTime = OffsetTime.of(2, 2, 2, 2, ZoneOffset.of("Z"));
		fbean.year = Year.of(2010);
		fbean.month = Month.of(3);
		fbean.dayOfWeek = DayOfWeek.of(3);
		fbean.yearMonth = YearMonth.of(2010, 3);
		fbean.zonedDateTime = ZonedDateTime.of(2010, 3, 3, 2, 2, 2, 2, ZoneOffset.of("Z"));
		assertEquals(fbean, JSON.decode("{"
				+ "\"dayOfWeek\":\"水\","
				+ "\"instant\":\"2010/3/3 11:2:2.000000002\","
				+ "\"localDate\":\"2010/3/3\","
				+ "\"localDateTime\":\"2010/3/3 2:2:2.000000002\","
				+ "\"localTime\":\"2:2:2.000000002\","
				+ "\"month\":\"3月\","
				+ "\"monthDay\":\"2/2\","
				+ "\"offsetDateTime\":\"2010/3/3 2:2:2.000000002+0000\","
				+ "\"offsetTime\":\"2:2:2.000000002+0000\","
				+ "\"year\":\"2010\","
				+ "\"yearMonth\":\"2010/3\","
				+ "\"zonedDateTime\":\"2010/3/3 2:2:2.000000002Z\""
				+ "}", Java8DataTimeAPIFormatterBean.class));

		Java8OptionalBean obean = new Java8OptionalBean();
		obean.optionalInt = OptionalInt.of(2);
		obean.optionalLong = OptionalLong.of(10000L);
		obean.optionalDouble = OptionalDouble.of(100.123D);
		obean.optionalBigDecimal = Optional.of(new BigDecimal("3.14"));
		obean.optionalBean = Optional.of(new OptionalBeanBean());
		assertEquals(obean, JSON.decode("{"
				+ "\"optionalBean\":{\"text\":\"text\"},"
				+ "\"optionalBigDecimal\":3.14,"
				+ "\"optionalDouble\":100.123,"
				+ "\"optionalInt\":2,"
				+ "\"optionalLong\":10000"
				+ "}",  Java8OptionalBean.class));

		obean = new Java8OptionalBean();
		obean.optionalInt = OptionalInt.empty();
		obean.optionalLong = OptionalLong.empty();
		obean.optionalDouble = OptionalDouble.empty();
		obean.optionalBigDecimal = Optional.empty();
		obean.optionalBean = Optional.empty();
		assertEquals(obean, JSON.decode("{"
				+ "\"optionalBean\":null,"
				+ "\"optionalBigDecimal\":null,"
				+ "\"optionalDouble\":null,"
				+ "\"optionalInt\":null,"
				+ "\"optionalLong\":null"
				+ "}", Java8OptionalBean.class));

		PathBean pbean = new PathBean();
		pbean.path = Paths.get("./test.txt");
		assertEquals(pbean, JSON.decode("{"
				+ "\"path\":\".\\\\test.txt\""
				+ "}", PathBean.class));
	}

	public static class Java8DataTimeAPIBean {
		public Duration duration;
		public Instant instant;
		public LocalDate localDate;
		public LocalDateTime localDateTime;
		public LocalTime localTime;
		public MonthDay monthDay;
		public OffsetDateTime offsetDateTime;
		public OffsetTime offsetTime;
		public Period period;
		public Year year;
		public Month month;
		public YearMonth yearMonth;
		public DayOfWeek dayOfWeek;
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
					+ ((offsetDateTime == null) ? 0 : offsetDateTime
							.hashCode());
			result = prime * result
					+ ((offsetTime == null) ? 0 : offsetTime.hashCode());
			result = prime * result
					+ ((period == null) ? 0 : period.hashCode());
			result = prime * result + ((year == null) ? 0 : year.hashCode());
			result = prime * result
					+ ((yearMonth == null) ? 0 : yearMonth.hashCode());
			result = prime * result
					+ ((dayOfWeek == null) ? 0 : dayOfWeek.hashCode());
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
			Java8DataTimeAPIBean other = (Java8DataTimeAPIBean) obj;
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
			if (offsetDateTime == null) {
				if (other.offsetDateTime != null)
					return false;
			} else if (!offsetDateTime.equals(other.offsetDateTime))
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
			if (dayOfWeek == null) {
				if (other.dayOfWeek != null)
					return false;
			} else if (!dayOfWeek.equals(other.dayOfWeek))
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
			return "Java8DataTimeAPIBean [duration=" + duration
					+ ", instant=" + instant
					+ ", localDate=" + localDate
					+ ", localDateTime=" + localDateTime
					+ ", localTime=" + localTime
					+ ", monthDay=" + monthDay
					+ ", offsetDateTimey=" + offsetDateTime
					+ ", offsetTime=" + offsetTime
					+ ", period=" + period
					+ ", year=" + year
					+ ", month=" + month
					+ ", yearMonth=" + yearMonth
					+ ", dayOfWeek=" + dayOfWeek
					+ ", zonedDateTime=" + zonedDateTime
					+ ", zoneId=" + zoneId
					+ ", zonedOffset=" + zonedOffset + "]";
		}
	}

	public static class Java8DataTimeAPIFormatterBean {
		@JSONHint(format = "yyyy/M/d H:m:s.nnnnnnnnn")
		public Instant instant;
		@JSONHint(format = "yyyy/M/d")
		public LocalDate localDate;
		@JSONHint(format = "yyyy/M/d H:m:s.nnnnnnnnn")
		public LocalDateTime localDateTime;
		@JSONHint(format = "H:m:s.nnnnnnnnn")
		public LocalTime localTime;
		@JSONHint(format = "M/d")
		public MonthDay monthDay;
		@JSONHint(format = "yyyy/M/d H:m:s.nnnnnnnnnZ")
		public OffsetDateTime offsetDateTime;
		@JSONHint(format = "H:m:s.nnnnnnnnnZ")
		public OffsetTime offsetTime;
		@JSONHint(format = "yyyy")
		public Year year;
		@JSONHint(format = "MMMM")
		public Month month;
		@JSONHint(format = "E")
		public DayOfWeek dayOfWeek;
		@JSONHint(format = "yyyy/M")
		public YearMonth yearMonth;
		@JSONHint(format = "yyyy/M/d H:m:s.nnnnnnnnnz")
		public ZonedDateTime zonedDateTime;
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((instant == null) ? 0 : instant.hashCode());
			result = prime * result
					+ ((localDate == null) ? 0 : localDate.hashCode());
			result = prime * result
					+ ((localDateTime == null) ? 0 : localDateTime.hashCode());
			result = prime * result
					+ ((localTime == null) ? 0 : localTime.hashCode());
			result = prime * result
					+ ((monthDay == null) ? 0 : monthDay.hashCode());
			result = prime
					* result
					+ ((offsetDateTime == null) ? 0 : offsetDateTime
							.hashCode());
			result = prime * result
					+ ((offsetTime == null) ? 0 : offsetTime.hashCode());
			result = prime * result + ((year == null) ? 0 : year.hashCode());
			result = prime * result + ((month == null) ? 0 : month.hashCode());
			result = prime * result + ((dayOfWeek == null) ? 0 : dayOfWeek.hashCode());
			result = prime * result
					+ ((yearMonth == null) ? 0 : yearMonth.hashCode());
			result = prime * result
					+ ((zonedDateTime == null) ? 0 : zonedDateTime.hashCode());
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
			Java8DataTimeAPIFormatterBean other = (Java8DataTimeAPIFormatterBean) obj;
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
			if (monthDay == null) {
				if (other.monthDay != null)
					return false;
			} else if (!monthDay.equals(other.monthDay))
				return false;
			if (offsetDateTime == null) {
				if (other.offsetDateTime != null)
					return false;
			} else if (!offsetDateTime.equals(other.offsetDateTime))
				return false;
			if (offsetTime == null) {
				if (other.offsetTime != null)
					return false;
			} else if (!offsetTime.equals(other.offsetTime))
				return false;
			if (year == null) {
				if (other.year != null)
					return false;
			} else if (!year.equals(other.year))
				return false;
			if (month != other.month)
				return false;
			if (dayOfWeek != other.dayOfWeek)
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
			return true;
		}

		@Override
		public String toString() {
			return "Java8DataTimeAPIFormatterBean [instant=" + instant
					+ ", localDate=" + localDate
					+ ", localDateTime=" + localDateTime
					+ ", localTime=" + localTime
					+ ", monthDay=" + monthDay
					+ ", offsetDateTimey=" + offsetDateTime
					+ ", offsetTime=" + offsetTime
					+ ", year=" + year
					+ ", month=" + month
					+ ", yearMonth=" + yearMonth
					+ ", zonedDateTime=" + zonedDateTime
					+ "]";
		}
	}

	public static class Java8OptionalBean {
		public OptionalInt optionalInt;
		public OptionalLong optionalLong;
		public OptionalDouble optionalDouble;
		public Optional<BigDecimal> optionalBigDecimal;
		public Optional<OptionalBeanBean> optionalBean;
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result
					+ ((optionalBean == null) ? 0 : optionalBean.hashCode());
			result = prime
					* result
					+ ((optionalBigDecimal == null) ? 0 : optionalBigDecimal
							.hashCode());
			result = prime
					* result
					+ ((optionalDouble == null) ? 0 : optionalDouble.hashCode());
			result = prime * result
					+ ((optionalInt == null) ? 0 : optionalInt.hashCode());
			result = prime * result
					+ ((optionalLong == null) ? 0 : optionalLong.hashCode());
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
			Java8OptionalBean other = (Java8OptionalBean) obj;
			if (optionalBean == null) {
				if (other.optionalBean != null)
					return false;
			} else if (!optionalBean.equals(other.optionalBean))
				return false;
			if (optionalBigDecimal == null) {
				if (other.optionalBigDecimal != null)
					return false;
			} else if (!optionalBigDecimal.equals(other.optionalBigDecimal))
				return false;
			if (optionalDouble == null) {
				if (other.optionalDouble != null)
					return false;
			} else if (!optionalDouble.equals(other.optionalDouble))
				return false;
			if (optionalInt == null) {
				if (other.optionalInt != null)
					return false;
			} else if (!optionalInt.equals(other.optionalInt))
				return false;
			if (optionalLong == null) {
				if (other.optionalLong != null)
					return false;
			} else if (!optionalLong.equals(other.optionalLong))
				return false;
			return true;
		}
		@Override
		public String toString() {
			return "Java8OptionalBean [optionalInt=" + optionalInt
					+ ", optionalLong=" + optionalLong + ", optionalDouble="
					+ optionalDouble + ", optionalBigDecimal="
					+ optionalBigDecimal + ", optionalBean=" + optionalBean
					+ "]";
		}
	}

	public static class OptionalBeanBean {
		public String text = "text";

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((text == null) ? 0 : text.hashCode());
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
			OptionalBeanBean other = (OptionalBeanBean) obj;
			if (text == null) {
				if (other.text != null)
					return false;
			} else if (!text.equals(other.text))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "OptionalBeanBean [text=" + text + "]";
		}
	}

	public static class PathBean {
		public Path path;

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((path == null) ? 0 : path.hashCode());
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
			PathBean other = (PathBean) obj;
			if (path == null) {
				if (other.path != null)
					return false;
			} else if (!path.equals(other.path))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "PathBean [path=" + path + "]";
		}
	}
}
