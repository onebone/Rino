package me.onebone.rino;

public class Request{
	public static final int TYPE_MESSAGE    = 0;
	public static final int TYPE_NAME    = 1;
	public static final int TYPE_ITEM       = 2;

	private int type;
	private Object val;

	public Request(int type, Object val){
		this.type = type;
		this.val = val;
	}

	public int getType(){
		return this.type;
	}

	public Object getValue(){
		return this.val;
	}

	public void setValue(Object val){
		this.val = val;
	}
}
