//This program exists to generate valid LaTeX from one (or many) decklist files
//The output wont always work as intended (very big decks), but for the most part it should be fine
package builder;

import java.util.*;
import java.util.stream.*;
import java.nio.file.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;
import java.net.*;
import org.apache.commons.io.IOUtils;
import builder.*;

public class Decklists {
    private static TreeMap<String, Faction> factions = new TreeMap<String, Faction>();
    private static TreeMap<String, String> cards = new TreeMap<String, String>();
    private static TreeMap<String, String> cardCodes = new TreeMap<String, String>();
    public static TreeMap<String, String> cardFaction = new TreeMap<String, String>();
    public static TreeMap<String, Integer> cardInfluence = new TreeMap<String, Integer>();
    
    private static String decklistPrefix = "latexGeneratedDecklist";
    private static String decknamePrefix = "latexGeneratedDeckname";
    private static StringBuilder flush = new StringBuilder();
    private static Integer deckNum = 1;

    private static boolean useMWL = false;
    private static String MWLVersion = null;
    private static boolean regeneratedLibrary = false;
    private static MWL chosenMWL = null;

    private static boolean processFactionInfluence = false;
    
    public static void main(String[] args) {
	//each argument is expected to be a decklist file
	//the name of the file will be considered the name of the deck

	for(String s : args) {
	    //if we are told to use an mwl, then do it
	    if(s.equals("-mwl")) {
		//TODO
		useMWL = true;
	    }
	    //if we are told to redownload the decklist files, then do so
	    //we can also re-download the mwl files
	    else if (s.equals("-r")) {
		System.err.println("%Downloading standard cardpool (-r option used)");
		download_library();
	    }

	    else if (s.equals("-f")) {
		System.err.println("%Processing faction influence for these cards");
		processFactionInfluence = true;
		//we should be able to regenerate the factions file from the api, if need be
	    }
	}
	
	process_preamble();

	process_netrunner_library();

	if(useMWL) {
	    boolean orgl = regeneratedLibrary;
	    regeneratedLibrary = false;
	    process_mwl_version();
	    System.err.println("%Using mwl version: " + MWLVersion);
	    
	    process_netrunner_mwl();
	    regeneratedLibrary = orgl;
	}

	if (processFactionInfluence) {
	    process_faction_list();
	}
	    
	
	for(String s : args) {
	    if(s.equals("-mwl") || s.equals("-r") || s.equals("-f"))
		continue;
	    process_decklist(s);
	    deckNum++;
	}

	System.out.println(flush.toString());

	System.out.println("\\end{document}");
    }

    private static void process_faction_list() {
	String fname = "conf/NRDB_factions.json";
	String factionFile = read_factions_list(fname);

	boolean valid = factionFile != null;
	System.out.println("%Faction List Read: " + valid);

	Gson gson = new Gson();
	FactionList fl = gson.fromJson(factionFile, FactionList.class);

	for(Faction f : fl.data) {
	    factions.put(f.code, f);
	}

	System.err.println("%Number of factions known: " + factions.size());
    }
    
    private static String read_factions_list(String fname /*NRDB_factions.json*/){
	StringBuilder builder = new StringBuilder();
	try (Stream<String> stream = Files.lines(Paths.get(fname), StandardCharsets.UTF_8)) {
	    stream.forEach(s -> builder.append(s).append("\n"));
	} catch (IOException e) {
	    //the file doesn't exist: have we tried fetching it yet this execution?
	    if(!regeneratedLibrary) {
		System.err.println("% couldn't open factions file. Checking online...");
		if(download_factions())
		    return read_factions_list(fname);
		else
		    System.err.println("% Could not read factions list");
	    }
	    
	    //e.printStackTrace();
	    return null;
	}
	
	return builder.toString();
    }
    
    private static void process_mwl_version() {
	String fname = "conf/mwl.conf";
	List<String> mwlFile = read_mwl_version(fname);

	while(mwlFile.size() > 0) {
	    String line = mwlFile.remove(0);
	    if(line.length() > 1 && line.charAt(0) != '#') {
		MWLVersion = line;
		break;
	    }
	}
    }

    private static List<String> read_mwl_version(String fname /*mwl.conf*/) {
	List<String> res;
	try (Stream<String> lines = Files.lines(Paths.get(fname))) {
	    res = lines.collect(Collectors.toList());
	} catch (Exception e) {
	    res = new ArrayList<String>();
	    res.add("Standard MWL 3.4b");
	}

	return res;
    }
    
    private static void process_netrunner_library() {
	//for the time being, we'll just use a file saved on disk
	//later on, we should be taking it directly from the API (and saving it to a file, then always using that file)
	//but we can do that later :)
	String fname = "conf/NRDB_cards.json";

	//now let us try to process this file to a string
	String libraryJSON = read_netrunner_library(fname);

	boolean valid = libraryJSON != null;
	System.out.println("%Library read: " + valid);

	Gson gson = new Gson();
	Library library = gson.fromJson(libraryJSON, Library.class);

	for(Card c : library.data) {
	    //put in cards (name, type)
	    cards.put(c.title, c.type_code);
	    cardCodes.put(c.code, c.title);
	    cardFaction.put(c.title, c.faction_code);
	    cardInfluence.put(c.title, c.faction_cost);
	}

	System.out.println("%" + library.data.size());	
    }

    private static void process_netrunner_mwl() {
	//for the time being, we'll just use a file saved on disk
	//later on, we should be taking it directly from the API (and saving it to a file, then always using that file)
	//but we can do that later :)
	String fname = "conf/NRDB_mwl.json";

	//now let us try to process this file to a string
	String mwlJSON = read_netrunner_mwl(fname);

	boolean valid = mwlJSON != null;
	System.out.println("%MWL read: " + valid);

	Gson gson = new Gson();
	MWL_List mlist = gson.fromJson(mwlJSON, MWL_List.class);

	/*for(Card c : library.data) {
	    //put in cards (name, type)
	    cards.put(c.title, c.type_code);
	}

	System.out.println("%" + library.data.size());	*/
	System.err.println("% MWL processed?");
	System.err.println("% Number of MWL's: " + mlist.data.size());
	System.err.println("% Chosen MWL: " + MWLVersion);

	MWL myMWL = null;
	for(MWL mwl : mlist.data) {
	    System.err.println("% MWL - " + mwl.name);
	    if(mwl.name.equals(MWLVersion))
		myMWL = mwl;
	}

	System.err.println("% Cards on MWL: " + myMWL.cards.size());
	chosenMWL = myMWL;
	//TODO: figure this out
	for(Map.Entry<String, Entry> entry : myMWL.cards.entrySet()) {
	    String code = entry.getKey();
	    Entry val = entry.getValue();
	    String name = cardCodes.get(code);	    
	    val.CARD_NAME = name;
	    System.err.println("%CARD " + code + " is on the MWL");
	}
    }

    private static String read_netrunner_library(String fname /*NRDB_cards.json*/){
	StringBuilder builder = new StringBuilder();
	try (Stream<String> stream = Files.lines(Paths.get(fname), StandardCharsets.UTF_8)) {
	    stream.forEach(s -> builder.append(s).append("\n"));
	} catch (IOException e) {
	    //the file doesn't exist: have we tried fetching it yet this execution?
	    if(!regeneratedLibrary) {
		System.err.println("% couldn't open decklist file. Checking online...");
		if(download_library())
		    return read_netrunner_library(fname);
	    }
	    
	    //e.printStackTrace();
	    return null;
	}
	
	return builder.toString();
    }

    private static String read_netrunner_mwl(String fname /*NRDB_mwl.json*/){
	StringBuilder builder = new StringBuilder();
	try (Stream<String> stream = Files.lines(Paths.get(fname), StandardCharsets.UTF_8)) {
	    stream.forEach(s -> builder.append(s).append("\n"));
	} catch (IOException e) {
	    //the file doesn't exist: have we tried fetching it yet this execution?
	    if(!regeneratedLibrary) {
		regeneratedLibrary = true;
		System.err.println("% couldn't open mwl file. Checking online...");
		if(download_mwl())
		    return read_netrunner_mwl(fname);
	    }
	    
	    //e.printStackTrace();
	    return null;
	}
	
	return builder.toString();
    }

    private static boolean download_factions() {
	regeneratedLibrary = true; //we only make one attempt at this per run
	//TODO: put these urls in a conf file
	try {
	    URL nrdb = new URL("https://netrunnerdb.com/api/2.0/public/factions");
	    
	    File targetFile = new File("conf/NRDB_factions.json");
	    InputStream stream = nrdb.openStream();
	    java.nio.file.Files.copy(stream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
	    
	    IOUtils.closeQuietly(stream);
	    return true;
	} catch (Exception e) {
	    //System.err.println(e.toString());
	    System.err.println("%Cannot download netrunner factions list. Please check your internet connection");
	    return false;
	}
    }
    
    private static boolean download_library() {
	regeneratedLibrary = true; //we only make one attempt at this per run
	//TODO: put these urls in a conf file
	try {
	    URL nrdb = new URL("https://netrunnerdb.com/api/2.0/public/cards");
	    //BufferedReader in = new BufferedReader(new InputStreamReader(nrdb.openStream()));
	    
	    File targetFile = new File("conf/NRDB_cards.json");
	    InputStream stream = nrdb.openStream();
	    java.nio.file.Files.copy(stream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
	    
	    IOUtils.closeQuietly(stream);
	    return true;
	} catch (Exception e) {
	    //System.err.println(e.toString());
	    System.err.println("%Cannot download netrunner cardpool. Please check your internet connection");
	    return false;
	}
    }

    private static boolean download_mwl() {
	regeneratedLibrary = true; //we only make one attempt at this per run
	//TODO: put these urls in a conf file
	try {
	    URL nrdb = new URL("https://netrunnerdb.com/api/2.0/public/mwl");
	    //BufferedReader in = new BufferedReader(new InputStreamReader(nrdb.openStream()));
	    
	    File targetFile = new File("conf/NRDB_mwl.json");
	    InputStream stream = nrdb.openStream();
	    java.nio.file.Files.copy(stream, targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
	    
	    IOUtils.closeQuietly(stream);
	    return true;
	} catch (Exception e) {
	    //System.err.println(e.toString());
	    System.err.println("%Cannot download netrunner MWL. Please check your internet connection");
	    return false;
	}
    }
    
    private static void process_decklist(String s) {
	//first thing to do is read the decklist into a list
	List<String> decklist;

	try (Stream<String> lines = Files.lines(Paths.get(s))){
	    decklist = lines.collect(Collectors.toList());
	} catch (Exception e) {
	    System.err.println("Unable to read decklist in file " + s);
	    return;
	}

	
	
	String deckName = s;
	String idName = "Unspecified ID";

	String chosenFaction = "auto";
	boolean facPassed = false;
	
	while(true) {
	    String first = decklist.get(0);

	    //see if the first line is a deck-name. If it is, we use that.
	    if(first.length() > 1 && first.charAt(0) == '_') {
		deckName = first.substring(1);
		decklist.remove(0);
		continue;
	    }
	    
	    //see if the (new) first line is an identity. If it is, we use that.
	    if(first.length() > 1 && first.charAt(0) == '#') {
		idName = first.substring(1);
		decklist.remove(0);
		continue;
	    }

	    //see if we get a faction choice
	    if(first.length() > 1 && first.charAt(0) == '*') {
		chosenFaction = first.substring(1);
		decklist.remove(0);
		facPassed = true;
		continue;
	    }

	    break;
	}


	//System.out.println("Deck Name: " + deckName);
	//System.out.println("Identity:  " + idName);

	TreeMap<String, TreeMap<String, Integer>> bins = new TreeMap<>();
	
	//now we want to build a dictionary of card name - card numbers
	for(String line : decklist) {
	    if(line.length() > 0) {
		try {
		    String parts[] = line.split(" ", 2);
		    int numInDeck = Integer.parseInt(parts[0]);
		    String cardName = parts[1];

		    //now that we have the cardName, we can get the type from our dictionary
		    String type = cards.get(cardName);
		    if(type == null)
			type = "Unassigned";

		    //now what we want to do is select a bin based on type
		    TreeMap<String, Integer> bin = bins.get(type);
		    if(bin == null) {
			bin = new TreeMap<String, Integer>();
			bins.put(type, bin);
		    }
		    bin.put(cardName, numInDeck);
		} catch (Exception e) {
		    //do nothing - we just skip this line
		}		
	    }
	}

	System.out.println("\\newcommand{\\" + decklistPrefix + ns(deckNum) +  "}{");
	System.out.println("\\textbf{" + idName + "}\\\\");
	boolean _first = true;
	//now we have a list of binned cards: let's print that out
	//here we also interface with the mwl (if applicable)

	if(chosenFaction == "auto") {
	    //determine which id gives us the least pips!
	    Set<String> facList = factions.keySet();
	    String lowestFac = "anarch";
	    Integer lowestInf = 99999999;
	    for(String factionName : facList) {
		int tally = 0;
		for(Map.Entry<String, TreeMap<String, Integer>> entry : bins.entrySet()) {
		    TreeMap<String, Integer> bin = entry.getValue();
		    for(Map.Entry<String, Integer> entry2 : bin.entrySet()) {
			Integer num = entry2.getValue();
			String name = entry2.getKey();
			String cFaction = cardFaction.get(name);
			Integer cInfluence = cardInfluence.get(name);
			if(!cFaction.equals(factionName)) {
			    if(cInfluence != null) {
				tally += num*cInfluence;
			    }
			}
		    }
		}

		if(tally < lowestInf) {
		    lowestInf = tally;
		    lowestFac = factionName;
		}
	    }

	    System.err.println("%Chose faction " +lowestFac+ " as it had the lowest influence cost (" + lowestInf);
	    chosenFaction = lowestFac;
	    facPassed = true;
	}
	
	boolean facWarningDisplayed = false;
	for(Map.Entry<String, TreeMap<String, Integer>> entry : bins.entrySet()) {
	    String type = entry.getKey();
	    TreeMap<String, Integer> bin = entry.getValue();
	    int size = 0;
	    for(Integer i : bin.values())
		size += i;
	    type = type.substring(0, 1).toUpperCase() + type.substring(1);
	    
	    if(!_first)
		System.out.println("\\clb");
	    _first = false;
		
	    System.out.println(String.format("\\textbf{%s (%d)}\\\\", type, size));
	    
	    for(Map.Entry<String, Integer> entry2 : bin.entrySet()) {
		String name = entry2.getKey();
		Integer num = entry2.getValue();
		//Entry mwlEntry = chosenMWL.cards.get(cardCode);
		//knowing the name, we can get the faction and influence cost
		String cFaction = cardFaction.get(name);
		Integer cInfluence = cardInfluence.get(name);
		
		String name2 = name;
		name = escape(name);
		//resolve influence
		if(processFactionInfluence && facPassed) {
		    if(!chosenFaction.equals(cFaction)) {
			//only apply influence when the faction is not our faction
			if(cInfluence != null && cInfluence > 0) {
			    name = name + " $";
			    for(int i = 0; i < num * cInfluence; i++) {
				name = name + "\\circ";
			    }
			    name = name + " $";
			}
		    }
		} else if (processFactionInfluence && !facWarningDisplayed) {
		    System.err.println("% No faction was supplied in the .deck file.  Supply a faction at the top of the file like *weyland-consortium. Alternatively, use *auto to automatically pick the faction with the lowest total inf cost");
		    facWarningDisplayed = true;
		}
	    
		//restricted: /wp
		//banned: \times
		//global_inf: \bullet
		
		
		if(chosenMWL != null) {
		    for(Entry mwlEntry : chosenMWL.cards.values()) {
			if(mwlEntry.CARD_NAME.equals(name2)) {
			    System.err.println("%CARD ON MWL: " + name);
			    
			    if (mwlEntry.is_restricted == 1)
				name = name + "$ \\Psi$";
			    else if (mwlEntry.global_penalty > 0) {
				name = name + " ";
				for(int i = 0; i < mwlEntry.global_penalty * num; i++)
				    name = name + "$\\bullet$";
			    }
			    else if (mwlEntry.universal_faction_cost > 0) {
				name = name + " ";
				for(int i = 0; i < mwlEntry.universal_faction_cost * num; i++)
				    name = name + "$\\bullet$";
			    }
			    else if(mwlEntry.deck_limit == 0)
				name = name + " $\\Chi$";
			    else
				continue;
			    break;
			}
		    }
		}
		
		
		System.out.println(String.format("%d %s\\\\", num, name));		
	    } 
	} 

	System.out.println("}");

	System.out.println("\\newcommand{\\" + decknamePrefix + ns(deckNum) + "}{\\deckname{" + escape(deckName) + "}}");

	flush.append("\\decklistCard{\\" + decknamePrefix+ns(deckNum) + "}{\\" + decklistPrefix + ns(deckNum) + "}\n");
	if(deckNum %2 == 0)
	    flush.append("\\\\ \n\\\\ \n");
    }

    private static String escape(String str) {
	String res = "";
	char[] esc = {'{', '}', '_', '^', '#', '&', '$', '%', '~'};
	
	for (char c: str.toCharArray()) {
	    for(int i = 0; i < esc.length; i++)
		if(c == esc[i])
		    res += '\\';
	    res += c;
	}

	return res;

    }
    
    private static String ns(int num) {
	String str = "" + num;
	String res = "";
	for (char c: str.toCharArray()) {
	    res += (char)(c + 21);
	}

	return res;
    }
    
    private static void process_preamble() {
	List<String> packages = read_packages("conf/packages.conf");
	System.out.println(process_packages(packages));

	List<String> pgfmacros = read_pgf_macros("conf/pgfmacros.conf");
	System.out.println(process_pgf_macros(pgfmacros));

	List<String> commands = read_commands("conf/commands.conf");
	System.out.println(process_commands(commands));

	List<String> card = read_card("conf/card.conf");
	System.out.println(process_card(card));
    }

    
    
    private static String process_packages(List<String> packages) {
	StringBuilder builder = new StringBuilder("\\documentclass[parskip]{scrartcl}\n");
	builder = process_helper(builder, "\\usepackage%s%n", packages);

	builder.append(String.format("%n%s%n", "\\begin{document}"));
	return builder.toString();
    }

    private static String process_pgf_macros(List<String> macros) {
	StringBuilder builder = new StringBuilder();
	builder = process_helper(builder, "\\pgfmathsetmacro%s%n", macros);

	return builder.toString();
    }

    private static String process_commands(List<String> commands) {
	StringBuilder builder = new StringBuilder();
	builder = process_helper(builder, "\\newcommand%s%n", commands);

	return builder.toString();
    }

    private static String process_card(List<String> card) {	
	StringBuilder builder = new StringBuilder();
	builder = process_helper(builder, "%s%n", card);

	return builder.toString();
    }
    
    private static StringBuilder process_helper(StringBuilder builder, String format, List<String> items) {
	for(String s : items) {
	    builder.append(String.format(format, s));
	}

	return builder;
    }

    //SECTION: PROCESS FILES
    private static List<String> read_card(String fname /*card.conf*/) {
	List<String> res;
	try (Stream<String> lines = Files.lines(Paths.get(fname))) {
	    res = lines.collect(Collectors.toList());
	} catch (Exception e) {
	    res = new ArrayList<String>();
	    res.add("\\newcommand{\\decklistCard}[2]{)");
	    res.add("\\scalebox{\\scaleFactor}{");
	    res.add("\\begin{tikzpicture}");
	    res.add("\\draw[rounded corners=\\cardroundingradius] (0,0) rectangle (\\cardwidth,\\cardheight);");
	    res.add("\\fill[\\stripcolor,rounded corners=\\striproundingradius] (\\strippadding,\\strippadding) rectangle (\\strippadding+\\stripwidth,\\cardheight-\\strippadding) node[rotate=90,above left,black,font=\\stripfontsize] {#1};");
	    res.add("\\node[text width=(\\cardwidth-\\strippadding-\\stripwidth-2*\\textpadding)*1cm,below right,inner sep=0] at (\\strippadding+\\stripwidth+\\textpadding,\\cardheight-\\textpadding)"); 
	    res.add("{");  
	    res.add("{\\decklistsize #2}");
	    res.add("};");
	    res.add("\\end{tikzpicture}");
	    res.add("}}");
	}

	return res;
    }
    private static List<String> read_commands(String fname /*commands.conf*/) {
	List<String> res;
	try (Stream<String> lines = Files.lines(Paths.get(fname))) {
	    res = lines.collect(Collectors.toList());
	} catch (Exception e) {
	    res = new ArrayList<String>();
	    res.add("{\\stripfontsize}{\\huge}");
	    res.add("{\\decklistsize}{\\tiny}");
	    res.add("{\\stripcolor}{cyan}");
	    res.add("{\\scaleFactor}{1.05}");
	    res.add("{\\clb}{\\vspace{1mm}\\hline\\\\ \\vspace{1mm}}");
	    res.add("{\\deckname}[1]{\\parbox{3.25in}{\\center #1}}");
	}

	return res;
    }
    
    private static List<String> read_packages(String fname /*packages.conf*/) {
	List<String> res;
	try (Stream<String> lines = Files.lines(Paths.get(fname))) {
	    res = lines.collect(Collectors.toList());
	} catch (Exception e) {
	    res = new ArrayList<String>();
	    res.add("[margin=15mm]{geometry}");
	    res.add("{tikz}");
	    res.add("{pifont}");
	    res.add("{graphicx}");
	}

	return res;
    }

    private static List<String> read_pgf_macros(String fname /*pgfmacros.conf*/) {
	List<String> res;
	try (Stream<String> lines = Files.lines(Paths.get(fname))) {
	    res = lines.collect(Collectors.toList());
	} catch (Exception e) {
	    res = new ArrayList<String>();
	    res.add("{\\cardroundingradius}{4mm}");
	    res.add("{\\striproundingradius}{3mm}");
	    res.add("{\\cardwidth}{6.30}");
	    res.add("{\\cardheight}{8.80}");
	    res.add("{\\stripwidth}{1.2}");
	    res.add("{\\strippadding}{0.1}");
	    res.add("{\\textpadding}{0.3}");
	}

	return res;
    }
}
