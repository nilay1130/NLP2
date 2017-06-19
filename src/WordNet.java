import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;

import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.WordNetDatabase;

public class WordNet
{
	public ArrayList<String> getSynonym(String wordForm)
	{
		File file = new File("C:\\Program Files (x86)\\WordNet\\2.1\\dict");
		System.setProperty("wordnet.database.dir", file.toString());

		WordNetDatabase database = WordNetDatabase.getFileInstance();
		
		String[] parts = wordForm.split("_");
		ArrayList<ArrayList<String>> synm_list = new ArrayList<ArrayList<String>>(parts.length);
		
		for(String p: parts)
		{
			Synset[] synsets = database.getSynsets(p);
			ArrayList<String> al = new ArrayList<>();
			
			if (synsets.length > 0)
			{
				HashSet<String> hs = new HashSet<>();
				
				for (int i = 0; i < synsets.length; i++)
				{
					String[] wordForms = synsets[i].getWordForms();
					
					for (int j = 0; j < wordForms.length; j++)
					{
						al.add(wordForms[j]);
					}
				}

				hs.addAll(al);
				al.clear();
				al.addAll(hs);
				synm_list.add(al);
			}
		}
		
		int size = 1;
		
		for(int i = 0; i < synm_list.size(); i++)
		{
			size *= synm_list.get(i).size();
		}
		
		ArrayList<String> synonims = new ArrayList<>();
		
		String[] array = new String[size];
		
		for(int i = 0; i < array.length; i++)
			array[i] = "";
		
		int temp_size = size;
		
		for(ArrayList<String> s_list: synm_list)
		{
			temp_size = temp_size / s_list.size();
			
			for(int i = 0; i < size;)
			{
				for(String s: s_list)
				{
					for(int j = 0; j < temp_size; j++)
					{
						array[i] += " " + s;
						i++;
					}
				}
			}
		}
		
		for(String s: array)
			synonims.add(s.trim());
		
		return synonims;
	}
}
