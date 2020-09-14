package populator.instance;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Defaults {
	
	public static FluentApi get(int counter) {
		FluentApi fluentApi = new FluentApi();
		string(fluentApi, counter);
		primitiveAndWrappers(fluentApi, counter);
		bigdecimal(fluentApi, counter);
		date(fluentApi, counter);
		calendar(fluentApi, counter);
		sqlDate(fluentApi, counter);
		time(fluentApi, counter);
		timeStamp(fluentApi, counter);
		collections(fluentApi, counter);
		maps(fluentApi, counter);
		arrays(fluentApi, counter);
		return fluentApi;
	}

	private static void timeStamp(FluentApi fluentApi, int counter) {
		fluentApi.putKey(new SmartClass(Timestamp.class)).putValue(new Timestamp(0));
	}

	private static void time(FluentApi fluentApi, int counter) {
		fluentApi.putKey(new SmartClass(Time.class)).putValue(new Time(0));
	}

	private static void sqlDate(FluentApi fluentApi, int counter) {
		fluentApi.putKey(new SmartClass(java.sql.Date.class)).putValue(new java.sql.Date(0));
	}

	private static void calendar(FluentApi fluentApi, int counter) {
		fluentApi.putKey(new SmartClass(Calendar.class)).putValue(getCalendarDate0());
	}

	private static void date(FluentApi fluentApi, int counter) {
		fluentApi.putKey(new SmartClass(Date.class)).putValue(new Date(0));
	}

	private static void bigdecimal(FluentApi fluentApi, int counter) {
		fluentApi.putKey(new SmartClass(BigInteger.class)).putValue(new BigInteger("0"));
		fluentApi.putKey(new SmartClass(BigDecimal.class)).putValue(new BigDecimal("0"));
	}

	private static void arrays(FluentApi fluentApi, int counter) {
		fluentApi.putKey(new SmartClass(byte[].class)).putValue(new byte[] { 0x00 });
		fluentApi.putKey(new SmartClass(char[].class)).putValue(new char[] { '0' });
		fluentApi.putKey(new SmartClass(Byte[].class)).putValue(new Byte[] { 0x00 });
		fluentApi.putKey(new SmartClass(Character[].class)).putValue(new Character[] { '0' });
	}

	private static void maps(FluentApi fluentApi, int counter) {
		fluentApi.putKey(new SmartClass(Map.class)).putValue(new HashMap<Object, Object>());
		fluentApi.putKey(new SmartClass(HashMap.class)).putValue(new HashMap<Object, Object>());
		fluentApi.putKey(new SmartClass(LinkedHashMap.class)).putValue(new LinkedHashMap<Object, Object>());
	}

	private static void primitiveAndWrappers(FluentApi fluentApi, int counter) {
		fluentApi.putKey(new SmartClass(boolean.class)).putValue(new Boolean(false));
		fluentApi.putKey(new SmartClass(byte.class)).putValue(new Byte((byte) 0));
		fluentApi.putKey(new SmartClass(char.class)).putValue(new Character((char) ('a' + counter)));
		fluentApi.putKey(new SmartClass(short.class)).putValue(new Short((short) 0));
		fluentApi.putKey(new SmartClass(int.class)).putValue(0 + counter);
		fluentApi.putKey(new SmartClass(long.class)).putValue(0L + counter);
		fluentApi.putKey(new SmartClass(float.class)).putValue(0F + counter);
		fluentApi.putKey(new SmartClass(double.class)).putValue(0D + counter);
		fluentApi.putKey(new SmartClass(Boolean.class)).putValue(new Boolean(false));
		fluentApi.putKey(new SmartClass(Byte.class)).putValue(new Byte((byte) 0));
		fluentApi.putKey(new SmartClass(Character.class)).putValue(new Character((char) ('a' + counter)));
		fluentApi.putKey(new SmartClass(Short.class)).putValue(new Short((short) (0+ counter)));
		fluentApi.putKey(new SmartClass(Integer.class)).putValue(0 + counter);
		fluentApi.putKey(new SmartClass(Long.class)).putValue(0L + counter);
		fluentApi.putKey(new SmartClass(Float.class)).putValue(0F + counter);
		fluentApi.putKey(new SmartClass(Double.class)).putValue(0D + counter);
	}

	private static void collections(FluentApi fluentApi, int counter) {
		fluentApi.putKey(new SmartClass(Collection.class)).putValue(new ArrayList<Object>());
		fluentApi.putKey(new SmartClass(List.class)).putValue(new ArrayList<Object>());
		fluentApi.putKey(new SmartClass(ArrayList.class)).putValue(new ArrayList<Object>());
		fluentApi.putKey(new SmartClass(LinkedList.class)).putValue(new LinkedList<Object>());
		fluentApi.putKey(new SmartClass(Set.class)).putValue(new HashSet<Object>());
		fluentApi.putKey(new SmartClass(HashSet.class)).putValue(new HashSet<Object>());
		fluentApi.putKey(new SmartClass(LinkedHashSet.class)).putValue(new LinkedHashSet<Object>());
	}

	private static void string(FluentApi fluentApi, int counter) {
		String word = "abc";
		StringBuffer b = new StringBuffer();
	    char[] chars = word.toCharArray();
	    for (char c : chars) {
	        if(c != ' ')
	            c = (char) (c + counter);
	        b.append(c);
	    }
		fluentApi.putKey(new SmartClass(String.class)).putValue(b.toString());
	}

	private static Calendar getCalendarDate0() {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(new Date(0));
		return calendar;
	}

}
