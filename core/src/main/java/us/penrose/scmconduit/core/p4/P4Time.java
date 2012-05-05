package us.penrose.scmconduit.core.p4;

public class P4Time {
	private final int year;
	private final int monthOfYear;
	private final int dayOfMonth;
	private final int hourOfDay;
	private final int minuteOfHour;
	private final int secondOfMinute;
	
	public P4Time(int year, int monthOfYear, int dayOfMonth, int hourOfDay,
			int minuteOfHour, int secondOfMinute) {
		super();
		this.year = year;
		this.monthOfYear = monthOfYear;
		this.dayOfMonth = dayOfMonth;
		this.hourOfDay = hourOfDay;
		this.minuteOfHour = minuteOfHour;
		this.secondOfMinute = secondOfMinute;
	}
	
	@Override
	public String toString() {
		return year + "-" + monthOfYear + "-" + dayOfMonth + " " + hourOfDay + ":" + minuteOfHour + ":" + secondOfMinute;
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (dayOfMonth ^ (dayOfMonth >>> 32));
		result = prime * result + (int) (hourOfDay ^ (hourOfDay >>> 32));
		result = prime * result + (int) (minuteOfHour ^ (minuteOfHour >>> 32));
		result = prime * result + (int) (monthOfYear ^ (monthOfYear >>> 32));
		result = prime * result
				+ (int) (secondOfMinute ^ (secondOfMinute >>> 32));
		result = prime * result + (int) (year ^ (year >>> 32));
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
		P4Time other = (P4Time) obj;
		if (dayOfMonth != other.dayOfMonth)
			return false;
		if (hourOfDay != other.hourOfDay)
			return false;
		if (minuteOfHour != other.minuteOfHour)
			return false;
		if (monthOfYear != other.monthOfYear)
			return false;
		if (secondOfMinute != other.secondOfMinute)
			return false;
		if (year != other.year)
			return false;
		return true;
	}
	
	
	public int year() {
		return year;
	}
	
	public int monthOfYear() {
		return monthOfYear;
	}
	
	public int dayOfMonth() {
		return dayOfMonth;
	}
	
	public int hourOfDay() {
		return hourOfDay;
	}
	public int minuteOfHour() {
		return minuteOfHour;
	}
	public int secondOfMinute() {
		return secondOfMinute;
	}
	
}
