package me.onebone.rino;

/**
 * The MIT License (MIT)
 * Copyright (c) 2016 onebone <jyc00410@gmail.com>
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

import java.util.HashMap;
import java.util.Map;

public class CommandParser{
	private boolean raw = false;
	private String[] args = null;
	private Options options = null;
	private int startFrom = 0;

	/**
	 * Constructor of CommandParser
	 *
	 * @param args	Arguments of command
	 * @param raw 	Whether command is raw. If it is true, if will concatenate arguments which is surrounded by quotes.
	 */
	public CommandParser(String[] args, Options options, boolean raw, int startFrom){
		if(args == null){
			throw new NullPointerException("Arguments cannot be null");
		}

		this.options = options;
		this.args = args;
		this.raw = raw;

		this.cur = startFrom;
	}

	public CommandParser(String[] args, Options options){
		this(args, options, false, 0);
	}

	public CommandParser(String[] args, Options options, int startFrom){
		this(args, options, false, startFrom);
	}

	public CommandParser(String[] args, boolean raw){
		this(args, null, raw, 0);
	}

	public CommandParser(String[] args){
		this(args, null, false, 0);
	}

	private int cur = 0;

	public boolean hasNext(){
		return this.args.length > cur && (this.options != null || cur == this.startFrom);
	}

	private String getNext(){
		if(this.hasNext() && raw){
			StringBuilder next = new StringBuilder();

			boolean open = false;
			do{
				String arg = this.args[cur++];

				if(!open && arg.startsWith("\"")){
					if(!arg.endsWith("\"")) open = true;

					next.append(arg.substring(1, arg.endsWith("\"") ? arg.length() - 1 : arg.length()));
				}else if(open && arg.endsWith("\"")){
					open = false;
					next.append(arg.substring(0, arg.length() - 1));
				}else{
					next.append(arg);
				}

				next.append(" ");
			}while(open && this.hasNext());

			return !open && next.length() > 0 ? next.substring(0, next.length() - 1) : null;
		}

		return this.hasNext() ? this.args[cur++] : null;
	}

	public Value getOpt(){ // TODO Check for required arguments
		if(!this.hasNext()) return null;

		String arg = this.getNext();

		if(arg == null) return null;

		if(arg.length() >= 1 && arg.startsWith("-")){
			if(arg.length() >= 2 && arg.startsWith("--")){
				Option opt;
				if((opt = this.options.getLongOpt(arg.substring(2))) != null){
					if(opt.hasArgument()){
						if(this.args.length > cur){
							return new Value(opt.getOpt(), this.getNext()); // --option ARG
						}else{
							throw new NoArgumentException(opt.getOpt());
						}
					}else return new Value(opt.getOpt(), null); // --option
				}

				return null;
			}

			Option opt;
			if((opt = this.options.getOpt(String.valueOf(arg.charAt(1)))) != null){
				if(opt.hasArgument()){
					if(arg.length() > 2){
						return new Value(opt.getOpt(), arg.substring(2, arg.length())); // -oARG
					}else if(this.args.length > cur){
						return new Value(opt.getOpt(), this.getNext()); // -o ARG
					}else{
						throw new NoArgumentException(opt.getOpt());
					}
				}else return new Value(opt.getOpt(), null); // -o
			}
		}

		return new Value("", arg);
	}

	public static class Value{
		private String opt, value;

		Value(String opt, String value){
			this.opt = opt;
			this.value = value;
		}

		public String getOpt(){
			return this.opt;
		}

		public String getValue(){
			return this.value;
		}

		public String toString(){
			return "Value (opt: " + opt + ", value: " + value + ")";
		}
	}

	public static class Options{
		private Map<String, Option> options = new HashMap<>();

		public Options(){}

		public boolean addOption(Option opt){
			if(options.containsKey(opt.getOpt())) return false;

			this.options.put(opt.getOpt(), opt);
			return true;
		}

		public boolean removeOption(Option opt){
			if(options.containsValue(opt)){
				options.remove(opt);
				return true;
			}
			return false;
		}

		public Option getOpt(String opt){
			return this.options.get(opt);
		}

		public Option getLongOpt(String longOpt){
			for(Option option : options.values()){
				if(longOpt == option.getLongOpt() || longOpt.equals(option.getLongOpt())) return option;
			}

			return null;
		}
	}

	public static class Option{
		private String opt, longOpt;
		private boolean hasArg, required;

		public Option(String opt, String longOpt, boolean hasArg, boolean required){
			this.opt = opt;
			this.longOpt = longOpt;
			this.hasArg = hasArg;
			this.required = required;
		}

		public Option(String opt, String longOpt, boolean hasArg){
			this(opt, longOpt, hasArg, false);
		}

		public Option(String opt, boolean hasArg){
			this(opt, null, hasArg, false);
		}

		public Option(String opt){
			this(opt, null, false, false);
		}

		public String getOpt(){
			return this.opt;
		}

		public String getLongOpt(){
			return this.longOpt;
		}

		public boolean hasArgument(){
			return this.hasArg;
		}

		public boolean isRequired(){
			return this.required;
		}
	}

	public static class NoArgumentException extends RuntimeException{
		private String opt;

		public NoArgumentException(String opt){
			this.opt = opt;
		}

		public String getOpt(){
			return this.opt;
		}
	}
}