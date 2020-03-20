# 'Android: Netrunner' decklist tool

This is a tool designed to take files like this:

```
_#SLOVAKIA HAYLEY
#Hayley Kaplan
2 Diesel
3 Stimhack
3 Sure Gamble
1 Buffer Drive
2 R&D Interface
3 Simulchip
1 Turntable
3 Aesop's Pawnshop
1 Artist Colony
1 Beth Kilrain-Chang
1 Cybertrooper Talut
3 Daily Casts
3 Fan Site
3 Professional Contacts
1 Bukhgalter
1 Corroder
3 Euler
3 Harbinger
1 Misdirection
3 Pelangi
3 Self-modifying Code
```

And turn them into printable decklist cards, like this:
![decklist cards](https://i.imgur.com/8zqz9Qe.jpg)

The decklists used in the picture come from [Here](https://netrunnerdb.com/en/decklist/58336/-slovakia-wants-to-play-hayley-too-23rd-at-german-nationals "#Slovakia wants to play Hayley too"), [Here](https://netrunnerdb.com/en/decklist/58546/rags-to-riches-movie-star-success-story "Rags to Riches Movie Star Success Story"), [Here](https://netrunnerdb.com/en/decklist/58558/aesops-rebooted-crit-hit-gnk-1st- "Aesops Rebooted"), [and Here](https://netrunnerdb.com/en/decklist/58494/argus-that-stopped-the-huns-5-0-at-hungarian-nationals- "Argus that stropped the Huns")

These cards should be (at least they are on my printer) about the perfect size to fit in a 2.5" x 3.5" card sleeve, infront of another card.

### Dependencies
The build for this tool relies on [Apache Ant](https://ant.apache.org/) and [Apache Ivy](https://ant.apache.org/ivy/). It is compiled using Java (version 8+), so that also needs to be installed on your system.

If you do not have a current install of Apache Ivy, but do have Apache Ant, then you can run the following commands to get Ivy set up.

```bash
cd Android-Netrunner-Decklist
ant bootstrap
```

This will put the Ivy v2.5 jars in your /home/user/.ant/lib/ folder.

### How do I build this program?
```bash
ant
```

That is all.

### How do I run this program?
If the build went well, a .jar file will be produced in the Android-Netrunner-Decklist/dist/ folder.

There will also be a conf folder, with various configs you can change to manipulate the output of the program. 

To run the program, use the following command: `java -jar ANR-Decklist.jar myDeckFile1.deck ... myDeckFileN.deck`

On the first run, the program will download the cardpool list from netrunnerdb, as well as the factions list and the mwl listings. All in all, this is around 1.5mb of data. This will be cached, but you can force re-downloading by either deleting the .json files produced in the `conf` folder, or by using the `-r` argument when running the program. 

Additionally, you can use the `-mwl` argument to have MWL information printed on the decklist cards (set which specific mwl in the mwl.conf file), and you can set whether to track faction influence with the `-f` command.

When tracking influence, you can specify the specific faction at the top of the decklist file like so:
```
*weyland-consortium
```
However, if you omit that line, or just use the word auto, then the program will automatically calculate which faction gives you the lowest influence score, and assign influence based on that.

The file presented at the top was produced with a command that looked like this:
```bash
java -jar ANR-DeckList.jar -mwl -f ../GermanHayley.deck ../RagsToRiches.deck ../AesopsRebooted.deck ../HungarianArgus.deck > target.tex
```

### What is the output of this program
This program produces latex code as output. You can paste the code into Overleaf (recommended if you don't want to install anything), or you can produce output using pdflatex. Using pdflatex, you need three packages installed: texlive-picture, texlive-latex-recommended, and texlive-latex-base.

If compiling with pdflatex, you probably have to mash your way through a handful of warnings.


GermanHayley deck comes from here: https://netrunnerdb.com/en/decklist/58336/-slovakia-wants-to-play-hayley-too-23rd-at-german-nationals 

### Issues
I don't account for Alliance cards. I may implement this at a later data. Each individual card needs its own special rule, which is quite tedious.
