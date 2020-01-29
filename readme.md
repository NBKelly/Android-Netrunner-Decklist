# 'Android: Netrunner' decklist tool

This is a tool designed to take a file like this:

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

And turn that file into a printable decklist card, like this:


If that does not work (ivy is not there), then run ant bootstrap, followed by ant dist

java decklists [-r: optional] [-mwl: optional] myDeck1.txt ... myDeckN.txt

-r to download json files from NRDB
-mwl to apply the mwl (specific format specified in mwl.conf)
TODO: -r, api



GermanHayley deck comes from here: https://netrunnerdb.com/en/decklist/58336/-slovakia-wants-to-play-hayley-too-23rd-at-german-nationals 
