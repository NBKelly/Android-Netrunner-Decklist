//This program exists to generate valid LaTeX from one (or many) decklist files
//The output wont always work as intended (very big decks), but for the most part it should be fine

import java.util.*;
import java.util.stream.*;
import java.nio.file.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;

public class decklists {
    private class Library {
	public String imageUrlTemplate;
	public List<Card> data;
	public Integer total;
	public boolean success;
	public String version_number;
	public String last_updated;
    }

    private class Card {
	public String code;
	public Integer deck_limit;
	public String faction_code;
	public Integer faction_cost;
	
	public String flavor;
	public String illustrator;
	public String influence_limit;
	public String keywords;
	
	public Integer minimum_deck_size;
	public String image_url;
	public String pack_code;
	public Integer position;

	public Integer quantity;
	public String side_code;
	public String text;
	public String title;
	
	public String type_code;
	public Boolean uniqueness;
	public Integer base_link;

	public Card(String code, int deck_limit, String faction_code, int faction_cost,
		    String flavor, String illustrator, String influence_limit, String keywords,
		    int minimum_deck_size, String image_url, String pack_code, int position,
		    int quantity, String side_code, String text, String title,
		    String type_code, boolean uniqueness, int base_link) {
	    this.code = code;
	    this.deck_limit = deck_limit;
	    this.faction_code = faction_code;
	    this.faction_cost = faction_cost;

	    this.flavor = flavor;
	    this.illustrator = illustrator;
	    this.influence_limit = influence_limit;
	    this.keywords = keywords;

	    this.minimum_deck_size = minimum_deck_size;
	    this.image_url = image_url;
	    this.pack_code = pack_code;
	    this.position = position;

	    this.quantity = quantity;
	    this.side_code = side_code;
	    this.text = text;
	    this.title = title;

	    this.type_code = type_code;
	    this.uniqueness = uniqueness;
	    this.base_link = base_link;
	}
    }

    private static TreeMap<String, String> cards = new TreeMap<String, String>();
    
    public static void main(String[] args) {
	//each argument is expected to be a decklist file
	//the name of the file will be considered the name of the deck
	// OVERRIDE if there is an _ as the first character of the first line - that is the name

	//format of a file:
	//  _DECKNAME (optional)
	//  IDENTITY
	//
	//  card type
	//  n card 1
	//  ...
	//  n card m
	//  [whitespace]
	//  card type
	//  n card 1
	//  ...
	//  n card m

	//extra-commands for latex (if you're a pro)

	process_preamble();
	
	for(String s : args) {
	    process_decklist(s);
	}

	process_netrunner_library();
    }

    private static void process_netrunner_library() {
	//for the time being, we'll just use a file saved on disk
	//later on, we should be taking it directly from the API (and saving it to a file, then always using that file)
	//but we can do that later :)
	String fname = "NRDB_cards.json";

	//now let us try to process this file to a string
	String libraryJSON = read_netrunner_library(fname);

	boolean valid = libraryJSON != null;
	System.out.println("Library read: " + valid);

	Gson gson = new Gson();
	Library library = gson.fromJson(libraryJSON, Library.class);

	for(Card c : library.data) {
	    //put in cards (name, type)
	    cards.put(c.title, c.type_code);
	}
	System.out.println(library.data.size());
	
    }

    private static String read_netrunner_library(String fname /*NRDB_cards.json*/){
	StringBuilder builder = new StringBuilder();
	try (Stream<String> stream = Files.lines( Paths.get(fname), StandardCharsets.UTF_8)) {
	    stream.forEach(s -> builder.append(s).append("\n"));
	} catch (IOException e) {
	    e.printStackTrace();
	    return null;
	}
	
	return builder.toString();
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

	
	String first = decklist.get(0);
	String deckName = s;
	String idName = "Unspecified ID";

	//see if the first line is a deck-name. If it is, we use that.
	if(first.length() > 1 && first.charAt(0) == '_') {
	    deckName = first.substring(1);
	    decklist.remove(0);
	}

	first = decklist.get(0);
	//see if the (new) first line is an identity. If it is, we use that.
	if(first.length() > 1 && first.charAt(0) == '#') {
	    idName = first.substring(1);
	    decklist.remove(0);
	}


	System.out.println("Deck Name: " + deckName);
	System.out.println("Identity:  " + idName);

	TreeMap<String, Integer> cardsInDeck = new TreeMap<>();
	
	//now we want to build a ditionary of card name - card numbers
	for(String line : decklist) {
	    if(line.length() > 0) {
		try {
		    String parts[] = line.split(" ", 2);
		    int numInDeck = Integer.parseInt(parts[0]);
		    String cardName = parts[1];
		    System.out.println(String.format("Card: %s, Copies: %d", cardName, numInDeck));
		    cardsInDeck.put(cardName, numInDeck);
		} catch (Exception e) {
		    //do nothing - we just skip this line
		}		
	    }
	}
    }

    private static void process_preamble() {
	List<String> packages = read_packages("packages.conf");
	System.out.println(process_packages(packages));

	List<String> pgfmacros = read_pgf_macros("pgfmacros.conf");
	System.out.println(process_pgf_macros(pgfmacros));

	List<String> commands = read_commands("commands.conf");
	System.out.println(process_commands(commands));

	List<String> card = read_card("card.conf");
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
