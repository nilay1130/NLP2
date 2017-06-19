import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.mysql.jdbc.exceptions.jdbc4.MySQLSyntaxErrorException;

public class MYSQLDBCONN
{
	Connection conn = null;
	
	public void Connect()
	{
        try
        {
        	Class.forName("com.mysql.jdbc.Driver").newInstance();
            conn = DriverManager.getConnection("jdbc:mysql://127.0.0.1:3306/nlp_project_2?" +
                                                "user=root&password=12345");
        }
        catch (SQLException ex)
        {
            System.out.println("SQLException: " + ex.getMessage());
            System.out.println("SQLState: " + ex.getSQLState());
            System.out.println("VendorError: " + ex.getErrorCode());
        }
        catch(Exception ex){ex.printStackTrace();}
    }
	
	public HashMap<String, List<String>> preProcess(String type)
	{
		HashMap<String, List<String>> hm = new HashMap<>();
		List<String> list;
		
		switch(type)
		{
			case "sql_command":
				list = new ArrayList<>();
				
				list.add("List");
				list.add("Show");
				list.add("Give");
				list.add("Write");
				list.add("Select");
				list.add("Choose");
				
				hm.put("SELECT", list);
				
				break;
				
			case "tables":
				DatabaseMetaData mdt;
				
				try
				{
					mdt = conn.getMetaData();
					ResultSet rs = mdt.getTables(null, null, "%", null);
					
					while (rs.next())
					{
						WordNet wn = new WordNet();
						list = new ArrayList<>();
						
						list.addAll(wn.getSynonym(rs.getString(3)));
						hm.put(rs.getString(3), list);
					}
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
				
				break;
				
			case "columns":
				DatabaseMetaData mdc;
				
				try
				{
					mdc = conn.getMetaData();
					ResultSet rs = mdc.getColumns(null, null, "%", "%");
					
					while (rs.next())
					{	
						WordNet wn = new WordNet();
						list = new ArrayList<>();
						
						if(!hm.containsKey(rs.getString(4)))
							list.addAll(wn.getSynonym(rs.getString(4)));
						
						String newName = rs.getString(4).replace(rs.getString(3), "");
						
						if(!rs.getString(4).equalsIgnoreCase(newName))
							list.addAll(wn.getSynonym(newName));
						
						if(list.size() > 0)
							hm.put(rs.getString(4), list);
					}
				}
				catch (SQLException e)
				{
					e.printStackTrace();
				}
				
				break;
		}
		
		return hm;
	}
	
	public boolean CTCR(String column, String table)
	{
		DatabaseMetaData mdc;
		
		try
		{
			mdc = conn.getMetaData();
			ResultSet rs = mdc.getColumns(null, null, "%", "%");
			
			while (rs.next())
			{	
				if(rs.getString(4).equalsIgnoreCase(column)
						&& rs.getString(3).equalsIgnoreCase(table))
					return true;
			}
		}
		catch (SQLException e){e.printStackTrace();}
		
		return false;
	}

	public ArrayList<String> getTables(List<String> clmns)
	{
		ArrayList<String> tbls = new ArrayList<>();
		DatabaseMetaData mdc;
		
		try
		{
			mdc = conn.getMetaData();
			
			for(String clmn: clmns)
			{
				ResultSet rs = mdc.getColumns(null, null, "%", "%");

				while (rs.next())
				{	
					if(rs.getString(4).equalsIgnoreCase(clmn))
						tbls.add(rs.getString(3));
				}
			}
		}
		catch (SQLException e){e.printStackTrace();}
		
		return tbls;
	}

	public String AttbViaTable(String t, String a)
	{
		DatabaseMetaData mdc;
		
		try
		{
			mdc = conn.getMetaData();
			ResultSet rs = mdc.getColumns(null, null, "%", "%");
			
			while (rs.next())
			{	
				if(rs.getString(3).equalsIgnoreCase(t))
				{
					String query = "SELECT COUNT(*) FROM " + rs.getString(3)
					+ " WHERE " + rs.getString(4) + " = \"" + a + "\";";
					
					Statement st = conn.createStatement();
					ResultSet rs2 = st.executeQuery(query);
					
					while (rs2.next())
					{
				        if(rs2.getInt(1) > 0)
				        	return rs.getString(4);
					}
				}
			}
		}
		catch (SQLException e){e.printStackTrace();}
		
		return "";
	}

	public String AttbViaColumn(String c, String a)
	{
		DatabaseMetaData mdc;
		
		try
		{
			mdc = conn.getMetaData();
			ResultSet rs = mdc.getColumns(null, null, "%", "%");
			
			while (rs.next())
			{	
				if(rs.getString(4).equalsIgnoreCase(c))
				{
					String query = "SELECT COUNT(*) FROM " + rs.getString(3)
					+ " WHERE " + rs.getString(4) + " = \"" + a + "\";";
					
					Statement st = conn.createStatement();
					ResultSet rs2 = st.executeQuery(query);
					
					while (rs2.next())
					{
				        if(rs2.getInt(1) > 0)
				        	return rs.getString(3);
					}
				}
			}
		}
		catch (SQLException e){e.printStackTrace();}
		
		return "";
	}

	public String Attb(String a)
	{
		DatabaseMetaData mdc;
		
		try
		{
			mdc = conn.getMetaData();
			ResultSet rs = mdc.getColumns(null, null, "%", "%");
			
			while (rs.next())
			{	
				String query = "SELECT COUNT(*) FROM " + rs.getString(3)
					+ " WHERE " + rs.getString(4) + " = \"" + a + "\";";
					
				Statement st = conn.createStatement();
				ResultSet rs2 = st.executeQuery(query);
					
				while (rs2.next())
				{
				    if(rs2.getInt(1) > 0)
				        return rs.getString(3) + "-" + rs.getString(4);
				}
			}
		}
		catch (SQLException e){e.printStackTrace();}
		
		return "";
	}

	public String getResult(String query)
	{
		String result = "";
		
		try
		{
			Statement st = conn.createStatement();
			ResultSet rs = st.executeQuery(query);
			ResultSetMetaData rsmd = rs.getMetaData();
			
			int columnsNumber = rsmd.getColumnCount();
			
			for(int i = 1; i <= columnsNumber; i++)
			{
		        if (i > 1)
		        	result += "\t| ";
		        
		        result += rsmd.getColumnName(i);
		    }
			
			result += "\n";
			
			while (rs.next())
			{
				for(int i = 1; i <= columnsNumber; i++)
				{
			        if (i > 1)
			        	result += "\t| ";
			        
			        String columnValue = rs.getString(i);
			        result += columnValue;
			    }
				
				result += "\n";
			}
		}
		catch (MySQLSyntaxErrorException e)
		{
			result = "Çözümlenemeyen input.";
		}
		catch (SQLException e) {e.printStackTrace();}
		
		return result;
	}
}
