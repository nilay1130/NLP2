import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;
import java.awt.Font;
import javax.swing.JTextPane;
import javax.swing.JButton;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class Main {

	private JFrame frame;

	public static void main(String[] args)
	{
		EventQueue.invokeLater(new Runnable()
		{
			public void run()
			{
				try 
				{
					Main window = new Main();
					window.frame.setVisible(true);
				}
				catch (Exception e){e.printStackTrace();}
			}
		});

		//String text = "List the name, surname, id and course code from scholar who take course from Özkan Kýlýç.";
		//String text = "List the name, surname and course code from scholar and course who has got id 1305012010 and take course CENG304.";
		//String text = "List the id from scholar who take course CENG304.";
		//String text = "List the id from scholar who take course Computer Networks.";
	    //String text = "List the students who takes course from Özkan Kýlýç.";
		//String text = "List the course codes.";
		
		
		//String text = "List the scholars.";
		//String text = "List the name of course.";
		//String text = "List the code of course.";
		
		
	}

	public Main()
	{
		initialize();
	}

	private void initialize()
	{
		frame = new JFrame();
		frame.setBounds(100, 100, 959, 670);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(null);
		
		JLabel lblCommand = new JLabel("Enter Your Command: ");
		lblCommand.setFont(new Font("Tahoma", Font.PLAIN, 16));
		lblCommand.setBounds(12, 13, 165, 34);
		frame.getContentPane().add(lblCommand);
		
		final JTextPane command = new JTextPane();
		command.setBounds(12, 56, 917, 54);
		frame.getContentPane().add(command);
		
		final JTextPane sql = new JTextPane();
		sql.setBounds(12, 166, 917, 54);
		frame.getContentPane().add(sql);
		
		JLabel lblSqlQuery = new JLabel("SQL Query: ");
		lblSqlQuery.setFont(new Font("Tahoma", Font.PLAIN, 16));
		lblSqlQuery.setBounds(12, 123, 88, 34);
		frame.getContentPane().add(lblSqlQuery);
		
		final JTextPane result = new JTextPane();
		result.setBounds(12, 276, 917, 287);
		frame.getContentPane().add(result);
		
		JLabel lblResult = new JLabel("Result: ");
		lblResult.setFont(new Font("Tahoma", Font.PLAIN, 16));
		lblResult.setBounds(12, 233, 165, 34);
		frame.getContentPane().add(lblResult);
		
		JButton btnRun = new JButton("RUN");
		btnRun.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseClicked(MouseEvent arg0)
			{
				sql.setText(" ");
				String text = command.getText();
				
				TextToSQL ttsql = new TextToSQL();
				String NEW_QUERY = ttsql.convertToSQL(text);
				
				sql.setText(NEW_QUERY);
				
				result.setText(getResult(NEW_QUERY));
			}
		});
		
		btnRun.setBounds(833, 576, 96, 34);
		frame.getContentPane().add(btnRun);
	}
	
	private String getResult(String q)
	{
		MYSQLDBCONN conn = new MYSQLDBCONN();
		conn.Connect();
		String result = conn.getResult(q);
		
		return result;
	}
}
