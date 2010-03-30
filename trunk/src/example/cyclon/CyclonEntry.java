package example.cyclon;

import java.util.Comparator;

import peersim.core.Node;

public class CyclonEntry implements Comparable<CyclonEntry>, Comparator<CyclonEntry>
{
	public Node n;
	public long age;
	public boolean removed;
	public long timeRemoved;
	public Node nodeSended;

	public CyclonEntry(){}

	public CyclonEntry(Node n, long age)
	{
		this.n = n;
		this.age = age;
		this.removed = false;
		this.timeRemoved = Long.MAX_VALUE;
		this.nodeSended = null;
	}

	public int compareTo(CyclonEntry ce)
	{
		if (age < ce.age)
			return 1;
		else if (ce.age == age)
			return 0;
		return -1;
	}

	public int compare(CyclonEntry ce1, CyclonEntry ce2){
		if (ce1.age < ce2.age)
			return 1;
		else if (ce1.age == ce2.age)
			return 0;
		return -1;

	}

	public void increase()
	{
		this.age++;
	}
}
