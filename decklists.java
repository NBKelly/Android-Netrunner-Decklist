//This program exists to generate valid LaTeX from one (or many) decklist files
//The output wont always work as intended (very big decks), but for the most part it should be fine

import java.util.*;
import java.util.stream.*;
import java.nio.file.*;

public class decklists {
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
