import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import edu.stanford.nlp.trees.GrammaticalStructure;
import edu.stanford.nlp.trees.TypedDependency;

public class TextToSQL
{
	private HashMap<String, List<String>> sql_command;
	private HashMap<String, List<String>> tables;
	private HashMap<String, List<String>> columns;
	
	private List<TypedDependency> dependencyParseTree;
	
	private List<String> COLUMNS;
	private List<String> TABLES;
	private List<String> CONDITION_COLUMNS;
	private List<String> CONDITION_TABLES;
	
	private boolean condition_existence = false;
	
	private String text;
	
	private MYSQLDBCONN conn;
	
	public TextToSQL()
	{
		conn = new MYSQLDBCONN();
		conn.Connect();
		
		sql_command = conn.preProcess("sql_command");
		tables = conn.preProcess("tables");
		columns = conn.preProcess("columns");
	}
	
	public String convertToSQL(String text)
	{
		this.text = text;
		
		DependencyParserDemo dpd = new DependencyParserDemo();
		GrammaticalStructure gs = dpd.DP(text);
		
		dependencyParseTree = gs.typedDependenciesEnhanced();
		String root = "";
		
		for(TypedDependency td: dependencyParseTree)
		{
			if(td.reln().toString().equalsIgnoreCase("root"))
				root = td.dep().value();
		}
		
		String SELECT_STATEMENT = "";
		String COLUMN_STATEMENT = "";
		String FROM_STATEMENT = "FROM ";
		String TABLE_STATEMENT = "";
		String CONDITION_STATEMENT = "";
		
		COLUMNS = new ArrayList<String>();
		TABLES = new ArrayList<String>();
		CONDITION_COLUMNS = new ArrayList<String>();
		CONDITION_TABLES = new ArrayList<String>();
		
		List<String> select_st_string = sql_command.get("SELECT");
		
		for(String st: select_st_string)
		{
			if(st.equalsIgnoreCase(root))
			{
				SELECT_STATEMENT = "SELECT ";
				LFColumns(root);
			}
		}
		
		for(TypedDependency td: dependencyParseTree)
		{
			String relation = td.reln().toString();
			
			switch(relation)
			{
				case "acl:relcl":
					condition_existence = true;
					
					break;
					
				case "acl":
					condition_existence = true;
					
					break;
					
				case "nmod:poss":
					condition_existence = true;
					
					break;
			}
		}
		
		if(condition_existence && CONDITION_COLUMNS.size() == 0)
		{
			LookCondition();
		}
		
		TABLES.addAll(CONDITION_TABLES);
		
		Clean();
		CheckTCRelation();
		
		if(TABLES.size() > 1)
		{
			RearrangeAll();
		}
		
		for(int i = 0; i < TABLES.size(); i++)
		{
			if(i != TABLES.size()-1)
				TABLE_STATEMENT += TABLES.get(i) + ", ";
			else
				TABLE_STATEMENT += TABLES.get(i) + " ";
		}
		
		if(COLUMNS.size() == 0)
			COLUMN_STATEMENT = "* ";
		else
		{
			for(int i = 0; i < COLUMNS.size(); i++)
			{
				if(i != COLUMNS.size()-1)
					COLUMN_STATEMENT += COLUMNS.get(i) + ", ";
				else
					COLUMN_STATEMENT += COLUMNS.get(i) + " ";
			}
		}
		
		if(CONDITION_COLUMNS.size() != 0)
		{
			CONDITION_STATEMENT = "WHERE ";
			
			for(int i = 0; i < CONDITION_COLUMNS.size(); i++)
			{
				String[] statement = CONDITION_COLUMNS.get(i).split("&");
				String s;
				
				if(!isInteger(statement[1]))
					s = statement[0] + " = \"" + statement[1] + "\"";
				else
					s = statement[0] + " = " + statement[1];
						
				if(i != CONDITION_COLUMNS.size()-1)
					CONDITION_STATEMENT += s + " AND ";
				else
					CONDITION_STATEMENT += s;
			}
		}
		
		String NEW_QUERY = SELECT_STATEMENT + COLUMN_STATEMENT + FROM_STATEMENT + TABLE_STATEMENT + CONDITION_STATEMENT;
		
		return NEW_QUERY;
	}

	private void LFColumns(String word)
	{
		ArrayList<String> tempColumns = new ArrayList<String>();
		
		for(TypedDependency td: dependencyParseTree)
		{
			String relation = td.reln().toString();
			
			switch(relation)
			{
				case "dobj":
					if(td.gov().value().equalsIgnoreCase(word))
					{
						String controlValue = LFCompound(td.dep().value());
						
						for(Entry<String, List<String>> entry: columns.entrySet())
						{
						    for(String e: entry.getValue())
						    {
							    if(controlValue.equalsIgnoreCase(e))
							   	{
								   	COLUMNS.add(entry.getKey());
								   	tempColumns.add(e);
								   	break;
							    }
						    }
						}
					}
					
					break;
			}
		}
		
		ArrayList<String> tempTable = new ArrayList<String>();

		if(COLUMNS.size() == 0)
			tempTable = LFTable(word);
		else
		{
			for(String w: tempColumns)
				tempTable.addAll(LFTable(w));
		}

		if(TABLES.size() == 0)
		{
			TABLES.addAll(conn.getTables(COLUMNS));
			for(String w: tempColumns)
			{
				String[] w_part = w.split(" ");
				for(String wp: w_part)
					LFCondition(wp);
			}
		}
		else
		{
			for(String w: tempTable)
				LFCondition(w);
		}
	}
	
	private ArrayList<String> LFTable(String word)
	{
		ArrayList<String> tempTable = new ArrayList<String>();
		
		for(TypedDependency td: dependencyParseTree)
		{
			String relation = td.reln().toString();
			
			switch(relation)
			{
				case "dobj":
					if(td.gov().value().equalsIgnoreCase(word))
					{
						for(Entry<String, List<String>> entry: tables.entrySet())
						{
						    for(String e: entry.getValue())
						    {
							    if(td.dep().value().equalsIgnoreCase(e))
							   	{
								   	TABLES.add(entry.getKey());
								   	tempTable.add(e);
								   	break;
							    }
						    }
						}
					}
					
					break;
					
				case "nmod:of":
					if(td.gov().value().equalsIgnoreCase(word))
					{
						for(Entry<String, List<String>> entry: tables.entrySet())
						{
						    for(String e: entry.getValue())
						    {
							    if(td.dep().value().equalsIgnoreCase(e))
							   	{
								   	TABLES.add(entry.getKey());
								   	tempTable.add(e);
								   	break;
							    }
						    }
						}
					}
					
					break;
					
				case "nmod:from":
					if(td.gov().value().equalsIgnoreCase(word))
					{
						for(Entry<String, List<String>> entry: tables.entrySet())
						{
						    for(String e: entry.getValue())
						    {
							    if(td.dep().value().equalsIgnoreCase(e))
							   	{
								   	TABLES.add(entry.getKey());
								   	tempTable.add(e);
								   	break;
							    }
						    }
						}
					}
					
					break;
			}
		}
		
		return tempTable;
	}

	private String LFCompound(String word)
	{
		String np = LFAdjective(word) + " ";
		
		for(TypedDependency td: dependencyParseTree)
		{
			String relation = td.reln().toString();
			
			switch(relation)
			{
				case "compound":
					if(td.gov().value().equalsIgnoreCase(word))
						np += td.dep().value() + " ";
					
					break;
			}
		}
		
		np += word;
		
		return np.trim();
	}

	private String LFAdjective(String word)
	{
		String np = "";
		
		for(TypedDependency td: dependencyParseTree)
		{
			String relation = td.reln().toString();
			
			switch(relation)
			{
				case "amod":
					if(td.gov().value().equalsIgnoreCase(word))
						np += td.dep().value() + " ";
					
					break;
			}
		}
		
		return np.trim();
	}
	
	private void LFCondition(String word)
	{
		ArrayList<String> keyWords = new ArrayList<String>();
		ArrayList<String> keyWordsSet = new ArrayList<String>();

		for(TypedDependency td: dependencyParseTree)
		{
			String relation = td.reln().toString();

			switch(relation)
			{
				case "acl:relcl":
					if(td.gov().value().equalsIgnoreCase(word))
						LFCondition(td.dep().value());
					
					break;
				
				case "dobj":
					if(td.gov().value().equalsIgnoreCase(word))
					{	
						if(td.gov().value().equalsIgnoreCase(word))
							keyWords.add(LFCompound(td.dep().value()));
					}
					
					break;
				
				case "nmod":
					if(td.gov().value().equalsIgnoreCase(word))
						keyWords.add(LFCompound(td.dep().value()));
					
					break;
			}
		}
		
		for(String words: keyWords)
		{
			String w[] = words.split(" ");
			String pa = " ";
			String table = " ";
			String column = " ";
			
			boolean[] isAdded = new boolean[w.length];
			
			for(int i = 0; i < w.length; i++)
			{
				if(!isAdded[i])
				{
					for(Entry<String, List<String>> entry: tables.entrySet())
					{
					    for(String e: entry.getValue())
					    {
						    if(w[i].equalsIgnoreCase(e))
						   	{
						    	table = entry.getKey();
							   	isAdded[i] = true;
							   	break;
						    }
					    }
					}
				}
				
				if(!isAdded[i])
				{
					for(Entry<String, List<String>> entry: columns.entrySet())
					{
					    for(String e: entry.getValue())
					    {
						    if(w[i].equalsIgnoreCase(e))
						   	{
						    	column = entry.getKey();
							   	isAdded[i] = true;
							   	break;
						    }
					    }
					}
				}
				
				if(!isAdded[i])
					pa += w[i] + " ";
			}
			
			keyWordsSet.add(table + "-" + column + "-" + pa);
		}
		
		for(String set: keyWordsSet)
		{
			if(!set.split("-")[0].equals(" "))
			{
				String t = set.split("-")[0];
				String a = set.split("-")[2].trim();
				
				String c = conn.AttbViaTable(t, a);
				
				if(!c.equalsIgnoreCase(""))
				{
					CONDITION_TABLES.add(t);
					CONDITION_COLUMNS.add(c + "&" + a);
				}
			}
			else if(!set.split("-")[1].equals(" "))
			{
				String c = set.split("-")[1];
				String a = set.split("-")[2].trim();
				
				boolean bool = true;
				
				for(String t: TABLES)
				{
					if(conn.CTCR(c, t))
					{
						CONDITION_TABLES.add(t);
						CONDITION_COLUMNS.add(c + "&" + a);
						bool = false;

						break;
					}
				}
				
				if(bool)
				{
					String t = conn.AttbViaColumn(c, a);
					
					if(!t.equalsIgnoreCase(""))
					{
						CONDITION_TABLES.add(t);
						CONDITION_COLUMNS.add(c + "&" + a);
					}
				}
			}
			else
			{
				String a = set.split("-")[2].trim();
				String t_c = conn.Attb(a);
					
				if(!t_c.equalsIgnoreCase("-"))
				{
					String t = t_c.split("-")[0];
					String c = t_c.split("-")[1];
					CONDITION_TABLES.add(t);
					CONDITION_COLUMNS.add(c + "&" + a);
				}
			}
		}
	}

	private void LookCondition()
	{
		for(TypedDependency td: dependencyParseTree)
		{
			String relation = td.reln().toString();
			
			if(relation.equalsIgnoreCase("acl:relcl") || relation.equalsIgnoreCase("acl")
					|| relation.equalsIgnoreCase("nmod:poss"))
			{
				int loc = Integer.parseInt(td.toString().split(",")[0].split("-")[1]);
				
				String[] ntext = text.split(" ");
				String[] condition_text = new String[ntext.length - loc];
				int counter = 0;
					
				for(int i = 0; i < ntext.length; i++)
				{
					if(i >= loc)
					{
						condition_text[counter] = ntext[i];
						counter++;
					}
				}
				
				int index = 0;
				String column = " ";

				while(index < condition_text.length)
				{
					String w = "";
					
					for(int i = index; i < condition_text.length; i++)
					{
						w += condition_text[i] + " ";
							
						for(Entry<String, List<String>> entry: columns.entrySet())
						{
						    for(String e: entry.getValue())
						    {
							    if(w.trim().equalsIgnoreCase(e))
							   	{
							    	column = entry.getKey();
								   	break;
							    }
						    }
						}
					}
					
					index++;
				}

				if(!column.equals(" "))
				{
					index = 0;
					
					while(index < condition_text.length)
					{
						String a = "";
						
						for(int i = index; i < condition_text.length; i++)
						{
							a += condition_text[i] + " ";
							
							String t = conn.AttbViaColumn(column, a.trim());
							
							if(!t.equalsIgnoreCase(""))
							{
								CONDITION_TABLES.add(t);
								CONDITION_COLUMNS.add(column + "&" + a.trim());
							}
						}
						
						index++;
					}
				}
				else
				{
					index = 0;
					
					while(index < condition_text.length)
					{
						String w = "";
						
						for(int i = index; i < condition_text.length; i++)
						{
							w += condition_text[i] + " ";
							String t_c = conn.Attb(w.trim());
								
							if(!t_c.equalsIgnoreCase("-") && !t_c.equalsIgnoreCase(""))
							{
								String t = t_c.split("-")[0];
								String c = t_c.split("-")[1];
								CONDITION_TABLES.add(t);
								CONDITION_COLUMNS.add(c + "&" + w.trim());
							}
						}
						
						index++;
					}
				}
			}	
		}
	}
	
	private void CheckTCRelation()
	{
		boolean[] isExists = new boolean[COLUMNS.size()];
		
		for(String column: COLUMNS)
		{
			for(String table: TABLES)
			{
				if(conn.CTCR(column, table))
					isExists[COLUMNS.indexOf(column)] = true;
			}
		}
		
		for(int i = isExists.length - 1; i >= 0; i--)
		{
			if(!isExists[i])
				COLUMNS.remove(i);
		}
	}

	private void Clean()
	{
		HashSet<String> hs = new HashSet<>();
		
		hs.addAll(COLUMNS);
		COLUMNS.clear();
		COLUMNS.addAll(hs);
		hs.clear();
		
		hs.addAll(TABLES);
		TABLES.clear();
		TABLES.addAll(hs);
		hs.clear();
		
		hs.addAll(CONDITION_COLUMNS);
		CONDITION_COLUMNS.clear();
		CONDITION_COLUMNS.addAll(hs);
		hs.clear();
	}

	private void RearrangeAll()
	{
		ArrayList<String> tempTable = new ArrayList<>();
		ArrayList<String> tempColumn = new ArrayList<>();
		ArrayList<String> tempConditionColumn = new ArrayList<>();
		
		for(String table: TABLES)
			tempTable.add(table + " " + table.substring(0, 4));
		
		for(String column: COLUMNS)
		{
			for(String table: TABLES)
			{
				if(conn.CTCR(column, table))
					tempColumn.add(table.substring(0, 4) + "." + column);
			}
		}
		
		for(String column: CONDITION_COLUMNS)
		{
			for(String table: TABLES)
			{
				if(conn.CTCR(column.split("&")[0], table))
					tempConditionColumn.add(table.substring(0, 4) + "." + column);
			}
		}
		
		TABLES.clear();
		TABLES.addAll(tempTable);
		COLUMNS.clear();
		COLUMNS.addAll(tempColumn);
		CONDITION_COLUMNS.clear();
		CONDITION_COLUMNS.addAll(tempConditionColumn);
	}
	
	private boolean isInteger(String str)
	{
	    if (str == null)
	        return false;
	    
	    int length = str.length();
	    
	    if (length == 0)
	        return false;
	    
	    int i = 0;
	    
	    if (str.charAt(0) == '-')
	    {
	        if (length == 1)
	            return false;
	        
	        i = 1;
	    }
	    
	    for (; i < length; i++)
	    {
	        char c = str.charAt(i);
	        
	        if (c < '0' || c > '9')
	            return false;

	    }
	    
	    return true;
	}
}

























