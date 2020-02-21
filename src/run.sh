#!/bin/bash

java -jar ANR-DeckList.jar $@ > target.tex
pdflatex -interaction=nonstopmode target.tex
firefox target.pdf
